package org.overengineer.bettacraft.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.plumbing.interior.InteriorLanes;
import org.overengineer.bettacraft.plumbing.interior.InteriorPlumbing;
import org.overengineer.bettacraft.plumbing.interior.Plumb;
import org.overengineer.bettacraft.plumbing.interior.TankAnchors;

/**
 * M68 part C: a dock per line of a bundle (where they sit, never touching), the codes of the index (old codes read as before),
 * and the interior bundles: lanes that stay apart, go on along the run and part to their own sides.
 */
class InteriorLinesTest {
    private static boolean touching(long a, long b) {
        int d = Math.abs(BlockPos.getX(a) - BlockPos.getX(b)) + Math.abs(BlockPos.getY(a) - BlockPos.getY(b)) + Math.abs(BlockPos.getZ(a) - BlockPos.getZ(b));
        return d <= 1;
    }

    @Test
    void everyLineHasItsDockAtItsFlangeAndNoneTouch() {
        for (int s : new int[]{16, 8, 4}) {
            for (Direction face : Direction.values()) {
                List<Long> keys = new ArrayList<>();
                for (int line = 0; line <= 4; line++) keys.add(TankAnchors.localKey(1, 0, 2, face, s, line));
                for (int i = 0; i < keys.size(); i++) {
                    long k = keys.get(i);
                    // in the cell, on the face
                    assertEquals(1, Math.floorDiv(BlockPos.getX(k), s), "cell x " + face + " " + s);
                    assertEquals(0, Math.floorDiv(BlockPos.getY(k), s), "cell y " + face + " " + s);
                    assertEquals(2, Math.floorDiv(BlockPos.getZ(k), s), "cell z " + face + " " + s);
                    for (int j = i + 1; j < keys.size(); j++) {
                        assertNotEquals(k, (long) keys.get(j), "lines " + i + " and " + j + " at " + face + " scale " + s);
                        if (i > 0) assertFalse(touching(k, keys.get(j)), "lines " + i + " and " + j + " touch at " + face + " scale " + s);
                    }
                }
            }
        }
        // line 0 is the middle (as before), the lines sit at their flanges: 8 -/+ 1.875 px -> blocks 6 and 9 at scale 16
        assertEquals(TankAnchors.localKey(1, 0, 2, Direction.EAST, 16), TankAnchors.localKey(1, 0, 2, Direction.EAST, 16, 0));
        assertEquals(BlockPos.asLong(31, 9, 38), TankAnchors.localKey(1, 0, 2, Direction.EAST, 16, 1), "slot 0 (-x, +y, -z) on the east face");
        assertEquals(BlockPos.asLong(31, 9, 41), TankAnchors.localKey(1, 0, 2, Direction.EAST, 16, 2), "slot 1 (+x, +y, +z)");
    }

    @Test
    void codesCarryTheLineAndOldCodesReadAsBefore() {
        int old = Plumb.code(Plumb.ANCHOR, Direction.EAST.get3DDataValue()); // saved before part C
        assertEquals(Plumb.ANCHOR, Plumb.kind(old));
        assertEquals(0, Plumb.extra(old), "an old dock is the face's dock");
        int line = Plumb.code(Plumb.ANCHOR, Direction.EAST.get3DDataValue(), 3);
        assertEquals(Plumb.ANCHOR, Plumb.kind(line));
        assertEquals(Direction.EAST.get3DDataValue(), Plumb.facing(line));
        assertEquals(3, Plumb.extra(line));
        int bundle = Plumb.code(Plumb.BUNDLE, Direction.Axis.Z.ordinal(), 4);
        assertEquals(Plumb.BUNDLE, Plumb.kind(bundle));
        assertEquals(Direction.Axis.Z, InteriorLanes.axisOf(bundle));
        for (Plumb p : Plumb.values()) assertEquals(p, Plumb.kind(Plumb.code(p, 3)), "every kind reads back");
        assertTrue(Plumb.BUNDLE.conduit() && Plumb.PIPE.conduit() && !Plumb.BUNDLE.outlet() && !Plumb.BUNDLE.port());
        // two docks of one face: line 1 is found as line 1, not as the face's dock
        Long2IntOpenHashMap m = new Long2IntOpenHashMap();
        long l1 = TankAnchors.localKey(0, 0, 0, Direction.EAST, 16, 1), l2 = TankAnchors.localKey(0, 0, 0, Direction.EAST, 16, 2);
        m.put(l1, Plumb.code(Plumb.ANCHOR, Direction.EAST.get3DDataValue(), 1));
        m.put(l2, Plumb.code(Plumb.ANCHOR, Direction.EAST.get3DDataValue(), 2));
        assertEquals(l1, TankAnchors.find(m, 0, 0, 0, Direction.EAST, 16, 1));
        assertEquals(l2, TankAnchors.find(m, 0, 0, 0, Direction.EAST, 16, 2));
        assertEquals(Long.MIN_VALUE, TankAnchors.find(m, 0, 0, 0, Direction.EAST, 16, 0), "no dock of the whole face");
        assertEquals(Long.MIN_VALUE, TankAnchors.find(m, 0, 0, 0, Direction.EAST, 16, 3));
    }

