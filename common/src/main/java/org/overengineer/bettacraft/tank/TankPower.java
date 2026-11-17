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
 * Optional power (M9, PLAN 11 {@code power.*}): every tank stores Forge Energy for its equipment. Heaters and coolers,
 * filters and the lights on the tank each need FE per tick while the matching config switch is on (all off by
 * default). FE comes in through the energy capability of any tank block, of an attached external filter or cabinet
 * and of a light on the tank ({@link Storage}); every FE mod can deliver it. Without enough FE the group stops: heaters
 * and coolers do not change the temperature, filters do not clean, lights go dark. Priority: filters, then heaters /
 * coolers, then lights.
 */
public final class TankPower {
    private TankPower() {}

    /** M33 part b: {@code features.power.enabled}: off, no device needs FE (as with every power option off). */
    public static boolean power() {
        return org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.POWER);
    }

    public static boolean heaterRequired() {
        return power() && ServerConfig.getOr(ServerConfig.POWER_HEATER, false);
    }

    public static boolean filterRequired() {
        return power() && ServerConfig.getOr(ServerConfig.POWER_FILTER, false);
    }

    public static boolean lightRequired() {
        return power() && ServerConfig.getOr(ServerConfig.POWER_LIGHT, false);
    }

    /** M23: air pumps need FE. */
    public static boolean airRequired() {
        return power() && ServerConfig.getOr(ServerConfig.POWER_AIR_PUMP, false);
    }

    /** M31: misters need FE. */
    public static boolean misterRequired() {
        return power() && ServerConfig.getOr(ServerConfig.POWER_MISTER, false);
    }

    public static boolean anyRequired() {
        return heaterRequired() || filterRequired() || lightRequired() || airRequired() || misterRequired();
    }

    public static long capacity() {
        return ServerConfig.getOr(ServerConfig.POWER_BUFFER, 100000);
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
        return (int) Math.ceil(Math.min(cap, Math.max(1.0, t.waterBlocks())) * perBlock) + matDemand(c);
    }

    /** M31: FE per tick of the heat mats that heat (their duty of the last climate step). */
    public static int matDemand(TankBlockEntity c) {
        return c.climateMats <= 0 ? 0 : (int) Math.ceil(c.climateMats * ServerConfig.getOr(ServerConfig.POWER_HEAT_MAT_FE, 4.0) - 1e-9);
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

    /** One care step of {@code ticks} game ticks: decides which groups are powered and takes their FE. */
    public static void step(ServerLevel level, TankBlockEntity c, long ticks) {
        boolean wasLight = c.lightPowered;
        c.springFilters.clear(); // M37
        c.springHeat.clear();
        c.springUnits = 0;
        if (!anyRequired()) {
            c.heatPowered = c.filterPowered = c.lightPowered = c.airPowered = c.misterPowered = true;
            c.lastDemand = 0;
        } else {
            TankData t = c.getTank();
            // M23: each filter by its power regulator
            int filterFe = filterRequired() ? (int) Math.ceil(TankWater.filterLoad(level, c) * ServerConfig.getOr(ServerConfig.POWER_FILTER_FE, 10) - 1e-9) : 0;
            int airFe = airRequired() ? (int) Math.ceil(TankAeration.airDemand(level, c) * ServerConfig.getOr(ServerConfig.POWER_AIR_PUMP_FE, 2.0) - 1e-9) : 0;
            int heatFe = heaterRequired() ? heaterDemand(level, c) : 0;
            int lightFe = lightRequired() ? lightsWanting(level, t) * ServerConfig.getOr(ServerConfig.POWER_LIGHT_FE, 4) : 0;
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
            int mistFe = misterRequired() ? (int) Math.ceil(c.climateMistWant * ServerConfig.getOr(ServerConfig.POWER_MISTER_FE, 6.0) - 1e-9) : 0;
            c.misterPowered = mistFe <= 0 || buf >= mistFe * n;
            if (mistFe > 0 && c.misterPowered) buf -= mistFe * n;
            c.lastDemand = filterFe + airFe + heatFe + lightFe + mistFe;
            if (buf != t.powerBuffer()) {
                t.setPowerBuffer(buf);
                c.setChanged();
            }
            // M37: where the FE is short, the wound springs (time in unloaded chunks does not run them down)
            if (org.overengineer.bettacraft.winding.Winding.enabled()) {
                springs(level, c, Math.min(n, org.overengineer.bettacraft.winding.Winding.MAX_STEP), filterFe, airFe, heatFe, lightFe, mistFe);
            }
        }
        if (wasLight != c.lightPowered) updateLights(level, c.getTank());
    }

    // ------------------------------------------------------------------ M37: wound springs

    /**
     * M37: the springs stand in for the FE that is short ({@code n} ticks). First the wound cabinets under the tank: they
     * power a whole group (filters, air pumps, heaters / coolers and heat mats, lights, misters) for one device-tick per
     * working device and tick, the more devices the faster they run down. Then each wound external filter runs itself
     * and the heaters or coolers in it ({@link TankBlockEntity#springFilters}, {@link TankBlockEntity#springHeat}).
     */
    static void springs(ServerLevel level, TankBlockEntity c, long n, int filterFe, int airFe, int heatFe, int lightFe, int mistFe) {
        java.util.List<org.overengineer.bettacraft.equipment.CabinetBlockEntity> cabs = new java.util.ArrayList<>();
        if (org.overengineer.bettacraft.winding.Winding.Device.CABINET.windable()) {
            for (var cab : TankEquipment.cabinets(level, c)) if (!cab.windSpring().empty()) cabs.add(cab);
        }
        if (!cabs.isEmpty()) {
            if (filterFe > 0 && !c.filterPowered) c.filterPowered = fromCabinets(c, cabs, TankWater.filterLoad(level, c), n);
            if (airFe > 0 && !c.airPowered) {
                double perPump = Math.max(1, ServerConfig.getOr(ServerConfig.AIR_OUTLETS_PER_PUMP, 4));
                c.airPowered = fromCabinets(c, cabs, Math.max(1.0, TankAeration.airDemand(level, c) / perPump), n);
            }
            if (heatFe > 0 && !c.heatPowered) c.heatPowered = fromCabinets(c, cabs, heatUnits(level, c), n);
            if (lightFe > 0 && !c.lightPowered) c.lightPowered = fromCabinets(c, cabs, lightsWanting(level, c.getTank()), n);
            if (mistFe > 0 && !c.misterPowered) c.misterPowered = fromCabinets(c, cabs, c.climateMistWant, n);
        }
        boolean filters = filterFe > 0 && !c.filterPowered, heat = heatFe > 0 && !c.heatPowered;
        if (!filters && !heat || !org.overengineer.bettacraft.winding.Winding.Device.EXTERNAL_FILTER.windable()) return;
        TankData t = c.getTank();
        Boolean heating = t.waterMb() > 0 ? TankTemperature.heating(TankCare.ambient(level, t), t.targetTemp()) : null;
        for (var f : TankEquipment.filters(level, c)) {
            if (!(f instanceof org.overengineer.bettacraft.equipment.ExternalFilterBlockEntity e) || e.windSpring().empty()) continue;
            int heaters = !heat || heating == null ? 0 : heating ? f.heaterCount() - f.coolerCount() : f.coolerCount();
            double units = (filters ? f.powerShare() : 0) + heaters;
            if (units <= 0) continue;
            if (!e.windSpring().use((long) Math.ceil(units * n - 1e-9))) continue;
            if (filters) c.springFilters.add(f);
            if (heaters > 0) c.springHeat.add(f);
            c.springUnits += units;
        }
    }

    /** Takes {@code units} device-ticks for {@code n} ticks from the cabinets' springs (all or nothing). */
    private static boolean fromCabinets(TankBlockEntity c, java.util.List<org.overengineer.bettacraft.equipment.CabinetBlockEntity> cabs, double units, long n) {
        if (units <= 0) return true;
        long need = (long) Math.ceil(units * n - 1e-9), have = 0;
        for (var cab : cabs) have += cab.windSpring().ticks();
        if (have < need) return false;
        for (var cab : cabs) need -= cab.windSpring().drain(need);
        c.springUnits += units;
        return true;
    }

    /** M37: the heating (or cooling) devices that work now: heaters / coolers in the filters and in the interior, the heat mats. */
    static double heatUnits(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        double n = c.climateMats;
        Boolean heating = t.waterMb() > 0 ? TankTemperature.heating(TankCare.ambient(level, t), t.targetTemp()) : null;
        if (heating != null) {
            for (var f : TankEquipment.filters(level, c)) n += heating ? f.heaterCount() - f.coolerCount() : f.coolerCount();
            var r = org.overengineer.bettacraft.tank.interior.InteriorRegions.get(level.getServer()).get(t.regionId());
            if (r != null && r.built()) {
                var eq = org.overengineer.bettacraft.tank.interior.InteriorManager.equipment(level.getServer(), r);
                n += heating ? eq.heaters().size() : eq.coolers().size();
            }
        }
        return Math.max(1.0, n);
    }

    /** M37: whether this filter cleans now: the tank's filters have FE (or need none), or its own spring drives it. */
    public static boolean filterRuns(TankBlockEntity c, org.overengineer.bettacraft.equipment.FilterHost f) {
        return c.filterPowered || c.springFilters.contains(f);
    }

    /** M37: whether any attached filter runs now. */
    public static boolean anyFilterRuns(TankBlockEntity c) {
        return c.filterPowered || !c.springFilters.isEmpty();
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
            long n = Math.min(max, Math.max(0, capacity() - t.powerBuffer()));
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
            return (int) Math.min(Integer.MAX_VALUE, capacity());
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
