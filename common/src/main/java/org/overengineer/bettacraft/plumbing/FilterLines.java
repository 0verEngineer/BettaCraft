package org.overengineer.bettacraft.plumbing;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.tank.TankBlock;

/**
 * M40: the filter lines from a filter connector to the tanks. A connector leads to tank blocks: directly (it touches the
 * tank), through the glass or steel pipes of a filter from before M40 ({@link PipeNetwork}) or through the filter pipes of
 * its line ({@link FilterPipeBlock}, branched by T-pieces, or a line of a pipe bundle, {@link FilterPipeBundleBlock}). Each
 * end is a {@link Dock}: the tank block and its side the line docks on.
 * <p>
 * Part b: a line search ({@link #reach}) also collects the connectors of its line it reaches and, for the fresh and the
 * waste water line, the fluid handlers of other blocks at its ends ({@link End}). Cached until a filter pipe or a filter
 * part changes ({@link FilterPipeBlock#version()}) and for {@link #CACHE_TICKS} at most.
 */
public final class FilterLines {
    private FilterLines() {}

    /** Pipes searched from one connector at most. */
    public static final int MAX_PIPES = 1024;
    /** A line search is looked at again after this long anyway (a chunk that loads later fires no shape update). */
    public static final long CACHE_TICKS = 100;

    /** Where a line docks on a tank: the tank block and its side (pointing out of the tank, to the pipe). */
    public record Dock(BlockPos member, Direction face) {}

    /** Part b: a block with a fluid handler at the end of a fresh or waste line, and its face towards the line. */
    public record End(BlockPos pos, Direction face) {
        public IFluidHandler handler(Level level) {
            return level.isLoaded(pos) ? org.overengineer.bettacraft.platform.Platform.get().fluidHandler(level, pos, face) : null;
        }
    }

    /** Part b: what one line reaches: tank docks, its connectors (facing the line), the blocks of other mods at its ends. */
    public record Reach(List<Dock> docks, List<BlockPos> connectors, List<End> ends) {
        static final Reach NONE = new Reach(List.of(), List.of(), List.of());
    }

    private record Key(BlockPos pos, PipeLine line) {}

    private record Cached(Reach reach, long time) {}

    private static final Map<Level, Map<Key, Cached>> CACHE = new WeakHashMap<>();
    private static int cacheVersion = Integer.MIN_VALUE;

    /** Forgets every cached line search (tests). */
    public static void forget() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    /** The docks the connector at {@code connector} (facing {@code facing}, of line {@code line}) leads to. */
    public static List<Dock> docks(Level level, BlockPos connector, Direction facing, PipeLine line) {
        List<Dock> out = new ArrayList<>();
        BlockPos n = connector.relative(facing);
        if (!level.isLoaded(n)) return out;
        BlockState s = level.getBlockState(n);
        if (s.getBlock() instanceof TankBlock) {
            out.add(new Dock(n.immutable(), facing.getOpposite()));
        } else if (s.getBlock() instanceof PipeBlock) {
            PipeNetwork net = PipeNetwork.of(level, n);
            if (net != null) for (PipeNetwork.End e : net.ends()) {
                if (level.isLoaded(e.pos()) && level.getBlockState(e.pos()).getBlock() instanceof TankBlock) add(out, new Dock(e.pos(), e.face()));
            }
        } else if (FilterPipes.joined(FilterPipes.at(level, n, s, line), facing.getOpposite())) {
            out.addAll(reach(level, n, line).docks());
        }
        return out;
    }

    /**
     * Part b: what the fresh or waste port at {@code port} (facing {@code facing}) reaches: through the pipes of its line,
     * or the fluid handler of the block right in front of it (a reservoir, a glass pipe, another mod's pipe or tank).
     */
    public static Reach portReach(Level level, BlockPos port, Direction facing, PipeLine line) {
        BlockPos n = port.relative(facing);
        if (!level.isLoaded(n)) return Reach.NONE;
        BlockState s = level.getBlockState(n);
        if (FilterPipes.isPipe(s)) return FilterPipes.joined(FilterPipes.at(level, n, s, line), facing.getOpposite()) ? reach(level, n, line) : Reach.NONE;
        if (FilterPipes.foreign(level, n, s, facing.getOpposite())) return new Reach(List.of(), List.of(), List.of(new End(n.immutable(), facing.getOpposite())));
        return Reach.NONE;
    }

    private static <T> void add(List<T> out, T d) {
        if (!out.contains(d)) out.add(d);
    }

    /** Everything the pipes of {@code line} joined to the one at {@code start} reach (cached). */
    public static Reach reach(Level level, BlockPos start, PipeLine line) {
        Key k = new Key(start.immutable(), line);
        long now = level.getGameTime();
        synchronized (CACHE) {
            if (cacheVersion != FilterPipeBlock.version()) {
                CACHE.clear();
                cacheVersion = FilterPipeBlock.version();
            }
            Cached c = CACHE.computeIfAbsent(level, x -> new HashMap<>()).get(k);
            if (c != null && now - c.time() >= 0 && now - c.time() < CACHE_TICKS) return c.reach();
        }
        Reach r = search(level, start, line);
        synchronized (CACHE) {
            if (cacheVersion == FilterPipeBlock.version()) CACHE.computeIfAbsent(level, x -> new HashMap<>()).put(k, new Cached(r, now));
        }
        return r;
    }

    private static Reach search(Level level, BlockPos start, PipeLine line) {
        List<Dock> docks = new ArrayList<>();
        List<BlockPos> connectors = new ArrayList<>();
        List<End> ends = new ArrayList<>();
        LongOpenHashSet seen = new LongOpenHashSet();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        seen.add(start.asLong());
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            int slot = FilterPipes.at(level, p, line);
            if (!FilterPipes.present(slot)) continue;
            for (Direction d : Direction.values()) {
                if (!FilterPipes.joined(slot, d)) continue;
                BlockPos q = p.relative(d);
                if (!level.isLoaded(q)) continue;
                BlockState qs = level.getBlockState(q);
                if (FilterPipes.isPipe(qs)) {
                    if (FilterPipes.present(FilterPipes.at(level, q, qs, line)) && seen.size() < MAX_PIPES && seen.add(q.asLong())) queue.add(q.immutable());
                } else if (qs.getBlock() instanceof TankBlock) {
                    if (line.toTanks()) add(docks, new Dock(q.immutable(), d.getOpposite()));
                } else if (qs.getBlock() instanceof FilterPartBlock f) {
                    if (f.part() == line.connector() && qs.getValue(FilterPartBlock.FACING) == d.getOpposite()) add(connectors, q.immutable());
                } else if (line.water() && FilterPipes.foreign(level, q, qs, d.getOpposite())) {
                    add(ends, new End(q.immutable(), d.getOpposite()));
                }
            }
        }
        return new Reach(List.copyOf(docks), List.copyOf(connectors), List.copyOf(ends));
    }
}
