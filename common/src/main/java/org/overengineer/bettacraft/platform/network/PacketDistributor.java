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
        Platform.get().sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        Platform.get().sendToPlayer(player, payload);
    }

    public static void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload payload) {
        Platform.get().sendToPlayersTrackingEntity(entity, payload);
    }

    public static void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload) {
        Platform.get().sendToPlayersTrackingEntityAndSelf(entity, payload);
    }

    public static void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunk, CustomPacketPayload payload) {
        Platform.get().sendToPlayersTrackingChunk(level, chunk, payload);
    }

    public static void sendToAllPlayers(CustomPacketPayload payload) {
        Platform.get().sendToAllPlayers(payload);
    }
}
