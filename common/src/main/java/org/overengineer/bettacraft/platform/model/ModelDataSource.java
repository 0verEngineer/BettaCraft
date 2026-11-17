package org.overengineer.bettacraft.platform.model;

/**
 * M52: a block entity whose block model draws from its data (connected glass, cabinet wood, pipe covers, substrate). The
 * loader asks it on the client (NeoForge: as the block entity's model data; Fabric: as its render data).
 */
public interface ModelDataSource {
    ModelValues modelValues();
}
