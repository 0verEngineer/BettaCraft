package org.overengineer.bettacraft.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** M21: A* on the interior grid (frogs, small animals, plecos). */
class GridPathfinderTest {
    /** A grid of booleans [x][y][z]. */
    static final class Box implements GridPathfinder.Space {
        final boolean[][][] solid;

        Box(int nx, int ny, int nz) {
            solid = new boolean[nx][ny][nz];
        }

        void fill(int x0, int y0, int z0, int x1, int y1, int z1, boolean v) {
            for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) solid[x][y][z] = v;
        }

        @Override public int nx() { return solid.length; }
        @Override public int ny() { return solid[0].length; }
        @Override public int nz() { return solid[0][0].length; }
        @Override public boolean solid(int x, int y, int z) { return solid[x][y][z]; }
    }

    private static void assertFreeSteps(Box b, List<int[]> path) {
        for (int[] c : path) assertTrue(!b.solid(c[0], c[1], c[2]), "path cell is free");
        for (int i = 1; i < path.size(); i++) assertTrue(GridPathfinder.line(b, path.get(i - 1), path.get(i)), "smoothed legs stay free");
    }

    @Test
    void swimsAroundAWall() {
        Box b = new Box(16, 8, 16);
        b.fill(8, 0, 0, 8, 7, 12, true); // a wall across the tank with a gap at z 13..15
        List<int[]> p = GridPathfinder.find(b, GridPathfinder.Mode.SWIM, new int[] {2, 1, 2}, new int[] {13, 1, 2}, 5000);
        assertNotNull(p);
        assertFreeSteps(b, p);
        assertArrayEquals(new int[] {13, 1, 2}, p.get(p.size() - 1));
        assertTrue(p.stream().anyMatch(c -> c[2] >= 13), "goes through the gap");
        // no gap: no way
        b.fill(8, 0, 13, 8, 7, 15, true);
        assertNull(GridPathfinder.find(b, GridPathfinder.Mode.SWIM, new int[] {2, 1, 2}, new int[] {13, 1, 2}, 5000));
    }

    @Test
    void swimsIntoACaveThroughItsMouth() {
        Box b = new Box(16, 8, 16);
        // a tube along x from 4 to 11, open at x = 4 (the mouth), closed at x = 11, inner cell row y 1, z 7
        b.fill(4, 0, 6, 11, 2, 8, true);
        b.fill(4, 1, 7, 10, 1, 7, false);
        int[] inside = {9, 1, 7};
        List<int[]> p = GridPathfinder.find(b, GridPathfinder.Mode.SWIM, new int[] {14, 1, 7}, inside, 5000);
        assertNotNull(p);
        assertFreeSteps(b, p);
        // it has to come round to the mouth at the west end
        assertTrue(p.stream().anyMatch(c -> c[0] <= 4), "enters through the mouth");
    }

    @Test
    void walksUnderAnOverhang() {
        Box b = new Box(12, 6, 12);
        // a rock: a pillar at x 4..5, with a roof at y 2 over x 4..9 (the floor under it is free)
        b.fill(4, 0, 0, 5, 3, 11, true);
        b.fill(4, 2, 0, 9, 3, 11, true);
        b.fill(4, 0, 0, 5, 1, 4, false); // an opening at the foot of the pillar (z 0..4)
        List<int[]> p = GridPathfinder.find(b, GridPathfinder.Mode.WALK, new int[] {1, 0, 8}, new int[] {8, 0, 8}, 5000);
        assertNotNull(p);
        for (int[] c : p) assertTrue(GridPathfinder.standable(b, c[0], c[1], c[2]), "walks on the floor");
        assertEquals(0, p.get(p.size() - 1)[1], "ends on the floor under the roof");
        assertTrue(p.stream().anyMatch(c -> c[2] <= 4), "round through the opening");
    }

    @Test
    void walksUpAStepAndDropsDown() {
        Box b = new Box(10, 6, 3);
        b.fill(4, 0, 0, 5, 0, 2, true); // a low step, one cell high
        List<int[]> p = GridPathfinder.find(b, GridPathfinder.Mode.WALK, new int[] {1, 0, 1}, new int[] {8, 0, 1}, 5000);
        assertNotNull(p);
        assertTrue(p.stream().anyMatch(c -> c[1] == 1), "over the step");
        // a wall two cells high: no way over it
        b.fill(4, 0, 0, 5, 1, 2, true);
        assertNull(GridPathfinder.find(b, GridPathfinder.Mode.WALK, new int[] {1, 0, 1}, new int[] {8, 0, 1}, 5000));
    }
}
