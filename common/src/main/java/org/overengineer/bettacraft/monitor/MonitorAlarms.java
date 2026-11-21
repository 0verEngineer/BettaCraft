package org.overengineer.bettacraft.monitor;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.overengineer.bettacraft.platform.network.PacketDistributor;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.api.ApiEvents;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.network.AlarmWaypointPayload;
import org.overengineer.bettacraft.tank.TankAccess;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;

/**
 * M32 part d: the alarm tiles of a monitor as events. Every cycle of a working screen ({@link MonitorSync#cycle}) the
 * triggered state of each alarm tile of a tank is compared with the last one: going off posts a {@code TankAlarmEvent}
 * (active) and opens a waypoint at the tank for its owner and its listed users whose waypoint switches are on (M53,
 * {@link TankAccess#waypointMembers}; {@code features.compat.journeymap}, client: JourneyMap, Xaero's Minimap, FTB Chunks);
 * clearing posts the event again (not active) and closes the
 * waypoint once no other alarm of the tank is on. A tank that is not loaded or out of range keeps its state. A screen
 * that stops watching (off, no power, broken, unloaded) clears all of its alarms; a changed tile counts as a new one.
 */
public final class MonitorAlarms {
    /** Every open waypoint of the server (static: alarms of all levels; cleared when the server stops). */
    static final AlarmWaypointBook BOOK = new AlarmWaypointBook();
    /** Waypoint payloads sent (GameTests). */
    public static int sentWaypoints;

    /** What an active alarm tile watched (to clear it with the same values, and to see a changed tile). */
    record Watch(UUID tank, String value, float limit, boolean below, BlockPos tankPos, String tankName) {
        static Watch of(DashboardTile t, MonitorTank k) {
            return new Watch(t.tank, t.value().id, t.limit(), t.below(), k.pos, k.name);
        }

        boolean sameTile(DashboardTile t) {
            return t.tank != null && t.tank.equals(tank) && t.value().id.equals(value) && t.limit() == limit && t.below() == below;
        }
    }

    private MonitorAlarms() {}

    private static String key(ServerLevel level, MonitorBlockEntity c, int tile) {
        return level.dimension().identifier() + "@" + c.getBlockPos().asLong() + "#" + tile;
    }

    /** One cycle: {@code watching} false = the screen does not watch (all of its alarms clear). */
    public static void update(ServerLevel level, MonitorBlockEntity c, boolean watching) {
        Map<Integer, Watch> active = c.activeAlarms;
        if (!watching) {
            clearAll(level, c);
            return;
        }
        Map<Integer, DashboardTile> tiles = new HashMap<>();
        for (DashboardTile t : c.layout().tiles) if (DashboardTile.ALARM.equals(t.type) && t.tank != null) tiles.put(t.id, t);
        // tiles that are gone or changed clear first
        for (Iterator<Map.Entry<Integer, Watch>> it = active.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Watch> e = it.next();
            DashboardTile t = tiles.get(e.getKey());
            if (t == null || !e.getValue().sameTile(t)) {
                fire(level, c, e.getKey(), e.getValue(), Float.NaN, false);
                it.remove();
            }
        }
        for (DashboardTile t : tiles.values()) {
            MonitorTank k = c.tank(t.tank);
            if (k == null || !k.live) continue; // unknown now: keeps its state
            boolean on = t.triggered(k);
            Watch w = active.get(t.id);
            if (on && w == null) {
                w = Watch.of(t, k);
                active.put(t.id, w);
                fire(level, c, t.id, w, k.value(t.value()), true);
            } else if (!on && w != null) {
                active.remove(t.id);
                fire(level, c, t.id, w, k.value(t.value()), false);
            }
        }
    }

    /** The screen stops watching (also when its block is removed or unloaded). */
    public static void clearAll(ServerLevel level, MonitorBlockEntity c) {
        if (c.activeAlarms.isEmpty()) return;
        Map<Integer, Watch> copy = new HashMap<>(c.activeAlarms);
        c.activeAlarms.clear();
        for (Map.Entry<Integer, Watch> e : copy.entrySet()) fire(level, c, e.getKey(), e.getValue(), Float.NaN, false);
    }

