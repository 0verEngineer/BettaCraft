package org.overengineer.bettacraft.client.interior;

import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.plants.PlantPart;
import org.overengineer.bettacraft.plants.PlantShapes;
import org.overengineer.bettacraft.plants.PlantSpecies;

/**
 * M22.1: the meshes of every species, built from every growth form the generator makes (scales, water depths, seeds,
 * half grown, full grown, with blocks missing): every piece hangs together with the plant down to its base on the floor
 * or the rock (no leaf, polyp or tentacle floating in the water), all vertices finite and near the plant's blocks.
 */
class PlantMeshBuilderTest {
    private static final int[] SCALES = {16, 8, 4};
    private static final int[] HEADROOMS = {6, 12, 20, 31};

    /** The blocks the renderer sees: the root and the blocks connected to it (26-neighbourhood), keyed like the renderer. */
    private static Long2ObjectOpenHashMap<PlantPart> blocks(List<PlantShapes.Voxel> vs, Random drop, double dropShare) {
        Long2ObjectOpenHashMap<PlantPart> all = new Long2ObjectOpenHashMap<>();
        all.put(BlockPos.asLong(0, 0, 0), PlantPart.ROOT);
        for (PlantShapes.Voxel v : vs) {
            if (v.part() == PlantPart.ROOT) continue;
            if (drop != null && drop.nextDouble() < dropShare) continue;
            all.put(BlockPos.asLong(v.x(), v.y(), v.z()), v.part());
        }
        Long2ObjectOpenHashMap<PlantPart> out = new Long2ObjectOpenHashMap<>();
        ArrayDeque<Long> q = new ArrayDeque<>();
        q.add(BlockPos.asLong(0, 0, 0));
        out.put(BlockPos.asLong(0, 0, 0), PlantPart.ROOT);
        while (!q.isEmpty()) {
            long c = q.poll();
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++)
                    for (int dx = -1; dx <= 1; dx++) {
                        long n = BlockPos.asLong(BlockPos.getX(c) + dx, BlockPos.getY(c) + dy, BlockPos.getZ(c) + dz);
                        if (all.containsKey(n) && !out.containsKey(n)) {
                            out.put(n, all.get(n));
                            q.add(n);
                        }
                    }
        }
        return out;
    }

    private static PlantMeshRenderer.Mesh build(PlantSpecies sp, Long2ObjectOpenHashMap<PlantPart> parts, PlantMeshRenderer.Style style) {
        PlantMeshBuilder b = new PlantMeshBuilder(BlockPos.ZERO, sp, parts, new Long2IntOpenHashMap(), style);
        b.build();
        return b.m;
    }

    /**
     * Checks one mesh: quads touching each other (any corner, edge middle or centre within half a block) are one piece;
     * every piece has to reach down to the lowest layer of the plant (the floor, the rock, the rhizome).
     */
    private static void check(PlantSpecies sp, Long2ObjectOpenHashMap<PlantPart> parts, PlantMeshRenderer.Mesh m, String what) {
        int minY = Integer.MAX_VALUE;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (long k : parts.keySet()) {
            minX = Math.min(minX, BlockPos.getX(k));
            maxX = Math.max(maxX, BlockPos.getX(k));
            minY = Math.min(minY, BlockPos.getY(k));
            maxY = Math.max(maxY, BlockPos.getY(k));
            minZ = Math.min(minZ, BlockPos.getZ(k));
            maxZ = Math.max(maxZ, BlockPos.getZ(k));
        }
        assertTrue(m.quads > 0, what + ": no quads");
        // all quads of all layers: 4 corners each
        List<float[]> quads = new java.util.ArrayList<>();
        for (PlantMeshRenderer.Layer l : m.layers) {
            float[] v = l.v.elements();
            for (int q = 0; q < l.light.size(); q++) {
                float[] c = new float[12];
                for (int k = 0; k < 4; k++) {
                    int o = (q * 4 + k) * PlantMeshRenderer.STRIDE;
                    for (int j = 0; j < 3; j++) {
                        float f = v[o + j];
                        assertTrue(Float.isFinite(f), what + ": vertex not finite");
                        c[k * 3 + j] = f;
                    }
                    float x = c[k * 3], y = c[k * 3 + 1], z = c[k * 3 + 2];
                    float mg = 4 + 0.3F * Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
                    assertTrue(x > minX - mg && x < maxX + 1 + mg && y > minY - 2 && y < maxY + 1 + mg && z > minZ - mg && z < maxZ + 1 + mg,
                            what + ": vertex far from the plant (" + x + ", " + y + ", " + z + ")");
                }
                quads.add(c);
            }
        }
        // union-find over a grid of half blocks: the corners, edge middles and the centre of every quad mark their cell
        int n = quads.size();
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        Long2IntOpenHashMap cell = new Long2IntOpenHashMap();
        cell.defaultReturnValue(-1);
        for (int i = 0; i < n; i++) {
            float[] c = quads.get(i);
            for (float[] p : points(c)) {
                int gx = (int) Math.floor(p[0] * 2), gy = (int) Math.floor(p[1] * 2), gz = (int) Math.floor(p[2] * 2);
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++)
                        for (int dx = -1; dx <= 1; dx++) {
                            int other = cell.get(BlockPos.asLong(gx + dx, gy + dy, gz + dz));
                            if (other >= 0) union(parent, i, other);
                        }
                cell.put(BlockPos.asLong(gx, gy, gz), i);
            }
        }
        // a mat of polyps lies on the rock under each of its columns (it grows over it, down from the root's column)
        boolean mat = sp.form() == PlantSpecies.Form.POLYPS;
        it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap bottom = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
        for (long k : parts.keySet()) {
            long ck = PlantMeshBuilder.col(BlockPos.getX(k), BlockPos.getZ(k));
            bottom.put(ck, Math.min(bottom.getOrDefault(ck, Integer.MAX_VALUE), BlockPos.getY(k)));
        }
        // every piece reaches the lowest layer of the plant or its root (a mat or a dome over the rock grows from it)
        boolean[] anchored = new boolean[n];
        for (int i = 0; i < n; i++) {
            float[] c = quads.get(i);
            for (int k = 0; k < 4; k++) {
                float dx = c[k * 3] - 0.5F, dy = c[k * 3 + 1] - 0.5F, dz = c[k * 3 + 2] - 0.5F;
                boolean onRock = mat && c[k * 3 + 1] <= bottom.getOrDefault(PlantMeshBuilder.col((int) Math.floor(c[k * 3]), (int) Math.floor(c[k * 3 + 2])),
                        Integer.MIN_VALUE / 2) + 0.8F;
                if (c[k * 3 + 1] <= minY + 0.8F || dx * dx + dy * dy + dz * dz <= 1.6F * 1.6F || onRock) anchored[find(parent, i)] = true;
            }
        }
        for (int i = 0; i < n; i++) {
            int r = find(parent, i);
            if (!anchored[r]) {
                float[] c = quads.get(i);
                StringBuilder sb = new StringBuilder();
                for (int yy = minY; yy <= maxY; yy++) {
                    sb.append(" | y=").append(yy).append(':');
                    for (long k : parts.keySet()) if (BlockPos.getY(k) == yy) sb.append(' ').append(BlockPos.getX(k)).append(',').append(BlockPos.getZ(k)).append('=').append(parts.get(k).name().charAt(0));
                }
                throw new AssertionError(what + ": a piece floats in the water without a base, e.g. a quad at ("
                        + c[0] + ", " + c[1] + ", " + c[2] + ")" + sb);
            }
        }
    }

    private static float[][] points(float[] c) {
        float[][] out = new float[9][];
        float cx = 0, cy = 0, cz = 0;
        for (int k = 0; k < 4; k++) {
            out[k] = new float[] {c[k * 3], c[k * 3 + 1], c[k * 3 + 2]};
            int j = (k + 1) % 4;
            out[4 + k] = new float[] {(c[k * 3] + c[j * 3]) / 2, (c[k * 3 + 1] + c[j * 3 + 1]) / 2, (c[k * 3 + 2] + c[j * 3 + 2]) / 2};
            cx += c[k * 3] / 4;
            cy += c[k * 3 + 1] / 4;
            cz += c[k * 3 + 2] / 4;
        }
        out[8] = new float[] {cx, cy, cz};
        return out;
    }

    private static int find(int[] p, int i) {
        while (p[i] != i) {
            p[i] = p[p[i]];
            i = p[i];
        }
        return i;
    }

    private static void union(int[] p, int a, int b) {
        int ra = find(p, a), rb = find(p, b);
        if (ra != rb) p[ra] = rb;
    }

    @Test
    void everySpeciesEveryFormHangsTogether() {
        int built = 0;
        for (PlantSpecies sp : PlantSpecies.values()) {
            for (int scale : SCALES)
                for (int head : HEADROOMS)
                    for (long seed = 0; seed < 8; seed++) {
                        PlantShapes.Shape shape = PlantShapes.of(sp, seed * 7919 + 13, scale, head);
                        for (int stage : new int[] {Math.max(1, sp.stages() / 2), sp.stages()}) {
                            Long2ObjectOpenHashMap<PlantPart> parts = blocks(shape.at(stage), null, 0);
                            String what = sp + " scale " + scale + " headroom " + head + " seed " + seed + " stage " + stage;
                            check(sp, parts, build(sp, parts, PlantMeshRenderer.Style.INSIDE), what);
                            built++;
                        }
                    }
        }
        assertTrue(built > 4000);
    }

    @Test
    void plantsWithMissingBlocksStillHangTogether() {
        Random r = new Random(2211);
        for (PlantSpecies sp : PlantSpecies.values()) {
            for (int i = 0; i < 40; i++) {
                PlantShapes.Shape shape = PlantShapes.of(sp, 100 + i, SCALES[i % 3], HEADROOMS[i % 4]);
                Long2ObjectOpenHashMap<PlantPart> parts = blocks(shape.all(), r, 0.15);
                check(sp, parts, build(sp, parts, PlantMeshRenderer.Style.INSIDE), sp + " with missing blocks, try " + i);
            }
        }
    }

    @Test
    void outsideStyleBuildsToo() {
        // the miniature's style (bigger leaves and polyps) without the atlas (no game in a unit test)
        PlantMeshRenderer.Style out = new PlantMeshRenderer.Style(1.25F, 1.5F, 1.8F, 1.35F, 1.18F, false, 1.3F);
        for (PlantSpecies sp : PlantSpecies.values()) {
            PlantShapes.Shape shape = PlantShapes.of(sp, 5, 16, 20);
            Long2ObjectOpenHashMap<PlantPart> parts = blocks(shape.all(), null, 0);
            check(sp, parts, build(sp, parts, out), sp + " outside");
        }
    }

    @Test
    void leafTipsAreOneLeafEach() {
        // a rosette of two leaves: two tips, each way from the root to its tip step by step
        Long2ObjectOpenHashMap<PlantPart> parts = new Long2ObjectOpenHashMap<>();
        parts.put(PlantSkeleton.key(0, 0, 0), PlantPart.ROOT);
        for (int i = 1; i <= 6; i++) {
            parts.put(PlantSkeleton.key(i, i / 2, 0), PlantPart.LEAF);
            parts.put(PlantSkeleton.key(i, i / 2, 1), PlantPart.LEAF); // the blade is two blocks wide
            parts.put(PlantSkeleton.key(-i, i / 2, 0), PlantPart.LEAF);
        }
        long root = PlantSkeleton.key(0, 0, 0);
        PlantSkeleton.Tree t = PlantSkeleton.tree(parts, k -> k == root, k -> true);
        LongArrayList tips = PlantSkeleton.leafTips(t, 2, 2.0);
        assertTrue(tips.size() == 2, "two leaves, found " + tips.size());
        LongOpenHashSet seen = new LongOpenHashSet();
        for (long tip : tips) {
            LongArrayList path = t.path(tip);
            assertTrue(path.getLong(0) == root, "a leaf starts at the crown");
            for (int i = 1; i < path.size(); i++) assertTrue(PlantSkeleton.distance(path.getLong(i - 1), path.getLong(i)) == 1, "a leaf has no gap");
            seen.add(path.getLong(path.size() - 1));
        }
        assertTrue(seen.size() == 2);
    }
}
