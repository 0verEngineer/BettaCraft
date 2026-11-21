package org.overengineer.bettacraft.client.map;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.BettaCraft;

/**
 * M32 part d (client): waypoints in JourneyMap (6.x, API v2) by reflection only, no compile dependency. Its client API
 * is the singleton {@code journeymap.api.client.impl.ClientAPI.INSTANCE} (an {@code IClientAPI}); it takes waypoints of
 * any mod id, also without a registered plugin. A waypoint is made with {@code WaypointFactory.createClientWaypoint}
 * (not persistent: JourneyMap does not save it), coloured red, and removed with the same object. Without JourneyMap,
 * or when its API does not fit, nothing happens (one warning in the log).
 */
public final class JourneyMapWaypoints implements MapWaypoints {
    static final JourneyMapWaypoints INSTANCE = new JourneyMapWaypoints();

    private static final String MOD = "journeymap";
    private static final int RED = 0xE03030;

    private boolean tried;
    @Nullable
    private Object api;
    private Method create, add, remove, setColor;
    private final Map<UUID, Object> waypoints = new HashMap<>();

    private JourneyMapWaypoints() {}

    @Override
    public String id() {
        return "journeymap";
    }

    @Override
    public boolean available() {
        return init();
    }

    private boolean init() {
        if (tried) return api != null;
        tried = true;
        if (!org.overengineer.bettacraft.platform.Platform.get().isModLoaded(MOD)) return false;
        try {
            ClassLoader cl = JourneyMapWaypoints.class.getClassLoader();
            Class<?> apiIf = Class.forName("journeymap.api.v2.client.IClientAPI", true, cl);
            Class<?> wp = Class.forName("journeymap.api.v2.common.waypoint.Waypoint", true, cl);
            Class<?> factory = Class.forName("journeymap.api.v2.common.waypoint.WaypointFactory", true, cl);
            Object instance = Class.forName("journeymap.api.client.impl.ClientAPI", true, cl).getField("INSTANCE").get(null);
            create = factory.getMethod("createClientWaypoint", String.class, BlockPos.class, String.class, ResourceKey.class, boolean.class);
            add = apiIf.getMethod("addWaypoint", String.class, wp);
            remove = apiIf.getMethod("removeWaypoint", String.class, wp);
            setColor = wp.getMethod("setColor", int.class);
            if (!apiIf.isInstance(instance)) throw new IllegalStateException("ClientAPI.INSTANCE is no IClientAPI");
            api = instance;
            org.slf4j.LoggerFactory.getLogger(JourneyMapWaypoints.class).info("JourneyMap found: alarm waypoints on");
        } catch (Throwable t) {
            api = null;
            org.slf4j.LoggerFactory.getLogger(JourneyMapWaypoints.class).warn("JourneyMap is loaded, but its waypoint API does not fit; no alarm waypoints ({})", t.toString());
        }
        return api != null;
    }

    /** Puts the waypoint of {@code tank} (again). Returns false without JourneyMap. */
    @Override
    public boolean add(UUID tank, Identifier dimension, BlockPos pos, String label) {
        if (!init()) return false;
        remove(tank);
        try {
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, dimension);
            Object w = create.invoke(null, BettaCraft.MOD_ID, pos.above(), label, dim, false);
            setColor.invoke(w, RED);
            add.invoke(api, BettaCraft.MOD_ID, w);
            waypoints.put(tank, w);
            return true;
        } catch (Throwable t) {
            org.slf4j.LoggerFactory.getLogger(JourneyMapWaypoints.class).warn("JourneyMap: could not add the alarm waypoint ({})", t.toString());
            return false;
        }
    }

    @Override
    public void remove(UUID tank) {
        Object w = waypoints.remove(tank);
        if (w == null || api == null) return;
        try {
            remove.invoke(api, BettaCraft.MOD_ID, w);
        } catch (Throwable t) {
            org.slf4j.LoggerFactory.getLogger(JourneyMapWaypoints.class).warn("JourneyMap: could not remove the alarm waypoint ({})", t.toString());
        }
    }
}
