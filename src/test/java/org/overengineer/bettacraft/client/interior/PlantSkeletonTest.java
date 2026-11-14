package org.overengineer.bettacraft.client.interior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.plants.PlantPart;
import org.overengineer.bettacraft.plants.PlantShapes;
import org.overengineer.bettacraft.plants.PlantSpecies;

/** M21.1: stems of the Rotala and ribbons of the Vallisneria are worked out without loose pieces (every growth form). */
class PlantSkeletonTest {
    private static final int[] SCALES = {16, 8, 4};
    private static final int[] HEADROOMS = {6, 12, 20, 31, 47};

    private static Long2ObjectOpenHashMap<PlantPart> blocks(List<PlantShapes.Voxel> vs) {
        Long2ObjectOpenHashMap<PlantPart> m = new Long2ObjectOpenHashMap<>();
        m.put(PlantSkeleton.key(0, 0, 0), PlantPart.ROOT);
        for (PlantShapes.Voxel v : vs) m.put(PlantSkeleton.key(v.x(), v.y(), v.z()), v.part());
        return m;
    }

    /** What the renderer sees: the blocks connected to the root (26-neighbourhood), like its flood fill. */
    private static Long2ObjectOpenHashMap<PlantPart> connected(Long2ObjectOpenHashMap<PlantPart> all) {
        Long2ObjectOpenHashMap<PlantPart> out = new Long2ObjectOpenHashMap<>();
        ArrayDeque<Long> q = new ArrayDeque<>();
        long root = PlantSkeleton.key(0, 0, 0);
        if (!all.containsKey(root)) return out;
        q.add(root);
        out.put(root, all.get(root));
        while (!q.isEmpty()) {
            long c = q.poll();
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++)
                    for (int dx = -1; dx <= 1; dx++) {
                        long n = PlantSkeleton.key(PlantSkeleton.x(c) + dx, PlantSkeleton.y(c) + dy, PlantSkeleton.z(c) + dz);
                        if (all.containsKey(n) && !out.containsKey(n)) {
                            out.put(n, all.get(n));
                            q.add(n);
                        }
                    }
        }
        return out;
    }

    private static void checkStems(Long2ObjectOpenHashMap<PlantPart> blocks, String what) {
        PlantSkeleton.Stems s = PlantSkeleton.stems(blocks);
        int minY = Integer.MAX_VALUE;
        for (long k : blocks.keySet()) minY = Math.min(minY, PlantSkeleton.y(k));
        for (long k : blocks.keySet()) {
            boolean node = s.parent.containsKey(k), leaf = s.leafAt.containsKey(k);
            assertTrue(node != leaf, what + ": block " + str(k) + " is either a stem node or a leaf");
            if (leaf) {
                long n = s.leafAt.get(k);
                assertTrue(s.parent.containsKey(n), what + ": leaf " + str(k) + " hangs on a stem node");
                // M22.1: or the outer part of a big leaf, two blocks out and one up, with its inner part between
                boolean outer = PlantSkeleton.distance(k, n) == 2 && PlantSkeleton.y(k) == PlantSkeleton.y(n) + 1
                        && s.leafAt.getOrDefault(PlantSkeleton.key(PlantSkeleton.x(n) + Integer.signum(PlantSkeleton.x(k) - PlantSkeleton.x(n)), PlantSkeleton.y(n),
                        PlantSkeleton.z(n) + Integer.signum(PlantSkeleton.z(k) - PlantSkeleton.z(n))), Long.MAX_VALUE) == n;
                assertTrue(PlantSkeleton.distance(k, n) == 1 || outer, what + ": leaf " + str(k) + " sits right at its stem");
            }
        }
        // every stem node leads down to the floor through its parents, and only the lowest layer stands on the floor
        for (long k : s.parent.keySet()) {
            long c = k;
            for (int guard = 0; guard <= s.parent.size(); guard++) {
                long p = s.parent.get(c);
                if (p == PlantSkeleton.FLOOR) {
                    assertEquals(minY, PlantSkeleton.y(c), what + ": only the lowest stem blocks stand on the floor, not " + str(c));
                    c = PlantSkeleton.FLOOR;
                    break;
                }
                assertTrue(s.parent.containsKey(p), what + ": parent of " + str(c) + " is a node");
                assertTrue(PlantSkeleton.y(p) <= PlantSkeleton.y(c), what + ": a stem never hangs on a node above it (" + str(c) + ")");
                c = p;
            }
            assertEquals(PlantSkeleton.FLOOR, c, what + ": stem from " + str(k) + " reaches the floor");
        }
    }

    private static void checkRibbons(Long2ObjectOpenHashMap<PlantPart> blocks, String what) {
        List<PlantSkeleton.Ribbon> rs = PlantSkeleton.ribbons(blocks);
        int minY = Integer.MAX_VALUE;
        for (long k : blocks.keySet()) minY = Math.min(minY, PlantSkeleton.y(k));
        LongOpenHashSet seen = new LongOpenHashSet();
        LongOpenHashSet earlier = new LongOpenHashSet();
        for (PlantSkeleton.Ribbon r : rs) {
            assertFalse(r.cells().isEmpty(), what);
            long first = r.cells().getLong(0);
            if (r.from() == PlantSkeleton.FLOOR) {
                assertEquals(minY, PlantSkeleton.y(first), what + ": a ribbon starting on its own starts at the floor, not at " + str(first));
            } else {
                assertTrue(earlier.contains(r.from()), what + ": a branch starts at a ribbon before it");
                assertEquals(1, PlantSkeleton.distance(first, r.from()), what + ": a branch starts right next to its ribbon");
            }
            for (int i = 1; i < r.cells().size(); i++) {
                assertEquals(1, PlantSkeleton.distance(r.cells().getLong(i - 1), r.cells().getLong(i)), what + ": ribbon pieces are joined");
                assertTrue(PlantSkeleton.y(r.cells().getLong(i)) >= PlantSkeleton.y(r.cells().getLong(i - 1)), what + ": a ribbon never runs down");
            }
            seen.addAll(r.cells());
            earlier.addAll(r.cells());
        }
        assertEquals(blocks.size(), seen.size(), what + ": every block is part of a ribbon");
    }

    private static String str(long k) {
        return "(" + PlantSkeleton.x(k) + "," + PlantSkeleton.y(k) + "," + PlantSkeleton.z(k) + ")";
    }

    @Test
    void keysRoundTrip() {
        for (int[] p : new int[][] {{0, 0, 0}, {-5, 3, 9}, {300, -4, -300}, {-1, -1, -1}}) {
            long k = PlantSkeleton.key(p[0], p[1], p[2]);
            assertEquals(p[0], PlantSkeleton.x(k));
            assertEquals(p[1], PlantSkeleton.y(k));
            assertEquals(p[2], PlantSkeleton.z(k));
        }
        assertNotEquals(PlantSkeleton.FLOOR, PlantSkeleton.key(0, 0, 0));
    }

    @Test
    void everyRotalaLeafSitsOnAStemThatReachesTheFloor() {
        int forms = 0;
        for (int scale : SCALES)
            for (int headroom : HEADROOMS)
                for (long seed = 1; seed <= 40; seed++) {
                    PlantShapes.Shape sh = PlantShapes.of(PlantSpecies.ROTALA, seed, scale, headroom);
                    for (int st = 1; st <= sh.stages(); st++) {
                        checkStems(blocks(sh.at(st)), "rotala scale " + scale + " headroom " + headroom + " seed " + seed + " stage " + st);
                        forms++;
                    }
                }
        assertTrue(forms > 1000);
    }

    @Test
    void everyLudwigiaAndAlternantheraLeafSitsOnAStemThatReachesTheFloor() {
        // M22.1: the other stem plants (a leaf every second node, wider leaves at the bigger scale)
        int forms = 0;
        for (PlantSpecies sp : new PlantSpecies[] {PlantSpecies.LUDWIGIA, PlantSpecies.ALTERNANTHERA})
            for (int scale : SCALES)
                for (int headroom : HEADROOMS)
                    for (long seed = 1; seed <= 20; seed++) {
                        PlantShapes.Shape sh = PlantShapes.of(sp, seed, scale, headroom);
                        for (int st = 1; st <= sh.stages(); st++) {
                            checkStems(blocks(sh.at(st)), sp + " scale " + scale + " headroom " + headroom + " seed " + seed + " stage " + st);
                            forms++;
                        }
                    }
        assertTrue(forms > 1000);
    }

    @Test
    void rotalaWithMissingBlocksStillHasNoLeafWithoutStem() {
        // blocks the plant could not place (decoration in the way) or a neighbour took: whatever is left and connected
        Random r = new Random(21);
        for (long seed = 1; seed <= 60; seed++) {
            PlantShapes.Shape sh = PlantShapes.of(PlantSpecies.ROTALA, seed, 16, 31);
            Long2ObjectOpenHashMap<PlantPart> all = blocks(sh.at(sh.stages()));
            for (int i = 0; i < 6; i++) {
                long[] keys = all.keySet().toLongArray();
                long k = keys[r.nextInt(keys.length)];
                if (k != PlantSkeleton.key(0, 0, 0)) all.remove(k);
            }
            checkStems(connected(all), "rotala seed " + seed + " with holes");
        }
    }

    @Test
    void stemsPassThroughLeavesOfANeighbouringShoot() {
        // a shoot right next to the main stem whose every second block is a leaf of the main stem
        Long2ObjectOpenHashMap<PlantPart> b = new Long2ObjectOpenHashMap<>();
        b.put(PlantSkeleton.key(0, 0, 0), PlantPart.ROOT);
        b.put(PlantSkeleton.key(1, 0, 0), PlantPart.STEM);
        for (int y = 1; y <= 8; y++) {
            b.put(PlantSkeleton.key(0, y, 0), PlantPart.STEM);
            b.put(PlantSkeleton.key(1, y, 0), y % 2 == 0 ? PlantPart.LEAF : PlantPart.STEM);
            b.put(PlantSkeleton.key(-1, y, 0), PlantPart.LEAF);
            b.put(PlantSkeleton.key(2, y, 0), PlantPart.LEAF);
        }
        PlantSkeleton.Stems s = PlantSkeleton.stems(b);
        checkStems(b, "neighbouring shoot");
        // the side shoot's stem blocks hang on its own column, not on the main stem
        assertEquals(PlantSkeleton.key(1, 1, 0), s.parent.get(PlantSkeleton.key(1, 3, 0)));
        assertEquals(PlantSkeleton.key(1, 0, 0), s.parent.get(PlantSkeleton.key(1, 1, 0)));
    }

    @Test
    void everyVallisneriaRibbonHangsOnABase() {
        int forms = 0;
        for (int scale : SCALES)
            for (int headroom : HEADROOMS)
                for (long seed = 1; seed <= 40; seed++) {
                    PlantShapes.Shape sh = PlantShapes.of(PlantSpecies.VALLISNERIA, seed, scale, headroom);
                    for (int st = 1; st <= sh.stages(); st++) {
                        checkRibbons(blocks(sh.at(st)), "vallisneria scale " + scale + " headroom " + headroom + " seed " + seed + " stage " + st);
                        forms++;
                    }
                }
        assertTrue(forms > 1000);
    }

    @Test
    void vallisneriaVersion2KeepsItsRibbonsApart() {
        // M22.1: the generator of new plants keeps the ribbons out of each other's blocks: the skeleton finds fewer blocks
        // shared by two ribbons (crossings) than with the old generator (which existing plants keep)
        int shared1 = 0, shared2 = 0, forms = 0;
        for (int scale : SCALES)
            for (int headroom : HEADROOMS)
                for (long seed = 1; seed <= 40; seed++) {
                    for (int v = 1; v <= 2; v++) {
                        PlantShapes.Shape sh = PlantShapes.of(PlantSpecies.VALLISNERIA, seed, scale, headroom, v);
                        Long2ObjectOpenHashMap<PlantPart> b = blocks(sh.all());
                        checkRibbons(b, "vallisneria v" + v + " scale " + scale + " headroom " + headroom + " seed " + seed);
                        LongOpenHashSet seen = new LongOpenHashSet();
                        int shared = 0;
                        for (PlantSkeleton.Ribbon r : PlantSkeleton.ribbons(b)) for (long k : r.cells()) if (!seen.add(k)) shared++;
                        if (v == 1) shared1 += shared;
                        else shared2 += shared;
                    }
                    forms++;
                }
        assertTrue(forms > 500);
        // the generator itself never puts two ribbons into one block any more; where ribbons stand diagonally next to
        // each other the skeleton may still walk along a neighbour for a few blocks
        assertTrue(shared2 < shared1, "version 2 shares fewer blocks: " + shared2 + " vs " + shared1);
    }

    @Test
    void vallisneriaRunAlongTheSurfaceContinuesItsRibbon() {
        // a ribbon up to y 6, then 4 blocks along the surface: one ribbon, not a separate piece in the air
        Long2ObjectOpenHashMap<PlantPart> b = new Long2ObjectOpenHashMap<>();
        b.put(PlantSkeleton.key(0, 0, 0), PlantPart.ROOT);
        for (int y = 1; y <= 6; y++) b.put(PlantSkeleton.key(0, y, 0), PlantPart.RIBBON);
        for (int j = 1; j <= 4; j++) b.put(PlantSkeleton.key(j, 6, 0), j >= 3 ? PlantPart.TOP : PlantPart.RIBBON);
        List<PlantSkeleton.Ribbon> rs = PlantSkeleton.ribbons(b);
        assertEquals(1, rs.size());
        assertEquals(11, rs.getFirst().cells().size());
        assertEquals(PlantSkeleton.key(4, 6, 0), rs.getFirst().cells().getLong(10));
        checkRibbons(b, "run");
    }

    @Test
    void vallisneriaWithMissingBlocksHasNoLoosePieces() {
        Random r = new Random(22);
        for (long seed = 1; seed <= 60; seed++) {
            PlantShapes.Shape sh = PlantShapes.of(PlantSpecies.VALLISNERIA, seed, 16, 31);
            Long2ObjectOpenHashMap<PlantPart> all = blocks(sh.at(sh.stages()));
            for (int i = 0; i < 5; i++) {
                long[] keys = all.keySet().toLongArray();
                long k = keys[r.nextInt(keys.length)];
                if (k != PlantSkeleton.key(0, 0, 0)) all.remove(k);
            }
            checkRibbons(connected(all), "vallisneria seed " + seed + " with holes");
        }
    }
}
