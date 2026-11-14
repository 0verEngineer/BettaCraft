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
        // M21.5: the handbook is registered from code (own <Small> tag for the byline on the start page)
        if (org.overengineer.bettacraft.progression.GuideBook.available()) org.overengineer.bettacraft.compat.guideme.GuideMEClientCompat.register();
        modBus.addListener(BettaCraftClient::onClientSetup);
        modBus.addListener(BettaCraftClient::onRegisterGeometryLoaders);
        modBus.addListener(BettaCraftClient::onItemColors);
        modBus.addListener(BettaCraftClient::onBlockColors);
        modBus.addListener(BettaCraftClient::onLayers);
        modBus.addListener(BettaCraftClient::onRenderers);
        modBus.addListener(BettaCraftClient::onScreens);
        // M15: how to leave a tank interior, bottom left
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterGuiLayersEvent e) -> e.registerBelow(
                net.neoforged.neoforge.client.gui.VanillaGuiLayers.CHAT, org.overengineer.bettacraft.BettaCraft.id("interior_leave_hint"),
                org.overengineer.bettacraft.client.interior.InteriorLeaveHud::render));
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent e) -> e.registerReloadListener(
                (net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm -> {
                    org.overengineer.bettacraft.client.render.BettaSkins.clear();
                    org.overengineer.bettacraft.client.render.CritterRenderer.reset(); // M14: baked again on the next frame
                }));
        modBus.addListener(org.overengineer.bettacraft.client.interior.SubstrateFloorModel::onModifyBakingResult);
        modBus.addListener(org.overengineer.bettacraft.client.interior.SubstrateFloorModel::onBlockColors);
        modBus.addListener(org.overengineer.bettacraft.client.model.CabinetBakedModel::onModifyBakingResult);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RecipesUpdatedEvent e) -> org.overengineer.bettacraft.equipment.CabinetStones.clear());
        // M21: plant prototype (Rotala, Vallisneria, Anubias with real leaves in the interior)
        modBus.addListener(org.overengineer.bettacraft.client.interior.PlantMeshModel::onModifyBakingResult);
        NeoForge.EVENT_BUS.addListener(org.overengineer.bettacraft.client.interior.PlantMeshRenderer::onRenderStage);
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
            // M21: features.plantMeshes switches the plant blocks of the interior between blocks and meshes
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
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> FilterEffects.tick());
        NeoForge.EVENT_BUS.addListener(ItemHintTooltips::onTooltip);

        NeoForge.EVENT_BUS.addListener(MiniatureRenderer::onRenderStage);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> ClientInteriorCache.tick());
        // M18: colour of the tank lights for the water (their shader light colour: IrisBlockIdsMixin)
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> org.overengineer.bettacraft.client.tank.TankLightTint.tick());
        NeoForge.EVENT_BUS.addListener(org.overengineer.bettacraft.client.decor.DecorPreview::onRenderStage);
        // M17: block entities of a dropped client level (dimension change, interior, disconnect) leave the static sets
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.LevelEvent.Unload e) -> {
            if (e.getLevel().isClientSide()) org.overengineer.bettacraft.util.ClientBlockEntitySets.purgeLevel(e.getLevel());
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            org.overengineer.bettacraft.util.ClientBlockEntitySets.purgeLevel(null);
            ClientInteriorCache.clear();
            org.overengineer.bettacraft.client.interior.PlantMeshRenderer.clear();
            org.overengineer.bettacraft.client.decor.DecorPreview.clear();
            org.overengineer.bettacraft.food.ArtemiaCystsItem.setClientOverride(0); // M21.8: the next server sends its own
            // back to the built-in genetics (the next server sends its own); not while our integrated server still runs
            if (net.minecraft.client.Minecraft.getInstance().getSingleplayerServer() == null) {
                org.overengineer.bettacraft.genetics.GeneticsData.reset();
            }
        });
        // M22: the client smoke tests, the showcase tour and the multiplayer test client live in the dev source set
        // (src/dev), which is not part of the released jar; DevClientHooks is only found in development runs
        if (!net.neoforged.fml.loading.FMLEnvironment.production) {
            org.overengineer.bettacraft.util.DevHooks.init("org.overengineer.bettacraft.client.DevClientHooks");
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
            if (tint == 0 || tint >= 3) return 0xFFFFFFFF; // container and (M21) the glass in front of the fish
            // M14: a bag of shrimp, crayfish or snails shows the colour of the first one
            var critters = stack.get(ModDataComponents.CRITTERS.get());
            if (critters != null && !critters.isEmpty()) {
                var k = org.overengineer.bettacraft.critters.Critter.loadAnimal(critters.getFirst(), 0, 0, 0, 0);
                int rgb = k == null ? 0x8A8070 : k.genes.rgb(k.species);
                return 0xFF000000 | (tint == 1 ? rgb : org.overengineer.bettacraft.critters.CritterGenes.mix(rgb, 0xFFFFFF, 0.3F));
            }
            // M19: a bag of tankmates in the species' colours (a clutch: cream eggs)
            if (stack.has(ModDataComponents.TANKMATE_CLUTCH.get())) return tint == 1 ? 0xFFEFE4C4 : 0xFFD8C8A0;
            var tm = org.overengineer.bettacraft.tankmates.TankmateItems.bagSpecies(stack);
            if (tm != null) return 0xFF000000 | (tint == 1 ? tm.eggColor1() : tm.eggColor2());
            BettaData d = stack.getItem() instanceof BettaBucketItem ? BettaBucketItem.data(stack) : stack.get(ModDataComponents.FISH_DATA.get());
            if (d == null) return tint == 1 ? OTHER_FISH_BODY : OTHER_FISH_FINS;
            return tint == 1 ? BettaTints.body(d.genome(), d.female(), false) : BettaTints.fins(d.genome(), d.female(), false);
        }, ModItems.BETTA_BUCKET.get(), ModItems.FISH_BAG.get());
        // M19: tankmate buckets in the species' colours
        for (var e : ModItems.TANKMATE_BUCKETS.entrySet()) {
            var sp = e.getKey();
            event.register((stack, tint) -> tint == 0 || tint >= 3 ? 0xFFFFFFFF : 0xFF000000 | (tint == 1 ? sp.eggColor1() : sp.eggColor2()), e.getValue().get());
        }
        event.register((stack, tint) -> tint == 0 ? LARVA_WATER : 0xFFFFFFFF, ModItems.LARVA_TUB.get());
        // M13: aquarium plants: leaves (layer 0) and rhizome / stem (layer 1) in the colours of the species (red ones as in good light)
        for (var e : ModItems.AQUATIC_PLANTS.entrySet()) {
            var s = e.getKey();
            event.register((stack, tint) -> 0xFF000000 | org.overengineer.bettacraft.plants.PlantColors.color(s,
                    tint == 0 ? org.overengineer.bettacraft.plants.PlantPart.LEAF
                            : org.overengineer.bettacraft.plants.PlantColors.rootLooksLike(s.form()) == org.overengineer.bettacraft.plants.PlantPart.STEM
                            ? org.overengineer.bettacraft.plants.PlantPart.STEM : org.overengineer.bettacraft.plants.PlantPart.RHIZOME, 2), e.getValue().get());
        }
        event.register((stack, tint) -> tint == 0 ? HATCHERY_WATER : 0xFFFFFFFF, ModItems.ARTEMIA_HATCHERY.get());
        // M18: the panel of the aquarium light in its colour
        event.register((stack, tint) -> tint == 1 ? 0xFF000000 | org.overengineer.bettacraft.equipment.TankLightColors.panel(
                org.overengineer.bettacraft.equipment.TankLightItem.colorOf(stack), true) : 0xFFFFFFFF, ModItems.TANK_LIGHT.get());
    }

    /** M9: water in the larva tub (murky green) and the artemia hatchery (salt water blue). */
    static final int LARVA_WATER = 0xFF6A7C3A, HATCHERY_WATER = 0xFF4E93C8;

    private static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> tint == 0 ? LARVA_WATER : 0xFFFFFFFF, org.overengineer.bettacraft.registry.ModBlocks.LARVA_TUB.get());
        // M13: aquarium plants: grey textures tinted per species, part and redness (also in the miniature)
        for (var b : org.overengineer.bettacraft.registry.ModBlocks.AQUATIC_PLANTS.values()) {
            event.register((state, level, pos, tint) -> {
                if (tint != 0 || !(state.getBlock() instanceof org.overengineer.bettacraft.plants.AquaticPlantBlock p)) return 0xFFFFFFFF;
                return 0xFF000000 | org.overengineer.bettacraft.plants.PlantColors.color(p.species(), state.getValue(org.overengineer.bettacraft.plants.AquaticPlantBlock.PART),
                        state.getValue(org.overengineer.bettacraft.plants.AquaticPlantBlock.TINT));
            }, b.get());
        }
        event.register((state, level, pos, tint) -> tint == 0 ? HATCHERY_WATER : 0xFFFFFFFF, org.overengineer.bettacraft.registry.ModBlocks.ARTEMIA_HATCHERY.get());
        // M18: the panel of the aquarium light in its colour (darker while off)
        event.register((state, level, pos, tint) -> tint == 0 ? 0xFF000000 | org.overengineer.bettacraft.equipment.TankLightColors.panel(
                state.getValue(org.overengineer.bettacraft.equipment.TankLightBlock.COLOR), state.getValue(org.overengineer.bettacraft.equipment.TankLightBlock.LIT))
                : 0xFFFFFFFF, org.overengineer.bettacraft.registry.ModBlocks.TANK_LIGHT.get());
    }

    private static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (BettaModelDefinitions.Shape s : BettaModelDefinitions.Shape.values()) {
            event.registerLayerDefinition(BettaRenderer.LAYERS.get(s), () -> BettaModelDefinitions.createBodyLayer(s));
        }
        // M14: shrimp, crayfish, snails (drawn by the tank) and the dwarf clawed frog
        for (var m : org.overengineer.bettacraft.critters.CritterSpecies.Model.values()) {
            event.registerLayerDefinition(org.overengineer.bettacraft.client.render.CritterRenderer.LAYERS.get(m),
                    () -> org.overengineer.bettacraft.client.model.CritterModelDefinitions.create(org.overengineer.bettacraft.client.render.CritterRenderer.name(m)));
        }
        event.registerLayerDefinition(org.overengineer.bettacraft.client.render.CritterRenderer.FROG_LAYER,
                org.overengineer.bettacraft.client.model.CritterModelDefinitions::createFrogLayer);
        // M19: one mesh per tankmate body plan
        for (var e : org.overengineer.bettacraft.client.render.TankmateRenderer.LAYERS.entrySet()) {
            String model = org.overengineer.bettacraft.client.render.TankmateRenderer.name(e.getKey());
            event.registerLayerDefinition(e.getValue(), () -> org.overengineer.bettacraft.client.model.TankmateModelDefinitions.create(model));
        }
        // M21.6: the guppy and endler model of the livebearer genetics
        event.registerLayerDefinition(org.overengineer.bettacraft.client.render.TankmateRenderer.GUPPY_LAYER,
                org.overengineer.bettacraft.client.model.GuppyModelDefinitions::createLayer);
    }

    private static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BETTA.get(), BettaRenderer::new);
        event.registerEntityRenderer(ModEntities.DWARF_FROG.get(), org.overengineer.bettacraft.client.render.DwarfFrogRenderer::new);
        for (var t : ModEntities.TANKMATES.values()) event.registerEntityRenderer(t.get(), org.overengineer.bettacraft.client.render.TankmateRenderer::new); // M19
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
