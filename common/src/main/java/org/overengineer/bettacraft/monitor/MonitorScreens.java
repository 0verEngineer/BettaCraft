package org.overengineer.bettacraft.monitor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * M27: monitor blocks join into screens. All monitor blocks with the same facing that touch in the plane of the screen
 * (above, below, left, right) form a group; the group is cut into rectangles of up to {@link #MAX_W} x {@link #MAX_H}
 * blocks, deterministically: the top left free block (as the viewer sees it) starts the next rectangle, and of all
 * rectangles that start there the largest is taken (the wider one on a tie). A wall of 8 x 5 blocks is one screen; a 9th
 * column becomes a screen of its own.
 * <p>
 * Each new screen takes over the state (layout, tanks, status) of the old screen it overlaps most, every old screen at most
 * once; the FE of all old screens of the group is shared out by size. A layout is clipped to the new size, so tiles stay
 * where they were when a screen grows.
 */
public final class MonitorScreens {
    public static final int MAX_W = 8, MAX_H = 5;
    /** Most blocks looked at in one group (a bigger wall of monitors is cut off there). */
    public static final int MAX_GROUP = 2048;

    private MonitorScreens() {}

    /** The state of an old screen. */
    public record Old(BlockPos origin, int w, int h, DashboardLayout layout, List<MonitorTank> tanks, int energy, int status, Set<BlockPos> members) {
        static Old of(MonitorBlockEntity c) {
            List<MonitorTank> tanks = new ArrayList<>();
            for (MonitorTank t : c.tanks()) tanks.add(t.copy());
            return new Old(c.origin(), c.width(), c.height(), c.layout().copy(), tanks, c.energy().getEnergyStored(), c.status(), new HashSet<>(c.members()));
        }
    }

    /** A new screen: its top left block and size. */
    public record Rect(BlockPos origin, int w, int h, List<BlockPos> members) {}

    /** The direction "to the right" on a screen facing {@code facing}, as its viewer sees it. */
    public static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    static int col(BlockPos p, Direction right) {
        return right.getStepX() * p.getX() + right.getStepZ() * p.getZ();
    }

    private static boolean isMonitor(ServerLevel level, BlockPos p, Direction facing) {
        if (!level.isLoaded(p)) return false;
        BlockState s = level.getBlockState(p);
        return s.getBlock() instanceof MonitorBlock && s.getValue(MonitorBlock.FACING) == facing;
    }

    /** The monitor blocks with {@code facing} connected to {@code seeds} in the plane of the screen. */
    static Set<BlockPos> group(ServerLevel level, Collection<BlockPos> seeds, Direction facing) {
        Direction right = MonitorScreens.right(facing);
        Direction[] dirs = {Direction.UP, Direction.DOWN, right, right.getOpposite()};
        Set<BlockPos> out = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos s : seeds) if (isMonitor(level, s, facing) && out.add(s.immutable())) queue.add(s.immutable());
        while (!queue.isEmpty() && out.size() < MAX_GROUP) {
            BlockPos p = queue.poll();
            for (Direction d : dirs) {
                BlockPos n = p.relative(d);
                if (!out.contains(n) && isMonitor(level, n, facing)) {
                    out.add(n);
                    queue.add(n);
                }
            }
        }
        return out;
    }

    /** Cuts {@code blocks} into screens (pure: also used by the unit test). */
    public static List<Rect> cut(Set<BlockPos> blocks, Direction facing) {
        Direction right = MonitorScreens.right(facing);
        List<BlockPos> sorted = new ArrayList<>(blocks);
        sorted.sort(Comparator.comparingInt((BlockPos p) -> -p.getY()).thenComparingInt(p -> col(p, right)));
        Set<BlockPos> free = new HashSet<>(blocks);
        List<Rect> out = new ArrayList<>();
        for (BlockPos p : sorted) {
            if (!free.contains(p)) continue;
            int maxW = 0;
            while (maxW < MAX_W && free.contains(p.relative(right, maxW))) maxW++;
            int bestW = 1, bestH = 1;
            for (int w = 1; w <= maxW; w++) {
                int h = 1;
                while (h < MAX_H && rowFree(free, p.below(h), right, w)) h++;
                if (w * h > bestW * bestH || (w * h == bestW * bestH && w > bestW)) {
                    bestW = w;
                    bestH = h;
                }
            }
            List<BlockPos> members = new ArrayList<>(bestW * bestH);
            for (int r = 0; r < bestH; r++) for (int c = 0; c < bestW; c++) members.add(p.relative(right, c).below(r));
            free.removeAll(members);
            out.add(new Rect(p, bestW, bestH, members));
        }
        return out;
    }

    private static boolean rowFree(Set<BlockPos> free, BlockPos start, Direction right, int w) {
        for (int c = 0; c < w; c++) if (!free.contains(start.relative(right, c))) return false;
        return true;
    }

    /**
     * Forms the screens again around {@code seeds} (a placed block, or the neighbours of a removed one). {@code removed}:
     * the state of a removed controller (its block entity is gone already).
     */
    public static void reform(ServerLevel level, Collection<BlockPos> seeds, Direction facing, @Nullable Old removed) {
        Set<BlockPos> group = group(level, seeds, facing);
        if (group.isEmpty()) return;
        // the old screens (controllers in the group, the removed one)
        Map<BlockPos, Old> old = new LinkedHashMap<>();
        if (removed != null) old.put(removed.origin(), removed);
        List<BlockPos> sortedGroup = new ArrayList<>(group);
        Direction right = MonitorScreens.right(facing);
        sortedGroup.sort(Comparator.comparingInt((BlockPos p) -> -p.getY()).thenComparingInt(p -> col(p, right)));
        for (BlockPos p : sortedGroup) {
            if (!(level.getBlockEntity(p) instanceof MonitorBlockEntity be) || be.isFresh()) continue;
            BlockPos o = be.origin();
            if (old.containsKey(o)) continue;
            if (group.contains(o) && level.getBlockEntity(o) instanceof MonitorBlockEntity c && c.isController() && !c.isFresh()) old.put(o, Old.of(c));
        }
        List<Rect> rects = cut(group, facing);
        // each new screen takes the old one it overlaps most (each old one once)
        record Pair(int rect, Old old, int overlap) {}
        List<Pair> pairs = new ArrayList<>();
        for (int i = 0; i < rects.size(); i++) {
            for (Old o : old.values()) {
                int n = 0;
                for (BlockPos m : rects.get(i).members()) if (o.members().contains(m)) n++;
                if (n > 0) pairs.add(new Pair(i, o, n));
            }
        }
        pairs.sort(Comparator.comparingInt((Pair p) -> -p.overlap()).thenComparingInt(p -> -p.old().w() * p.old().h()).thenComparingInt(Pair::rect));
        Old[] taken = new Old[rects.size()];
        Set<Old> used = new HashSet<>();
        for (Pair p : pairs) {
            if (taken[p.rect()] != null || used.contains(p.old())) continue;
            taken[p.rect()] = p.old();
            used.add(p.old());
        }
        long totalEnergy = 0;
        for (Old o : old.values()) totalEnergy += o.energy();
        int blocks = 0;
        for (Rect r : rects) blocks += r.w() * r.h();
        for (int i = 0; i < rects.size(); i++) {
            Rect r = rects.get(i);
            Old o = taken[i];
            boolean same = o != null && o.origin().equals(r.origin()) && o.w() == r.w() && o.h() == r.h();
            for (BlockPos m : r.members()) {
                if (!(level.getBlockEntity(m) instanceof MonitorBlockEntity be)) continue;
                if (!same || !be.origin().equals(r.origin()) || be.width() != r.w() || be.height() != r.h() || be.isFresh()) {
                    be.setScreen(r.origin(), r.w(), r.h());
                }
            }
            if (level.getBlockEntity(r.origin()) instanceof MonitorBlockEntity c) {
                int share = blocks == 0 ? 0 : (int) Math.min(Integer.MAX_VALUE, totalEnergy * (r.w() * r.h()) / blocks);
                if (o != null) c.inherit(o.layout().copy(), o.tanks(), share, o.status());
                else c.inherit(new DashboardLayout(), List.of(), share, c.status());
            }
            for (BlockPos m : r.members()) updateState(level, m, r, right);
            for (BlockPos m : r.members()) if (level.getBlockEntity(m) instanceof MonitorBlockEntity be) be.syncToClients();
        }
    }

    /** The frame flags of a block of {@code r}: on which sides the screen goes on. */
    private static void updateState(ServerLevel level, BlockPos p, Rect r, Direction right) {
        BlockState s = level.getBlockState(p);
        if (!(s.getBlock() instanceof MonitorBlock)) return;
        Set<BlockPos> in = new HashSet<>(r.members());
        BlockState n = s.setValue(MonitorBlock.UP, in.contains(p.above())).setValue(MonitorBlock.DOWN, in.contains(p.below()))
                .setValue(MonitorBlock.RIGHT, in.contains(p.relative(right))).setValue(MonitorBlock.LEFT, in.contains(p.relative(right.getOpposite())));
        if (n != s) level.setBlock(p, n, Block.UPDATE_CLIENTS);
    }
}
