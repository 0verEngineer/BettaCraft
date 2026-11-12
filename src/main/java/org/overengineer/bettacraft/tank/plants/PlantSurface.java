package org.overengineer.bettacraft.tank.plants;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;

/**
 * The water surface of a tank as a set of block columns (M12, pure, unit-tested): the tank blocks of the layer that
 * holds the surface. Plants only float over these columns, never over a notch of a free shape or over a lower step
 * whose water is closed off by the glass above it. Margins keep the leaves inside the glass, also at concave corners.
 */
public final class PlantSurface {
    private final LongSet columns = new LongOpenHashSet();
    private final int layer;
    private int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

    public PlantSurface(int layer) {
        this.layer = layer;
    }

    /** The surface of the tank blocks {@code members} in layer {@code layer}. */
    public static PlantSurface of(LongSet members, int layer) {
        PlantSurface s = new PlantSurface(layer);
        for (long l : members) if (BlockPos.getY(l) == layer) s.add(BlockPos.getX(l), BlockPos.getZ(l));
        return s;
    }

    public PlantSurface add(int x, int z) {
        columns.add(key(x, z));
        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);
        minZ = Math.min(minZ, z);
        maxZ = Math.max(maxZ, z);
        return this;
    }

    static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public int layer() { return layer; }

    /** Surface area in blocks (= number of columns). */
    public int area() { return columns.size(); }

    public boolean isEmpty() { return columns.isEmpty(); }

    public boolean hasColumn(int x, int z) {
        return columns.contains(key(x, z));
    }

    public boolean contains(double x, double z) {
        return hasColumn((int) Math.floor(x), (int) Math.floor(z));
    }

    /**
     * The point of the surface closest to (x, z) that keeps {@code margin} from the glass: inside a surface column,
     * {@code margin} from each side without a neighbouring column and out of the corner of a missing diagonal column.
     */
    public double[] clamp(double x, double z, double margin) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        if (!hasColumn(bx, bz)) {
            long best = nearest(x, z);
            if (best == Long.MIN_VALUE) return new double[]{x, z};
            bx = (int) (best >> 32);
            bz = (int) best;
        }
        double m = Math.max(0, Math.min(0.49, margin));
        double x0 = bx + (hasColumn(bx - 1, bz) ? 0 : m), x1 = bx + 1 - (hasColumn(bx + 1, bz) ? 0 : m);
        double z0 = bz + (hasColumn(bx, bz - 1) ? 0 : m), z1 = bz + 1 - (hasColumn(bx, bz + 1) ? 0 : m);
        double cx = clampAxis(x, x0, x1), cz = clampAxis(z, z0, z1);
        // concave corner: both side neighbours exist but the diagonal one does not (inside corner of an L)
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                if (!hasColumn(bx + dx, bz) || !hasColumn(bx, bz + dz) || hasColumn(bx + dx, bz + dz)) continue;
                double ex = dx > 0 ? bx + 1 - cx : cx - bx; // distance to the corner lines
                double ez = dz > 0 ? bz + 1 - cz : cz - bz;
                if (ex < m && ez < m) {
                    // push out along the shorter way (the axis that is already closer to the margin)
                    if (ex >= ez) cx = dx > 0 ? bx + 1 - m : bx + m;
                    else cz = dz > 0 ? bz + 1 - m : bz + m;
                }
            }
        }
        return new double[]{cx, cz};
    }

    /** Column key closest to (x, z), or {@link Long#MIN_VALUE} if the surface is empty. */
    private long nearest(double x, double z) {
        long best = Long.MIN_VALUE;
        double bestD = Double.MAX_VALUE;
        for (long k : columns) {
            int cx = (int) (k >> 32), cz = (int) k;
            double dx = axis(x, cx, cx + 1), dz = axis(z, cz, cz + 1);
            double d = dx * dx + dz * dz;
            if (d < bestD || (d == bestD && k < best)) {
                bestD = d;
                best = k;
            }
        }
        return best;
    }

    /** A random point of the surface (uniform over the columns), {@code margin} inside the glass. */
    public double[] randomPoint(java.util.function.DoubleSupplier rnd, double margin) {
        if (columns.isEmpty()) return new double[]{0, 0};
        long[] ks = columns.toLongArray();
        java.util.Arrays.sort(ks);
        long k = ks[Math.min(ks.length - 1, (int) (rnd.getAsDouble() * ks.length))];
        double x = (int) (k >> 32) + rnd.getAsDouble(), z = (int) k + rnd.getAsDouble();
        return clamp(x, z, margin);
    }

    public int minX() { return minX; }
    public int minZ() { return minZ; }
    public int maxX() { return maxX; }
    public int maxZ() { return maxZ; }

    private static double axis(double v, double lo, double hi) {
        return v < lo ? lo - v : v > hi ? v - hi : 0;
    }

    private static double clampAxis(double v, double lo, double hi) {
        return lo > hi ? (lo + hi) / 2 : Math.max(lo, Math.min(hi, v));
    }
}
