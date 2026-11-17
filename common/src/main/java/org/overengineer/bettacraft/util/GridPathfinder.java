package org.overengineer.bettacraft.util;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import javax.annotation.Nullable;

/**
 * M21: A* on a voxel grid (pure Java, unit tested). Two kinds of animals use it: {@link Mode#SWIM} moves in all 26
 * directions through free cells (dwarf frogs, plecos on their way into the cave); {@link Mode#WALK} stays on the floor:
 * a cell is standable when it is free and the cell below is solid (or it is the lowest layer), steps go to the 8
 * horizontal neighbours, one cell up (with headroom) or down any drop of up to {@link #MAX_DROP} cells. Diagonal moves
 * never cut a solid corner. The result is smoothed by line of sight (swimming) and returned as cells.
 */
public final class GridPathfinder {
    public enum Mode { SWIM, WALK }

    /** The grid: solid cells; everything outside counts as solid. */
    public interface Space {
        int nx();

        int ny();

        int nz();

        boolean solid(int x, int y, int z);
    }

    public static final int MAX_DROP = 4;

    private GridPathfinder() {}

    private static boolean free(Space s, int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < s.nx() && y < s.ny() && z < s.nz() && !s.solid(x, y, z);
    }

    public static boolean standable(Space s, int x, int y, int z) {
        return free(s, x, y, z) && (y == 0 || !free(s, x, y - 1, z));
    }

    /**
     * A path from {@code from} to {@code to} (cells {x, y, z}), both included, or null when there is none within
     * {@code maxNodes} expanded cells. The start cell may be solid (an animal pushed into the decoration gets out); a
     * solid goal is moved to the nearest free (standable) cell within two cells.
     */
    @Nullable
    public static List<int[]> find(Space s, Mode mode, int[] from, int[] to, int maxNodes) {
        int[] goal = settle(s, mode, to);
        if (goal == null) return null;
        int nx = s.nx(), ny = s.ny(), nz = s.nz();
        if (!inside(s, from)) return null;
        int start = idx(from[0], from[1], from[2], nx, nz);
        int end = idx(goal[0], goal[1], goal[2], nx, nz);
        Int2FloatOpenHashMap g = new Int2FloatOpenHashMap();
        g.defaultReturnValue(Float.MAX_VALUE);
        Int2IntOpenHashMap parent = new Int2IntOpenHashMap();
        PriorityQueue<float[]> open = new PriorityQueue<>((a, b) -> Float.compare(a[0], b[0]));
        g.put(start, 0F);
        open.add(new float[] {h(from, goal), start});
        int expanded = 0;
        while (!open.isEmpty()) {
            float[] top = open.poll();
            int cur = (int) top[1];
            if (cur == end) return smooth(s, mode, unwind(parent, start, end, nx, nz));
            float gc = g.get(cur);
            if (top[0] - h(cell(cur, nx, nz), goal) > gc + 1e-3) continue; // stale entry
            if (++expanded > maxNodes) return null;
            int[] c = cell(cur, nx, nz);
            for (int[] n : neighbours(s, mode, c, cur == start)) {
                int ni = idx(n[0], n[1], n[2], nx, nz);
                float cost = gc + (float) Math.sqrt(sq(n[0] - c[0]) + sq(n[1] - c[1]) + sq(n[2] - c[2])) + (mode == Mode.WALK && n[1] > c[1] ? 0.5F : 0F);
                if (cost < g.get(ni)) {
                    g.put(ni, cost);
                    parent.put(ni, cur);
                    open.add(new float[] {cost + h(n, goal), ni});
                }
            }
        }
        return null;
    }

    private static boolean inside(Space s, int[] c) {
        return c[0] >= 0 && c[1] >= 0 && c[2] >= 0 && c[0] < s.nx() && c[1] < s.ny() && c[2] < s.nz();
    }

    /** The goal, or the nearest usable cell within two cells (null if none). */
    @Nullable
    private static int[] settle(Space s, Mode mode, int[] to) {
        int[] t = {Math.max(0, Math.min(s.nx() - 1, to[0])), Math.max(0, Math.min(s.ny() - 1, to[1])), Math.max(0, Math.min(s.nz() - 1, to[2]))};
        if (mode == Mode.WALK) {
            // drop onto the floor below the goal
            int y = t[1];
            while (y > 0 && free(s, t[0], y - 1, t[2]) && free(s, t[0], y, t[2])) y--;
            t[1] = y;
        }
        if (usable(s, mode, t)) return t;
        int[] best = null;
        int bestD = Integer.MAX_VALUE;
        for (int dy = -2; dy <= 2; dy++)
            for (int dz = -2; dz <= 2; dz++)
                for (int dx = -2; dx <= 2; dx++) {
                    int[] c = {t[0] + dx, t[1] + dy, t[2] + dz};
                    int d = dx * dx + dy * dy + dz * dz;
                    if (d < bestD && usable(s, mode, c)) {
                        best = c;
                        bestD = d;
                    }
                }
        return best;
    }

    private static boolean usable(Space s, Mode mode, int[] c) {
        return mode == Mode.SWIM ? free(s, c[0], c[1], c[2]) : standable(s, c[0], c[1], c[2]);
    }

