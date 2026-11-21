package org.overengineer.bettacraft.plumbing.interior;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCurrents;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.interior.InteriorEquipment;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;

/**
 * M24: the pipes in a tank interior. A bulkhead sits in a floor block of the tank; its connection ({@link Plumb#PORT})
 * stands on the interior floor of that block. The interior pipes joined to it lead to outlets: water pushed into the
 * tank from below that block comes out of the lily pipes, nozzles and spray bars, water pulled out goes in through the
 * intake strainers. While water flows, those outlets are active: they show their stream and their current acts where
 * they sit ({@link TankCurrents}: seahorses, corals, floating plants, the fish that swim into it). Everything works from
 * the equipment index, so it also works while nobody is inside the interior.
 */
public final class InteriorPlumbing {
    private InteriorPlumbing() {}

    /** An outlet counts as active this many ticks after water last went through it. */
    public static final long ACTIVE_TICKS = 40;
    /** Pieces searched from one bulkhead at most. */
    public static final int MAX_PIECES = 4096;

    /**
     * The outlets reached from one bulkhead connection. M40: {@code ports} are the other connections to the outside
     * (bulkheads, docks of filter pipes) the interior pipes also reach: a pipe that leads into the tank and out again.
     */
    public record Net(LongArrayList outflows, LongArrayList intakes, LongArrayList ports) {
        public Net(LongArrayList outflows, LongArrayList intakes) {
            this(outflows, intakes, new LongArrayList());
        }

        public boolean empty() {
            return outflows.isEmpty() && intakes.isEmpty();
        }

        /** M40: the pipes lead out of the tank again (no water goes through such a connection). */
        public boolean passThrough() {
            return !ports.isEmpty();
        }
    }

    private record Cache(int version, Long2ObjectOpenHashMap<Net> byPort) {}

    private static final Map<Integer, Cache> CACHE = new ConcurrentHashMap<>();
    /** Region id -> outlet key -> game time water last went through it. */
    private static final Map<Integer, Long2LongOpenHashMap> ACTIVE = new ConcurrentHashMap<>();
    /** Region id -> outlet key -> the look last set on the block (so the interior is only touched when it changes). */
    private static final Map<Integer, it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap> SHOWN = new ConcurrentHashMap<>();

