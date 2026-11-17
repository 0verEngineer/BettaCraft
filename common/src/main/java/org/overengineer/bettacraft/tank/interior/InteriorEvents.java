package org.overengineer.bettacraft.tank.interior;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.decor.DecorItem;

/** Game-bus events of the interior dimension (server side; also harmless on the client's integrated server). */
public final class InteriorEvents {
    static final Map<UUID, Integer> SNEAK_TICKS = new ConcurrentHashMap<>();

    private InteriorEvents() {}



    /**
     * M15: one tick of the leave timer with the player's own input: it counts while the player sneaks AND looks steeply
     * up ({@code interior.leaveLookUpDegrees}), so sneaking to set substrate does not take you out. True if the player left.
     */
    public static boolean leaveTick(ServerPlayer player) {
        return sneakTick(player, player.isShiftKeyDown() && looksUp(player));
    }

    /** Whether the player looks up steeply enough to leave (pitch at most minus {@code interior.leaveLookUpDegrees}). */
    public static boolean looksUp(Player player) {
        int deg = ServerConfig.getOr(ServerConfig.INTERIOR_LEAVE_LOOK_UP, 60);
        return deg <= 0 || player.getXRot() <= -deg;
    }

    /** One tick of the "hold to leave" timer (PLAN 7.1); the client shows the progress (M15). Returns true if the player left. */
    public static boolean sneakTick(ServerPlayer player, boolean sneaking) {
        int needed = ServerConfig.getOr(ServerConfig.INTERIOR_LEAVE_SNEAK_TICKS, 60);
        if (!sneaking) {
            resetLeave(player);
            return false;
        }
        int t = SNEAK_TICKS.merge(player.getUUID(), 1, Integer::sum);
        if (t >= needed) {
            SNEAK_TICKS.remove(player.getUUID());
            progress(player, 0, needed);
            InteriorManager.leave(player);
            return true;
        }
        progress(player, t, needed);
        return false;
    }

    /** M15: placing or breaking something (any use of a block) starts the leave timer over. */
    public static void resetLeave(Player player) {
        if (player instanceof ServerPlayer sp && SNEAK_TICKS.remove(sp.getUUID()) != null) {
            progress(sp, 0, ServerConfig.getOr(ServerConfig.INTERIOR_LEAVE_SNEAK_TICKS, 60));
        }
    }

    /** Ticks the leave timer has counted for this player (0 = not holding). */
    public static int leaveTicks(Player player) {
        return SNEAK_TICKS.getOrDefault(player.getUUID(), 0);
    }

    private static void progress(ServerPlayer player, int ticks, int needed) {
        if (player.connection != null) {
            org.overengineer.bettacraft.platform.network.PacketDistributor.sendToPlayer(player,
                    new org.overengineer.bettacraft.network.InteriorLeavePayload(ticks, needed));
        }
    }








}
