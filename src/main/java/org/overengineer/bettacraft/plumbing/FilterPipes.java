package org.overengineer.bettacraft.plumbing;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M40 part b: which filter lines a block carries and on which sides each is joined, for the single filter pipes
 * ({@link FilterPipeBlock}: one line, the sides in the block state) and the pipe bundle ({@link FilterPipeBundleBlock}: up
 * to all four lines, kept in its block entity). Everything that follows a line (the line search in {@link FilterLines}, the
 * joining rules of both blocks, the docks on tanks) asks here instead of looking at one block class.
 * <p>
 * A line at a position is a <em>slot</em>: {@link #PRESENT} (the line runs here), {@link #JUNCTION} (a T-piece: it may join
 * all six sides) and six side bits ({@code 1 << direction.get3DDataValue()}). The lines of a bundle are packed into one int,
 * eight bits per line in the order of {@link PipeLine} (saved; never reorder).
 */
public final class FilterPipes {
    private FilterPipes() {}

    public static final int PRESENT = 0x80, JUNCTION = 0x40, SIDES = 0x3F;

    // ------------------------------------------------------------------ slots

    public static int bit(Direction d) {
        return 1 << d.get3DDataValue();
    }

    /** The slot of {@code line} in packed bundle lines (0: not there). */
    public static int slot(int lines, PipeLine line) {
        return (lines >>> (line.ordinal() * 8)) & 0xFF;
    }

    /** Packed lines with the slot of {@code line} replaced (0 takes the line out). */
    public static int with(int lines, PipeLine line, int slot) {
        int shift = line.ordinal() * 8;
        return (lines & ~(0xFF << shift)) | ((slot & 0xFF) << shift);
    }

    public static boolean present(int slot) {
        return (slot & PRESENT) != 0;
    }

    public static boolean junction(int slot) {
        return (slot & JUNCTION) != 0;
    }

    public static boolean joined(int slot, Direction d) {
        return (slot & bit(d)) != 0;
    }

    public static int count(int slot) {
        return Integer.bitCount(slot & SIDES);
    }

    /** Sides one line joins at most: two (a straight run or a bend), a T-piece all six. */
    public static int limit(int slot) {
        return junction(slot) ? 6 : 2;
    }

    /** The lines in packed bundle lines. */
    public static EnumSet<PipeLine> lines(int lines) {
        EnumSet<PipeLine> out = EnumSet.noneOf(PipeLine.class);
        for (PipeLine l : PipeLine.values()) if (present(slot(lines, l))) out.add(l);
        return out;
    }

    // ------------------------------------------------------------------ what a position carries

    /** Whether the block is one of our filter pipes (a single pipe or a bundle). */
    public static boolean isPipe(BlockState s) {
        return s.getBlock() instanceof FilterPipeBlock || s.getBlock() instanceof FilterPipeBundleBlock;
    }

    /** All lines at {@code pos}, packed (a single pipe gives its one line). */
    public static int packed(BlockGetter level, BlockPos pos, BlockState s) {
        if (s.getBlock() instanceof FilterPipeBlock f) return with(0, f.line(), FilterPipeBlock.slot(s));
        if (s.getBlock() instanceof FilterPipeBundleBlock && level.getBlockEntity(pos) instanceof FilterPipeBlockEntity be) return be.lines();
        return 0;
    }

    /** The slot of {@code line} at {@code pos} (0: no pipe of that line there). */
    public static int at(BlockGetter level, BlockPos pos, PipeLine line) {
        return at(level, pos, level.getBlockState(pos), line);
    }

    public static int at(BlockGetter level, BlockPos pos, BlockState s, PipeLine line) {
        if (s.getBlock() instanceof FilterPipeBlock f) return f.line() == line ? FilterPipeBlock.slot(s) : 0;
        if (s.getBlock() instanceof FilterPipeBundleBlock && level.getBlockEntity(pos) instanceof FilterPipeBlockEntity be) return slot(be.lines(), line);
        return 0;
    }

    public static EnumSet<PipeLine> lines(BlockGetter level, BlockPos pos) {
        return lines(packed(level, pos, level.getBlockState(pos)));
    }

    // ------------------------------------------------------------------ joining

    /** What is on side {@code d} of a line at {@code pos}: a pipe of the same line, a tank, its connector, another mod's (or our) fluid block, or nothing it joins. */
    public enum Target { NONE, PIPE, TANK, CONNECTOR, FOREIGN }

    public static Target target(PipeLine line, BlockGetter level, BlockPos pos, Direction d) {
        BlockPos np = pos.relative(d);
        BlockState n = level.getBlockState(np);
        if (isPipe(n)) return present(at(level, np, n, line)) ? Target.PIPE : Target.NONE;
        if (n.getBlock() instanceof TankBlock) return line.toTanks() ? Target.TANK : Target.NONE;
        if (n.getBlock() instanceof FilterPartBlock p)
            return p.part() == line.connector() && n.getValue(FilterPartBlock.FACING) == d.getOpposite() ? Target.CONNECTOR : Target.NONE;
        if (line.water() && foreign(level, np, n, d.getOpposite())) return Target.FOREIGN;
        return Target.NONE;
    }

