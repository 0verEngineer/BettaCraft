package org.overengineer.bettacraft.fabric;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import org.overengineer.bettacraft.network.ModNetwork;
import org.overengineer.bettacraft.platform.network.IPayloadContext;
import org.overengineer.bettacraft.platform.network.PayloadRegistrar;

/**
 * M52: the payloads of {@link ModNetwork} on Fabric: types registered on both sides, the server's handlers here; the
 * client's handlers are kept for the client entry point ({@link #CLIENT_BOUND}).
 */
public final class FabricNetwork {
    /** A client-bound payload type and its handler (registered by the client entry point). */
    public record ClientHandler<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type, PayloadRegistrar.Handler<T> handler) {}

    public static final List<ClientHandler<?>> CLIENT_BOUND = new ArrayList<>();

    private FabricNetwork() {}

    static void register() {
        ModNetwork.register(new PayloadRegistrar() {
            @Override
            public <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Handler<T> handler) {
                PayloadTypeRegistry.playS2C().register(type, codec);
                CLIENT_BOUND.add(new ClientHandler<>(type, handler));
            }

            @Override
            public <T extends CustomPacketPayload> void playToServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Handler<T> handler) {
                PayloadTypeRegistry.playC2S().register(type, codec);
                ServerPlayNetworking.registerGlobalReceiver(type, (p, ctx) -> handler.handle(p, context(ctx.player(), ctx.server()::execute)));
            }

            @Override
            public <T extends CustomPacketPayload> void playBidirectional(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Handler<T> handler) {
                playToServer(type, codec, handler);
                PayloadTypeRegistry.playS2C().register(type, codec);
                CLIENT_BOUND.add(new ClientHandler<>(type, handler));
            }
        });
    }

    public static IPayloadContext context(Player player, java.util.function.Consumer<Runnable> executor) {
        return new IPayloadContext() {
            @Override
            public Player player() {
                return player;
            }

            @Override
            public void enqueueWork(Runnable work) {
                executor.accept(work);
            }
        };
    }
}
