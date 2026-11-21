package org.overengineer.bettacraft.critters;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
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

    /** M21: the floor under an overhang or in a cave at the height y (see {@link CritterMovement.Terrain#groundAt}). */
    @Override
    public double groundAt(double x, double y, double z) {
        if (region == null || server == null || !Features.enabled(Feature.PATHFINDING)) return ground(x, z);
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        int ix = bx - minX, iz = bz - minZ;
        if (ix < 0 || iz < 0 || ix >= sx || iz >= sz) return Double.NaN;
        int low = lowest[iz * sx + ix];
        if (low == Integer.MAX_VALUE) return Double.NaN;
        double fx = (sx - 2 * MARGIN) / region.sizeX(), fy = (t.sizeY() - 2 * MARGIN) / region.sizeY(), fz = (sz - 2 * MARGIN) / region.sizeZ();
        int lx = (int) Math.floor((x - minX - MARGIN) / fx), lz = (int) Math.floor((z - minZ - MARGIN) / fz);
        lx = Math.max(0, Math.min(region.sizeX() - 1, lx));
        lz = Math.max(0, Math.min(region.sizeZ() - 1, lz));
        int from16 = (int) Math.floor((y - t.min().getY() - MARGIN) / fy * 16);
        int f16 = InteriorGround.floor16(server, region, lx, lz, from16);
        if (f16 < 0) return ground(x, z);
        double g = t.min().getY() + MARGIN + f16 / 16.0 * fy;
        return Math.max(low + MARGIN * 0.5, g);
    }

    /** M21: a way on the floor around the decoration ({@link org.overengineer.bettacraft.tank.interior.InteriorPaths#walk}). */
    @Override
    public java.util.List<double[]> walkPath(double x, double y, double z, double tx, double ty, double tz) {
        if (region == null || server == null) return null;
        var way = org.overengineer.bettacraft.tank.interior.InteriorPaths.walk(server, t, new net.minecraft.world.phys.Vec3(x, y, z),
                new net.minecraft.world.phys.Vec3(tx, ty, tz), surface);
        if (way == null) return null;
        java.util.List<double[]> out = new java.util.ArrayList<>(way.size());
        for (int i = 0; i < way.size(); i++) {
            var v = way.get(i);
            boolean last = i == way.size() - 1;
            double wx = last ? tx : v.x, wz = last ? tz : v.z;
            double g = groundAt(wx, v.y + 0.07, wz);
            out.add(new double[] {wx, Double.isFinite(g) ? g : v.y, wz});
        }
        return out;
    }

    /** M21: a spot on the floor with decoration or leaves above it (under an overhang, in a cave) near (x, z). */
    @Override
    public double[] shelter(double x, double y, double z, double reach, java.util.random.RandomGenerator rnd) {
        if (region == null || server == null || !Features.enabled(Feature.PATHFINDING)) return null;
        for (int i = 0; i < 16; i++) {
            double sx2 = x + (rnd.nextDouble() * 2 - 1) * reach, sz2 = z + (rnd.nextDouble() * 2 - 1) * reach;
            double top = ground(sx2, sz2);
            if (!Double.isFinite(top)) continue;
            // the floor under the rock: scan down from just above the animal (or from the top of the substrate)
            double g = groundAt(sx2, Math.min(top - 0.01, Math.max(y, lowestFloor(sx2, sz2)) + 0.12), sz2);
            if (!Double.isFinite(g) || top - g < 0.12) continue;
            return new double[] {sx2, g, sz2};
        }
        return null;
    }

    /** M56: a spot at the foot of a grown terrarium plant near (x, z) (snakes and geckos hide there), or null. */
    @Nullable
    public double[] plantShelter(double x, double z, double reach, java.util.random.RandomGenerator rnd) {
        if (region == null || server == null) return null;
        double[] s = org.overengineer.bettacraft.plants.TerrariumPlantLife.shelter(server, t, x, z, reach, rnd);
        if (s == null) return null;
        double g = ground(s[0], s[2]);
        if (Double.isFinite(g) && Math.abs(g - s[1]) < 0.15) s[1] = g; // on the ground (the plant's root sits on it)
        if (Double.isFinite(surface) && s[1] < surface - 0.01) return null; // not under water
        return s;
    }

    private double lowestFloor(double x, double z) {
        int ix = (int) Math.floor(x) - minX, iz = (int) Math.floor(z) - minZ;
        if (ix < 0 || iz < 0 || ix >= sx || iz >= sz) return t.min().getY();
        int low = lowest[iz * sx + ix];
        return low == Integer.MAX_VALUE ? t.min().getY() : low;
    }

    @Override
    public boolean lively() {
        return Features.enabled(Feature.LIVELY_MOVEMENT); // M54
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
