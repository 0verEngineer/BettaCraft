package org.overengineer.bettacraft.platform;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * M52: a block entity that reads its client update tag itself (NeoForge: handleUpdateTag / onDataPacket by these names;
 * Fabric's glue sends every client-side load of the block entity here).
 */
public interface ClientSyncedBlockEntity {
    void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries);
}
