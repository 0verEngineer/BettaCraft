package org.overengineer.bettacraft.plumbing;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.platform.fluid.FluidStack;
import org.overengineer.bettacraft.platform.fluid.IFluidHandler;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.WaterValues;

/**
 * M24: the pipes joined to each other and the fluid handlers at their ends. A network holds no water: what is pushed in
 * goes straight on to the ends (round robin, never back to where it came from), what is pulled comes from them. It
 * carries at most what its weakest pipe carries per tick (counted over windows of 10 ticks, so pumps that move a batch
 * every 10 ticks fit). Built lazily and forgotten whenever a pipe or a block next to a pipe changes.
 * <p>
 * M68: one network for every pipe: its pipes are nodes (a single pipe, or one slot of a bundle, {@link PipeSlots}), walked
 * along their joined sides ({@link Pipes}). Besides the fluid ends it collects the tank docks (where it joins a tank block:
 * {@link #docks()}) and the ports it touches ({@link PipePort}: {@link #ports()}); the ports give it its role ({@link #role()},
 * {@link #ring()}): one role for all its ports, none without a port, a short circuit with two ({@link #shortCircuit()}). The
 * filter lines (intake, return) and a short circuit carry no water for others ({@link #carriesWater()}). A change forgets only
 * the networks around it ({@link #invalidate(Level, BlockPos)}); {@link #version()} counts the changes for the caches of the
 * filters.
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
    /** M68: a flow reported by a filter's pump head counts for this long (the pump head runs every {@link #WINDOW} ticks). */
    public static final long FLOW_TICKS = 40;

    /** A fluid handler at an end of the network: the block and its face that touches the pipe. */
    public record End(BlockPos pos, Direction face) {}

    /** M68: where the network joins a tank: the tank block and its side towards the pipe. */
    public record Dock(BlockPos member, Direction face) {}

    /** M68: a port the network touches: the block, its face towards the pipe and the role it gives. */
    public record Port(BlockPos pos, Direction face, PipeRole role) {}

    private static final Map<Level, Long2ObjectOpenHashMap<PipeNetwork[]>> CACHE = new WeakHashMap<>();
    /** Networks working right now (a foreign pipe at an end that pushes straight back must not loop). */
    private static final ThreadLocal<java.util.Set<PipeNetwork>> BUSY = ThreadLocal.withInitial(java.util.HashSet::new);
    private static volatile int version;

    private final long[] nodePos;
    private final byte[] nodeSlot;
    private final List<End> ends;
    private final List<Dock> docks;
    private final List<Port> ports;
    private final int rate;
    @Nullable
    private final PipeRole role;
    private final boolean shortCircuit;
    /** Every port it touches is a filter connector (with the filter lines off it is a plain pipe). */
    private final boolean filterPortsOnly;
    /** Built while a neighbour chunk was not loaded (see {@link #INCOMPLETE_TICKS}), and when. */
    private final boolean incomplete;
    private final long built;
    private long window = Long.MIN_VALUE;
    private int used;
    private int next;
    /** mB moved in the last full window (for the info lines and the GameTests). */
    private int lastWindowMb;
    /** M68: what a pump head moves through this (filter) line, mB per tick, and when it said so. */
    private int reportedFlow;
    private long reportedAt = Long.MIN_VALUE;

    private PipeNetwork(long[] nodePos, byte[] nodeSlot, List<End> ends, List<Dock> docks, List<Port> ports, int rate, boolean incomplete, long built) {
        this.nodePos = nodePos;
        this.nodeSlot = nodeSlot;
        this.ends = ends;
        this.docks = docks;
        this.ports = ports;
        this.rate = rate;
        this.incomplete = incomplete;
        this.built = built;
        EnumSet<PipeRole> roles = EnumSet.noneOf(PipeRole.class);
        for (Port p : ports) roles.add(p.role());
        this.shortCircuit = roles.size() > 1;
        boolean f = true;
        for (PipeRole r : roles) f &= r.filterLine();
        this.filterPortsOnly = f;
        this.role = roles.size() == 1 ? roles.iterator().next() : null;
    }

    /** Whether the network was built with a part of it in unloaded chunks. */
    public boolean incomplete() { return incomplete; }

    /** The number of pipes (nodes: single pipes and bundle slots). */
    public int size() { return nodePos.length; }

    /** The position of node {@code i}. */
    public BlockPos node(int i) { return BlockPos.of(nodePos[i]); }

    /** The slot of node {@code i} (0 for a single pipe). */
    public int nodeSlot(int i) { return nodeSlot[i]; }

    /** Whether the pipe at {@code pos} belongs to this network. */
    public boolean contains(BlockPos pos) {
        long k = pos.asLong();
        for (long p : nodePos) if (p == k) return true;
        return false;
    }

    public List<End> ends() { return Collections.unmodifiableList(ends); }

    /** M68: where the network joins tanks. */
    public List<Dock> docks() { return Collections.unmodifiableList(docks); }

    /** M68: the ports it touches. */
    public List<Port> ports() { return Collections.unmodifiableList(ports); }

    /** M68: the role of all its ports; null for a plain water line and for a short circuit. */
    @Nullable
    public PipeRole role() { return role; }

    /** M68: it touches ports of two or more roles: it does nothing for any of them, the ports' devices warn. */
    public boolean shortCircuit() { return shortCircuit; }

    /** M68: the ring its pipes show. */
    public PipeRing ring() { return shortCircuit ? PipeRing.SHORT : role == null ? PipeRing.NONE : role.ring(); }

    /**
     * M68: whether pumps and other mods' pipes move water through it (a plain line or a line of a role that carries water). With
     * {@code features.pipes.filterLines} off the filter's lines are plain pipes again (the filter works as before M40, through them).
     */
    public boolean carriesWater() {
        if (filterPortsOnly && !ports.isEmpty() && !Features.enabled(Feature.FILTER_LINES)) return true;
        return !shortCircuit && (role == null || role.carriesWater());
    }

    /** mB per tick (its weakest pipe). */
    public int rate() { return rate; }

    public int lastWindowMb() { return lastWindowMb; }

    /** M68: a pump head moves {@code mbPerTick} through this line now (the info lines show it). */
    public void reportFlow(Level level, int mbPerTick) {
        reportedFlow = mbPerTick;
        reportedAt = level.getGameTime();
    }

    /** M68: mB per tick it moves now: what a pump head reported (filter lines), else what went through it in the last window. */
    public int flow(Level level) {
        long now = level.getGameTime();
        if (reportedAt != Long.MIN_VALUE && now - reportedAt <= FLOW_TICKS && now >= reportedAt) return reportedFlow;
        long w = now / WINDOW;
        return w == window || w == window + 1 ? (w == window ? lastWindowMb : used) / WINDOW : 0;
    }

    // ------------------------------------------------------------------ cache

    /** Bumped whenever a network may have changed (the filters look at their lines again). */
    public static int version() {
        return version;
    }

    /** Forgets every network of this level. */
    public static void invalidate(Level level) {
        synchronized (CACHE) {
            var m = CACHE.get(level);
            if (m != null) m.clear();
            version++;
        }
    }

    /** Forgets every network of every level (tests). */
    public static void forgetAll() {
        synchronized (CACHE) {
            CACHE.clear();
            version++;
        }
    }

    /** M68: forgets the networks of the pipes at {@code pos} and next to it (a pipe or a block next to one changed). */
    public static void invalidate(Level level, BlockPos pos) {
        synchronized (CACHE) {
            version++;
            var m = CACHE.get(level);
            if (m == null || m.isEmpty()) return;
            drop(m, pos.asLong());
            for (Direction d : Direction.values()) drop(m, BlockPos.offset(pos.asLong(), d));
        }
    }

    private static void drop(Long2ObjectOpenHashMap<PipeNetwork[]> m, long key) {
        PipeNetwork[] arr = m.get(key);
        if (arr == null) return;
        for (PipeNetwork n : arr) {
            if (n == null) continue;
            for (int i = 0; i < n.nodePos.length; i++) {
                PipeNetwork[] a = m.get(n.nodePos[i]);
                if (a == null) continue;
                if (a[n.nodeSlot[i]] == n) a[n.nodeSlot[i]] = null;
                boolean empty = true;
                for (PipeNetwork x : a) empty &= x == null;
                if (empty) m.remove(n.nodePos[i]);
            }
        }
    }

    /** The network of the pipe at {@code pos} (a bundle: its first pipe), or null if there is no pipe. */
    @Nullable
    public static PipeNetwork of(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.getBlock() instanceof PipeBlock) return of(level, pos, 0);
        if (s.getBlock() instanceof PipeBundleBlock) {
            int[] slots = Pipes.slotsAt(level, pos);
            for (int i = 0; i < slots.length; i++) if (PipeSlots.present(slots[i])) return of(level, pos, i);
        }
        return null;
    }

    /** M68: the network of the pipe at {@code pos} that is joined on side {@code side} (a bundle: that slot), or null. */
    @Nullable
    public static PipeNetwork towards(Level level, BlockPos pos, Direction side) {
        BlockState s = level.getBlockState(pos);
        int slot = Pipes.slotJoined(level, pos, s, side);
        return slot < 0 ? null : of(level, pos, slot);
    }

    /** M68: the network of slot {@code slot} of the pipe at {@code pos} (0 for a single pipe), or null. */
    @Nullable
    public static PipeNetwork of(Level level, BlockPos pos, int slot) {
        BlockState s = level.getBlockState(pos);
        if (s.getBlock() instanceof PipeBlock) slot = 0;
        else if (!(s.getBlock() instanceof PipeBundleBlock) || slot < 0 || slot >= PipeSlots.COUNT || !PipeSlots.present(Pipes.slotsAt(level, pos)[slot])) return null;
        synchronized (CACHE) {
            var m = CACHE.computeIfAbsent(level, k -> new Long2ObjectOpenHashMap<>());
            PipeNetwork[] arr = m.get(pos.asLong());
            PipeNetwork n = arr == null ? null : arr[slot];
            if (n != null && !(n.incomplete && level.getGameTime() - n.built >= INCOMPLETE_TICKS)) return n;
            if (n != null) drop(m, pos.asLong());
            n = build(level, pos, slot);
            for (int i = 0; i < n.nodePos.length; i++) m.computeIfAbsent(n.nodePos[i], k -> new PipeNetwork[PipeSlots.COUNT])[n.nodeSlot[i]] = n;
            return n;
        }
    }

    private static PipeNetwork build(Level level, BlockPos start, int startSlot) {
        Long2ByteOpenHashMap seen = new Long2ByteOpenHashMap();
        it.unimi.dsi.fastutil.longs.LongArrayList pos = new it.unimi.dsi.fastutil.longs.LongArrayList();
        it.unimi.dsi.fastutil.bytes.ByteArrayList slots = new it.unimi.dsi.fastutil.bytes.ByteArrayList();
        List<End> ends = new ArrayList<>();
        List<Dock> docks = new ArrayList<>();
        List<Port> ports = new ArrayList<>();
        ArrayDeque<long[]> queue = new ArrayDeque<>();
        queue.add(new long[]{start.asLong(), startSlot});
        seen.put(start.asLong(), (byte) (1 << startSlot));
        int rate = Integer.MAX_VALUE;
        boolean incomplete = false;
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        while (!queue.isEmpty()) {
            long[] e = queue.poll();
            BlockPos p = BlockPos.of(e[0]);
            int slot = (int) e[1];
            BlockState s = level.getBlockState(p);
            Boolean steel = Pipes.steel(level, p, s, s.getBlock() instanceof PipeBlock ? -1 : slot);
            if (steel == null) continue;
            pos.add(e[0]);
            slots.add((byte) slot);
            rate = Math.min(rate, PipeBlock.rate(steel));
            int sides = Pipes.joinedSides(level, p, s, s.getBlock() instanceof PipeBlock ? -1 : slot);
            for (Direction d : Direction.values()) {
                if ((sides & PipeSlots.bit(d)) == 0) continue;
                q.setWithOffset(p, d);
                if (!level.isLoaded(q)) {
                    incomplete = true;
                    continue;
                }
                BlockState ns = level.getBlockState(q);
                Direction back = d.getOpposite();
                int nslot = -1;
                if (ns.getBlock() instanceof PipeBlock) {
                    nslot = ns.getValue(PipeBlock.SIDES.get(back)) ? 0 : -1;
                } else if (ns.getBlock() instanceof PipeBundleBlock) {
                    int[] n = Pipes.slotsAt(level, q);
                    if (s.getBlock() instanceof PipeBundleBlock) nslot = PipeSlots.present(n[slot]) && PipeSlots.joined(n[slot], back) ? slot : -1;
                    else nslot = Pipes.slotJoined(level, q, ns, back);
                } else if (ns.getBlock() instanceof TankBlock) {
                    BlockPos qi = q.immutable();
                    docks.add(new Dock(qi, back));
                    ends.add(new End(qi, back));
                    continue;
                } else if (ns.getBlock() instanceof PipePort port) {
                    PipeRole r = port.pipeRole(level, q, ns, back);
                    if (r != null) {
                        Port pt = new Port(q.immutable(), back, r);
                        if (!ports.contains(pt)) ports.add(pt);
                        continue;
                    }
                    ends.add(new End(q.immutable(), back));
                    continue;
                } else if (ns.getBlock() instanceof PipeConnectable c && c.pipeConnects(ns, back)) {
                    continue; // our pumps work the network themselves
                } else {
                    ends.add(new End(q.immutable(), back));
                    continue;
                }
                if (nslot < 0) continue;
                long k = q.asLong();
                byte mask = seen.get(k);
                if ((mask & (1 << nslot)) != 0 || seen.size() >= MAX_PIPES) continue;
                seen.put(k, (byte) (mask | 1 << nslot));
                queue.add(new long[]{k, nslot});
            }
        }
        return new PipeNetwork(pos.toLongArray(), slots.toByteArray(), ends, docks, ports, rate == Integer.MAX_VALUE ? 0 : rate, incomplete, level.getGameTime());
    }

    // ------------------------------------------------------------------ water

    @Nullable
    private static IFluidHandler handler(Level level, End e) {
        if (!level.isLoaded(e.pos())) return null;
        if (Pipes.isPipe(level.getBlockState(e.pos()))) return null;
        return org.overengineer.bettacraft.platform.Platform.get().fluidHandler(level, e.pos(), e.face());
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
        if (resource.isEmpty() || ends.isEmpty() || !Features.enabled(Feature.PIPES) || !carriesWater()) return 0;
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
        if (max <= 0 || ends.isEmpty() || !Features.enabled(Feature.PIPES) || !carriesWater()) return FluidStack.EMPTY;
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
        for (Dock d : docks) if (level.isLoaded(d.member()) && level.getBlockState(d.member()).getBlock() instanceof TankBlock && !out.contains(d.member())) out.add(d.member());
        return out;
    }
}