    @Test
    void lanesOfABundle() {
        // the middle lane along the axis, the side lanes each to its own side, none twice
        for (Direction.Axis a : Direction.Axis.values()) {
            for (Direction d : Direction.values()) {
                int lane = InteriorLanes.laneOn(a, 4, d);
                if (d.getAxis() == a) assertEquals(0, lane);
                else if (lane > 0) assertEquals(d, InteriorLanes.laneSide(a, lane));
            }
            List<Direction> sides = new ArrayList<>();
            for (int lane = 1; lane < 4; lane++) {
                Direction s = InteriorLanes.laneSide(a, lane);
                assertNotEquals(a, s.getAxis(), "a side lane leaves across the run");
                assertFalse(sides.contains(s));
                sides.add(s);
            }
            assertEquals(-1, InteriorLanes.laneOn(a, 2, InteriorLanes.laneSide(a, 2)), "two lanes: no lane 2 yet");
        }
        // a pipe that runs straight or ends keeps its axis; a corner takes the clicked one
        int ew = 1 << Direction.EAST.get3DDataValue() | 1 << Direction.WEST.get3DDataValue();
        assertEquals(Direction.Axis.X, InteriorLanes.axisFor(ew, Direction.Axis.Y));
        assertEquals(Direction.Axis.Z, InteriorLanes.axisFor(1 << Direction.NORTH.get3DDataValue(), Direction.Axis.Y));
        assertEquals(Direction.Axis.Y, InteriorLanes.axisFor(1 << Direction.NORTH.get3DDataValue() | 1 << Direction.EAST.get3DDataValue(), Direction.Axis.Y));
        // the boxes of the side lanes lie towards their sides and inside the block
        for (Direction.Axis a : Direction.Axis.values())
            for (int lane = 0; lane < 4; lane++)
                for (int piece = 0; piece < 4; piece++) {
                    if (lane == 0 && piece == InteriorLanes.ARM) continue;
                    float[] b = InteriorLanes.box(a, lane, piece);
                    for (int i = 0; i < 3; i++) assertTrue(b[i] >= 0 && b[i + 3] <= 16 && b[i] < b[i + 3], a + " " + lane + " " + piece);
                }
    }

    /**
     * A run of three bundles along x (4 lanes): line A comes in at the east end (the middle lane) and leaves at the west end into
     * an intake guard; line B joins its side lane 3 at the east block and parts at the west block into an outlet guard. The two
     * docks do not meet; a pipe of line A next to the side of lane 1 does not join line B.
     */
    @Test
    void aRunOfBundlesKeepsItsLinesApartAndPartsThem() {
        Long2IntOpenHashMap m = new Long2IntOpenHashMap();
        int pipe = Plumb.code(Plumb.PIPE, 0), bundle = Plumb.code(Plumb.BUNDLE, Direction.Axis.X.ordinal(), 4);
        int anchor = Plumb.code(Plumb.ANCHOR, Direction.EAST.get3DDataValue(), 1), anchorB = Plumb.code(Plumb.ANCHOR, Direction.EAST.get3DDataValue(), 2);
        Direction south = InteriorLanes.laneSide(Direction.Axis.X, 3);
        for (int x = 4; x <= 6; x++) m.put(BlockPos.asLong(x, 5, 5), bundle);
        // line A: dock (9,5,5), pipes to the east end of the run, out at the west end into an intake guard
        long dockA = BlockPos.asLong(9, 5, 5);
        m.put(dockA, anchor);
        m.put(BlockPos.asLong(8, 5, 5), pipe);
        m.put(BlockPos.asLong(7, 5, 5), pipe);
        m.put(BlockPos.asLong(3, 5, 5), pipe);
        long intake = BlockPos.asLong(2, 5, 5);
        m.put(intake, Plumb.code(Plumb.INTAKE_GUARD, 0));
        // line B: dock (9,5,9), pipes to the side of lane 3 of the east block, out of lane 3 of the west block into an outlet guard
        long dockB = BlockPos.asLong(9, 5, 9);
        m.put(dockB, anchorB);
        for (int x = 6; x <= 8; x++) m.put(BlockPos.asLong(x, 5, 9), pipe);
        m.put(BlockPos.asLong(6, 5, 8), pipe);
        m.put(BlockPos.asLong(6, 5, 7), pipe);
        m.put(BlockPos.asLong(6 + south.getStepX(), 5 + south.getStepY(), 5 + south.getStepZ()), pipe); // (6,5,6)
        m.put(BlockPos.asLong(4, 5, 6), pipe);
        long outlet = BlockPos.asLong(4, 5, 7);
        m.put(outlet, Plumb.code(Plumb.OUTLET_GUARD, 0));
        InteriorPlumbing.Net a = InteriorPlumbing.search(m, dockA);
        InteriorPlumbing.Net b = InteriorPlumbing.search(m, dockB);
        assertEquals(List.of(intake), a.intakes(), "line A through the middle lane to its intake guard: " + a);
        assertTrue(a.outflows().isEmpty() && a.ports().isEmpty(), "and nothing of line B: " + a);
        assertEquals(List.of(outlet), b.outflows(), "line B through lane 3 to its outlet guard: " + b);
        assertTrue(b.intakes().isEmpty() && b.ports().isEmpty(), "and nothing of line A: " + b);
        // a pipe on the top of the run (lane 1's side) joins lane 1 only, which leads nowhere
        long top = BlockPos.asLong(5, 6, 5);
        m.put(top, pipe);
        InteriorPlumbing.Net t = InteriorPlumbing.search(m, top);
        assertTrue(t.empty() && t.ports().isEmpty(), "lane 1 is empty: " + t);
        assertEquals(List.of(intake), InteriorPlumbing.search(m, dockA).intakes(), "line A unchanged");
        // a bundle across the run (another axis) joins nothing of it
        m.put(BlockPos.asLong(5, 4, 5), Plumb.code(Plumb.BUNDLE, Direction.Axis.Z.ordinal(), 2));
        assertTrue(InteriorPlumbing.search(m, BlockPos.asLong(5, 4, 5), 0).ports().isEmpty(), "a bundle of another axis below the run stays apart");
    }
}
