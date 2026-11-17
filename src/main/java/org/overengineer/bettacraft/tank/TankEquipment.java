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
 * directly below the tank (PLAN 6.2 / 6.5); since M10 along every outer face of a free shape. The positions are cached in the controller and rescanned at most
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
        List<BlockPos> filters = new ArrayList<>(), cabinets = new ArrayList<>(), skimmers = new ArrayList<>(), pumps = new ArrayList<>(), co2 = new ArrayList<>();
        List<BlockPos> mats = new ArrayList<>(), misters = new ArrayList<>(); // M31
        List<BlockPos> drives = new ArrayList<>(); // M41
        TankDrive.Bridge drive = TankDrive.BRIDGE;
        // M10: every outer side face of the shape (also the walls of a notch), and below every block without a tank
        // block under it (the bottom layer of a cuboid; for a free shape also under raised parts)
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        // M17: the outer faces are worked out once per tank (its members never change), not every second for every member
        if (c.equipmentFacesOf != t) {
            it.unimi.dsi.fastutil.longs.LongArrayList faces = new it.unimi.dsi.fastutil.longs.LongArrayList();
            it.unimi.dsi.fastutil.bytes.ByteArrayList dirs = new it.unimi.dsi.fastutil.bytes.ByteArrayList();
            for (long l : t.members()) {
                for (Direction d : Direction.values()) {
                    if (d == Direction.UP || t.contains(BlockPos.offset(l, d))) continue;
                    faces.add(BlockPos.offset(l, d));
                    dirs.add((byte) d.get3DDataValue());
                }
            }
            c.equipmentFaces = faces.toLongArray();
            c.equipmentFaceDirs = dirs.toByteArray();
            c.equipmentFacesOf = t;
        }
        for (int i = 0; i < c.equipmentFaces.length; i++) {
            Direction d = Direction.from3DDataValue(c.equipmentFaceDirs[i]);
            p.set(c.equipmentFaces[i]);
            // M41: a drive adapter with its back on this face (on a wall or under the tank)
            if (drive != null && level.isLoaded(p) && drive.isAdapter(level.getBlockState(p), d)) drives.add(p.immutable());
            if (d == Direction.DOWN) {
                if (level.isLoaded(p) && level.getBlockState(p).getBlock() instanceof CabinetBlock) cabinets.add(p.immutable());
                if (level.isLoaded(p) && isMat(level.getBlockState(p), Direction.UP)) mats.add(p.immutable()); // M31: a heat mat under the floor
            } else {
                // M31: a heat mat stuck to the wall, a mister beside the tank
                if (level.isLoaded(p) && isMat(level.getBlockState(p), d.getOpposite())) mats.add(p.immutable());
                if (level.isLoaded(p) && level.getBlockState(p).getBlock() instanceof org.overengineer.bettacraft.terrarium.MisterBlock) misters.add(p.immutable());
                check(level, t, p, d.getOpposite(), filters, skimmers);
                // M23: an air pump beside the tank (it serves the first tank it touches)
                if (level.isLoaded(p) && level.getBlockState(p).getBlock() instanceof org.overengineer.bettacraft.equipment.AirPumpBlock) pumps.add(p.immutable());
                if (level.isLoaded(p) && level.getBlockState(p).getBlock() instanceof org.overengineer.bettacraft.co2.Co2SourceBlock) co2.add(p.immutable()); // M25
            }
        }
        for (BlockPos top : t.openTops()) {
            if (level.isLoaded(top) && level.getBlockState(top).getBlock() instanceof org.overengineer.bettacraft.terrarium.MisterBlock) misters.add(top.immutable()); // M31
            if (level.isLoaded(top) && level.getBlockState(top).getBlock() instanceof org.overengineer.bettacraft.equipment.AirPumpBlock) pumps.add(top.immutable());
            if (level.isLoaded(top) && level.getBlockState(top).getBlock() instanceof org.overengineer.bettacraft.co2.Co2SourceBlock) co2.add(top.immutable()); // M25
        }
        filters.sort(java.util.Comparator.comparingLong(BlockPos::asLong));
        cabinets.sort(java.util.Comparator.comparingLong(BlockPos::asLong));
        c.filterCache = List.copyOf(filters);
        c.cabinetCache = List.copyOf(cabinets);
        drives.sort(java.util.Comparator.comparingLong(BlockPos::asLong));
        c.driveCache = List.copyOf(new java.util.LinkedHashSet<>(drives));
        skimmers.sort(java.util.Comparator.comparingLong(BlockPos::asLong));
        c.skimmerCache = List.copyOf(skimmers);
        List<BlockPos> mine = new ArrayList<>();
        for (BlockPos ap : new java.util.LinkedHashSet<>(pumps)) {
            BlockPos tb = org.overengineer.bettacraft.equipment.AirPumpBlockEntity.tankBlock(level, ap);
            if (tb != null && t.contains(tb)) mine.add(ap);
        }
        mine.sort(java.util.Comparator.comparingLong(BlockPos::asLong));
        c.airPumpCache = List.copyOf(mine);
        // M25: CO2 sources serve the first tank they touch, like the air pumps
        List<BlockPos> gas = new ArrayList<>();
        for (BlockPos sp : new java.util.LinkedHashSet<>(co2)) {
            BlockPos tb = org.overengineer.bettacraft.equipment.AirPumpBlockEntity.tankBlock(level, sp);
            if (tb != null && t.contains(tb)) gas.add(sp);
        }
        gas.sort(java.util.Comparator.comparingLong(BlockPos::asLong));
        c.co2Cache = List.copyOf(gas);
        // M31: heat mats (deduplicated: a mat on an inner corner touches two faces) and the misters that serve this tank
        c.heatMatCache = List.copyOf(new java.util.LinkedHashSet<>(mats));
        List<BlockPos> mist = new ArrayList<>();
        for (BlockPos mp : new java.util.LinkedHashSet<>(misters)) {
            BlockPos tb = org.overengineer.bettacraft.equipment.AirPumpBlockEntity.tankBlock(level, mp);
            if (tb != null && t.contains(tb)) mist.add(mp);
        }
        mist.sort(java.util.Comparator.comparingLong(BlockPos::asLong));
        c.misterCache = List.copyOf(mist);
    }

    private static boolean isMat(BlockState s, Direction towardsTank) {
        return s.getBlock() instanceof org.overengineer.bettacraft.terrarium.HeatMatBlock
                && s.getValue(org.overengineer.bettacraft.terrarium.HeatMatBlock.FACING) == towardsTank;
    }

    /** M31: heat mats on this tank (under its floor or on its walls). */
    public static List<BlockPos> heatMats(Level level, TankBlockEntity c) {
        scanIfNeeded(level, c);
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : c.heatMatCache) if (level.isLoaded(p) && level.getBlockState(p).getBlock() instanceof org.overengineer.bettacraft.terrarium.HeatMatBlock) out.add(p);
        return out;
    }

    /** M31: misters on or beside this tank. */
    public static List<org.overengineer.bettacraft.terrarium.MisterBlockEntity> misters(Level level, TankBlockEntity c) {
        scanIfNeeded(level, c);
        List<org.overengineer.bettacraft.terrarium.MisterBlockEntity> out = new ArrayList<>();
        for (BlockPos p : c.misterCache) if (level.isLoaded(p) && level.getBlockEntity(p) instanceof org.overengineer.bettacraft.terrarium.MisterBlockEntity m) out.add(m);
        return out;
    }

    /** M25: CO2 sources (bio CO2 bottles, gas cylinders) that serve this tank. */
    public static List<BlockPos> co2Sources(Level level, TankBlockEntity c) {
        scanIfNeeded(level, c);
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : c.co2Cache) if (level.isLoaded(p) && level.getBlockState(p).getBlock() instanceof org.overengineer.bettacraft.co2.Co2SourceBlock) out.add(p);
        return out;
    }

    /** M23: air pumps that serve this tank (M29.1: also the air pumps in the cabinets under it). */
    public static List<org.overengineer.bettacraft.equipment.AirSource> airPumps(Level level, TankBlockEntity c) {
        scanIfNeeded(level, c);
        List<org.overengineer.bettacraft.equipment.AirSource> out = new ArrayList<>();
        for (BlockPos p : c.airPumpCache) {
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof org.overengineer.bettacraft.equipment.AirPumpBlockEntity a) out.add(a);
        }
        for (BlockPos p : c.cabinetCache) {
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof org.overengineer.bettacraft.equipment.CabinetBlockEntity cab && cab.hasAirPump()) out.add(cab);
        }
        return out;
    }

    private static void check(Level level, TankData t, BlockPos p, Direction towardsTank, List<BlockPos> out, List<BlockPos> skimmers) {
        if (!level.isLoaded(p)) return;
        BlockState s = level.getBlockState(p);
        if (s.getBlock() instanceof ExternalFilterBlock && s.getValue(ExternalFilterBlock.FACING) == towardsTank && t.contains(p.relative(towardsTank))) {
            out.add(p.immutable());
        } else if (s.getBlock() instanceof org.overengineer.bettacraft.reef.ProteinSkimmerBlock
                && s.getValue(org.overengineer.bettacraft.reef.ProteinSkimmerBlock.FACING) == towardsTank && t.contains(p.relative(towardsTank))) {
            skimmers.add(p.immutable()); // M20
        }
    }

    /** M20: protein skimmers hanging on the tank. */
    public static List<BlockPos> skimmers(Level level, TankBlockEntity c) {
        scanIfNeeded(level, c);
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : c.skimmerCache) {
            if (level.isLoaded(p) && level.getBlockState(p).getBlock() instanceof org.overengineer.bettacraft.reef.ProteinSkimmerBlock) out.add(p);
        }
        return out;
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
        out.addAll(org.overengineer.bettacraft.equipment.Sump.hosts(level, c)); // M49: the return pumps of the sumps
        return out;
    }

    /** M41: the drive adapters on this tank (only with Create). */
    public static List<BlockPos> driveAdapters(Level level, TankBlockEntity c) {
        scanIfNeeded(level, c);
        return c.driveCache;
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
