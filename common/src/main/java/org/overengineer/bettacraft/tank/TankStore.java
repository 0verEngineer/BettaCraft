package org.overengineer.bettacraft.tank;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.debug.LogArea;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity;
import org.overengineer.bettacraft.equipment.FilterHost;
import org.overengineer.bettacraft.winding.Windable;
import org.overengineer.bettacraft.winding.Winding;

/**
 * M67: the one energy store of a tank ({@link TankData#powerBuffer()}). Before M67 a tank had a fixed FE buffer
 * ({@code power.bufferFe}) and, next to it, the springs of its cabinets and external filters (M37 / M62). Now there is one
 * store for every device of the tank:
 * <ul>
 * <li>FE cables charge it through any tank block, external filter, cabinet or light ({@link TankPower.Storage}), Create's
 * rotation through the drive adapters and cabinet shafts ({@link TankDrive}), and the crank at a cabinet under the tank or at
 * an attached external filter ({@link #crank}, {@code power.crankFePerClick} a click).</li>
 * <li>Its capacity grows with the tank: {@code power.storePerWaterBlock} per water block the tank holds when full, plus
 * {@code power.storePerCabinet} per cabinet block under it (and, with {@code features.power.cabinetSharedSpring}, per cabinet
 * joined to those that stands under no other tank).</li>
 * <li>The springs of older worlds are turned into FE once ({@link #migrateSprings}, {@link #FE_PER_SPRING_TICK} per wound
 * device-tick), even above the capacity: nothing is lost, the excess simply runs down first.</li>
 * </ul>
 * Devices without a tank (food maker, fertilizer maker, osmosis, water pump and changer, multiblock filter) keep their own spring.
 */
public final class TankStore {
    private TankStore() {}

    /**
     * FE of one wound device-tick of an old spring (one tick of one device's work): 10, what a filter (the device the springs
     * ran most) took per tick on the old consumption (HARD).
     */
    public static final long FE_PER_SPRING_TICK = 10;
    /** Most cabinets one store collects (a huge build stays cheap to walk). */
    public static final int MAX_CABINETS = 1024;

    public static long perWaterBlock() {
        return ServerConfig.getOr(ServerConfig.POWER_STORE_PER_BLOCK, 80000);
    }

    public static long perCabinet() {
        return ServerConfig.getOr(ServerConfig.POWER_STORE_PER_CABINET, 20000);
    }

    public static long fePerClick() {
        return ServerConfig.getOr(ServerConfig.POWER_CRANK_FE, 6500);
    }

    /** {@code features.power.cabinetSharedSpring}: cabinets joined to the ones under the tank count too. */
    public static boolean joined() {
        return Features.enabled(Feature.CABINET_SHARED_SPRING);
    }

    /** {@code features.power.tankCrank} (and winding): the crank at a cabinet or an attached external filter charges the store. */
    public static boolean crankEnabled() {
        return Winding.enabled() && Features.enabled(Feature.TANK_CRANK);
    }

    /** Pure: the capacity of a tank of {@code waterBlocks} (when full) with {@code cabinets} cabinet blocks. */
    public static long capacity(double waterBlocks, int cabinets) {
        return Math.max(1, Math.round(Math.max(1.0, waterBlocks) * perWaterBlock()) + (long) Math.max(0, cabinets) * perCabinet());
    }

    /** Computes the capacity of the tank now (and keeps it for {@link #capacity(TankBlockEntity)}). */
    public static long capacity(Level level, TankBlockEntity c) {
        long cap = capacity(c.getTank().capacityMb() / (double) TankData.MB_PER_BLOCK, cabinets(level, c).size());
        c.storeCapacity = cap;
        return cap;
    }

    /** The capacity of the last care step (computed now if there was none yet). */
    public static long capacity(TankBlockEntity c) {
        if (c.storeCapacity > 0) return c.storeCapacity;
        Level level = c.getLevel();
        if (level == null || level.isClientSide()) return capacity(c.getTank().capacityMb() / (double) TankData.MB_PER_BLOCK, 0);
        return capacity(level, c);
    }

    /** 0..100 of the store (more than full after a migration shows 100). */
    public static int percent(TankBlockEntity c) {
        long cap = capacity(c);
        return (int) Math.round(Math.min(1.0, c.getTank().powerBuffer() / (double) Math.max(1, cap)) * 100);
    }

    /** In-game hours the store runs the tank at its demand of the last care step ("12.5"; "-" without a demand). */
    public static String hours(TankBlockEntity c) {
        if (c.lastDemand() <= 0) return "-";
        return String.format(java.util.Locale.ROOT, "%.1f", c.getTank().powerBuffer() / (double) c.lastDemand() / 1000.0);
    }

    // ------------------------------------------------------------------ the cabinets of a tank

    /**
     * The cabinets of the tank's store: those under it, and (with {@link #joined()}) those joined to them at the same height,
     * side by side, that stand under no tank. A joined cabinet that could belong to two tanks belongs to the nearer one
     * ({@link #owners}).
     */
    public static List<CabinetBlockEntity> cabinets(Level level, TankBlockEntity c) {
        List<CabinetBlockEntity> under = TankEquipment.cabinets(level, c);
        if (!joined() || under.isEmpty()) return under;
        List<CabinetBlockEntity> out = new ArrayList<>(under);
        Set<BlockPos> seen = new HashSet<>();
        for (CabinetBlockEntity cab : under) seen.add(cab.getBlockPos());
        Set<BlockPos> walked = new HashSet<>();
        for (CabinetBlockEntity cab : under) {
            if (walked.contains(cab.getBlockPos())) continue;
            Map<BlockPos, BlockPos> owner = owners(level, cab.getBlockPos());
            walked.addAll(owner.keySet());
            for (var e : owner.entrySet()) {
                if (!c.getBlockPos().equals(e.getValue()) || !seen.add(e.getKey())) continue;
                if (level.getBlockEntity(e.getKey()) instanceof CabinetBlockEntity j && !j.isRemoved()) out.add(j);
            }
        }
        return out;
    }

