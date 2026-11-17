package org.overengineer.bettacraft.network;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import org.overengineer.bettacraft.platform.network.PacketDistributor;
import org.overengineer.bettacraft.platform.network.IPayloadContext;
import org.overengineer.bettacraft.platform.network.PayloadRegistrar;
import org.overengineer.bettacraft.decor.DecorItem;
import org.overengineer.bettacraft.decor.DecorType;
import org.overengineer.bettacraft.tank.interior.InteriorSync;
import org.overengineer.bettacraft.tank.menu.TankActions;

public final class ModNetwork {
    public static final String VERSION = "19"; // M51: the menu of the hired aquarist (M50: travelling tanks; M37: fertilizer in water values)

    private ModNetwork() {}

    /** M52: the payload types and their handlers (the loader passes its registrar for {@link #VERSION}). */
    public static void register(PayloadRegistrar r) {
        r.playToClient(InteriorDataPayload.TYPE, InteriorDataPayload.STREAM_CODEC, ModNetwork::onInteriorData);
        r.playToClient(InteriorDeltaPayload.TYPE, InteriorDeltaPayload.STREAM_CODEC, ModNetwork::onInteriorDelta);
        r.playToServer(InteriorRequestPayload.TYPE, InteriorRequestPayload.STREAM_CODEC, ModNetwork::onInteriorRequest);
        r.playToClient(TankViewPayload.TYPE, TankViewPayload.STREAM_CODEC, ModNetwork::onTankView);
        r.playToServer(TankActionPayload.TYPE, TankActionPayload.STREAM_CODEC, ModNetwork::onTankAction);
        r.playToServer(RegulatorPayload.TYPE, RegulatorPayload.STREAM_CODEC, ModNetwork::onRegulator); // M23
        r.playToServer(WaterChangerPayload.TYPE, WaterChangerPayload.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) WaterChangerPayload.apply(sp, p);
        })); // M24
        r.playToServer(TankModePayload.TYPE, TankModePayload.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) TankModePayload.apply(sp, p);
        })); // M33.1
        r.playToClient(GeneticsSyncPayload.TYPE, GeneticsSyncPayload.STREAM_CODEC, ModNetwork::onGenetics);
        r.playToClient(TankPlantsPayload.TYPE, TankPlantsPayload.STREAM_CODEC, ModNetwork::onTankPlants);
        r.playToClient(TankCrittersPayload.TYPE, TankCrittersPayload.STREAM_CODEC, ModNetwork::onTankCritters);
        r.playToClient(InteriorLeavePayload.TYPE, InteriorLeavePayload.STREAM_CODEC, ModNetwork::onInteriorLeave); // M15
        r.playToClient(ServerSettingsPayload.TYPE, ServerSettingsPayload.STREAM_CODEC, ModNetwork::onServerSettings); // M21.8
        r.playToServer(EmptyConfirmPayload.TYPE, EmptyConfirmPayload.STREAM_CODEC, ModNetwork::onEmptyConfirm); // M22.1
        r.playToClient(MonitorDataPayload.TYPE, MonitorDataPayload.STREAM_CODEC, ModNetwork::onMonitorData); // M27
        r.playToServer(MonitorLayoutPayload.TYPE, MonitorLayoutPayload.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) org.overengineer.bettacraft.monitor.MonitorBlockEntity.applyLayout(sp, p.pos(), p.layout());
        })); // M27 (the editor of part b)
        r.playToClient(AlarmWaypointPayload.TYPE, AlarmWaypointPayload.STREAM_CODEC, ModNetwork::onAlarmWaypoint); // M32 part d
        r.playToClient(FeatureSyncPayload.TYPE, FeatureSyncPayload.STREAM_CODEC, ModNetwork::onFeatures); // M33 part b
        r.playToClient(TravellingTankPayload.TYPE, TravellingTankPayload.STREAM_CODEC, ModNetwork::onTravellingTank); // M50
        r.playToClient(RecipeSyncPayload.TYPE, RecipeSyncPayload.STREAM_CODEC, (p, ctx) -> {
            if (org.overengineer.bettacraft.platform.Platform.get().isClient()) ctx.enqueueWork(() -> org.overengineer.bettacraft.crafting.RecipeSync.accept(p));
        }); // M52b
        r.playToClient(AquaristViewPayload.TYPE, AquaristViewPayload.STREAM_CODEC, ModNetwork::onAquaristView); // M51
        r.playToServer(AquaristActionPayload.TYPE, AquaristActionPayload.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) org.overengineer.bettacraft.aquarist.AquaristMenu.handle(sp, p);
        })); // M51
        r.playBidirectional(DecorTemplatePayload.TYPE, DecorTemplatePayload.STREAM_CODEC, ModNetwork::onDecorTemplate);
    }

    private static void onAquaristView(AquaristViewPayload payload, IPayloadContext ctx) {
        if (org.overengineer.bettacraft.platform.Platform.get().isClient()) {
            ctx.enqueueWork(() -> org.overengineer.bettacraft.client.screen.AquaristScreen.accept(payload.view()));
        }
    }

    private static void onTravellingTank(TravellingTankPayload payload, IPayloadContext ctx) {
        if (org.overengineer.bettacraft.platform.Platform.get().isClient() && org.overengineer.bettacraft.compat.create.CreateCompat.loaded()) {
            ctx.enqueueWork(() -> org.overengineer.bettacraft.compat.create.CreateCompat.travelClient.accept(payload));
        }
    }

    private static void onMonitorData(MonitorDataPayload payload, IPayloadContext ctx) {
        if (org.overengineer.bettacraft.platform.Platform.get().isClient()) {
            ctx.enqueueWork(() -> org.overengineer.bettacraft.client.render.MonitorRenderer.accept(payload));
        }
    }

    private static void onFeatures(FeatureSyncPayload payload, IPayloadContext ctx) {
        if (org.overengineer.bettacraft.platform.Platform.get().isClient()) {
            ctx.enqueueWork(() -> org.overengineer.bettacraft.client.ClientFeatures.accept(payload.off(), payload.preset(), payload.keep()));
        }
    }

    private static void onAlarmWaypoint(AlarmWaypointPayload payload, IPayloadContext ctx) {
        if (org.overengineer.bettacraft.platform.Platform.get().isClient()) {
            ctx.enqueueWork(() -> org.overengineer.bettacraft.client.map.AlarmWaypoints.accept(payload));
        }
    }

    private static void onEmptyConfirm(EmptyConfirmPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) org.overengineer.bettacraft.containers.ContainerRules.confirm(sp, payload.interactionHand());
        });
    }

    private static void onServerSettings(ServerSettingsPayload payload, IPayloadContext ctx) {
        // common code: on an integrated server both sides read the same config anyway
        ctx.enqueueWork(() -> {
            org.overengineer.bettacraft.genetics.GeneVisibility.setClientFree(payload.genesFree());
            org.overengineer.bettacraft.config.AnimalSizes.setClient(org.overengineer.bettacraft.config.AnimalSize.byName(payload.bettaSize())); // M36
        });
    }

    private static void onGenetics(GeneticsSyncPayload payload, IPayloadContext ctx) {
        // common code (no client classes): on an integrated server both sides share the same data anyway
        ctx.enqueueWork(() -> org.overengineer.bettacraft.genetics.GeneticsData.set(
                org.overengineer.bettacraft.genetics.GeneticsData.fromNbt(payload.data())));
    }

    private static void onInteriorData(InteriorDataPayload payload, IPayloadContext ctx) {
        // Only ever invoked on the client; the client class is loaded lazily inside the lambda.
        if (org.overengineer.bettacraft.platform.Platform.get().isClient()) {
            ctx.enqueueWork(() -> org.overengineer.bettacraft.client.interior.ClientInteriorCache.accept(payload));
        }
    }

    private static void onInteriorDelta(InteriorDeltaPayload payload, IPayloadContext ctx) {
        if (org.overengineer.bettacraft.platform.Platform.get().isClient()) {
            ctx.enqueueWork(() -> org.overengineer.bettacraft.client.interior.ClientInteriorCache.acceptDelta(payload));
        }
    }

    private static void onInteriorRequest(InteriorRequestPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) InteriorSync.handleRequest(sp, payload);
        });
    }

    private static void onTankView(TankViewPayload payload, IPayloadContext ctx) {
        if (org.overengineer.bettacraft.platform.Platform.get().isClient()) {
            ctx.enqueueWork(() -> org.overengineer.bettacraft.client.screen.ClientTankView.accept(payload));
        }
    }

    private static void onTankPlants(TankPlantsPayload payload, IPayloadContext ctx) {
        if (org.overengineer.bettacraft.platform.Platform.get().isClient()) {
            ctx.enqueueWork(() -> org.overengineer.bettacraft.client.render.FloatingPlantRenderer.accept(payload));
        }
    }

    private static void onTankCritters(TankCrittersPayload payload, IPayloadContext ctx) {
        if (org.overengineer.bettacraft.platform.Platform.get().isClient()) {
            ctx.enqueueWork(() -> org.overengineer.bettacraft.client.render.CritterRenderer.accept(payload));
        }
    }

    private static void onInteriorLeave(InteriorLeavePayload payload, IPayloadContext ctx) {
        if (org.overengineer.bettacraft.platform.Platform.get().isClient()) {
            ctx.enqueueWork(() -> org.overengineer.bettacraft.client.interior.InteriorLeaveHud.accept(payload));
        }
    }

    private static void onRegulator(RegulatorPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp)
                org.overengineer.bettacraft.equipment.Regulator.apply(sp, payload.pos(), payload.value(), !payload.relative());
        });
    }

    private static void onTankAction(TankActionPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) TankActions.handle(sp, payload);
        });
    }

    /** M33 part d (hardening): decoration templates a player asked for in the current second (server thread only). */
    private static final java.util.Map<ServerPlayer, long[]> TEMPLATE_REQUESTS = new java.util.WeakHashMap<>();
    /** Requests per player and second that are answered (there are fewer decoration types; a client asks once each). */
    public static final int TEMPLATE_REQUESTS_PER_SECOND = 32;

    /** Whether a template request of this player is answered now (counts it). */
    public static boolean templateRequestAllowed(ServerPlayer sp) {
        long second = sp.level().getGameTime() / 20;
        long[] c = TEMPLATE_REQUESTS.computeIfAbsent(sp, k -> new long[]{second, 0});
        if (c[0] != second) {
            c[0] = second;
            c[1] = 0;
        }
        return ++c[1] <= TEMPLATE_REQUESTS_PER_SECOND;
    }

    private static void onDecorTemplate(DecorTemplatePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                if (!templateRequestAllowed(sp)) return;
                // only our own decoration structures are served
                boolean known = false;
                for (DecorType t : DecorType.values()) known |= t.structure().equals(payload.id());
                if (!known) return;
                PacketDistributor.sendToPlayer(sp, new DecorTemplatePayload(payload.id(),
                        DecorItem.template(sp.level(), payload.id()).map(Optional::of).orElse(Optional.empty())));
            } else if (org.overengineer.bettacraft.platform.Platform.get().isClient() && payload.template().isPresent()) {
                org.overengineer.bettacraft.client.decor.DecorPreview.accept(payload);
            }
        });
    }
}
