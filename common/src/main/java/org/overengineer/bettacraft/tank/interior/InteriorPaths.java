package org.overengineer.bettacraft.tank.interior;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.util.GridPathfinder;

/**
 * M21: path finding in a tank (server) on the interior voxels, coarsened to cells of two tank pixels
 * ({@link InteriorObstacles#fineCell}): solid where the interior has decoration with a collision shape or aquarium
 * plant parts that are obstacles, and outside the tank's shape. Used by the dwarf frogs (they walked into the
 * decoration and got stuck), the small animals (under overhangs and into caves) and the pleco males (into their
 * spawning cave instead of only to its mouth). Positions are tank-world coordinates like {@link InteriorObstacles}.
 * Without an interior (or with {@code features.interior.pathfinding} off) every call returns null and the animals move
 * in straight lines as before.
 */
public final class InteriorPaths {
    /** Expanded cells per search at most (a 4x3x4 tank has 32x24x32 cells; a search is usually a few hundred). */
    public static final int MAX_NODES = 8000;

    private InteriorPaths() {}

    /** The grid of one tank interior in tank-world coordinates. */
    public static final class Grid implements GridPathfinder.Space {
        final ServerLevel interior;
        final InteriorRegion r;
        final int cell, nx, ny, nz;
        final double minX, minY, minZ, size;
        final int maxCy;
        /** Distance (interior blocks) a swimmer keeps from the glass, and whether it keeps a cell from the decoration. */
        int margin;
        boolean inflate;
        /** Around the goal (a cave mouth) the clearance from the decoration is not needed: within this many cells. */
        int[] relaxAt;
        int relax = 3;

        Grid(ServerLevel interior, InteriorRegion r, TankData tank, double topY) {
            this.interior = interior;
            this.r = r;
            this.cell = InteriorObstacles.fineCell(r);
            this.nx = (r.sizeX() + cell - 1) / cell;
            this.ny = (r.sizeY() + cell - 1) / cell;
            this.nz = (r.sizeZ() + cell - 1) / cell;
            BlockPos min = tank.min();
            this.minX = min.getX();
            this.minY = min.getY();
            this.minZ = min.getZ();
            this.size = cell / (double) Math.max(1, r.scale());
            this.maxCy = Double.isFinite(topY) ? Math.min(ny - 1, (int) Math.floor((topY - minY) / size)) : ny - 1;
        }

        @Override public int nx() { return nx; }
        @Override public int ny() { return maxCy + 1; }
        @Override public int nz() { return nz; }

        @Override
        public boolean solid(int x, int y, int z) {
            if (base(x, y, z)) return true;
            if (inflate && (relaxAt == null || Math.max(Math.abs(x - relaxAt[0]), Math.max(Math.abs(y - relaxAt[1]), Math.abs(z - relaxAt[2]))) > relax)
                    && (base(x - 1, y, z) || base(x + 1, y, z) || base(x, y, z - 1) || base(x, y, z + 1))) return true;
            if (margin > 0) {
                int c = cell, lx = x * c + c / 2, ly = Math.min(y * c + c / 2, r.sizeY() - 1), lz = z * c + c / 2;
                for (int i = 0; i < 4; i++) {
                    int ox = (i & 1) == 0 ? -margin : margin, oz = (i & 2) == 0 ? -margin : margin;
                    if (!r.inShape(lx + ox, ly, lz + oz) || lx + ox >= r.sizeX() || lz + oz >= r.sizeZ()) return true;
                }
            }
            return false;
        }

        private boolean base(int x, int y, int z) {
            if (x < 0 || z < 0 || x >= nx || z >= nz) return true;
            int c = cell, lx = x * c + c / 2, ly = y * c + c / 2, lz = z * c + c / 2;
            if (!r.inShape(Math.min(lx, r.sizeX() - 1), Math.min(ly, r.sizeY() - 1), Math.min(lz, r.sizeZ() - 1))) return true;
            return InteriorObstacles.fineSolid(interior, r, x, y, z);
        }

        public int[] cellOf(Vec3 p) {
            return new int[] {clamp((int) Math.floor((p.x - minX) / size), nx), clamp((int) Math.floor((p.y - minY) / size), maxCy + 1),
                    clamp((int) Math.floor((p.z - minZ) / size), nz)};
        }

        private static int clamp(int v, int n) {
            return Math.max(0, Math.min(n - 1, v));
        }

        /** Centre of a cell (tank-world). */
        public Vec3 centre(int[] c) {
            return new Vec3(minX + (c[0] + 0.5) * size, minY + (c[1] + 0.5) * size, minZ + (c[2] + 0.5) * size);
        }

