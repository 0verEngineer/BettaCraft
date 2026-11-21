package org.overengineer.bettacraft.client;

import net.minecraft.world.level.block.state.BlockState;
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

/**
 * M52: the client registrations of both loaders (entity renderers and layers, block entity renderers, colours, screens,
 * item properties, render layers, particles) and the common client hooks. The sinks have the method names of NeoForge's
 * registration events; NeoForge passes its events, Fabric its registries.
 */
public final class ClientSetup {
    private ClientSetup() {}

    private static final int OTHER_FISH_BODY = 0xFFB0B8C0, OTHER_FISH_FINS = 0xFF8A949E;
    static final int LARVA_WATER = 0xFF6A7C3A, HATCHERY_WATER = 0xFF4E93C8;
    static final int TUBIFEX_WATER = 0xFF6E5A3A, DAPHNIA_WATER = 0xFF5E8A3E;

    public interface ItemColors {
        void register(org.overengineer.bettacraft.client.item.ItemTints.Tint color, net.minecraft.world.level.ItemLike... items);
    }

    public interface BlockColors {
        void register(BlockColor color, net.minecraft.world.level.block.Block... blocks);
    }

    /**
     * M52c: a block colour of 1.21.x ({@code BlockColor}: state, level and position (null outside the world), tint index);
     * 26.1 tints through one {@link net.minecraft.client.color.block.BlockTintSource} per tint index ({@link #tintSources}).
     */
    @FunctionalInterface
    public interface BlockColor {
        int getColor(BlockState state, @org.jetbrains.annotations.Nullable net.minecraft.client.renderer.block.BlockAndTintGetter level,
                @org.jetbrains.annotations.Nullable net.minecraft.core.BlockPos pos, int tint);
    }

    /** M52c: the highest tint index of our block models (+1): the tint sources a {@link BlockColor} gets. */
    private static final int TINT_LAYERS = 3;

    /** M52c: the tint sources of a {@link BlockColor} (index 0, 1, 2 of the models), for {@code BlockColors#register}. */
    public static java.util.List<net.minecraft.client.color.block.BlockTintSource> tintSources(BlockColor color) {
        java.util.List<net.minecraft.client.color.block.BlockTintSource> out = new java.util.ArrayList<>();
        for (int i = 0; i < TINT_LAYERS; i++) {
            int tint = i;
            out.add(new net.minecraft.client.color.block.BlockTintSource() {
                @Override
                public int color(BlockState state) {
                    return color.getColor(state, null, null, tint);
                }

                @Override
                public int colorInWorld(BlockState state, net.minecraft.client.renderer.block.BlockAndTintGetter level, net.minecraft.core.BlockPos pos) {
                    return color.getColor(state, level, pos, tint);
                }
            });
        }
        return java.util.List.copyOf(out);
    }

    /** M52c: the tint of a (foreign) block state as the 1.21.x {@code BlockColors#getColor} gave it, -1 without one. */
    public static int tint(BlockState state, @org.jetbrains.annotations.Nullable net.minecraft.client.renderer.block.BlockAndTintGetter level,
            @org.jetbrains.annotations.Nullable net.minecraft.core.BlockPos pos, int tint) {
        var src = net.minecraft.client.Minecraft.getInstance().getBlockColors().getTintSource(state, tint);
        if (src == null) return -1;
        return level != null && pos != null ? src.colorInWorld(state, level, pos) : src.color(state);
    }

    public interface Layers {
        void registerLayerDefinition(net.minecraft.client.model.geom.ModelLayerLocation layer,
                java.util.function.Supplier<net.minecraft.client.model.geom.builders.LayerDefinition> definition);
    }

    public interface Renderers {
        <T extends net.minecraft.world.entity.Entity> void registerEntityRenderer(net.minecraft.world.entity.EntityType<? extends T> type,
                net.minecraft.client.renderer.entity.EntityRendererProvider<T> provider);

