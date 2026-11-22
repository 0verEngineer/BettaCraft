package org.overengineer.bettacraft.equipment;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankEquipment;
import org.overengineer.bettacraft.winding.WindSpring;

/**
 * M62 (question A2): one spring store for the whole under-cabinet ({@code features.power.cabinetSharedSpring}). The store of
 * a cabinet is every cabinet joined to it at the same height (side by side, front and back, whatever way they face) and every
 * cabinet under a tank above any of them, also one that touches none of the others (e.g. the two ends of a long tank), with
 * the cabinets joined to those in turn.
 * <p>
 * The winding still lives in the springs of the blocks (nothing to migrate: the springs of an older world simply form the
 * store); {@link Store} is a view over them that keeps them evenly filled. Its capacity grows with every block; one click of
 * the crank at any block winds one block's step into the whole store; a broken block hands its share to the others as far as
 * they have room ({@link #handOver}).
 */
public final class CabinetSprings {
    /** Most cabinets one store collects (a huge build stays cheap to walk). */
    public static final int MAX = 1024;

    private CabinetSprings() {}

    public static boolean shared() {
        return Features.enabled(Feature.CABINET_SHARED_SPRING);
    }

    /** The cabinets of the store of {@code cab} (itself first); just {@code cab} while the switch is off or on the client. */
    public static List<CabinetBlockEntity> store(CabinetBlockEntity cab) {
        Level level = cab.getLevel();
        if (level == null || level.isClientSide() || !shared()) return List.of(cab);
        return collect(level, cab.getBlockPos());
    }

    /** Walks the store from {@code start} (a cabinet position). */
    static List<CabinetBlockEntity> collect(Level level, BlockPos start) {
        List<CabinetBlockEntity> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Set<BlockPos> tanks = new HashSet<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        todo.add(start.immutable());
        seen.add(start.immutable());
        while (!todo.isEmpty() && out.size() < MAX) {
            BlockPos p = todo.poll();
            if (!level.isLoaded(p) || !(level.getBlockEntity(p) instanceof CabinetBlockEntity cab) || cab.isRemoved()) continue;
            out.add(cab);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (!seen.contains(n) && level.isLoaded(n) && level.getBlockState(n).getBlock() instanceof CabinetBlock) {
                    seen.add(n);
                    todo.add(n);
                }
            }
            // the cabinets under the tank above, also those that do not touch this one
            TankBlockEntity c = FilterHost.tankOf(level, p.above());
            if (c != null && tanks.add(c.getBlockPos())) {
                for (CabinetBlockEntity o : TankEquipment.cabinets(level, c)) {
                    BlockPos q = o.getBlockPos();
                    if (seen.add(q)) todo.add(q);
                }
            }
        }
        return out;
    }

    /** The union of the stores of these cabinets (the cabinets under a tank and everything joined to them). */
    public static List<CabinetBlockEntity> union(Level level, List<CabinetBlockEntity> cabs) {
        if (!shared() || cabs.isEmpty()) return cabs;
        java.util.LinkedHashSet<CabinetBlockEntity> all = new java.util.LinkedHashSet<>();
        for (CabinetBlockEntity c : cabs) {
            if (all.contains(c)) continue;
            all.addAll(collect(level, c.getBlockPos()));
        }
        return new ArrayList<>(all);
    }

    /** Spreads the winding of these cabinets evenly over them again (the total stays). */
    public static void level(List<CabinetBlockEntity> cabs) {
        if (cabs.size() < 2) return;
        Store s = new Store(cabs);
        s.set(s.ticks());
    }

    /**
     * A cabinet of a store goes (broken, replaced): its winding moves into the other cabinets of the store as far as they have
     * room; what does not fit is lost with the block.
     */
    public static void handOver(CabinetBlockEntity gone) {
        Level level = gone.getLevel();
        if (level == null || level.isClientSide() || !shared() || gone.windSpring().empty()) return;
        List<CabinetBlockEntity> rest = new ArrayList<>(collect(level, gone.getBlockPos()));
        rest.remove(gone);
        if (rest.isEmpty()) return;
        Store s = new Store(rest);
        long free = s.capacity() - s.ticks();
        long moved = Math.min(free, gone.windSpring().ticks());
        if (moved <= 0) return;
        s.set(s.ticks() + moved);
        gone.windSpring().drain(moved);
    }

    /** The store view of a cabinet (its own spring when it stands alone). */
    public static WindSpring of(CabinetBlockEntity cab) {
        List<CabinetBlockEntity> cabs = store(cab);
        return cabs.size() < 2 ? cab.windSpring() : new Store(cabs);
    }

    /** M62: the springs of several cabinets as one: sums, and every change spreads evenly over them. */
    public static final class Store extends WindSpring {
        private final List<CabinetBlockEntity> cabs;

        public Store(List<CabinetBlockEntity> cabs) {
            super(() -> 1, () -> 1, () -> {});
            this.cabs = List.copyOf(cabs);
        }

        /** The cabinets of the store. */
        public int size() {
            return cabs.size();
        }

        @Override
        public long ticks() {
            long n = 0;
            for (CabinetBlockEntity c : cabs) n += c.windSpring().ticks();
            return n;
        }

        @Override
        public long capacity() {
            long n = 0;
            for (CabinetBlockEntity c : cabs) n += c.windSpring().capacity();
            return Math.max(1, n);
        }

        /** One click winds what it winds at one cabinet. */
        @Override
        public long step() {
            return cabs.isEmpty() ? 1 : cabs.getFirst().windSpring().step();
        }

        /** Sets the total, spread over the cabinets by their capacity (the remainder from the first). */
        @Override
        public void set(long v) {
            long total = Math.max(0, Math.min(capacity(), v)), cap = capacity(), left = total;
            long[] want = new long[cabs.size()];
            for (int i = 0; i < cabs.size(); i++) {
                WindSpring s = cabs.get(i).windSpring();
                want[i] = (long) Math.floor(s.capacity() * (double) total / cap);
                left -= want[i];
            }
            for (int i = 0; i < cabs.size() && left > 0; i++) {
                long room = cabs.get(i).windSpring().capacity() - want[i];
                long add = Math.min(room, left);
                want[i] += add;
                left -= add;
            }
            for (int i = 0; i < cabs.size(); i++) cabs.get(i).windSpring().set(want[i]);
        }

        @Override
        public boolean use(long n) {
            if (n <= 0) return true;
            long t = ticks();
            if (t < n) return false;
            set(t - n);
            return true;
        }

        @Override
        public long drain(long n) {
            long t = ticks(), took = Math.max(0, Math.min(n, t));
            if (took > 0) set(t - took);
            return took;
        }

        @Override
        public void save(net.minecraft.nbt.CompoundTag t, String key) {
            // a view: the blocks save their own springs
        }

        @Override
        public void load(net.minecraft.nbt.CompoundTag t, String key) {
        }
    }
}
