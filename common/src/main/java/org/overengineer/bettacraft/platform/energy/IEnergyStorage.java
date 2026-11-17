package org.overengineer.bettacraft.platform.energy;

/**
 * M52: an energy buffer (FE on NeoForge; on Fabric offered as Team Reborn Energy, 1 E = 1 FE). The loader-independent
 * counterpart of NeoForge's interface of the same name.
 */
public interface IEnergyStorage {
    int receiveEnergy(int toReceive, boolean simulate);

    int extractEnergy(int toExtract, boolean simulate);

    int getEnergyStored();

    int getMaxEnergyStored();

    boolean canExtract();

    boolean canReceive();
}
