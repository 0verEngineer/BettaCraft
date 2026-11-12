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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.overengineer.bettacraft.BettaCraft;
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
 *     <li>shears: take out the plants looked at; sneaking: thin the whole tank out by half (empty hand + sneaking still
 *     enters the interior).</li>
 * </ul>
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class FloatingPlantEvents {
    private FloatingPlantEvents() {}

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
        boolean shears = stack.is(Tags.Items.TOOLS_SHEAR);
        if (shears && sneak) {
            handled(e);
            if (level instanceof ServerLevel server && allowed(player, t)) {
                List<ItemStack> items = TankPlants.thin(server, c);
                if (items.isEmpty()) player.displayClientMessage(Component.translatable("message.bettacraft.plants.none"), true);
                else TankPlants.taken(server, c, player, items, true);
            }
            return;
        }
        boolean hand = stack.isEmpty() && e.getHand() == InteractionHand.MAIN_HAND && !sneak;
        if (!shears && !hand) return;
        Vec3 at = TankPlants.surfaceHit(t, player.getEyePosition(), player.getViewVector(1.0F));
        if (at == null || !plantAt(t, at)) return; // nothing there: menu / shears as usual
        handled(e);
        if (level instanceof ServerLevel server && allowed(player, t)) {
            List<ItemStack> items = TankPlants.removeAt(server, c, at.x, at.z);
            if (!items.isEmpty()) TankPlants.taken(server, c, player, items, false);
        }
    }

    private static boolean plantAt(TankData t, Vec3 at) {
        for (FloatingPlant p : t.plants()) {
            if (Math.hypot(p.x - at.x, p.z - at.z) <= TankPlants.TAKE_REACH + p.radius()) return true;
        }
        return false;
    }

    private static boolean allowed(Player player, TankData t) {
        if (TankInhabitants.canModify(player, t)) return true;
        player.displayClientMessage(Component.translatable("message.bettacraft.tank.locked"), true);
        return false;
    }

    private static void handled(PlayerInteractEvent.RightClickBlock e) {
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.sidedSuccess(e.getLevel().isClientSide));
    }
}
