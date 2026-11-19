package org.overengineer.bettacraft.neoforge;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.overengineer.bettacraft.network.ModNetwork;
import org.overengineer.bettacraft.platform.network.IPayloadContext;
import org.overengineer.bettacraft.platform.network.PayloadRegistrar;

/** M52: registers the payloads of {@link ModNetwork} with NeoForge's payload registrar (version {@link ModNetwork#VERSION}). */
final class NeoForgeNetwork {
    private NeoForgeNetwork() {}

    static void register(RegisterPayloadHandlersEvent event) {
        net.neoforged.neoforge.network.registration.PayloadRegistrar neo = event.registrar(ModNetwork.VERSION);
        ModNetwork.register(new PayloadRegistrar() {
            @Override
            public <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Handler<T> handler) {
                neo.playToClient(type, codec, (p, ctx) -> handler.handle(p, context(ctx)));
            }

            @Override
            public <T extends CustomPacketPayload> void playToServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Handler<T> handler) {
                neo.playToServer(type, codec, (p, ctx) -> handler.handle(p, context(ctx)));
            }

            @Override
            public <T extends CustomPacketPayload> void playBidirectional(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Handler<T> handler) {
                // NeoForge 21.11: the three-argument form registers no client handler (the client then refuses to load)
                net.neoforged.neoforge.network.handling.IPayloadHandler<T> h = (p, ctx) -> handler.handle(p, context(ctx));
                neo.playBidirectional(type, codec, h, h);
            }
        });
    }

    private static IPayloadContext context(net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        return new IPayloadContext() {
            @Override
            public Player player() {
                return ctx.player();
            }

            @Override
            public void enqueueWork(Runnable work) {
                ctx.enqueueWork(work);
            }
        };
    }
}
