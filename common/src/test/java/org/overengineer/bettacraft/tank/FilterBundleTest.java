package org.overengineer.bettacraft.tank;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.filter.FilterWater;
import org.overengineer.bettacraft.plumbing.FilterPipeBundleBlock;
import org.overengineer.bettacraft.plumbing.FilterPipes;
import org.overengineer.bettacraft.plumbing.PipeLine;

/** M40 part b: the packed lines of a pipe bundle, its arms never touch each other, water shared by need. */
class FilterBundleTest {
    @Test
    void linesArePackedSeparately() {
        int a = FilterPipes.PRESENT | FilterPipes.bit(Direction.NORTH) | FilterPipes.bit(Direction.SOUTH);
        int b = FilterPipes.PRESENT | FilterPipes.JUNCTION | FilterPipes.bit(Direction.UP);
        int lines = FilterPipes.with(FilterPipes.with(0, PipeLine.INTAKE, a), PipeLine.WASTE, b);
        assertEquals(a, FilterPipes.slot(lines, PipeLine.INTAKE));
        assertEquals(0, FilterPipes.slot(lines, PipeLine.RETURN));
        assertEquals(0, FilterPipes.slot(lines, PipeLine.FRESH));
        assertEquals(b, FilterPipes.slot(lines, PipeLine.WASTE));
        assertEquals(EnumSet.of(PipeLine.INTAKE, PipeLine.WASTE), FilterPipes.lines(lines));
        assertEquals(2, FilterPipes.count(a));
        assertEquals(2, FilterPipes.limit(a));
        assertEquals(6, FilterPipes.limit(b));
        assertTrue(FilterPipes.junction(b) && !FilterPipes.junction(a));
        assertTrue(FilterPipes.joined(b, Direction.UP) && !FilterPipes.joined(b, Direction.DOWN));
        // taking a line out leaves the others
        int out = FilterPipes.with(lines, PipeLine.INTAKE, 0);
        assertEquals(EnumSet.of(PipeLine.WASTE), FilterPipes.lines(out));
        assertEquals(b, FilterPipes.slot(out, PipeLine.WASTE));
        // the order of the lines is saved: never reorder
        assertEquals(0, PipeLine.INTAKE.ordinal());
        assertEquals(1, PipeLine.RETURN.ordinal());
        assertEquals(2, PipeLine.FRESH.ordinal());
        assertEquals(3, PipeLine.WASTE.ordinal());
    }

    private static boolean overlap(float[] a, float[] b) {
        return a[0] < b[3] && b[0] < a[3] && a[1] < b[4] && b[1] < a[4] && a[2] < b[5] && b[2] < a[5];
    }

    @Test
    void theArmsOfTheLinesNeverTouch() {
        for (PipeLine l : PipeLine.values()) for (Direction d : Direction.values()) {
            float[] a = FilterPipeBundleBlock.arm(l, d, FilterPipeBundleBlock.ARM, 0);
            // from the manifold to the face, inside the block
            for (int i = 0; i < 3; i++) assertTrue(a[i] >= 0 && a[i + 3] <= 16 && a[i] < a[i + 3], l + " " + d);
            for (PipeLine m : PipeLine.values()) for (Direction e : Direction.values()) {
                if (l == m) continue;
                float[] b = FilterPipeBundleBlock.arm(m, e, FilterPipeBundleBlock.ARM, 0);
                assertFalse(overlap(a, b), l + " " + d + " touches " + m + " " + e);
                // the collars of T-pieces too
                assertFalse(overlap(FilterPipeBundleBlock.arm(l, d, FilterPipeBundleBlock.COLLAR, 1.5F),
                        FilterPipeBundleBlock.arm(m, e, FilterPipeBundleBlock.COLLAR, 1.5F)), "collars " + l + " " + d + " " + m + " " + e);
            }
        }
        // a line's arms sit at the same spot of a face in every bundle: east of one bundle meets west of the next
        for (PipeLine l : PipeLine.values()) {
            float[] e = FilterPipeBundleBlock.arm(l, Direction.EAST, FilterPipeBundleBlock.ARM, 0), w = FilterPipeBundleBlock.arm(l, Direction.WEST, FilterPipeBundleBlock.ARM, 0);
            assertArrayEquals(new float[]{e[1], e[2], e[4], e[5]}, new float[]{w[1], w[2], w[4], w[5]}, 1e-6F, l.name());
        }
    }

    @Test
    void waterIsSharedByNeed() {
        // enough: everyone gets what it wants
        assertArrayEquals(new long[]{500, 1000, 0}, FilterWater.distribute(new long[]{500, 1000, 0}, 5000));
        // short: by want (a share of each tank's volume, so by volume), nothing lost to rounding
        long[] g = FilterWater.distribute(new long[]{1000, 2000, 1000}, 1000);
        assertArrayEquals(new long[]{250, 500, 250}, g);
        long[] h = FilterWater.distribute(new long[]{1, 1, 1}, 2);
        assertEquals(2, h[0] + h[1] + h[2]);
        for (long x : h) assertTrue(x <= 1);
        assertArrayEquals(new long[]{0, 0}, FilterWater.distribute(new long[]{100, 100}, 0));
        assertArrayEquals(new long[]{0, 0}, FilterWater.distribute(new long[]{0, -5}, 100));
    }
}
