package org.overengineer.bettacraft.client.map;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.monitor.MonitorValue;
import org.overengineer.bettacraft.network.AlarmWaypointPayload;

/**
 * M32 part d (client): the waypoints of the monitor alarms the server sends ({@link AlarmWaypointPayload}), one per tank,
 * handed to the map mod ({@link JourneyMapWaypoints}; Xaero's Minimap is not supported). {@code map.showAlarmWaypoints}
 * off: new ones are ignored, removals still go through. They are not saved by the map mod and go when the player leaves.
 */
public final class AlarmWaypoints {
    /** The waypoints this client shows, by tank (the label for the log and the GameTests of the client smoke). */
    private static final Map<UUID, String> SHOWN = new HashMap<>();

    private AlarmWaypoints() {}

    public static void accept(AlarmWaypointPayload p) {
        if (p.add()) {
            if (!ClientConfig.alarmWaypoints()) return;
            String label = label(p);
            if (JourneyMapWaypoints.add(p.tank(), p.dimension(), p.pos(), label)) SHOWN.put(p.tank(), label);
        } else if (SHOWN.remove(p.tank()) != null) {
            JourneyMapWaypoints.remove(p.tank());
        }
    }

    static String label(AlarmWaypointPayload p) {
        MonitorValue v = MonitorValue.byId(p.value());
        Component name = p.name().isEmpty() ? Component.translatable("waypoint.bettacraft.tank") : Component.literal(p.name());
        Component value = v == null ? Component.literal(p.value()) : Component.translatable(v.translationKey());
        return Component.translatable("waypoint.bettacraft.alarm", name, value).getString();
    }

    /** The labels of the waypoints shown now (client smoke). */
    public static Map<UUID, String> shown() {
        return Map.copyOf(SHOWN);
    }

    public static void clear() {
        for (UUID t : SHOWN.keySet()) JourneyMapWaypoints.remove(t);
        SHOWN.clear();
    }
}
