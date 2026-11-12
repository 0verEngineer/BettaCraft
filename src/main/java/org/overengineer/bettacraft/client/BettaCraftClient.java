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
    private static final int OTHER_FISH_BODY = 0xFFB0B8C0, OTHER_FISH_FINS = 0xFF8A949E;

    public BettaCraftClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(BettaCraftClient::onClientSetup);
        modBus.addListener(BettaCraftClient::onRegisterGeometryLoaders);
        modBus.addListener(BettaCraftClient::onItemColors);
        modBus.addListener(BettaCraftClient::onLayers);
        modBus.addListener(BettaCraftClient::onRenderers);
        modBus.addListener(BettaCraftClient::onScreens);
        modBus.addListener(org.overengineer.bettacraft.client.interior.SubstrateFloorModel::onModifyBakingResult);
        modBus.addListener(org.overengineer.bettacraft.client.interior.SubstrateFloorModel::onBlockColors);
        modBus.addListener(org.overengineer.bettacraft.client.model.CabinetBakedModel::onModifyBakingResult);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> FilterEffects.tick());

        NeoForge.EVENT_BUS.addListener(MiniatureRenderer::onRenderStage);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> ClientInteriorCache.tick());
        NeoForge.EVENT_BUS.addListener(org.overengineer.bettacraft.client.decor.DecorPreview::onRenderStage);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            ClientInteriorCache.clear();
            org.overengineer.bettacraft.client.decor.DecorPreview.clear();
            // back to the built-in genetics (the next server sends its own); not while our integrated server still runs
            if (net.minecraft.client.Minecraft.getInstance().getSingleplayerServer() == null) {
                org.overengineer.bettacraft.genetics.GeneticsData.reset();
            }
        });
        if (Boolean.getBoolean("bettacraft.smoketest.world")) {
            DevClientSmokeTest.register();
        } else if (Boolean.getBoolean("bettacraft.smoketest")) {
            // dev only: quit as soon as the title screen opens (models/textures baked without crash)
            NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ScreenEvent.Opening e) -> {
                if (e.getNewScreen() instanceof net.minecraft.client.gui.screens.TitleScreen) {
                    org.slf4j.LoggerFactory.getLogger("bettacraft/smoketest").info("[smoketest] client reached title screen");
                    net.minecraft.client.Minecraft.getInstance().stop();
                }
            });
        }
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        // Client extensions were registered during Minecraft construction (before this event); wrap vanilla water now.
        event.enqueueWork(TankAwareWaterExtensions::install);
        event.enqueueWork(() -> ItemProperties.register(ModItems.FISH_BAG.get(), BettaCraft.id("filled"),
                (stack, level, entity, seed) -> FishBagItem.isFilled(stack) ? 1.0F : 0.0F));
    }

    /** Fish overlay of bucket and bag in the fish's own colours (tint 1 = body, 2 = fins). */
    private static void onItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> {
            if (tint == 0) return 0xFFFFFFFF;
            BettaData d = stack.getItem() instanceof BettaBucketItem ? BettaBucketItem.data(stack) : stack.get(ModDataComponents.FISH_DATA.get());
            if (d == null) return tint == 1 ? OTHER_FISH_BODY : OTHER_FISH_FINS;
            return tint == 1 ? BettaTints.body(d.genome(), d.female(), false) : BettaTints.fins(d.genome(), d.female(), false);
        }, ModItems.BETTA_BUCKET.get(), ModItems.FISH_BAG.get());
    }

    private static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (BettaModelDefinitions.Shape s : BettaModelDefinitions.Shape.values()) {
            event.registerLayerDefinition(BettaRenderer.LAYERS.get(s), () -> BettaModelDefinitions.createBodyLayer(s));
        }
    }

    private static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BETTA.get(), BettaRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.TANK.get(), TankSignRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.EXTERNAL_FILTER.get(), org.overengineer.bettacraft.client.render.FilterRenderer::forFilter);
        event.registerBlockEntityRenderer(ModBlockEntities.CABINET.get(), org.overengineer.bettacraft.client.render.FilterRenderer::forCabinet);
    }

    private static void onScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.TANK.get(), TankScreen::new);
        event.register(ModMenus.FEEDER.get(), FeederScreen::new);
        event.register(ModMenus.EXTERNAL_FILTER.get(), org.overengineer.bettacraft.client.screen.ExternalFilterScreen::new);
        event.register(ModMenus.CABINET.get(), org.overengineer.bettacraft.client.screen.CabinetScreen::new);
    }

    private static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(TankModelLoader.ID, TankModelLoader.INSTANCE);
    }
}
