package org.overengineer.bettacraft.monitor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.api.ApiEvents;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.network.AlarmWaypointPayload;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * M32 part d: the alarm tiles of a monitor as events. Every cycle of a working screen ({@link MonitorSync#cycle}) the
 * triggered state of each alarm tile of a tank is compared with the last one: going off posts a {@code TankAlarmEvent}
 * (active) and opens a waypoint at the tank for its owner and the players within {@link #WAYPOINT_RANGE} blocks
 * ({@code features.alarmWaypoints}, client: JourneyMap); clearing posts the event again (not active) and closes the
 * waypoint once no other alarm of the tank is on. A tank that is not loaded or out of range keeps its state. A screen
 * that stops watching (off, no power, broken, unloaded) clears all of its alarms; a changed tile counts as a new one.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class MonitorAlarms {
    public static final int WAYPOINT_RANGE = 128;
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
        return level.dimension().location() + "@" + c.getBlockPos().asLong() + "#" + tile;
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
            if (!CommonConfig.alarmWaypoints()) return;
            UUID owner = owner(level, w);
            AlarmWaypointPayload add = new AlarmWaypointPayload(w.tank(), true, level.dimension().location(), w.tankPos(), w.tankName(), w.value());
            Set<UUID> to = BOOK.open(w.tank(), key, owner, add, () -> recipients(level, w.tankPos(), owner));
            for (UUID p : to) send(level, p, add);
        } else {
            Set<UUID> to = BOOK.close(w.tank(), key);
            for (UUID p : to) send(level, p, new AlarmWaypointPayload(w.tank(), false, level.dimension().location(), w.tankPos(), w.tankName(), w.value()));
        }
    }

    private static UUID owner(ServerLevel level, Watch w) {
        if (!level.isLoaded(w.tankPos())) return null;
        TankBlockEntity t = org.overengineer.bettacraft.tank.TankMultiblock.getBE(level, w.tankPos());
        return t != null && t.isController() ? t.getTank().owner() : null;
    }

    /** The owner (online, any level) and the players of the tank's level within {@link #WAYPOINT_RANGE} blocks. */
    static Set<UUID> recipients(ServerLevel level, BlockPos pos, UUID owner) {
        Set<UUID> out = new LinkedHashSet<>();
        if (owner != null && level.getServer().getPlayerList().getPlayer(owner) != null) out.add(owner);
        double r2 = (double) WAYPOINT_RANGE * WAYPOINT_RANGE;
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(pos.getCenter()) <= r2) out.add(p.getUUID());
        return out;
    }

    private static void send(ServerLevel level, UUID player, AlarmWaypointPayload p) {
        ServerPlayer sp = level.getServer().getPlayerList().getPlayer(player);
        if (sp == null) return;
        sentWaypoints++;
        PacketDistributor.sendToPlayer(sp, p);
    }

    /** The owner of a tank with an open waypoint comes online: they get it too. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || !CommonConfig.alarmWaypoints()) return;
        for (UUID tank : BOOK.joined(sp.getUUID())) {
            AlarmWaypointBook.Entry e = BOOK.entry(tank);
            if (e == null || !(e.data instanceof AlarmWaypointPayload add)) continue;
            sentWaypoints++;
            PacketDistributor.sendToPlayer(sp, add);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BOOK.clear();
    }

    /** The tanks with an open waypoint (GameTests). */
    public static boolean waypointOpen(UUID tank) {
        return BOOK.isOpen(tank);
    }
}
