package org.overengineer.bettacraft.platform;

/**
 * M52: a block entity that wants to know when it joins and leaves its level (NeoForge calls these two by their names;
 * Fabric's glue calls them from its block entity load and unload events).
 */
public interface BlockEntityLifecycle {
    void onLoad();

    void onChunkUnloaded();
}