        <T extends net.minecraft.world.level.block.entity.BlockEntity, S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState> void registerBlockEntityRenderer(
                net.minecraft.world.level.block.entity.BlockEntityType<? extends T> type,
                net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider<T, S> provider);
    }

    public interface Screens {
        <M extends net.minecraft.world.inventory.AbstractContainerMenu, U extends net.minecraft.client.gui.screens.Screen & net.minecraft.client.gui.screens.inventory.MenuAccess<M>> void register(
                net.minecraft.world.inventory.MenuType<? extends M> type, net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor<M, U> factory);
    }

    public interface ItemProperties {
        void register(net.minecraft.world.item.Item item, net.minecraft.resources.Identifier id,
                org.overengineer.bettacraft.client.item.ItemPredicates.Function function);
    }

    public interface RenderLayers {
        void put(net.minecraft.world.level.block.Block block, net.minecraft.client.renderer.rendertype.RenderType layer);
    }

    public interface Particles {
        <T extends net.minecraft.core.particles.ParticleOptions> void registerSpriteSet(net.minecraft.core.particles.ParticleType<T> type,
                java.util.function.Function<net.minecraft.client.particle.SpriteSet, net.minecraft.client.particle.ParticleProvider<T>> factory);
    }

    public static void itemColors(ItemColors event) {
        event.register((stack, tint) -> {
            if (tint == 0 || tint >= 3) return 0xFFFFFFFF; // container and (M21) the glass in front of the fish
            // M55: a bag with an animal of its own icon: its sprite has its colours (a gene-coloured small animal: the body tinted)
            int own = critterIconTint(stack, tint);
            if (own != 0) return own;
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
        // M22.3: the critter bucket shows the first animal's colour (layer 1 body, layer 2 legs and feelers); M55: its own sprite
        event.register((stack, tint) -> {
            if (tint == 0 || tint >= 3) return 0xFFFFFFFF;
            int own = critterIconTint(stack, tint);
            if (own != 0) return own;
            var critters = stack.get(ModDataComponents.CRITTERS.get());
            org.overengineer.bettacraft.critters.Critter k = critters == null || critters.isEmpty() ? null : org.overengineer.bettacraft.critters.Critter.loadAnimal(critters.getFirst(), 0, 0, 0, 0);
            int rgb = k == null ? 0xC8A070 : k.genes.rgb(k.species);
            if (k != null && (k.species.nerite() || k.species == org.overengineer.bettacraft.critters.CritterSpecies.ASSASSIN_SNAIL)) rgb = k.species.wildColor();
            return 0xFF000000 | (tint == 1 ? rgb : org.overengineer.bettacraft.critters.CritterGenes.mix(rgb, 0xFFFFFF, 0.35F));
        }, ModItems.CRITTER_BUCKET.get());
        // M19: tankmate buckets in the species' colours; M55: every species has its own coloured sprite (no tint)
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

    public static void blockColors(BlockColors event) {
        // M40 part c: a covered filter pipe or bundle is tinted like its cover (grass, leaves)
        event.register((state, level, pos, tint) -> {
            if (level == null || pos == null) return -1;
            var c = org.overengineer.bettacraft.plumbing.PipeCovers.coverAt(level, pos);
            return c == null ? -1 : tint(c, level, pos, tint);
        }, org.overengineer.bettacraft.registry.ModBlocks.FILTER_INLET_PIPE.get(), org.overengineer.bettacraft.registry.ModBlocks.FILTER_OUTLET_PIPE.get(),
                org.overengineer.bettacraft.registry.ModBlocks.FILTER_FRESH_PIPE.get(), org.overengineer.bettacraft.registry.ModBlocks.FILTER_WASTE_PIPE.get(),
                org.overengineer.bettacraft.registry.ModBlocks.FILTER_PIPE_BUNDLE.get());
        event.register((state, level, pos, tint) -> tint == 0 ? LARVA_WATER : 0xFFFFFFFF, org.overengineer.bettacraft.registry.ModBlocks.LARVA_TUB.get());
        // the interior floor is tinted like its substrate
        event.register(org.overengineer.bettacraft.client.interior.SubstrateFloorModel::blockColor, org.overengineer.bettacraft.registry.ModBlocks.INTERIOR_FLOOR.get());
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

    public static void layers(Layers event) {
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

    public static void renderers(Renderers event) {
        event.registerEntityRenderer(ModEntities.BETTA.get(), BettaRenderer::new);
        event.registerEntityRenderer(ModEntities.HIRED_AQUARIST.get(), org.overengineer.bettacraft.client.render.AquaristRenderer::new); // M51
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
        registerHerp(event, org.overengineer.bettacraft.terrarium.HerpSpecies.MUDSKIPPER, 0.42F, 0.14F, 0F); // M49 part b
        event.registerEntityRenderer(ModEntities.WILD_CRITTER.get(), org.overengineer.bettacraft.client.render.WildCritterRenderer::new); // M22.3
        for (var t : ModEntities.TANKMATES.values()) event.registerEntityRenderer(t.get(), org.overengineer.bettacraft.client.render.TankmateRenderer::new); // M19
        event.registerBlockEntityRenderer(ModBlockEntities.TANK.get(), TankSignRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.EXTERNAL_FILTER.get(), org.overengineer.bettacraft.client.render.FilterRenderer::forFilter);
        event.registerBlockEntityRenderer(ModBlockEntities.CABINET.get(), org.overengineer.bettacraft.client.render.FilterRenderer::forCabinet);
        event.registerBlockEntityRenderer(ModBlockEntities.AIR_PUMP.get(), org.overengineer.bettacraft.client.render.AirHoseRenderer::new); // M23
        event.registerBlockEntityRenderer(ModBlockEntities.THERMO_HYGROMETER.get(), org.overengineer.bettacraft.client.render.ThermoHygrometerRenderer::new); // M31
        event.registerBlockEntityRenderer(ModBlockEntities.MONITOR.get(), org.overengineer.bettacraft.client.render.MonitorRenderer::new); // M27
    }

    public static void screens(Screens event) {
        event.register(ModMenus.TANK.get(), TankScreen::new);
        event.register(ModMenus.FEEDER.get(), FeederScreen::new);
        event.register(ModMenus.DOSING_PUMP.get(), org.overengineer.bettacraft.client.screen.DosingPumpScreen::new); // M25 part c
        event.register(ModMenus.SUMP.get(), org.overengineer.bettacraft.client.screen.SumpScreen::new); // M49
        event.register(ModMenus.CALCIUM_REACTOR.get(), org.overengineer.bettacraft.client.screen.CalciumReactorScreen::new); // M49
        event.register(ModMenus.LIVE_FOOD_PUMP.get(), org.overengineer.bettacraft.client.screen.LiveFoodPumpScreen::new); // M26
        event.register(ModMenus.FOOD_MAKER.get(), org.overengineer.bettacraft.client.screen.FoodMakerScreen::new); // M26 part b
        event.register(ModMenus.FERTILIZER_MAKER.get(), org.overengineer.bettacraft.client.screen.FertilizerMakerScreen::new); // M37
        event.register(ModMenus.EXTERNAL_FILTER.get(), org.overengineer.bettacraft.client.screen.ExternalFilterScreen::new);
        event.register(ModMenus.CRUCIBLE.get(), org.overengineer.bettacraft.client.screen.CrucibleScreen::new); // M24
        event.register(ModMenus.CABINET.get(), org.overengineer.bettacraft.client.screen.CabinetScreen::new);
    }

    /** The client hooks and the listeners of the common client events (both loaders, once at client start). */
    public static void init() {
        // M22.1: the question before animals leave a bucket or a bag into the world
        org.overengineer.bettacraft.containers.ContainerRules.confirmScreen = org.overengineer.bettacraft.client.screen.ConfirmEmptyScreen::open;
        // M50: the sloshing water of travelling tanks (one step per frame; the blocks leave their water out)
        org.overengineer.bettacraft.tank.TankBlockEntity.DYNAMIC_WATER = org.overengineer.bettacraft.client.render.TravelWater::drawsWater;
        org.overengineer.bettacraft.platform.client.ClientEvents.TICK_POST.add(FilterEffects::tick);
        org.overengineer.bettacraft.client.DeviceEffects.init(); // M23: air pumps, air stones, regulator, particles
        org.overengineer.bettacraft.platform.client.ClientEvents.TICK_POST.add(org.overengineer.bettacraft.client.interior.ClientInteriorCache::tick);
        // M18: colour of the tank lights for the water (their shader light colour: IrisBlockIdsMixin)
        org.overengineer.bettacraft.platform.client.ClientEvents.TICK_POST.add(org.overengineer.bettacraft.client.tank.TankLightTint::tick);
        org.overengineer.bettacraft.platform.client.ClientEvents.TICK_POST.add(org.overengineer.bettacraft.client.map.AlarmWaypoints::tick); // M53: map mod switches
        // M21: plant prototype (Rotala, Vallisneria, Anubias with real leaves in the interior), then the miniatures and the decor preview
        org.overengineer.bettacraft.platform.client.ClientEvents.RENDER_LEVEL.add(org.overengineer.bettacraft.client.interior.PlantMeshRenderer::onRenderStage);
        org.overengineer.bettacraft.platform.client.ClientEvents.RENDER_LEVEL.add(org.overengineer.bettacraft.client.interior.MiniatureRenderer::onRenderStage);
        org.overengineer.bettacraft.platform.client.ClientEvents.RENDER_LEVEL.add(org.overengineer.bettacraft.client.decor.DecorPreview::onRenderStage);
        org.overengineer.bettacraft.platform.client.ClientEvents.ITEM_TOOLTIP.add((stack, ctx, lines, flag) -> ItemHintTooltips.onTooltip(stack, lines));
    }

    /** The resources were reloaded: skins and baked critter models are made again. */
    public static void onResourcesReloaded() {
        org.overengineer.bettacraft.client.render.BettaSkins.clear();
        org.overengineer.bettacraft.client.render.CritterRenderer.reset(); // M14: baked again on the next frame
    }

    /** The air bubble particles (M23). */
    public static void particles(Particles event) {
        event.registerSpriteSet(org.overengineer.bettacraft.registry.ModParticles.AIR_BUBBLE.get(),
                org.overengineer.bettacraft.client.particle.AirBubbleParticle.Provider::new);
    }

    /**
     * M52b: the item model types of the mod (1.21.4 item models): the tint source of the 1.21.1 item colours and the
     * conditional property of the 1.21.1 item properties; NeoForge registers them in its events, Fabric in its registries.
     */
    public interface ItemModelTypes {
        void tintSource(net.minecraft.resources.Identifier id, com.mojang.serialization.MapCodec<? extends net.minecraft.client.color.item.ItemTintSource> codec);

        void conditionalProperty(net.minecraft.resources.Identifier id,
                com.mojang.serialization.MapCodec<? extends net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty> codec);

        void itemModel(net.minecraft.resources.Identifier id, com.mojang.serialization.MapCodec<? extends net.minecraft.client.renderer.item.ItemModel.Unbaked> codec);
    }

    public static void itemModelTypes(ItemModelTypes event) {
        event.tintSource(org.overengineer.bettacraft.client.item.ItemTints.TYPE, org.overengineer.bettacraft.client.item.ItemTints.Source.MAP_CODEC);
        event.conditionalProperty(org.overengineer.bettacraft.client.item.ItemPredicates.TYPE, org.overengineer.bettacraft.client.item.ItemPredicates.Property.MAP_CODEC);
        event.itemModel(org.overengineer.bettacraft.client.item.BlockModelItem.TYPE, org.overengineer.bettacraft.client.item.BlockModelItem.Unbaked.MAP_CODEC);
    }

    /**
     * M55: the tint of a container showing a small animal's own sprite (layer 1 its body: the colour of its genes for the
     * gene-coloured species, else untinted; layer 2 the details, untinted), 0 when the container shows no own sprite.
     */
    private static int critterIconTint(net.minecraft.world.item.ItemStack stack, int tint) {
        if (org.overengineer.bettacraft.containers.AnimalIcons.number(stack) <= 0) return 0;
        var c = org.overengineer.bettacraft.containers.AnimalIcons.critter(stack);
        if (c == null || tint != 1 || !org.overengineer.bettacraft.containers.AnimalIcons.tintedBody(c)) return 0xFFFFFFFF;
        var k = org.overengineer.bettacraft.critters.Critter.loadAnimal(stack.get(ModDataComponents.CRITTERS.get()).getFirst(), 0, 0, 0, 0);
        return 0xFF000000 | (k == null ? c.wildColor() : k.genes.rgb(k.species));
    }

    /** The item models that change with the item's state ("filled"; M55: the animal inside). */
    public static void itemProperties(ItemProperties event) {
        for (var item : java.util.List.of(org.overengineer.bettacraft.registry.ModItems.CRITTER_BUCKET.get(), org.overengineer.bettacraft.registry.ModItems.FISH_BAG.get(),
                org.overengineer.bettacraft.registry.ModItems.TRANSPORT_TUB.get())) {
            event.register(item, org.overengineer.bettacraft.containers.AnimalIcons.PROPERTY,
                    (stack, level, entity, seed) -> org.overengineer.bettacraft.containers.AnimalIcons.value(stack));
        }
        event.register(org.overengineer.bettacraft.registry.ModItems.FISH_BAG.get(), org.overengineer.bettacraft.BettaCraft.id("filled"),
                (stack, level, entity, seed) -> org.overengineer.bettacraft.entity.betta.FishBagItem.isFilled(stack) ? 1.0F : 0.0F);
        event.register(org.overengineer.bettacraft.registry.ModItems.TRANSPORT_TUB.get(), org.overengineer.bettacraft.BettaCraft.id("filled"), // M30
                (stack, level, entity, seed) -> org.overengineer.bettacraft.big.TransportTubItem.isFilled(stack) ? 1.0F : 0.0F);
        event.register(org.overengineer.bettacraft.registry.ModItems.MOB_JAR.get(), org.overengineer.bettacraft.BettaCraft.id("filled"), // M31 part d
                (stack, level, entity, seed) -> org.overengineer.bettacraft.mobs.MobJarItem.isFilled(stack) ? 1.0F : 0.0F);
    }

    /** M15: how to leave a tank interior, bottom left (NeoForge: a GUI layer below the chat). */
    public static void renderHud(net.minecraft.client.gui.GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        org.overengineer.bettacraft.client.interior.InteriorLeaveHud.extractRenderState(graphics, delta);
    }

    /** M31 part c: the renderer of an amphibian or reptile species. */
    static void registerHerp(Renderers event, org.overengineer.bettacraft.terrarium.HerpSpecies s, float scale, float shadow,
                                     float bury) {
        event.registerEntityRenderer(ModEntities.HERPS.get(s).get(), ctx -> new org.overengineer.bettacraft.client.render.HerpRenderer(ctx, s, scale, shadow, bury));
    }

    /** The player left a server or world: the client caches go (M17), the genetics back to the built-in ones. */
    public static void onLogout() {
        org.overengineer.bettacraft.util.ClientBlockEntitySets.purgeLevel(null);
        org.overengineer.bettacraft.client.interior.ClientInteriorCache.clear();
        org.overengineer.bettacraft.client.interior.PlantMeshRenderer.clear();
        org.overengineer.bettacraft.client.decor.DecorPreview.clear();
        org.overengineer.bettacraft.client.map.AlarmWaypoints.clear(); // M32 part d
        org.overengineer.bettacraft.crafting.RecipeSync.clear(); // M52b
        // back to the built-in genetics (the next server sends its own); not while our integrated server still runs
        if (net.minecraft.client.Minecraft.getInstance().getSingleplayerServer() == null) {
            org.overengineer.bettacraft.genetics.GeneticsData.reset();
        }
    }
}
