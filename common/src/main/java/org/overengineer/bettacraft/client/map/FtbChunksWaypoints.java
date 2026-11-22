package org.overengineer.bettacraft.client.map;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import org.overengineer.bettacraft.platform.Platform;

/**
 * M53: alarm waypoints in FTB Chunks by reflection: {@code FTBChunksAPI.clientApi().getWaypointManager(dimension)}, then
 * {@code addTransientWaypointAt(pos, name)} (transient waypoints are never written to its waypoints file), coloured red,
 * removed again with {@code removeWaypoint}.
 */
final class FtbChunksWaypoints implements MapWaypoints {
    static final FtbChunksWaypoints INSTANCE = new FtbChunksWaypoints();
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(FtbChunksWaypoints.class);
    private static final int RED = 0xE03030;

    private boolean tried, ok;
    private java.lang.reflect.Method clientApi;
    /** Waypoint manager and waypoint of each tank. */
    private final Map<UUID, Object[]> waypoints = new HashMap<>();

    private FtbChunksWaypoints() {}

    @Override
    public String id() {
        return "ftbchunks";
    }

    @Override
    public boolean available() {
        if (tried) return ok;
        tried = true;
        if (!Platform.get().isModLoaded("ftbchunks")) return false;
        try {
            clientApi = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI", true, FtbChunksWaypoints.class.getClassLoader()).getMethod("clientApi");
            ok = true;
            LOGGER.info("FTB Chunks found: alarm waypoints on");
        } catch (Throwable t) {
            LOGGER.warn("FTB Chunks is loaded, but its waypoint API does not fit; no alarm waypoints there ({})", t.toString());
        }
        return ok;
    }

    @Override
    public boolean add(UUID tank, Identifier dimension, BlockPos pos, String label) {
        if (!available()) return false;
        remove(tank);
        try {
            Object api = clientApi.invoke(null);
            Object m = Reflect.call(api, "getWaypointManager", ResourceKey.create(Registries.DIMENSION, dimension));
            if (!(m instanceof Optional<?> o) || o.isEmpty()) return false;
            Object manager = o.get();
            Object w = Reflect.call(manager, "addTransientWaypointAt", pos.above(), label);
            Reflect.call(w, "setColor", RED);
            waypoints.put(tank, new Object[]{manager, w});
            return true;
        } catch (Throwable t) {
            org.overengineer.bettacraft.debug.LogArea.COMPAT.warnThrottled("ftbchunks_add", "FTB Chunks: could not add the alarm waypoint ({})", t.toString());
            return false;
        }
    }

    @Override
    public void remove(UUID tank) {
        Object[] e = waypoints.remove(tank);
        if (e == null) return;
        try {
            Reflect.call(e[0], "removeWaypoint", e[1]);
        } catch (Throwable t) {
            org.overengineer.bettacraft.debug.LogArea.COMPAT.warnThrottled("ftbchunks_remove", "FTB Chunks: could not remove the alarm waypoint ({})", t.toString());
        }
    }
}
