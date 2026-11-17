package org.overengineer.bettacraft.platform.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** M52: registers our payload types (play phase) with the loader; {@link org.overengineer.bettacraft.network.ModNetwork} fills it. */
public interface PayloadRegistrar {
    @FunctionalInterface
    interface Handler<T extends CustomPacketPayload> {
        void handle(T payload, IPayloadContext context);
    }

    <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Handler<T> handler);

    <T extends CustomPacketPayload> void playToServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Handler<T> handler);

    <T extends CustomPacketPayload> void playBidirectional(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Handler<T> handler);
}
