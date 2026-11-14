package org.overengineer.bettacraft.co2;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/** M25: the brew of a bio CO2 bottle ({@link BioCo2Block}): water, sugar, the start of the fermentation. */
public class BioCo2BlockEntity extends BlockEntity implements Co2Source {
    public static final int MAX_SUGAR = 4;
    private boolean water;
    private int sugar;
    /** Game time the yeast went in (-1: not fermenting). */
    private long start = -1;
    /** Days the brew ferments. */
    private double days;
    /** Bubbles per second in the last care step (for the status). */
    private double lastBubbles;

    public BioCo2BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIO_CO2.get(), pos, state);
    }

    // ------------------------------------------------------------------ pure model

    /**
     * Pure: how strong the fermentation is at {@code p} (0..1 of its time): it starts within the first sixth, holds and
     * fades over the last 40 %; 0 before and after.
     */
    public static double curve(double p) {
        if (p <= 0 || p >= 1) return 0;
        if (p < 0.15) return p / 0.15;
        if (p > 0.6) return (1 - p) / 0.4;
        return 1;
    }

    /** Pure: the uneven yeast: 0.7..1.3, changes every few hours (deterministic from the position and the time). */
    public static double unevenness(long seed, long gameTime) {
        long h = seed * 0x9E3779B97F4A7C15L + Math.floorDiv(gameTime, 4000L) * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return 0.7 + 0.6 * ((h >>> 11) / (double) (1L << 53));
    }

    /** Pure: days a brew with this much sugar ferments. */
    public static double daysFor(int sugar) {
        return ServerConfig.getOr(ServerConfig.CO2_BIO_DAYS, 4.0) * Math.max(1, Math.min(MAX_SUGAR, sugar)) / MAX_SUGAR;
    }

    // ------------------------------------------------------------------ state

    public boolean fermenting() {
        return start >= 0;
    }

    /** 0..1 of the fermentation time at this game time. */
    public double progress(long now) {
        if (start < 0 || days <= 0) return 0;
        return Math.max(0, (now - start) / (days * 24000.0));
    }

    /** Bubbles per second now. */
    public double bubblesAt(long now) {
        if (start < 0) return 0;
        return ServerConfig.getOr(ServerConfig.CO2_BIO_BUBBLES, 1.0) * curve(progress(now)) * unevenness(worldPosition.asLong(), now);
    }

    @Override
    public double bubbles(ServerLevel level, TankBlockEntity tank) {
        return bubblesAt(level.getGameTime());
    }

    @Override
    public void used(ServerLevel level, double bubbles, double days) {
        lastBubbles = bubbles;
        checkSpent(level);
    }

    private int stage() {
        if (start >= 0) return BioCo2Block.FERMENTING;
        if (water) return BioCo2Block.MIXED;
        return getBlockState().getValue(BioCo2Block.STAGE) == BioCo2Block.SPENT ? BioCo2Block.SPENT : BioCo2Block.EMPTY;
    }

    private void checkSpent(Level level) {
        if (start >= 0 && progress(level.getGameTime()) >= 1) {
            start = -1;
            water = false;
            sugar = 0;
            lastBubbles = 0;
            setStage(BioCo2Block.SPENT);
            setChanged();
        }
    }

    private void setStage(int stage) {
        if (level == null) return;
        BlockState s = getBlockState();
        if (s.getValue(BioCo2Block.STAGE) != stage) level.setBlock(worldPosition, s.setValue(BioCo2Block.STAGE, stage), Block.UPDATE_ALL);
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, BioCo2BlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 200 == 0) be.checkSpent(level);
    }

    static void clientTick(Level level, BlockPos pos, BlockState state, BioCo2BlockEntity be) {
        if (state.getValue(BioCo2Block.STAGE) != BioCo2Block.FERMENTING || level.random.nextInt(25) != 0) return;
        level.addParticle(net.minecraft.core.particles.ParticleTypes.BUBBLE_POP, pos.getX() + 0.4 + level.random.nextDouble() * 0.2,
                pos.getY() + 0.75, pos.getZ() + 0.4 + level.random.nextDouble() * 0.2, 0, 0.02, 0);
    }

    // ------------------------------------------------------------------ interaction

    public boolean accepts(ItemStack s) {
        return s.is(Items.WATER_BUCKET) || s.is(ModItems.RO_WATER_BUCKET.get()) || s.is(Items.SUGAR) || s.is(ModItems.YEAST.get());
    }

    /** Server: water, sugar or yeast from the hand. */
    public void use(Player player, InteractionHand hand, ItemStack stack) {
        if (level == null) return;
        if (stack.is(Items.WATER_BUCKET) || stack.is(ModItems.RO_WATER_BUCKET.get())) {
            if (start >= 0 || water) {
                msg(player, "message.bettacraft.bio_co2.has_water", ChatFormatting.GOLD);
                return;
            }
            water = true;
            sugar = 0;
            if (!player.getAbilities().instabuild) player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.8F, 1.2F);
            setStage(BioCo2Block.MIXED);
            msg(player, "message.bettacraft.bio_co2.water", ChatFormatting.AQUA);
        } else if (stack.is(Items.SUGAR)) {
            if (!water || start >= 0) {
                msg(player, start >= 0 ? "message.bettacraft.bio_co2.fermenting_already" : "message.bettacraft.bio_co2.needs_water", ChatFormatting.GOLD);
                return;
            }
            if (sugar >= MAX_SUGAR) {
                msg(player, "message.bettacraft.bio_co2.sugar_full", ChatFormatting.GOLD);
                return;
            }
            sugar++;
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.displayClientMessage(Component.translatable("message.bettacraft.bio_co2.sugar", sugar, MAX_SUGAR).withStyle(ChatFormatting.AQUA), true);
        } else if (stack.is(ModItems.YEAST.get())) {
            if (!water || sugar <= 0 || start >= 0) {
                msg(player, start >= 0 ? "message.bettacraft.bio_co2.fermenting_already" : !water ? "message.bettacraft.bio_co2.needs_water"
                        : "message.bettacraft.bio_co2.needs_sugar", ChatFormatting.GOLD);
                return;
            }
            start(level.getGameTime());
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.displayClientMessage(Component.translatable("message.bettacraft.bio_co2.started",
                    String.format(java.util.Locale.ROOT, "%.1f", days)).withStyle(ChatFormatting.GREEN), true);
        }
        setChanged();
    }

    /** Starts the fermentation now (also for the GameTests: a full brew with {@code sugar} sugar). */
    public void start(long now) {
        water = true;
        if (sugar <= 0) sugar = MAX_SUGAR;
        days = daysFor(sugar);
        start = now;
        setStage(BioCo2Block.FERMENTING);
        setChanged();
    }

    public void pourOut(Player player) {
        if (!water && start < 0 && stage() != BioCo2Block.SPENT) {
            tell(player);
            return;
        }
        water = false;
        sugar = 0;
        start = -1;
        lastBubbles = 0;
        setStage(BioCo2Block.EMPTY);
        setChanged();
        if (level != null) level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6F, 0.8F);
        msg(player, "message.bettacraft.bio_co2.poured", ChatFormatting.GRAY);
    }

    public void tell(Player player) {
        if (level == null) return;
        checkSpent(level);
        Component c;
        if (!TankCo2.enabled()) c = Component.translatable("message.bettacraft.co2.off").withStyle(ChatFormatting.GOLD);
        else if (start >= 0) {
            long now = level.getGameTime();
            double left = Math.max(0, (1 - progress(now)) * days);
            c = Component.translatable("message.bettacraft.bio_co2.status", String.format(java.util.Locale.ROOT, "%.1f", bubblesAt(now)),
                    String.format(java.util.Locale.ROOT, "%.1f", left)).withStyle(ChatFormatting.GREEN);
            if (org.overengineer.bettacraft.equipment.AirPumpBlockEntity.findTank(level, worldPosition) == null)
                c = Component.translatable("message.bettacraft.co2.no_tank").withStyle(ChatFormatting.GOLD);
        } else if (water) c = Component.translatable("message.bettacraft.bio_co2.mixed", sugar, MAX_SUGAR).withStyle(ChatFormatting.AQUA);
        else if (stage() == BioCo2Block.SPENT) c = Component.translatable("message.bettacraft.bio_co2.spent").withStyle(ChatFormatting.GOLD);
        else c = Component.translatable("message.bettacraft.bio_co2.empty").withStyle(ChatFormatting.GRAY);
        player.displayClientMessage(c, true);
    }

    private static void msg(Player p, String key, ChatFormatting f) {
        p.displayClientMessage(Component.translatable(key).withStyle(f), true);
    }

    public int sugar() {
        return sugar;
    }

    public double lastBubbles() {
        return lastBubbles;
    }

    // ------------------------------------------------------------------ save

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("water", water);
        tag.putInt("sugar", sugar);
        tag.putLong("start", start);
        tag.putDouble("days", days);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water = tag.getBoolean("water");
        sugar = Math.max(0, Math.min(MAX_SUGAR, tag.getInt("sugar")));
        start = tag.contains("start") ? tag.getLong("start") : -1;
        days = tag.getDouble("days");
    }
}
