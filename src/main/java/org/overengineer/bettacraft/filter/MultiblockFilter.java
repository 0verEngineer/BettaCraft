package org.overengineer.bettacraft.filter;

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
import org.overengineer.bettacraft.plumbing.PipeBlock;
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
 */
public final class MultiblockFilter {
    private MultiblockFilter() {}

    public enum Problem { NONE, NO_HEAD, TWO_HEADS, NO_CHAMBER, TOO_MANY, NO_INLET, NO_OUTLET }

    /** The parts of one filter. */
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
        } else if (s.getBlock() instanceof PipeBlock) {
            PipeNetwork net = PipeNetwork.of(level, n);
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
        if (!CommonConfig.multiblockFilter()) return new double[2];
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
        if (!CommonConfig.multiblockFilter()) return 0;
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
