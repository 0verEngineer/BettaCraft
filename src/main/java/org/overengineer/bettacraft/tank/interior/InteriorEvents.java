package org.overengineer.bettacraft.tank.interior;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;

/** Game-bus events of the interior dimension (server side; also harmless on the client's integrated server). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class InteriorEvents {
    private static final Map<UUID, Integer> SNEAK_TICKS = new ConcurrentHashMap<>();

    private InteriorEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !InteriorManager.isInterior(player.level())) {
            if (!event.getEntity().level().isClientSide) SNEAK_TICKS.remove(event.getEntity().getUUID());
            return;
        }
        // Players outside every valid region (e.g. teleported in by command, region deleted) are sent back.
        if (player.tickCount % 40 == 0 && InteriorManager.regionAt(player.server, player.blockPosition()) == null
                && !player.hasPermissions(2)) {
            InteriorManager.leave(player);
            player.displayClientMessage(Component.translatable("message.bettacraft.interior.invalid"), true);
            return;
        }
        if (player.isShiftKeyDown()) {
            int needed = ServerConfig.getOr(ServerConfig.INTERIOR_LEAVE_SNEAK_TICKS, 60);
            int t = SNEAK_TICKS.merge(player.getUUID(), 1, Integer::sum);
            if (t >= needed) {
                SNEAK_TICKS.remove(player.getUUID());
                InteriorManager.leave(player);
            } else if (t % 10 == 0) {
                int secondsLeft = (int) Math.ceil((needed - t) / 20.0);
                player.displayClientMessage(Component.translatable("message.bettacraft.interior.leaving", secondsLeft), true);
            }
        } else {
            SNEAK_TICKS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level && InteriorManager.isInterior(level)
                && InteriorManager.isShellBlock(event.getState())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide && InteriorManager.isInterior(level)) {
            InteriorSync.onInteriorBlockChanged(level.getServer(), event.getPos());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 5 == 0) {
            InteriorSync.flush(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && InteriorManager.isInterior(player.level())
                && InteriorManager.regionAt(player.server, player.blockPosition()) == null) {
            InteriorManager.leave(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        InteriorSync.clearAll();
        SNEAK_TICKS.clear();
    }
}
