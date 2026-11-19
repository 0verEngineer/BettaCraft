package org.overengineer.bettacraft.reef;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tankmates.Clutch;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M48 part b: the clownfish larvae and their food chain ({@code features.saltwater.larvaRearing}). The larvae of a clownfish
 * clutch drift for {@code reef.clownLarvaDays} after hatching (the larva stage of the clutch). In the first half of it they
 * only eat <b>rotifers</b>, later also <b>artemia nauplii</b>; fed they mostly survive (about 80 %), without food only
 * {@code reef.clownLarvaSurvival} of them (5 %: the breeding of older worlds goes on, just poorly). The food is a pool of
 * the tank ({@link TankData#rotifers()}, {@link TankData#nauplii()}): rotifers and nauplii fed into a reef tank go there
 * (hand, feeder, live food pump), <b>phytoplankton</b> (a bottle used on the tank) keeps the rotifers alive and lets them
 * multiply; without it they die within a few days, the nauplii too.
 */
public final class ClownLarvae {
    /** Food portions one larva eats per day. */
    public static final double FOOD_PER_LARVA = 0.05;
    /** Portions of one bottle of rotifers, artemia nauplii or phytoplankton in the tank. */
    public static final double PER_ITEM = 4.0;
    /** Most portions of each kind the water of a tank holds. */
    public static final double CAP = 24.0;
    /** Share of the larvae that survive the larva days when they are always fed. */
    public static final double FED_SURVIVAL = 0.8;

    private ClownLarvae() {}

    public static boolean active() {
        return Features.enabled(Feature.LARVA_REARING);
    }

    public static double larvaDays() {
        return ServerConfig.getOr(ServerConfig.REEF_CLOWN_LARVA_DAYS, 3.0);
    }

    public static double baseSurvival() {
        return ServerConfig.getOr(ServerConfig.REEF_CLOWN_LARVA_SURVIVAL, 0.05);
    }

    /** A clownfish clutch (they spawn at their anemone). */
    public static boolean clownClutch(Clutch k) {
        return k.species.breeding() == TankmateSpecies.Breeding.ANEMONE;
    }

    public static boolean hasClownClutch(TankData t) {
        for (Clutch k : t.clutches()) if (clownClutch(k)) return true;
        return false;
    }

    // ------------------------------------------------------------------ the plankton in the water

    /** Once per care step: the phytoplankton fades (eaten, settled), the rotifers live on it, the nauplii outgrow their use. */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        TankData t = c.getTank();
        if (days <= 0 || t.rotifers() <= 0 && t.phytoplankton() <= 0 && t.nauplii() <= 0) return;
        double phyto = t.phytoplankton(), rot = t.rotifers();
        // the rotifers graze the phytoplankton: fed, they multiply a little; hungry, most die within three days
        double eat = Math.min(phyto, rot * 0.25 * days);
        double fedShare = rot <= 0 ? 0 : Math.min(1, eat / Math.max(1e-9, rot * 0.25 * days));
        rot *= Math.exp((fedShare * 0.3 - (1 - fedShare) * 0.8) * days);
        phyto = (phyto - eat) * Math.exp(-0.3 * days);
        t.setRotifers(Math.min(CAP, rot));
        t.setPhytoplankton(phyto);
        t.setNauplii(t.nauplii() * Math.exp(-0.6 * days));
        c.setChanged();
    }

    // ------------------------------------------------------------------ the larvae

    /**
     * One step of a clownfish clutch in its larva stage: the larvae eat from the pool (the first half of the larva days rotifers
     * only), the unfed die faster. Returns whether the count changed. Marks the clutch as raised on rotifers.
     */
    public static boolean larvae(ServerLevel level, TankBlockEntity c, Clutch k, double days, RandomSource r) {
        if (!active() || !clownClutch(k) || k.stage() != 1 || days <= 0 || k.count <= 0) return false;
        TankData t = c.getTank();
        double larva = Math.max(0, k.age - Clutch.eggDays(k.species));
        boolean early = larva < Clutch.larvaDays(k.species) * 0.5;
        double need = k.count * FOOD_PER_LARVA * days;
        double got = Math.min(t.rotifers(), need);
        if (got > 0) {
            t.setRotifers(t.rotifers() - got);
            k.rotifers = true;
        }
        if (!early && got < need) {
            double n = Math.min(t.nauplii(), need - got);
            t.setNauplii(t.nauplii() - n);
            got += n;
        }
        double q = need <= 0 ? 0 : Math.min(1, got / need);
        double l = Math.max(0.05, Clutch.larvaDays(k.species));
        double hFed = -Math.log(FED_SURVIVAL) / l, hUnfed = -Math.log(Math.max(1e-4, baseSurvival())) / l;
        double h = q * hFed + (1 - q) * hUnfed;
        double p = 1 - Math.exp(-h * days);
        int dead = 0;
        for (int i = 0; i < k.count; i++) if (r.nextDouble() < p) dead++;
        if (dead <= 0) return false;
        k.count -= dead;
        if (q < 0.5 && c.tankmateLogged.add("clown_larvae_hungry")) TankCare.log(level, c, "log.bettacraft.reef.larvae_hungry");
        if (q >= 0.5) c.tankmateLogged.remove("clown_larvae_hungry");
        return true;
    }

    // ------------------------------------------------------------------ feeding

    /** Rotifers or artemia nauplii into a reef tank: the larvae's food pool takes them while it has room. */
    public static boolean wantFood(TankData t, ItemStack food) {
        if (!active() || !t.marine()) return false;
        if (food.is(ModItems.ROTIFERS.get())) return t.rotifers() < CAP - 1;
        if (food.is(ModItems.ARTEMIA_NAUPLII.get())) return hasClownClutch(t) && t.nauplii() < CAP - 1;
        return false;
    }

    /** {@code items} of rotifers or nauplii into the pool (at most to the cap); returns the items taken. */
    public static int feed(TankData t, ItemStack food, int items) {
        if (items <= 0 || !wantFood(t, food)) return 0;
        boolean rot = food.is(ModItems.ROTIFERS.get());
        double have = rot ? t.rotifers() : t.nauplii();
        int n = (int) Math.max(1, Math.min(items, Math.ceil((CAP - have) / PER_ITEM)));
        double v = Math.min(CAP, have + n * PER_ITEM);
        if (rot) t.setRotifers(v);
        else t.setNauplii(v);
        return n;
    }

    /** A bottle of phytoplankton used on a reef tank: it greens the water and feeds the rotifers. */
    public static InteractionResult useOnTank(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!stack.is(ModItems.PHYTOPLANKTON.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        if (c == null) return InteractionResult.CONSUME;
        TankData t = c.getTank();
        if (!TankInhabitants.canModify(player, t)) {
            player.sendOverlayMessage(Component.translatable("message.bettacraft.tank.locked"));
            return InteractionResult.CONSUME;
        }
        if (!active()) return InteractionResult.CONSUME;
        if (!t.marine() || t.waterMb() <= 0) {
            player.sendOverlayMessage(Component.translatable("message.bettacraft.reef.phytoplankton_salt").withStyle(ChatFormatting.GOLD));
            return InteractionResult.CONSUME;
        }
        if (!dosePhytoplankton(t)) {
            player.sendOverlayMessage(Component.translatable("message.bettacraft.reef.phytoplankton_full").withStyle(ChatFormatting.GOLD));
            return InteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.getInventory().add(bottle)) player.drop(bottle, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8F, 1.0F);
        TankCare.log((ServerLevel) level, c, "log.bettacraft.reef.phytoplankton");
        c.markTankChanged();
        return InteractionResult.CONSUME;
    }

    /** One bottle of phytoplankton into the tank (false: the water is green enough). */
    public static boolean dosePhytoplankton(TankData t) {
        if (t.phytoplankton() >= CAP - 1) return false;
        t.setPhytoplankton(Math.min(CAP, t.phytoplankton() + PER_ITEM));
        return true;
    }
}
