package org.overengineer.bettacraft.plumbing;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.energy.EnergyStorage;
import org.overengineer.bettacraft.config.ServerConfig;

/**
 * M24: the FE buffer of a water device (water pump, filter pump head, water change controller, level sensor, crucible).
 * Only offered as a capability while it is needed ({@code pipes.pumpsRequireEnergy}; the crucible whenever a power
 * option is on).
 */
public class DeviceEnergy extends EnergyStorage {
    private final Runnable changed;

    public DeviceEnergy(int capacity, Runnable changed) {
        super(capacity, capacity, capacity);
        this.changed = changed;
    }

    /** Whether the water devices need FE. */
    public static boolean required() {
        return org.overengineer.bettacraft.tank.TankPower.power() && ServerConfig.getOr(ServerConfig.POWER_PUMPS, false);
    }

    /** FE per tick of a pump at 100 %. */
    public static int pumpFe() {
        return ServerConfig.getOr(ServerConfig.POWER_PUMP_FE, 8);
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int n = super.receiveEnergy(toReceive, simulate);
        if (n > 0 && !simulate) changed.run();
        return n;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    /**
     * M32 part c: Create's rotation next to the device at {@code pos} gives it the FE of its work ({@code fe} at full
     * speed, proportionally less when slower; {@link org.overengineer.bettacraft.compat.create.CreateKinetics}). Returns
     * the FE it added.
     */
    public int drive(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, int fe) {
        if (fe <= 0) return 0;
        float w = org.overengineer.bettacraft.compat.create.CreateKinetics.drive(level, pos);
        if (w <= 0F) return 0;
        int add = Math.min(capacity - energy, (int) Math.ceil(fe * w));
        if (add <= 0) return 0;
        energy += add;
        changed.run();
        return add;
    }

    /** Takes {@code fe} if there is that much (true), else nothing. */
    public boolean use(int fe) {
        if (fe <= 0) return true;
        if (energy < fe) return false;
        energy -= fe;
        changed.run();
        return true;
    }

    public void set(int fe) {
        energy = Math.max(0, Math.min(capacity, fe));
    }

    public void save(CompoundTag t) {
        t.putInt("energy", energy);
    }

    public void load(CompoundTag t) {
        set(t.getInt("energy"));
    }
}
