package org.overengineer.bettacraft.tankmates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.TankTerrain;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.food.BettaFood;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankWaterVolume;

/**
 * M21.8: what makes a seahorse a seahorse in a Bettacraft tank:
 * <ul>
 *     <li><b>Hitching posts</b>: plants, corals and decoration that stick up from the substrate of the tank interior (the
 *     interior height map, {@link TankTerrain#ground}); a seahorse curls its tail around one and stays there most of the
 *     time. Without any it lies on the bottom and is stressed ({@code seahorses.needHitchingPosts}).</li>
 *     <li><b>Low flow</b>: water movement ({@link org.overengineer.bettacraft.reef.TankReef#flow}: pumps, filters) above
 *     {@code seahorses.maxFlow} stresses them.</li>
 *     <li><b>Food</b>: only live food (mosquito larvae) and artemia nauplii, also for the fry from birth; no pellets, wafers
 *     or infusoria. They need it often ({@code seahorses.feedingsPerDay}) and are slow eaters next to fast fish.</li>
 *     <li><b>Tankmates</b>: fast or pushy reef fish (tangs, damselfish, chromis, clownfish) harass them.</li>
 * </ul>
 */
public final class Seahorses {
    /** A post sticks up at least this far (blocks) above the substrate. */
    public static final double POST_HEIGHT = 0.16;
    private static final double STEP = 0.125;
    private static final int MAX_SPOTS = 96;
    private static final long CACHE_TICKS = 200, FLOW_TICKS = 100;

    /** A place to hitch: where the seahorse stays ({@code pos}, its feet) and the centre of the post column it holds on to. */
    public record Spot(Vec3 pos, Vec3 post) {}

    private record Cache(long time, List<Spot> spots) {}

    private record FlowCache(long time, double flow) {}

    private static final Map<Long, Cache> SPOTS = new ConcurrentHashMap<>();
    private static final Map<Long, FlowCache> FLOW = new ConcurrentHashMap<>();

    private Seahorses() {}

    public static double maxFlow() {
        return ServerConfig.getOr(ServerConfig.SEAHORSE_MAX_FLOW, 0.6);
    }

    public static double pouchDays() {
        return ServerConfig.getOr(ServerConfig.SEAHORSE_POUCH_DAYS, 4.0);
    }

    // ------------------------------------------------------------------ hitching posts

    /** The hitching spots of a tank (cached for 10 s; empty without a built interior). */
    public static List<Spot> spots(ServerLevel level, TankData t) {
        long key = t.min().asLong() ^ ((long) t.regionId() << 40);
        long now = level.getGameTime();
        Cache c = SPOTS.get(key);
        if (c != null && now - c.time >= 0 && now - c.time < CACHE_TICKS) return c.spots;
        if (SPOTS.size() > 256) SPOTS.clear();
        List<Spot> s = compute(level, t);
        SPOTS.put(key, new Cache(now, s));
        return s;
    }

    /** GameTests: forget the cached posts (after building decoration). */
    public static void forget() {
        SPOTS.clear();
        FLOW.clear();
    }

