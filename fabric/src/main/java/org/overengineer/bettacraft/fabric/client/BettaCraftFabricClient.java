package org.overengineer.bettacraft.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.ClientSetup;
import org.overengineer.bettacraft.fabric.FabricNetwork;
import org.overengineer.bettacraft.fabric.FabricPlatform;
import org.overengineer.bettacraft.platform.client.ClientEvents;
import org.overengineer.bettacraft.platform.client.RenderLevelStageEvent;
import org.overengineer.bettacraft.registry.ModFluids;

/**
 * M52: the Fabric client entry point: the client payload handlers, renderers, layers, colours, screens, particles, fluids
 * and the common client events ({@link ClientEvents}) from Fabric's client callbacks. The registrations themselves are
 * the loader-independent {@link ClientSetup} (the same lists NeoForge uses).
 */
public final class BettaCraftFabricClient implements ClientModInitializer {
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void onInitializeClient() {
        FabricPlatform.clientSender = ClientPlayNetworking::send;
        for (FabricNetwork.ClientHandler<?> h : FabricNetwork.CLIENT_BOUND) register((FabricNetwork.ClientHandler) h);

        ClientSetup.init();
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jei")) {
            org.overengineer.bettacraft.client.ClientFeatures.jeiRefresh = () -> org.overengineer.bettacraft.compat.jei.BettaCraftJeiPlugin.applyFeatures();
        }
        ClientSetup.layers((loc, def) -> EntityModelLayerRegistry.registerModelLayer(loc, def::get));
        ClientSetup.renderers(new ClientSetup.Renderers() {
            @Override
            public <T extends net.minecraft.world.entity.Entity> void registerEntityRenderer(net.minecraft.world.entity.EntityType<? extends T> type,
                    net.minecraft.client.renderer.entity.EntityRendererProvider<T> provider) {
                EntityRendererRegistry.register((net.minecraft.world.entity.EntityType) type, provider);
            }

            @Override
            public <T extends net.minecraft.world.level.block.entity.BlockEntity> void registerBlockEntityRenderer(
                    net.minecraft.world.level.block.entity.BlockEntityType<? extends T> type,
                    net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider<T> provider) {
                BlockEntityRenderers.register((net.minecraft.world.level.block.entity.BlockEntityType) type, provider);
            }
        });
        ClientSetup.itemColors((color, items) -> ColorProviderRegistry.ITEM.register(color, items));
        ClientSetup.blockColors((color, blocks) -> ColorProviderRegistry.BLOCK.register(color, blocks));
        ClientSetup.screens(new ClientSetup.Screens() {
            @Override
            public <M extends net.minecraft.world.inventory.AbstractContainerMenu, U extends net.minecraft.client.gui.screens.Screen & net.minecraft.client.gui.screens.inventory.MenuAccess<M>> void register(
                    net.minecraft.world.inventory.MenuType<? extends M> type, MenuScreens.ScreenConstructor<M, U> factory) {
                MenuScreens.register((net.minecraft.world.inventory.MenuType) type, (MenuScreens.ScreenConstructor) factory);
            }
        });
        ClientSetup.itemProperties(ItemProperties::register);
        FabricRenderLayers.register();
        FabricModels.register();
        ClientSetup.particles(new ClientSetup.Particles() {
            @Override
            public <T extends net.minecraft.core.particles.ParticleOptions> void registerSpriteSet(net.minecraft.core.particles.ParticleType<T> type,
                    net.minecraft.client.particle.ParticleEngine.SpriteParticleRegistration<T> factory) {
                ParticleFactoryRegistry.getInstance().register(type, factory::create);
            }
        });
        // M25: our water fluids look like water, tinted (NeoForge: their fluid types' client extensions)
        water(ModFluids.RO_WATER_SPEC.source().get(), ModFluids.RO_WATER_SPEC.flowing().get(), 0xFFB0DCF4);
        water(ModFluids.SEA_WATER_SPEC.source().get(), ModFluids.SEA_WATER_SPEC.flowing().get(), 0xFF2A64B0);
        water(ModFluids.LIVE_FOOD_SPEC.source().get(), ModFluids.LIVE_FOOD_SPEC.flowing().get(), 0xFF8A9A4A);
        water(ModFluids.FERTILIZER_SPEC.source().get(), ModFluids.FERTILIZER_SPEC.flowing().get(), 0xFF6E8A1E);

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return BettaCraft.id("client_skins");
            }

            @Override
            public void onResourceManagerReload(ResourceManager rm) {
                ClientSetup.onResourcesReloaded();
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> ClientEvents.fireTickPost());
        // a screen is about to open (NeoForge: ScreenEvent.Opening; here: before it is initialised, also on a resize)
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.BEFORE_INIT.register((mc, screen, w, h) -> ClientEvents.fireScreenOpening(screen));
        // water in tank blocks: TankWaterRenderer (NeoForge: TankAwareWaterExtensions)
        TankAwareWater.register();
        // M21 follow-up: plant meshes follow their chunks
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.CHUNK_LOAD.register((level, chunk) ->
                org.overengineer.bettacraft.client.interior.PlantMeshRenderer.onChunkLoad(chunk.getPos().x, chunk.getPos().z));
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) ->
                org.overengineer.bettacraft.client.interior.PlantMeshRenderer.onChunkUnload(chunk.getPos().x, chunk.getPos().z));
        // M11 / M21: the tank look and the plant meshes are baked into the chunk meshes: rebuild them on a config change
        fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeModConfigEvents.reloading(BettaCraft.MOD_ID).register(config -> {
            if (config.getSpec() == org.overengineer.bettacraft.config.ClientConfig.SPEC) rebuildChunks();
            if (config.getSpec() == org.overengineer.bettacraft.config.CommonConfig.SPEC) {
                rebuildChunks();
                org.overengineer.bettacraft.client.interior.MiniatureSway.rebuildAll();
            }
        });
        fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeModConfigEvents.loading(BettaCraft.MOD_ID).register(config -> {
            if (config.getSpec() == org.overengineer.bettacraft.config.CommonConfig.SPEC) org.overengineer.bettacraft.client.interior.MiniatureSway.reset();
        });
        // M22.1: the swaying plants' tag; the stone cabinet recipes (NeoForge: RecipesUpdatedEvent; both come on join and reload)
        net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            if (!client) return;
            org.overengineer.bettacraft.client.interior.MiniatureSway.rebuildAll();
            org.overengineer.bettacraft.equipment.CabinetStones.clear();
        });
        // M17: block entities of a dropped client level leave the static sets
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((mc, level) -> lastLevel = purge(lastLevel, level));
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((be, level) -> {
            if (be instanceof org.overengineer.bettacraft.platform.BlockEntityLifecycle l) l.onLoad();
        });
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((be, level) -> {
            if (be instanceof org.overengineer.bettacraft.platform.BlockEntityLifecycle l) l.onChunkUnloaded();
        });
        ItemTooltipCallback.EVENT.register((stack, ctx, flag, lines) -> ClientEvents.fireTooltip(stack, ctx, lines, flag));
        HudRenderCallback.EVENT.register((gui, delta) -> ClientSetup.renderHud(gui, delta));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> {
            org.overengineer.bettacraft.client.ClientFeatures.logout();
            ClientSetup.onLogout();
            lastLevel = purge(lastLevel, null);
        });
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            // vanilla draws the block entities with the entities: the stage after both
            fire(ctx, RenderLevelStageEvent.Stage.AFTER_ENTITIES);
            fire(ctx, RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES);
        });
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            fire(ctx, RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS);
            fire(ctx, RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS);
        });
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> fire(ctx, RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS));
        WorldRenderEvents.LAST.register(ctx -> fire(ctx, RenderLevelStageEvent.Stage.AFTER_LEVEL));
    }

    private static net.minecraft.client.multiplayer.ClientLevel lastLevel;

    private static net.minecraft.client.multiplayer.ClientLevel purge(net.minecraft.client.multiplayer.ClientLevel old,
            net.minecraft.client.multiplayer.ClientLevel now) {
        if (old != null && old != now) org.overengineer.bettacraft.util.ClientBlockEntitySets.purgeLevel(old);
        return now;
    }

    private static Object lastTankLook;

    private static void rebuildChunks() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        Object look = java.util.List.of(org.overengineer.bettacraft.config.ClientConfig.CONNECTED_GLASS.get(),
                org.overengineer.bettacraft.config.ClientConfig.TANK_EDGE_WIDTH.get(), org.overengineer.bettacraft.client.interior.PlantMeshRenderer.active());
        if (look.equals(lastTankLook)) return;
        lastTankLook = look;
        mc.execute(() -> {
            if (mc.levelRenderer != null && mc.level != null) mc.levelRenderer.allChanged();
        });
    }

    private static <T extends CustomPacketPayload> void register(FabricNetwork.ClientHandler<T> h) {
        ClientPlayNetworking.registerGlobalReceiver(h.type(), (p, ctx) -> h.handler().handle(p, FabricNetwork.context(ctx.player(), ctx.client()::execute)));
    }

    private static void water(net.minecraft.world.level.material.Fluid still, net.minecraft.world.level.material.Fluid flowing, int tint) {
        FluidRenderHandlerRegistry.INSTANCE.register(still, flowing, SimpleFluidRenderHandler.coloredWater(tint));
    }

    private static void fire(WorldRenderContext ctx, RenderLevelStageEvent.Stage stage) {
        var ps = ctx.matrixStack() != null ? ctx.matrixStack() : new com.mojang.blaze3d.vertex.PoseStack();
        ClientEvents.fireRenderLevel(new RenderLevelStageEvent(stage, ctx.worldRenderer(), ps, ctx.positionMatrix(), ctx.projectionMatrix(),
                0, ctx.tickCounter(), ctx.camera(), ctx.frustum()));
    }
}
