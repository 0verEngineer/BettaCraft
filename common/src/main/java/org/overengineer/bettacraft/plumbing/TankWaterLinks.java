package org.overengineer.bettacraft.plumbing;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * M68 part B: which level sensors sit at a tank and which water change controllers serve it, without a cable. A sensor says so on
 * every check ({@link #sensor}), a controller on every step for each tank it serves ({@link #changer}); the entries are kept in
 * memory only (they come back within a second after a load) and are checked again when they are read (the block entity must still
 * be there and still serve that tank). Keyed by the position of the tank's controller block.
 */
public final class TankWaterLinks {
    private TankWaterLinks() {}

    /** An entry counts for this long after it was last said (the devices check every 10 to 20 ticks). */
    public static final long TTL = 100;

    /** A controller that serves a tank: where it drains the tank itself (its waste water docks; empty: through a filter or none). */
    public record Changer(BlockPos pos, List<PipeNetwork.Dock> wasteDocks, long time) {}

    private record Links(Long2ObjectOpenHashMap<Map<BlockPos, Long>> sensors, Long2ObjectOpenHashMap<Map<BlockPos, Changer>> changers) {}

    private static final Map<Level, Links> LINKS = new WeakHashMap<>();

    private static Links of(Level level) {
        return LINKS.computeIfAbsent(level, k -> new Links(new Long2ObjectOpenHashMap<>(), new Long2ObjectOpenHashMap<>()));
    }

    /** The sensor at {@code sensor} watches the tank whose controller block is {@code tank}. */
    public static synchronized void sensor(Level level, BlockPos tank, BlockPos sensor) {
        of(level).sensors.computeIfAbsent(tank.asLong(), k -> new java.util.HashMap<>()).put(sensor.immutable(), level.getGameTime());
    }

    /** The sensor at {@code sensor} is gone (or watches no tank). */
    public static synchronized void forgetSensor(Level level, BlockPos sensor) {
        Links l = LINKS.get(level);
        if (l == null) return;
        for (var it = l.sensors.values().iterator(); it.hasNext(); ) {
            Map<BlockPos, Long> m = it.next();
            m.remove(sensor);
            if (m.isEmpty()) it.remove();
        }
    }

    /** The sensors that said lately that they watch {@code tank} (positions; the caller checks the block entity). */
    public static synchronized List<BlockPos> sensors(Level level, BlockPos tank) {
        Links l = LINKS.get(level);
        List<BlockPos> out = new ArrayList<>();
        Map<BlockPos, Long> m = l == null ? null : l.sensors.get(tank.asLong());
        if (m == null) return out;
        long now = level.getGameTime();
        for (var e : m.entrySet()) if (now - e.getValue() <= TTL && now >= e.getValue()) out.add(e.getKey());
        return out;
    }

    /** The controller at {@code changer} serves the tank {@code tank} (draining it at {@code wasteDocks} itself). */
    public static synchronized void changer(Level level, BlockPos tank, BlockPos changer, List<PipeNetwork.Dock> wasteDocks) {
        of(level).changers.computeIfAbsent(tank.asLong(), k -> new java.util.HashMap<>())
                .put(changer.immutable(), new Changer(changer.immutable(), List.copyOf(wasteDocks), level.getGameTime()));
    }

    /** The controller at {@code changer} is gone (or serves no tank any more). */
    public static synchronized void forgetChanger(Level level, BlockPos changer) {
        Links l = LINKS.get(level);
        if (l == null) return;
        for (var it = l.changers.values().iterator(); it.hasNext(); ) {
            Map<BlockPos, Changer> m = it.next();
            m.remove(changer);
            if (m.isEmpty()) it.remove();
        }
    }

    /** The controllers that said lately that they serve {@code tank}. */
    public static synchronized List<Changer> changers(Level level, BlockPos tank) {
        Links l = LINKS.get(level);
        List<Changer> out = new ArrayList<>();
        Map<BlockPos, Changer> m = l == null ? null : l.changers.get(tank.asLong());
        if (m == null) return out;
        long now = level.getGameTime();
        for (Changer c : m.values()) if (now - c.time() <= TTL && now >= c.time()) out.add(c);
        out.sort(java.util.Comparator.comparingLong(c -> c.pos().asLong()));
        return out;
    }

    /** Forgets everything (tests). */
    public static synchronized void forgetAll() {
        LINKS.clear();
    }
}
