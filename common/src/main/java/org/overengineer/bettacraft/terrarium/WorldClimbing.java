package org.overengineer.bettacraft.terrarium;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;

/**
 * M62: how our reptiles and amphibians climb outside a tank (switch {@code visuals.worldClimbing}). Vanilla mobs jump up a
 * one-block step; these animals climb it instead: they rise along the face with their legs moving and their belly to it (the
 * wall pose of the tank, {@link LandWalker.Pose}) and walk on over the edge.
 * <ul>
 *     <li>{@link Style#STEP}: turtles, the tortoise, the leopard gecko and the corn snake climb one-block steps (no wall: none of
 *     them has adhesive toes);</li>
 *     <li>{@link Style#WALL}: the crested gecko (adhesive toes, lives in trees) climbs whole walls up to their top and over, now
 *     and then only part of the way to cling there for a while and come down again ({@link HerpWorldGoals.WallClimb});</li>
 *     <li>{@link Style#ROOTS}: the mudskipper climbs roots and trunks (logs, mangrove roots) like the crested gecko; it hops up an
 *     ordinary step (it hops anyway);</li>
 *     <li>{@link Style#NONE}: the frogs hop (vanilla jump), tadpoles swim.</li>
 * </ul>
 * In water every animal swims as before. The state machine itself lives in {@link HerpEntity} (server); this class holds the
 * rules that GameTests and the goals share.
 */
public final class WorldClimbing {
    private WorldClimbing() {}

    public enum Style { NONE, STEP, WALL, ROOTS }

    /** Highest wall (blocks) one climb goes up. */
    public static final int MAX_WALL = 8;

    /** How a species climbs in the world (the switch off: {@link Style#NONE}). */
    public static Style style(HerpSpecies s) {
        if (!Features.enabled(Feature.WORLD_CLIMBING)) return Style.NONE;
        if (s == HerpSpecies.CRESTED_GECKO) return Style.WALL;
        if (s == HerpSpecies.MUDSKIPPER) return Style.ROOTS;
        if (s.body() == HerpSpecies.Body.FROG) return Style.NONE;
        return Style.STEP;
    }

    /** Climbing speed (blocks per tick) of a species: from its walking pace, 1 block in about 1.2 (geckos) to 2 s (tortoise). */
    public static double speed(HerpSpecies s) {
        return Mth.clamp(s.gait().walk() * 1.6, 0.025, 0.06);
    }

    /** A face it can climb: a block with a sturdy face towards the animal. */
    public static boolean wall(Level level, BlockPos pos, Direction towards) {
        BlockState st = level.getBlockState(pos);
        return !st.getCollisionShape(level, pos).isEmpty() && st.isFaceSturdy(level, pos, towards.getOpposite());
    }

    /** Roots and trunks (the mudskipper's walls). */
    public static boolean roots(Level level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        return st.is(BlockTags.LOGS) || st.is(Blocks.MANGROVE_ROOTS) || st.is(Blocks.MUDDY_MANGROVE_ROOTS);
    }

    /** Room for the animal (nothing to collide with). */
    public static boolean free(Level level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    /**
     * How many blocks the wall in front of {@code feet} (direction {@code d}) rises that a climber of this style can go up: 1 for
     * a step climber at a one-block step, the height of the climbable face (at most {@link #MAX_WALL}) for a wall climber, 0
     * when it cannot climb here (no wall, a step of two for a step climber, no room along the face).
     */
    public static int climbable(Level level, BlockPos feet, Direction d, Style style) {
        BlockPos front = feet.relative(d);
        if (style == Style.NONE || !wall(level, front, d)) return 0;
        boolean wallClimber = style == Style.WALL || style == Style.ROOTS && roots(level, front);
        if (!wallClimber) {
            if (style == Style.ROOTS) return 0; // the mudskipper hops up an ordinary step
            // one step: free above it and above the animal
            return wall(level, front.above(), d) || !free(level, front.above()) || !free(level, feet.above()) ? 0 : 1;
        }
        int k = 1;
        while (k < MAX_WALL) {
            BlockPos up = front.above(k);
            if (!free(level, feet.above(k))) return 0; // a ceiling over the face: no way up
            if (!wall(level, up, d) || style == Style.ROOTS && !roots(level, up)) break;
            k++;
        }
        // over the top: room on it
        if (!free(level, front.above(k)) || !free(level, feet.above(k))) return k == MAX_WALL ? k : 0;
        return k;
    }

    /** The direction of the face it walks into (from its motion, else its heading). */
    public static Direction facing(Vec3 motion, float yRot) {
        if (motion.horizontalDistanceSqr() > 1e-6) return Direction.getNearest(motion.x, 0, motion.z);
        return Direction.fromYRot(yRot);
    }

    /** A spot next to a climbable face within {@code r} blocks (nearest first), and the direction of the face, or null. */
    @Nullable
    public static Spot wallSpot(Level level, BlockPos at, int r, Style style, net.minecraft.util.RandomSource random) {
        Spot best = null;
        double bestD = Double.MAX_VALUE;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dy = -2; dy <= 2; dy++) for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            double d = dx * dx + dz * dz + dy * dy * 2 + random.nextDouble() * 0.5;
            if (d >= bestD) continue;
            p.set(at.getX() + dx, at.getY() + dy, at.getZ() + dz);
            if (!level.isLoaded(p) || !free(level, p) || !level.getFluidState(p).isEmpty() || !wall(level, p.below(), Direction.DOWN)) continue;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                int h = climbable(level, p, dir, style);
                if (h >= 2) {
                    best = new Spot(p.immutable(), dir, h);
                    bestD = d;
                    break;
                }
            }
        }
        return best;
    }

    /** A place at the foot of a wall: where it stands, the face's direction, the face's height. */
    public record Spot(BlockPos foot, Direction face, int height) {}
}
