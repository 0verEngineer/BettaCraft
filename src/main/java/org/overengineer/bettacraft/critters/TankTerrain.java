package org.overengineer.bettacraft.critters;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.algae.AlgaeSim;
import org.overengineer.bettacraft.tank.interior.InteriorGround;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;

/**
 * The tank as the small animals see it (server): the floor of every column from the interior (substrate, decoration,
 * plants, the walls of a free shape, through {@link InteriorGround}) mapped like the miniature, or the bottom of the
 * lowest tank block when there is no interior; the water surface, the glass and the algae on it.
 */
public final class TankTerrain implements CritterMovement.Terrain {
    /** Gap between the glass and the miniature (as in {@code MiniatureRenderer}). */
    public static final double MARGIN = 0.5 / 16;

    private final TankData t;
    @Nullable
    private final MinecraftServer server;
    @Nullable
    private final InteriorRegion region;
    private final boolean day;
    private final double surface, airTop;
    private final int[] lowest;
    private final int minX, minZ, sx, sz;

    public TankTerrain(TankData t, @Nullable MinecraftServer server, @Nullable InteriorRegion region, boolean day) {
        this.t = t;
        this.server = server;
        this.region = region != null && region.built() ? region : null;
        this.day = day;
        this.surface = t.waterMb() <= 0 ? Double.NaN : t.waterSurfaceY();
        this.minX = t.min().getX();
        this.minZ = t.min().getZ();
        this.sx = t.sizeX();
        this.sz = t.sizeZ();
        // lowest tank block of every column (the floor without an interior)
        lowest = new int[sx * sz];
        java.util.Arrays.fill(lowest, Integer.MAX_VALUE);
        for (long l : t.members()) {
            int i = (BlockPos.getZ(l) - minZ) * sx + (BlockPos.getX(l) - minX);
            if (i >= 0 && i < lowest.length) lowest[i] = Math.min(lowest[i], BlockPos.getY(l));
        }
        this.airTop = org.overengineer.bettacraft.tank.TankFrogs.hasAir(t) && Double.isFinite(surface)
                ? Math.min(surface + 0.2, topAbove() - 0.02) : Double.NaN;
    }

    private double topAbove() {
        int layer = (int) Math.floor(surface - 1e-4);
        double top = layer + 1;
        for (long l : t.members()) {
            if (BlockPos.getY(l) != layer) continue;
            int x = BlockPos.getX(l), z = BlockPos.getZ(l), y = layer;
            while (t.contains(BlockPos.asLong(x, y + 1, z))) y++;
            top = Math.max(top, y + 1);
        }
        return top;
    }

    @Override
    public double ground(double x, double z) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        int ix = bx - minX, iz = bz - minZ;
        if (ix < 0 || iz < 0 || ix >= sx || iz >= sz) return Double.NaN;
        int low = lowest[iz * sx + ix];
        if (low == Integer.MAX_VALUE) return Double.NaN;
        if (region == null || server == null) return low + MARGIN;
        double fx = (sx - 2 * MARGIN) / region.sizeX(), fy = (t.sizeY() - 2 * MARGIN) / region.sizeY(), fz = (sz - 2 * MARGIN) / region.sizeZ();
        int lx = (int) Math.floor((x - minX - MARGIN) / fx), lz = (int) Math.floor((z - minZ - MARGIN) / fz);
        lx = Math.max(0, Math.min(region.sizeX() - 1, lx));
        lz = Math.max(0, Math.min(region.sizeZ() - 1, lz));
        int top16 = InteriorGround.top16(server, region, lx, lz);
        double y = t.min().getY() + MARGIN + top16 / 16.0 * fy;
        return Math.max(low + MARGIN * 0.5, y);
    }

    @Override
    public double surface() {
        return surface;
    }

    @Override
    public boolean member(int x, int y, int z) {
        return t.contains(BlockPos.asLong(x, y, z));
    }

    @Override
    public double algae(int x, int y, int z, int dir) {
        return AlgaeSim.get(t.algae(), BlockPos.asLong(x, y, z), dir);
    }

    @Override
    public boolean day() {
        return day;
    }

    @Override public int minX() { return t.min().getX(); }
    @Override public int minY() { return t.min().getY(); }
    @Override public int minZ() { return t.min().getZ(); }
    @Override public int maxX() { return t.max().getX(); }
    @Override public int maxY() { return t.max().getY(); }
    @Override public int maxZ() { return t.max().getZ(); }

    @Override
    public double airTop() {
        return airTop;
    }
}
