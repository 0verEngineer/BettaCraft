package org.overengineer.bettacraft.tank;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.energy.IEnergyStorage;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.registry.ModBlocks;

/**
 * Optional power (M9, PLAN 11 {@code power.*}): every tank has one energy store for its equipment (M67, {@link TankStore}).
 * With {@code power.costs} EASY or HARD (M67; before: one {@code *RequiresEnergy} switch per device group) heaters and
 * coolers, filters, air pumps, misters, heat mats and the lights on the tank need FE per tick (EASY a third of the
 * {@code *Fe*} values). The store is charged by the energy capability of any tank block, of an attached external filter or
 * cabinet and of a light on the tank ({@link Storage}), by Create's rotation ({@link TankDrive}) and by the crank at a cabinet
 * or an attached external filter ({@link TankStore#crank}). Without enough FE a group stops: heaters and coolers do not
 * change the temperature, filters do not clean, lights go dark. Priority: filters, air pumps, heaters / coolers, lights,
 * misters.
 */
public final class TankPower {
    private TankPower() {}

    /** M33 part b: {@code features.power.enabled}: off, no device needs FE (as with {@code power.costs} OFF). */
    public static boolean power() {
        return org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.POWER);
    }

    /** M67: {@code power.costs} (OFF while {@code features.power.enabled} is off). */
    public static ServerConfig.PowerCosts costs() {
        if (!power()) return ServerConfig.PowerCosts.OFF;
        return ServerConfig.getOr(ServerConfig.POWER_COSTS, ServerConfig.PowerCosts.OFF);
    }

    /** M67: whether the devices need FE at all ({@code power.costs} EASY or HARD). */
    public static boolean costsOn() {
        return costs() != ServerConfig.PowerCosts.OFF;
    }

    /** M67: the share of the FE values the devices take: 0 (OFF), 1/3 (EASY), 1 (HARD). */
    public static double factor() {
        return switch (costs()) {
            case OFF -> 0.0;
            case EASY -> 1.0 / 3.0;
            case HARD -> 1.0;
        };
    }

    /** M67: an FE value of the config ({@code *Fe*}, the HARD value) at the current costs. */
    public static double fe(double hard) {
        return hard * factor();
    }

    /** M67: an FE per tick at the current costs, rounded up (at least 1 while the HARD value is above 0 and costs are on). */
    public static int feInt(double hard) {
        if (hard <= 0 || !costsOn()) return 0;
        return Math.max(1, (int) Math.ceil(fe(hard) - 1e-9));
    }

    public static boolean heaterRequired() {
        return costsOn();
    }

    public static boolean filterRequired() {
        return costsOn();
    }

    public static boolean lightRequired() {
        return costsOn();
    }

    /** M23: air pumps need FE. */
    public static boolean airRequired() {
        return costsOn();
    }

    /** M31: misters need FE. */
    public static boolean misterRequired() {
        return costsOn();
    }

    public static boolean anyRequired() {
        return costsOn();
    }

    /** M67: the capacity of the tank's store ({@link TankStore}: grows with the tank and its cabinets). */
    public static long capacity(TankBlockEntity c) {
        return TankStore.capacity(c);
    }

    /** FE per tick the heaters or coolers need: only the type that works now, only for the water they handle. */
    public static int heaterDemand(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        Boolean heating = TankTemperature.heating(TankCare.ambient(level, t), t.targetTemp());
        if (heating == null || t.waterMb() <= 0) return matDemand(c); // M31: no water, no water heating
        double cap = heating ? TankCare.rawHeaterBlocks(level, t) : TankCare.rawCoolerBlocks(level, t);
        if (cap <= 0) return matDemand(c);
        double perBlock = heating ? ServerConfig.getOr(ServerConfig.POWER_HEATER_FE_PER_BLOCK, 8.0)
                : ServerConfig.getOr(ServerConfig.POWER_COOLER_FE_PER_BLOCK, 8.0);
        return feInt(Math.min(cap, Math.max(1.0, t.waterBlocks())) * perBlock) + matDemand(c);
    }

    /** M31: FE per tick of the heat mats that heat (their duty of the last climate step). */
    public static int matDemand(TankBlockEntity c) {
        return c.climateMats <= 0 ? 0 : feInt(c.climateMats * ServerConfig.getOr(ServerConfig.POWER_HEAT_MAT_FE, 4.0));
    }

    /** Light blocks on top of the tank that are on or want to be on (not switched off by redstone). */
    public static int lightsWanting(Level level, TankData tank) {
        int n = 0;
        for (BlockPos p : TankCare.aboveTop(tank)) {
            if (!level.isLoaded(p)) continue;
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof TankLightBlock && (s.getValue(TankLightBlock.LIT) || !TankLightBlock.barSwitchedOff(level, p, s))) n++;
        }
        return n;
    }

    /** One care step of {@code ticks} game ticks: decides which groups are powered and takes their FE from the store. */
    public static void step(ServerLevel level, TankBlockEntity c, long ticks) {
        boolean wasLight = c.lightPowered;
        TankStore.migrateSprings(level, c); // M67: the springs of an older world go into the store, once
        TankStore.capacity(level, c); // M67: the capacity of this step (water blocks, cabinets)
        if (!anyRequired()) {
            c.heatPowered = c.filterPowered = c.lightPowered = c.airPowered = c.misterPowered = true;
            c.lastDemand = 0;
        } else {
            TankData t = c.getTank();
            // M23: each filter by its power regulator
            int filterFe = feInt(TankWater.filterLoad(level, c) * ServerConfig.getOr(ServerConfig.POWER_FILTER_FE, 10));
            int airFe = feInt(TankAeration.airDemand(level, c) * ServerConfig.getOr(ServerConfig.POWER_AIR_PUMP_FE, 2.0));
            int heatFe = heaterDemand(level, c);
            int lightFe = feInt(lightsWanting(level, t) * (double) ServerConfig.getOr(ServerConfig.POWER_LIGHT_FE, 4));
            long buf = t.powerBuffer();
            long n = Math.max(1, ticks);
            c.filterPowered = filterFe <= 0 || buf >= filterFe * n;
            if (filterFe > 0 && c.filterPowered) buf -= filterFe * n;
            c.airPowered = airFe <= 0 || buf >= airFe * n;
            if (airFe > 0 && c.airPowered) buf -= airFe * n;
            c.heatPowered = heatFe <= 0 || buf >= heatFe * n;
            if (heatFe > 0 && c.heatPowered) buf -= heatFe * n;
            c.lightPowered = lightFe <= 0 || buf >= lightFe * n;
            if (lightFe > 0 && c.lightPowered) buf -= lightFe * n;
            // M31: the misters that want to spray, last
            int mistFe = feInt(c.climateMistWant * ServerConfig.getOr(ServerConfig.POWER_MISTER_FE, 6.0));
            c.misterPowered = mistFe <= 0 || buf >= mistFe * n;
            if (mistFe > 0 && c.misterPowered) buf -= mistFe * n;
            c.lastDemand = filterFe + airFe + heatFe + lightFe + mistFe;
            if (buf != t.powerBuffer()) {
                t.setPowerBuffer(buf);
                c.setChanged();
            }
        }
        if (wasLight != c.lightPowered) updateLights(level, c.getTank());
    }

    /** Whether this filter cleans now: the tank's filters have FE (or need none). M67: the springs of the filters are gone. */
    public static boolean filterRuns(TankBlockEntity c, org.overengineer.bettacraft.equipment.FilterHost f) {
        return c.filterPowered;
    }

    /** Whether any attached filter runs now. */
    public static boolean anyFilterRuns(TankBlockEntity c) {
        return c.filterPowered;
    }

    /**
     * Re-announces the energy capabilities of the tank and of everything around it that offers the tank's storage
     * (external filters, cabinets below, lights above): every member block and its neighbours. Mods that cache
     * capabilities hear the invalidation; cables that only look again on a block update (e.g. Powah) get a neighbour
     * update from each of those blocks.
     */
    public static void invalidateCapabilities(ServerLevel level, TankData tank) {
        java.util.Set<BlockPos> done = new java.util.LinkedHashSet<>();
        for (long l : tank.members()) {
            BlockPos p = BlockPos.of(l);
            done.add(p);
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) done.add(p.relative(d));
        }
        for (BlockPos p : done) {
            if (!level.isLoaded(p)) continue;
            org.overengineer.bettacraft.platform.Platform.get().invalidateCapabilities(level, p);
        }
        for (BlockPos p : done) {
            if (!level.isLoaded(p)) continue;
            BlockState s = level.getBlockState(p);
            if (!s.isAir()) level.updateNeighborsAt(p, s.getBlock());
        }
    }

    /** Lets the lights on the tank recompute whether they shine. */
    public static void updateLights(ServerLevel level, TankData tank) {
        for (BlockPos p : TankCare.aboveTop(tank)) {
            if (!level.isLoaded(p)) continue;
            var b = level.getBlockState(p).getBlock();
            if (b instanceof TankLightBlock) level.scheduleTick(p, b, 1); // M23: every tier
        }
    }

    /** Whether a light bar has power: lights on a tank need the tank's FE (if required); lights elsewhere always shine. */
    public static boolean lightHasPower(Level level, Iterable<BlockPos> bar) {
        if (!lightRequired()) return true;
        for (BlockPos p : bar) {
            TankBlockEntity c = level.isLoaded(p.below()) ? TankMultiblock.getController(level, p.below()) : null;
            if (c != null) return c.lightPowered;
        }
        return true;
    }

    // ------------------------------------------------------------------ capability

    /** Energy storage of the tank whose block is at {@code tankPos} (receive only); forwards to its controller. */
    public static final class Storage implements IEnergyStorage {
        private final Level level;
        private final java.util.function.Supplier<BlockPos> tankPos;

        public Storage(Level level, BlockPos tankPos) {
            BlockPos p = tankPos.immutable();
            this.level = level;
            this.tankPos = () -> p;
        }

        /** The tank block is looked up on every call (an external filter can turn to another tank). */
        public Storage(Level level, java.util.function.Supplier<BlockPos> tankPos) {
            this.level = level;
            this.tankPos = tankPos;
        }

        @Nullable
        private TankBlockEntity controller() {
            BlockPos p = tankPos.get();
            if (level == null || level.isClientSide || p == null || !level.isLoaded(p)) return null;
            return TankMultiblock.getController(level, p);
        }

        @Override
        public int receiveEnergy(int max, boolean simulate) {
            if (max <= 0 || !anyRequired()) return 0;
            TankBlockEntity c = controller();
            if (c == null) return 0;
            TankData t = c.getTank();
            long n = Math.min(max, Math.max(0, capacity(c) - t.powerBuffer()));
            if (!simulate && n > 0) {
                t.setPowerBuffer(t.powerBuffer() + n);
                c.setChanged();
            }
            return (int) n;
        }

        @Override
        public int extractEnergy(int max, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            TankBlockEntity c = controller();
            return c == null ? 0 : (int) Math.min(Integer.MAX_VALUE, c.getTank().powerBuffer());
        }

        @Override
        public int getMaxEnergyStored() {
            TankBlockEntity c = controller();
            return c == null ? 0 : (int) Math.min(Integer.MAX_VALUE, capacity(c));
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return anyRequired() && controller() != null;
        }
    }
}
