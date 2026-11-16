package org.overengineer.bettacraft.tank;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The water of a tank as the set of wet tank blocks up to the surface (M10). Keeps fish, food, nests and spawned fry
 * inside the actual shape: in an L-shaped tank nothing is put into the notch, even though it lies inside the bounding
 * box. Margins keep things off the glass on every outer face (including concave walls) and below the surface.
 * <p>
 * The member set is shared with the {@link TankData} it was made from (tank data never changes its members; merges and
 * splits create new tank data), so creating a volume is cheap.
 */
public final class TankWaterVolume {
    private final LongSet members;
    private final double surface;
    private final AABB box;

    public TankWaterVolume(TankData tank) {
        this(tank.members(), tank.waterSurfaceY(), TankCare.waterBox(tank));
    }

    public TankWaterVolume(LongSet members, double surface, AABB box) {
        this.members = members;
        this.surface = surface;
        this.box = box;
    }

    /** Bounding box of the water (also covers the notches of a free shape). */
    public AABB box() {
        return box;
    }

    public double surface() {
        return surface;
    }

    /** A tank block with water in it (every block of a layer below the surface is wet). */
    public boolean wet(long pos) {
        return members.contains(pos) && BlockPos.getY(pos) < surface;
    }

    public boolean contains(Vec3 v) {
        return v.y <= surface && wet(BlockPos.asLong(Mth.floor(v.x), Mth.floor(v.y), Mth.floor(v.z)));
    }

    /**
     * The point of the water closest to {@code v}: inside a wet block, at least {@code side} from glass walls, {@code bottom}
     * above the floor and {@code top} below the surface (where the block holds the surface).
     */
    public Vec3 clamp(Vec3 v, double side, double bottom, double top) {
        return clamp(v, side, bottom, top, 0);
    }

    /**
     * Like {@link #clamp(Vec3, double, double, double)} for a body {@code height} high: its head also stays below the
     * glass lid of its column (the topmost tank block above it). An entity that starts inside the lid's collision box is
     * not held by it and slips out of the tank (M40 part c: the flaky squid of {@code aquaticspecies_0}: feet 0.3 below a
     * nearly full surface put the head of a 0.8 high squid through the lid of a 2 high tank).
     */
    public Vec3 clamp(Vec3 v, double side, double bottom, double top, double height) {
        double yy = Math.min(v.y, surface - 1e-4);
        // 1.33.3: a body taller than the distance to the surface (an angelfish's fin) must go down into the block below:
        // clamped inside its own top block it stuck at the block's floor with the fin above the surface
        long b = BlockPos.asLong(Mth.floor(v.x), Mth.floor(Math.min(yy, surface - top)), Mth.floor(v.z));
        if (!wet(b)) b = BlockPos.asLong(Mth.floor(v.x), Mth.floor(yy), Mth.floor(v.z));
        if (!wet(b)) b = nearestWet(v);
        if (b == Long.MIN_VALUE) return v;
        int x = BlockPos.getX(b), y = BlockPos.getY(b), z = BlockPos.getZ(b);
        double x0 = x + (members.contains(BlockPos.offset(b, Direction.WEST)) ? 0 : side);
        double x1 = x + 1 - (members.contains(BlockPos.offset(b, Direction.EAST)) ? 0 : side);
        double z0 = z + (members.contains(BlockPos.offset(b, Direction.NORTH)) ? 0 : side);
        double z1 = z + 1 - (members.contains(BlockPos.offset(b, Direction.SOUTH)) ? 0 : side);
        double y0 = y + (members.contains(BlockPos.offset(b, Direction.DOWN)) ? 0 : bottom);
        long up = BlockPos.offset(b, Direction.UP);
        double y1 = Math.min(wet(up) ? y + 1 : y + 1 - (members.contains(up) ? 0 : 1.0 / 16), surface - top);
        if (height > 0) {
            long c = b;
            while (members.contains(BlockPos.offset(c, Direction.UP))) c = BlockPos.offset(c, Direction.UP);
            y1 = Math.min(y1, BlockPos.getY(c) + 1 - 1.0 / 16 - 0.01 - height);
        }
        y1 = Math.max(y0, y1);
        return new Vec3(clamp(v.x, x0, x1), clamp(v.y, y0, y1), clamp(v.z, z0, z1));
    }

    /** A point just below the surface, near {@code near}, in a block that holds the surface (for the bubble nest). */
    public Vec3 surfacePoint(Vec3 near, double side, double below) {
        int layer = Mth.floor(surface - 1e-4);
        long best = Long.MIN_VALUE;
        double bestD = Double.MAX_VALUE;
        for (long l : members) {
            if (BlockPos.getY(l) != layer) continue;
            double d = distSqToColumn(l, near);
            if (d < bestD) {
                bestD = d;
                best = l;
            }
        }
        if (best == Long.MIN_VALUE) return clamp(near, side, 0.1, below);
        int x = BlockPos.getX(best), z = BlockPos.getZ(best);
        double x0 = x + (members.contains(BlockPos.offset(best, Direction.WEST)) ? 0 : side);
        double x1 = x + 1 - (members.contains(BlockPos.offset(best, Direction.EAST)) ? 0 : side);
        double z0 = z + (members.contains(BlockPos.offset(best, Direction.NORTH)) ? 0 : side);
        double z1 = z + 1 - (members.contains(BlockPos.offset(best, Direction.SOUTH)) ? 0 : side);
        return new Vec3(clamp(near.x, x0, x1), Math.max(layer + 0.1, surface - below), clamp(near.z, z0, z1));
    }

    private long nearestWet(Vec3 v) {
        long best = Long.MIN_VALUE;
        double bestD = Double.MAX_VALUE;
        for (long l : members) {
            int y = BlockPos.getY(l);
            if (y >= surface) continue;
            double top = Math.min(y + 1, surface);
            double dx = axis(v.x, BlockPos.getX(l), BlockPos.getX(l) + 1);
            double dy = axis(v.y, y, top);
            double dz = axis(v.z, BlockPos.getZ(l), BlockPos.getZ(l) + 1);
            double d = dx * dx + dy * dy + dz * dz;
            if (d < bestD) {
                bestD = d;
                best = l;
            }
        }
        return best;
    }

    private static double distSqToColumn(long l, Vec3 v) {
        double dx = axis(v.x, BlockPos.getX(l), BlockPos.getX(l) + 1);
        double dz = axis(v.z, BlockPos.getZ(l), BlockPos.getZ(l) + 1);
        return dx * dx + dz * dz;
    }

    private static double axis(double v, double lo, double hi) {
        return v < lo ? lo - v : v > hi ? v - hi : 0;
    }

    private static double clamp(double v, double lo, double hi) {
        return lo > hi ? (lo + hi) / 2 : Math.max(lo, Math.min(hi, v));
    }
}
