package org.overengineer.bettacraft.network;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.overengineer.bettacraft.decor.DecorItem;
import org.overengineer.bettacraft.decor.DecorType;
import org.overengineer.bettacraft.tank.interior.InteriorSync;
import org.overengineer.bettacraft.tank.menu.TankActions;

public final class ModNetwork {
    public static final String VERSION = "4";

    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(VERSION);
        r.playToClient(InteriorDataPayload.TYPE, InteriorDataPayload.STREAM_CODEC, ModNetwork::onInteriorData);
        r.playToClient(InteriorDeltaPayload.TYPE, InteriorDeltaPayload.STREAM_CODEC, ModNetwork::onInteriorDelta);
        r.playToServer(InteriorRequestPayload.TYPE, InteriorRequestPayload.STREAM_CODEC, ModNetwork::onInteriorRequest);
        r.playToClient(TankViewPayload.TYPE, TankViewPayload.STREAM_CODEC, ModNetwork::onTankView);
        r.playToServer(TankActionPayload.TYPE, TankActionPayload.STREAM_CODEC, ModNetwork::onTankAction);
        r.playToClient(GeneticsSyncPayload.TYPE, GeneticsSyncPayload.STREAM_CODEC, ModNetwork::onGenetics);
        r.playBidirectional(DecorTemplatePayload.TYPE, DecorTemplatePayload.STREAM_CODEC, ModNetwork::onDecorTemplate);
    }

    private static void onGenetics(GeneticsSyncPayload payload, IPayloadContext ctx) {
        // common code (no client classes): on an integrated server both sides share the same data anyway
        ctx.enqueueWork(() -> org.overengineer.bettacraft.genetics.GeneticsData.set(
                org.overengineer.bettacraft.genetics.GeneticsData.fromNbt(payload.data())));
    }

    private static void onInteriorData(InteriorDataPayload payload, IPayloadContext ctx) {
        // Only ever invoked on the client; the client class is loaded lazily inside the lambda.
        if (FMLEnvironment.dist.isClient()) {
            ctx.enqueueWork(() -> org.overengineer.bettacraft.client.interior.ClientInteriorCache.accept(payload));
        }
    }

    private static void onInteriorDelta(InteriorDeltaPayload payload, IPayloadContext ctx) {
        if (FMLEnvironment.dist.isClient()) {
            ctx.enqueueWork(() -> org.overengineer.bettacraft.client.interior.ClientInteriorCache.acceptDelta(payload));
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

    private static void onDecorTemplate(DecorTemplatePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                // only our own decoration structures are served
                boolean known = false;
                for (DecorType t : DecorType.values()) known |= t.structure().equals(payload.id());
                if (!known) return;
                PacketDistributor.sendToPlayer(sp, new DecorTemplatePayload(payload.id(),
                        DecorItem.template(sp.serverLevel(), payload.id()).map(Optional::of).orElse(Optional.empty())));
            } else if (FMLEnvironment.dist.isClient() && payload.template().isPresent()) {
                org.overengineer.bettacraft.client.decor.DecorPreview.accept(payload);
            }
        });
    }
}
