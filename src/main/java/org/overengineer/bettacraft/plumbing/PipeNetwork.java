package org.overengineer.bettacraft.plumbing;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.tank.WaterValues;

/**
 * M24: the pipes joined to each other and the fluid handlers at their ends. A network holds no water: what is pushed in
 * goes straight on to the ends (round robin, never back to where it came from), what is pulled comes from them. It
 * carries at most what its weakest pipe carries per tick (counted over windows of 10 ticks, so pumps that move a batch
 * every 10 ticks fit). Built lazily and forgotten whenever a pipe or a block next to a pipe changes.
 */
public final class PipeNetwork {
    /** Pipes in one network at most (the search stops there). */
    public static final int MAX_PIPES = 1024;
    public static final int WINDOW = 10;
    /**
     * M33 part d (hardening): a network that touched an unloaded chunk while it was built is built again after this many
     * ticks (a chunk that loads later fires no neighbour change, so its pipes and ends would stay missing).
     */
    public static final long INCOMPLETE_TICKS = 100;

    /** A fluid handler at an end of the network: the block and its face that touches the pipe. */
    public record End(BlockPos pos, Direction face) {}

    private static final Map<Level, Long2ObjectOpenHashMap<PipeNetwork>> CACHE = new WeakHashMap<>();
    /** Networks working right now (a foreign pipe at an end that pushes straight back must not loop). */
    private static final ThreadLocal<java.util.Set<PipeNetwork>> BUSY = ThreadLocal.withInitial(java.util.HashSet::new);

    private final LongOpenHashSet pipes;
    private final List<End> ends;
    private final int rate;
    /** Built while a neighbour chunk was not loaded (see {@link #INCOMPLETE_TICKS}), and when. */
    private final boolean incomplete;
    private final long built;
    private long window = Long.MIN_VALUE;
    private int used;
    private int next;
    /** mB moved in the last full window (for the info lines and the GameTests). */
    private int lastWindowMb;

    private PipeNetwork(LongOpenHashSet pipes, List<End> ends, int rate, boolean incomplete, long built) {
        this.pipes = pipes;
        this.ends = ends;
        this.rate = rate;
        this.incomplete = incomplete;
        this.built = built;
    }

    /** Whether the network was built with a part of it in unloaded chunks. */
    public boolean incomplete() { return incomplete; }

    public LongOpenHashSet pipes() { return pipes; }
    public List<End> ends() { return Collections.unmodifiableList(ends); }
    /** mB per tick. */
    public int rate() { return rate; }
    public int lastWindowMb() { return lastWindowMb; }

    /** Forgets every network of this level (a pipe or a neighbour changed). */
    public static void invalidate(Level level) {
        synchronized (CACHE) {
            var m = CACHE.get(level);
            if (m != null) m.clear();
        }
    }

