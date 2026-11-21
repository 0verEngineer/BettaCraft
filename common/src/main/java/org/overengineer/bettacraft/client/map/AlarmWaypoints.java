package org.overengineer.bettacraft.client.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.monitor.MonitorValue;
import org.overengineer.bettacraft.network.AlarmWaypointPayload;

/**
 * M32 part d (client): the waypoints of the monitor alarms the server sends ({@link AlarmWaypointPayload}), one per tank,
 * handed to the map mods. M53: JourneyMap, Xaero's Minimap (with its World Map) and FTB Chunks, each with its own switch in
 * the client config ({@code map.journeyMap}, {@code map.xaerosMinimap}, {@code map.ftbChunks}); {@code map.showAlarmWaypoints}
 * ("alarm waypoints of all tanks") off: none at all. The server already sends only the waypoints of tanks this player is a
 * user of with his switches on. A switch changed while waypoints are shown takes effect within a second (they are kept
 * here and put into / taken out of the map mods again). They are not saved by the map mods and go when the player leaves.
 */
public final class AlarmWaypoints {
    /** The adapters in the order of the client config. */
    private static final List<MapWaypoints> MAPS = List.of(JourneyMapWaypoints.INSTANCE, XaeroWaypoints.INSTANCE, FtbChunksWaypoints.INSTANCE);
    /** The waypoints the server gave this client, by tank (also those not shown now because a switch is off). */
    private static final Map<UUID, AlarmWaypointPayload> RECEIVED = new LinkedHashMap<>();
    /** Which map mods show the waypoint of each tank now. */
    private static final Map<UUID, Set<String>> SHOWN = new HashMap<>();
    private static String lastSwitches = "";
    private static int ticks;

    private AlarmWaypoints() {}

    public static void accept(AlarmWaypointPayload p) {
        if (p.add()) {
            RECEIVED.put(p.tank(), p);
            show(p);
        } else {
            RECEIVED.remove(p.tank());
            hide(p.tank());
        }
    }

    private static void show(AlarmWaypointPayload p) {
        if (!ClientConfig.alarmWaypoints()) return;
        String label = label(p);
        Set<String> on = SHOWN.computeIfAbsent(p.tank(), k -> new LinkedHashSet<>());
        for (MapWaypoints m : MAPS) {
            if (!ClientConfig.mapMod(m.id()) || on.contains(m.id()) || !m.available()) continue;
            if (m.add(p.tank(), p.dimension(), p.pos(), label)) on.add(m.id());
        }
        if (on.isEmpty()) SHOWN.remove(p.tank());
    }

    private static void hide(UUID tank) {
        Set<String> on = SHOWN.remove(tank);
        if (on == null) return;
        for (MapWaypoints m : MAPS) if (on.contains(m.id())) m.remove(tank);
    }

    /** Once a second: a changed switch of the client config shows or hides the waypoints the server gave. */
    public static void tick() {
        if (++ticks < 20) return;
        ticks = 0;
        StringBuilder b = new StringBuilder().append(ClientConfig.alarmWaypoints());
        for (MapWaypoints m : MAPS) b.append(',').append(ClientConfig.mapMod(m.id()));
        String now = b.toString();
        if (now.equals(lastSwitches)) return;
        lastSwitches = now;
        for (UUID tank : new ArrayList<>(SHOWN.keySet())) {
            Set<String> on = SHOWN.get(tank);
            for (MapWaypoints m : MAPS) {
                if (on.contains(m.id()) && (!ClientConfig.alarmWaypoints() || !ClientConfig.mapMod(m.id()))) {
                    m.remove(tank);
                    on.remove(m.id());
                }
            }
            if (on.isEmpty()) SHOWN.remove(tank);
        }
        for (AlarmWaypointPayload p : RECEIVED.values()) show(p);
    }

    static String label(AlarmWaypointPayload p) {
        MonitorValue v = MonitorValue.byId(p.value());
        Component name = p.name().isEmpty() ? Component.translatable("waypoint.bettacraft.tank") : Component.literal(p.name());
        // M51: a warning of the hired aquarist ("aquarist:warning.<key>")
        Component value = v != null ? Component.translatable(v.translationKey()) : p.value().startsWith("aquarist:")
                ? Component.translatable("aquarist.bettacraft." + p.value().substring("aquarist:".length())) : Component.literal(p.value());
        return Component.translatable("waypoint.bettacraft.alarm", name, value).getString();
    }

    /** The labels of the waypoints shown now in at least one map mod (client smoke). */
    public static Map<UUID, String> shown() {
        Map<UUID, String> out = new LinkedHashMap<>();
        for (UUID t : SHOWN.keySet()) {
            AlarmWaypointPayload p = RECEIVED.get(t);
            if (p != null) out.put(t, label(p));
        }
        return out;
    }

    /** The map mods that show the waypoint of {@code tank} now (client smoke). */
    public static Set<String> shownIn(UUID tank) {
        Set<String> s = SHOWN.get(tank);
        return s == null ? Set.of() : Set.copyOf(s);
    }

    /** Whether the map mod with this config id is installed and its API fits (client smoke, log). */
    public static boolean available(String id) {
        for (MapWaypoints m : MAPS) if (m.id().equals(id)) return m.available();
        return false;
    }

    public static void clear() {
        for (UUID t : new ArrayList<>(SHOWN.keySet())) hide(t);
        SHOWN.clear();
        RECEIVED.clear();
    }
}
