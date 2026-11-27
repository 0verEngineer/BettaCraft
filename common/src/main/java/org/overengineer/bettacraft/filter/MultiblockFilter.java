package org.overengineer.bettacraft.filter;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.plumbing.FilterLines;
import org.overengineer.bettacraft.plumbing.interior.TankAnchors;
import org.overengineer.bettacraft.plumbing.PipeNetwork;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M24: the multiblock external filter. Filter chambers joined face to face (any arrangement: a column, a 2x2 block ...,
 * 1 to {@code multiblockFilter.maxChambers}) with exactly one pump head touching them, at least one inlet and one outlet
 * connector against them. The pump head pulls water through the inlet and pushes it out of the outlet; when both lead to
 * the same tank (directly or through pipes and bulkheads) it keeps that tank's water clean like an external filter:
 * the sum of its chambers' media ({@link FilterMedium#blocks} x how clean they are) x its power regulator. The old
 * external filter stays the small one-block variant.
 * <p>
 * M40 ({@code features.pipes.filterLines}): one filter serves every tank its intake and its return line both reach
 * ({@link #links}: directly, through the filter pipes and their T-pieces or the old pipes); its cleaning and its flow are
 * shared by the tanks' water volumes ({@link #shares}). Every place where a line docks on a tank needs a guard in the
 * interior ({@link TankAnchors}); without one, or with a pipe that leads through the tank, the filter does not work for
 * that tank and its menus warn ({@link #warnings}). This holds for filters built before M40 too.
 */
public final class MultiblockFilter {
    private MultiblockFilter() {}

    /** M68: SHORT_CIRCUIT: a line of the filter also touches a port of another role (set by the pump head, not by {@link #scan}). */
    public enum Problem { NONE, NO_HEAD, TWO_HEADS, NO_CHAMBER, TOO_MANY, NO_INLET, NO_OUTLET, SHORT_CIRCUIT }

    /** The parts of one filter (M68: the fresh and waste water ports of M40 part b are gone). */
    public record Structure(BlockPos head, List<BlockPos> chambers, List<BlockPos> inlets, List<BlockPos> outlets, Problem problem) {
        public boolean formed() {
            return problem == Problem.NONE;
        }
    }

    public static int maxChambers() {
        return ServerConfig.getOr(ServerConfig.MULTIBLOCK_FILTER_MAX_CHAMBERS, 16);
    }

    private static boolean is(BlockState s, FilterPartBlock.Part p) {
        return s.getBlock() instanceof FilterPartBlock f && f.part() == p;
    }

    /** Scans the filter around the pump head at {@code head}. */
    public static Structure scan(Level level, BlockPos head) {
        List<BlockPos> chambers = new ArrayList<>(), inlets = new ArrayList<>(), outlets = new ArrayList<>();
        int heads = 0;
        int limit = maxChambers();
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(head.immutable());
        seen.add(head.immutable());
        boolean tooMany = false;
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            BlockState s = level.getBlockState(p);
            if (is(s, FilterPartBlock.Part.PUMP_HEAD)) heads++;
            else if (is(s, FilterPartBlock.Part.CHAMBER)) chambers.add(p);
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (seen.contains(n) || !level.isLoaded(n)) continue;
                BlockState ns = level.getBlockState(n);
                if (is(ns, FilterPartBlock.Part.CHAMBER) || is(ns, FilterPartBlock.Part.PUMP_HEAD)) {
                    if (seen.size() > limit + 4) {
                        tooMany = true;
                        continue;
                    }
                    seen.add(n.immutable());
                    queue.add(n.immutable());
                } else if ((is(ns, FilterPartBlock.Part.INLET) || is(ns, FilterPartBlock.Part.OUTLET)) && ns.getValue(FilterPartBlock.FACING) == d) {
                    seen.add(n.immutable());
                    (is(ns, FilterPartBlock.Part.INLET) ? inlets : outlets).add(n.immutable());
                }
            }
        }
        Problem pr = heads > 1 ? Problem.TWO_HEADS : chambers.isEmpty() ? Problem.NO_CHAMBER : tooMany || chambers.size() > limit ? Problem.TOO_MANY
                : inlets.isEmpty() ? Problem.NO_INLET : outlets.isEmpty() ? Problem.NO_OUTLET : Problem.NONE;
        return new Structure(head.immutable(), chambers, inlets, outlets, pr);
    }

    /** The pump head of the filter the part at {@code pos} belongs to, or null. */
    @Nullable
    public static BlockPos headNear(Level level, BlockPos pos) {
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(pos.immutable());
        seen.add(pos.immutable());
        while (!queue.isEmpty() && seen.size() < 80) {
            BlockPos p = queue.poll();
            BlockState s = level.getBlockState(p);
            if (is(s, FilterPartBlock.Part.PUMP_HEAD)) return p;
            if (!(s.getBlock() instanceof FilterPartBlock)) continue;
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (!seen.contains(n) && level.isLoaded(n) && level.getBlockState(n).getBlock() instanceof FilterPartBlock) {
                    seen.add(n.immutable());
                    queue.add(n.immutable());
                }
            }
        }
        return null;
    }

    /** The tanks the connector at {@code connector} leads to: a tank block in front of it or the tanks at the ends of its pipes. */
    public static List<TankBlockEntity> tanksAt(Level level, BlockPos connector, Direction facing) {
        List<TankBlockEntity> out = new ArrayList<>();
        BlockPos n = connector.relative(facing);
        if (!level.isLoaded(n)) return out;
        BlockState s = level.getBlockState(n);
        if (s.getBlock() instanceof TankBlock) {
            TankBlockEntity c = TankMultiblock.getController(level, n);
            if (c != null) out.add(c);
        } else if (org.overengineer.bettacraft.plumbing.Pipes.isPipe(s)) {
            PipeNetwork net = PipeNetwork.towards(level, n, facing.getOpposite());
            if (net != null) for (BlockPos p : net.tankEnds(level)) {
                TankBlockEntity c = TankMultiblock.getController(level, p);
                if (c != null && !out.contains(c)) out.add(c);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ which filters serve which tank

    private record Serving(double blocks, double peat, long time) {}

    private static final Map<Level, Map<BlockPos, Map<BlockPos, Serving>>> SERVING = new WeakHashMap<>();
    /** A filter counts for its tank this long after its last run. */
    public static final long SERVE_TICKS = 40;

    /** The pump head reports: it cleans {@code blocks} water blocks of the tank with this controller now. */
    static void serve(Level level, BlockPos controller, BlockPos head, double blocks, double peat) {
        synchronized (SERVING) {
            SERVING.computeIfAbsent(level, k -> new HashMap<>()).computeIfAbsent(controller.immutable(), k -> new HashMap<>())
                    .put(head.immutable(), new Serving(blocks, peat, level.getGameTime()));
        }
    }

    static void stop(Level level, BlockPos head) {
        synchronized (SERVING) {
            var m = SERVING.get(level);
            if (m != null) for (var e : m.values()) e.remove(head);
        }
    }

    /** {count, water blocks} of the multiblock filters cleaning this tank now. */
    public static double[] serving(Level level, TankBlockEntity c) {
        if (!Features.enabled(Feature.MULTIBLOCK_FILTER)) return new double[2];
        synchronized (SERVING) {
            var m = SERVING.get(level);
            var e = m == null ? null : m.get(c.getBlockPos());
            if (e == null) return new double[2];
            double n = 0, b = 0;
            long now = level.getGameTime();
            for (Serving s : e.values()) {
                if (now - s.time > SERVE_TICKS || now < s.time) continue;
                n++;
                b += s.blocks;
            }
            return new double[]{n, b};
        }
    }

    /** M25: water blocks the peat of the multiblock filters running for this tank softens now. */
    public static double peat(Level level, TankBlockEntity c) {
        if (!Features.enabled(Feature.MULTIBLOCK_FILTER)) return 0;
        synchronized (SERVING) {
            var m = SERVING.get(level);
            var e = m == null ? null : m.get(c.getBlockPos());
            if (e == null) return 0;
            double p = 0;
            long now = level.getGameTime();
            for (Serving s : e.values()) if (now - s.time <= SERVE_TICKS && now >= s.time) p += s.peat;
            return p;
        }
    }

    /** Forgets every report (tests). */
    public static void forget() {
        synchronized (SERVING) {
            SERVING.clear();
        }
        synchronized (REPORTS) {
            REPORTS.clear();
        }
        FilterLines.forget();
    }

    // ------------------------------------------------------------------ M40: one filter, several tanks

    /** M40: how a filter works for one of its tanks (all but OK: it does not filter that tank). */
    public enum LinkStatus {
        OK, NO_INTAKE_GUARD, NO_OUTLET_GUARD, PASS_THROUGH, NO_WATER;

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        /** A missing guard or a pipe through the tank: a warning in the tank and filter menus. */
        public boolean warning() {
            return this == NO_INTAKE_GUARD || this == NO_OUTLET_GUARD || this == PASS_THROUGH;
        }
    }

    /** M40: a tank of a filter with the docks of its intake and return lines on it. */
    public record Link(TankBlockEntity tank, List<FilterLines.Dock> intakes, List<FilterLines.Dock> returns) {
        public long water() {
            return tank.getTank().waterMb();
        }
    }

    /** M40: the tanks the filter reaches with its intake and its return line (both are needed). */
    public static List<Link> links(Level level, Structure s) {
        Map<BlockPos, TankBlockEntity> tanks = new java.util.LinkedHashMap<>();
        Map<BlockPos, List<FilterLines.Dock>> in = new HashMap<>(), out = new HashMap<>();
        for (int role = 0; role < 2; role++) {
            List<BlockPos> connectors = role == 0 ? s.inlets() : s.outlets();
            Map<BlockPos, List<FilterLines.Dock>> into = role == 0 ? in : out;
            for (BlockPos p : connectors) {
                Direction f = level.getBlockState(p).getValue(FilterPartBlock.FACING);
                // M68: a line that is a short circuit leads nowhere for the filter
                if (org.overengineer.bettacraft.plumbing.PipePorts.line(level, p, f).shortCircuit()) continue;
                for (FilterLines.Dock d : FilterLines.docks(level, p, f)) {
                    TankBlockEntity c = TankMultiblock.getController(level, d.member());
                    if (c == null) continue;
                    tanks.putIfAbsent(c.getBlockPos(), c);
                    List<FilterLines.Dock> l = into.computeIfAbsent(c.getBlockPos(), k -> new ArrayList<>());
                    if (!l.contains(d)) l.add(d);
                }
            }
        }
        List<Link> links = new ArrayList<>();
        for (var e : tanks.entrySet()) {
            List<FilterLines.Dock> i = in.get(e.getKey()), o = out.get(e.getKey());
            if (i != null && o != null) links.add(new Link(e.getValue(), i, o));
        }
        return links;
    }

    /**
     * M68: what the filter's lines carry: the intake lines together ({@code intake}, mB per tick: the sum of the rates of the
     * different pipe networks at its inlets, {@link org.overengineer.bettacraft.plumbing.PipePorts#UNLIMITED} where an inlet touches
     * the tank directly), the return lines ({@code ret}) the same; and whether a line at the filter is a short circuit (it also
     * touches a port of another role: the filter does not filter, its menu and the overlays warn).
     */
    public record Flow(long intake, long ret, boolean shortCircuit, List<PipeNetwork> intakes, List<PipeNetwork> returns) {
        /** mB per tick the lines let through (the weaker side). */
        public long limit() {
            return Math.min(intake, ret);
        }
    }

    /** M68: the flow limit of the filter's lines ({@link Flow}). */
    public static Flow flow(Level level, Structure s) {
        long[] sum = new long[2];
        boolean sc = false;
        List<List<PipeNetwork>> nets = List.of(new ArrayList<>(), new ArrayList<>());
        for (int role = 0; role < 2; role++) {
            for (BlockPos p : role == 0 ? s.inlets() : s.outlets()) {
                Direction f = level.getBlockState(p).getValue(FilterPartBlock.FACING);
                org.overengineer.bettacraft.plumbing.PipePorts.Line l = org.overengineer.bettacraft.plumbing.PipePorts.line(level, p, f);
                if (l.shortCircuit()) {
                    sc = true;
                    continue;
                }
                if (l.network() != null) {
                    boolean seen = false;
                    for (PipeNetwork n : nets.get(role)) seen |= n == l.network();
                    if (seen) continue;
                    nets.get(role).add(l.network());
                }
                sum[role] = Math.min(org.overengineer.bettacraft.plumbing.PipePorts.UNLIMITED, sum[role] + l.rate());
            }
        }
        return new Flow(sum[0], sum[1], sc, List.copyOf(nets.get(0)), List.copyOf(nets.get(1)));
    }

    /** M40: whether the filter works for this tank: every dock of its lines guarded, no pipe through the tank, water in it. */
    public static LinkStatus status(net.minecraft.server.level.ServerLevel level, Link l) {
        if (TankAnchors.required(level)) {
            boolean pass = false, intake = false, outlet = false;
            for (FilterLines.Dock d : l.intakes()) {
                TankAnchors.Guard g = TankAnchors.guardOf(level, l.tank(), d.member(), d.face(), d.slot(), true);
                pass |= g == TankAnchors.Guard.PASS_THROUGH;
                intake |= g == TankAnchors.Guard.OPEN;
            }
            for (FilterLines.Dock d : l.returns()) {
                TankAnchors.Guard g = TankAnchors.guardOf(level, l.tank(), d.member(), d.face(), d.slot(), false);
                pass |= g == TankAnchors.Guard.PASS_THROUGH;
                outlet |= g == TankAnchors.Guard.OPEN;
            }
            if (pass) return LinkStatus.PASS_THROUGH;
            if (intake) return LinkStatus.NO_INTAKE_GUARD;
            if (outlet) return LinkStatus.NO_OUTLET_GUARD;
        }
        return l.water() <= 0 ? LinkStatus.NO_WATER : LinkStatus.OK;
    }

    /**
     * A1: the share of each working tank of a filter, by its water volume (pure; {@code water[i] <= 0} gets none). The
     * shares add up to 1 when any tank has water.
     */
    public static double[] shares(long[] water) {
        double sum = 0;
        for (long w : water) if (w > 0) sum += w;
        double[] out = new double[water.length];
        if (sum <= 0) return out;
        for (int i = 0; i < water.length; i++) out[i] = water[i] > 0 ? water[i] / sum : 0;
        return out;
    }

    private record Report(LinkStatus status, long time) {}

    private static final Map<Level, Map<BlockPos, Map<BlockPos, Report>>> REPORTS = new WeakHashMap<>();

    /** The pump head reports how it works for the tank with this controller (the tank menu shows the warnings). */
    static void report(Level level, BlockPos controller, BlockPos head, LinkStatus status) {
        synchronized (REPORTS) {
            REPORTS.computeIfAbsent(level, k -> new HashMap<>()).computeIfAbsent(controller.immutable(), k -> new HashMap<>())
                    .put(head.immutable(), new Report(status, level.getGameTime()));
        }
    }

    /** M40: the multiblock filters that reach this tank but do not filter it now (missing guard, pipe through it), by pump head. */
    public static Map<BlockPos, LinkStatus> warnings(Level level, TankBlockEntity c) {
        Map<BlockPos, LinkStatus> out = new java.util.TreeMap<>();
        if (!Features.enabled(Feature.MULTIBLOCK_FILTER) || !Features.enabled(Feature.FILTER_LINES)) return out;
        synchronized (REPORTS) {
            var m = REPORTS.get(level);
            var e = m == null ? null : m.get(c.getBlockPos());
            if (e == null) return out;
            long now = level.getGameTime();
            for (var r : e.entrySet()) {
                if (now - r.getValue().time > SERVE_TICKS || now < r.getValue().time || !r.getValue().status.warning()) continue;
                out.put(r.getKey(), r.getValue().status);
            }
        }
        return out;
    }

    /** The tank served by a multiblock filter whose part touches {@code pos} (for the level sensor at the filter), or null. */
    @Nullable
    public static TankBlockEntity tankNextTo(Level level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (!level.isLoaded(n) || !(level.getBlockState(n).getBlock() instanceof FilterPartBlock)) continue;
            BlockPos head = headNear(level, n);
            if (head != null && level.getBlockEntity(head) instanceof FilterPumpHeadBlockEntity h) {
                BlockPos t = h.servedTank();
                if (t != null && level.isLoaded(t)) {
                    TankBlockEntity c = TankMultiblock.getController(level, t);
                    if (c != null) return c;
                }
            }
        }
        return null;
    }
}