    /**
     * The cabinets joined to {@code start} (the same height, side by side), each with the controller position of the tank it
     * belongs to: a cabinet under a tank belongs to that tank, a joined one to the tank whose cabinets are fewest steps away
     * (a tie: the tank whose cabinet came first in the walk, which is fixed by the block positions). A joined cabinet no tank
     * reaches is left out.
     */
    static Map<BlockPos, BlockPos> owners(Level level, BlockPos start) {
        List<BlockPos> all = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        seen.add(start.immutable());
        todo.add(start.immutable());
        while (!todo.isEmpty() && all.size() < MAX_CABINETS) {
            BlockPos p = todo.poll();
            if (!level.isLoaded(p) || !(level.getBlockState(p).getBlock() instanceof CabinetBlock)) continue;
            all.add(p);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (seen.add(n)) todo.add(n);
            }
        }
        all.sort(java.util.Comparator.comparingLong(BlockPos::asLong));
        Map<BlockPos, BlockPos> owner = new java.util.LinkedHashMap<>();
        ArrayDeque<BlockPos> wave = new ArrayDeque<>();
        Set<BlockPos> member = new HashSet<>(all);
        for (BlockPos p : all) {
            TankBlockEntity t = FilterHost.tankOf(level, p.above());
            if (t == null) continue;
            owner.put(p, t.getBlockPos());
            wave.add(p);
        }
        while (!wave.isEmpty()) {
            BlockPos p = wave.poll();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (!member.contains(n) || owner.containsKey(n)) continue;
                owner.put(n, owner.get(p));
                wave.add(n);
            }
        }
        return owner;
    }

    /** The tank a cabinet belongs to: the one above it, else (joined) the nearest one above a cabinet joined to it. */
    @Nullable
    public static TankBlockEntity tankOf(Level level, CabinetBlockEntity cab) {
        TankBlockEntity above = FilterHost.tankOf(level, cab.getBlockPos().above());
        if (above != null || !joined()) return above;
        BlockPos ctrl = owners(level, cab.getBlockPos()).get(cab.getBlockPos());
        return ctrl == null ? null : TankMultiblock.getController(level, ctrl);
    }

    /** Whether this windable device charges a tank's store (a cabinet or an external filter) instead of its own spring. */
    public static boolean feedsTank(Windable w) {
        return w instanceof CabinetBlockEntity || w instanceof ExternalFilterBlockEntity;
    }

    /** The tank whose store a cabinet or an external filter charges, or null. */
    @Nullable
    public static TankBlockEntity tankOf(Level level, Windable w) {
        if (level == null) return null;
        if (w instanceof CabinetBlockEntity cab) return tankOf(level, cab);
        if (w instanceof ExternalFilterBlockEntity f) return FilterHost.tankOf(level, f.tankBlockPos());
        return null;
    }

    // ------------------------------------------------------------------ the crank

    /** One click of the crank: puts {@link #fePerClick()} into the store as far as it has room; returns what went in (0: full). */
    public static long crank(TankBlockEntity c) {
        TankData t = c.getTank();
        long room = capacity(c) - t.powerBuffer();
        long add = Math.min(room, fePerClick());
        if (add <= 0) return 0;
        t.setPowerBuffer(t.powerBuffer() + add);
        c.setChanged();
        return add;
    }

    // ------------------------------------------------------------------ migration of the springs (M37 / M62)

    /** FE of {@code ticks} wound device-ticks. */
    public static long springFe(long ticks) {
        return Math.max(0, ticks) * FE_PER_SPRING_TICK;
    }

    /**
     * The springs of the tank's cabinets and attached external filters (worlds from before M67) go into the store, once: their
     * device-ticks times {@link #FE_PER_SPRING_TICK}, also beyond the capacity (nothing is lost). Runs every care step and
     * costs nothing once the springs are empty.
     */
    public static long migrateSprings(Level level, TankBlockEntity c) {
        long fe = 0;
        for (CabinetBlockEntity cab : cabinets(level, c)) fe += take(cab);
        for (FilterHost f : TankEquipment.filters(level, c)) if (f instanceof ExternalFilterBlockEntity e) fe += take(e);
        if (fe > 0) {
            TankData t = c.getTank();
            t.setPowerBuffer(t.powerBuffer() + fe);
            c.setChanged();
            if (LogArea.TANK.debug) LogArea.TANK.debug("M67: wound springs of tank {} became {} FE in its store", c.getBlockPos(), fe);
        }
        return fe;
    }

    private static long take(Windable w) {
        var s = w.windSpring();
        if (s.empty()) return 0;
        long ticks = s.ticks();
        s.drain(ticks);
        return springFe(ticks);
    }

    /** A cabinet goes (broken, replaced): a spring it still holds goes into the store of its tank first. */
    public static void handOver(CabinetBlockEntity gone) {
        Level level = gone.getLevel();
        if (!(level instanceof ServerLevel) || gone.windSpring().empty()) return;
        TankBlockEntity c = tankOf(level, gone);
        if (c == null) return;
        long fe = take(gone);
        c.getTank().setPowerBuffer(c.getTank().powerBuffer() + fe);
        c.setChanged();
    }
}
