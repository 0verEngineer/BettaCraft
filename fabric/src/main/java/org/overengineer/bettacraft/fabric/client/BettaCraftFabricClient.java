package org.overengineer.bettacraft.fabric.client;

import net.fabricmc.api.ClientModInitializer;
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
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
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
        // M53: the config screen in Mod Menu (Forge Config API Port's port of NeoForge's screen, like NeoForge's mod list)
        fuzs.forgeconfigapiport.fabric.api.v5.client.ConfigScreenFactoryRegistry.INSTANCE.register(org.overengineer.bettacraft.BettaCraft.MOD_ID,
                net.neoforged.neoforge.client.gui.ConfigurationScreen::new);
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
            public <T extends net.minecraft.world.level.block.entity.BlockEntity, S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState> void registerBlockEntityRenderer(
                    net.minecraft.world.level.block.entity.BlockEntityType<? extends T> type,
                    net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider<T, S> provider) {
                BlockEntityRenderers.register((net.minecraft.world.level.block.entity.BlockEntityType) type, provider);
            }
        });
        // M52b: the item colours and properties of 1.21.1 as our own tint source and item model property (1.21.4 item models)
        ClientSetup.itemModelTypes(new ClientSetup.ItemModelTypes() {
            @Override
            public void tintSource(Identifier id, com.mojang.serialization.MapCodec<? extends net.minecraft.client.color.item.ItemTintSource> codec) {
                net.minecraft.client.color.item.ItemTintSources.ID_MAPPER.put(id, codec);
            }

            @Override
            public void conditionalProperty(Identifier id,
                    com.mojang.serialization.MapCodec<? extends net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty> codec) {
                net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties.ID_MAPPER.put(id, codec);
            }

            @Override
            public void itemModel(Identifier id, com.mojang.serialization.MapCodec<? extends net.minecraft.client.renderer.item.ItemModel.Unbaked> codec) {
                net.minecraft.client.renderer.item.ItemModels.ID_MAPPER.put(id, codec);
            }
        });
        ClientSetup.itemColors(org.overengineer.bettacraft.client.item.ItemTints::register);
        ClientSetup.blockColors((color, blocks) -> ColorProviderRegistry.BLOCK.register(color, blocks));
        ClientSetup.screens(new ClientSetup.Screens() {
            @Override
            public <M extends net.minecraft.world.inventory.AbstractContainerMenu, U extends net.minecraft.client.gui.screens.Screen & net.minecraft.client.gui.screens.inventory.MenuAccess<M>> void register(
                    net.minecraft.world.inventory.MenuType<? extends M> type, MenuScreens.ScreenConstructor<M, U> factory) {
                MenuScreens.register((net.minecraft.world.inventory.MenuType) type, (MenuScreens.ScreenConstructor) factory);
            }
        });
        ClientSetup.itemProperties(org.overengineer.bettacraft.client.item.ItemPredicates::register);
        FabricRenderLayers.register();
        FabricModels.register();
        ClientSetup.particles(new ClientSetup.Particles() {
            @Override
            public <T extends net.minecraft.core.particles.ParticleOptions> void registerSpriteSet(net.minecraft.core.particles.ParticleType<T> type,
                    java.util.function.Function<net.minecraft.client.particle.SpriteSet, net.minecraft.client.particle.ParticleProvider<T>> factory) {
                ParticleFactoryRegistry.getInstance().register(type, factory::apply);
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
        fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents.reloading(BettaCraft.MOD_ID).register(config -> {
            if (config.getSpec() == org.overengineer.bettacraft.config.ClientConfig.SPEC) rebuildChunks();
            if (config.getSpec() == org.overengineer.bettacraft.config.CommonConfig.SPEC) {
                rebuildChunks();
                org.overengineer.bettacraft.client.interior.MiniatureSway.rebuildAll();
            }
        });
        fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents.loading(BettaCraft.MOD_ID).register(config -> {
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
        // M15: how to leave a tank interior, bottom left (below the chat, as on NeoForge)
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, BettaCraft.id("interior_leave_hint"), ClientSetup::renderHud);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> {
            org.overengineer.bettacraft.client.ClientFeatures.logout();
            ClientSetup.onLogout();
            lastLevel = purge(lastLevel, null);
        });
        // M52b: the stages of NeoForge 21.11 (the opaque terrain, then entities and block entities, translucent terrain)
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> fire(ctx.matrices(), RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS,
                RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS, RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS));
        // vanilla draws the block entities with the entities: the stage after both
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> fire(ctx.matrices(), RenderLevelStageEvent.Stage.AFTER_ENTITIES,
                RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES));
        // AFTER_TRANSLUCENT_BLOCKS: LevelRendererTranslucentMixin (Fabric has no event after the translucent terrain)
        WorldRenderEvents.END_MAIN.register(ctx -> fire(ctx.matrices(), RenderLevelStageEvent.Stage.AFTER_LEVEL));
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

    /** Our render stages at one of Fabric's world render events (or the translucent mixin: no pose stack). */
    public static void fire(com.mojang.blaze3d.vertex.@org.jetbrains.annotations.Nullable PoseStack matrices, RenderLevelStageEvent.Stage... stages) {
        Minecraft mc = Minecraft.getInstance();
        var ps = matrices != null ? matrices : new com.mojang.blaze3d.vertex.PoseStack();
        for (RenderLevelStageEvent.Stage s : stages) {
            ClientEvents.fireRenderLevel(new RenderLevelStageEvent(s, mc.levelRenderer, ps, org.overengineer.bettacraft.platform.client.RenderFrame.modelView(),
                    org.overengineer.bettacraft.platform.client.RenderFrame.projection(), mc.levelRenderer.ticks, mc.getDeltaTracker(),
                    mc.gameRenderer.getMainCamera(), org.overengineer.bettacraft.platform.client.RenderFrame.frustum()));
        }
    }
}