    /**
     * Whether the block at {@code pos} takes or gives water on {@code face} for a fresh or waste line: a fluid handler
     * (a reservoir, our glass and steel pipes, other mods' tanks and pipes, an AE2/RS interface) or a pump of ours.
     */
    public static boolean foreign(BlockGetter level, BlockPos pos, BlockState s, Direction face) {
        if (s.isAir() || isPipe(s) || s.getBlock() instanceof TankBlock || s.getBlock() instanceof FilterPartBlock) return false;
        if (s.getBlock() instanceof PipeConnectable c && c.pipeConnects(s, face)) return true;
        if (!(level instanceof Level l)) return false;
        return l.getCapability(Capabilities.FluidHandler.BLOCK, pos, s, l.getBlockEntity(pos), face) != null;
    }

    /** Whether a new connection of {@code line} on side {@code d} can be made (the neighbour pipe has room or is joined to us already). */
    public static boolean canJoin(PipeLine line, BlockGetter level, BlockPos pos, Direction d) {
        Target t = target(line, level, pos, d);
        if (t == Target.NONE) return false;
        if (t == Target.PIPE) {
            int n = at(level, pos.relative(d), line);
            return joined(n, d.getOpposite()) || count(n) < limit(n);
        }
        return true;
    }

    /** Joins free sides (in this order) while the line has room. */
    public static int connect(PipeLine line, int slot, BlockGetter level, BlockPos pos, List<Direction> order) {
        for (Direction d : order) {
            if (count(slot) >= limit(slot)) break;
            if (!joined(slot, d) && canJoin(line, level, pos, d)) slot |= bit(d);
        }
        return slot;
    }

    /** Joins the free sides of a new line: first its own pipes, then its connector, tanks, the blocks of other mods. */
    public static int connectNew(PipeLine line, int slot, BlockGetter level, BlockPos pos) {
        for (Target want : new Target[]{Target.PIPE, Target.CONNECTOR, Target.TANK, Target.FOREIGN}) {
            for (Direction d : Direction.values()) {
                if (count(slot) >= limit(slot)) return slot;
                if (!joined(slot, d) && target(line, level, pos, d) == want && canJoin(line, level, pos, d)) slot |= bit(d);
            }
        }
        return slot;
    }

    /** Whether the line stays (or becomes) joined on side {@code dir} after its neighbour there changed. */
    public static boolean side(PipeLine line, int slot, BlockGetter level, BlockPos pos, Direction dir) {
        Target t = target(line, level, pos, dir);
        if (t == Target.PIPE) return joined(at(level, pos.relative(dir), line), dir.getOpposite()); // the neighbour pipe decided when it was placed
        if (t == Target.NONE) return false;
        return joined(slot, dir) || count(slot) < limit(slot); // a tank, the connector or a block of another mod appeared: joined while there is room
    }

    /** The slot after a neighbour changed without a shape update (another mod's block got or lost its fluid handler). */
    public static int refresh(PipeLine line, int slot, BlockGetter level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            Target t = target(line, level, pos, d);
            if (t == Target.NONE && joined(slot, d)) slot &= ~bit(d);
            else if (t == Target.FOREIGN && !joined(slot, d) && count(slot) < limit(slot)) slot |= bit(d);
        }
        return slot;
    }

    /** E2: where a line docks on a tank, its connection appears in the interior right away. */
    public static void dock(ServerLevel level, BlockPos pos, PipeLine line, int slot) {
        if (!line.toTanks() || !Features.enabled(Feature.FILTER_LINES)) return;
        for (Direction d : Direction.values()) {
            if (!joined(slot, d)) continue;
            BlockPos n = pos.relative(d);
            if (!(level.getBlockState(n).getBlock() instanceof TankBlock)) continue;
            TankBlockEntity c = TankMultiblock.getController(level, n);
            if (c == null) continue;
            if (org.overengineer.bettacraft.plumbing.interior.TankAnchors.existing(level, c, n, d.getOpposite()) == Long.MIN_VALUE)
                org.overengineer.bettacraft.plumbing.interior.TankAnchors.ensure(level, c, n, d.getOpposite());
        }
    }

    /** Whether a filter pipe of a tank line at {@code pos} is joined towards {@code back} (it docks there). */
    public static boolean docksTowards(BlockGetter level, BlockPos pos, BlockState s, Direction back) {
        int lines = packed(level, pos, s);
        for (PipeLine l : PipeLine.values()) if (l.toTanks() && joined(slot(lines, l), back)) return true;
        return false;
    }
}
