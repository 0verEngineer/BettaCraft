package org.overengineer.bettacraft.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.interior.ClientInteriorCache;
import org.overengineer.bettacraft.client.interior.MiniatureRenderer;
import org.overengineer.bettacraft.client.model.BettaModelDefinitions;
import org.overengineer.bettacraft.client.render.BettaRenderer;
import org.overengineer.bettacraft.client.render.BettaTints;
import org.overengineer.bettacraft.client.render.TankSignRenderer;
import org.overengineer.bettacraft.client.screen.FeederScreen;
import org.overengineer.bettacraft.client.screen.TankScreen;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModMenus;

/** Client-only entry point (never loaded on a dedicated server). */
@Mod(value = BettaCraft.MOD_ID, dist = Dist.CLIENT)
public final class BettaCraftClient {
    /** Tint of the fish overlay of a bag holding a non-betta animal. */
    private static Object lastTankLook;

    /** Re-meshes all chunks when the tank look settings changed (called from the config screen's reload). */
    private static void rebuildChunks() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        Object look = java.util.List.of(org.overengineer.bettacraft.config.ClientConfig.CONNECTED_GLASS.get(),
                org.overengineer.bettacraft.config.ClientConfig.TANK_EDGE_WIDTH.get(), org.overengineer.bettacraft.client.interior.PlantMeshRenderer.active());
        if (look.equals(lastTankLook)) return;
        lastTankLook = look;
        mc.execute(() -> {
            if (mc.levelRenderer != null && mc.level != null) mc.levelExtractor.allChanged();
        });
    }

    public BettaCraftClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        // M52: the loader-independent client set-up (hooks, the listeners of the common client events)
        ClientSetup.init();
        // M21.5: the handbook is registered from code (own <Small> tag for the byline on the start page)
        if (org.overengineer.bettacraft.progression.GuideBook.installed()) org.overengineer.bettacraft.compat.guideme.GuideMEClientCompat.register();
        ClientFeatures.jeiRefresh = () -> org.overengineer.bettacraft.compat.jei.BettaCraftJeiPlugin.applyFeatures();
        // M52b: no Create on 1.21.11 (the travelling tank client of compat/create is left out of this branch)
        modBus.addListener(BettaCraftClient::onClientSetup);
        // M52b: the item colours and properties of 1.21.1 as our own tint source and item model property (1.21.4 item models)
        modBus.addListener((RegisterColorHandlersEvent.ItemTintSources e) -> ClientSetup.itemModelTypes(itemModelTypes(e, null, null)));
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent e) -> ClientSetup.itemModelTypes(itemModelTypes(null, e, null)));
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterItemModelsEvent e) -> ClientSetup.itemModelTypes(itemModelTypes(null, null, e)));
        modBus.addListener(BettaCraftClient::onBlockColors);
        modBus.addListener(BettaCraftClient::onLayers);
        modBus.addListener(BettaCraftClient::onRenderers);
        modBus.addListener(BettaCraftClient::onScreens);
        // M25: reverse osmosis and sea water look like water in other mods' tanks (lighter / deeper blue). M52c (26.1): fluid models
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterFluidModelsEvent e) -> {
            e.register(waterLook(0xFFB0DCF4), org.overengineer.bettacraft.registry.ModFluids.RO_WATER.get(), org.overengineer.bettacraft.registry.ModFluids.FLOWING_RO_WATER.get());
            e.register(waterLook(0xFF2A64B0), org.overengineer.bettacraft.registry.ModFluids.SEA_WATER.get(), org.overengineer.bettacraft.registry.ModFluids.FLOWING_SEA_WATER.get());
            e.register(waterLook(0xFF8A9A4A), org.overengineer.bettacraft.registry.ModFluids.LIVE_FOOD.get(), org.overengineer.bettacraft.registry.ModFluids.FLOWING_LIVE_FOOD.get()); // M26: murky green-brown
            e.register(waterLook(0xFF6E8A1E), org.overengineer.bettacraft.registry.ModFluids.FERTILIZER.get(), org.overengineer.bettacraft.registry.ModFluids.FLOWING_FERTILIZER.get()); // M37: olive green
        });
        // M15: how to leave a tank interior, bottom left
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterGuiLayersEvent e) -> e.registerBelow(
                net.neoforged.neoforge.client.gui.VanillaGuiLayers.CHAT, org.overengineer.bettacraft.BettaCraft.id("interior_leave_hint"),
                org.overengineer.bettacraft.client.interior.InteriorLeaveHud::extractRenderState));
        modBus.addListener((net.neoforged.neoforge.client.event.AddClientReloadListenersEvent e) -> e.addListener(BettaCraft.id("client_resources"),
                (net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm -> ClientSetup.onResourcesReloaded()));
        // our dynamic models (cabinets, filter pipes and bundles M40, interior floor, plant meshes M21): client.model.ModelSwaps
        modBus.addListener(NeoForgeClientPlatform::registerStandalone);
        modBus.addListener((ModelEvent.ModifyBakingResult e) -> {
            org.overengineer.bettacraft.client.model.ModelSwaps.beforeBake();
            // M52e: NeoForge 26.3 hands over the stitched block atlas instead of a texture getter
            var atlas = e.getBlockAtlasPreparations();
            java.util.function.Function<net.minecraft.resources.Identifier, net.minecraft.client.renderer.texture.TextureAtlasSprite> sprites = id -> {
                var sprite = atlas.getSprite(id);
                return sprite != null ? sprite : atlas.missing();
            };
            e.getBakingResult().blockStateModels().replaceAll((state, model) -> {
                var swapped = org.overengineer.bettacraft.client.model.ModelSwaps.swap(state, model, sprites);
                return swapped != null ? swapped : model;
            });
        });
        // (M52b: the stone cabinet pairs come with our recipe sync, crafting.RecipeSync, since clients have no recipes in 1.21.2+)
        // M33 part b: leaving a server goes back to the local feature switches
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) -> ClientFeatures.logout());
        // M52: the render stages of the miniatures, plant meshes, jets and the decor preview (ClientSetup.init)
        // (M52b: NeoForge 21.9+ has one event per stage; several of our 1.21.1 stages share one)
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterSky e) -> NeoForgeClientPlatform.fire(e, org.overengineer.bettacraft.platform.client.RenderLevelStageEvent.Stage.AFTER_SKY));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterOpaqueBlocks e) -> NeoForgeClientPlatform.fire(e, org.overengineer.bettacraft.platform.client.RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS, org.overengineer.bettacraft.platform.client.RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS, org.overengineer.bettacraft.platform.client.RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS));
        // M52c (26.1): entities and block entities are drawn as features (solid, then translucent); no tripwire layer any more
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterOpaqueFeatures e) -> NeoForgeClientPlatform.fire(e, org.overengineer.bettacraft.platform.client.RenderLevelStageEvent.Stage.AFTER_ENTITIES, org.overengineer.bettacraft.platform.client.RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterTranslucentBlocks e) -> NeoForgeClientPlatform.fire(e, org.overengineer.bettacraft.platform.client.RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterTranslucentParticles e) -> NeoForgeClientPlatform.fire(e, org.overengineer.bettacraft.platform.client.RenderLevelStageEvent.Stage.AFTER_PARTICLES));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterWeather e) -> NeoForgeClientPlatform.fire(e, org.overengineer.bettacraft.platform.client.RenderLevelStageEvent.Stage.AFTER_WEATHER));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterLevel e) -> NeoForgeClientPlatform.fire(e, org.overengineer.bettacraft.platform.client.RenderLevelStageEvent.Stage.AFTER_LEVEL));
        // M21 follow-up: plant meshes follow their chunks (rebuilt when one arrives again, forgotten when it leaves)
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.ChunkEvent.Load e) -> {
            if (e.getLevel().isClientSide()) org.overengineer.bettacraft.client.interior.PlantMeshRenderer.onChunkLoad(e.getChunk().getPos().x(), e.getChunk().getPos().z());
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.ChunkEvent.Unload e) -> {
            if (e.getLevel().isClientSide()) org.overengineer.bettacraft.client.interior.PlantMeshRenderer.onChunkUnload(e.getChunk().getPos().x(), e.getChunk().getPos().z());
        });
        // M11: tank look settings (connected glass, edge width) are baked into the chunk meshes: rebuild them on change
        modBus.addListener((net.neoforged.fml.event.config.ModConfigEvent.Reloading e) -> {
            if (e.getConfig().getSpec() == org.overengineer.bettacraft.config.ClientConfig.SPEC) rebuildChunks();
            // M21: features.visuals.plantMeshes switches the plant blocks of the interior between blocks and meshes
            if (e.getConfig().getSpec() == org.overengineer.bettacraft.config.CommonConfig.SPEC) {
                rebuildChunks();
                org.overengineer.bettacraft.client.interior.MiniatureSway.rebuildAll(); // M22.1: which blocks sway
            }
        });
        modBus.addListener((net.neoforged.fml.event.config.ModConfigEvent.Loading e) -> {
            if (e.getConfig().getSpec() == org.overengineer.bettacraft.config.CommonConfig.SPEC) org.overengineer.bettacraft.client.interior.MiniatureSway.reset();
        });
        // M22.1: the tag of the swaying water plants may change with a data pack
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.TagsUpdatedEvent e) -> {
            if (e instanceof net.neoforged.neoforge.event.TagsUpdatedEvent.ClientPacketReceived) {
                org.overengineer.bettacraft.client.interior.MiniatureSway.rebuildAll();
            }
        });
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent e) -> ClientSetup.particles(new ClientSetup.Particles() {
            @Override
            public <T extends net.minecraft.core.particles.ParticleOptions> void registerSpriteSet(net.minecraft.core.particles.ParticleType<T> type,
                    java.util.function.Function<net.minecraft.client.particle.SpriteSet, net.minecraft.client.particle.ParticleProvider<T>> factory) {
                e.registerSpriteSet(type, factory::apply);
            }
        }));
        // M52: the common client events (ClientEvents) from NeoForge's
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> org.overengineer.bettacraft.platform.client.ClientEvents.fireTickPost());
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ScreenEvent.Opening e) ->
                org.overengineer.bettacraft.platform.client.ClientEvents.fireScreenOpening(e.getNewScreen()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent e) -> {
            if (org.overengineer.bettacraft.platform.client.ClientEvents.fireScroll(e.getScrollDeltaX(), e.getScrollDeltaY())) e.setCanceled(true);
        });
        org.overengineer.bettacraft.client.SoundTest.init(); // M29.1: /bettacraft sounds plays every device sound
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.ItemTooltipEvent e) ->
                org.overengineer.bettacraft.platform.client.ClientEvents.fireTooltip(e.getItemStack(), e.getContext(), e.getToolTip(), e.getFlags()));

        // M50: the sloshing water of travelling tanks (one step per frame; the blocks leave their water out)
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RenderFrameEvent.Pre e) -> org.overengineer.bettacraft.client.render.TravelWater.nextFrame());
        // M18: colour of the tank lights for the water (their shader light colour: IrisBlockIdsMixin)
        // M17: block entities of a dropped client level (dimension change, interior, disconnect) leave the static sets
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.LevelEvent.Unload e) -> {
            if (e.getLevel().isClientSide()) org.overengineer.bettacraft.util.ClientBlockEntitySets.purgeLevel(e.getLevel());
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> ClientSetup.onLogout());
        // M22: the client smoke tests, the showcase tour and the multiplayer test client live in the dev source set
        // (src/dev), which is not part of the released jar; DevClientHooks is only found in development runs
        if (!!org.overengineer.bettacraft.platform.Platform.get().isDevelopment()) {
            org.overengineer.bettacraft.util.DevHooks.init("org.overengineer.bettacraft.client.DevClientHooks");
        }
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        // M52c (26.1): the water of tank blocks comes from the common FluidRendererMixin
        event.enqueueWork(() -> ClientSetup.itemProperties(org.overengineer.bettacraft.client.item.ItemPredicates::register));
        event.enqueueWork(() -> ClientSetup.itemColors(org.overengineer.bettacraft.client.item.ItemTints::register));
    }

    /** M52b: our item model types into whichever of NeoForge's three registration events fired. */
    private static ClientSetup.ItemModelTypes itemModelTypes(RegisterColorHandlersEvent.ItemTintSources tints,
            net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent properties,
            net.neoforged.neoforge.client.event.RegisterItemModelsEvent models) {
        return new ClientSetup.ItemModelTypes() {
            @Override
            public void tintSource(net.minecraft.resources.Identifier id, com.mojang.serialization.MapCodec<? extends net.minecraft.client.color.item.ItemTintSource> codec) {
                if (tints != null) tints.register(id, codec);
            }

            @Override
            public void conditionalProperty(net.minecraft.resources.Identifier id,
                    com.mojang.serialization.MapCodec<? extends net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty> codec) {
                if (properties != null) properties.register(id, codec);
            }

            @Override
            public void itemModel(net.minecraft.resources.Identifier id, com.mojang.serialization.MapCodec<? extends net.minecraft.client.renderer.item.ItemModel.Unbaked> codec) {
                if (models != null) models.register(id, codec);
            }
        };
    }

    /** M9: water in the larva tub (murky green) and the artemia hatchery (salt water blue). */
    /** M26: the stale brown water of the tubifex dish and the green water of the daphnia barrel. */
    private static void onBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
        // M52c (26.1): one tint source per tint index
        ClientSetup.blockColors((color, blocks) -> event.register(ClientSetup.tintSources(color), blocks));
    }

    private static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        ClientSetup.layers(event::registerLayerDefinition);
    }

    private static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ClientSetup.renderers(new ClientSetup.Renderers() {
            @Override
            public <T extends net.minecraft.world.entity.Entity> void registerEntityRenderer(net.minecraft.world.entity.EntityType<? extends T> type,
                    net.minecraft.client.renderer.entity.EntityRendererProvider<T> provider) {
                event.registerEntityRenderer(type, provider);
            }

            @Override
            public <T extends net.minecraft.world.level.block.entity.BlockEntity, S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState> void registerBlockEntityRenderer(
                    net.minecraft.world.level.block.entity.BlockEntityType<? extends T> type,
                    net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider<T, S> provider) {
                event.registerBlockEntityRenderer(type, provider);
            }
        });
    }

    private static void onScreens(RegisterMenuScreensEvent event) {
        ClientSetup.screens(new ClientSetup.Screens() {
            @Override
            public <M extends net.minecraft.world.inventory.AbstractContainerMenu, U extends net.minecraft.client.gui.screens.Screen & net.minecraft.client.gui.screens.inventory.MenuAccess<M>> void register(
                    net.minecraft.world.inventory.MenuType<? extends M> type, net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor<M, U> factory) {
                event.register(type, factory);
            }
        });
    }

    /** M25: the look of our water fluids: Minecraft's water textures in a tint. */
    private static net.minecraft.client.renderer.block.FluidModel.Unbaked waterLook(int tint) {
        return new net.minecraft.client.renderer.block.FluidModel.Unbaked(
                new net.minecraft.client.resources.model.sprite.Material(net.minecraft.resources.Identifier.withDefaultNamespace("block/water_still")),
                new net.minecraft.client.resources.model.sprite.Material(net.minecraft.resources.Identifier.withDefaultNamespace("block/water_flow")),
                new net.minecraft.client.resources.model.sprite.Material(net.minecraft.resources.Identifier.withDefaultNamespace("block/water_overlay")),
                net.neoforged.neoforge.client.fluid.FluidTintSources.constant(tint));
    }

}
