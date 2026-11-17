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
import org.overengineer.bettacraft.platform.event.EventPriority;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.PlayerInteractEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;
import static org.overengineer.bettacraft.tank.plants.FloatingPlantEvents.*;

/** M52: the game event handlers of {@link FloatingPlantEvents} (M52: on the common game events, fired by each loader). */
public final class FloatingPlantEventsHandlers {
    private FloatingPlantEventsHandlers() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        Level level = e.getLevel();
        BlockPos pos = e.getPos();
        if (!(level.getBlockState(pos).getBlock() instanceof TankBlock)) return;
        Player player = e.getEntity();
        ItemStack stack = e.getItemStack();
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        if (c == null || c.getTank() == null) return;
        TankData t = c.getTank();
        boolean sneak = player.isSecondaryUseActive();
        if (TankPlants.typeOf(stack) != null) {
            handled(e);
            if (level instanceof ServerLevel server && allowed(player, t)) {
                TankPlants.place(server, c, player, stack, e.getHitVec().getLocation());
            }
            return;
        }
        if (stack.is(ModItems.AQUARIUM_NET.get())) {
            handled(e);
            if (level instanceof ServerLevel server && allowed(player, t)) net(server, c, player, sneak);
            return;
        }
        boolean hand = stack.isEmpty() && e.getHand() == InteractionHand.MAIN_HAND && !sneak;
        if (!hand) return;
        Vec3 at = TankPlants.surfaceHit(t, player.getEyePosition(), player.getViewVector(1.0F));
        if (at == null || !plantAt(t, at)) return; // nothing there: the menu opens as usual
        handled(e);
        if (level instanceof ServerLevel server && allowed(player, t)) {
            List<ItemStack> items = TankPlants.removeAt(server, c, at.x, at.z);
            if (!items.isEmpty()) TankPlants.taken(server, c, player, items, false);
        }
    }

    static void handled(PlayerInteractEvent.RightClickBlock e) {
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }
}
