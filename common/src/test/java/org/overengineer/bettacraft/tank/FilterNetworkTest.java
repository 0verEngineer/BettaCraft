package org.overengineer.bettacraft.tank;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.filter.MultiblockFilter;
import org.overengineer.bettacraft.plumbing.interior.InteriorPlumbing;
import org.overengineer.bettacraft.plumbing.interior.Plumb;
import org.overengineer.bettacraft.plumbing.interior.TankAnchors;

/** M40 part a: the shares of a filter by water volume, where a pipe docks inside, the guards and the pipes through a tank. */
class FilterNetworkTest {
    @Test
    void cleaningAndFlowAreSharedByWaterVolume() {
        assertArrayEquals(new double[]{0.75, 0.25}, MultiblockFilter.shares(new long[]{3000, 1000}), 1e-9);
        double[] three = MultiblockFilter.shares(new long[]{8000, 8000, 16000});
        assertArrayEquals(new double[]{0.25, 0.25, 0.5}, three, 1e-9);
        assertArrayEquals(new double[]{1, 0}, MultiblockFilter.shares(new long[]{500, 0}), 1e-9);
        assertArrayEquals(new double[]{0, 0}, MultiblockFilter.shares(new long[]{0, 0}), 1e-9);
    }

    @Test
    void theDockSitsInTheMiddleOfThatFace() {
        // cell (1, 0, 2) at scale 16: floor, top, the four walls
        assertEquals(BlockPos.asLong(24, 0, 40), TankAnchors.localKey(1, 0, 2, Direction.DOWN, 16));
        assertEquals(BlockPos.asLong(24, 15, 40), TankAnchors.localKey(1, 0, 2, Direction.UP, 16));
        assertEquals(BlockPos.asLong(24, 8, 32), TankAnchors.localKey(1, 0, 2, Direction.NORTH, 16));
        assertEquals(BlockPos.asLong(24, 8, 47), TankAnchors.localKey(1, 0, 2, Direction.SOUTH, 16));
        assertEquals(BlockPos.asLong(16, 8, 40), TankAnchors.localKey(1, 0, 2, Direction.WEST, 16));
        assertEquals(BlockPos.asLong(31, 8, 40), TankAnchors.localKey(1, 0, 2, Direction.EAST, 16));
        // big tanks: scale 8
        assertEquals(BlockPos.asLong(4, 7, 4), TankAnchors.localKey(0, 0, 0, Direction.UP, 8));
    }

    @Test
    void theDockOfAFaceIsFoundAndAFloorUsesItsBulkhead() {
        Long2IntOpenHashMap m = new Long2IntOpenHashMap();
        long east = TankAnchors.localKey(0, 0, 0, Direction.EAST, 16);
        m.put(east, Plumb.code(Plumb.ANCHOR, Direction.EAST.get3DDataValue()));
        assertEquals(east, TankAnchors.find(m, 0, 0, 0, Direction.EAST, 16));
        assertEquals(Long.MIN_VALUE, TankAnchors.find(m, 0, 0, 0, Direction.WEST, 16), "another side has no dock");
        assertEquals(Long.MIN_VALUE, TankAnchors.find(m, 1, 0, 0, Direction.EAST, 16), "another block has no dock");
        long port = BlockPos.asLong(8, 0, 8);
        m.put(port, Plumb.code(Plumb.PORT, 0));
        assertEquals(port, TankAnchors.find(m, 0, 0, 0, Direction.DOWN, 16), "a floor block with a bulkhead docks there");
    }

    @Test
    void guardsCountAsIntakeAndOutletsAndPipesMayNotLeadOutAgain() {
        assertTrue(Plumb.INTAKE_GUARD.intake() && Plumb.STRAINER.intake() && !Plumb.OUTLET_GUARD.intake());
        assertTrue(Plumb.OUTLET_GUARD.outflow() && Plumb.LILY.outflow());
        assertTrue(Plumb.ANCHOR.port() && Plumb.PORT.port() && !Plumb.ANCHOR.outlet());
        Long2IntOpenHashMap m = new Long2IntOpenHashMap();
        long bottom = BlockPos.asLong(8, 0, 8), top = BlockPos.asLong(8, 6, 8);
        m.put(bottom, Plumb.code(Plumb.ANCHOR, Direction.DOWN.get3DDataValue()));
        m.put(top, Plumb.code(Plumb.ANCHOR, Direction.UP.get3DDataValue()));
        for (int y = 1; y <= 4; y++) m.put(BlockPos.asLong(8, y, 8), Plumb.code(Plumb.PIPE, 0));
        m.put(BlockPos.asLong(9, 1, 8), Plumb.code(Plumb.INTAKE_GUARD, 0));
        InteriorPlumbing.Net n = InteriorPlumbing.search(m, bottom);
        assertFalse(n.passThrough());
        assertEquals(1, n.intakes().size(), "the guard at the pipe");
        // the last piece up to the top dock would lead the pipe out again
        assertTrue(InteriorPlumbing.passThrough(withPipe(m, BlockPos.asLong(8, 5, 8)), BlockPos.asLong(8, 5, 8)));
        assertFalse(InteriorPlumbing.passThrough(withPipe(m, BlockPos.asLong(7, 4, 8)), BlockPos.asLong(7, 4, 8)));
        m.put(BlockPos.asLong(8, 5, 8), Plumb.code(Plumb.PIPE, 0));
        assertTrue(InteriorPlumbing.search(m, bottom).passThrough(), "built anyway (a command): the dock stays closed");
    }

    private static Long2IntOpenHashMap withPipe(Long2IntOpenHashMap m, long k) {
        Long2IntOpenHashMap c = new Long2IntOpenHashMap(m);
        c.put(k, Plumb.code(Plumb.PIPE, 0));
        return c;
    }
}
