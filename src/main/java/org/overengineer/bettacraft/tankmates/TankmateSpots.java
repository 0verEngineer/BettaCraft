package org.overengineer.bettacraft.tankmates;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.critters.TankTerrain;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;

/**
 * M19: places in a tank where the tankmates spawn: spawning caves and spawning mops of the interior (clusters of their
 * blocks), fine-leaved and broad-leaved aquarium plants, the inside of the glass and the ground. Interior positions are
 * turned into tank-world positions like the miniature ({@link TankTerrain#MARGIN}).
 */
public final class TankmateSpots {
    /** Smallest cluster of spawning cave blocks that counts as a cave. */
    public static final int CAVE_MIN_BLOCKS = 20;

    /** A spot in tank-world coordinates; for a cave: inside it near the mouth, {@code dir} pointing out of the mouth. */
    public record Spot(Vec3 pos, int blocks, Vec3 dir) {
        public Spot(Vec3 pos, int blocks) {
            this(pos, blocks, Vec3.ZERO);
        }
    }

    private record Cache(int region, int version, List<Spot> caves, List<Spot> mops, List<Spot> smallCaves) {}

    /** M29: smallest cluster of small cave blocks (coconut shell, slate cave) that counts as a cave for the dwarf cichlids. */
    public static final int SMALL_CAVE_MIN_BLOCKS = 12;

    private static final java.util.Map<Integer, Cache> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private TankmateSpots() {}

    @Nullable
    public static InteriorRegion region(MinecraftServer server, TankData t) {
        InteriorRegion r = InteriorRegions.get(server).get(t.regionId());
        return r != null && r.built() ? r : null;
    }

    /** Tank-world position of the centre of an interior block. */
    public static Vec3 toWorld(TankData t, InteriorRegion r, double lx, double ly, double lz) {
        double m = TankTerrain.MARGIN;
        double fx = (t.sizeX() - 2 * m) / r.sizeX(), fy = (t.sizeY() - 2 * m) / r.sizeY(), fz = (t.sizeZ() - 2 * m) / r.sizeZ();
        return new Vec3(t.min().getX() + m + lx * fx, t.min().getY() + m + ly * fy, t.min().getZ() + m + lz * fz);
    }

    /** Spawning caves: clusters of at least {@link #CAVE_MIN_BLOCKS} cave blocks, at their centre (inside the tube). */
    public static List<Spot> caves(MinecraftServer server, TankData t) {
        Cache c = cache(server, t);
        return c == null ? List.of() : c.caves;
    }

    /**
     * M29: the caves of the cave brooding cichlids: the small caves (coconut shell, slate cave: clusters of
     * {@code #bettacraft:small_caves} blocks, at the middle of their floor) first, then the spawning caves of the plecos.
     */
    public static List<Spot> cichlidCaves(MinecraftServer server, TankData t) {
        Cache c = cache(server, t);
        if (c == null) return List.of();
        List<Spot> out = new ArrayList<>(c.smallCaves);
        out.addAll(c.caves);
        return out;
    }

    private static List<Spot> smallCaves(TankData t, InteriorRegion r, LongOpenHashSet blocks) {
        List<Spot> out = new ArrayList<>();
        for (Spot s : clusters(t, r, blocks, SMALL_CAVE_MIN_BLOCKS, false)) out.add(s);
        // the spot of a shell or slate cave: the middle of its floor (inside, under the roof), not the middle of its blocks
        List<Spot> floor = new ArrayList<>();
        for (Spot s : out) {
            double fy = (t.sizeY() - 2 * TankTerrain.MARGIN) / r.sizeY();
            floor.add(new Spot(new Vec3(s.pos().x, Math.max(t.min().getY() + TankTerrain.MARGIN + fy, s.pos().y - 1.0 * fy), s.pos().z), s.blocks(), Vec3.ZERO));
        }
        return floor;
    }

    /** Spawning mops: clusters of mop strands, at their centre, with their size. */
    public static List<Spot> mops(MinecraftServer server, TankData t) {
        Cache c = cache(server, t);
        return c == null ? List.of() : c.mops;
    }

