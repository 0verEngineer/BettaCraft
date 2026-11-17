package org.overengineer.bettacraft.tankmates;

import net.minecraft.world.InteractionResult;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankWater;

/**
 * M49 part b: the archerfish ({@link TankmateSpecies#ARCHERFISH}) shoots insects above the water down with a jet of water
 * (switch {@code features.brackish.archerfish}). The insects are a pool of the tank ({@link TankData#flies()}): fruit flies
 * let out over an open tank with an archerfish go there instead of being eaten at once ({@link #release}), and by day insects
 * come to the plants above an open top (leaves, flowers or saplings on top of the tank, a mangrove crown over the water, the
 * land of a paludarium; {@code brackish.archerfishInsectsPerDay}). Every second an archerfish may shoot one: a line of water
 * particles from its mouth to the insect, the insect falls and is its meal. Now and then it splashes a player whose head is
 * over its open tank ({@code brackish.archerfishSplashChance}; harmless, it only puts out a fire).
 */
public final class Archerfish {
    /** Fruit flies one item lets out. */
    public static final int FLIES_PER_ITEM = 6;
    /** Most insects above one tank. */
    public static final double MAX_FLIES = 24;
    /** Chance per second that an archerfish shoots while there are insects. */
    public static final double SHOT_CHANCE = 0.35;

    private Archerfish() {}

    public static boolean enabled() {
        return Features.enabled(Feature.ARCHERFISH);
    }

    public static List<TankmateEntity> archers(ServerLevel level, TankData t) {
        List<TankmateEntity> out = new ArrayList<>();
        for (TankmateEntity m : TankmateBreeding.tankmates(level, t)) if (m.species() == TankmateSpecies.ARCHERFISH && m.lifeStage().ordinal() >= org.overengineer.bettacraft.entity.betta.LifeStage.JUVENILE.ordinal()) out.add(m);
        return out;
    }