    /** Pure: the outlets joined to the piece at {@code start} (pieces join their six neighbours; outlets end a line). */
    public static Net search(Long2IntOpenHashMap plumbing, long start) {
        LongArrayList out = new LongArrayList(), in = new LongArrayList(), ports = new LongArrayList();
        LongOpenHashSet seen = new LongOpenHashSet();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty()) {
            long k = queue.poll();
            int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
            for (Direction d : Direction.values()) {
                long n = BlockPos.asLong(x + d.getStepX(), y + d.getStepY(), z + d.getStepZ());
                if (!plumbing.containsKey(n) || seen.contains(n) || seen.size() >= MAX_PIECES) continue;
                seen.add(n);
                Plumb p = Plumb.kind(plumbing.get(n));
                if (p.outflow()) out.add(n);
                else if (p.intake()) in.add(n);
                else if (p == Plumb.PIPE) queue.add(n);
                else if (p.port()) ports.add(n); // M40: another connection to the outside
            }
        }
        return new Net(out, in, ports);
    }

    /** The outlets of the bulkhead connection {@code port} of a region (cached until a piece changes). */
    public static Net net(InteriorRegion r, long port) {
        InteriorEquipment eq = r.equipment();
        Cache c = CACHE.get(r.id());
        if (c == null || c.version != eq.plumbingVersion()) {
            if (CACHE.size() > 512) CACHE.clear();
            c = new Cache(eq.plumbingVersion(), new Long2ObjectOpenHashMap<>());
            CACHE.put(r.id(), c);
        }
        Net n = c.byPort.get(port);
        if (n == null) {
            n = search(eq.plumbing(), port);
            c.byPort.put(port, n);
        }
        return n;
    }

    /** Pure: the interior-local key of the bulkhead connection in the column of tank cell (cx, cz), or {@link Long#MIN_VALUE}. */
    public static long portInCell(Long2IntOpenHashMap plumbing, int cx, int cz, int scale) {
        for (var e : plumbing.long2IntEntrySet()) {
            if (Plumb.kind(e.getIntValue()) != Plumb.PORT) continue;
            long k = e.getLongKey();
            if (BlockPos.getY(k) >= scale) continue;
            if (Math.floorDiv(BlockPos.getX(k), scale) == cx && Math.floorDiv(BlockPos.getZ(k), scale) == cz) return k;
        }
        return Long.MIN_VALUE;
    }

    /** The bulkhead of the tank under the block {@code member} (only floor blocks have one), or null. */
    @Nullable
    public static Bulkhead bulkheadAt(ServerLevel level, TankBlockEntity c, BlockPos member) {
        if (!Features.enabled(Feature.INTERIOR_PIPES)) return null;
        TankData t = c.getTank();
        if (t.contains(member.below())) return null;
        InteriorRegion r = InteriorRegions.get(level.getServer()).get(t.regionId());
        if (r == null || !r.built() || InteriorManager.equipment(level.getServer(), r).plumbing().isEmpty()) return null;
        BlockPos cell = member.subtract(t.min());
        long port = portInCell(r.equipment().plumbing(), cell.getX(), cell.getZ(), r.scale());
        if (port == Long.MIN_VALUE) return null;
        return new Bulkhead(r, port, net(r, port));
    }

    /** A bulkhead and the outlets behind it. */
    public record Bulkhead(InteriorRegion region, long port, Net net) {
        public boolean canFill() {
            return !net.outflows().isEmpty() && !net.passThrough(); // M40: no water through a pipe that leads out again
        }

        public boolean canDrain() {
            return !net.intakes().isEmpty() && !net.passThrough();
        }

        public void flowIn(long now, long mb) {
            if (mb > 0) mark(region.id(), net.outflows(), now);
        }

        public void flowOut(long now, long mb) {
            if (mb > 0) mark(region.id(), net.intakes(), now);
        }
    }

    /** M40: water went through these outlets now (the filter marks the guards and outlets behind its connections). */
    public static void markFlow(int regionId, LongArrayList keys, long now) {
        mark(regionId, keys, now);
    }

    static void mark(int regionId, LongArrayList keys, long now) {
        Long2LongOpenHashMap m = ACTIVE.computeIfAbsent(regionId, k -> new Long2LongOpenHashMap());
        synchronized (m) {
            for (long k : keys) m.put(k, now);
        }
    }

    public static boolean active(int regionId, long key, long now) {
        Long2LongOpenHashMap m = ACTIVE.get(regionId);
        if (m == null) return false;
        synchronized (m) {
            long t = m.getOrDefault(key, Long.MIN_VALUE);
            return t != Long.MIN_VALUE && now - t >= 0 && now - t <= ACTIVE_TICKS;
        }
    }

    /** M53c: one region (removed; its id is handed out again later). */
    public static void forget(int regionId) {
        ACTIVE.remove(regionId);
        CACHE.remove(regionId);
        SHOWN.remove(regionId);
    }

    /** Forgets the activity (tests). */
    public static void forget() {
        ACTIVE.clear();
        CACHE.clear();
        SHOWN.clear();
    }

    /** The currents of the active outlets of a tank, in world coordinates (empty with the feature off). */
    public static List<TankCurrents.Current> currents(ServerLevel level, TankBlockEntity c) {
        List<TankCurrents.Current> out = new ArrayList<>();
        if (!Features.enabled(Feature.INTERIOR_PIPES)) return out;
        TankData t = c.getTank();
        InteriorRegion r = InteriorRegions.get(level.getServer()).get(t.regionId());
        if (r == null || !r.built() || InteriorManager.equipment(level.getServer(), r).plumbing().isEmpty()) return out;
        long now = level.getGameTime();
        double s = r.scale();
        for (var e : r.equipment().plumbing().long2IntEntrySet()) {
            Plumb p = Plumb.kind(e.getIntValue());
            if (!p.outlet() || !active(r.id(), e.getLongKey(), now)) continue;
            long k = e.getLongKey();
            Direction f = Direction.from2DDataValue(Plumb.facing(e.getIntValue()));
            double x = t.min().getX() + (BlockPos.getX(k) + 0.5) / s, z = t.min().getZ() + (BlockPos.getZ(k) + 0.5) / s;
            out.add(new TankCurrents.Current(x, z, f.getStepX(), f.getStepZ(), p.strength()));
        }
        return out;
    }

    /** Active outlets of a tank (for the menu and the tests). */
    public static int activeOutlets(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        InteriorRegion r = InteriorRegions.get(level.getServer()).get(t.regionId());
        if (r == null || !r.built()) return 0;
        long now = level.getGameTime();
        int n = 0;
        for (var e : r.equipment().plumbing().long2IntEntrySet()) if (Plumb.kind(e.getIntValue()).outlet() && active(r.id(), e.getLongKey(), now)) n++;
        return n;
    }

    /** {bulkheads and pipe docks, pipes, outlets and guards} of a tank. */
    public static int[] count(ServerLevel level, TankData t) {
        InteriorRegion r = InteriorRegions.get(level.getServer()).get(t.regionId());
        int[] n = new int[3];
        if (r == null || !r.built()) return n;
        InteriorManager.equipment(level.getServer(), r);
        for (int code : r.equipment().plumbing().values()) {
            Plumb p = Plumb.kind(code);
            n[p.port() ? 0 : p == Plumb.PIPE ? 1 : 2]++;
        }
        return n;
    }

    /** Care step: the outlets switch their look on and off with the water (only where the interior is loaded). */
    public static void syncStates(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        InteriorRegion r = InteriorRegions.get(level.getServer()).get(t.regionId());
        if (r == null || !r.built() || InteriorManager.equipment(level.getServer(), r).plumbing().isEmpty()) return;
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        if (interior == null) return;
        long now = level.getGameTime();
        for (var e : new ArrayList<>(r.equipment().plumbing().long2IntEntrySet())) {
            if (!Plumb.kind(e.getIntValue()).outlet()) continue;
            long k = e.getLongKey();
            boolean on = Features.enabled(Feature.INTERIOR_PIPES) && active(r.id(), k, now);
            var shown = SHOWN.computeIfAbsent(r.id(), x -> new it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap());
            if (shown.containsKey(k) && shown.get(k) == on) continue;
            // the look changes (rarely: water starts or stops): the interior chunk is loaded for it if needed, so the
            // stream shows when a player enters (the tank does not tick while everybody is inside)
            BlockPos p = r.toWorld(BlockPos.getX(k), BlockPos.getY(k), BlockPos.getZ(k));
            BlockState s = interior.getBlockState(p);
            shown.put(k, on);
            if (!(s.getBlock() instanceof PlumbingBlock)) continue;
            if (s.getValue(PlumbingBlock.ACTIVE) != on) interior.setBlock(p, s.setValue(PlumbingBlock.ACTIVE, on), Block.UPDATE_CLIENTS);
        }
    }
}
