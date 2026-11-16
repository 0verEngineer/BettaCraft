package org.overengineer.bettacraft.plumbing;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.tank.TankBlock;

/**
 * M40: the filter lines from a filter connector to the tanks. A connector leads to tank blocks: directly (it touches the
 * tank), through the glass or steel pipes of a filter from before M40 ({@link PipeNetwork}) or through the filter pipes of
 * its line ({@link FilterPipeBlock}, branched by T-pieces). Each end is a {@link Dock}: the tank block and its side the line
 * docks on.
 */
public final class FilterLines {
    private FilterLines() {}

    /** Pipes searched from one connector at most. */
    public static final int MAX_PIPES = 1024;

    /** Where a line docks on a tank: the tank block and its side (pointing out of the tank, to the pipe). */
    public record Dock(BlockPos member, Direction face) {}

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
        } else if (s.getBlock() instanceof FilterPipeBlock f && f.line() == line && FilterPipeBlock.joined(s, facing.getOpposite())) {
            search(level, n, line, out);
        }
        return out;
    }

    private static void add(List<Dock> out, Dock d) {
        if (!out.contains(d)) out.add(d);
    }

    /** The tank docks of the filter pipes of {@code line} joined to the one at {@code start}. */
    static void search(Level level, BlockPos start, PipeLine line, List<Dock> out) {
        LongOpenHashSet seen = new LongOpenHashSet();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        seen.add(start.asLong());
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof FilterPipeBlock)) continue;
            for (Direction d : Direction.values()) {
                if (!FilterPipeBlock.joined(s, d)) continue;
                BlockPos q = p.relative(d);
                if (!level.isLoaded(q)) continue;
                BlockState qs = level.getBlockState(q);
                if (qs.getBlock() instanceof FilterPipeBlock f && f.line() == line) {
                    if (seen.size() < MAX_PIPES && seen.add(q.asLong())) queue.add(q.immutable());
                } else if (qs.getBlock() instanceof TankBlock) {
                    add(out, new Dock(q.immutable(), d.getOpposite()));
                }
            }
        }
    }
}
