package org.overengineer.bettacraft.fabric;

import net.minecraft.nbt.CompoundTag;

/** M52: NeoForge's persistent entity data on Fabric (added to Entity by EntityPersistentDataMixin, saved as "NeoForgeData"). */
public interface PersistentDataHolder {
    CompoundTag bettacraft$persistentData();
}
