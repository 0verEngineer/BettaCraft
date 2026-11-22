package org.overengineer.bettacraft.platform.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.overengineer.bettacraft.platform.Platform;

/** M52: sending our payloads (the same calls as NeoForge's class of the same name, through the platform layer). */
public final class PacketDistributor {
    private PacketDistributor() {}

    public static void sendToServer(CustomPacketPayload payload) {
        if (org.overengineer.bettacraft.debug.LogArea.NETWORK.debug) org.overengineer.bettacraft.debug.PacketStats.sent(payload, "server"); // M57
        Platform.get().sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (org.overengineer.bettacraft.debug.LogArea.NETWORK.debug) org.overengineer.bettacraft.debug.PacketStats.sent(payload, player.getScoreboardName()); // M57
        Platform.get().sendToPlayer(player, payload);
    }

    public static void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload payload) {
        if (org.overengineer.bettacraft.debug.LogArea.NETWORK.debug) org.overengineer.bettacraft.debug.PacketStats.sent(payload, "tracking"); // M57
        Platform.get().sendToPlayersTrackingEntity(entity, payload);
    }

    public static void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload) {
        if (org.overengineer.bettacraft.debug.LogArea.NETWORK.debug) org.overengineer.bettacraft.debug.PacketStats.sent(payload, "tracking"); // M57
        Platform.get().sendToPlayersTrackingEntityAndSelf(entity, payload);
    }

    public static void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunk, CustomPacketPayload payload) {
        if (org.overengineer.bettacraft.debug.LogArea.NETWORK.debug) org.overengineer.bettacraft.debug.PacketStats.sent(payload, "chunk"); // M57
        Platform.get().sendToPlayersTrackingChunk(level, chunk, payload);
    }

    public static void sendToAllPlayers(CustomPacketPayload payload) {
        if (org.overengineer.bettacraft.debug.LogArea.NETWORK.debug) org.overengineer.bettacraft.debug.PacketStats.sent(payload, "all"); // M57
        Platform.get().sendToAllPlayers(payload);
    }
}
