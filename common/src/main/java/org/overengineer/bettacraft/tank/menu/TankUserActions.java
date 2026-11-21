package org.overengineer.bettacraft.tank.menu;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.monitor.MonitorAlarms;
import org.overengineer.bettacraft.network.TankActionPayload;
import org.overengineer.bettacraft.tank.TankAccess;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankUsers;
import org.overengineer.bettacraft.tank.TankUsers.Role;

/** M53: the actions of the users tab (add, change, remove users; the alarm waypoint switches). Returns an error or null. */
final class TankUserActions {
    private TankUserActions() {}

    @Nullable
    static Component handle(ServerPlayer player, TankBlockEntity controller, TankActionPayload p, boolean manage) {
        TankData tank = controller.getTank();
        MinecraftServer server = player.server;
        switch (p.action()) {
            case TankActions.WAYPOINT_OWN -> {
                if (!Features.enabled(Feature.ALARM_WAYPOINTS)) return Component.translatable("message.bettacraft.users.waypoints_off");
                boolean on = p.value() != 0;
                UUID me = player.getUUID();
                if (!tank.users().setWaypoint(me, tank.owner(), on)) {
                    // a party member of the owner is a caretaker without an entry: switching his waypoint lists him
                    if (TankAccess.role(player, tank) != Role.CARETAKER || !tank.users().put(me, player.getGameProfile().getName(), Role.CARETAKER)) {
                        return Component.translatable("message.bettacraft.users.not_user");
                    }
                    tank.users().setWaypoint(me, tank.owner(), on);
                }
                MonitorAlarms.refreshWaypoints(controller);
                return null;
            }
            case TankActions.WAYPOINT_ALL -> {
                if (!manage) return Component.translatable("message.bettacraft.tank.only_owner");
                if (!Features.enabled(Feature.ALARM_WAYPOINTS)) return Component.translatable("message.bettacraft.users.waypoints_off");
                tank.users().setWaypointsAll(p.value() != 0);
                MonitorAlarms.refreshWaypoints(controller);
                return null;
            }
            default -> {
            }
        }
        if (!TankAccess.usersEnabled()) return Component.translatable("message.bettacraft.users.feature_off");
        switch (p.action()) {
            case TankActions.USERS_ADD -> {
                if (!manage) return Component.translatable("message.bettacraft.tank.only_owner");
                Role role = role(p.value());
                if (role == null) return null;
                String name = p.text().strip();
                Optional<com.mojang.authlib.GameProfile> profile = profile(server, name);
                if (profile.isEmpty()) return Component.translatable("message.bettacraft.users.unknown", name);
                UUID id = profile.get().getId();
                if (id.equals(tank.owner())) return Component.translatable("message.bettacraft.users.is_owner", profile.get().getName());
                if (tank.users().get(id) == null && tank.users().size() >= TankUsers.MAX) {
                    return Component.translatable("message.bettacraft.users.full", TankUsers.MAX);
                }
                if (tank.owner() == null) tank.setOwner(player.getUUID()); // an ownerless tank: whoever lists the first user owns it
                tank.users().put(id, profile.get().getName(), role);
                org.overengineer.bettacraft.progression.Progress.award(player, org.overengineer.bettacraft.progression.Progress.SHARED_TANK);
                TankCare.log(player.serverLevel(), controller, "log.bettacraft.users.added", TankCare.nameArg(player), profile.get().getName(),
                        "@" + role.translationKey());
                player.displayClientMessage(Component.translatable("message.bettacraft.users.added", profile.get().getName(),
                        Component.translatable(role.translationKey())).withStyle(ChatFormatting.GREEN), true);
                MonitorAlarms.refreshWaypoints(controller);
                return null;
            }
            case TankActions.USERS_ROLE -> {
                if (!manage) return Component.translatable("message.bettacraft.tank.only_owner");
                Role role = role(p.value());
                TankUsers.Entry e = p.target().map(tank.users()::get).orElse(null);
                if (role == null || e == null) return Component.translatable("message.bettacraft.users.not_listed");
                tank.users().put(e.id, TankAccess.name(server, e.id, e.name), role);
                TankCare.log(player.serverLevel(), controller, "log.bettacraft.users.role", TankCare.nameArg(player), e.name, "@" + role.translationKey());
                return null;
            }
            case TankActions.USERS_REMOVE -> {
                TankUsers.Entry e = p.target().map(tank.users()::get).orElse(null);
                if (e == null) return Component.translatable("message.bettacraft.users.not_listed");
                // a user may take himself off the list
                if (!manage && !e.id.equals(player.getUUID())) return Component.translatable("message.bettacraft.tank.only_owner");
                tank.users().remove(e.id);
                TankCare.log(player.serverLevel(), controller, "log.bettacraft.users.removed", TankCare.nameArg(player), e.name);
                MonitorAlarms.refreshWaypoints(controller);
                return null;
            }
            default -> {
                return null;
            }
        }
    }

    @Nullable
    private static Role role(int ordinal) {
        if (ordinal < 0 || ordinal >= Role.values().length) return null;
        Role r = Role.values()[ordinal];
        return r.listable() ? r : null;
    }

    /** An online player by name (the profile cache is not asked: an unknown name would mean a blocking web lookup). */
    private static Optional<com.mojang.authlib.GameProfile> profile(MinecraftServer server, String name) {
        if (name.isEmpty() || name.length() > 16) return Optional.empty();
        ServerPlayer sp = server.getPlayerList().getPlayerByName(name);
        return sp == null ? Optional.empty() : Optional.of(sp.getGameProfile());
    }
}
