package org.overengineer.bettacraft.fertilizer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModFluids;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M37: liquid fertilizer in a tank. The level ({@link TankData#fertilizer}, 1 = 100 % = the right amount) rises with each
 * portion: a portion ({@value #PORTION_MB} mB, a fifth of a bottle) brings {@code fertilizer.portionWaterBlocks} water
 * blocks to 100 %. Up to 100 % it speeds up the growth of Bettacraft's aquarium plants (not the corals) and floating plants
 * ({@code fertilizer.growthBoost}); plants grow without it as well. The plants use it up (more in a dense tank), a little
 * goes by itself, water changes dilute it (it travels with the water in the pipes). From {@code fertilizer.overdoseAt}
 * (150 %) the tank is overdosed: no more growth, but the algae grow faster ({@code fertilizer.overdoseAlgae} at twice
 * that). Plants of other mods in the interior grow by their own (vanilla) rules and never see it.
 */
public final class Fertilizer {
    /** mB (and mL) of one portion; a bottle holds {@value #BOTTLE_PORTIONS}, a batch of the maker is a bottle. */
    public static final int PORTION_MB = 50, BOTTLE_PORTIONS = 5, BATCH_MB = PORTION_MB * BOTTLE_PORTIONS;

    private Fertilizer() {}

    public static boolean enabled() {
        return Features.enabled(Feature.FERTILIZER);
    }

    /** The dosing pump may dose it ({@code features.fertilizer.dosing}). */
    public static boolean dosingEnabled() {
        return Features.enabled(Feature.FERTILIZER_DOSING);
    }

    public static double portionBlocks() {
        return ServerConfig.getOr(ServerConfig.FERT_PORTION_BLOCKS, 4.0);
    }

    public static double overdoseAt() {
        return ServerConfig.getOr(ServerConfig.FERT_OVERDOSE, 1.5);
    }

    /** How much {@code portions} raise the level of this tank. */
    public static double rise(TankData t, double portions) {
        return portions * portionBlocks() / Math.max(1.0, t.waterBlocks());
    }

    /** Adds {@code portions} to the tank (shared by its whole water). */
    public static void dose(TankData t, double portions) {
        if (portions <= 0 || t.waterMb() <= 0) return;
        t.setFertilizer((float) (t.fertilizer() + rise(t, portions)));
    }

    /** Adds {@code ml} (mB) of fertilizer (the dosing pump, the pipes). */
    public static void doseMl(TankData t, double ml) {
        dose(t, ml / PORTION_MB);
    }

    // ------------------------------------------------------------------ effects

    /** Growth factor of our plants: 1 without fertilizer, 1 + growthBoost at 100 % and above (1 with the feature off). */
    public static double growth(TankData t) {
        if (!enabled() || t.waterMb() <= 0) return 1.0;
        return growth(t.fertilizer(), ServerConfig.getOr(ServerConfig.FERT_BOOST, 0.5));
    }

    /** Pure: the growth factor at {@code level}. */
    public static double growth(double level, double boost) {
        return 1.0 + Math.max(0, boost) * Math.max(0, Math.min(1.0, level));
    }

    /** Algae factor: 1 up to the overdose level, then rising to {@code overdoseAlgae} at twice that level. */
    public static double algae(TankData t) {
        if (!enabled() || t.waterMb() <= 0) return 1.0;
        return algae(t.fertilizer(), overdoseAt(), ServerConfig.getOr(ServerConfig.FERT_ALGAE, 3.0));
    }

    /** Pure: the algae factor at {@code level}. */
    public static double algae(double level, double over, double max) {
        if (level <= over || over <= 0) return 1.0;
        double x = Math.min(1.0, (level - over) / over);
        return 1.0 + (Math.max(1.0, max) - 1.0) * x;
    }

    public static boolean overdosed(TankData t) {
        return enabled() && t.waterMb() > 0 && t.fertilizer() > overdoseAt();
    }

    /** What the water tab says: none, low, good, overdosed. */
    public enum State { NONE, LOW, GOOD, OVERDOSED }

    public static State state(double level, double over) {
        if (level < 0.05) return State.NONE;
        if (level < 0.5) return State.LOW;
        if (level <= over) return State.GOOD;
        return State.OVERDOSED;
    }

    // ------------------------------------------------------------------ use

    /** The plants used it: {@code plantBlocks} blocks (in tank blocks) of growing plants for {@code days}. */
    public static void used(TankData t, double plantBlocks, double days) {
        if (!enabled() || days <= 0 || plantBlocks <= 0 || t.fertilizer() <= 0) return;
        double k = ServerConfig.getOr(ServerConfig.FERT_PLANT_USE, 0.5);
        t.setFertilizer((float) (t.fertilizer() - k * plantBlocks / Math.max(1.0, t.waterBlocks()) * days));
    }

    /** The care step: a little goes by itself (exponential, {@code fertilizer.fertilizerDecayPerDay}). */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        TankData t = c.getTank();
        if (!enabled() || days <= 0 || t.fertilizer() <= 0) return;
        double k = ServerConfig.getOr(ServerConfig.FERT_DECAY, 0.05);
        float before = t.fertilizer();
        float now = (float) (before * Math.exp(-k * days));
        if (now < 1e-4F) now = 0F;
        t.setFertilizer(now);
        double over = overdoseAt();
        if (before <= over && now > over || before > over && now <= over) c.markTankChanged();
        else c.setChanged();
    }

    // ------------------------------------------------------------------ the fluid

    public static boolean isFluid(FluidStack s) {
        return !s.isEmpty() && s.getFluid().isSame(ModFluids.FERTILIZER.get());
    }

    public static FluidStack fluid(int mb) {
        return new FluidStack(ModFluids.FERTILIZER.get(), mb);
    }

    /**
     * Fertilizer flows into a tank through a pipe (from the fertilizer maker): it takes what brings its level up to
     * {@code fertilizer.pipeUpTo} (100 %), so a pipe never overdoses it. Returns the mB taken.
     */
    public static int fillTank(ServerLevel level, TankBlockEntity c, FluidStack resource, IFluidHandler.FluidAction action) {
        if (!enabled() || !isFluid(resource)) return 0;
        TankData t = c.getTank();
        if (t.waterMb() <= 0) return 0;
        double room = ServerConfig.getOr(ServerConfig.FERT_PIPE_UP_TO, 1.0) - t.fertilizer();
        if (room <= 0) return 0;
        int mb = (int) Math.min(resource.getAmount(), Math.floor(room * Math.max(1.0, t.waterBlocks()) / portionBlocks() * PORTION_MB));
        if (mb <= 0) return 0;
        if (action.execute()) {
            doseMl(t, mb);
            c.markTankChanged();
        }
        return mb;
    }

    /** A bottle of liquid fertilizer used on a tank: one portion into the water. */
    public static ItemInteractionResult useOnTank(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!stack.is(ModItems.LIQUID_FERTILIZER.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        if (c == null) return ItemInteractionResult.CONSUME;
        TankData t = c.getTank();
        if (!TankInhabitants.canModify(player, t)) {
            player.displayClientMessage(Component.translatable("message.bettacraft.tank.locked"), true);
            return ItemInteractionResult.CONSUME;
        }
        if (!enabled()) {
            player.displayClientMessage(Component.translatable("message.bettacraft.fertilizer.off").withStyle(ChatFormatting.GOLD), true);
            return ItemInteractionResult.CONSUME;
        }
        if (t.waterMb() <= 0) {
            player.displayClientMessage(Component.translatable("message.bettacraft.reef.no_water").withStyle(ChatFormatting.GOLD), true);
            return ItemInteractionResult.CONSUME;
        }
        boolean wasOver = overdosed(t);
        dose(t, 1);
        if (!player.getAbilities().instabuild) player.setItemInHand(hand, FertilizerBottleItem.usePortion(stack));
        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.7F, 1.2F);
        int pct = Math.round(t.fertilizer() * 100);
        boolean over = overdosed(t);
        player.displayClientMessage(Component.translatable(over ? "message.bettacraft.fertilizer.overdosed" : "message.bettacraft.fertilizer.dosed", pct)
                .withStyle(over ? ChatFormatting.GOLD : ChatFormatting.GREEN), true);
        if (over && !wasOver) TankCare.log((ServerLevel) level, c, "log.bettacraft.fertilizer_overdosed", String.valueOf(pct));
        c.markTankChanged();
        return ItemInteractionResult.CONSUME;
    }

    /** An empty glass bottle for a used-up fertilizer bottle. */
    static ItemStack emptyBottle() {
        return new ItemStack(Items.GLASS_BOTTLE);
    }
}