    @Nullable
    private static Cache cache(MinecraftServer server, TankData t) {
        InteriorRegion r = region(server, t);
        if (r == null) return null;
        var eq = InteriorManager.equipment(server, r);
        Cache c = CACHE.get(r.id());
        if (c != null && c.version == eq.spawningVersion() && c.region == r.id()) return c;
        c = new Cache(r.id(), eq.spawningVersion(), clusters(t, r, eq.caveBlocks(), CAVE_MIN_BLOCKS, true), clusters(t, r, eq.mopBlocks(), 3, false),
                smallCaves(t, r, eq.smallCaveBlocks()));
        CACHE.put(r.id(), c);
        return c;
    }

    public static void forget(int regionId) {
        CACHE.remove(regionId);
    }

    private static List<Spot> clusters(TankData t, InteriorRegion r, LongOpenHashSet blocks, int min, boolean cave) {
        List<Spot> out = new ArrayList<>();
        if (blocks.isEmpty()) return out;
        LongOpenHashSet seen = new LongOpenHashSet();
        LongArrayFIFOQueue q = new LongArrayFIFOQueue();
        for (long start : blocks) {
            if (!seen.add(start)) continue;
            q.enqueue(start);
            double sx = 0, sy = 0, sz = 0;
            int n = 0;
            List<Long> members = new ArrayList<>();
            while (!q.isEmpty()) {
                long l = q.dequeueLong();
                members.add(l);
                int x = BlockPos.getX(l), y = BlockPos.getY(l), z = BlockPos.getZ(l);
                sx += x + 0.5;
                sy += y + 0.5;
                sz += z + 0.5;
                n++;
                for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                    long nb = BlockPos.asLong(x + dx, y + dy, z + dz);
                    if (blocks.contains(nb) && seen.add(nb)) q.enqueue(nb);
                }
            }
            if (n < min) continue;
            if (cave) out.add(caveSpot(t, r, members, sx / n, sy / n, sz / n));
            else out.add(new Spot(toWorld(t, r, sx / n, sy / n, sz / n), n));
        }
        return out;
    }

    /**
     * The spot of a cave: along its long axis, the end with fewer blocks is the open mouth; the spot lies a third of
     * the length inside from the mouth, in the middle of the tube (just above its floor).
     */
    private static Spot caveSpot(TankData t, InteriorRegion r, List<Long> blocks, double cx, double cy, double cz) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE, minY = Integer.MAX_VALUE;
        for (long l : blocks) {
            minX = Math.min(minX, BlockPos.getX(l));
            maxX = Math.max(maxX, BlockPos.getX(l));
            minZ = Math.min(minZ, BlockPos.getZ(l));
            maxZ = Math.max(maxZ, BlockPos.getZ(l));
            minY = Math.min(minY, BlockPos.getY(l));
        }
        boolean alongX = maxX - minX >= maxZ - minZ;
        int lo = 0, hi = 0;
        for (long l : blocks) {
            int a = alongX ? BlockPos.getX(l) : BlockPos.getZ(l);
            if (a == (alongX ? minX : minZ)) lo++;
            if (a == (alongX ? maxX : maxZ)) hi++;
        }
        boolean openLow = lo <= hi;
        double len = alongX ? maxX - minX + 1 : maxZ - minZ + 1;
        double mouth = openLow ? (alongX ? minX : minZ) : (alongX ? maxX + 1 : maxZ + 1);
        double inside = mouth + (openLow ? 1 : -1) * len * 0.35;
        double lx = alongX ? inside : (minX + maxX + 1) / 2.0, lz = alongX ? (minZ + maxZ + 1) / 2.0 : inside;
        Vec3 p = toWorld(t, r, lx, minY + 1.0, lz);
        Vec3 dir = alongX ? new Vec3(openLow ? -1 : 1, 0, 0) : new Vec3(0, 0, openLow ? -1 : 1);
        return new Spot(p, blocks.size(), dir);
    }

    /** Fine-leaved plants (stem plants, moss, needle leaves) as tank-world spots with their blocks. */
    public static List<Spot> plants(MinecraftServer server, TankData t, boolean broad) {
        InteriorRegion r = region(server, t);
        List<Spot> out = new ArrayList<>();
        if (r == null) return out;
        for (InteriorPlant p : r.plants().all()) {
            if (p.stage < 1 || (broad ? !isBroad(p.species) : !isFine(p.species))) continue;
            double h = Math.max(1, Math.min(p.blocks, 12)) * 0.5;
            out.add(new Spot(toWorld(t, r, p.x + 0.5, p.y + h, p.z + 0.5), p.blocks));
        }
        return out;
    }

    public static boolean isFine(org.overengineer.bettacraft.plants.PlantSpecies s) {
        String leaf = s.leafTexture();
        return "fine".equals(leaf) || "moss".equals(leaf) || "needle".equals(leaf);
    }

    public static boolean isBroad(org.overengineer.bettacraft.plants.PlantSpecies s) {
        String leaf = s.leafTexture();
        return "broad".equals(leaf) || "wavy".equals(leaf) || "frond".equals(leaf);
    }

    /**
     * M29: where an open brooding cichlid pair spawns, near {@code near}: rams on a flat stone (the top of a raised, level
     * piece of decoration in the lower half of the water) or else in a pit in the substrate; angelfish and discus on a
     * broad leaf (sword plants, anubias) or else on the glass. Returns {x, y, z, face} (face -1 off the glass, 0..3 on it)
     * and whether it is a stone or leaf in {@code out[4]} (1) or the bare floor (0).
     */
    public static double[] openSpot(MinecraftServer server, TankData t, TankmateSpecies s, Vec3 near, RandomSource r) {
        double[] out = new double[5];
        out[3] = -1;
        TankWaterVolume w = new TankWaterVolume(t);
        var box = w.box();
        if (s.zone() == TankmateSpecies.Zone.MID) {
            // angelfish, discus: a broad leaf, else the glass
            Spot best = null;
            for (Spot p : plants(server, t, true)) if (best == null || p.pos().distanceToSqr(near) < best.pos().distanceToSqr(near)) best = p;
            if (best != null) {
                Vec3 p = w.clamp(best.pos(), 0.06, 0.06, 0.06);
                out[0] = p.x;
                out[1] = p.y;
                out[2] = p.z;
                out[4] = 1;
                return out;
            }
            double[] g = new double[4];
            if (glassSpot(t, w, r, g) != null) {
                out[0] = g[0];
                out[1] = Math.max(g[1], box.minY + (w.surface() - box.minY) * 0.3);
                out[2] = g[2];
                out[3] = g[3];
                out[4] = 1;
                return out;
            }
        }
        InteriorRegion region = region(server, t);
        TankTerrain terrain = new TankTerrain(t, server, region, true);
        double floor = Double.MAX_VALUE;
        List<double[]> samples = new ArrayList<>();
        double step = 0.125;
        for (double x = box.minX + 0.1; x <= box.maxX - 0.1; x += step) {
            for (double z = box.minZ + 0.1; z <= box.maxZ - 0.1; z += step) {
                double g = terrain.ground(x, z);
                if (!Double.isFinite(g)) continue;
                floor = Math.min(floor, g);
                samples.add(new double[]{x, g, z});
            }
        }
        if (s.zone() != TankmateSpecies.Zone.MID && floor < Double.MAX_VALUE) {
            double top = floor + (w.surface() - floor) * 0.5;
            double[] best = null;
            for (double[] p : samples) {
                if (p[1] < floor + 0.05 || p[1] > top) continue;
                boolean flat = true;
                for (int d = 0; d < 4 && flat; d++) {
                    double nx = p[0] + (d == 0 ? 0.06 : d == 1 ? -0.06 : 0), nz = p[2] + (d == 2 ? 0.06 : d == 3 ? -0.06 : 0);
                    double ng = terrain.ground(nx, nz);
                    flat = Double.isFinite(ng) && Math.abs(ng - p[1]) <= 0.03;
                }
                if (!flat) continue;
                if (best == null || dist2(p, near) < dist2(best, near)) best = p;
            }
            if (best != null) {
                out[0] = best[0];
                out[1] = best[1] + 0.01;
                out[2] = best[2];
                out[4] = 1;
                return out;
            }
        }
        // a pit in the substrate where the pair is
        Vec3 p = w.clamp(near, 0.12, 0.05, 0.12);
        double g = terrain.ground(p.x, p.z);
        out[0] = p.x;
        out[1] = (Double.isFinite(g) ? g : box.minY) + 0.01;
        out[2] = p.z;
        return out;
    }

    private static double dist2(double[] p, Vec3 v) {
        double dx = p[0] - v.x, dy = p[1] - v.y, dz = p[2] - v.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Plant blocks per water block (share of the volume) of one leaf kind. */
    /**
     * M20: host anemones of the clownfish (bubble tip anemones grown past the planted frag), each at the middle of its
     * tentacles in tank-world coordinates; the eggs go to its foot.
     */
    public static List<Spot> anemones(MinecraftServer server, TankData t) {
        InteriorRegion r = region(server, t);
        if (r == null) return List.of();
        List<Spot> out = new ArrayList<>();
        for (InteriorPlant p : r.plants().all()) {
            if (!p.species.anemone() || p.stage < 1) continue;
            double up = Math.max(2, 7.0 * r.scale() / 16);
            out.add(new Spot(toWorld(t, r, p.x + 0.5, p.y + up, p.z + 0.5), p.blocks, Vec3.ZERO));
        }
        return out;
    }

    /** M20: the foot of a host anemone (where the clownfish glue their eggs) for the spot of {@link #anemones}. */
    public static Vec3 anemoneFoot(MinecraftServer server, TankData t, Vec3 spot) {
        InteriorRegion r = region(server, t);
        if (r == null) return spot;
        double up = Math.max(2, 7.0 * r.scale() / 16);
        double fy = (t.sizeY() - 2 * TankTerrain.MARGIN) / r.sizeY();
        return new Vec3(spot.x + 0.06, spot.y - (up - 0.6) * fy, spot.z);
    }

    public static double plantShare(MinecraftServer server, TankData t, boolean broad) {
        InteriorRegion r = region(server, t);
        if (r == null) return 0;
        int n = 0;
        for (InteriorPlant p : r.plants().all()) if (broad ? isBroad(p.species) : isFine(p.species)) n += p.blocks;
        return n / Math.pow(r.scale(), 3) / Math.max(1.0, t.waterBlocks());
    }

    /**
     * A point on the inside of a side wall under water (for corydoras eggs), in the lower two thirds; face = 0..3
     * (N, S, W, E). Null if the tank has no wet wall.
     */
    @Nullable
    public static Clutch.Kind glassSpot(TankData t, TankWaterVolume w, RandomSource r, double[] out) {
        List<Long> blocks = new ArrayList<>();
        for (long l : t.members()) if (BlockPos.getY(l) + 0.1 < w.surface()) blocks.add(l);
        if (blocks.isEmpty()) return null;
        for (int attempt = 0; attempt < 24; attempt++) {
            long l = blocks.get(r.nextInt(blocks.size()));
            int dir = r.nextInt(4);
            int x = BlockPos.getX(l), y = BlockPos.getY(l), z = BlockPos.getZ(l);
            int nx = x + (dir == 3 ? 1 : dir == 2 ? -1 : 0), nz = z + (dir == 1 ? 1 : dir == 0 ? -1 : 0);
            if (t.contains(BlockPos.asLong(nx, y, nz))) continue;
            double top = Math.min(y + 1.0, w.surface()) - 0.1;
            double bottom = y + (y == t.min().getY() ? 0.08 : 0.0);
            if (top <= bottom) continue;
            double py = bottom + r.nextDouble() * (top - bottom) * 0.7;
            double in = 1.0 / 16 + 0.004, along = 0.15 + r.nextDouble() * 0.7;
            out[0] = dir == 2 ? x + in : dir == 3 ? x + 1 - in : x + along;
            out[1] = py;
            out[2] = dir == 0 ? z + in : dir == 1 ? z + 1 - in : z + along;
            out[3] = dir;
            return Clutch.Kind.GLASS;
        }
        return null;
    }
}
