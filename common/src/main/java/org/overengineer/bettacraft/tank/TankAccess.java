package org.overengineer.bettacraft.tank;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.overengineer.bettacraft.compat.FtbTeamsCompat;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.tank.TankUsers.Role;

/**
 * M53: who may do what at a tank ({@link TankUsers}). Operators (permission level 2) may always do everything.
 * <ul>
 *     <li>owner and caretakers: change everything, go in, manage the users, the lock and the waypoints of all;</li>
 *     <li>viewers: open the menu and look, change nothing, do not go in; their own alarm waypoint only;</li>
 *     <li>everyone else: as before M53, the lock decides (open: change and go in, locked: look only).</li>
 * </ul>
 * With {@code features.equipment.tankUsers} off the list is ignored (only the owner and the lock count, as before).
 * A tank without an owner (made by a command or a machine) can be managed by anyone.
 */
public final class TankAccess {
    private TankAccess() {}

    public static boolean usersEnabled() {
        return Features.enabled(Feature.TANK_USERS);
    }

    /** The role of {@code player} at the tank (operators get their real role; see {@link #canModify}). */
    public static Role role(Player player, TankData tank) {
        return role(player.getUUID(), tank);
    }

    public static Role role(UUID id, TankData tank) {
        if (id.equals(tank.owner())) return Role.OWNER;
        if (!usersEnabled()) return Role.OTHER;
        TankUsers.Entry e = tank.users().get(id);
        if (e != null) return e.role;
        if (tank.owner() != null && FtbTeamsCompat.sameTeam(tank.owner(), id)) return Role.CARETAKER;
        return Role.OTHER;
    }

    private static boolean op(Player player) {
        return player.hasPermissions(2);
    }

    /** May change the tank (settings, animals, plants, equipment, items into the menu) and go into its interior. */
    public static boolean canModify(Player player, TankData tank) {
        if (op(player)) return true;
        return switch (role(player, tank)) {
            case OWNER, CARETAKER -> true;
            case VIEWER -> false;
            case OTHER -> !tank.locked();
        };
    }

    /** May go into the interior (the same as {@link #canModify}: a viewer only looks from outside). */
    public static boolean canEnter(Player player, TankData tank) {
        return canModify(player, tank);
    }

    /** May manage the tank: the lock, break protection, the aquarist, the users and the waypoints of all of them. */
    public static boolean canManage(Player player, TankData tank) {
        if (op(player) || tank.owner() == null) return true;
        Role r = role(player, tank);
        return r == Role.OWNER || r == Role.CARETAKER;
    }

    /** Owner or listed user (or party member): has an own waypoint switch. */
    public static boolean isUser(UUID id, TankData tank) {
        return role(id, tank) != Role.OTHER;
    }

    /** The own waypoint switch of {@code id} (owner default on, others default off; party members follow the owner's default off). */
    public static boolean ownWaypoint(UUID id, TankData tank) {
        return tank.users().waypoint(id, tank.owner());
    }

    /**
     * M53: who should have the alarm waypoints of the tank (online or not): the owner and the listed users with their own
     * switch on, while the tank's switch for all is on. Players near the tank who are no users get none.
     */
    public static Set<UUID> waypointMembers(TankData tank) {
        Set<UUID> out = new LinkedHashSet<>();
        TankUsers u = tank.users();
        if (!u.waypointsAll()) return out;
        if (tank.owner() != null && u.ownerWaypoint()) out.add(tank.owner());
        if (usersEnabled()) for (TankUsers.Entry e : u.entries()) if (e.waypoint) out.add(e.id);
        return out;
    }

    /** The {@link #waypointMembers} that are online now. */
    public static Set<UUID> waypointRecipients(MinecraftServer server, TankData tank) {
        Set<UUID> out = new LinkedHashSet<>();
        for (UUID id : waypointMembers(tank)) if (server.getPlayerList().getPlayer(id) != null) out.add(id);
        return out;
    }

    /** The name of a player: online, else the server's profile cache, else {@code fallback}. */
    public static String name(MinecraftServer server, UUID id, @Nullable String fallback) {
        ServerPlayer sp = server.getPlayerList().getPlayer(id);
        if (sp != null) return sp.getGameProfile().getName();
        if (server.getProfileCache() != null) {
            var p = server.getProfileCache().get(id);
            if (p.isPresent()) return p.get().getName();
        }
        return fallback == null ? "" : fallback;
    }
}