    private static void fire(ServerLevel level, MonitorBlockEntity c, int tile, Watch w, float current, boolean on) {
        ApiEvents.alarm(level, w.tankPos(), w.tank(), w.tankName(), c.getBlockPos(), w.value(), current, w.limit(), w.below(), on);
        String key = key(level, c, tile);
        if (on) {
            if (!Features.enabled(Feature.ALARM_WAYPOINTS)) return;
            TankData tank = tank(level, w.tankPos());
            Set<UUID> members = tank == null ? Set.of() : TankAccess.waypointMembers(tank);
            AlarmWaypointPayload add = new AlarmWaypointPayload(w.tank(), true, level.dimension().identifier(), w.tankPos(), w.tankName(), w.value());
            Set<UUID> to = BOOK.open(w.tank(), key, members, add, () -> online(level, members));
            for (UUID p : to) send(level, p, add);
        } else {
            Set<UUID> to = BOOK.close(w.tank(), key);
            for (UUID p : to) send(level, p, new AlarmWaypointPayload(w.tank(), false, level.dimension().identifier(), w.tankPos(), w.tankName(), w.value()));
        }
    }

    @javax.annotation.Nullable
    private static TankData tank(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return null;
        TankBlockEntity t = org.overengineer.bettacraft.tank.TankMultiblock.getBE(level, pos);
        return t != null && t.isController() ? t.getTank() : null;
    }

    private static Set<UUID> online(ServerLevel level, Set<UUID> members) {
        Set<UUID> out = new LinkedHashSet<>();
        for (UUID p : members) if (level.getServer().getPlayerList().getPlayer(p) != null) out.add(p);
        return out;
    }

    /**
     * M53: the waypoint switches or the users of a tank changed: the players who should have its open waypoint now get it,
     * the others lose it.
     */
    public static void refreshWaypoints(TankBlockEntity controller) {
        if (!(controller.getLevel() instanceof ServerLevel level)) return;
        TankData t = controller.getTank();
        AlarmWaypointBook.Entry e = BOOK.entry(t.id());
        if (e == null) return;
        Set<UUID> members = Features.enabled(Feature.ALARM_WAYPOINTS) ? TankAccess.waypointMembers(t) : Set.of();
        AlarmWaypointBook.Change c = BOOK.update(t.id(), members, online(level, members));
        if (e.data instanceof AlarmWaypointPayload add) {
            for (UUID p : c.added()) send(level, p, add);
            for (UUID p : c.removed()) send(level, p, new AlarmWaypointPayload(add.tank(), false, add.dimension(), add.pos(), add.name(), add.value()));
        }
    }

    /**
     * M51: an alarm of something else than a monitor (the hired aquarist's warnings): the waypoint at the tank opens for
     * {@code player} only (and again when he comes online) and closes when the last alarm of the tank clears. Off without
     * {@code features.compat.journeymap}. M53: only when {@code player} is a user of the tank with his waypoint switch on.
     */
    public static void external(ServerLevel level, UUID tank, String key, BlockPos pos, String tankName, String value, boolean on, @javax.annotation.Nullable UUID player) {
        if (on) {
            if (!Features.enabled(Feature.ALARM_WAYPOINTS) || player == null) return;
            TankData data = tank(level, pos);
            if (data == null || !TankAccess.waypointMembers(data).contains(player)) return;
            AlarmWaypointPayload add = new AlarmWaypointPayload(tank, true, level.dimension().identifier(), pos, tankName, value);
            Set<UUID> to = BOOK.open(tank, key, Set.of(player), add, () -> {
                Set<UUID> s = new LinkedHashSet<>();
                if (level.getServer().getPlayerList().getPlayer(player) != null) s.add(player);
                return s;
            });
            for (UUID p : to) send(level, p, add);
        } else {
            AlarmWaypointBook.Entry e = BOOK.entry(tank);
            Set<UUID> to = BOOK.close(tank, key);
            if (to.isEmpty()) return;
            BlockPos at = e != null && e.data instanceof AlarmWaypointPayload a ? a.pos() : pos;
            for (UUID p : to) send(level, p, new AlarmWaypointPayload(tank, false, level.dimension().identifier(), at, tankName, value));
        }
    }

    private static void send(ServerLevel level, UUID player, AlarmWaypointPayload p) {
        ServerPlayer sp = level.getServer().getPlayerList().getPlayer(player);
        if (sp == null) return;
        sentWaypoints++;
        PacketDistributor.sendToPlayer(sp, p);
    }



    /** M53: the players who have the open waypoint of {@code tank} (GameTests). */
    public static Set<UUID> waypointHolders(UUID tank) {
        AlarmWaypointBook.Entry e = BOOK.entry(tank);
        return e == null ? Set.of() : Set.copyOf(e.players());
    }

    /** The tanks with an open waypoint (GameTests). */
    public static boolean waypointOpen(UUID tank) {
        return BOOK.isOpen(tank);
    }
}
