package org.overengineer.bettacraft.tank;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.event.EventPriority;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.BlockEvent;
import org.overengineer.bettacraft.platform.event.ExplosionEvent;
import org.overengineer.bettacraft.platform.event.PistonEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModBlocks;
import static org.overengineer.bettacraft.tank.TankProtection.*;

/** M52: the game event handlers of {@link TankProtection} (M52: on the common game events, fired by each loader). */
public final class TankProtectionHandlers {
    private TankProtectionHandlers() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getLevel() instanceof Level level) || level.isClientSide()) return;
        if (protectedBy(level, e.getPos()) == null) return;
        e.setCanceled(true);
        if (e.getPlayer() instanceof ServerPlayer sp) sp.sendOverlayMessage(message());
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate e) {
        if (!ServerConfig.getOr(ServerConfig.PROTECT_FROM_EXPLOSIONS_AND_PISTONS, true)) return;
        e.getAffectedBlocks().removeIf(p -> protectedBy(e.getLevel(), p) != null);
    }

    @SubscribeEvent
    public static void onPiston(PistonEvent.Pre e) {
        if (!(e.getLevel() instanceof Level level) || level.isClientSide()) return;
        if (!ServerConfig.getOr(ServerConfig.PROTECT_FROM_EXPLOSIONS_AND_PISTONS, true)) return;
        var helper = e.getStructureHelper();
        if (helper == null || !helper.resolve()) return;
        for (BlockPos p : helper.getToPush()) if (protectedBy(level, p) != null) { e.setCanceled(true); return; }
        for (BlockPos p : helper.getToDestroy()) if (protectedBy(level, p) != null) { e.setCanceled(true); return; }
    }
}
