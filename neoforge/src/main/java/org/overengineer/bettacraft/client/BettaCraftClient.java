package org.overengineer.bettacraft.client;

import net.minecraft.client.renderer.item.ItemProperties;
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
import org.overengineer.bettacraft.client.tank.TankAwareWaterExtensions;
import org.overengineer.bettacraft.client.tank.TankModelLoader;
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
            if (mc.levelRenderer != null && mc.level != null) mc.levelRenderer.allChanged();
        });
    }

    public BettaCraftClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        // M52: the loader-independent client set-up (hooks, the listeners of the common client events)
        ClientSetup.init();
        // M21.5: the handbook is registered from code (own <Small> tag for the byline on the start page)
        if (org.overengineer.bettacraft.progression.GuideBook.installed()) org.overengineer.bettacraft.compat.guideme.GuideMEClientCompat.register();
        ClientFeatures.jeiRefresh = () -> org.overengineer.bettacraft.compat.jei.BettaCraftJeiPlugin.applyFeatures();
        if (org.overengineer.bettacraft.compat.create.CreateCompat.loaded()) {
            org.overengineer.bettacraft.compat.create.CreateCompat.travelClient = org.overengineer.bettacraft.compat.create.CreateTravelClient::accept;
        }
        modBus.addListener(BettaCraftClient::onClientSetup);
        modBus.addListener(BettaCraftClient::onRegisterGeometryLoaders);
        modBus.addListener(BettaCraftClient::onItemColors);
        modBus.addListener(BettaCraftClient::onBlockColors);
        modBus.addListener(BettaCraftClient::onLayers);
        modBus.addListener(BettaCraftClient::onRenderers);
        modBus.addListener(BettaCraftClient::onScreens);
        // M25: reverse osmosis and sea water look like water in other mods' tanks (lighter / deeper blue)
        modBus.addListener((net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e) -> {
            e.registerFluidType(waterLook(0xFFB0DCF4), org.overengineer.bettacraft.registry.NeoForgeFluids.RO_WATER_TYPE.get());
            e.registerFluidType(waterLook(0xFF2A64B0), org.overengineer.bettacraft.registry.NeoForgeFluids.SEA_WATER_TYPE.get());
            e.registerFluidType(waterLook(0xFF8A9A4A), org.overengineer.bettacraft.registry.NeoForgeFluids.LIVE_FOOD_TYPE.get()); // M26: murky green-brown
            e.registerFluidType(waterLook(0xFF6E8A1E), org.overengineer.bettacraft.registry.NeoForgeFluids.FERTILIZER_TYPE.get()); // M37: olive green
        });
        // M15: how to leave a tank interior, bottom left
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterGuiLayersEvent e) -> e.registerBelow(
                net.neoforged.neoforge.client.gui.VanillaGuiLayers.CHAT, org.overengineer.bettacraft.BettaCraft.id("interior_leave_hint"),
                org.overengineer.bettacraft.client.interior.InteriorLeaveHud::render));
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent e) -> e.registerReloadListener(
                (net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm -> ClientSetup.onResourcesReloaded()));
        // our dynamic models (cabinets, filter pipes and bundles M40, interior floor, plant meshes M21): client.model.ModelSwaps
        modBus.addListener((ModelEvent.RegisterAdditional e) -> {
            for (var id : org.overengineer.bettacraft.client.model.ModelSwaps.extraModels())
                e.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(id));
        });
        modBus.addListener((ModelEvent.ModifyBakingResult e) -> {
            org.overengineer.bettacraft.client.model.ModelSwaps.beforeBake();
            for (var entry : e.getModels().entrySet()) {
                var swapped = org.overengineer.bettacraft.client.model.ModelSwaps.swap(entry.getKey(), entry.getValue());
                if (swapped != null) entry.setValue(swapped);
            }
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RecipesUpdatedEvent e) -> org.overengineer.bettacraft.equipment.CabinetStones.clear());
        // M33 part b: leaving a server goes back to the local feature switches
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) -> ClientFeatures.logout());
        // M52: the render stages of the miniatures, plant meshes, jets and the decor preview (ClientSetup.init)
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RenderLevelStageEvent e) ->
                org.overengineer.bettacraft.platform.client.ClientEvents.fireRenderLevel(NeoForgeClientPlatform.wrap(e)));
        // M21 follow-up: plant meshes follow their chunks (rebuilt when one arrives again, forgotten when it leaves)
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.ChunkEvent.Load e) -> {
            if (e.getLevel().isClientSide()) org.overengineer.bettacraft.client.interior.PlantMeshRenderer.onChunkLoad(e.getChunk().getPos().x, e.getChunk().getPos().z);
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.ChunkEvent.Unload e) -> {
            if (e.getLevel().isClientSide()) org.overengineer.bettacraft.client.interior.PlantMeshRenderer.onChunkUnload(e.getChunk().getPos().x, e.getChunk().getPos().z);
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
            if (e.getUpdateCause() == net.neoforged.neoforge.event.TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED) {
                org.overengineer.bettacraft.client.interior.MiniatureSway.rebuildAll();
            }
        });
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent e) -> ClientSetup.particles(e::registerSpriteSet));
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
        // Client extensions were registered during Minecraft construction (before this event); wrap vanilla water now.
        event.enqueueWork(TankAwareWaterExtensions::install);
        event.enqueueWork(() -> ClientSetup.itemProperties(ItemProperties::register));
    }

    /** Fish overlay of bucket and bag in the fish's own colours (tint 1 = body, 2 = fins). */
    private static void onItemColors(RegisterColorHandlersEvent.Item event) {
        ClientSetup.itemColors(event::register);
    }

    /** M9: water in the larva tub (murky green) and the artemia hatchery (salt water blue). */
    /** M26: the stale brown water of the tubifex dish and the green water of the daphnia barrel. */
    private static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        ClientSetup.blockColors(event::register);
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
            public <T extends net.minecraft.world.level.block.entity.BlockEntity> void registerBlockEntityRenderer(
                    net.minecraft.world.level.block.entity.BlockEntityType<? extends T> type,
                    net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider<T> provider) {
                event.registerBlockEntityRenderer(type, provider);
            }
        });
        // M41: the drive adapter's shaft (the block exists only with Create)
        if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded(org.overengineer.bettacraft.compat.create.CreateCompat.MOD_ID)) {
            org.overengineer.bettacraft.compat.create.DriveAdapterRenderer.register(event);
        }
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

    private static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(TankModelLoader.ID, TankModelLoader.INSTANCE);
    }

    /** M25: the look of our water fluids: Minecraft's water textures in a tint. */
    private static net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions waterLook(int tint) {
        return new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
            @Override
            public net.minecraft.resources.Identifier getStillTexture() {
                return net.minecraft.resources.Identifier.withDefaultNamespace("block/water_still");
            }

            @Override
            public net.minecraft.resources.Identifier getFlowingTexture() {
                return net.minecraft.resources.Identifier.withDefaultNamespace("block/water_flow");
            }

            @Override
            public int getTintColor() {
                return tint;
            }
        };
    }

}
