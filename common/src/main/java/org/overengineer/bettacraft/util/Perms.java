package org.overengineer.bettacraft.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionSetSupplier;
import net.minecraft.server.permissions.Permissions;

/**
 * M52b: the operator levels of 1.21.1 ({@code hasPermission(2)}) on the permission sets of 1.21.11: level 1 = moderators,
 * 2 = game masters (cheats, our admin commands), 3 = admins, 4 = owners, 0 = everybody.
 */
public final class Perms {
    private Perms() {}

    public static Permission level(int level) {
        return switch (level) {
            case 1 -> Permissions.COMMANDS_MODERATOR;
            case 2 -> Permissions.COMMANDS_GAMEMASTER;
            case 3 -> Permissions.COMMANDS_ADMIN;
            default -> Permissions.COMMANDS_OWNER;
        };
    }

    public static boolean has(PermissionSetSupplier who, int level) {
        return level <= 0 || who.permissions().hasPermission(level(level));
    }

    public static boolean has(net.minecraft.world.entity.player.Player player, int level) {
        return level <= 0 || player.permissions().hasPermission(level(level));
    }

    public static boolean has(CommandSourceStack source, int level) {
        return level <= 0 || source.permissions().hasPermission(level(level));
    }
}
