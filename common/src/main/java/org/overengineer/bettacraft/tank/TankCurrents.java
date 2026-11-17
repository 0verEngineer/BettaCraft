package org.overengineer.bettacraft.tank;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.equipment.FilterFlow;
import org.overengineer.bettacraft.equipment.FilterHost;

/**
 * M22.4: the direction of the water movement in a tank. Every running external filter (a filter block or the filter in a
 * cabinet with its outlet side) sends a current from its outlet across the tank. In front of an outlet the water moves
 * faster than the tank's average ({@code TankReef#flow}): seahorses there feel the stronger flow, corals there get more of
 * it, floating plants drift with it ({@code TankPlants#jets}). Off with {@code features.equipment.cabinetOutletSide} (the flow is even
 * like before).
 */
public final class TankCurrents {
    private TankCurrents() {}

    /** One outlet: where it enters the water (world x/z), the direction into the tank and its strength (0.6 under water). */
    public record Current(double x, double z, double dx, double dz, double strength) {}

    /** How much faster the water moves right in front of an outlet (on top of the tank's average). */
    public static final double JET_BOOST = 0.8;
    /** Half the width of the current (blocks to each side of the line of the outlet). */
    public static final double HALF_WIDTH = 0.75;
    /** The current loses its extra speed over this many blocks (never below a quarter). */
    public static final double FADE = 8.0;
    private static final int CACHE_TICKS = 100;
    private static final Map<Long, Cached> CACHE = new ConcurrentHashMap<>();

    private record Cached(long time, List<Current> currents) {}

    /** The outlets of the running filters of a tank (empty without power or water, or with the feature off). */
    public static List<Current> of(Level level, TankBlockEntity c) {
        List<Current> out = new ArrayList<>();
        TankData t = c.getTank();
        if (t == null || t.waterMb() <= 0) return out;
        // M24: the outlets of the interior pipes act where they sit
        if (level instanceof net.minecraft.server.level.ServerLevel s) out.addAll(org.overengineer.bettacraft.plumbing.interior.InteriorPlumbing.currents(s, c));
        if (!TankPower.anyFilterRuns(c) || !Features.enabled(Feature.CABINET_OUTLET_SIDE)) return out;
        for (FilterHost f : TankEquipment.filters(level, c)) {
            if (!TankPower.filterRuns(c, f)) continue; // M37
            FilterFlow.Path path = path(t, f);
            if (path == null || !path.wet()) continue;
            Vec3 at = path.submerged() ? path.nozzle() : path.impact();
            out.add(new Current(at.x, at.z, path.inward().getStepX(), path.inward().getStepZ(), path.submerged() ? 0.6 : 1.0));
        }
        return out;
    }

    /** As {@link #of}, cached for 5 seconds per tank (seahorses and corals ask often). */
    public static List<Current> cached(Level level, TankBlockEntity c) {
        long key = key(level, c), now = level.getGameTime();
        Cached e = CACHE.get(key);
        if (e != null && now - e.time >= 0 && now - e.time < CACHE_TICKS) return e.currents;
        if (CACHE.size() > 256) CACHE.clear();
        List<Current> l = of(level, c);
        CACHE.put(key, new Cached(now, l));
        return l;
    }

    /** Forgets the cached currents of a tank (the outlet side changed; tests). */
    public static void invalidate(TankBlockEntity c) {
        if (c.getLevel() != null) CACHE.remove(key(c.getLevel(), c));
    }

    /** M33 part d: by position and dimension (tanks at the same place in two dimensions are two tanks). */
    private static long key(Level level, TankBlockEntity c) {
        return c.getBlockPos().asLong() * 31 + level.dimension().location().hashCode();
    }

    /** Forgets every cached current (tests). */
    public static void forget() {
        CACHE.clear();
    }

    /** The path of a filter host into its tank, or null. */
    public static FilterFlow.Path path(TankData t, FilterHost f) {
        if (f instanceof ExternalFilterBlockEntity e) {
            BlockState s = e.getBlockState();
            return s.getBlock() instanceof ExternalFilterBlock ? FilterFlow.forFilterBlock(t, e.getBlockPos(), s.getValue(ExternalFilterBlock.FACING)) : null;
        }
        if (f instanceof CabinetBlockEntity cab) return FilterFlow.forCabinet(t, cab);
        return null;
    }

    /**
     * Pure: how many times the tank's average flow the water moves at (x, z): 1 outside every current, up to
     * {@code 1 + JET_BOOST} right in front of an outlet, fading along the current. Several currents add up.
     */
    public static double factor(List<Current> currents, double x, double z) {
        double f = 1.0;
        for (Current c : currents) {
            double rx = x - c.x(), rz = z - c.z();
            double along = rx * c.dx() + rz * c.dz(), side = Math.abs(rx * c.dz() - rz * c.dx());
            if (along < -0.4 || side > HALF_WIDTH) continue;
            double fade = Math.max(0.25, 1 - Math.max(0, along) / FADE);
            f += JET_BOOST * c.strength() * fade;
        }
        return f;
    }

    /** The factor at a world position in this tank (1 with the feature off). */
    public static double factor(Level level, TankBlockEntity c, double x, double z) {
        return factor(cached(level, c), x, z);
    }
}
