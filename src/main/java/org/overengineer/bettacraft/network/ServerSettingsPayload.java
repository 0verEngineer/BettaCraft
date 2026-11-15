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
 * the server config file at login): M22.3: whether the hidden genes of the animals are free (no magnifier needed) for the
 * tooltips. (M21.8..M35 also sent the stack size of the artemia cyst jars; M35.1: they always stack to 64.) M36: the size
 * setting of the bettas ({@code sizes.betta}), so every player sees the size the hitbox has on the server.
 */
public record ServerSettingsPayload(boolean genesFree, String bettaSize) implements CustomPacketPayload {
    public static final Type<ServerSettingsPayload> TYPE = new Type<>(BettaCraft.id("server_settings"));

    public static final StreamCodec<FriendlyByteBuf, ServerSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ServerSettingsPayload::genesFree,
            ByteBufCodecs.STRING_UTF8, ServerSettingsPayload::bettaSize, ServerSettingsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static ServerSettingsPayload current() {
        return new ServerSettingsPayload(org.overengineer.bettacraft.genetics.GeneVisibility.serverFree(), // M22.3: no magnifier needed on this server
                org.overengineer.bettacraft.config.AnimalSizes.serverBetta().name()); // M36
    }

    public static void send(ServerPlayer sp) {
        // fake and GameTest mock players have no channel
        if (sp.connection != null && sp.connection.hasChannel(TYPE)) PacketDistributor.sendToPlayer(sp, current());
    }

    public static void sendAll(MinecraftServer server) {
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) send(sp);
    }
}