    /** The network of the pipe at {@code pos}, or null if there is no pipe. */
    @Nullable
    public static PipeNetwork of(Level level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof PipeBlock)) return null;
        synchronized (CACHE) {
            var m = CACHE.computeIfAbsent(level, k -> new Long2ObjectOpenHashMap<>());
            PipeNetwork n = m.get(pos.asLong());
            if (n != null && !(n.incomplete && level.getGameTime() - n.built >= INCOMPLETE_TICKS)) return n;
            if (n != null) for (long l : n.pipes) m.remove(l);
            n = build(level, pos);
            for (long l : n.pipes) m.put(l, n);
            return n;
        }
    }

    private static PipeNetwork build(Level level, BlockPos start) {
        LongOpenHashSet seen = new LongOpenHashSet();
        List<End> ends = new ArrayList<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        seen.add(start.asLong());
        int rate = Integer.MAX_VALUE;
        boolean incomplete = false;
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof PipeBlock pipe)) continue;
            rate = Math.min(rate, pipe.rate());
            for (Direction d : Direction.values()) {
                if (!s.getValue(PipeBlock.SIDES.get(d))) continue;
                BlockPos n = p.relative(d);
                if (!level.isLoaded(n)) {
                    incomplete = true;
                    continue;
                }
                BlockState ns = level.getBlockState(n);
                if (ns.getBlock() instanceof PipeBlock) {
                    if (seen.size() < MAX_PIPES && seen.add(n.asLong())) queue.add(n);
                } else if (!(ns.getBlock() instanceof PipeConnectable)) {
                    ends.add(new End(n, d.getOpposite()));
                }
            }
        }
        return new PipeNetwork(seen, ends, rate == Integer.MAX_VALUE ? 0 : rate, incomplete, level.getGameTime());
    }

    @Nullable
    private static IFluidHandler handler(Level level, End e) {
        if (!level.isLoaded(e.pos())) return null;
        if (level.getBlockState(e.pos()).getBlock() instanceof PipeBlock) return null;
        return level.getCapability(Capabilities.FluidHandler.BLOCK, e.pos(), e.face());
    }

    private int budget(Level level) {
        long w = level.getGameTime() / WINDOW;
        if (w != window) {
            lastWindowMb = w == window + 1 ? used : 0;
            window = w;
            used = 0;
        }
        return Math.max(0, rate * WINDOW - used);
    }

    /** M26: mB the network can still carry in this window (it carries at most its rate per tick, counted per window). */
    public int room(Level level) {
        return budget(level);
    }

    /** Pushes water into the network (from the block at {@code from}, which is skipped). Returns what the ends took. */
    public int fill(Level level, FluidStack resource, IFluidHandler.FluidAction action, @Nullable BlockPos from) {
        if (resource.isEmpty() || ends.isEmpty() || !Features.enabled(Feature.PIPES)) return 0;
        if (!BUSY.get().add(this)) return 0;
        try {
            int left = Math.min(resource.getAmount(), budget(level));
            int total = 0;
            int n = ends.size();
            for (int i = 0; i < n && left > 0; i++) {
                End e = ends.get((next + i) % n);
                if (from != null && e.pos().equals(from)) continue;
                IFluidHandler h = handler(level, e);
                if (h == null) continue;
                int took = h.fill(resource.copyWithAmount(left), action);
                // a foreign tank that only takes plain water (no values) gets plain water
                if (took <= 0 && WaterValues.from(resource) != null && !(h instanceof org.overengineer.bettacraft.tank.TankFluid)) {
                    took = h.fill(new FluidStack(resource.getFluidHolder(), left), action);
                }
                if (took > 0) {
                    total += took;
                    left -= took;
                }
            }
            if (action.execute() && total > 0) {
                used += total;
                next = (next + 1) % Math.max(1, n);
            }
            return total;
        } finally {
            BUSY.get().remove(this);
        }
    }

    /** Pulls up to {@code max} mB of water (or of {@code like}'s fluid) from the ends (not from {@code from}). */
    public FluidStack drain(Level level, int max, @Nullable FluidStack like, IFluidHandler.FluidAction action, @Nullable BlockPos from) {
        if (max <= 0 || ends.isEmpty() || !Features.enabled(Feature.PIPES)) return FluidStack.EMPTY;
        if (!BUSY.get().add(this)) return FluidStack.EMPTY;
        try {
            int want = Math.min(max, budget(level));
            if (want <= 0) return FluidStack.EMPTY;
            int n = ends.size();
            for (int i = 0; i < n; i++) {
                End e = ends.get((next + i) % n);
                if (from != null && e.pos().equals(from)) continue;
                IFluidHandler h = handler(level, e);
                if (h == null) continue;
                // only water: look first, then take exactly that (other mods' tanks compare the values too)
                FluidStack sim = h.drain(want, IFluidHandler.FluidAction.SIMULATE);
                if (sim.isEmpty() || !org.overengineer.bettacraft.registry.ModFluids.isWater(sim.getFluid())) continue;
                // M33 part d: drain(FluidStack) gives only that fluid (fresh, RO or sea water; its values may differ)
                if (like != null && !like.isEmpty() && sim.getFluid() != like.getFluid()) continue;
                FluidStack got = action.simulate() ? sim : h.drain(sim, action);
                if (got.isEmpty()) continue;
                if (action.execute()) {
                    used += got.getAmount();
                    next = (next + 1) % n;
                }
                return got;
            }
            return FluidStack.EMPTY;
        } finally {
            BUSY.get().remove(this);
        }
    }

    /** The ends of the network that are tank blocks (to find the tank a filter or a pump serves). */
    public List<BlockPos> tankEnds(Level level) {
        List<BlockPos> out = new ArrayList<>();
        for (End e : ends) if (level.isLoaded(e.pos()) && level.getBlockState(e.pos()).getBlock() instanceof org.overengineer.bettacraft.tank.TankBlock) out.add(e.pos());
        return out;
    }
}
