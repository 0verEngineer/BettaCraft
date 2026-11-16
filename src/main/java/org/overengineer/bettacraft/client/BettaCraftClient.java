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
        // M22.1: the question before animals leave a bucket or a bag into the world
        org.overengineer.bettacraft.containers.ContainerRules.confirmScreen = org.overengineer.bettacraft.client.screen.ConfirmEmptyScreen::open;
        // M21.5: the handbook is registered from code (own <Small> tag for the byline on the start page)
        if (org.overengineer.bettacraft.progression.GuideBook.installed()) org.overengineer.bettacraft.compat.guideme.GuideMEClientCompat.register();
        modBus.addListener(BettaCraftClient::onClientSetup);
        modBus.addListener(BettaCraftClient::onRegisterGeometryLoaders);
        modBus.addListener(BettaCraftClient::onItemColors);
        modBus.addListener(BettaCraftClient::onBlockColors);
        modBus.addListener(BettaCraftClient::onLayers);
        modBus.addListener(BettaCraftClient::onRenderers);
        modBus.addListener(BettaCraftClient::onScreens);
        // M25: reverse osmosis and sea water look like water in other mods' tanks (lighter / deeper blue)
        modBus.addListener((net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e) -> {
            e.registerFluidType(waterLook(0xFFB0DCF4), org.overengineer.bettacraft.registry.ModFluids.RO_WATER_TYPE.get());
            e.registerFluidType(waterLook(0xFF2A64B0), org.overengineer.bettacraft.registry.ModFluids.SEA_WATER_TYPE.get());
            e.registerFluidType(waterLook(0xFF8A9A4A), org.overengineer.bettacraft.registry.ModFluids.LIVE_FOOD_TYPE.get()); // M26: murky green-brown
            e.registerFluidType(waterLook(0xFF6E8A1E), org.overengineer.bettacraft.registry.ModFluids.FERTILIZER_TYPE.get()); // M37: olive green
        });
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
        modBus.addListener(org.overengineer.bettacraft.client.model.FilterPipeModel::onModifyBakingResult); // M40
        modBus.addListener(org.overengineer.bettacraft.client.model.FilterBundleModel::onRegisterAdditional); // M40 part b: the pipe bundle
        modBus.addListener(org.overengineer.bettacraft.client.model.FilterBundleModel::onModifyBakingResult);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RecipesUpdatedEvent e) -> org.overengineer.bettacraft.equipment.CabinetStones.clear());
        // M33 part b: leaving a server goes back to the local feature switches
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) -> ClientFeatures.logout());
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
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> FilterEffects.tick());
        org.overengineer.bettacraft.client.DeviceEffects.init(modBus); // M23: air pumps, air stones, regulator, particles
        org.overengineer.bettacraft.client.SoundTest.init(); // M29.1: /bettacraft sounds plays every device sound
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
            org.overengineer.bettacraft.client.map.AlarmWaypoints.clear(); // M32 part d
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
        event.enqueueWork(() -> ItemProperties.register(ModItems.TRANSPORT_TUB.get(), BettaCraft.id("filled"), // M30
                (stack, level, entity, seed) -> org.overengineer.bettacraft.big.TransportTubItem.isFilled(stack) ? 1.0F : 0.0F));
        event.enqueueWork(() -> ItemProperties.register(ModItems.MOB_JAR.get(), BettaCraft.id("filled"), // M31 part d
                (stack, level, entity, seed) -> org.overengineer.bettacraft.mobs.MobJarItem.isFilled(stack) ? 1.0F : 0.0F));
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
        // M22.3: the critter bucket shows the first animal's colour (layer 1 body, layer 2 legs and feelers)
        event.register((stack, tint) -> {
            if (tint == 0 || tint >= 3) return 0xFFFFFFFF;
            var critters = stack.get(ModDataComponents.CRITTERS.get());
            var k = critters == null || critters.isEmpty() ? null : org.overengineer.bettacraft.critters.Critter.loadAnimal(critters.getFirst(), 0, 0, 0, 0);
            int rgb = k == null ? 0xC8A070 : k.genes.rgb(k.species);
            if (k != null && (k.species.nerite() || k.species == org.overengineer.bettacraft.critters.CritterSpecies.ASSASSIN_SNAIL)) rgb = k.species.wildColor();
            return 0xFF000000 | (tint == 1 ? rgb : org.overengineer.bettacraft.critters.CritterGenes.mix(rgb, 0xFFFFFF, 0.35F));
        }, ModItems.CRITTER_BUCKET.get());
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
        event.register((stack, tint) -> tint == 0 ? TUBIFEX_WATER : 0xFFFFFFFF, ModItems.TUBIFEX_DISH.get()); // M26
        event.register((stack, tint) -> tint == 0 ? DAPHNIA_WATER : 0xFFFFFFFF, ModItems.DAPHNIA_BARREL.get());
        // M18: the panel of the aquarium light in its colour (M29.1: the 3D item tints the panel like the block, tint 0)
        event.register((stack, tint) -> tint == 0 ? 0xFF000000 | org.overengineer.bettacraft.equipment.TankLightColors.panel(
                org.overengineer.bettacraft.equipment.TankLightItem.colorOf(stack), true) : 0xFFFFFFFF, ModItems.TANK_LIGHT.get(),
                ModItems.LED_PANEL.get(), ModItems.HIGH_POWER_LED.get()); // M23: every light tier
    }

    /** M9: water in the larva tub (murky green) and the artemia hatchery (salt water blue). */
    static final int LARVA_WATER = 0xFF6A7C3A, HATCHERY_WATER = 0xFF4E93C8;
    /** M26: the stale brown water of the tubifex dish and the green water of the daphnia barrel. */
    static final int TUBIFEX_WATER = 0xFF6E5A3A, DAPHNIA_WATER = 0xFF5E8A3E;

    private static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        // M40 part c: a covered filter pipe or bundle is tinted like its cover (grass, leaves)
        event.register((state, level, pos, tint) -> {
            if (level == null || pos == null) return -1;
            var c = org.overengineer.bettacraft.plumbing.PipeCovers.coverAt(level, pos);
            return c == null ? -1 : net.minecraft.client.Minecraft.getInstance().getBlockColors().getColor(c, level, pos, tint);
        }, org.overengineer.bettacraft.registry.ModBlocks.FILTER_INLET_PIPE.get(), org.overengineer.bettacraft.registry.ModBlocks.FILTER_OUTLET_PIPE.get(),
                org.overengineer.bettacraft.registry.ModBlocks.FILTER_FRESH_PIPE.get(), org.overengineer.bettacraft.registry.ModBlocks.FILTER_WASTE_PIPE.get(),
                org.overengineer.bettacraft.registry.ModBlocks.FILTER_PIPE_BUNDLE.get());
        event.register((state, level, pos, tint) -> tint == 0 ? LARVA_WATER : 0xFFFFFFFF, org.overengineer.bettacraft.registry.ModBlocks.LARVA_TUB.get());
        // M13: aquarium plants: grey textures tinted per species, part and redness (also in the miniature)
        for (var b : org.overengineer.bettacraft.registry.ModBlocks.AQUATIC_PLANTS.values()) {
            event.register((state, level, pos, tint) -> {
                if (tint != 0 || !(state.getBlock() instanceof org.overengineer.bettacraft.plants.AquaticPlantBlock p)) return 0xFFFFFFFF;
                return 0xFF000000 | org.overengineer.bettacraft.plants.PlantColors.color(p.species(), state.getValue(org.overengineer.bettacraft.plants.AquaticPlantBlock.PART),
                        state.getValue(org.overengineer.bettacraft.plants.AquaticPlantBlock.TINT));
            }, b.get());
        }
        // M28.1: the wild plants: leaves (0), stalks / stems / rhizome (1) and young leaves (2) in the colours of the species
        for (var b : org.overengineer.bettacraft.registry.ModBlocks.WILD_PLANTS.values()) {
            event.register((state, level, pos, tint) -> {
                if (!(state.getBlock() instanceof org.overengineer.bettacraft.plants.wild.WildPlantBlock w)) return 0xFFFFFFFF;
                var s = w.species(state);
                var part = switch (tint) {
                    case 0 -> org.overengineer.bettacraft.plants.PlantPart.LEAF;
                    case 1 -> w.kind() == org.overengineer.bettacraft.plants.wild.WildPlantKind.EPIPHYTE
                            ? org.overengineer.bettacraft.plants.PlantPart.RHIZOME : org.overengineer.bettacraft.plants.PlantPart.STEM;
                    default -> org.overengineer.bettacraft.plants.PlantPart.TOP;
                };
                return 0xFF000000 | org.overengineer.bettacraft.plants.PlantColors.color(s, part, org.overengineer.bettacraft.plants.wild.WildPlantBlock.WILD_TINT);
            }, b.get());
        }
        event.register((state, level, pos, tint) -> tint == 0 ? HATCHERY_WATER : 0xFFFFFFFF, org.overengineer.bettacraft.registry.ModBlocks.ARTEMIA_HATCHERY.get());
        event.register((state, level, pos, tint) -> tint == 0 ? TUBIFEX_WATER : 0xFFFFFFFF, org.overengineer.bettacraft.registry.ModBlocks.TUBIFEX_DISH.get()); // M26
        event.register((state, level, pos, tint) -> tint == 0 ? DAPHNIA_WATER : 0xFFFFFFFF, org.overengineer.bettacraft.registry.ModBlocks.DAPHNIA_BARREL.get());
        // M18: the panel of the aquarium light in its colour (darker while off)
        event.register((state, level, pos, tint) -> tint == 0 ? 0xFF000000 | org.overengineer.bettacraft.equipment.TankLightColors.panel(
                state.getValue(org.overengineer.bettacraft.equipment.TankLightBlock.COLOR), state.getValue(org.overengineer.bettacraft.equipment.TankLightBlock.LIT))
                : 0xFFFFFFFF, org.overengineer.bettacraft.registry.ModBlocks.TANK_LIGHT.get(),
                org.overengineer.bettacraft.registry.ModBlocks.LED_PANEL.get(), org.overengineer.bettacraft.registry.ModBlocks.HIGH_POWER_LED.get());
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
        // M31: the terrarium animals
        for (String m : org.overengineer.bettacraft.client.model.TerrariumModelDefinitions.MODELS) {
            event.registerLayerDefinition(org.overengineer.bettacraft.client.render.TerrariumAnimalRenderer.layer(m),
                    () -> org.overengineer.bettacraft.client.model.TerrariumModelDefinitions.create(m));
        }
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
        event.registerEntityRenderer(ModEntities.MUSK_TURTLE.get(), ctx -> new org.overengineer.bettacraft.client.render.TerrariumAnimalRenderer(ctx, "musk_turtle", 0.8F, 0.2F)); // M31
        event.registerEntityRenderer(ModEntities.SLIDER_TURTLE.get(), ctx -> new org.overengineer.bettacraft.client.render.TerrariumAnimalRenderer(ctx, "slider_turtle", 0.9F, 0.3F)); // M31 part b
        event.registerEntityRenderer(ModEntities.HERMANN_TORTOISE.get(), ctx -> new org.overengineer.bettacraft.client.render.TerrariumAnimalRenderer(ctx, "hermann_tortoise", 0.85F, 0.28F));
        event.registerEntityRenderer(ModEntities.REEVES_TURTLE.get(), ctx -> new org.overengineer.bettacraft.client.render.TerrariumAnimalRenderer(ctx, "reeves_turtle", 0.85F, 0.25F));
        // M31 part c: the amphibians and reptiles (size of the model, shadow, how deep the horned frog sits buried)
        registerHerp(event, org.overengineer.bettacraft.terrarium.HerpSpecies.DART_FROG, 0.3F, 0.08F, 0F);
        registerHerp(event, org.overengineer.bettacraft.terrarium.HerpSpecies.HORNED_FROG, 0.7F, 0.22F, 0.22F);
        registerHerp(event, org.overengineer.bettacraft.terrarium.HerpSpecies.LEOPARD_GECKO, 0.52F, 0.16F, 0F);
        registerHerp(event, org.overengineer.bettacraft.terrarium.HerpSpecies.CRESTED_GECKO, 0.52F, 0.14F, 0F);
        registerHerp(event, org.overengineer.bettacraft.terrarium.HerpSpecies.CORN_SNAKE, 0.88F, 0.2F, 0F);
        event.registerEntityRenderer(ModEntities.WILD_CRITTER.get(), org.overengineer.bettacraft.client.render.WildCritterRenderer::new); // M22.3
        for (var t : ModEntities.TANKMATES.values()) event.registerEntityRenderer(t.get(), org.overengineer.bettacraft.client.render.TankmateRenderer::new); // M19
        event.registerBlockEntityRenderer(ModBlockEntities.TANK.get(), TankSignRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.EXTERNAL_FILTER.get(), org.overengineer.bettacraft.client.render.FilterRenderer::forFilter);
        event.registerBlockEntityRenderer(ModBlockEntities.CABINET.get(), org.overengineer.bettacraft.client.render.FilterRenderer::forCabinet);
        event.registerBlockEntityRenderer(ModBlockEntities.AIR_PUMP.get(), org.overengineer.bettacraft.client.render.AirHoseRenderer::new); // M23
        event.registerBlockEntityRenderer(ModBlockEntities.THERMO_HYGROMETER.get(), org.overengineer.bettacraft.client.render.ThermoHygrometerRenderer::new); // M31
        event.registerBlockEntityRenderer(ModBlockEntities.MONITOR.get(), org.overengineer.bettacraft.client.render.MonitorRenderer::new); // M27
    }

    private static void onScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.TANK.get(), TankScreen::new);
        event.register(ModMenus.FEEDER.get(), FeederScreen::new);
        event.register(ModMenus.DOSING_PUMP.get(), org.overengineer.bettacraft.client.screen.DosingPumpScreen::new); // M25 part c
        event.register(ModMenus.LIVE_FOOD_PUMP.get(), org.overengineer.bettacraft.client.screen.LiveFoodPumpScreen::new); // M26
        event.register(ModMenus.FOOD_MAKER.get(), org.overengineer.bettacraft.client.screen.FoodMakerScreen::new); // M26 part b
        event.register(ModMenus.FERTILIZER_MAKER.get(), org.overengineer.bettacraft.client.screen.FertilizerMakerScreen::new); // M37
        event.register(ModMenus.EXTERNAL_FILTER.get(), org.overengineer.bettacraft.client.screen.ExternalFilterScreen::new);
        event.register(ModMenus.CRUCIBLE.get(), org.overengineer.bettacraft.client.screen.CrucibleScreen::new); // M24
        event.register(ModMenus.CABINET.get(), org.overengineer.bettacraft.client.screen.CabinetScreen::new);
    }

    private static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(TankModelLoader.ID, TankModelLoader.INSTANCE);
    }

    /** M25: the look of our water fluids: Minecraft's water textures in a tint. */
    private static net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions waterLook(int tint) {
        return new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
            @Override
            public net.minecraft.resources.ResourceLocation getStillTexture() {
                return net.minecraft.resources.ResourceLocation.withDefaultNamespace("block/water_still");
            }

            @Override
            public net.minecraft.resources.ResourceLocation getFlowingTexture() {
                return net.minecraft.resources.ResourceLocation.withDefaultNamespace("block/water_flow");
            }

            @Override
            public int getTintColor() {
                return tint;
            }
        };
    }

    /** M31 part c: the renderer of an amphibian or reptile species. */
    private static void registerHerp(EntityRenderersEvent.RegisterRenderers event, org.overengineer.bettacraft.terrarium.HerpSpecies s, float scale, float shadow,
                                     float bury) {
        event.registerEntityRenderer(ModEntities.HERPS.get(s).get(), ctx -> new org.overengineer.bettacraft.client.render.HerpRenderer(ctx, s, scale, shadow, bury));
    }
}
