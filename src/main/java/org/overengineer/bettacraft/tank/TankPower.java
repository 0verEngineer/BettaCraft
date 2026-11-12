package org.overengineer.bettacraft.tank;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
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

    public static boolean heaterRequired() {
        return ServerConfig.getOr(ServerConfig.POWER_HEATER, false);
    }

    public static boolean filterRequired() {
        return ServerConfig.getOr(ServerConfig.POWER_FILTER, false);
    }

    public static boolean lightRequired() {
        return ServerConfig.getOr(ServerConfig.POWER_LIGHT, false);
    }

    public static boolean anyRequired() {
        return heaterRequired() || filterRequired() || lightRequired();
    }

    public static long capacity() {
        return ServerConfig.getOr(ServerConfig.POWER_BUFFER, 100000);
    }

    /** FE per tick the heaters or coolers need: only the type that works now, only for the water they handle. */
    public static int heaterDemand(ServerLevel level, TankBlockEntity c) {
        TankData t = c.getTank();
        Boolean heating = TankTemperature.heating(TankCare.ambient(level, t), t.targetTemp());
        if (heating == null) return 0;
        double cap = heating ? TankCare.rawHeaterBlocks(level, t) : TankCare.rawCoolerBlocks(level, t);
        if (cap <= 0) return 0;
        return (int) Math.ceil(Math.min(cap, Math.max(1.0, t.waterBlocks())) * ServerConfig.getOr(ServerConfig.POWER_HEATER_FE_PER_BLOCK, 8.0));
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
        if (!anyRequired()) {
            c.heatPowered = c.filterPowered = c.lightPowered = true;
            c.lastDemand = 0;
        } else {
            TankData t = c.getTank();
            int filterFe = filterRequired() ? TankWater.filterCount(level, c) * ServerConfig.getOr(ServerConfig.POWER_FILTER_FE, 10) : 0;
            int heatFe = heaterRequired() ? heaterDemand(level, c) : 0;
            int lightFe = lightRequired() ? lightsWanting(level, t) * ServerConfig.getOr(ServerConfig.POWER_LIGHT_FE, 4) : 0;
            long buf = t.powerBuffer();
            long n = Math.max(1, ticks);
            c.filterPowered = filterFe <= 0 || buf >= filterFe * n;
            if (filterFe > 0 && c.filterPowered) buf -= filterFe * n;
            c.heatPowered = heatFe <= 0 || buf >= heatFe * n;
            if (heatFe > 0 && c.heatPowered) buf -= heatFe * n;
            c.lightPowered = lightFe <= 0 || buf >= lightFe * n;
            if (lightFe > 0 && c.lightPowered) buf -= lightFe * n;
            c.lastDemand = filterFe + heatFe + lightFe;
            if (buf != t.powerBuffer()) {
                t.setPowerBuffer(buf);
                c.setChanged();
            }
        }
        if (wasLight != c.lightPowered) updateLights(level, c.getTank());
    }

    /** Lets the lights on the tank recompute whether they shine. */
    public static void updateLights(ServerLevel level, TankData tank) {
        for (BlockPos p : TankCare.aboveTop(tank)) {
            if (level.isLoaded(p) && level.getBlockState(p).is(ModBlocks.TANK_LIGHT.get())) level.scheduleTick(p, ModBlocks.TANK_LIGHT.get(), 1);
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
