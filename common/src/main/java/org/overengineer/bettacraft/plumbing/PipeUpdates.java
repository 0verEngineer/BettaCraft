package org.overengineer.bettacraft.plumbing;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.platform.event.LevelTickEvent;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M68: keeps the rings of the pipes right (their network's role, {@link PipeNetwork#ring()}). Every change of a pipe or of a
 * block next to one schedules its position; at the end of the level's tick the networks of the pipes there and next to it are
 * built (each once) and every pipe whose ring differs gets the new one (a single pipe in its block state without block updates,
 * a bundle slot in its block entity; the clients redraw them). So a line that reaches a filter's inlet turns blue, one cut off
 * from it plain again, without anyone asking for the network. A pipe loaded from before M68 ({@link PipeBlockEntity#legacy()})
 * is joined again first ({@code legacy}). Where an intake or return line docks on a tank, its connection appears in the
 * interior right away (the M40 dock, {@code TankAnchors}). At most {@link #BUDGET} positions per tick; the rest waits.
 */
public final class PipeUpdates {
    private PipeUpdates() {}

    public static final int BUDGET = 512;

    private static final Map<Level, LongLinkedOpenHashSet> PENDING = new WeakHashMap<>();
    private static final Map<Level, LongLinkedOpenHashSet> LEGACY = new WeakHashMap<>();

    /** Looks at the pipes at {@code pos} and next to it at the end of this tick ({@code legacy}: join the pipe there again first). */
    public static void schedule(Level level, BlockPos pos, boolean legacy) {
        if (level.isClientSide) return;
        synchronized (PENDING) {
            PENDING.computeIfAbsent(level, k -> new LongLinkedOpenHashSet()).add(pos.asLong());
            if (legacy) LEGACY.computeIfAbsent(level, k -> new LongLinkedOpenHashSet()).add(pos.asLong());
        }
    }

    /** Forgets everything scheduled (tests). */
    public static void forget() {
        synchronized (PENDING) {
            PENDING.clear();
            LEGACY.clear();
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) run(level, BUDGET);
    }

    /** Does everything scheduled for this level now (GameTests: right after building, before they look at the rings). */
    public static void flush(Level level) {
        for (int i = 0; i < 64 && run(level, Integer.MAX_VALUE); i++) {
            // joining a legacy pipe again may schedule more
        }
    }

    /** Works off up to {@code budget} positions; true when it did something. */
    private static boolean run(Level level, int budget) {
        long[] todo, legacy;
        synchronized (PENDING) {
            LongLinkedOpenHashSet p = PENDING.get(level);
            if (p == null || p.isEmpty()) return false;
            int n = Math.min(budget, p.size());
            todo = new long[n];
            for (int i = 0; i < n; i++) todo[i] = p.removeFirstLong();
            LongLinkedOpenHashSet l = LEGACY.get(level);
            if (l != null && !l.isEmpty()) {
                it.unimi.dsi.fastutil.longs.LongArrayList hit = new it.unimi.dsi.fastutil.longs.LongArrayList();
                for (long k : todo) if (l.remove(k)) hit.add(k);
                legacy = hit.toLongArray();
            } else legacy = new long[0];
        }
        for (long k : legacy) rejoin(level, BlockPos.of(k));
        Set<PipeNetwork> done = Collections.newSetFromMap(new IdentityHashMap<>());
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        for (long k : todo) {
            BlockPos p = BlockPos.of(k);
            if (!level.isLoaded(p)) continue;
            apply(level, p, done);
            for (Direction d : Direction.values()) {
                q.setWithOffset(p, d);
                if (level.isLoaded(q)) apply(level, q.immutable(), done);
            }
        }
        return true;
    }

    /** A pipe loaded from before M68: its sides looked at again (its closed sides keep its old connections). */
    private static void rejoin(Level level, BlockPos pos) {
        if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof PipeBlockEntity be)) return;
        BlockState s = level.getBlockState(pos);
        if (s.getBlock() instanceof PipeBlock) {
            BlockState now = PipeBlock.withConnections(s, level, pos);
            if (now != s) level.setBlock(pos, now, Block.UPDATE_ALL);
        } else if (s.getBlock() instanceof PipeBundleBlock) {
            int[] now = PipeBundleBlock.rejoin(level, pos, be.slots(), Direction.values());
            if (!java.util.Arrays.equals(now, be.slots())) {
                be.setSlots(now);
                PipeBundleBlock.after(level, pos);
            }
        }
        be.migrated();
        PipeNetwork.invalidate(level, pos);
    }

    private static void apply(Level level, BlockPos pos, Set<PipeNetwork> done) {
        BlockState s = level.getBlockState(pos);
        if (s.getBlock() instanceof PipeBlock) {
            apply(level, PipeNetwork.of(level, pos, 0), done);
        } else if (s.getBlock() instanceof PipeBundleBlock) {
            int[] slots = Pipes.slotsAt(level, pos);
            for (int i = 0; i < slots.length; i++) if (PipeSlots.present(slots[i])) apply(level, PipeNetwork.of(level, pos, i), done);
        }
    }

    /** Gives every pipe of the network its ring (once per pass). */
    public static void apply(Level level, PipeNetwork n, Set<PipeNetwork> done) {
        if (n == null || !done.add(n)) return;
        PipeRing ring = n.ring();
        for (int i = 0; i < n.size(); i++) {
            BlockPos p = n.node(i);
            if (!level.isLoaded(p)) continue;
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof PipeBlock) {
                if (s.getValue(PipeBlock.RING) != ring) level.setBlock(p, s.setValue(PipeBlock.RING, ring), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            } else if (s.getBlock() instanceof PipeBundleBlock && level.getBlockEntity(p) instanceof PipeBlockEntity be) {
                int slot = n.nodeSlot(i), v = be.slot(slot);
                if (PipeSlots.present(v) && PipeSlots.ring(v) != ring) be.setSlot(slot, PipeSlots.withRing(v, ring));
            }
        }
        // M40 (E2): where a filter line docks on a tank, its connection appears in the interior right away
        if (level instanceof ServerLevel sl && n.role() != null && n.role().filterLine() && Features.enabled(Feature.FILTER_LINES)) {
            for (PipeNetwork.Dock d : n.docks()) {
                if (!level.isLoaded(d.member()) || !(level.getBlockState(d.member()).getBlock() instanceof TankBlock)) continue;
                TankBlockEntity c = TankMultiblock.getController(level, d.member());
                if (c == null) continue;
                if (org.overengineer.bettacraft.plumbing.interior.TankAnchors.existing(sl, c, d.member(), d.face()) == Long.MIN_VALUE)
                    org.overengineer.bettacraft.plumbing.interior.TankAnchors.ensure(sl, c, d.member(), d.face());
            }
        }
    }
}
