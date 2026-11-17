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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModBlocks;
import static org.overengineer.bettacraft.tank.TankProtection.*;

/** M52: the NeoForge event handlers of {@link TankProtection} (moved here from the common class). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
final class TankProtectionNeoForgeEvents {
    private TankProtectionNeoForgeEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getLevel() instanceof Level level) || level.isClientSide) return;
        if (protectedBy(level, e.getPos()) == null) return;
        e.setCanceled(true);
        if (e.getPlayer() instanceof ServerPlayer sp) sp.displayClientMessage(message(), true);
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate e) {
        if (!ServerConfig.getOr(ServerConfig.PROTECT_FROM_EXPLOSIONS_AND_PISTONS, true)) return;
        e.getAffectedBlocks().removeIf(p -> protectedBy(e.getLevel(), p) != null);
    }

    @SubscribeEvent
    public static void onPiston(PistonEvent.Pre e) {
        if (!(e.getLevel() instanceof Level level) || level.isClientSide) return;
        if (!ServerConfig.getOr(ServerConfig.PROTECT_FROM_EXPLOSIONS_AND_PISTONS, true)) return;
        var helper = e.getStructureHelper();
        if (helper == null || !helper.resolve()) return;
        for (BlockPos p : helper.getToPush()) if (protectedBy(level, p) != null) { e.setCanceled(true); return; }
        for (BlockPos p : helper.getToDestroy()) if (protectedBy(level, p) != null) { e.setCanceled(true); return; }
    }
}
