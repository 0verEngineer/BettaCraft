package org.overengineer.bettacraft.fabric.compat;

import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.TheOneProbePlugin;
import org.overengineer.bettacraft.compat.top.TheOneProbeCompat;

/** M52: The One Probe on Fabric finds its plugins as the entry point "theoneprobe" (NeoForge: IMC); the same providers. */
public final class TopFabricPlugin implements TheOneProbePlugin {
    @Override
    public void register(ITheOneProbe probe) {
        new TheOneProbeCompat().apply(probe);
    }
}
