package org.overengineer.bettacraft.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.overengineer.bettacraft.tank.interior.InteriorSync;
import org.overengineer.bettacraft.tank.menu.TankActions;

public final class ModNetwork {
    public static final String VERSION = "2";

    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(VERSION);
        r.playToClient(InteriorDataPayload.TYPE, InteriorDataPayload.STREAM_CODEC, ModNetwork::onInteriorData);
        r.playToServer(InteriorRequestPayload.TYPE, InteriorRequestPayload.STREAM_CODEC, ModNetwork::onInteriorRequest);
        r.playToClient(TankViewPayload.TYPE, TankViewPayload.STREAM_CODEC, ModNetwork::onTankView);
        r.playToServer(TankActionPayload.TYPE, TankActionPayload.STREAM_CODEC, ModNetwork::onTankAction);
    }

    private static void onInteriorData(InteriorDataPayload payload, IPayloadContext ctx) {
        // Only ever invoked on the client; the client class is loaded lazily inside the lambda.
        if (FMLEnvironment.dist.isClient()) {
            ctx.enqueueWork(() -> org.overengineer.bettacraft.client.interior.ClientInteriorCache.accept(payload));
        }
    }

    private static void onInteriorRequest(InteriorRequestPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) InteriorSync.handleRequest(sp, payload);
        });
    }

    private static void onTankView(TankViewPayload payload, IPayloadContext ctx) {
        if (FMLEnvironment.dist.isClient()) {
            ctx.enqueueWork(() -> org.overengineer.bettacraft.client.screen.ClientTankView.accept(payload));
        }
    }

    private static void onTankAction(TankActionPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) TankActions.handle(sp, payload);
        });
    }
}