    /** The open tops of the tank (nothing covers them: no lid, light, feeder or full block). */
    public static List<BlockPos> openTops(ServerLevel level, TankData t) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : t.openTops()) if (level.isLoaded(p) && !TankWater.covers(level, p)) out.add(p);
        return out;
    }

    /** Pure: a block on top of a tank where insects gather (leaves, flowers, saplings, crops, vines). */
    public static boolean insectPlant(BlockState s) {
        return s.is(BlockTags.LEAVES) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS) || s.is(BlockTags.CROPS)
                || s.is(net.minecraft.world.level.block.Blocks.VINE) || s.is(BlockTags.CAVE_VINES);
    }

    /** Places where insects come to above this tank: plants on the open tops, mangrove crowns over the water, a paludarium's land. */
    public static int insectSpots(ServerLevel level, TankBlockEntity c, List<BlockPos> tops) {
        int n = 0;
        for (BlockPos p : tops) if (insectPlant(level.getBlockState(p)) || insectPlant(level.getBlockState(p.above()))) n++;
        n += org.overengineer.bettacraft.plants.Mangroves.crowns(c.getTank());
        if (c.getTank().terrariumMode().hasLand()) n++;
        return n;
    }

    /**
     * Fruit flies on a tank with an archerfish and an open top: let out over the water for the archerfish to shoot. Returns
     * the items used (0: not for the archerfish, feed as usual).
     */
    public static int release(ServerLevel level, TankBlockEntity c, ItemStack food, int maxItems) {
        if (!enabled() || maxItems <= 0 || !food.is(ModTags.SMALL_FEEDER_INSECTS)) return 0;
        TankData t = c.getTank();
        if (t.waterMb() <= 0 || archers(level, t).isEmpty() || openTops(level, t).isEmpty()) return 0;
        if (t.flies() >= MAX_FLIES - 1e-6) return 0;
        t.setFlies(Math.min(MAX_FLIES, t.flies() + FLIES_PER_ITEM));
        c.setChanged();
        TankCare.log(level, c, "log.bettacraft.archerfish.flies", String.valueOf(FLIES_PER_ITEM));
        return 1;
    }

    /** Fruit flies in the hand on a tank with an archerfish and an open top: one item let out over the water. */
    public static net.minecraft.world.InteractionResult useOnTank(ItemStack stack, net.minecraft.world.level.Level level, BlockPos pos,
                                                                     net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        if (!enabled() || !stack.is(ModTags.SMALL_FEEDER_INSECTS)) return net.minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server)) return net.minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND;
        TankBlockEntity c = org.overengineer.bettacraft.tank.TankMultiblock.getController(level, pos);
        if (c == null || release(server, c, stack, 1) <= 0) return net.minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.displayClientMessage(Component.translatable("message.bettacraft.archerfish.flies").withStyle(ChatFormatting.GREEN), true);
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    /** The care step (every second): insects come to the plants, the archerfish shoot, now and then a player is splashed. */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        if (!enabled() || days <= 0) return;
        TankData t = c.getTank();
        if (t.waterMb() <= 0) return;
        List<TankmateEntity> archers = archers(level, t);
        if (archers.isEmpty()) return;
        List<BlockPos> tops = openTops(level, t);
        if (tops.isEmpty()) return;
        var rnd = level.getRandom();
        // insects come to the plants above the open tank by day
        if (level.isBrightOutside()) {
            int spots = insectSpots(level, c, tops);
            if (spots > 0) {
                double rate = ServerConfig.getOr(ServerConfig.ARCHER_INSECTS, 6.0) * Math.min(1.0, spots / 2.0);
                double expected = rate * days;
                if (rnd.nextDouble() < expected - Math.floor(expected)) expected = Math.floor(expected) + 1;
                if ((int) expected > 0) t.setFlies(Math.min(MAX_FLIES, t.flies() + (int) expected));
            }
        }
        // the shots (at most one per archerfish and step)
        for (TankmateEntity a : archers) {
            if (t.flies() < 1) break;
            if (rnd.nextDouble() >= SHOT_CHANCE * Math.min(1.0, days * 1200)) continue; // one try per second of care
            BlockPos top = tops.get(rnd.nextInt(tops.size()));
            Vec3 target = new Vec3(top.getX() + 0.2 + rnd.nextDouble() * 0.6, top.getY() + 0.3 + rnd.nextDouble() * 0.7, top.getZ() + 0.2 + rnd.nextDouble() * 0.6);
            shoot(level, c, a, target);
        }
        // a player leaning over the tank
        double chance = ServerConfig.getOr(ServerConfig.ARCHER_SPLASH, 0.02) * Math.min(1.0, days * 1200);
        if (chance > 0) {
            for (ServerPlayer p : level.players()) {
                if (p.isSpectator() || !leansOver(p, tops)) continue;
                if (rnd.nextDouble() >= chance) continue;
                splash(level, c, archers.get(rnd.nextInt(archers.size())), p);
                break;
            }
        }
    }

    /** One shot at an insect above the water: the jet, the fall, the meal. */
    public static void shoot(ServerLevel level, TankBlockEntity c, TankmateEntity a, Vec3 target) {
        TankData t = c.getTank();
        jet(level, mouth(a), target);
        level.playSound(null, target.x, target.y, target.z, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.25F, 1.6F + level.getRandom().nextFloat() * 0.3F);
        t.setFlies(t.flies() - 1);
        a.health().feed();
        c.setChanged();
        org.overengineer.bettacraft.progression.Progress.awardNear(level, c, org.overengineer.bettacraft.progression.Progress.ARCHER_SHOT, 0);
    }

    /** Splashes the player's face: water drops, a sound and a short message (a burning player is put out). */
    public static void splash(ServerLevel level, TankBlockEntity c, TankmateEntity a, ServerPlayer p) {
        Vec3 face = p.getEyePosition();
        jet(level, mouth(a), face);
        level.sendParticles(ParticleTypes.SPLASH, face.x, face.y, face.z, 18, 0.2, 0.15, 0.2, 0.1);
        level.playSound(null, face.x, face.y, face.z, SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 0.35F, 1.5F);
        if (p.isOnFire()) p.clearFire();
        p.displayClientMessage(Component.translatable("message.bettacraft.archerfish.splash").withStyle(ChatFormatting.AQUA), true);
        org.overengineer.bettacraft.progression.Progress.award(p, org.overengineer.bettacraft.progression.Progress.ARCHER_SPLASH);
    }

    /** Whether the player's head is over an open top of the tank (up to three blocks above it). */
    public static boolean leansOver(ServerPlayer p, List<BlockPos> tops) {
        Vec3 e = p.getEyePosition();
        for (BlockPos top : tops) {
            if (e.x >= top.getX() - 0.1 && e.x <= top.getX() + 1.1 && e.z >= top.getZ() - 0.1 && e.z <= top.getZ() + 1.1
                    && e.y >= top.getY() && e.y <= top.getY() + 3.0) return true;
        }
        return false;
    }

    private static Vec3 mouth(TankmateEntity a) {
        Vec3 look = Vec3.directionFromRotation(0, a.getYRot());
        return a.position().add(look.scale(a.getBbWidth() * 0.5)).add(0, a.getBbHeight() * 0.6, 0);
    }

    /** The pale blue of the jet (small dust particles: they stay where they are for a moment, in the water and in the air). */
    private static final net.minecraft.core.particles.DustParticleOptions JET = new net.minecraft.core.particles.DustParticleOptions(
            net.minecraft.util.ARGB.colorFromFloat(1.0F, 0.62F, 0.84F, 1.0F), 0.7F);

    /** A thin line of water particles from the mouth to the target, a splash where it hits. */
    static void jet(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        int n = Math.max(8, (int) Math.ceil(d.length() * 14));
        for (int i = 1; i <= n; i++) {
            Vec3 p = from.add(d.scale(i / (double) n));
            level.sendParticles(JET, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        level.sendParticles(ParticleTypes.SPLASH, to.x, to.y, to.z, 6, 0.06, 0.04, 0.06, 0.0);
        level.sendParticles(ParticleTypes.FALLING_WATER, to.x, to.y, to.z, 3, 0.05, 0.05, 0.05, 0.0);
    }

    /** The area above the tank where its insects fly (for the client and the tests). */
    public static AABB above(TankData t) {
        BlockPos min = t.min();
        return new AABB(min.getX(), t.max().getY() + 1, min.getZ(), t.max().getX() + 1, t.max().getY() + 2, t.max().getZ() + 1);
    }
}
