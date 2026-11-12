package org.overengineer.bettacraft.tank;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Pure geometry of free tank shapes (M10): any face-connected set of tank blocks. No world access, unit-tested
 * ({@code TankShapeTest}). Positions are packed {@link BlockPos#asLong} values.
 */
public final class TankShape {
    private TankShape() {}

    // ------------------------------------------------------------------ bounds / connectivity

    /** {minX, minY, minZ, maxX, maxY, maxZ}; all zero for an empty set. */
    public static int[] bounds(LongSet set) {
        if (set.isEmpty()) return new int[6];
        int[] b = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        for (long l : set) {
            int x = BlockPos.getX(l), y = BlockPos.getY(l), z = BlockPos.getZ(l);
            b[0] = Math.min(b[0], x); b[1] = Math.min(b[1], y); b[2] = Math.min(b[2], z);
            b[3] = Math.max(b[3], x); b[4] = Math.max(b[4], y); b[5] = Math.max(b[5], z);
        }
        return b;
    }

    /** Whether the bounding box of the set fits into the size limits. */
    public static boolean fits(int[] bounds, int maxX, int maxY, int maxZ) {
        return bounds[3] - bounds[0] + 1 <= maxX && bounds[4] - bounds[1] + 1 <= maxY && bounds[5] - bounds[2] + 1 <= maxZ;
    }

    public static boolean isCuboid(LongSet set) {
        if (set.isEmpty()) return false;
        int[] b = bounds(set);
        return (long) (b[3] - b[0] + 1) * (b[4] - b[1] + 1) * (b[5] - b[2] + 1) == set.size();
    }

    /** Face-connected components, each sorted set ordered by its lowest block (y, z, x); deterministic. */
    public static List<LongSet> components(LongSet input) {
        LongSet left = new LongOpenHashSet(input);
        List<Long> sorted = new ArrayList<>(input);
        sorted.sort(Comparator.comparingInt((Long l) -> BlockPos.getY(l)).thenComparingInt(l -> BlockPos.getZ(l)).thenComparingInt(l -> BlockPos.getX(l)));
        List<LongSet> out = new ArrayList<>();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        for (long start : sorted) {
            if (!left.remove(start)) continue;
            LongSet comp = new LongOpenHashSet();
            comp.add(start);
            queue.enqueue(start);
            while (!queue.isEmpty()) {
                long p = queue.dequeueLong();
                for (Direction d : Direction.values()) {
                    long n = BlockPos.offset(p, d);
                    if (left.remove(n)) {
                        comp.add(n);
                        queue.enqueue(n);
                    }
                }
            }
            out.add(comp);
        }
        return out;
    }

    public static boolean isConnected(LongSet set) {
        return !set.isEmpty() && components(set).size() == 1;
    }

    // ------------------------------------------------------------------ water

    /** Blocks per layer, index 0 = {@code minY}. */
    public static int[] layerCounts(LongSet set, int minY, int maxY) {
        int[] c = new int[Math.max(0, maxY - minY + 1)];
        for (long l : set) {
            int i = BlockPos.getY(l) - minY;
            if (i >= 0 && i < c.length) c[i]++;
        }
        return c;
    }

    /** Water (mB) in each layer when {@code waterMb} fills the shape bottom-up, layer by layer. */
    public static long[] layerWater(int[] layerCounts, long waterMb) {
        long[] w = new long[layerCounts.length];
        long left = Math.max(0, waterMb);
        for (int i = 0; i < w.length && left > 0; i++) {
            long cap = layerCounts[i] * TankData.MB_PER_BLOCK;
            w[i] = Math.min(cap, left);
            left -= w[i];
        }
        return w;
    }

    /**
     * Absolute height of the water surface (bottom of the shape if dry). Each layer fills completely before the next,
     * so the surface is flat over every block of the topmost wet layer.
     */
    public static double surfaceY(int[] layerCounts, int minY, long waterMb) {
        return surfaceY(layerCounts, minY, waterMb, 0);
    }

    /** Like {@link #surfaceY(int[], int, long)}; the top layer only fills up to 16 - {@code headroom} sixteenths. */
    public static double surfaceY(int[] layerCounts, int minY, long waterMb, int headroom) {
        long left = Math.max(0, waterMb);
        double top = 1 - headroom / 16.0;
        for (int i = 0; i < layerCounts.length; i++) {
            long cap = layerCounts[i] * TankData.MB_PER_BLOCK;
            if (cap <= 0) continue;
            double h = i == layerCounts.length - 1 ? top : 1;
            if (left < cap) return minY + i + h * left / (double) cap;
            left -= cap;
        }
        return minY + layerCounts.length - 1 + top;
    }

    /**
     * Splits the water of {@code old} among the {@code pieces} that remain after removing blocks: every piece keeps the
     * water of its own blocks (each block of a layer holds the same amount), the removed blocks' water is gone.
     * Rounding never creates water; the shares add up to the rounded total kept.
     */
    public static long[] splitWater(LongSet old, long waterMb, List<? extends Collection<Long>> pieces) {
        int[] b = bounds(old);
        int[] counts = layerCounts(old, b[1], b[4]);
        long[] layer = layerWater(counts, waterMb);
        double[] exact = new double[pieces.size()];
        double total = 0;
        for (int i = 0; i < pieces.size(); i++) {
            for (long l : pieces.get(i)) {
                int y = BlockPos.getY(l) - b[1];
                if (y >= 0 && y < counts.length && counts[y] > 0) exact[i] += layer[y] / (double) counts[y];
            }
            total += exact[i];
        }
        long kept = Math.min(Math.max(0, waterMb), Math.round(total));
        long[] out = new long[pieces.size()];
        long sum = 0;
        for (int i = 0; i < out.length; i++) {
            out[i] = (long) Math.floor(exact[i] + 1e-9);
            sum += out[i];
        }
        // hand out the rounding rest to the pieces with the largest fractional part
        while (sum < kept) {
            int best = -1;
            double frac = -1;
            for (int i = 0; i < out.length; i++) {
                double f = exact[i] - out[i];
                if (f > frac && out[i] < pieces.get(i).size() * TankData.MB_PER_BLOCK) {
                    frac = f;
                    best = i;
                }
            }
            if (best < 0) break;
            out[best]++;
            sum++;
        }
        return out;
    }

    /** Positions directly above every block whose upper neighbour is not part of the shape (open tops). */
    public static List<BlockPos> openTops(LongSet set) {
        List<BlockPos> out = new ArrayList<>();
        for (long l : set) {
            long up = BlockPos.offset(l, Direction.UP);
            if (!set.contains(up)) out.add(BlockPos.of(up));
        }
        out.sort(Comparator.comparingInt((BlockPos p) -> p.getY()).thenComparingInt(p -> p.getZ()).thenComparingInt(p -> p.getX()));
        return out;
    }

    /**
     * Open tops (see {@link #openTops}) the water can evaporate through: the block below is wet ({@code y <= surfaceLayer})
     * or a continuous column of tank blocks leads down from it to the surface layer. For a cuboid that is the whole top.
     */
    public static List<BlockPos> exposedTops(LongSet set, int surfaceLayer) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos top : openTops(set)) {
            long p = BlockPos.offset(top.asLong(), Direction.DOWN);
            boolean reaches = false;
            for (int i = 0; i < 4096 && set.contains(p); i++) {
                if (BlockPos.getY(p) <= surfaceLayer) {
                    reaches = true;
                    break;
                }
                p = BlockPos.offset(p, Direction.DOWN);
            }
            if (reaches) out.add(top);
        }
        return out;
    }

    /** Blocks with an outer face towards {@code d} (the neighbour in that direction is not part of the shape). */
    public static List<BlockPos> outerFaces(LongSet set, Direction d) {
        List<BlockPos> out = new ArrayList<>();
        for (long l : set) if (!set.contains(BlockPos.offset(l, d))) out.add(BlockPos.of(l));
        return out;
    }

    /** Walks from {@code start} in direction {@code d} while the next block is part of the shape; returns the last one. */
    public static BlockPos walk(LongSet set, BlockPos start, Direction d) {
        long p = start.asLong();
        for (int i = 0; i < 4096 && set.contains(BlockPos.offset(p, d)); i++) p = BlockPos.offset(p, d);
        return BlockPos.of(p);
    }

    // ------------------------------------------------------------------ name sign

    /**
     * The flat part of the front where the sign sits: one block row ({@code y}), the glass plane ({@code plane}, a world
     * coordinate on the front axis) and the lateral run of blocks [{@code latMin}, {@code latMax}] (world coordinates on
     * the other horizontal axis).
     */
    public record SignFace(int y, double plane, int latMin, int latMax) {
        public int width() {
            return latMax - latMin + 1;
        }

        public double centre() {
            return (latMin + latMax + 1) / 2.0;
        }
    }

    /**
     * Picks the face for the name sign on an irregular front: the top (or bottom) row of the shape; per lateral
     * position its frontmost block; the longest run of neighbouring positions whose front faces lie in one plane
     * (ties: the one further to the front, then the one nearer to the middle). For a cuboid that is the whole front.
     */
    @Nullable
    public static SignFace signFace(LongSet set, Direction front, boolean top) {
        if (set.isEmpty() || !front.getAxis().isHorizontal()) return null;
        int[] b = bounds(set);
        int row = top ? b[4] : b[1];
        boolean latIsX = front.getAxis() == Direction.Axis.Z;
        int latLo = latIsX ? b[0] : b[2], latHi = latIsX ? b[3] : b[5];
        int step = front.getAxisDirection().getStep();
        int n = latHi - latLo + 1;
        long[] depth = new long[n];
        java.util.Arrays.fill(depth, Long.MIN_VALUE);
        for (long l : set) {
            if (BlockPos.getY(l) != row) continue;
            int lat = latIsX ? BlockPos.getX(l) : BlockPos.getZ(l);
            int dep = (latIsX ? BlockPos.getZ(l) : BlockPos.getX(l)) * step;
            depth[lat - latLo] = Math.max(depth[lat - latLo], dep);
        }
        double mid = (latLo + latHi + 1) / 2.0;
        int bestStart = -1, bestLen = 0;
        long bestDepth = Long.MIN_VALUE;
        double bestMid = Double.MAX_VALUE;
        int i = 0;
        while (i < n) {
            if (depth[i] == Long.MIN_VALUE) {
                i++;
                continue;
            }
            int j = i;
            while (j + 1 < n && depth[j + 1] == depth[i]) j++;
            int len = j - i + 1;
            double m = Math.abs((latLo + i + latLo + j + 1) / 2.0 - mid);
            if (len > bestLen || (len == bestLen && (depth[i] > bestDepth || (depth[i] == bestDepth && m < bestMid)))) {
                bestStart = i;
                bestLen = len;
                bestDepth = depth[i];
                bestMid = m;
            }
            i = j + 1;
        }
        if (bestStart < 0) return null;
        long blockCoord = bestDepth * step; // back to the world coordinate of the frontmost blocks
        double plane = step > 0 ? blockCoord + 1 : blockCoord;
        return new SignFace(row, plane, latLo + bestStart, latLo + bestStart + bestLen - 1);
    }

    // ------------------------------------------------------------------ compact encoding (client view, regions)

    /** Bit set over the box (index ((y * sz) + z) * sx + x, relative to min). */
    public static byte[] encode(LongSet set, int minX, int minY, int minZ, int sx, int sy, int sz) {
        byte[] bits = new byte[(sx * sy * sz + 7) / 8];
        for (long l : set) {
            int x = BlockPos.getX(l) - minX, y = BlockPos.getY(l) - minY, z = BlockPos.getZ(l) - minZ;
            if (x < 0 || y < 0 || z < 0 || x >= sx || y >= sy || z >= sz) continue;
            int i = (y * sz + z) * sx + x;
            bits[i >> 3] |= (byte) (1 << (i & 7));
        }
        return bits;
    }

    public static LongSet decode(byte[] bits, int minX, int minY, int minZ, int sx, int sy, int sz) {
        LongSet out = new LongOpenHashSet();
        for (int y = 0; y < sy; y++)
            for (int z = 0; z < sz; z++)
                for (int x = 0; x < sx; x++) {
                    int i = (y * sz + z) * sx + x;
                    if ((i >> 3) < bits.length && (bits[i >> 3] & (1 << (i & 7))) != 0) out.add(BlockPos.asLong(minX + x, minY + y, minZ + z));
                }
        return out;
    }

    /** A full box as a bit set (all ones). */
    public static byte[] fullBox(int sx, int sy, int sz) {
        int n = sx * sy * sz;
        byte[] bits = new byte[(n + 7) / 8];
        for (int i = 0; i < n; i++) bits[i >> 3] |= (byte) (1 << (i & 7));
        return bits;
    }

    public static boolean bit(byte[] bits, int sx, int sz, int x, int y, int z) {
        int i = (y * sz + z) * sx + x;
        return (i >> 3) < bits.length && (bits[i >> 3] & (1 << (i & 7))) != 0;
    }
}