        /** The bottom of a cell at its centre (where a walker stands). */
        public Vec3 floor(int[] c) {
            return new Vec3(minX + (c[0] + 0.5) * size, minY + c[1] * size, minZ + (c[2] + 0.5) * size);
        }

        public double cellSize() {
            return size;
        }
    }

    /** The grid of a tank, or null without a built interior or with the feature off. */
    @Nullable
    public static Grid grid(MinecraftServer server, TankData tank, double topY) {
        if (!Features.enabled(Feature.PATHFINDING) || server == null) return null;
        InteriorRegion r = InteriorRegions.get(server).get(tank.regionId());
        if (r == null || !r.built()) return null;
        ServerLevel interior = InteriorManager.interiorLevel(server);
        if (interior == null) return null;
        return new Grid(interior, r, tank, topY);
    }

    /**
     * A swimming path from {@code from} to {@code to} below {@code topY} (the water surface; NaN = no limit): the waypoints
     * after the start, the last one exactly {@code to}; null when there is no interior or no way.
     */
    @Nullable
    public static List<Vec3> swim(MinecraftServer server, TankData tank, Vec3 from, Vec3 to, double topY) {
        return swim(server, tank, from, to, topY, 0, false);
    }

    /**
     * Like {@link #swim(MinecraftServer, TankData, Vec3, Vec3, double)} for an animal that keeps {@code margin} (blocks)
     * from the glass and, with {@code inflate}, a cell from the decoration (it does not cut corners); without a way with
     * that clearance the plain way.
     */
    @Nullable
    public static List<Vec3> swim(MinecraftServer server, TankData tank, Vec3 from, Vec3 to, double topY, double margin, boolean inflate) {
        Grid g = grid(server, tank, topY);
        if (g == null) return null;
        if (margin > 0 || inflate) {
            g.margin = (int) Math.ceil(margin * Math.max(1, g.r.scale()));
            g.inflate = inflate;
            g.relaxAt = g.cellOf(to);
            List<Vec3> wide = swim(g, from, to);
            if (wide != null) return wide;
            g.margin = 0;
            g.inflate = false;
        }
        return swim(g, from, to);
    }

    @Nullable
    private static List<Vec3> swim(Grid g, Vec3 from, Vec3 to) {
        // an animal on the substrate stands in its cells: start and goal rise out of solid cells (up to a block)
        List<int[]> cells = GridPathfinder.find(g, GridPathfinder.Mode.SWIM, rise(g, g.cellOf(from)), rise(g, g.cellOf(to)), MAX_NODES);
        if (cells == null) return null;
        List<Vec3> out = new ArrayList<>(cells.size());
        for (int i = 1; i < cells.size() - 1; i++) out.add(g.centre(cells.get(i)));
        out.add(to);
        return out;
    }

    private static int[] rise(Grid g, int[] c) {
        int limit = Math.min(g.maxCy, c[1] + Math.max(1, (int) Math.round(1 / g.size)));
        while (c[1] < limit && g.base(c[0], c[1], c[2])) c[1]++;
        return c;
    }

    /** A walking path on the floor (small animals): waypoints after the start at floor height, or null. */
    @Nullable
    public static List<Vec3> walk(MinecraftServer server, TankData tank, Vec3 from, Vec3 to, double topY) {
        Grid g = grid(server, tank, topY);
        if (g == null) return null;
        List<int[]> cells = GridPathfinder.find(g, GridPathfinder.Mode.WALK, g.cellOf(from.add(0, 0.01, 0)), g.cellOf(to.add(0, 0.01, 0)), MAX_NODES);
        if (cells == null) return null;
        List<Vec3> out = new ArrayList<>(cells.size());
        for (int i = 1; i < cells.size(); i++) out.add(g.floor(cells.get(i)));
        return out;
    }

    /** Whether a swimmer can go straight from a to b (the path grid's line of sight); true without an interior. */
    public static boolean straight(MinecraftServer server, TankData tank, Vec3 a, Vec3 b) {
        return straight(server, tank, a, b, 0, false);
    }

    /** Line of sight with the clearance of {@link #swim(MinecraftServer, TankData, Vec3, Vec3, double, double, boolean)}. */
    public static boolean straight(MinecraftServer server, TankData tank, Vec3 a, Vec3 b, double margin, boolean inflate) {
        Grid g = grid(server, tank, Double.NaN);
        if (g != null) {
            g.margin = (int) Math.ceil(margin * Math.max(1, g.r.scale()));
            g.inflate = inflate;
        }
        return g == null || GridPathfinder.line(g, rise(g, g.cellOf(a)), rise(g, g.cellOf(b)));
    }
}
