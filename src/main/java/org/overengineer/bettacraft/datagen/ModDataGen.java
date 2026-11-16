package org.overengineer.bettacraft.datagen;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.AndCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.OrCondition;
import net.neoforged.neoforge.common.conditions.TagEmptyCondition;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.decor.DecorType;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModDataComponents;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankRecipe;
import org.overengineer.bettacraft.world.AddItemLootModifier;
import org.overengineer.bettacraft.world.BettaSpawnsBiomeModifier;
import org.overengineer.bettacraft.world.FishBagRecipeCondition;

/** All generated JSON (run with {@code gradlew runData}; output in src/generated/resources). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class ModDataGen {
    private ModDataGen() {}

    public static final ResourceKey<BiomeModifier> BETTA_SPAWNS = ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, BettaCraft.id("betta_spawns"));
    // M22.3
    public static final ResourceKey<BiomeModifier> WILDLIFE_SPAWNS = ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, BettaCraft.id("wildlife_spawns"));
    public static final ResourceKey<BiomeModifier> WILD_CORALS = ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, BettaCraft.id("wild_corals"));
    public static final ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> WILD_CORAL_CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, BettaCraft.id("wild_coral"));
    public static final ResourceKey<net.minecraft.world.level.levelgen.placement.PlacedFeature> WILD_CORAL_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE, BettaCraft.id("wild_coral"));

    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput out = gen.getPackOutput();
        ExistingFileHelper efh = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        gen.addProvider(event.includeClient(), new EnglishLang(out));
        gen.addProvider(event.includeClient(), new BlockStates(out, efh));
        gen.addProvider(event.includeClient(), new ItemModels(out, efh));
        gen.addProvider(event.includeServer(), new Recipes(out, lookup));
        gen.addProvider(event.includeServer(), new LootTableProvider(out, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(BlockLoot::new, LootContextParamSets.BLOCK)), lookup));
        BlockTagsGen blockTags = gen.addProvider(event.includeServer(), new BlockTagsGen(out, lookup, efh));
        gen.addProvider(event.includeServer(), new ItemTagsGen(out, lookup, blockTags.contentsGetter(), efh));
        gen.addProvider(event.includeServer(), new EntityTagsGen(out, lookup, efh));
        gen.addProvider(event.includeServer(), new DataGenM25b.FluidTagsGen(out, lookup, efh)); // M25
        gen.addProvider(event.includeServer(), new BiomeTagsGen(out, lookup, efh));
        gen.addProvider(event.includeServer(), new DatapackBuiltinEntriesProvider(out, lookup, new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, ctx -> {
                    ctx.register(WILD_CORAL_CONFIGURED, new net.minecraft.world.level.levelgen.feature.ConfiguredFeature<>(
                            org.overengineer.bettacraft.registry.ModCodecs.WILD_CORAL.get(), net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration.NONE));
                    DataGenM281.configuredFeatures(ctx); // M28.1
                })
                .add(Registries.PLACED_FEATURE, ctx -> {
                    DataGenM281.placedFeatures(ctx); // M28.1
                    ctx.register(WILD_CORAL_PLACED, new net.minecraft.world.level.levelgen.placement.PlacedFeature(
                        ctx.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(WILD_CORAL_CONFIGURED), List.of(
                        net.minecraft.world.level.levelgen.placement.RarityFilter.onAverageOnceEvery(2),
                        net.minecraft.world.level.levelgen.placement.InSquarePlacement.spread(),
                        net.minecraft.world.level.levelgen.placement.HeightmapPlacement.onHeightmap(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG),
                        net.minecraft.world.level.levelgen.placement.BiomeFilter.biome())));
                })
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ctx -> {
                    DataGenM281.biomeModifiers(ctx); // M28.1: wild aquarium and floating plants
                    ctx.register(BETTA_SPAWNS, new BettaSpawnsBiomeModifier(ctx.lookup(Registries.BIOME).getOrThrow(ModTags.BETTA_SPAWN_BIOMES)));
                    // M22.3: wild tankmates, frogs and small animals; the wild corals on the warm ocean reefs
                    ctx.register(WILDLIFE_SPAWNS, new org.overengineer.bettacraft.world.WildlifeSpawnsBiomeModifier());
                    // M25.1: #bettacraft:coral_reef_biomes or both #c:is_ocean and #c:is_hot (like BiomeWaters.coralReef)
                    var biomes = ctx.lookup(Registries.BIOME);
                    ctx.register(WILD_CORALS, new net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier(
                            new net.neoforged.neoforge.registries.holdersets.OrHolderSet<>(biomes.getOrThrow(ModTags.CORAL_REEF_BIOMES),
                                    new net.neoforged.neoforge.registries.holdersets.AndHolderSet<>(biomes.getOrThrow(Tags.Biomes.IS_OCEAN), biomes.getOrThrow(Tags.Biomes.IS_HOT))),
                            net.minecraft.core.HolderSet.direct(ctx.lookup(Registries.PLACED_FEATURE).getOrThrow(WILD_CORAL_PLACED)),
                            net.minecraft.world.level.levelgen.GenerationStep.Decoration.VEGETAL_DECORATION));
                }),
                Set.of(BettaCraft.MOD_ID)));
        gen.addProvider(event.includeServer(), new LootModifiers(out, lookup));
        gen.addProvider(event.includeServer(), new GeneticsFiles(out));
        gen.addProvider(event.includeServer(), new net.neoforged.neoforge.common.data.AdvancementProvider(out, lookup, efh, List.of(new ModAdvancements())));
    }

    // ------------------------------------------------------------------ genetics (M5): built-in colours, genes, varieties

    /** data/bettacraft/bettacraft/{colors,genes,varieties}/*.json from the built-in tables (see GeneticsData). */
    static final class GeneticsFiles implements net.minecraft.data.DataProvider {
        private final PackOutput.PathProvider colors, genes, varieties;

        GeneticsFiles(PackOutput out) {
            colors = out.createPathProvider(PackOutput.Target.DATA_PACK, "bettacraft/colors");
            genes = out.createPathProvider(PackOutput.Target.DATA_PACK, "bettacraft/genes");
            varieties = out.createPathProvider(PackOutput.Target.DATA_PACK, "bettacraft/varieties");
        }

        @Override
        public CompletableFuture<?> run(net.minecraft.data.CachedOutput cache) {
            var d = org.overengineer.bettacraft.genetics.GeneticsData.defaults();
            List<CompletableFuture<?>> all = new java.util.ArrayList<>();
            d.colors().values().forEach(c -> all.add(net.minecraft.data.DataProvider.saveStable(cache,
                    org.overengineer.bettacraft.genetics.GeneticsData.colorJson(c), colors.json(BettaCraft.id(c.id())))));
            org.overengineer.bettacraft.genetics.GeneticsData.DEFAULT_GENES.forEach((locus, list) -> all.add(net.minecraft.data.DataProvider.saveStable(cache,
                    org.overengineer.bettacraft.genetics.GeneticsData.genesJson(list), genes.json(BettaCraft.id(locus)))));
            d.varieties().forEach(v -> all.add(net.minecraft.data.DataProvider.saveStable(cache,
                    org.overengineer.bettacraft.genetics.GeneticsData.varietyJson(v), varieties.json(BettaCraft.id(v.id())))));
            return CompletableFuture.allOf(all.toArray(CompletableFuture[]::new));
        }

        @Override
        public String getName() {
            return "Bettacraft genetics";
        }
    }

    // ------------------------------------------------------------------ lang (en_us; de_de is maintained by hand)

    static final class EnglishLang extends LanguageProvider {
        EnglishLang(PackOutput out) {
            super(out, BettaCraft.MOD_ID, "en_us");
        }

        @Override
        protected void addTranslations() {
            LangEn.addAll(this::add);
        }
    }

    // ------------------------------------------------------------------ models

    static final class BlockStates extends BlockStateProvider {
        BlockStates(PackOutput out, ExistingFileHelper efh) {
            super(out, BettaCraft.MOD_ID, efh);
        }

        private void aquaticPlants() {
            for (var e : ModBlocks.AQUATIC_PLANTS.entrySet()) {
                var s = e.getKey();
                java.util.Map<String, ModelFile> made = new java.util.HashMap<>();
                getVariantBuilder(e.getValue().get()).forAllStates(state -> {
                    var look = e.getValue().get().looks(state);
                    String name = plantModel(s, look);
                    ModelFile m = made.computeIfAbsent(name, n -> {
                        String template = switch (look) {
                            case CUTTING -> "template_plant_cutting";
                            case STEM -> "template_plant_stem";
                            case RHIZOME -> "template_plant_rhizome";
                            case RIBBON -> "template_plant_ribbon";
                            case PAD -> "template_plant_pad";
                            case MAT -> "template_plant_mat";
                            default -> "template_plant_leaf";
                        };
                        String tex = switch (look) {
                            case CUTTING -> "plant_cutting";
                            case STEM -> "plant_stem";
                            case RHIZOME -> "plant_rhizome";
                            case RIBBON -> "plant_ribbon";
                            case PAD -> "plant_pad";
                            case MAT -> "plant_mat";
                            case MOSS -> "plant_moss";
                            default -> "plant_leaf_" + s.leafTexture();
                        };
                        return models().withExistingParent(n, modLoc("block/" + template)).texture("tex", modLoc("block/" + tex))
                                .renderType("cutout_mipped");
                    });
                    return ConfiguredModel.builder().modelFile(m).build();
                });
            }
        }

        /** M22.3: the wild corals of the reefs: a mat, discs, a dome, a toadstool or a bushy cross, coloured sprites. */
        private void wildCorals() {
            for (var e : ModBlocks.WILD_CORALS.entrySet()) {
                String id = "wild_" + e.getKey().id();
                String shape = org.overengineer.bettacraft.reef.WildCoralBlock.shape(e.getKey());
                ModelFile m = switch (shape) {
                    case "mat" -> models().withExistingParent(id, modLoc("block/template_wild_coral_mat")).texture("tex", modLoc("block/" + id))
                            .texture("cross", modLoc("block/" + id + "_cross"));
                    case "toadstool" -> models().withExistingParent(id, modLoc("block/template_wild_coral_toadstool")).texture("tex", modLoc("block/" + id))
                            .texture("stem", modLoc("block/" + id + "_stem"));
                    case "disc", "dome" -> models().withExistingParent(id, modLoc("block/template_wild_coral_" + shape)).texture("tex", modLoc("block/" + id));
                    default -> models().cross(id, modLoc("block/" + id)).renderType("cutout");
                };
                getVariantBuilder(e.getValue().get()).forAllStates(state -> ConfiguredModel.builder().modelFile(m).build());
            }
        }

        @Override
        protected void registerStatesAndModels() {
            wildCorals(); // M22.3
            // block/tank.json is hand-written (custom geometry loader bettacraft:tank)
            ModelFile tank = models().getExistingFile(modLoc("block/tank"));
            getMultipartBuilder(ModBlocks.TANK.get()).part().modelFile(tank).addModel().end();
            simpleBlock(ModBlocks.INTERIOR_FLOOR.get(), cubeAll(ModBlocks.INTERIOR_FLOOR.get()));
            simpleBlock(ModBlocks.INTERIOR_WALL.get(), models().getBuilder("interior_wall")
                    .texture("particle", modLoc("block/interior_wall")));
            // interior equipment (M3): one texture per segment part; the heater size does not change the look
            for (String kind : new String[]{"heater_segment", "cooler_segment", "filter_segment"}) {
                var block = kind.equals("heater_segment") ? ModBlocks.HEATER_SEGMENT.get() : kind.equals("cooler_segment") ? ModBlocks.COOLER_SEGMENT.get()
                        : ModBlocks.FILTER_SEGMENT.get();
                java.util.function.Function<net.minecraft.world.level.block.state.BlockState, ConfiguredModel[]> fn = state -> {
                    var part = state.getValue(org.overengineer.bettacraft.equipment.SegmentBlock.PART);
                    // M23: an intake sponge at the foot of the internal filter
                    if (state.hasProperty(org.overengineer.bettacraft.equipment.DeviceSegmentBlock.SPONGE) && state.getValue(org.overengineer.bettacraft.equipment.DeviceSegmentBlock.SPONGE)
                            && (part == org.overengineer.bettacraft.equipment.SegmentBlock.Part.ANCHOR || part == org.overengineer.bettacraft.equipment.SegmentBlock.Part.BASE)) {
                        return ConfiguredModel.builder().modelFile(DataGenM23.filterSponge(this)).build();
                    }
                    String tex = switch (part) {
                        case ANCHOR, BASE -> "anchor";
                        case MIDDLE -> "middle";
                        case TOP -> "top";
                    };
                    String side = "block/" + kind + "_" + (tex.equals("top") && kind.equals("filter_segment") ? "middle" : tex);
                    String top = "block/" + kind + "_" + (tex.equals("top") ? "top" : "middle");
                    if (kind.equals("filter_segment") && part == org.overengineer.bettacraft.equipment.SegmentBlock.Part.TOP) {
                        // M4: outlet on the facing side (north in the model, rotated)
                        ModelFile m = models().cube("filter_segment_top_outlet", modLoc(side), modLoc(top), modLoc("block/filter_segment_outlet"),
                                modLoc(side), modLoc(side), modLoc(side)).texture("particle", modLoc(side));
                        return ConfiguredModel.builder().modelFile(m)
                                .rotationY(((int) state.getValue(org.overengineer.bettacraft.equipment.SegmentBlock.FACING).toYRot() + 180) % 360).build();
                    }
                    return ConfiguredModel.builder().modelFile(models().cubeColumn(kind + "_" + tex, modLoc(side), modLoc(top))).build();
                };
                if (block instanceof org.overengineer.bettacraft.equipment.DeviceSegmentBlock) {
                    getVariantBuilder(block).forAllStatesExcept(fn, org.overengineer.bettacraft.equipment.SegmentBlock.SIZE,
                            org.overengineer.bettacraft.equipment.DeviceSegmentBlock.POWER, org.overengineer.bettacraft.equipment.DeviceSegmentBlock.ACTIVE);
                } else {
                    getVariantBuilder(block).forAllStates(fn);
                }
            }

            aquaticPlants();
            // M19: spawning cave (fired clay) and spawning mop (wool strands, crossed planes)
            simpleBlock(ModBlocks.SPAWNING_CAVE.get(), cubeAll(ModBlocks.SPAWNING_CAVE.get()));
            simpleBlock(ModBlocks.SPAWNING_MOP.get(), models().cross("spawning_mop", modLoc("block/spawning_mop")).renderType("cutout"));
            reef();

            BlockModelBuilder feeder = models().getBuilder("feeder")
                    .texture("side", modLoc("block/feeder_side")).texture("top", modLoc("block/feeder_top"))
                    .texture("bottom", modLoc("block/feeder_bottom")).texture("particle", modLoc("block/feeder_side"))
                    .element().from(3, 0, 3).to(13, 8, 13)
                    .face(Direction.NORTH).uvs(3, 8, 13, 16).texture("#side").end()
                    .face(Direction.SOUTH).uvs(3, 8, 13, 16).texture("#side").end()
                    .face(Direction.EAST).uvs(3, 8, 13, 16).texture("#side").end()
                    .face(Direction.WEST).uvs(3, 8, 13, 16).texture("#side").end()
                    .face(Direction.UP).uvs(3, 3, 13, 13).texture("#top").end()
                    .face(Direction.DOWN).uvs(3, 3, 13, 13).texture("#bottom").cullface(Direction.DOWN).end()
                    .end();
            simpleBlock(ModBlocks.FEEDER.get(), feeder);
            clipFeeder();

            externalFilter();
            cabinet();
            tankLid();
            cultureTubs();

            getVariantBuilder(ModBlocks.TANK_LIGHT.get()).forAllStatesExcept(state -> {
                boolean neg = state.getValue(TankLightBlock.NEG), pos = state.getValue(TankLightBlock.POS);
                boolean hanging = state.getValue(TankLightBlock.HANGING), lit = state.getValue(TankLightBlock.LIT);
                ModelFile m = lightModel(neg, pos, hanging, lit);
                return ConfiguredModel.builder().modelFile(m)
                        .rotationY(state.getValue(TankLightBlock.AXIS) == Direction.Axis.Z ? 90 : 0).build();
            }, TankLightBlock.POWERED, TankLightBlock.COLOR, TankLightBlock.DIM); // M18: the colour is a tint of the panel (BettaCraftClient)
            DataGenM23.blockStates(this); // M23
            DataGenM24.blockStates(this); // M24
            DataGenM40.blockStates(this); // M40 part a
            DataGenM40b.blockStates(this); // M40 part b
            DataGenM25b.blockStates(this); // M25 part b
            DataGenM25c.blockStates(this); // M25 part c
            DataGenM26.blockStates(this); // M26 part a
            DataGenM37.blockStates(this); // M37
            DataGenM41.blockStates(this); // M41: the drive adapter (only the model)
            DataGenM27.blockStates(this); // M27
            DataGenM28.blockStates(this); // M28 part c
            DataGenM281.blockStates(this); // M28.1
            DataGenM31.blockStates(this); // M31 part a
            DataGenM31b.blockStates(this); // M31 part b
            DataGenM31c.blockStates(this); // M31 part c
            simpleBlock(ModBlocks.ACTIVE_SOIL.get(), cubeAll(ModBlocks.ACTIVE_SOIL.get())); // M25 part d
        }

        /** M20: reef rock, live rock, the flow pump in the interior and the protein skimmer on the tank. */
        private void reef() {
            simpleBlock(ModBlocks.REEF_ROCK.get(), cubeAll(ModBlocks.REEF_ROCK.get()));
            simpleBlock(ModBlocks.LIVE_ROCK.get(), cubeAll(ModBlocks.LIVE_ROCK.get()));
            ModelFile side = models().cube("flow_pump_segment", modLoc("block/flow_pump_segment_side"), modLoc("block/flow_pump_segment_side"),
                    modLoc("block/flow_pump_segment_outlet"), modLoc("block/flow_pump_segment_side"), modLoc("block/flow_pump_segment_side"),
                    modLoc("block/flow_pump_segment_side")).texture("particle", modLoc("block/flow_pump_segment_side"));
            getVariantBuilder(ModBlocks.PUMP_SEGMENT.get()).forAllStatesExcept(state -> ConfiguredModel.builder().modelFile(side)
                    .rotationY(((int) state.getValue(org.overengineer.bettacraft.equipment.SegmentBlock.FACING).toYRot() + 180) % 360).build(),
                    org.overengineer.bettacraft.equipment.SegmentBlock.PART, org.overengineer.bettacraft.equipment.SegmentBlock.SIZE,
                    org.overengineer.bettacraft.equipment.DeviceSegmentBlock.POWER, org.overengineer.bettacraft.equipment.DeviceSegmentBlock.SPONGE,
                    org.overengineer.bettacraft.equipment.DeviceSegmentBlock.ACTIVE); // M23: the regulator and the rest do not change the look
            // skimmer: reaction tube 5..11 x 0..12 x 2..8 and the collection cup 4..12 x 12..16 x 1..9, a hook over the glass
            BlockModelBuilder m = models().getBuilder("protein_skimmer")
                    .texture("side", modLoc("block/protein_skimmer_side")).texture("top", modLoc("block/protein_skimmer_top"))
                    .texture("particle", modLoc("block/protein_skimmer_side")).texture("port", modLoc("block/cabinet_trim"));
            m.element().from(5, 0, 2).to(11, 12, 8)
                    .face(Direction.NORTH).uvs(5, 4, 11, 16).texture("#side").end()
                    .face(Direction.SOUTH).uvs(5, 4, 11, 16).texture("#side").end()
                    .face(Direction.EAST).uvs(5, 4, 11, 16).texture("#side").end()
                    .face(Direction.WEST).uvs(5, 4, 11, 16).texture("#side").end()
                    .face(Direction.DOWN).uvs(5, 5, 11, 11).texture("#port").cullface(Direction.DOWN).end()
                    .end();
            m.element().from(4, 12, 1).to(12, 16, 9)
                    .face(Direction.NORTH).uvs(4, 0, 12, 4).texture("#side").end()
                    .face(Direction.SOUTH).uvs(4, 0, 12, 4).texture("#side").end()
                    .face(Direction.EAST).uvs(4, 0, 12, 4).texture("#side").end()
                    .face(Direction.WEST).uvs(4, 0, 12, 4).texture("#side").end()
                    .face(Direction.UP).uvs(4, 4, 12, 12).texture("#top").end()
                    .face(Direction.DOWN).uvs(4, 4, 12, 12).texture("#port").end()
                    .end();
            m.element().from(6, 13, 0).to(10, 16, 1).allFaces((d, fb) -> fb.texture("#port")).end();
            getVariantBuilder(ModBlocks.PROTEIN_SKIMMER.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(m)
                    .rotationY(((int) state.getValue(org.overengineer.bettacraft.reef.ProteinSkimmerBlock.FACING).toYRot() + 180) % 360).build());
        }

        /** M4: canister 3..13 x 0..12 with lid 4..12 x 12..14 and a hose port; model front (north) = towards the tank. */
        private void externalFilter() {
            BlockModelBuilder m = models().getBuilder("external_filter")
                    .texture("side", modLoc("block/external_filter_side")).texture("top", modLoc("block/external_filter_top"))
                    .texture("bottom", modLoc("block/external_filter_bottom")).texture("particle", modLoc("block/external_filter_side"))
                    .texture("port", modLoc("block/cabinet_trim"));
            m.element().from(3, 0, 3).to(13, 12, 13)
                    .face(Direction.NORTH).uvs(3, 4, 13, 16).texture("#side").end()
                    .face(Direction.SOUTH).uvs(3, 4, 13, 16).texture("#side").end()
                    .face(Direction.EAST).uvs(3, 4, 13, 16).texture("#side").end()
                    .face(Direction.WEST).uvs(3, 4, 13, 16).texture("#side").end()
                    .face(Direction.UP).uvs(3, 3, 13, 13).texture("#bottom").end()
                    .face(Direction.DOWN).uvs(3, 3, 13, 13).texture("#bottom").cullface(Direction.DOWN).end()
                    .end();
            m.element().from(4, 12, 4).to(12, 14, 12)
                    .face(Direction.NORTH).uvs(4, 2, 12, 4).texture("#side").end()
                    .face(Direction.SOUTH).uvs(4, 2, 12, 4).texture("#side").end()
                    .face(Direction.EAST).uvs(4, 2, 12, 4).texture("#side").end()
                    .face(Direction.WEST).uvs(4, 2, 12, 4).texture("#side").end()
                    .face(Direction.UP).uvs(4, 4, 12, 12).texture("#top").end()
                    .end();
            // outlet port towards the tank (the hose renderer continues from here)
            m.element().from(7, 12.5F, 1.5F).to(9, 14.5F, 4).allFaces((d, f) -> f.texture("#port")).end();
            getVariantBuilder(ModBlocks.EXTERNAL_FILTER.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(m)
                    .rotationY(((int) state.getValue(org.overengineer.bettacraft.equipment.ExternalFilterBlock.FACING).toYRot() + 180) % 360).build());
        }

        /**
         * M4: cabinet, front = north in the model. Wooden parts use the placeholder {@code block/cabinet_wood}, which the
         * client swaps for the planks of the cabinet. Joined sides lose the counter-top overhang.
         * <p>
         * M44: one model per front ({@code CabinetFront}), joints and joined sides: {@code cabinet[_left|_right|_both]} is the
         * old two-door front without joints (also the item), the others add {@code _<front>} and {@code _joints}.
         */
        private void cabinet() {
            var fronts = org.overengineer.bettacraft.equipment.CabinetFront.values();
            ModelFile[][][] variants = new ModelFile[fronts.length][2][4];
            for (var front : fronts) for (int j = 0; j < 2; j++) for (int i = 0; i < 4; i++) {
                boolean left = (i & 1) != 0, right = (i & 2) != 0, joints = j == 1;
                String name = "cabinet" + (left && right ? "_both" : left ? "_left" : right ? "_right" : "")
                        + (front == org.overengineer.bettacraft.equipment.CabinetFront.TWO && !joints ? "" : "_" + front.getSerializedName())
                        + (joints ? "_joints" : "");
                // M21.7: parent block/block for the display transforms of the item (it lay flat, back to the viewer)
                BlockModelBuilder m = models().getBuilder(name).parent(models().getExistingFile(mcLoc("block/block")))
                        .texture("wood", modLoc("block/cabinet_wood")).texture("trim", modLoc("block/cabinet_trim"))
                        .texture("particle", modLoc("block/cabinet_wood"));
                // carcass (full block below the counter top)
                // M21.5: every face gets explicit UVs clamped to the texture (the auto UVs of the overhangs reached past
                // the 16 px of the texture and showed neighbouring atlas textures: pink, purple, blue edges)
                box(m, 0, 0, 0, 16, 14, 16, "#wood", d -> d != Direction.UP);
                // counter top: overhangs at the front and at free sides (left = east, right = west in the model)
                float x0 = right ? 0 : -0.5F, x1 = left ? 16 : 16.5F;
                box(m, x0, 14, -0.5F, x1, 16, 16, "#wood", d -> d == Direction.UP || d == Direction.SOUTH || (d == Direction.EAST && left) || (d == Direction.WEST && right));
                // plinth and the front
                box(m, 0, 0, -0.25F, 16, 1.5F, 0, "#trim", d -> false);
                front(m, front, joints);
                variants[front.ordinal()][j][i] = m;
            }
            getVariantBuilder(ModBlocks.CABINET.get()).forAllStates(state -> {
                int i = (state.getValue(org.overengineer.bettacraft.equipment.CabinetBlock.LEFT) ? 1 : 0)
                        | (state.getValue(org.overengineer.bettacraft.equipment.CabinetBlock.RIGHT) ? 2 : 0);
                int j = state.getValue(org.overengineer.bettacraft.equipment.CabinetBlock.JOINTS) ? 1 : 0;
                return ConfiguredModel.builder().modelFile(variants[state.getValue(org.overengineer.bettacraft.equipment.CabinetBlock.FRONT).ordinal()][j][i])
                        .rotationY(((int) state.getValue(org.overengineer.bettacraft.equipment.CabinetBlock.FACING).toYRot() + 180) % 360).build();
            });
        }

        /** M44: a box on the front in the viewer's coordinates ({@code u} from the viewer's left edge; model x = 16 - u). */
        private static void fbox(BlockModelBuilder m, float u0, float y0, float z0, float u1, float y1, float z1, String tex) {
            box(m, 16 - u1, y0, z0, 16 - u0, y1, z1, tex, d -> false);
        }

        /** M44: a door leaf from {@code u0} to {@code u1} with its handle at {@code handle} (NaN: none). */
        private static void door(BlockModelBuilder m, float u0, float u1, float handle) {
            fbox(m, u0, 2.5F, -0.5F, u1, 13, 0, "#wood");
            if (!Float.isNaN(handle)) fbox(m, handle, 7, -1.25F, handle + 0.75F, 10.5F, -0.5F, "#trim");
        }

        /** M44: a dark joint in a gap of the front: a thin strip of trim just in front of the carcass, between the leaves. */
        private static void joint(BlockModelBuilder m, float u0, float y0, float u1, float y1) {
            fbox(m, u0, y0, -0.25F, u1, y1, -0.2F, "#trim");
        }

        /**
         * M44: the front of one block. The old two doors keep their exact elements; a double door over two blocks has its
         * handles at the joint between the blocks (with joints the leaves leave a gap of 0.5 px there, as between the two
         * doors, filled with the dark joint).
         */
        private static void front(BlockModelBuilder m, org.overengineer.bettacraft.equipment.CabinetFront front, boolean joints) {
            switch (front) {
                case TWO -> { // the elements of M4 in model coordinates (symmetric)
                    box(m, 1, 2.5F, -0.5F, 7.75F, 13, 0, "#wood", d -> false);
                    box(m, 8.25F, 2.5F, -0.5F, 15, 13, 0, "#wood", d -> false);
                    box(m, 6.25F, 7, -1.25F, 7, 10.5F, -0.5F, "#trim", d -> false);
                    box(m, 9, 7, -1.25F, 9.75F, 10.5F, -0.5F, "#trim", d -> false);
                    if (joints) joint(m, 7.78F, 2.5F, 8.22F, 13);
                }
                case PAIR_L -> {
                    door(m, 1, joints ? 15.75F : 15.875F, 14.25F);
                    if (joints) joint(m, 15.78F, 2.5F, 16, 13);
                }
                case PAIR_R -> {
                    door(m, joints ? 0.25F : 0.125F, 15, 1);
                    if (joints) joint(m, 0, 2.5F, 0.22F, 13);
                }
                case SINGLE -> door(m, 1, 15, 12.75F);
                case DRAWERS -> {
                    float[][] rows = {{2.5F, 5.75F}, {6.25F, 9.5F}, {10, 13}};
                    for (float[] r : rows) {
                        fbox(m, 1, r[0], -0.5F, 15, r[1], 0, "#wood");
                        float c = (r[0] + r[1]) / 2;
                        fbox(m, 6, c - 0.375F, -1.0F, 10, c + 0.375F, -0.5F, "#trim");
                    }
                    if (joints) {
                        joint(m, 1, 5.78F, 15, 6.22F);
                        joint(m, 1, 9.53F, 15, 9.97F);
                    }
                }
            }
        }

        /** M21.5: a box element with the vanilla default UVs, clamped to 0..16 (faces that reach past the block). */
        private static void box(BlockModelBuilder m, float x0, float y0, float z0, float x1, float y1, float z1, String tex,
                                java.util.function.Predicate<Direction> cull) {
            m.element().from(x0, y0, z0).to(x1, y1, z1).allFaces((d, f) -> {
                float[] uv = switch (d) {
                    case DOWN -> new float[] {x0, 16 - z1, x1, 16 - z0};
                    case UP -> new float[] {x0, z0, x1, z1};
                    case NORTH -> new float[] {16 - x1, 16 - y1, 16 - x0, 16 - y0};
                    case SOUTH -> new float[] {x0, 16 - y1, x1, 16 - y0};
                    case WEST -> new float[] {z0, 16 - y1, z1, 16 - y0};
                    case EAST -> new float[] {16 - z1, 16 - y1, 16 - z0, 16 - y0};
                };
                // M21.7: a face that lies wholly outside the 16 px (the sides of the doors and handles in front of the
                // carcass, z < 0) is moved into the texture instead of being squashed onto its edge (a zero-wide UV on the
                // edge of the sprite showed a line of the neighbouring atlas texture: blue handles, coloured door seams)
                for (int k = 0; k < 2; k++) {
                    float lo = Math.min(uv[k], uv[k + 2]), hi = Math.max(uv[k], uv[k + 2]);
                    float shift = hi - lo <= 16 && lo < 0 ? 16 * (float) Math.ceil(-lo / 16) : hi - lo <= 16 && hi > 16 ? -16 * (float) Math.ceil((hi - 16) / 16) : 0;
                    uv[k] += shift;
                    uv[k + 2] += shift;
                }
                for (int k = 0; k < 4; k++) uv[k] = Math.max(0, Math.min(16, uv[k]));
                f.texture(tex).uvs(uv[0], uv[1], uv[2], uv[3]);
                if (cull.test(d)) f.cullface(d);
            }).end();
        }

        /**
         * M21.5: clip-on feeder, facing north = it hangs on the north side of the tank (the glass at +z): a small box at
         * the top edge and the clip that reaches over the glass rim into the tank block.
         */
        private void clipFeeder() {
            BlockModelBuilder m = models().getBuilder("clip_feeder")
                    .texture("front", modLoc("block/clip_feeder_front")).texture("side", modLoc("block/feeder_side"))
                    .texture("top", modLoc("block/feeder_top")).texture("bottom", modLoc("block/feeder_bottom"))
                    .texture("particle", modLoc("block/feeder_side"));
            m.element().from(5, 7, 11.5F).to(11, 15.5F, 15.75F)
                    .face(Direction.NORTH).uvs(5, 1, 11, 9.5F).texture("#front").end()
                    .face(Direction.SOUTH).uvs(5, 1, 11, 9.5F).texture("#side").end()
                    .face(Direction.EAST).uvs(11.5F, 1, 15.75F, 9.5F).texture("#side").end()
                    .face(Direction.WEST).uvs(0.25F, 1, 4.5F, 9.5F).texture("#side").end()
                    .face(Direction.UP).uvs(5, 11.5F, 11, 15.75F).texture("#top").end()
                    .face(Direction.DOWN).uvs(5, 0.25F, 11, 4.5F).texture("#bottom").end()
                    .end();
            // the clip: over the rim and a little way down the inside of the glass
            m.element().from(6, 15.5F, 11.5F).to(10, 16.5F, 17.25F).allFaces((d, f) -> f.texture("#side").uvs(6, 0, 10, 1)).end();
            m.element().from(6, 14, 16.25F).to(10, 15.5F, 17.25F).allFaces((d, f) -> f.texture("#side").uvs(6, 0, 10, 1)).end();
            getVariantBuilder(ModBlocks.CLIP_FEEDER.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(m)
                    .rotationY(((int) state.getValue(org.overengineer.bettacraft.equipment.ClipFeederBlock.FACING).toYRot() + 180) % 360).build());
        }

        /** M9: thin glass lid with an iron frame, lying on the block below. */
        private void tankLid() {
            BlockModelBuilder m = models().getBuilder("tank_lid").renderType("cutout")
                    .texture("glass", mcLoc("block/glass")).texture("frame", mcLoc("block/iron_block")).texture("particle", mcLoc("block/glass"));
            m.element().from(0, 0, 0).to(16, 1, 16)
                    .face(Direction.UP).texture("#glass").end()
                    .face(Direction.DOWN).texture("#glass").cullface(Direction.DOWN).end()
                    .face(Direction.NORTH).uvs(0, 15, 16, 16).texture("#frame").end()
                    .face(Direction.SOUTH).uvs(0, 15, 16, 16).texture("#frame").end()
                    .face(Direction.EAST).uvs(0, 15, 16, 16).texture("#frame").end()
                    .face(Direction.WEST).uvs(0, 15, 16, 16).texture("#frame").end()
                    .end();
            // handle in the middle
            m.element().from(6, 1, 7.5F).to(10, 1.5F, 8.5F).allFaces((d, f) -> f.texture("#frame")).end();
            simpleBlock(ModBlocks.TANK_LID.get(), m);
        }

        /**
         * M9: larva tub (wooden staves, 1..15 x 0..10) and artemia hatchery (glass box on a grey base, 2..14 x 0..13).
         * The water surface rises with {@code water} (tinted water texture), {@code ready} shows the animals on it.
         */
        private void cultureTubs() {
            for (boolean hatchery : new boolean[]{false, true}) {
                var block = hatchery ? ModBlocks.ARTEMIA_HATCHERY.get() : ModBlocks.LARVA_TUB.get();
                String name = hatchery ? "artemia_hatchery" : "larva_tub";
                getVariantBuilder(block).forAllStates(state -> {
                    int w = state.getValue(org.overengineer.bettacraft.food.CultureTubBlock.WATER);
                    boolean ready = state.getValue(org.overengineer.bettacraft.food.CultureTubBlock.READY);
                    return ConfiguredModel.builder().modelFile(tubModel(name, hatchery, w, ready && w > 0)).build();
                });
            }
        }

        private ModelFile tubModel(String name, boolean hatchery, int water, boolean ready) {
            String id = name + (water > 0 ? "_water" + water : "") + (ready ? "_ready" : "");
            BlockModelBuilder m = models().getBuilder(id).renderType("translucent")
                    .texture("water", mcLoc("block/water_still"))
                    .texture("food", modLoc(hatchery ? "block/culture_nauplii" : "block/culture_larvae"));
            float x0 = hatchery ? 2 : 1, x1 = hatchery ? 14 : 15, base = hatchery ? 3 : 1, top = hatchery ? 13 : 10;
            if (hatchery) {
                m.texture("side", modLoc("block/artemia_hatchery_glass")).texture("base", modLoc("block/artemia_hatchery_base"))
                        .texture("particle", modLoc("block/artemia_hatchery_glass"));
                m.element().from(x0, 0, x0).to(x1, base, x1).allFaces((d, f) -> {
                    f.texture("#base");
                    if (d == Direction.DOWN) f.cullface(d);
                }).end();
            } else {
                m.texture("side", modLoc("block/larva_tub_side")).texture("base", modLoc("block/larva_tub_bottom"))
                        .texture("particle", modLoc("block/larva_tub_side"));
                m.element().from(x0 + 1, 0, x0 + 1).to(x1 - 1, base, x1 - 1).allFaces((d, f) -> {
                    f.texture("#base");
                    if (d == Direction.DOWN) f.cullface(d);
                }).end();
            }
            // four walls, 1 px thick
            float wy0 = hatchery ? base : 0;
            m.element().from(x0, wy0, x0).to(x1, top, x0 + 1).allFaces((d, f) -> f.texture("#side")).end();
            m.element().from(x0, wy0, x1 - 1).to(x1, top, x1).allFaces((d, f) -> f.texture("#side")).end();
            m.element().from(x0, wy0, x0 + 1).to(x0 + 1, top, x1 - 1).allFaces((d, f) -> f.texture("#side")).end();
            m.element().from(x1 - 1, wy0, x0 + 1).to(x1, top, x1 - 1).allFaces((d, f) -> f.texture("#side")).end();
            if (water > 0) {
                float h = base + (top - 1 - base) * water / 4F;
                m.element().from(x0 + 1, h - 0.01F, x0 + 1).to(x1 - 1, h, x1 - 1)
                        .face(Direction.UP).uvs(x0 + 1, x0 + 1, x1 - 1, x1 - 1).texture("#water").tintindex(0).end().end();
                if (ready) {
                    m.element().from(x0 + 1, h + 0.05F, x0 + 1).to(x1 - 1, h + 0.06F, x1 - 1)
                            .face(Direction.UP).uvs(x0 + 1, x0 + 1, x1 - 1, x1 - 1).texture("#food").end().end();
                }
            }
            return m;
        }

        private ModelFile lightModel(boolean neg, boolean pos, boolean hanging, boolean lit) {
            String ends = neg && pos ? "both" : neg ? "neg" : pos ? "pos" : "none";
            String name = "tank_light_" + ends + (hanging ? "_hanging" : "") + (lit ? "_on" : "_off");
            float a0 = neg ? 0 : 1, a1 = pos ? 16 : 15, y0 = hanging ? 14 : 0;
            return models().getBuilder(name)
                    .texture("housing", modLoc("block/tank_light"))
                    .texture("panel", modLoc(lit ? "block/tank_light_panel_on" : "block/tank_light_panel_off"))
                    .texture("particle", modLoc("block/tank_light"))
                    .element().from(a0, y0, 6.5F).to(a1, y0 + 2, 9.5F)
                    .face(Direction.DOWN).uvs(a0, 6.5F, a1, 9.5F).texture("#panel").tintindex(0).emissivity(lit ? 15 : 0, lit ? 15 : 0).end()
                    .face(Direction.UP).uvs(a0, 6.5F, a1, 9.5F).texture("#housing").end()
                    .face(Direction.NORTH).uvs(16 - a1, 14, 16 - a0, 16).texture("#housing").end()
                    .face(Direction.SOUTH).uvs(a0, 14, a1, 16).texture("#housing").end()
                    .face(Direction.WEST).uvs(6.5F, 14, 9.5F, 16).texture("#housing").end()
                    .face(Direction.EAST).uvs(6.5F, 14, 9.5F, 16).texture("#housing").end()
                    .end();
        }
    }

    /** M13: models of the aquarium plant blocks (grey textures, tinted by the block colour handler). */
    static String plantModel(org.overengineer.bettacraft.plants.PlantSpecies s, org.overengineer.bettacraft.plants.PlantPart look) {
        return s.id() + "_" + switch (look) {
            case LEAF, TOP -> "leaf";
            case ROOT -> "leaf";
            default -> look.getSerializedName();
        };
    }

    static final class ItemModels extends ItemModelProvider {
        ItemModels(PackOutput out, ExistingFileHelper efh) {
            super(out, BettaCraft.MOD_ID, efh);
        }

        @Override
        protected void registerModels() {
            withExistingParent("tank", modLoc("block/tank"));
            withExistingParent("feeder", modLoc("block/feeder"));
            withExistingParent("clip_feeder", modLoc("block/clip_feeder"));
            // M29.1: the light bar in 3D with its beam, panel and beam tinted in the light colour (tint 0)
            DataGenM291.lamp(this, "tank_light", "tank_light", "tank_light", 1, 15, 6.5F, 9.5F, 2);
            withExistingParent("external_filter", modLoc("block/external_filter"));
            DataGenM23.itemModels(this); // M23
            DataGenM24.itemModels(this); // M24
            DataGenM40.itemModels(this); // M40 part a
            DataGenM40b.itemModels(this); // M40 part b
            DataGenM40c.itemModels(this); // M40 part c
            DataGenM25.itemModels(this); // M25
            DataGenM25b.itemModels(this);
            DataGenM25c.itemModels(this);
            DataGenM26.itemModels(this); // M26 part a
            DataGenM37.itemModels(this); // M37
            DataGenM41.itemModels(this); // M41
            DataGenM27.itemModels(this); // M27
            DataGenM28.itemModels(this); // M28 part c
            DataGenM30.itemModels(this); // M30 part a
            DataGenM31.itemModels(this); // M31 part a
            DataGenM31b.itemModels(this); // M31 part b
            DataGenM31c.itemModels(this); // M31 part c
            DataGenM31d.itemModels(this); // M31 part d
            withExistingParent("active_soil", modLoc("block/active_soil")); // M25 part d
            withExistingParent("cabinet", modLoc("block/cabinet"));
            withExistingParent("tank_lid", modLoc("block/tank_lid"));
            withExistingParent("larva_tub", modLoc("block/larva_tub_water3"));
            withExistingParent("artemia_hatchery", modLoc("block/artemia_hatchery_water3"));
            // bucket and filled bag: fish overlay as layer1/layer2 (tinted via ItemColor)
            withExistingParent("betta_bucket", mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/betta_bucket")).texture("layer1", modLoc("item/betta_bucket_fish")).texture("layer2", modLoc("item/betta_bucket_fish_fins"))
                    .texture("layer3", modLoc("item/betta_bucket_glass")); // M21: the glass in front of the fish
            ModelFile filledBag = withExistingParent("fish_bag_filled", mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/fish_bag")).texture("layer1", modLoc("item/fish_bag_fish")).texture("layer2", modLoc("item/fish_bag_fish_fins"));
            withExistingParent("fish_bag", mcLoc("item/generated")).texture("layer0", modLoc("item/fish_bag"))
                    .override().predicate(BettaCraft.id("filled"), 1.0F).model(filledBag).end();
            withExistingParent("betta_spawn_egg", mcLoc("item/template_spawn_egg"));
            basicItem(ModItems.SWAMP_WATER_BOTTLE.get());
            basicItem(ModItems.BETTA_PELLETS.get());
            handheldItem(ModItems.MIRROR_STICK.get());
            handheldItem(ModItems.AQUARIUM_NET.get());
            handheldItem(ModItems.ALGAE_SCRAPER.get()); // M14
            basicItem(ModItems.FROG_BUCKET.get());
            withExistingParent("dwarf_frog_spawn_egg", mcLoc("item/template_spawn_egg"));
            for (var p : ModItems.FLOATING_PLANTS.values()) basicItem(p.get());
            basicItem(ModItems.MOSQUITO_LARVAE.get());
            basicItem(ModItems.SALT_WATER_BOTTLE.get());
            basicItem(ModItems.INFUSORIA_CULTURE.get());
            basicItem(ModItems.INFUSORIA.get());
            basicItem(ModItems.ARTEMIA_CULTURE.get());
            basicItem(ModItems.ARTEMIA_NAUPLII.get());
            basicItem(ModItems.ARTEMIA_CYSTS.get());
            // M29.1: 3D models of the whole device (taller by size)
            DataGenM291.tube(this, "heater_small", "heater_segment", 7);
            DataGenM291.tube(this, "heater_medium", "heater_segment", 9.5F);
            DataGenM291.tube(this, "heater_large", "heater_segment", 12);
            DataGenM291.tube(this, "cooler_small", "cooler_segment", 7);
            DataGenM291.tube(this, "cooler_medium", "cooler_segment", 9.5F);
            DataGenM291.tube(this, "cooler_large", "cooler_segment", 12);
            DataGenM291.filter(this, "internal_filter", "filter_segment_middle", "filter_segment_anchor", "filter_segment_top", "filter_segment_outlet");
            for (var d : ModItems.DECOR.values()) basicItem(d.get());
            // M19: tankmates (bucket with the betta's fish overlay, tinted per species), spawn eggs, breeding items
            for (var sp : org.overengineer.bettacraft.tankmates.TankmateSpecies.values()) {
                String fish = sp.seahorse() ? "item/seahorse_bucket_fish" : "item/betta_bucket_fish"; // M21.8: a seahorse in the bucket
                if (!sp.isLarge()) withExistingParent(sp.bucketId(), mcLoc("item/generated")) // M30: no bucket for a big animal
                        .texture("layer0", modLoc("item/betta_bucket")).texture("layer1", modLoc(fish))
                        .texture("layer2", modLoc(fish + "_fins")).texture("layer3", modLoc("item/betta_bucket_glass"));
                withExistingParent(sp.spawnEggId(), mcLoc("item/template_spawn_egg"));
            }
            basicItem(ModItems.BREEDING_BOX.get());
            basicItem(ModItems.FRY_POWDER.get());
            basicItem(ModItems.ALGAE_WAFER.get());
            withExistingParent("spawning_cave", modLoc("block/spawning_cave"));
            withExistingParent("spawning_mop", mcLoc("item/generated")).texture("layer0", modLoc("block/spawning_mop")); // crossed planes: flat
            // M20: reef tanks
            basicItem(ModItems.SEA_WATER_BUCKET.get());
            basicItem(ModItems.SEA_SALT.get());
            basicItem(ModItems.CALCIUM_ADDITIVE.get());
            DataGenM291.block(this, "flow_pump", "flow_pump_segment"); // M29.1: 3D
            withExistingParent("protein_skimmer", modLoc("block/protein_skimmer"));
            withExistingParent("reef_rock", modLoc("block/reef_rock"));
            withExistingParent("live_rock", modLoc("block/live_rock"));
            // M22.3: the critter bucket (a shrimp in the water, tinted per animal), the magnifier, the wild corals
            withExistingParent("critter_bucket", mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/betta_bucket")).texture("layer1", modLoc("item/critter_bucket_animal"))
                    .texture("layer2", modLoc("item/critter_bucket_animal_legs")).texture("layer3", modLoc("item/betta_bucket_glass"));
            handheldItem(ModItems.MAGNIFIER.get());
            for (var e : ModItems.WILD_CORALS.entrySet()) {
                String id = "wild_" + e.getKey().id();
                if ("cross".equals(org.overengineer.bettacraft.reef.WildCoralBlock.shape(e.getKey()))) {
                    withExistingParent(id, mcLoc("item/generated")).texture("layer0", modLoc("block/" + id));
                } else {
                    withExistingParent(id, modLoc("block/" + id));
                }
            }
            // M13: aquarium plants: two grey layers per growth form, tinted per species (ItemColor)
            for (var e : ModItems.AQUATIC_PLANTS.entrySet()) {
                String form = e.getKey().form().name().toLowerCase(java.util.Locale.ROOT);
                withExistingParent(e.getKey().id(), mcLoc("item/generated")).texture("layer0", modLoc("item/plant_" + form))
                        .texture("layer1", modLoc("item/plant_" + form + "_detail"));
            }
            DataGenM291.blockDisplays(this, generatedModels); // M29.1: block items in 3D
        }
    }

    // ------------------------------------------------------------------ recipes

    static final class Recipes extends RecipeProvider {
        Recipes(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup) {
            super(out, lookup);
        }

        @Override
        protected void buildRecipes(RecipeOutput raw) {
            RecipeOutput output = featureGated(raw); // M33: every recipe only while its features are on
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.TANK.get(), 4)
                    .pattern("GGG")
                    .pattern("GIG")
                    .pattern("III")
                    .define('G', Tags.Items.GLASS_BLOCKS)
                    .define('I', Tags.Items.INGOTS_IRON)
                    .unlockedBy("has_glass", has(Tags.Items.GLASS_BLOCKS))
                    .unlockedBy("has_betta", has(ModItems.BETTA_BUCKET.get()))
                    .save(asTankRecipe(output), BettaCraft.id("tank"));

            // M11: the clean tank (clear vanilla glass without texture): iron nugget in the middle instead of the ingot
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, org.overengineer.bettacraft.tank.TankBlockItem.of(ModItems.TANK.get(),
                            org.overengineer.bettacraft.tank.TankBlockEntity.DEFAULT_GLASS, org.overengineer.bettacraft.tank.TankStyle.CLEAN, 4))
                    .pattern("GGG")
                    .pattern("GNG")
                    .pattern("III")
                    .define('G', Items.GLASS)
                    .define('N', Tags.Items.NUGGETS_IRON)
                    .define('I', Tags.Items.INGOTS_IRON)
                    .unlockedBy("has_glass", has(Items.GLASS))
                    .unlockedBy("has_tank", has(ModItems.TANK.get()))
                    .unlockedBy("has_betta", has(ModItems.BETTA_BUCKET.get()))
                    .save(output, BettaCraft.id("tank_clean"));
            // M11: one vanilla-glass tank converts between textured and clean (both directions)
            for (var from : org.overengineer.bettacraft.tank.TankStyle.values()) {
                var to = from == org.overengineer.bettacraft.tank.TankStyle.CLEAN ? org.overengineer.bettacraft.tank.TankStyle.TEXTURED
                        : org.overengineer.bettacraft.tank.TankStyle.CLEAN;
                ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, org.overengineer.bettacraft.tank.TankBlockItem.of(ModItems.TANK.get(),
                                org.overengineer.bettacraft.tank.TankBlockEntity.DEFAULT_GLASS, to, 1))
                        .requires(net.neoforged.neoforge.common.crafting.DataComponentIngredient.of(false,
                                net.minecraft.core.component.DataComponentPredicate.builder()
                                        .expect(ModDataComponents.GLASS.get(), org.overengineer.bettacraft.tank.TankBlockEntity.DEFAULT_GLASS)
                                        .expect(ModDataComponents.TANK_STYLE.get(), from).build(), ModItems.TANK.get()))
                        .group("bettacraft:tank_style")
                        .unlockedBy("has_tank", has(ModItems.TANK.get()))
                        .save(asStyleRecipe(output, from), BettaCraft.id("tank_" + from.getSerializedName() + "_to_" + to.getSerializedName()));
            }
            // M11: mirror stick (optional-features #2): a glass pane silvered with an iron nugget on a stick
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.MIRROR_STICK.get())
                    .pattern("P")
                    .pattern("N")
                    .pattern("S")
                    .define('P', Tags.Items.GLASS_PANES)
                    .define('N', Tags.Items.NUGGETS_IRON)
                    .define('S', Tags.Items.RODS_WOODEN)
                    .unlockedBy("has_betta", has(ModItems.BETTA_BUCKET.get()))
                    .unlockedBy("has_tank", has(ModItems.TANK.get()))
                    .save(output, BettaCraft.id("mirror_stick"));

            // M12: aquarium net: a string mesh on a stick
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.AQUARIUM_NET.get())
                    .pattern(" TT")
                    .pattern(" TT")
                    .pattern("S  ")
                    .define('T', Tags.Items.STRINGS)
                    .define('S', Tags.Items.RODS_WOODEN)
                    .unlockedBy("has_tank", has(ModItems.TANK.get()))
                    .unlockedBy("has_plant", has(ModItems.FLOATING_PLANTS.get(org.overengineer.bettacraft.tank.plants.FloatingPlantType.DUCKWEED).get()))
                    .save(output, BettaCraft.id("tank_net"));

            // M14: algae scraper: an iron blade on a stick
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.ALGAE_SCRAPER.get())
                    .pattern("  I")
                    .pattern(" S ")
                    .pattern("S  ")
                    .define('I', Tags.Items.INGOTS_IRON)
                    .define('S', Tags.Items.RODS_WOODEN)
                    .unlockedBy("has_tank", has(ModItems.TANK.get()))
                    .save(output, BettaCraft.id("algae_scraper"));

            // Fish bag (PLAN 4.3): kelp unless #bettacraft:plastic_sheets has entries; COMMON config can force one.
            ICondition plasticEmpty = new TagEmptyCondition(ModTags.PLASTIC_SHEETS);
            ICondition kelp = new OrCondition(List.of(new FishBagRecipeCondition("kelp"),
                    new AndCondition(List.of(new FishBagRecipeCondition("auto"), plasticEmpty))));
            ICondition plastic = new OrCondition(List.of(new FishBagRecipeCondition("plastic"),
                    new AndCondition(List.of(new FishBagRecipeCondition("auto"), new NotCondition(plasticEmpty)))));
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.FISH_BAG.get(), 2)
                    .pattern(" S ").pattern("K K").pattern(" K ")
                    .define('S', Tags.Items.STRINGS).define('K', Items.DRIED_KELP)
                    .unlockedBy("has_kelp", has(Items.DRIED_KELP))
                    .unlockedBy("has_betta", has(ModItems.BETTA_BUCKET.get()))
                    .save(output.withConditions(kelp), BettaCraft.id("fish_bag_kelp"));
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.FISH_BAG.get(), 4)
                    .pattern(" S ").pattern("P P").pattern(" P ")
                    .define('S', Tags.Items.STRINGS).define('P', ModTags.PLASTIC_SHEETS)
                    .unlockedBy("has_plastic", has(ModTags.PLASTIC_SHEETS))
                    .unlockedBy("has_betta", has(ModItems.BETTA_BUCKET.get()))
                    .save(output.withConditions(plastic), BettaCraft.id("fish_bag_plastic"));

            ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.BETTA_PELLETS.get(), 8)
                    .requires(ModTags.PELLET_FISH)
                    .requires(ModItems.MOSQUITO_LARVAE.get())
                    .requires(Items.BONE_MEAL)
                    .requires(Items.DRIED_KELP)
                    .unlockedBy("has_larvae", has(ModItems.MOSQUITO_LARVAE.get()))
                    .unlockedBy("has_betta", has(ModItems.BETTA_BUCKET.get()))
                    .save(output, BettaCraft.id("betta_pellets"));

            Ingredient swamp = Ingredient.of(ModItems.SWAMP_WATER_BOTTLE.get());
            SimpleCookingRecipeBuilder.smelting(swamp, RecipeCategory.FOOD, ModItems.MOSQUITO_LARVAE.get(), 0.1F, 200)
                    .unlockedBy("has_swamp_water", has(ModItems.SWAMP_WATER_BOTTLE.get())).save(output, BettaCraft.id("mosquito_larvae_smelting"));
            SimpleCookingRecipeBuilder.smoking(swamp, RecipeCategory.FOOD, ModItems.MOSQUITO_LARVAE.get(), 0.1F, 100)
                    .unlockedBy("has_swamp_water", has(ModItems.SWAMP_WATER_BOTTLE.get())).save(output, BettaCraft.id("mosquito_larvae_smoking"));
            SimpleCookingRecipeBuilder.campfireCooking(swamp, RecipeCategory.FOOD, ModItems.MOSQUITO_LARVAE.get(), 0.1F, 600)
                    .unlockedBy("has_swamp_water", has(ModItems.SWAMP_WATER_BOTTLE.get())).save(output, BettaCraft.id("mosquito_larvae_campfire"));

            // fry food (M2): cultures become ready after a day (CultureItem)
            ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.INFUSORIA_CULTURE.get())
                    .requires(ModItems.SWAMP_WATER_BOTTLE.get()).requires(Items.SEAGRASS)
                    .unlockedBy("has_swamp_water", has(ModItems.SWAMP_WATER_BOTTLE.get()))
                    .save(output, BettaCraft.id("infusoria_culture_seagrass"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.INFUSORIA_CULTURE.get())
                    .requires(ModItems.SWAMP_WATER_BOTTLE.get()).requires(net.minecraft.tags.ItemTags.LEAVES)
                    .unlockedBy("has_swamp_water", has(ModItems.SWAMP_WATER_BOTTLE.get()))
                    .save(output, BettaCraft.id("infusoria_culture_leaves"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.ARTEMIA_CULTURE.get())
                    .requires(ModItems.SALT_WATER_BOTTLE.get()).requires(ModItems.ARTEMIA_CYSTS.get())
                    .unlockedBy("has_cysts", has(ModItems.ARTEMIA_CYSTS.get()))
                    .save(output, BettaCraft.id("artemia_culture"));

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.HEATER_SMALL.get())
                    .pattern("C").pattern("R").pattern("G")
                    .define('C', Tags.Items.INGOTS_COPPER).define('R', Tags.Items.DUSTS_REDSTONE).define('G', Tags.Items.GLASS_PANES)
                    .unlockedBy("has_copper", has(Tags.Items.INGOTS_COPPER))
                    .unlockedBy("has_betta", has(ModItems.BETTA_BUCKET.get()))
                    .save(output, BettaCraft.id("heater_small"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.HEATER_MEDIUM.get())
                    .requires(ModItems.HEATER_SMALL.get(), 2).requires(Tags.Items.DUSTS_REDSTONE)
                    .unlockedBy("has_heater", has(ModItems.HEATER_SMALL.get()))
                    .save(output, BettaCraft.id("heater_medium"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.HEATER_LARGE.get())
                    .requires(ModItems.HEATER_MEDIUM.get(), 2).requires(Tags.Items.DUSTS_REDSTONE)
                    .unlockedBy("has_heater", has(ModItems.HEATER_MEDIUM.get()))
                    .save(output, BettaCraft.id("heater_large"));

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FEEDER.get())
                    .pattern(" I ").pattern("IHI")
                    .define('I', Tags.Items.INGOTS_IRON).define('H', Items.HOPPER)
                    .unlockedBy("has_hopper", has(Items.HOPPER))
                    .unlockedBy("has_betta", has(ModItems.BETTA_BUCKET.get()))
                    .save(output, BettaCraft.id("feeder"));
            // M21.5: the clip-on feeder: iron, a glass window, the clip from nuggets
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CLIP_FEEDER.get())
                    .pattern("NIN").pattern(" G ")
                    .define('N', Tags.Items.NUGGETS_IRON).define('I', Tags.Items.INGOTS_IRON).define('G', Tags.Items.GLASS_PANES)
                    .unlockedBy("has_feeder", has(ModItems.FEEDER.get()))
                    .unlockedBy("has_betta", has(ModItems.BETTA_BUCKET.get()))
                    .save(output, BettaCraft.id("clip_feeder"));
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.TANK_LIGHT.get(), 3)
                    .pattern("III").pattern("GDG")
                    .define('I', Tags.Items.INGOTS_IRON).define('G', Tags.Items.GLASS_PANES).define('D', Tags.Items.DUSTS_GLOWSTONE)
                    .unlockedBy("has_glowstone", has(Tags.Items.DUSTS_GLOWSTONE))
                    .unlockedBy("has_betta", has(ModItems.BETTA_BUCKET.get()))
                    .save(output, BettaCraft.id("tank_light"));
            // M18: coloured lights: one light (any colour) + dye, or eight lights around a dye (like stained glass)
            for (net.minecraft.world.item.DyeColor c : net.minecraft.world.item.DyeColor.values()) {
                net.minecraft.world.item.Item dye = net.minecraft.world.item.DyeItem.byColor(c);
                ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, org.overengineer.bettacraft.equipment.TankLightItem.of(ModItems.TANK_LIGHT.get(), c, 1))
                        .requires(ModItems.TANK_LIGHT.get()).requires(dye)
                        .group("bettacraft:tank_light_dye")
                        .unlockedBy("has_light", has(ModItems.TANK_LIGHT.get()))
                        .save(output, BettaCraft.id("tank_light_" + c.getName()));
                ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, org.overengineer.bettacraft.equipment.TankLightItem.of(ModItems.TANK_LIGHT.get(), c, 8))
                        .pattern("LLL").pattern("LDL").pattern("LLL")
                        .define('L', ModItems.TANK_LIGHT.get()).define('D', dye)
                        .group("bettacraft:tank_light_dye8")
                        .unlockedBy("has_light", has(ModItems.TANK_LIGHT.get()))
                        .save(output, BettaCraft.id("tank_light_" + c.getName() + "_8"));
            }
            decorRecipes(output);

            // M9: coolers (snow instead of copper, sizes like the heaters), lid, live food breeders
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COOLER_SMALL.get())
                    .pattern("S").pattern("R").pattern("G")
                    .define('S', Items.SNOWBALL).define('R', Tags.Items.DUSTS_REDSTONE).define('G', Tags.Items.GLASS_PANES)
                    .unlockedBy("has_heater", has(ModItems.HEATER_SMALL.get()))
                    .unlockedBy("has_snowball", has(Items.SNOWBALL))
                    .save(output, BettaCraft.id("cooler_small"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.COOLER_MEDIUM.get())
                    .requires(ModItems.COOLER_SMALL.get(), 2).requires(Tags.Items.DUSTS_REDSTONE)
                    .unlockedBy("has_cooler", has(ModItems.COOLER_SMALL.get()))
                    .save(output, BettaCraft.id("cooler_medium"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.COOLER_LARGE.get())
                    .requires(ModItems.COOLER_MEDIUM.get(), 2).requires(Tags.Items.DUSTS_REDSTONE)
                    .unlockedBy("has_cooler", has(ModItems.COOLER_MEDIUM.get()))
                    .save(output, BettaCraft.id("cooler_large"));
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.TANK_LID.get(), 3)
                    .pattern("N N").pattern("PPP")
                    .define('N', Tags.Items.NUGGETS_IRON).define('P', Tags.Items.GLASS_PANES)
                    .unlockedBy("has_tank", has(ModItems.TANK.get()))
                    .save(output, BettaCraft.id("tank_lid"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.LARVA_TUB.get())
                    .pattern("P P").pattern("PIP").pattern("SSS")
                    .define('P', net.minecraft.tags.ItemTags.PLANKS).define('I', Tags.Items.NUGGETS_IRON)
                    .define('S', net.minecraft.tags.ItemTags.WOODEN_SLABS)
                    .unlockedBy("has_larvae", has(ModItems.MOSQUITO_LARVAE.get()))
                    .unlockedBy("has_swamp_water", has(ModItems.SWAMP_WATER_BOTTLE.get()))
                    .save(output, BettaCraft.id("larva_tub"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ARTEMIA_HATCHERY.get())
                    .pattern("G G").pattern("G G").pattern("IRI")
                    .define('G', Tags.Items.GLASS_BLOCKS).define('I', Tags.Items.INGOTS_IRON).define('R', Tags.Items.DUSTS_REDSTONE)
                    .unlockedBy("has_cysts", has(ModItems.ARTEMIA_CYSTS.get()))
                    .unlockedBy("has_salt_water", has(ModItems.SALT_WATER_BOTTLE.get()))
                    .save(output, BettaCraft.id("artemia_hatchery"));

            // M4: external filter and the cabinet of any planks
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EXTERNAL_FILTER.get())
                    .pattern("KHK").pattern("IWI").pattern("IRI")
                    .define('K', Items.DRIED_KELP).define('H', Items.HOPPER).define('I', Tags.Items.INGOTS_IRON)
                    .define('W', net.minecraft.tags.ItemTags.WOOL).define('R', Tags.Items.DUSTS_REDSTONE)
                    .unlockedBy("has_hopper", has(Items.HOPPER))
                    .save(output, BettaCraft.id("external_filter"));
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.CABINET.get())
                    .pattern("SSS").pattern("PCP").pattern("PPP")
                    .define('S', net.minecraft.tags.ItemTags.WOODEN_SLABS).define('P', net.minecraft.tags.ItemTags.PLANKS)
                    .define('C', Tags.Items.CHESTS_WOODEN)
                    .unlockedBy("has_planks", has(net.minecraft.tags.ItemTags.PLANKS))
                    .save(as(output, org.overengineer.bettacraft.equipment.CabinetRecipe::of), BettaCraft.id("cabinet"));
            // M21.7: the cabinet of stone; the ingredient lists hold the vanilla stones and their slabs in the same order
            // (JEI, the recipe book and the handbook cycle through matching pairs), the recipe itself checks any stone
            var stones = org.overengineer.bettacraft.equipment.CabinetStones.VANILLA;
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.CABINET.get())
                    .pattern("SSS").pattern("PCP").pattern("PPP")
                    .define('S', net.minecraft.world.item.crafting.Ingredient.of(stones.stream().map(p -> p.slab()).toArray(net.minecraft.world.level.ItemLike[]::new)))
                    .define('P', net.minecraft.world.item.crafting.Ingredient.of(stones.stream().map(p -> p.stone()).toArray(net.minecraft.world.level.ItemLike[]::new)))
                    .define('C', Tags.Items.CHESTS_WOODEN)
                    .unlockedBy("has_cobblestone", has(Items.COBBLESTONE))
                    .save(as(output, org.overengineer.bettacraft.equipment.StoneCabinetRecipe::of), BettaCraft.id("stone_cabinet"));
        }

        /**
         * M33: adds a {@code bettacraft:feature} condition per feature of the recipe ({@link
         * org.overengineer.bettacraft.config.FeatureContent#ofRecipe}: its result, its type, a few by id) to every recipe.
         */
        private static RecipeOutput featureGated(RecipeOutput parent) {
            return new RecipeOutput() {
                @Override
                public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
                    String result = null;
                    try {
                        var stack = recipe.getResultItem(net.minecraft.core.RegistryAccess.EMPTY);
                        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
                        if (!stack.isEmpty() && key.getNamespace().equals(BettaCraft.MOD_ID)) result = key.getPath();
                    } catch (RuntimeException ignored) {
                        // a recipe without a fixed result
                    }
                    var type = net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
                    List<ICondition> all = new java.util.ArrayList<>(List.of(conditions));
                    for (var f : org.overengineer.bettacraft.config.FeatureContent.ofRecipe(id.getPath(), result, type == null ? null : type.toString())) {
                        all.add(new org.overengineer.bettacraft.config.FeatureCondition(f));
                    }
                    parent.accept(id, recipe, advancement, all.toArray(ICondition[]::new));
                }

                @Override
                public Advancement.Builder advancement() {
                    return parent.advancement();
                }
            };
        }

        /** Turns the ShapedRecipe produced by the builder into one of our ShapedRecipe subclasses. */
        private static RecipeOutput as(RecipeOutput parent, java.util.function.Function<ShapedRecipe, Recipe<?>> wrap) {
            return new RecipeOutput() {
                @Override
                public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
                    parent.accept(id, wrap.apply((ShapedRecipe) recipe), advancement, conditions);
                }

                @Override
                public Advancement.Builder advancement() {
                    return parent.advancement();
                }
            };
        }

        /** M3: internal filter and the decoration structures (own, plausible recipes). */
        private void decorRecipes(RecipeOutput output) {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.INTERNAL_FILTER.get())
                    .requires(Tags.Items.INGOTS_IRON).requires(Tags.Items.INGOTS_IRON).requires(net.minecraft.tags.ItemTags.WOOL)
                    .requires(Tags.Items.DUSTS_REDSTONE).requires(Items.IRON_BARS)
                    .unlockedBy("has_iron_bars", has(Items.IRON_BARS)).save(output, BettaCraft.id("internal_filter"));
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, decor(DecorType.CASTLE))
                    .pattern("W W").pattern("BIB").pattern("BBB")
                    .define('W', Items.STONE_BRICK_WALL).define('B', Items.STONE_BRICKS).define('I', Items.IRON_BARS)
                    .unlockedBy("has_stone_bricks", has(Items.STONE_BRICKS)).save(output, BettaCraft.id("decor_castle"));
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, decor(DecorType.SHIPWRECK))
                    .pattern(" SW").pattern("PBP").pattern(" P ")
                    .define('S', Tags.Items.RODS_WOODEN).define('W', net.minecraft.tags.ItemTags.WOOL).define('P', net.minecraft.tags.ItemTags.PLANKS)
                    .define('B', Items.BARREL)
                    .unlockedBy("has_barrel", has(Items.BARREL)).save(output, BettaCraft.id("decor_shipwreck"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, decor(DecorType.DINO_SKULL))
                    .requires(Items.BONE_BLOCK).requires(Items.BONE_BLOCK).requires(Items.BONE).requires(Items.POLISHED_DIORITE_SLAB)
                    .unlockedBy("has_bone_block", has(Items.BONE_BLOCK)).save(output, BettaCraft.id("decor_dino_skull"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, decor(DecorType.VOLCANO))
                    .requires(Items.BASALT).requires(Items.BASALT).requires(Items.BLACKSTONE).requires(Items.MAGMA_BLOCK)
                    .unlockedBy("has_magma", has(Items.MAGMA_BLOCK)).save(output, BettaCraft.id("decor_volcano"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, decor(DecorType.DRIFTWOOD), 2)
                    .requires(net.minecraft.tags.ItemTags.LOGS).requires(net.minecraft.tags.ItemTags.LOGS).requires(Items.MANGROVE_ROOTS)
                    .unlockedBy("has_mangrove_roots", has(Items.MANGROVE_ROOTS)).save(output, BettaCraft.id("decor_driftwood"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, decor(DecorType.STONES), 2)
                    .requires(Items.COBBLESTONE).requires(Items.ANDESITE).requires(Items.MOSSY_COBBLESTONE)
                    .unlockedBy("has_mossy_cobblestone", has(Items.MOSSY_COBBLESTONE)).save(output, BettaCraft.id("decor_stones"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, decor(DecorType.MOSS_BALL), 4)
                    .requires(Items.MOSS_BLOCK).requires(Items.SLIME_BALL)
                    .unlockedBy("has_moss", has(Items.MOSS_BLOCK)).save(output, BettaCraft.id("decor_moss_ball"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, decor(DecorType.HAMMOCK), 2)
                    .requires(Items.OAK_LEAVES).requires(Items.OAK_FENCE).requires(Tags.Items.STRINGS)
                    .unlockedBy("has_leaves", has(Items.OAK_LEAVES)).save(output, BettaCraft.id("decor_hammock"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, decor(DecorType.ALMOND_LEAF), 2)
                    .requires(net.minecraft.tags.ItemTags.LEAVES).requires(Items.BROWN_DYE)
                    .unlockedBy("has_brown_dye", has(Items.BROWN_DYE)).save(output, BettaCraft.id("decor_almond_leaf"));
            // M19: spawning cave (fired clay tube) and spawning mop (green wool on a stone)
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, decor(DecorType.SPAWNING_CAVE))
                    .pattern("CCC").pattern("C  ").pattern("CCC")
                    .define('C', ModItems.SPAWNING_CAVE.get())
                    .unlockedBy("has_spawning_cave", has(ModItems.SPAWNING_CAVE.get())).save(output, BettaCraft.id("decor_spawning_cave"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, decor(DecorType.SPAWNING_MOP))
                    .requires(ModItems.SPAWNING_MOP.get()).requires(ModItems.SPAWNING_MOP.get()).requires(ModItems.SPAWNING_MOP.get())
                    .requires(Items.COBBLESTONE)
                    .unlockedBy("has_spawning_mop", has(ModItems.SPAWNING_MOP.get())).save(output, BettaCraft.id("decor_spawning_mop"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModItems.SPAWNING_CAVE.get(), 8)
                    .requires(Items.TERRACOTTA).requires(Items.TERRACOTTA).requires(Items.CLAY_BALL)
                    .unlockedBy("has_terracotta", has(Items.TERRACOTTA)).save(output, BettaCraft.id("spawning_cave"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, ModItems.SPAWNING_MOP.get(), 4)
                    .requires(Items.GREEN_WOOL).requires(Tags.Items.STRINGS)
                    .unlockedBy("has_green_wool", has(Items.GREEN_WOOL)).save(output, BettaCraft.id("spawning_mop"));
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.BREEDING_BOX.get())
                    .pattern("P P").pattern("PSP").pattern("PPP")
                    .define('P', Tags.Items.GLASS_PANES).define('S', Tags.Items.STRINGS)
                    .unlockedBy("has_glass_pane", has(Tags.Items.GLASS_PANES)).save(output, BettaCraft.id("breeding_box"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.FRY_POWDER.get(), 3)
                    .requires(ModItems.BETTA_PELLETS.get()).requires(ModItems.BETTA_PELLETS.get()).requires(Items.BONE_MEAL)
                    .unlockedBy("has_pellets", has(ModItems.BETTA_PELLETS.get())).save(output, BettaCraft.id("fry_powder"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.ALGAE_WAFER.get(), 4)
                    .requires(Items.DRIED_KELP).requires(Items.DRIED_KELP).requires(ModItems.BETTA_PELLETS.get())
                    .unlockedBy("has_dried_kelp", has(Items.DRIED_KELP)).save(output, BettaCraft.id("algae_wafer"));
            reefRecipes(output);
            DataGenM23.recipes(output, hasIt(ModItems.INTERNAL_FILTER.get()), hasIt(ModItems.TANK_LIGHT.get())); // M23
            DataGenM24.recipes(output, hasIt(ModItems.INTERNAL_FILTER.get())); // M24
            DataGenM40.recipes(output, hasIt(ModItems.FILTER_PUMP_HEAD.get())); // M40 part a
            DataGenM40b.recipes(output, hasIt(ModItems.FILTER_PUMP_HEAD.get())); // M40 part b
            DataGenM40c.recipes(output, hasIt(ModItems.FILTER_INLET_PIPE.get())); // M40 part c
            DataGenM25.recipes(output, hasIt(ModItems.TANK.get())); // M25
            DataGenM25b.recipes(output, hasIt(ModItems.TANK.get()));
            DataGenM25c.recipes(output, hasIt(ModItems.TANK.get()));
            DataGenM26.recipes(output, hasIt(ModItems.TANK.get())); // M26 part a
            DataGenM37.recipes(output, hasIt(ModItems.TANK.get())); // M37
            DataGenM27.recipes(output, hasIt(ModItems.TANK.get())); // M27
            DataGenM28.recipes(output, hasIt(ModItems.TANK.get())); // M28 part c
            DataGenM30.recipes(output, hasIt(ModItems.TANK.get())); // M30 part a
            DataGenM31.recipes(output, hasIt(ModItems.TANK.get())); // M31 part a
            DataGenM31b.recipes(output, hasIt(ModItems.TANK.get())); // M31 part b
            DataGenM31c.recipes(output, hasIt(ModItems.TANK.get())); // M31 part c
            DataGenM31d.recipes(output, hasIt(ModItems.TANK.get())); // M31 part d
            // M25 part d: active soil from clay, mud and filter peat (humic acids)
            ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.ACTIVE_SOIL.get(), 8)
                    .pattern("CMC").pattern("MPM").pattern("CMC")
                    .define('C', Items.CLAY_BALL).define('M', Items.MUD).define('P', ModItems.FILTER_PEAT.get())
                    .unlockedBy("has_tank", hasIt(ModItems.TANK.get())).save(output, BettaCraft.id("active_soil"));
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, decor(DecorType.CAVE))
                    .pattern("TMT").pattern("T T")
                    .define('T', Items.TUFF).define('M', Items.MOSS_BLOCK)
                    .unlockedBy("has_tuff", has(Items.TUFF)).save(output, BettaCraft.id("decor_cave"));
        }

        private static net.minecraft.world.item.Item decor(DecorType t) {
            return ModItems.DECOR.get(t).get();
        }

        /** Turns the ShapedRecipe produced by the builder into our {@link TankRecipe} type. */
        private static RecipeOutput asStyleRecipe(RecipeOutput parent, org.overengineer.bettacraft.tank.TankStyle from) {
            return new RecipeOutput() {
                @Override
                public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
                    parent.accept(id, new org.overengineer.bettacraft.tank.TankStyleRecipe((net.minecraft.world.item.crafting.ShapelessRecipe) recipe, from),
                            advancement, conditions);
                }

                @Override
                public Advancement.Builder advancement() {
                    return parent.advancement();
                }
            };
        }

        private static RecipeOutput asTankRecipe(RecipeOutput parent) {
            return new RecipeOutput() {
                @Override
                public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
                    parent.accept(id, TankRecipe.of((ShapedRecipe) recipe), advancement, conditions);
                }

                @Override
                public Advancement.Builder advancement() {
                    return parent.advancement();
                }
            };
        }
    }

    // ------------------------------------------------------------------ loot

    /** M20: salt, sea water, calcium, skimmer, pump, rock and coral frags from vanilla corals. */
    static void reefRecipes(RecipeOutput output) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.SEA_SALT.get())
                .requires(ModItems.SEA_WATER_BUCKET.get())
                .unlockedBy("has_sea_water", hasIt(ModItems.SEA_WATER_BUCKET.get())).save(output, BettaCraft.id("sea_salt"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.SEA_WATER_BUCKET.get())
                .requires(Items.WATER_BUCKET).requires(ModItems.SEA_SALT.get())
                .unlockedBy("has_sea_salt", hasIt(ModItems.SEA_SALT.get())).save(output, BettaCraft.id("sea_water_bucket"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.CALCIUM_ADDITIVE.get(), 4)
                .requires(Items.CALCITE).requires(ModItems.SEA_SALT.get())
                .unlockedBy("has_sea_salt", hasIt(ModItems.SEA_SALT.get())).save(output, BettaCraft.id("calcium_additive"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.CALCIUM_ADDITIVE.get(), 2)
                .requires(Items.BONE_MEAL).requires(Items.BONE_MEAL).requires(Items.BONE_MEAL).requires(ModItems.SEA_SALT.get())
                .unlockedBy("has_sea_salt", hasIt(ModItems.SEA_SALT.get())).save(output, BettaCraft.id("calcium_additive_from_bone_meal"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PROTEIN_SKIMMER.get())
                .pattern("GIG").pattern("G G").pattern("IRI")
                .define('G', Tags.Items.GLASS_PANES).define('I', Tags.Items.INGOTS_IRON).define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_sea_salt", hasIt(ModItems.SEA_SALT.get())).save(output, BettaCraft.id("protein_skimmer"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FLOW_PUMP.get())
                .pattern(" I ").pattern("IRI").pattern(" K ")
                .define('I', Tags.Items.INGOTS_IRON).define('R', Tags.Items.DUSTS_REDSTONE).define('K', Items.BLACK_DYE)
                .unlockedBy("has_sea_salt", hasIt(ModItems.SEA_SALT.get())).save(output, BettaCraft.id("flow_pump"));
        // M22.3: the magnifier: a glass pane in a ring of copper (or gold) on a stick
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.MAGNIFIER.get())
                .pattern(" C ").pattern("CGC").pattern(" S ")
                .define('C', Tags.Items.INGOTS_COPPER).define('G', Tags.Items.GLASS_PANES).define('S', Tags.Items.RODS_WOODEN)
                .unlockedBy("has_copper", hasIt(Items.COPPER_INGOT)).save(output, BettaCraft.id("magnifier"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.MAGNIFIER.get())
                .pattern(" C ").pattern("CGC").pattern(" S ")
                .define('C', Tags.Items.INGOTS_GOLD).define('G', Tags.Items.GLASS_PANES).define('S', Tags.Items.RODS_WOODEN)
                .unlockedBy("has_gold", hasIt(Items.GOLD_INGOT)).save(output, BettaCraft.id("magnifier_from_gold"));
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.REEF_ROCK.get(), 4)
                .pattern("CS").pattern("SC")
                .define('C', Items.CALCITE).define('S', net.minecraft.tags.ItemTags.SAND)
                .unlockedBy("has_calcite", hasIt(Items.CALCITE)).save(output, BettaCraft.id("reef_rock"));
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.LIVE_ROCK.get(), 8)
                .pattern("RRR").pattern("RCR").pattern("RRR")
                .define('R', ModItems.REEF_ROCK.get())
                .define('C', net.minecraft.world.item.crafting.Ingredient.of(Items.TUBE_CORAL, Items.BRAIN_CORAL, Items.BUBBLE_CORAL, Items.FIRE_CORAL,
                        Items.HORN_CORAL, Items.TUBE_CORAL_FAN, Items.BRAIN_CORAL_FAN, Items.BUBBLE_CORAL_FAN, Items.FIRE_CORAL_FAN, Items.HORN_CORAL_FAN))
                .unlockedBy("has_reef_rock", hasIt(ModItems.REEF_ROCK.get())).save(output, BettaCraft.id("live_rock"));
        // frags from the corals of the ocean: tube -> star polyps, brain -> Lobophyllia, bubble -> hammer, fire -> Acropora, horn -> leather
        var map = new java.util.LinkedHashMap<net.minecraft.world.item.Item, org.overengineer.bettacraft.plants.PlantSpecies>();
        map.put(Items.TUBE_CORAL, org.overengineer.bettacraft.plants.PlantSpecies.GREEN_STAR_POLYPS);
        map.put(Items.BRAIN_CORAL, org.overengineer.bettacraft.plants.PlantSpecies.LOBOPHYLLIA);
        map.put(Items.BUBBLE_CORAL, org.overengineer.bettacraft.plants.PlantSpecies.HAMMER_CORAL);
        map.put(Items.FIRE_CORAL, org.overengineer.bettacraft.plants.PlantSpecies.ACROPORA);
        map.put(Items.HORN_CORAL, org.overengineer.bettacraft.plants.PlantSpecies.LEATHER_CORAL);
        for (var e : map.entrySet()) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.getKey()).getPath();
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.AQUATIC_PLANTS.get(e.getValue()).get(), 2)
                    .requires(e.getKey()).requires(ModItems.REEF_ROCK.get())
                    .unlockedBy("has_" + id, hasIt(e.getKey())).save(output, BettaCraft.id("frag_from_" + id));
        }
    }

    private static net.minecraft.advancements.Criterion<net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance> hasIt(
            net.minecraft.world.level.ItemLike item) {
        return net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }

    static final class BlockLoot extends BlockLootSubProvider {
        BlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
            // Tank keeps its glass (component copied from the block entity). Silk touch not required.
            add(ModBlocks.TANK.get(), LootTable.lootTable().withPool(applyExplosionCondition(ModBlocks.TANK.get(), LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(ModBlocks.TANK.get())
                            .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                    .include(ModDataComponents.GLASS.get()).include(ModDataComponents.TANK_STYLE.get()))))));
            dropSelf(ModBlocks.FEEDER.get());
            dropSelf(ModBlocks.CLIP_FEEDER.get());
            // M18: coloured lights drop in their colour (white ones as the plain item, so they stack with old ones)
            add(ModBlocks.TANK_LIGHT.get(), LootTable.lootTable().withPool(applyExplosionCondition(ModBlocks.TANK_LIGHT.get(), LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(ModBlocks.TANK_LIGHT.get())
                            .apply(net.minecraft.world.level.storage.loot.functions.CopyBlockState.copyState(ModBlocks.TANK_LIGHT.get()).copy(TankLightBlock.COLOR)
                                    .when(net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.TANK_LIGHT.get())
                                            .setProperties(net.minecraft.advancements.critereon.StatePropertiesPredicate.Builder.properties()
                                                    .hasProperty(TankLightBlock.COLOR, net.minecraft.world.item.DyeColor.WHITE)).invert()))))));
            dropSelf(ModBlocks.TANK_LID.get());
            dropSelf(ModBlocks.SPAWNING_CAVE.get()); // M19
            dropSelf(ModBlocks.SPAWNING_MOP.get());
            dropSelf(ModBlocks.PROTEIN_SKIMMER.get()); // M20
            // M23
            dropSelf(ModBlocks.AIR_PUMP.get());
            dropSelf(ModBlocks.AIR_STONE.get());
            for (Block led : List.of(ModBlocks.LED_PANEL.get(), ModBlocks.HIGH_POWER_LED.get())) {
                add(led, LootTable.lootTable().withPool(applyExplosionCondition(led, LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(led)
                                .apply(net.minecraft.world.level.storage.loot.functions.CopyBlockState.copyState(led).copy(TankLightBlock.COLOR)
                                        .when(net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition.hasBlockStateProperties(led)
                                                .setProperties(net.minecraft.advancements.critereon.StatePropertiesPredicate.Builder.properties()
                                                        .hasProperty(TankLightBlock.COLOR, net.minecraft.world.item.DyeColor.WHITE)).invert()))))));
            }
            dropSelf(ModBlocks.REEF_ROCK.get());
            dropSelf(ModBlocks.LIVE_ROCK.get());
            for (Block b : m24Blocks()) dropSelf(b); // M24
            // M25: the cylinder keeps its gas and its solenoid valve
            dropSelf(ModBlocks.BIO_CO2.get());
            dropSelf(ModBlocks.CO2_STATION.get());
            dropSelf(ModBlocks.RO_UNIT.get());
            dropSelf(ModBlocks.DOSING_PUMP.get()); // M25 part c
            for (Block b : DataGenM31.blocks()) dropSelf(b); // M31 part a
            add(ModBlocks.TURTLE_EGGS.get(), LootTable.lootTable()); // M31 part b: broken eggs give nothing
            add(ModBlocks.FROG_EGGS.get(), LootTable.lootTable()); // M31 part c
            for (Block b : DataGenM26.blocks()) dropSelf(b); // M26 part a
            for (Block b : DataGenM37.blocks()) dropSelf(b); // M37
            for (Block b : DataGenM40.blocks()) dropSelf(b); // M40 part a
            for (Block b : DataGenM40b.blocks()) dropSelf(b); // M40 part b
            for (Block b : java.util.stream.Stream.concat(DataGenM40.pipes().stream(), DataGenM40b.pipes().stream()).toList()) { // M40: a filter pipe, and its T-piece back from a branch
                add(b, LootTable.lootTable()
                        .withPool(applyExplosionCondition(b, LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(b))))
                        .withPool(applyExplosionCondition(b, LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(ModItems.PIPE_TEE.get()))
                                .when(net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition.hasBlockStateProperties(b)
                                        .setProperties(net.minecraft.advancements.critereon.StatePropertiesPredicate.Builder.properties()
                                                .hasProperty(org.overengineer.bettacraft.plumbing.FilterPipeBlock.JUNCTION, true))))));
            }
            for (Block b : DataGenM27.blocks()) dropSelf(b); // M27
            dropOther(ModBlocks.ALMOND_LEAF_LITTER.get(), ModItems.ALMOND_LEAF.get()); // M28 part c
            dropOther(ModBlocks.ALDER_CONES.get(), ModItems.ALDER_CONE.get());
            dropSelf(ModBlocks.ACTIVE_SOIL.get()); // M25 part d
            add(ModBlocks.CO2_CYLINDER.get(), LootTable.lootTable().withPool(applyExplosionCondition(ModBlocks.CO2_CYLINDER.get(), LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(ModBlocks.CO2_CYLINDER.get())
                            .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                    .include(ModDataComponents.CO2_GAS.get()).include(ModDataComponents.CO2_SOLENOID.get()))))));
            dropSelf(ModBlocks.LARVA_TUB.get());
            dropSelf(ModBlocks.ARTEMIA_HATCHERY.get());
            // M22.3: wild corals only with silk touch (like vanilla corals); shears cut frags off them
            for (var b : ModBlocks.WILD_CORALS.values()) dropWhenSilkTouch(b.get());
            // M4: the filter keeps its heaters, the cabinet its planks (its contents drop separately)
            add(ModBlocks.EXTERNAL_FILTER.get(), LootTable.lootTable().withPool(applyExplosionCondition(ModBlocks.EXTERNAL_FILTER.get(), LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(ModBlocks.EXTERNAL_FILTER.get())
                            .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                    .include(net.minecraft.core.component.DataComponents.CONTAINER))))));
            add(ModBlocks.CABINET.get(), LootTable.lootTable().withPool(applyExplosionCondition(ModBlocks.CABINET.get(), LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(ModBlocks.CABINET.get())
                            .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                    .include(ModDataComponents.WOOD.get()).include(ModDataComponents.OUTLET_SIDE.get())
                                    .include(ModDataComponents.CABINET_FRONT.get()).include(ModDataComponents.CABINET_JOINTS.get()))))));
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(ModBlocks.TANK.get(), ModBlocks.FEEDER.get(), ModBlocks.CLIP_FEEDER.get(), ModBlocks.TANK_LIGHT.get(), ModBlocks.EXTERNAL_FILTER.get(), ModBlocks.CABINET.get(),
                    ModBlocks.TANK_LID.get(), ModBlocks.LARVA_TUB.get(), ModBlocks.ARTEMIA_HATCHERY.get(), ModBlocks.SPAWNING_CAVE.get(),
                    ModBlocks.SPAWNING_MOP.get(), ModBlocks.PROTEIN_SKIMMER.get(), ModBlocks.REEF_ROCK.get(), ModBlocks.LIVE_ROCK.get(),
                    ModBlocks.AIR_PUMP.get(), ModBlocks.AIR_STONE.get(), ModBlocks.LED_PANEL.get(), ModBlocks.HIGH_POWER_LED.get())
                    .stream().collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(), l -> {
                        for (var b : ModBlocks.WILD_CORALS.values()) l.add(b.get());
                        l.addAll(m24Blocks());
                        l.addAll(DataGenM25b.blocks()); // M25
                        l.addAll(DataGenM25c.blocks());
                        l.addAll(DataGenM31.blocks()); // M31 part a
                        l.add(ModBlocks.TURTLE_EGGS.get()); // M31 part b
                        l.add(ModBlocks.FROG_EGGS.get()); // M31 part c
                        l.addAll(DataGenM26.blocks()); // M26 part a
                        l.addAll(DataGenM37.blocks()); // M37
                        l.addAll(DataGenM40.blocks()); // M40 part a
                        l.addAll(DataGenM40.pipes());
                        l.addAll(DataGenM40b.blocks()); // M40 part b
                        l.addAll(DataGenM40b.pipes());
                        l.addAll(DataGenM27.blocks()); // M27
                        l.addAll(DataGenM28.blocks()); // M28 part c
                        l.add(ModBlocks.ACTIVE_SOIL.get()); // M25 part d
                        return l;
                    }));
        }
    }

    /** M24: the new blocks (each drops itself). */
    static List<Block> m24Blocks() {
        return List.of(ModBlocks.CRUCIBLE.get(), ModBlocks.GLASS_PIPE.get(), ModBlocks.STAINLESS_STEEL_PIPE.get(), ModBlocks.WATER_PUMP.get(),
                ModBlocks.RESERVOIR.get(), ModBlocks.LEVEL_SENSOR.get(), ModBlocks.WATER_CHANGE_CONTROLLER.get(), ModBlocks.BULKHEAD.get(),
                ModBlocks.INTERIOR_PIPE.get(), ModBlocks.INTAKE_STRAINER.get(), ModBlocks.LILY_PIPE.get(), ModBlocks.NOZZLE.get(), ModBlocks.SPRAY_BAR.get(),
                ModBlocks.FILTER_CHAMBER.get(), ModBlocks.FILTER_PUMP_HEAD.get(), ModBlocks.FILTER_INLET.get(), ModBlocks.FILTER_OUTLET.get());
    }

    /** Mud mined in a betta biome sometimes drops mosquito larvae (PLAN 6.3). */
    static final class LootModifiers extends GlobalLootModifierProvider {
        LootModifiers(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup) {
            super(out, lookup, BettaCraft.MOD_ID);
        }

        @Override
        protected void start() {
            var biomes = registries.lookupOrThrow(Registries.BIOME).getOrThrow(ModTags.BETTA_SPAWN_BIOMES);
            // M33: every modifier only while its feature is on (bettacraft:feature)
            add("mud_mosquito_larvae", new AddItemLootModifier(new LootItemCondition[]{
                    new org.overengineer.bettacraft.config.FeatureLootCondition(org.overengineer.bettacraft.config.Feature.LIVE_FOOD),
                    LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("blocks/mud")).build(),
                    LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiomes(biomes)).build(),
                    LootItemRandomChanceCondition.randomChance(org.overengineer.bettacraft.sources.WildTables.MUD_LARVAE_CHANCE).build()
            }, new ItemStack(ModItems.MOSQUITO_LARVAE.get())));
            // M26: and tubifex worms (the start of a tubifex dish)
            add("mud_tubifex", new AddItemLootModifier(new LootItemCondition[]{
                    new org.overengineer.bettacraft.config.FeatureLootCondition(org.overengineer.bettacraft.config.Feature.LIVE_FOOD_CULTURES),
                    LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("blocks/mud")).build(),
                    LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiomes(biomes)).build(),
                    LootItemRandomChanceCondition.randomChance(org.overengineer.bettacraft.sources.WildTables.MUD_TUBIFEX_CHANCE).build()
            }, new ItemStack(ModItems.TUBIFEX.get())));
            // sand -> artemia cysts, chance from the server config (drops.artemiaFromSandChance)
            add("sand_artemia_cysts", new org.overengineer.bettacraft.world.ArtemiaSandLootModifier(new LootItemCondition[]{new org.overengineer.bettacraft.config.FeatureLootCondition(org.overengineer.bettacraft.config.Feature.LIVE_FOOD)},
                    new ItemStack(ModItems.ARTEMIA_CYSTS.get())));
            // M12: fishing in swamps / mangrove swamps sometimes brings up a floating plant (chance from the server config)
            add("fishing_floating_plants", new org.overengineer.bettacraft.world.FloatingPlantFishingModifier(new LootItemCondition[]{
                    new org.overengineer.bettacraft.config.FeatureLootCondition(org.overengineer.bettacraft.config.Feature.FLOATING_PLANTS),
                    LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("gameplay/fishing")).build()
            }));
            // M13: fishing in jungles, rivers and swamps sometimes brings up an aquarium plant
            add("fishing_aquatic_plants", new org.overengineer.bettacraft.world.AquaticPlantFishingModifier(new LootItemCondition[]{
                    new org.overengineer.bettacraft.config.FeatureLootCondition(org.overengineer.bettacraft.config.Feature.AQUATIC_PLANTS),
                    LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("gameplay/fishing")).build()
            }));
        }
    }

    // ------------------------------------------------------------------ tags

    static final class BlockTagsGen extends BlockTagsProvider {
        BlockTagsGen(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper efh) {
            super(out, lookup, BettaCraft.MOD_ID, efh);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(BlockTags.DRAGON_IMMUNE).add(ModBlocks.INTERIOR_FLOOR.get(), ModBlocks.INTERIOR_WALL.get());
            tag(BlockTags.WITHER_IMMUNE).add(ModBlocks.INTERIOR_FLOOR.get(), ModBlocks.INTERIOR_WALL.get());
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.FEEDER.get(), ModBlocks.CLIP_FEEDER.get(), ModBlocks.TANK_LIGHT.get(), ModBlocks.FILTER_SEGMENT.get(),
                    ModBlocks.EXTERNAL_FILTER.get(), ModBlocks.ARTEMIA_HATCHERY.get());
            tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.CABINET.get(), ModBlocks.LARVA_TUB.get());
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.SPAWNING_CAVE.get()); // M19
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.AIR_PUMP.get(), ModBlocks.AIR_STONE.get(), ModBlocks.LED_PANEL.get(), ModBlocks.HIGH_POWER_LED.get(),
                    ModBlocks.CORNER_FILTER_SEGMENT.get()); // M23
            for (Block b : m24Blocks()) tag(BlockTags.MINEABLE_WITH_PICKAXE).add(b); // M24
            DataGenM25b.blockTags(this::tag); // M25
            DataGenM25c.blockTags(this::tag);
            DataGenM31.blockTags(this::tag); // M31 part a
            DataGenM31b.blockTags(this::tag); // M31 part b
            DataGenM31c.blockTags(this::tag); // M31 part c
            DataGenM26.blockTags(this::tag); // M26 part a
            DataGenM37.blockTags(this::tag); // M37
            DataGenM40.blockTags(this::tag); // M40 part a
            DataGenM40b.blockTags(this::tag); // M40 part b
            DataGenM40c.blockTags(this::tag); // M40 part c: Cable Facades
            DataGenM41.blockTags(this::tag); // M41: the drive adapter (optional: only with Create)
            DataGenM38.blockTags(this::tag); // M38
            DataGenM27.blockTags(this::tag); // M27
            DataGenM28.blockTags(this::tag); // M28 part c
            DataGenM32b.blockTags(this::tag); // M32 part b
            tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.ACTIVE_SOIL.get()); // M25 part d
            tag(ModTags.ACTIVE_SOIL).add(ModBlocks.ACTIVE_SOIL.get());
            tag(ModTags.ARTEMIA_SAND).addTag(Tags.Blocks.SANDS);
            // M16: no tank inside a tank
            tag(ModTags.NOT_IN_TANK_INTERIOR).add(ModBlocks.TANK.get())
                    .addOptional(org.overengineer.bettacraft.compat.betterfishtanks.BetterFishTanksCompat.TANK_BLOCK);
            tag(ModTags.FLOATING_PLANTS).add(net.minecraft.world.level.block.Blocks.LILY_PAD)
                    .addOptional(Compat.rl(Compat.FINTASTIC, "duckweed")).addOptional(Compat.rl(Compat.FINTASTIC, "lotus_pad"));
            // Fintastic plants may stand in the dry interior like seagrass and kelp (M7)
            var plants = tag(ModTags.INTERIOR_WATER_PLANTS);
            for (String p : Compat.FINTASTIC_PLANTS) plants.addOptional(Compat.rl(Compat.FINTASTIC, p));
            // M22.1: they sway in the miniature outside the tank too
            var sway = tag(ModTags.SWAYING_PLANTS).add(net.minecraft.world.level.block.Blocks.KELP, net.minecraft.world.level.block.Blocks.KELP_PLANT,
                    net.minecraft.world.level.block.Blocks.SEAGRASS, net.minecraft.world.level.block.Blocks.TALL_SEAGRASS)
                    .addTag(BlockTags.CORALS).addTag(BlockTags.WALL_CORALS);
            for (String p : Compat.FINTASTIC_PLANTS) sway.addOptional(Compat.rl(Compat.FINTASTIC, p));
            // M13: where aquarium plants grow
            tag(ModTags.EPIPHYTE_SURFACES).addTag(BlockTags.BASE_STONE_OVERWORLD).addTag(BlockTags.LOGS).addTag(BlockTags.PLANKS)
                    .addTag(BlockTags.STONE_BRICKS).addTag(Tags.Blocks.COBBLESTONES).addTag(BlockTags.WOODEN_SLABS).addTag(BlockTags.WOODEN_STAIRS)
                    .addTag(BlockTags.WOODEN_FENCES).addTag(BlockTags.WALLS).addTag(BlockTags.SLABS).addTag(BlockTags.STAIRS)
                    .add(net.minecraft.world.level.block.Blocks.MOSSY_COBBLESTONE, net.minecraft.world.level.block.Blocks.SMOOTH_STONE,
                            net.minecraft.world.level.block.Blocks.BONE_BLOCK, net.minecraft.world.level.block.Blocks.MAGMA_BLOCK,
                            net.minecraft.world.level.block.Blocks.BLACKSTONE, net.minecraft.world.level.block.Blocks.BASALT,
                            net.minecraft.world.level.block.Blocks.POLISHED_BASALT, net.minecraft.world.level.block.Blocks.SMOOTH_BASALT,
                            net.minecraft.world.level.block.Blocks.MUD_BRICKS, net.minecraft.world.level.block.Blocks.PACKED_MUD,
                            net.minecraft.world.level.block.Blocks.SANDSTONE, net.minecraft.world.level.block.Blocks.RED_SANDSTONE,
                            net.minecraft.world.level.block.Blocks.CALCITE, net.minecraft.world.level.block.Blocks.DRIPSTONE_BLOCK,
                            net.minecraft.world.level.block.Blocks.MOSS_BLOCK, net.minecraft.world.level.block.Blocks.MANGROVE_ROOTS,
                            net.minecraft.world.level.block.Blocks.MUDDY_MANGROVE_ROOTS, net.minecraft.world.level.block.Blocks.BAMBOO_BLOCK)
                    .addTag(BlockTags.TERRACOTTA);
            tag(ModTags.PLANT_SUBSTRATE).add(ModBlocks.ACTIVE_SOIL.get()).addTag(BlockTags.SAND).addTag(BlockTags.DIRT).addTag(Tags.Blocks.GRAVELS)
                    .add(net.minecraft.world.level.block.Blocks.GRAVEL, net.minecraft.world.level.block.Blocks.CLAY,
                            net.minecraft.world.level.block.Blocks.MUD, net.minecraft.world.level.block.Blocks.SOUL_SAND,
                            net.minecraft.world.level.block.Blocks.SOUL_SOIL, net.minecraft.world.level.block.Blocks.SUSPICIOUS_SAND,
                            net.minecraft.world.level.block.Blocks.SUSPICIOUS_GRAVEL);
            // M20: corals are glued onto reef rock, live rock and coral blocks (also dead ones)
            tag(ModTags.REEF_SURFACES).add(ModBlocks.REEF_ROCK.get(), ModBlocks.LIVE_ROCK.get()).addTag(BlockTags.CORAL_BLOCKS)
                    .add(net.minecraft.world.level.block.Blocks.DEAD_TUBE_CORAL_BLOCK, net.minecraft.world.level.block.Blocks.DEAD_BRAIN_CORAL_BLOCK,
                            net.minecraft.world.level.block.Blocks.DEAD_BUBBLE_CORAL_BLOCK, net.minecraft.world.level.block.Blocks.DEAD_FIRE_CORAL_BLOCK,
                            net.minecraft.world.level.block.Blocks.DEAD_HORN_CORAL_BLOCK);
            tag(ModTags.EPIPHYTE_SURFACES).add(ModBlocks.REEF_ROCK.get(), ModBlocks.LIVE_ROCK.get());
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.PROTEIN_SKIMMER.get(), ModBlocks.REEF_ROCK.get(), ModBlocks.LIVE_ROCK.get(),
                    ModBlocks.PUMP_SEGMENT.get());
            var plantBlocks = tag(BlockTags.SWORD_EFFICIENT);
            for (var b : ModBlocks.AQUATIC_PLANTS.values()) plantBlocks.add(b.get());
            // M25: water chemistry
            tag(ModTags.ACID_SUBSTRATE).addTag(BlockTags.DIRT).add(net.minecraft.world.level.block.Blocks.SOUL_SOIL);
            tag(ModTags.LIMESTONE).add(net.minecraft.world.level.block.Blocks.CALCITE, net.minecraft.world.level.block.Blocks.DRIPSTONE_BLOCK,
                            net.minecraft.world.level.block.Blocks.DEAD_TUBE_CORAL_BLOCK, net.minecraft.world.level.block.Blocks.DEAD_BRAIN_CORAL_BLOCK,
                            net.minecraft.world.level.block.Blocks.DEAD_BUBBLE_CORAL_BLOCK, net.minecraft.world.level.block.Blocks.DEAD_FIRE_CORAL_BLOCK,
                            net.minecraft.world.level.block.Blocks.DEAD_HORN_CORAL_BLOCK)
                    .addOptional(Compat.rl("create", "limestone")).addOptional(Compat.rl("create", "cut_limestone"))
                    .addOptional(Compat.rl("create", "polished_cut_limestone")).addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "stones/limestone"));
            tag(ModTags.DRIFTWOOD).addTag(BlockTags.LOGS).add(net.minecraft.world.level.block.Blocks.MANGROVE_ROOTS);
            tag(ModTags.SUBSTRATE_BLACKLIST).add(net.minecraft.world.level.block.Blocks.BARRIER, net.minecraft.world.level.block.Blocks.BEDROCK,
                    net.minecraft.world.level.block.Blocks.REINFORCED_DEEPSLATE, net.minecraft.world.level.block.Blocks.TNT);
        }
    }

    static final class ItemTagsGen extends ItemTagsProvider {
        ItemTagsGen(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup,
                    CompletableFuture<TagsProvider.TagLookup<Block>> blocks, ExistingFileHelper efh) {
            super(out, lookup, blocks, BettaCraft.MOD_ID, efh);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(ModTags.BETTA_FOOD).add(ModItems.BETTA_PELLETS.get(), ModItems.MOSQUITO_LARVAE.get(), ModItems.INFUSORIA.get(),
                    ModItems.ARTEMIA_NAUPLII.get());
            tag(ModTags.LIVE_FOOD).add(ModItems.MOSQUITO_LARVAE.get());
            DataGenM26.itemTags(this::tag); // M26 part a: tubifex, daphnia and what the cultures eat
            DataGenM31b.itemTags(this::tag); // M31 part b: the greens of the turtles
            DataGenM31c.itemTags(this::tag); // M31 part c: insects, fruit, prey
            DataGenM31d.itemTags(this::tag); // M31 part d: capture items of other mods
            DataGenM39.itemTags(this::tag); // M39 part a: live, flake and frozen food of other mods
            // M9: organic matter for the larva tub
            tag(ModTags.LARVA_TUB_FOOD).addTag(net.minecraft.tags.ItemTags.LEAVES).add(Items.WHEAT, Items.ROTTEN_FLESH, Items.BONE_MEAL);
            tag(ModTags.FRY_FOOD_START).add(ModItems.INFUSORIA.get(), ModItems.FRY_POWDER.get()); // M19: fry powder for all fry
            tag(ModTags.BOTTOM_FOOD).add(ModItems.ALGAE_WAFER.get());
            tag(ModTags.FRY_FOOD).add(ModItems.ARTEMIA_NAUPLII.get());
            tag(ModTags.ENTITY_WATER_BUCKETS).add(ModItems.BETTA_BUCKET.get());
            // M24: our stainless steel (never the steel of other mods: #c:ingots/steel stays theirs)
            tag(ModTags.STAINLESS_STEEL_INGOTS).add(ModItems.STAINLESS_STEEL_INGOT.get());
            tag(Tags.Items.INGOTS).addTag(ModTags.STAINLESS_STEEL_INGOTS);
            // M22.3: a bucket of sea water is a bucket of water for the recipes and machines of other mods (convention tag)
            tag(Tags.Items.BUCKETS_WATER).add(ModItems.SEA_WATER_BUCKET.get());
            // M21.7: stones of the stone cabinet (modpacks add to it) and what never counts as one
            tag(ModTags.CABINET_STONES).addTag(Tags.Items.STONES).addTag(Tags.Items.COBBLESTONES).addTag(net.minecraft.tags.ItemTags.STONE_BRICKS)
                    .addTag(Tags.Items.SANDSTONE_BLOCKS).addTag(net.minecraft.tags.ItemTags.TERRACOTTA)
                    .add(Items.SMOOTH_STONE, Items.POLISHED_GRANITE, Items.POLISHED_DIORITE, Items.POLISHED_ANDESITE, Items.COBBLED_DEEPSLATE,
                            Items.POLISHED_DEEPSLATE, Items.DEEPSLATE_BRICKS, Items.CRACKED_DEEPSLATE_BRICKS, Items.DEEPSLATE_TILES,
                            Items.CRACKED_DEEPSLATE_TILES, Items.CHISELED_DEEPSLATE, Items.TUFF, Items.POLISHED_TUFF, Items.TUFF_BRICKS, Items.CHISELED_TUFF,
                            Items.CHISELED_TUFF_BRICKS, Items.CALCITE, Items.BLACKSTONE, Items.POLISHED_BLACKSTONE, Items.POLISHED_BLACKSTONE_BRICKS,
                            Items.CRACKED_POLISHED_BLACKSTONE_BRICKS, Items.CHISELED_POLISHED_BLACKSTONE, Items.BASALT, Items.POLISHED_BASALT,
                            Items.SMOOTH_BASALT, Items.DRIPSTONE_BLOCK, Items.BRICKS, Items.NETHER_BRICKS, Items.CRACKED_NETHER_BRICKS,
                            Items.CHISELED_NETHER_BRICKS, Items.RED_NETHER_BRICKS, Items.QUARTZ_BLOCK, Items.QUARTZ_BRICKS, Items.SMOOTH_QUARTZ,
                            Items.CHISELED_QUARTZ_BLOCK, Items.QUARTZ_PILLAR);
            // (#c:storage_blocks is checked in CabinetStones for stones outside #bettacraft:cabinet_stones: some mods
            // tag the quartz block as a storage block)
            tag(ModTags.CABINET_EXCLUDED).addTag(Tags.Items.ORES).addTag(Tags.Items.GLASS_BLOCKS)
                    .addTag(Tags.Items.CONCRETES).addTag(Tags.Items.OBSIDIANS).addTag(Tags.Items.END_STONES).addTag(Tags.Items.GLAZED_TERRACOTTAS)
                    .add(Items.PURPUR_BLOCK, Items.PURPUR_PILLAR, Items.END_STONE_BRICKS, Items.PRISMARINE, Items.PRISMARINE_BRICKS, Items.DARK_PRISMARINE,
                            Items.MUD_BRICKS, Items.PACKED_MUD, Items.COPPER_BLOCK, Items.EXPOSED_COPPER, Items.WEATHERED_COPPER, Items.OXIDIZED_COPPER,
                            Items.CUT_COPPER, Items.EXPOSED_CUT_COPPER, Items.WEATHERED_CUT_COPPER, Items.OXIDIZED_CUT_COPPER, Items.WAXED_COPPER_BLOCK,
                            Items.WAXED_EXPOSED_COPPER, Items.WAXED_WEATHERED_COPPER, Items.WAXED_OXIDIZED_COPPER, Items.WAXED_CUT_COPPER,
                            Items.WAXED_EXPOSED_CUT_COPPER, Items.WAXED_WEATHERED_CUT_COPPER, Items.WAXED_OXIDIZED_CUT_COPPER, Items.CHISELED_COPPER,
                            Items.EXPOSED_CHISELED_COPPER, Items.WEATHERED_CHISELED_COPPER, Items.OXIDIZED_CHISELED_COPPER, Items.WAXED_CHISELED_COPPER,
                            Items.WAXED_EXPOSED_CHISELED_COPPER, Items.WAXED_WEATHERED_CHISELED_COPPER, Items.WAXED_OXIDIZED_CHISELED_COPPER,
                            Items.MAGMA_BLOCK, Items.GILDED_BLACKSTONE, Items.REINFORCED_DEEPSLATE);
            for (var b : ModItems.TANKMATE_BUCKETS.values()) tag(ModTags.ENTITY_WATER_BUCKETS).add(b.get());
            var aq = tag(ModTags.AQUATIC_PLANT_ITEMS);
            for (var it : ModItems.AQUATIC_PLANTS.values()) aq.add(it.get());
            // plastic of other mods (all optional): Industrial Foregoing, PneumaticCraft, Mekanism
            tag(ModTags.PLASTIC_SHEETS)
                    .addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "plastics"))
                    .addOptionalTag(ResourceLocation.fromNamespaceAndPath("pneumaticcraft", "plastic_sheets"))
                    .addOptional(ResourceLocation.fromNamespaceAndPath("mekanism", "hdpe_sheet"));
            // raw fish for pellets: cod, salmon and the raw fish of other mods (Aquaculture, Fish of Thieves, ...)
            tag(ModTags.PELLET_FISH).add(Items.COD, Items.SALMON).addOptionalTag(Tags.Items.FOODS_RAW_FISH)
                    .addOptional(Compat.rl(Compat.AQUACULTURE, "fish_fillet_raw"));
            // food of other mods (M7): fish feed and pellets as normal food, worms and shrimp as live food
            for (var f : Compat.FOOD) tag(ModTags.BETTA_FOOD).addOptional(f);
            for (var f : Compat.LIVE_FOOD) {
                tag(ModTags.LIVE_FOOD).addOptional(f);
                tag(ModTags.BETTA_FOOD).addOptional(f);
            }
        }
    }

    static final class EntityTagsGen extends EntityTypeTagsProvider {
        EntityTagsGen(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper efh) {
            super(out, lookup, BettaCraft.MOD_ID, efh);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            EntityType<?> betta = ModEntities.BETTA.get();
            tag(ModTags.C_FISH).add(betta);
            tag(ModTags.C_FISHES).add(betta);
            for (var t : ModEntities.TANKMATES.values()) { // M19
                tag(ModTags.C_FISH).add(t.get());
                tag(ModTags.C_FISHES).add(t.get());
            }
            tag(ModTags.BFT_CATCHABLE).addOptional(BettaCraft.id("betta"));
            var inhabitants = tag(ModTags.TANK_INHABITANTS).add(EntityType.AXOLOTL, EntityType.TADPOLE);
            for (var e : Compat.INHABITANTS) inhabitants.addOptional(e);
            // giant fish of other mods (their hitboxes are small, the animals are not)
            // M16: the vanilla frog is no tank animal (lives on land, the tadpole grows into it and hops out)
            var blacklist = tag(ModTags.TANK_BLACKLIST).add(EntityType.FROG);
            for (var e : Compat.BLACKLIST) blacklist.addOptional(e);
            // M30: big animals that go into big tanks by the size rule (their length from the hitbox): the vanilla dolphin
            tag(ModTags.BIG_TANK_ANIMALS).add(EntityType.DOLPHIN);
            // M16: axolotls hunt small fish and fry
            var predators = tag(ModTags.PREDATORS).add(EntityType.PUFFERFISH, EntityType.AXOLOTL);
            for (var e : Compat.PREDATORS) predators.addOptional(e);
            var prey = tag(ModTags.BETTA_PREY);
            for (var e : Compat.PREY) prey.addOptional(e);
            // M28.1: every animal of the mod, and the ones that spawn wild (for commands: @e[type=#bettacraft:all_animals])
            var all = tag(ModTags.ALL_ANIMALS).add(betta, ModEntities.DWARF_FROG.get(), ModEntities.WILD_CRITTER.get());
            var wild = tag(ModTags.WILD_ANIMALS).add(betta, ModEntities.DWARF_FROG.get(), ModEntities.WILD_CRITTER.get());
            for (var s : org.overengineer.bettacraft.terrarium.HerpSpecies.ALL) { // M31 part b: the turtles (all of them wild)
                all.add(ModEntities.herp(s));
                wild.add(ModEntities.herp(s));
            }
            for (var e : ModEntities.TANKMATES.entrySet()) {
                all.add(e.getValue().get());
                if (org.overengineer.bettacraft.world.Wildlife.spawnsWild(e.getKey())) wild.add(e.getValue().get());
            }
        }
    }

    static final class BiomeTagsGen extends TagsProvider<Biome> {
        BiomeTagsGen(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper efh) {
            super(out, Registries.BIOME, lookup, BettaCraft.MOD_ID, efh);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(ModTags.BETTA_SPAWN_BIOMES).addTag(Tags.Biomes.IS_SWAMP);
            // M25.1: the waters by biome (BiomeWaters): vanilla, the common tags and known biome mods (optional)
            var warm = tag(ModTags.WARM_OCEAN_WATERS).add(net.minecraft.world.level.biome.Biomes.WARM_OCEAN);
            Compat.WARM_OCEAN_BIOMES.forEach(warm::addOptional);
            tag(ModTags.LUKEWARM_OCEAN_WATERS).add(net.minecraft.world.level.biome.Biomes.LUKEWARM_OCEAN, net.minecraft.world.level.biome.Biomes.DEEP_LUKEWARM_OCEAN);
            var mangrove = tag(ModTags.MANGROVE_WATERS).add(net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP);
            Compat.MANGROVE_BIOMES.forEach(mangrove::addOptional);
            tag(ModTags.LUSH_CAVE_WATERS).add(net.minecraft.world.level.biome.Biomes.LUSH_CAVES);
            var swamp = tag(ModTags.SWAMP_WATERS).add(net.minecraft.world.level.biome.Biomes.SWAMP).addTag(Tags.Biomes.IS_SWAMP);
            Compat.SWAMP_BIOMES.forEach(swamp::addOptional);
            var river = tag(ModTags.RIVER_WATERS).addTag(net.minecraft.tags.BiomeTags.IS_RIVER);
            Compat.RIVER_BIOMES.forEach(river::addOptional);
            tag(ModTags.BEACH_WATERS).addTag(net.minecraft.tags.BiomeTags.IS_BEACH);
            var jungle = tag(ModTags.JUNGLE_WATERS).addTag(net.minecraft.tags.BiomeTags.IS_JUNGLE);
            Compat.JUNGLE_BIOMES.forEach(jungle::addOptional);
            tag(ModTags.CORAL_REEF_BIOMES).addTag(ModTags.WARM_OCEAN_WATERS);
            // M28.1: cold fresh water (the wild Marimo): frozen rivers and the lakes of taigas and snowy plains
            tag(ModTags.COLD_WATERS).add(net.minecraft.world.level.biome.Biomes.FROZEN_RIVER, net.minecraft.world.level.biome.Biomes.SNOWY_PLAINS,
                    net.minecraft.world.level.biome.Biomes.SNOWY_TAIGA, net.minecraft.world.level.biome.Biomes.TAIGA,
                    net.minecraft.world.level.biome.Biomes.OLD_GROWTH_PINE_TAIGA, net.minecraft.world.level.biome.Biomes.OLD_GROWTH_SPRUCE_TAIGA);
            tag(ModTags.SALT_WATER_BIOMES).addTag(Tags.Biomes.IS_OCEAN).addTag(Tags.Biomes.IS_BEACH); // M20: beaches too
            // M31 part b: Hermann's tortoise: savannas and the dry shrublands of biome mods (its home is Mediterranean scrub)
            var tortoise = tag(ModTags.TORTOISE_LANDS).addTag(Tags.Biomes.IS_SAVANNA);
            Compat.TORTOISE_BIOMES.forEach(tortoise::addOptional);
            // M31 part c: the leopard gecko (rocky semi-desert) and the corn snake (fields and woods of temperate lands)
            tag(ModTags.ARID_LANDS).addTag(Tags.Biomes.IS_DESERT).addTag(net.minecraft.tags.BiomeTags.IS_BADLANDS);
            tag(ModTags.SNAKE_LANDS).add(net.minecraft.world.level.biome.Biomes.FOREST, net.minecraft.world.level.biome.Biomes.FLOWER_FOREST,
                    net.minecraft.world.level.biome.Biomes.BIRCH_FOREST, net.minecraft.world.level.biome.Biomes.OLD_GROWTH_BIRCH_FOREST,
                    net.minecraft.world.level.biome.Biomes.DARK_FOREST, net.minecraft.world.level.biome.Biomes.PLAINS,
                    net.minecraft.world.level.biome.Biomes.SUNFLOWER_PLAINS);
        }
    }
}