    private static List<Spot> compute(ServerLevel level, TankData t) {
        if (t.waterMb() <= 0) return List.of();
        var region = TankmateSpots.region(level.getServer(), t);
        if (region == null) return List.of();
        TankTerrain terrain = new TankTerrain(t, level.getServer(), region, level.isDay());
        TankWaterVolume w = new TankWaterVolume(t);
        var box = w.box();
        double surface = w.surface();
        int nx = Math.max(1, (int) Math.floor((box.maxX - box.minX) / STEP)), nz = Math.max(1, (int) Math.floor((box.maxZ - box.minZ) / STEP));
        double[] g = new double[nx * nz];
        List<Double> valid = new ArrayList<>();
        for (int i = 0; i < nx; i++) {
            for (int k = 0; k < nz; k++) {
                double x = box.minX + (i + 0.5) * STEP, z = box.minZ + (k + 0.5) * STEP;
                double v = w.contains(new Vec3(x, Math.min(surface - 0.05, box.minY + 0.1), z)) ? terrain.ground(x, z) : Double.NaN;
                // vanilla seagrass, kelp and corals have no collision (not in the height map) but are fine posts too
                if (Double.isFinite(v)) v = Math.max(v, vanillaTop(level, t, region, x, z));
                g[i * nz + k] = v;
                if (Double.isFinite(v)) valid.add(v);
            }
        }
        if (valid.isEmpty()) return List.of();
        double[] sorted = valid.stream().mapToDouble(Double::doubleValue).toArray();
        Arrays.sort(sorted);
        double base = sorted[(int) Math.floor(sorted.length * 0.25)];
        List<Spot> out = new ArrayList<>();
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int i = 0; i < nx; i++) {
            for (int k = 0; k < nz; k++) {
                double top = g[i * nz + k];
                if (!Double.isFinite(top) || top - base < POST_HEIGHT || top > surface + 0.5) continue;
                double x = box.minX + (i + 0.5) * STEP, z = box.minZ + (k + 0.5) * STEP;
                for (int[] d : dirs) {
                    int ni = i + d[0], nk = k + d[1];
                    if (ni < 0 || nk < 0 || ni >= nx || nk >= nz) continue;
                    double side = g[ni * nz + nk];
                    if (!Double.isFinite(side) || side > top - POST_HEIGHT * 0.75) continue;
                    // beside the post, the tail at about its middle height
                    double y = Math.max(side + 0.02, Math.min(top - 0.08, base + (top - base) * 0.45));
                    if (y > surface - 0.2) continue;
                    Vec3 p = new Vec3(x + d[0] * STEP, y, z + d[1] * STEP);
                    out.add(new Spot(p, new Vec3(x, y, z)));
                }
            }
        }
        if (out.size() > MAX_SPOTS) {
            // an even spread over the tank
            List<Spot> thin = new ArrayList<>();
            double stride = out.size() / (double) MAX_SPOTS;
            for (int i = 0; i < MAX_SPOTS; i++) thin.add(out.get((int) Math.floor(i * stride)));
            out = thin;
        }
        return List.copyOf(out);
    }

    /** Top (tank-world y) of vanilla seagrass, kelp or corals in the interior column under x/z, or -inf. */
    private static double vanillaTop(ServerLevel level, TankData t, org.overengineer.bettacraft.tank.interior.InteriorRegion r, double x, double z) {
        ServerLevel in = org.overengineer.bettacraft.tank.interior.InteriorManager.interiorLevel(level.getServer());
        if (in == null) return Double.NEGATIVE_INFINITY;
        double m = TankTerrain.MARGIN;
        double fx = (t.sizeX() - 2 * m) / r.sizeX(), fy = (t.sizeY() - 2 * m) / r.sizeY(), fz = (t.sizeZ() - 2 * m) / r.sizeZ();
        int lx = (int) Math.floor((x - t.min().getX() - m) / fx), lz = (int) Math.floor((z - t.min().getZ() - m) / fz);
        if (lx < 0 || lz < 0 || lx >= r.sizeX() || lz >= r.sizeZ()) return Double.NEGATIVE_INFINITY;
        var pos = r.toWorld(lx, 0, lz);
        var chunk = in.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return Double.NEGATIVE_INFINITY;
        net.minecraft.core.BlockPos.MutableBlockPos p = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int ly = r.sizeY() - 1; ly >= 0; ly--) {
            p.set(pos.getX(), pos.getY() + ly, pos.getZ());
            var st = chunk.getBlockState(p);
            if (st.isAir()) continue;
            if (st.is(net.minecraft.world.level.block.Blocks.SEAGRASS) || st.is(net.minecraft.world.level.block.Blocks.TALL_SEAGRASS)
                    || st.is(net.minecraft.world.level.block.Blocks.KELP) || st.is(net.minecraft.world.level.block.Blocks.KELP_PLANT)
                    || st.is(net.minecraft.tags.BlockTags.CORALS)) {
                return t.min().getY() + m + (ly + 1) * fy;
            }
        }
        return Double.NEGATIVE_INFINITY;
    }

    /** The tank has at least one hitching post. */
    public static boolean hasPosts(ServerLevel level, TankData t) {
        return !spots(level, t).isEmpty();
    }

    // ------------------------------------------------------------------ flow

    /** The water movement of the tank (cached for 5 s). */
    public static double flow(ServerLevel level, TankBlockEntity c) {
        long key = c.getBlockPos().asLong();
        long now = level.getGameTime();
        FlowCache f = FLOW.get(key);
        if (f != null && now - f.time >= 0 && now - f.time < FLOW_TICKS) return f.flow;
        if (FLOW.size() > 256) FLOW.clear();
        double v = org.overengineer.bettacraft.reef.TankReef.flow(level, c);
        FLOW.put(key, new FlowCache(now, v));
        return v;
    }

    public static boolean flowTooStrong(double flow) {
        double max = maxFlow();
        return max > 0 && flow > max + 1e-9;
    }

    // ------------------------------------------------------------------ food

    /** Seahorses eat live food (juveniles and older) and artemia nauplii (every stage, the fry from birth). */
    public static boolean canEat(LifeStage stage, @Nullable BettaFood.Kind kind) {
        if (kind == null || !stage.isEntity()) return false;
        if (kind == BettaFood.Kind.FRY) return true;
        return kind == BettaFood.Kind.LIVE && stage != LifeStage.FRY;
    }

    /** Health parameters of a seahorse: hungry after a share of a day ({@code seahorses.feedingsPerDay}). */
    public static BettaHealth.Params params(BettaHealth.Params p) {
        double feedings = ServerConfig.getOr(ServerConfig.SEAHORSE_FEEDINGS, 2.0);
        if (feedings <= 1.0) return p;
        return new BettaHealth.Params(p.daysUntilSick(), p.daysSickUntilDeath(), p.bothBadMultiplier(), p.recoveryPerDay(), p.hungryAfterDays() / feedings);
    }

    // ------------------------------------------------------------------ tankmates

    /** Fast or pushy fish that harass seahorses. */
    public static boolean harasser(TankmateSpecies s) {
        return switch (s.group()) {
            case TANG, DAMSEL, CLOWNFISH -> true;
            default -> false;
        };
    }

    /** One of the harassers in the tank (adults), or null. */
    @Nullable
    public static TankmateEntity harasserIn(List<TankmateEntity> mates) {
        if (!ServerConfig.getOr(ServerConfig.SEAHORSE_HARASSED, true)) return null;
        for (TankmateEntity m : mates) if (m.isAdult() && harasser(m.species())) return m;
        return null;
    }
}
