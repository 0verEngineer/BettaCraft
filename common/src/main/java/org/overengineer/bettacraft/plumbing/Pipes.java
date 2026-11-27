package org.overengineer.bettacraft.plumbing;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.tank.TankBlock;

/**
 * M68: the joining rules of the pipes, shared by the single pipe ({@link PipeBlock}) and the slots of a bundle
 * ({@link PipeBundleBlock}). A pipe joins every neighbour that fits, on every side, unless that side is closed with the pipe
 * wrench (on either pipe):
 * <ul>
 * <li>another pipe: a single pipe joins the bundle's slot that faces it (the first slot not closed on that side,
 * {@link PipeSlots#facing}); two bundles join slot to slot (the same spot of the face);</li>
 * <li>a tank: every pipe and every slot of a bundle (a bundle docks its lines side by side);</li>
 * <li>a port ({@link PipePort}), a device of ours ({@link PipeConnectable}) or a block with a fluid handler on that face (a
 * reservoir, another mod's tank or pipe): a single pipe, of a bundle only the slot that faces it.</li>
 * </ul>
 * A slot is {@code -1} for a single pipe, else the bundle slot. Both sides of a joint come to the same answer, so a network
 * walk may trust either.
 */
public final class Pipes {
    private Pipes() {}

    public static boolean isPipe(BlockState s) {
        return s.getBlock() instanceof PipeBlock || s.getBlock() instanceof PipeBundleBlock;
    }

    /** The slots of the bundle at {@code pos} (all empty for anything else). */
    public static int[] slotsAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof PipeBlockEntity be ? be.slots() : new int[PipeSlots.COUNT];
    }

    /** The sides of the single pipe at {@code pos} closed with the wrench. */
    public static int closedAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof PipeBlockEntity be ? be.closed() : 0;
    }

    /** Whether the pipe (slot {@code slot}, -1 = the single pipe) at {@code pos} is closed on {@code d}. */
    public static boolean closed(BlockGetter level, BlockPos pos, int slot, Direction d) {
        if (slot < 0) return (closedAt(level, pos) & PipeSlots.bit(d)) != 0;
        return PipeSlots.closed(slotsAt(level, pos)[slot], d);
    }

    /** Whether the pipe at {@code pos} (slot, -1 = single) is a glass one; null: no pipe there. */
    @Nullable
    public static Boolean steel(BlockGetter level, BlockPos pos, BlockState s, int slot) {
        if (s.getBlock() instanceof PipeBlock p) return p.steel();
        if (s.getBlock() instanceof PipeBundleBlock && slot >= 0) {
            int v = slotsAt(level, pos)[slot];
            return PipeSlots.present(v) ? PipeSlots.steel(v) : null;
        }
        return null;
    }

    /**
     * Whether the pipe at {@code pos} (slot {@code slot}, -1 = single pipe) joins its neighbour on {@code d}. {@code current}:
     * the side as it is now, kept while the neighbour's chunk is not loaded (a chunk border must not cut a line).
     */
    public static boolean canJoin(BlockGetter level, BlockPos pos, int slot, Direction d, boolean current) {
        if (closed(level, pos, slot, d)) return false;
        BlockPos q = pos.relative(d);
        if (level instanceof LevelReader r && !r.hasChunkAt(q)) return current;
        BlockState n = level.getBlockState(q);
        Direction back = d.getOpposite();
        boolean bundle = slot >= 0;
        if (n.getBlock() instanceof PipeBundleBlock) {
            int[] ns = slotsAt(level, q);
            if (bundle) return PipeSlots.present(ns[slot]) && !PipeSlots.closed(ns[slot], back);
            return PipeSlots.facing(ns, back) >= 0;
        }
        if (n.getBlock() instanceof TankBlock) return true;
        // everything else meets one pipe of a bundle: the slot that faces it
        if (bundle && PipeSlots.facing(slotsAt(level, pos), d) != slot) return false;
        if (n.getBlock() instanceof PipeBlock) return (closedAt(level, q) & PipeSlots.bit(back)) == 0;
        if (n.getBlock() instanceof PipePort p && p.pipeRole(level, q, n, back) != null) return true;
        if (n.getBlock() instanceof PipeConnectable c && c.pipeConnects(n, back)) return true;
        if (n.isAir() || !(level instanceof Level l)) return false;
        return org.overengineer.bettacraft.platform.Platform.get().fluidHandler(l, q, back) != null;
    }

    /** The joined sides of the pipe at {@code pos} (slot, -1 = single) as they are now. */
    public static int joinedSides(BlockGetter level, BlockPos pos, BlockState s, int slot) {
        if (s.getBlock() instanceof PipeBlock) return PipeBlock.sides(s);
        if (s.getBlock() instanceof PipeBundleBlock && slot >= 0) return PipeSlots.sides(slotsAt(level, pos)[slot]);
        return 0;
    }

    /**
     * The pipe at {@code pos} that is joined on side {@code d}: 0 for a single pipe joined there, the slot of a bundle, or -1
     * (none). For a port or a device asking which line leaves it on that side ({@code d} points from the pipe to it).
     */
    public static int slotJoined(BlockGetter level, BlockPos pos, BlockState s, Direction d) {
        if (s.getBlock() instanceof PipeBlock) return s.getValue(PipeBlock.SIDES.get(d)) ? 0 : -1;
        if (s.getBlock() instanceof PipeBundleBlock) {
            int[] slots = slotsAt(level, pos);
            for (int i = 0; i < slots.length; i++) if (PipeSlots.present(slots[i]) && PipeSlots.joined(slots[i], d)) return i;
        }
        return -1;
    }

    /** Whether any pipe at {@code pos} is joined towards {@code d} (a tank's flange, an anchor in the interior). */
    public static boolean joinedTowards(BlockGetter level, BlockPos pos, BlockState s, Direction d) {
        if (s.getBlock() instanceof PipeBlock) return s.getValue(PipeBlock.SIDES.get(d));
        if (s.getBlock() instanceof PipeBundleBlock) {
            for (int v : slotsAt(level, pos)) if (PipeSlots.present(v) && PipeSlots.joined(v, d)) return true;
        }
        return false;
    }
}
