package org.overengineer.bettacraft.client.map;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * M53: one map mod that shows the alarm waypoints (JourneyMap, Xaero's Minimap, FTB Chunks), always by reflection (no
 * compile dependency). Every adapter keeps its own waypoint objects by tank; a missing mod or an API that does not fit
 * makes {@link #available} false (one log line).
 */
interface MapWaypoints {
    /** The id of the client config switch ({@code ClientConfig.mapMod}). */
    String id();

    /** The mod is loaded and its API fits. */
    boolean available();

    /** Puts the waypoint of {@code tank} (again); false when it could not. */
    boolean add(UUID tank, ResourceLocation dimension, BlockPos pos, String label);

    void remove(UUID tank);
}
