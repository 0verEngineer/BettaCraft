package org.overengineer.bettacraft.tank.interior;

/** M31.1: implemented by {@code ChunkMap} (mixin): whether it is the chunk map of the interior dimension. */
public interface InteriorChunkMap {
    boolean bettacraft$isInterior();

    /** M53c: the chunks the map holds (loading, loaded or waiting to be saved and unloaded). */
    it.unimi.dsi.fastutil.longs.LongSet bettacraft$heldChunks();

    /** M53c: the chunk was deleted from the disk: the map's note of what is stored there goes. */
    void bettacraft$forgetStored(long chunk);
}
