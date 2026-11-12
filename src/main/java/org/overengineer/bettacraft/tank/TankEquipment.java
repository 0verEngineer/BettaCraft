package org.overengineer.bettacraft.tank;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.equipment.CabinetBlock;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;
import org.overengineer.bettacraft.equipment.ExternalFilterBlock;
import org.overengineer.bettacraft.equipment.FilterHeaters;
import org.overengineer.bettacraft.equipment.FilterHost;

/**
 * Equipment around a tank (M4): external filter blocks next to the walls (facing a block of the tank) and cabinets
 * directly below the bottom layer (PLAN 6.2 / 6.5). The positions are cached in the controller and rescanned at most
 * once per second; every use re-checks the block entity.
 */
public final class TankEquipment {
    private static final long RESCAN_TICKS = 20;

    private TankEquipment() {}

    private static void scanIfNeeded(Level level, TankBlockEntity c) {
        long now = level.getGameTime();
        long last = c.equipmentScanTime;
        if (last != Long.MIN_VALUE && now >= last && now - last < RESCAN_TICKS) return;
        c.equipmentScanTime = now;
        TankData t = c.getTank();
        List<BlockPos> filters = new ArrayList<>(), cabinets = new ArrayList<>();
        BlockPos mn = t.min(), mx = t.max();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = mn.getY(); y <= mx.getY(); y++) {
            for (int x = mn.getX(); x <= mx.getX(); x++) {
                check(level, t, p.set(x, y, mn.getZ() - 1), Direction.SOUTH, filters);
                check(level, t, p.set(x, y, mx.getZ() + 1), Direction.NORTH, filters);
            }
            for (int z = mn.getZ(); z <= mx.getZ(); z++) {
                check(level, t, p.set(mn.getX() - 1, y, z), Direction.EAST, filters);
                check(level, t, p.set(mx.getX() + 1, y, z), Direction.WEST, filters);
            }
        }
        int below = mn.getY() - 1;
        for (int x = mn.getX(); x <= mx.getX(); x++)
            for (int z = mn.getZ(); z <= mx.getZ(); z++) {
                p.set(x, below, z);
                if (!level.isLoaded(p) || !t.contains(p.above())) continue;
                if (level.getBlockState(p).getBlock() instanceof CabinetBlock) cabinets.add(p.immutable());
            }
        c.filterCache = List.copyOf(filters);
        c.cabinetCache = List.copyOf(cabinets);
    }

    private static void check(Level level, TankData t, BlockPos p, Direction towardsTank, List<BlockPos> out) {
        if (!level.isLoaded(p)) return;
        BlockState s = level.getBlockState(p);
        if (s.getBlock() instanceof ExternalFilterBlock && s.getValue(ExternalFilterBlock.FACING) == towardsTank && t.contains(p.relative(towardsTank))) {
            out.add(p.immutable());
        }
    }

    /** Forget the cached positions (after placing / breaking equipment, so a test or menu sees it at once). */
    public static void invalidate(TankBlockEntity c) {
        c.equipmentScanTime = Long.MIN_VALUE;
    }

    /** Filters attached to the tank: filter blocks and cabinets below the tank that hold a filter. */
    public static List<FilterHost> filters(Level level, TankBlockEntity c) {
        scanIfNeeded(level, c);
        List<FilterHost> out = new ArrayList<>();
        for (BlockPos p : c.filterCache) {
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof FilterHost f && f.hasFilter()) out.add(f);
        }
        for (CabinetBlockEntity cab : cabinets(level, c)) if (cab.hasFilter()) out.add(cab);
        return out;
    }

    /** Cabinets directly below the tank. */
    public static List<CabinetBlockEntity> cabinets(Level level, TankBlockEntity c) {
        scanIfNeeded(level, c);
        List<CabinetBlockEntity> out = new ArrayList<>();
        for (BlockPos p : c.cabinetCache) {
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof CabinetBlockEntity cab) out.add(cab);
        }
        return out;
    }

    /** Number of filter blocks next to the tank (without cabinet filters). */
    public static int filterBlockCount(Level level, TankBlockEntity c) {
        int n = 0;
        for (FilterHost f : filters(level, c)) if (!(f instanceof CabinetBlockEntity)) n++;
        return n;
    }

    /**
     * Migration of the old heater slots (schema v3): heaters left in them move into free heater slots of attached
     * filters. Returns the number moved. Nothing is lost: what does not fit stays in the old slots.
     */
    public static int moveLegacyHeaters(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        if (!t.hasLegacyHeaters()) return 0;
        List<FilterHost> filters = filters(level, c);
        if (filters.isEmpty()) return 0;
        int moved = 0;
        var legacy = t.legacyHeaters();
        for (int i = 0; i < legacy.getContainerSize(); i++) {
            ItemStack s = legacy.getItem(i);
            if (s.isEmpty() || !FilterHeaters.isHeater(s)) continue;
            for (FilterHost f : filters) {
                while (!s.isEmpty() && f.freeHeaterSlots() > 0 && f.insertHeater(s.copyWithCount(1))) {
                    s.shrink(1);
                    moved++;
                }
            }
            legacy.setItem(i, s.isEmpty() ? ItemStack.EMPTY : s);
        }
        if (moved > 0) {
            TankCare.log(level, c, "log.bettacraft.heaters_moved", String.valueOf(moved));
            c.setChanged();
        }
        return moved;
    }
}
