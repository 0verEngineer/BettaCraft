package org.overengineer.bettacraft.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.overengineer.bettacraft.BettaCraft;

/**
 * M21.8: server settings the client must know at once (join and live reloads of the server config; NeoForge only sends
 * the server config file at login): the stack size of the artemia cyst jars, so client and server agree on merging.
 */
public record ServerSettingsPayload(int cystStack) implements CustomPacketPayload {
    public static final Type<ServerSettingsPayload> TYPE = new Type<>(BettaCraft.id("server_settings"));

    public static final StreamCodec<FriendlyByteBuf, ServerSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ServerSettingsPayload::cystStack, ServerSettingsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static ServerSettingsPayload current() {
        return new ServerSettingsPayload(org.overengineer.bettacraft.food.ArtemiaCystsItem.stackSize());
    }

    public static void send(ServerPlayer sp) {
        // fake and GameTest mock players have no channel
        if (sp.connection != null && sp.connection.hasChannel(TYPE)) PacketDistributor.sendToPlayer(sp, current());
    }

    public static void sendAll(MinecraftServer server) {
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) send(sp);
    }
}