    private static List<int[]> neighbours(Space s, Mode mode, int[] c, boolean fromStart) {
        List<int[]> out = new ArrayList<>(26);
        int x = c[0], y = c[1], z = c[2];
        if (mode == Mode.SWIM) {
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++)
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        if (!free(s, x + dx, y + dy, z + dz)) continue;
                        // no cutting of corners: every axis step on the way is free
                        if (dx != 0 && (dy != 0 || dz != 0) && !free(s, x + dx, y, z)) continue;
                        if (dy != 0 && (dx != 0 || dz != 0) && !free(s, x, y + dy, z)) continue;
                        if (dz != 0 && (dx != 0 || dy != 0) && !free(s, x, y, z + dz)) continue;
                        out.add(new int[] {x + dx, y + dy, z + dz});
                    }
            return out;
        }
        for (int dz = -1; dz <= 1; dz++)
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dz == 0) continue;
                if (dx != 0 && dz != 0 && (!free(s, x + dx, y, z) || !free(s, x, y, z + dz))) {
                    // a diagonal past a corner: only on the same level when both sides are open
                    continue;
                }
                int ax = x + dx, az = z + dz;
                if (standable(s, ax, y, az)) {
                    out.add(new int[] {ax, y, az});
                } else if (!free(s, ax, y, az)) {
                    // one cell up (needs headroom above the animal and above the step)
                    if (standable(s, ax, y + 1, az) && free(s, x, y + 1, z)) out.add(new int[] {ax, y + 1, az});
                } else {
                    // a drop
                    int yy = y;
                    while (yy > 0 && y - yy < MAX_DROP && free(s, ax, yy - 1, az)) yy--;
                    if (standable(s, ax, yy, az)) out.add(new int[] {ax, yy, az});
                }
            }
        // the start cell may be in the air or inside the decoration: fall / climb out
        if (fromStart && !standable(s, x, y, z)) {
            if (free(s, x, y, z) && y > 0) out.add(new int[] {x, y - 1, z});
            else if (free(s, x, y + 1, z)) out.add(new int[] {x, y + 1, z});
        }
        return out;
    }

    private static List<int[]> unwind(Int2IntOpenHashMap parent, int start, int end, int nx, int nz) {
        List<int[]> path = new ArrayList<>();
        int c = end;
        path.add(cell(c, nx, nz));
        while (c != start) {
            c = parent.get(c);
            path.add(cell(c, nx, nz));
        }
        Collections.reverse(path);
        return path;
    }

    /** Drops cells that can be skipped in a straight free line (swimming) or along a level stretch (walking). */
    private static List<int[]> smooth(Space s, Mode mode, List<int[]> path) {
        if (path.size() <= 2) return path;
        List<int[]> out = new ArrayList<>();
        out.add(path.get(0));
        int i = 0;
        while (i < path.size() - 1) {
            int j = path.size() - 1;
            while (j > i + 1 && !(mode == Mode.SWIM ? line(s, path.get(i), path.get(j)) : level(s, path, i, j))) j--;
            out.add(path.get(j));
            i = j;
        }
        return out;
    }

    /** Whether the straight line between two cell centres only crosses free cells. */
    public static boolean line(Space s, int[] a, int[] b) {
        double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        int steps = (int) Math.ceil(Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) * 3);
        for (int k = 1; k < steps; k++) {
            double f = k / (double) steps;
            // the centre line and a slightly wider tube (no grazing of corners)
            for (int o = 0; o < 3; o++) {
                double ox = o == 1 ? 0.3 : o == 2 ? -0.3 : 0, oz = o == 1 ? -0.3 : o == 2 ? 0.3 : 0;
                int x = (int) Math.floor(a[0] + 0.5 + dx * f + ox), y = (int) Math.floor(a[1] + 0.5 + dy * f), z = (int) Math.floor(a[2] + 0.5 + dz * f + oz);
                if (!free(s, x, y, z)) return false;
            }
        }
        return true;
    }

    /** Walking: the cells i..j are on one level and the straight line between them is standable all the way. */
    private static boolean level(Space s, List<int[]> path, int i, int j) {
        int[] a = path.get(i), b = path.get(j);
        for (int k = i; k <= j; k++) if (path.get(k)[1] != a[1]) return false;
        double dx = b[0] - a[0], dz = b[2] - a[2];
        int steps = (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dz)) * 3);
        for (int k = 1; k < steps; k++) {
            double f = k / (double) steps;
            int x = (int) Math.floor(a[0] + 0.5 + dx * f), z = (int) Math.floor(a[2] + 0.5 + dz * f);
            if (!standable(s, x, a[1], z)) return false;
        }
        return true;
    }

    private static float h(int[] a, int[] b) {
        return (float) Math.sqrt(sq(a[0] - b[0]) + sq(a[1] - b[1]) + sq(a[2] - b[2]));
    }

    private static int sq(int v) {
        return v * v;
    }

    private static int idx(int x, int y, int z, int nx, int nz) {
        return (y * nz + z) * nx + x;
    }

    private static int[] cell(int i, int nx, int nz) {
        int x = i % nx, rest = i / nx;
        return new int[] {x, rest / nz, rest % nz};
    }
}
