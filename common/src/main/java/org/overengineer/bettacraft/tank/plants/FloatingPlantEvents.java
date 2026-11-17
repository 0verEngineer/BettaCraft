package org.overengineer.bettacraft.tank.plants;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * Right-clicks on a tank that concern its floating plants (M12). Runs before the block's own interactions and only
 * takes over in these cases; everything else (buckets, food, bags, menu, entering the interior) stays as it was:
 * <ul>
 *     <li>a floating plant item or a vanilla lily pad (also sneaking): the plant floats where the player looks at the
 *     surface;</li>
 *     <li>empty main hand (not sneaking) while looking at plants on the surface: takes them out (otherwise the menu opens
 *     as before);</li>
 *     <li>aquarium net: takes out the plants looked at from any angle (also from the side through the glass); sneaking:
 *     thins the whole tank out by half (empty hand + sneaking still enters the interior). Shears have no special use
 *     on a tank any more.</li>
 * </ul>
 */
public final class FloatingPlantEvents {
    private FloatingPlantEvents() {}


    /** The aquarium net: sneaking thins out by half, otherwise takes out the plants looked at (any angle). */
    static void net(ServerLevel server, TankBlockEntity c, Player player, boolean sneak) {
        TankData t = c.getTank();
        if (sneak) {
            List<ItemStack> items = TankPlants.thin(server, c);
            if (items.isEmpty()) player.displayClientMessage(Component.translatable("message.bettacraft.plants.none"), true);
            else TankPlants.taken(server, c, player, items, true);
            return;
        }
        // M14: a shrimp, crayfish, snail, body, clutch or moult the net is aimed at comes first (nearer than the plants)
        var aimed = org.overengineer.bettacraft.critters.TankCritters.aimed(t, player.getEyePosition(), player.getViewVector(1.0F));
        Vec3 at = TankPlants.netTarget(t, player.getEyePosition(), player.getViewVector(1.0F));
        // M19: a clutch of tankmate eggs (glass, plants, cave, foam nest) the net is aimed at, if nothing is nearer
        var clutch = org.overengineer.bettacraft.tankmates.TankmateItems.aimed(t, player.getEyePosition(), player.getViewVector(1.0F));
        if (clutch != null && (aimed == null || clutch.distance() <= aimed.distance()) && (at == null || clutch.distance() <= player.getEyePosition().distanceTo(at) + 0.05)) {
            Component msg = org.overengineer.bettacraft.tankmates.TankmateItems.net(server, c, player, clutch.clutch());
            if (msg != null) player.displayClientMessage(msg, true);
            return;
        }
        if (aimed != null && (at == null || aimed.distance() <= player.getEyePosition().distanceTo(at) + 0.05)) {
            Component msg = org.overengineer.bettacraft.critters.TankCritters.net(server, c, player, aimed);
            if (msg != null) player.displayClientMessage(msg, true);
            return;
        }
        List<ItemStack> items = at == null ? List.of() : TankPlants.removeAt(server, c, at.x, at.z, TankPlants.NET_REACH);
        if (items.isEmpty()) player.displayClientMessage(Component.translatable(!t.critters().isEmpty() && t.plants().isEmpty()
                ? "message.bettacraft.critters.none_here" : t.plants().isEmpty()
                ? "message.bettacraft.plants.none" : "message.bettacraft.plants.none_here"), true);
        else TankPlants.taken(server, c, player, items, false);
    }

    static boolean plantAt(TankData t, Vec3 at) {
        for (FloatingPlant p : t.plants()) {
            if (Math.hypot(p.x - at.x, p.z - at.z) <= TankPlants.TAKE_REACH + p.radius()) return true;
        }
        return false;
    }

    static boolean allowed(Player player, TankData t) {
        if (TankInhabitants.canModify(player, t)) return true;
        player.displayClientMessage(Component.translatable("message.bettacraft.tank.locked"), true);
        return false;
    }

}
