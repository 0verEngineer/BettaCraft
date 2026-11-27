package org.overengineer.bettacraft.tank;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.filter.FilterWater;
import org.overengineer.bettacraft.plumbing.PipeBundleBlock;
import org.overengineer.bettacraft.plumbing.PipeRing;
import org.overengineer.bettacraft.plumbing.PipeSlots;

/** M40 part b, M68: the slots of a pipe bundle, the old packed lines, its arms never touch each other, water shared by need. */
class FilterBundleTest {
    @Test
    void slotsKeepTheirBits() {
        int a = PipeSlots.of(true, PipeSlots.bit(Direction.NORTH) | PipeSlots.bit(Direction.SOUTH), PipeSlots.bit(Direction.UP), PipeRing.INTAKE);
        assertTrue(PipeSlots.present(a) && PipeSlots.steel(a));
        assertTrue(PipeSlots.joined(a, Direction.NORTH) && PipeSlots.joined(a, Direction.SOUTH) && !PipeSlots.joined(a, Direction.UP));
        assertTrue(PipeSlots.closed(a, Direction.UP) && !PipeSlots.closed(a, Direction.DOWN));
        assertEquals(PipeRing.INTAKE, PipeSlots.ring(a));
        int b = PipeSlots.withRing(PipeSlots.withClosed(PipeSlots.withSides(a, PipeSlots.bit(Direction.EAST)), 0), PipeRing.SHORT);
        assertEquals(PipeSlots.bit(Direction.EAST), PipeSlots.sides(b));
        assertEquals(0, PipeSlots.closed(b));
        assertEquals(PipeRing.SHORT, PipeSlots.ring(b));
        assertTrue(PipeSlots.steel(b) && PipeSlots.present(b));
        assertFalse(PipeSlots.steel(PipeSlots.of(false, 0, 0, PipeRing.NONE)));
        // the order of the rings is saved: never reorder
        assertEquals(0, PipeRing.NONE.ordinal());
        assertEquals(5, PipeRing.SHORT.ordinal());
    }

    /** M68: an M40 bundle (eight bits per line: present, T-piece, sides) becomes steel slots with exactly its old connections. */
    @Test
    void oldBundlesKeepTheirConnections() {
        int intake = 0x80 | PipeSlots.bit(Direction.WEST) | PipeSlots.bit(Direction.EAST);
        int waste = 0x80 | 0x40 | PipeSlots.bit(Direction.UP);
        int[] slots = PipeSlots.fromLegacy(intake | waste << 24);
        assertEquals(2, PipeSlots.count(slots));
        assertTrue(PipeSlots.present(slots[0]) && PipeSlots.steel(slots[0]) && !PipeSlots.present(slots[1]) && !PipeSlots.present(slots[2]));
        assertEquals(PipeSlots.bit(Direction.WEST) | PipeSlots.bit(Direction.EAST), PipeSlots.sides(slots[0]));
        assertEquals(PipeSlots.SIDES & ~(PipeSlots.bit(Direction.WEST) | PipeSlots.bit(Direction.EAST)), PipeSlots.closed(slots[0]), "every other side closed");
        assertEquals(PipeSlots.bit(Direction.UP), PipeSlots.sides(slots[3]));
        assertEquals(PipeRing.NONE, PipeSlots.ring(slots[3]));
    }

    /** M68: a single pipe meets the slot joined towards it, else the open end (fewest sides) that is not closed there. */
    @Test
    void theFacingSlot() {
        int through = PipeSlots.of(true, PipeSlots.bit(Direction.WEST) | PipeSlots.bit(Direction.EAST), 0, PipeRing.NONE);
        int end = PipeSlots.of(false, PipeSlots.bit(Direction.UP), 0, PipeRing.NONE);
        int[] slots = {through, end, 0, 0};
        assertEquals(0, PipeSlots.facing(slots, Direction.EAST), "joined there");
        assertEquals(1, PipeSlots.facing(slots, Direction.NORTH), "the open end");
        slots[1] = PipeSlots.withClosed(end, PipeSlots.bit(Direction.NORTH));
        assertEquals(0, PipeSlots.facing(slots, Direction.NORTH), "closed: the other one");
        slots[0] = PipeSlots.withClosed(through, PipeSlots.bit(Direction.NORTH));
        assertEquals(-1, PipeSlots.facing(slots, Direction.NORTH), "both closed");
    }

    private static boolean overlap(float[] a, float[] b) {
        return a[0] < b[3] && b[0] < a[3] && a[1] < b[4] && b[1] < a[4] && a[2] < b[5] && b[2] < a[5];
    }

    @Test
    void theArmsOfTheLinesNeverTouch() {
        for (int l = 0; l < PipeSlots.COUNT; l++) for (Direction d : Direction.values()) {
            float[] a = PipeBundleBlock.arm(l, d, PipeBundleBlock.ARM, 0);
            // from the manifold to the face, inside the block
            for (int i = 0; i < 3; i++) assertTrue(a[i] >= 0 && a[i + 3] <= 16 && a[i] < a[i + 3], l + " " + d);
            for (int m = 0; m < PipeSlots.COUNT; m++) for (Direction e : Direction.values()) {
                if (l == m) continue;
                float[] b = PipeBundleBlock.arm(m, e, PipeBundleBlock.ARM, 0);
                assertFalse(overlap(a, b), l + " " + d + " touches " + m + " " + e);
                // the caps of closed sides too
                assertFalse(overlap(PipeBundleBlock.arm(l, d, PipeBundleBlock.COLLAR, 1.0F), PipeBundleBlock.arm(m, e, PipeBundleBlock.COLLAR, 1.0F)),
                        "caps " + l + " " + d + " " + m + " " + e);
            }
        }
        // a slot's arms sit at the same spot of a face in every bundle: east of one bundle meets west of the next
        for (int l = 0; l < PipeSlots.COUNT; l++) {
            float[] e = PipeBundleBlock.arm(l, Direction.EAST, PipeBundleBlock.ARM, 0), w = PipeBundleBlock.arm(l, Direction.WEST, PipeBundleBlock.ARM, 0);
            assertArrayEquals(new float[]{e[1], e[2], e[4], e[5]}, new float[]{w[1], w[2], w[4], w[5]}, 1e-6F, "slot " + l);
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

    /** M65: towards a single pipe a line's arm leaves the middle of the face, exactly where the single pipe (3 px, 6.5..9.5) is. */
    @Test
    void theArmTowardsASinglePipeIsInLineWithIt() {
        for (int l = 0; l < PipeSlots.COUNT; l++) for (Direction d : Direction.values()) {
            float[] a = PipeBundleBlock.arm(l, d, PipeBundleBlock.ARM, 0, true);
            for (int i = 0; i < 3; i++) {
                if (i == d.getAxis().ordinal()) {
                    assertTrue(a[i] == 0 || a[i + 3] == 16, "reaches the face " + l + " " + d);
                    continue;
                }
                assertEquals(6.5F, a[i], 1e-6F, l + " " + d);
                assertEquals(9.5F, a[i + 3], 1e-6F, l + " " + d);
            }
        }
    }
}
