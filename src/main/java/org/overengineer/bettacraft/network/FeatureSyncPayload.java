package org.overengineer.bettacraft.network;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Presets;

/**
 * M33 part b: the feature switches of the server (the paths whose own switch is off) for its clients, at login and after
 * every reload of the common config. NeoForge does not sync the common config, so without this a client joining a server
 * with other switches would show the creative tab, JEI and the handbook of its own config. M33 part c: also the preset and
 * its keep list ({@link Presets}), so the client reads the server config values (synced by NeoForge) with the server's preset.
 */
public record FeatureSyncPayload(List<String> off, String preset, List<String> keep) implements CustomPacketPayload {
    public static final Type<FeatureSyncPayload> TYPE = new Type<>(BettaCraft.id("features"));

    public static final StreamCodec<FriendlyByteBuf, FeatureSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(512)), FeatureSyncPayload::off,
            ByteBufCodecs.STRING_UTF8, FeatureSyncPayload::preset,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(512)), FeatureSyncPayload::keep, FeatureSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static FeatureSyncPayload current() {
        return new FeatureSyncPayload(List.copyOf(Features.ownOff()), Presets.active().id(), List.copyOf(Presets.keep()));
    }

    public static void send(ServerPlayer sp) {
        // fake and GameTest mock players have no channel
        if (sp.connection != null && sp.connection.hasChannel(TYPE)) PacketDistributor.sendToPlayer(sp, current());
    }

    /** To every player (after a reload of the common config, or when a preset changed the switches). */
    public static void sendAll(MinecraftServer server) {
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) send(sp);
    }
}
