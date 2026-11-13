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
        gen.addProvider(event.includeServer(), new BiomeTagsGen(out, lookup, efh));
        gen.addProvider(event.includeServer(), new DatapackBuiltinEntriesProvider(out, lookup, new RegistrySetBuilder()
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ctx -> ctx.register(BETTA_SPAWNS,
                        new BettaSpawnsBiomeModifier(ctx.lookup(Registries.BIOME).getOrThrow(ModTags.BETTA_SPAWN_BIOMES)))),
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

        @Override
        protected void registerStatesAndModels() {
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
                getVariantBuilder(block).forAllStates(state -> {
                    var part = state.getValue(org.overengineer.bettacraft.equipment.SegmentBlock.PART);
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
                });
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
            }, TankLightBlock.POWERED, TankLightBlock.COLOR); // M18: the colour is a tint of the panel (BettaCraftClient)
        }

        /** M20: reef rock, live rock, the flow pump in the interior and the protein skimmer on the tank. */
        private void reef() {
            simpleBlock(ModBlocks.REEF_ROCK.get(), cubeAll(ModBlocks.REEF_ROCK.get()));
            simpleBlock(ModBlocks.LIVE_ROCK.get(), cubeAll(ModBlocks.LIVE_ROCK.get()));
            ModelFile side = models().cube("flow_pump_segment", modLoc("block/flow_pump_segment_side"), modLoc("block/flow_pump_segment_side"),
                    modLoc("block/flow_pump_segment_outlet"), modLoc("block/flow_pump_segment_side"), modLoc("block/flow_pump_segment_side"),
                    modLoc("block/flow_pump_segment_side")).texture("particle", modLoc("block/flow_pump_segment_side"));
            getVariantBuilder(ModBlocks.PUMP_SEGMENT.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(side)
                    .rotationY(((int) state.getValue(org.overengineer.bettacraft.equipment.SegmentBlock.FACING).toYRot() + 180) % 360).build());
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
         */
        private void cabinet() {
            ModelFile[] variants = new ModelFile[4];
            for (int i = 0; i < 4; i++) {
                boolean left = (i & 1) != 0, right = (i & 2) != 0;
                String name = "cabinet" + (left && right ? "_both" : left ? "_left" : right ? "_right" : "");
                BlockModelBuilder m = models().getBuilder(name)
                        .texture("wood", modLoc("block/cabinet_wood")).texture("trim", modLoc("block/cabinet_trim"))
                        .texture("particle", modLoc("block/cabinet_wood"));
                // carcass (full block below the counter top)
                // M21.5: every face gets explicit UVs clamped to the texture (the auto UVs of the overhangs reached past
                // the 16 px of the texture and showed neighbouring atlas textures: pink, purple, blue edges)
                box(m, 0, 0, 0, 16, 14, 16, "#wood", d -> d != Direction.UP);
                // counter top: overhangs at the front and at free sides (left = east, right = west in the model)
                float x0 = right ? 0 : -0.5F, x1 = left ? 16 : 16.5F;
                box(m, x0, 14, -0.5F, x1, 16, 16, "#wood", d -> d == Direction.UP || d == Direction.SOUTH || (d == Direction.EAST && left) || (d == Direction.WEST && right));
                // plinth and two doors with handles
                box(m, 0, 0, -0.25F, 16, 1.5F, 0, "#trim", d -> false);
                box(m, 1, 2.5F, -0.5F, 7.75F, 13, 0, "#wood", d -> false);
                box(m, 8.25F, 2.5F, -0.5F, 15, 13, 0, "#wood", d -> false);
                box(m, 6.25F, 7, -1.25F, 7, 10.5F, -0.5F, "#trim", d -> false);
                box(m, 9, 7, -1.25F, 9.75F, 10.5F, -0.5F, "#trim", d -> false);
                variants[i] = m;
            }
            getVariantBuilder(ModBlocks.CABINET.get()).forAllStates(state -> {
                int i = (state.getValue(org.overengineer.bettacraft.equipment.CabinetBlock.LEFT) ? 1 : 0)
                        | (state.getValue(org.overengineer.bettacraft.equipment.CabinetBlock.RIGHT) ? 2 : 0);
                return ConfiguredModel.builder().modelFile(variants[i])
                        .rotationY(((int) state.getValue(org.overengineer.bettacraft.equipment.CabinetBlock.FACING).toYRot() + 180) % 360).build();
            });
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
            // M18: a flat icon: the housing and the panel with its light cone (layer 1, tinted in the light colour)
            withExistingParent("tank_light", mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/tank_light")).texture("layer1", modLoc("item/tank_light_glow"));
            withExistingParent("external_filter", modLoc("block/external_filter"));
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
            basicItem(ModItems.HEATER_SMALL.get());
            basicItem(ModItems.HEATER_MEDIUM.get());
            basicItem(ModItems.HEATER_LARGE.get());
            basicItem(ModItems.COOLER_SMALL.get());
            basicItem(ModItems.COOLER_MEDIUM.get());
            basicItem(ModItems.COOLER_LARGE.get());
            basicItem(ModItems.INTERNAL_FILTER.get());
            for (var d : ModItems.DECOR.values()) basicItem(d.get());
            // M19: tankmates (bucket with the betta's fish overlay, tinted per species), spawn eggs, breeding items
            for (var sp : org.overengineer.bettacraft.tankmates.TankmateSpecies.values()) {
                withExistingParent(sp.bucketId(), mcLoc("item/generated"))
                        .texture("layer0", modLoc("item/betta_bucket")).texture("layer1", modLoc("item/betta_bucket_fish"))
                        .texture("layer2", modLoc("item/betta_bucket_fish_fins")).texture("layer3", modLoc("item/betta_bucket_glass"));
                withExistingParent(sp.spawnEggId(), mcLoc("item/template_spawn_egg"));
            }
            basicItem(ModItems.BREEDING_BOX.get());
            basicItem(ModItems.FRY_POWDER.get());
            basicItem(ModItems.ALGAE_WAFER.get());
            withExistingParent("spawning_cave", modLoc("block/spawning_cave"));
            withExistingParent("spawning_mop", mcLoc("item/generated")).texture("layer0", modLoc("block/spawning_mop"));
            // M20: reef tanks
            basicItem(ModItems.SEA_WATER_BUCKET.get());
            basicItem(ModItems.SEA_SALT.get());
            basicItem(ModItems.CALCIUM_ADDITIVE.get());
            basicItem(ModItems.FLOW_PUMP.get());
            withExistingParent("protein_skimmer", modLoc("block/protein_skimmer"));
            withExistingParent("reef_rock", modLoc("block/reef_rock"));
            withExistingParent("live_rock", modLoc("block/live_rock"));
            // M13: aquarium plants: two grey layers per growth form, tinted per species (ItemColor)
            for (var e : ModItems.AQUATIC_PLANTS.entrySet()) {
                String form = e.getKey().form().name().toLowerCase(java.util.Locale.ROOT);
                withExistingParent(e.getKey().id(), mcLoc("item/generated")).texture("layer0", modLoc("item/plant_" + form))
                        .texture("layer1", modLoc("item/plant_" + form + "_detail"));
            }
        }
    }

    // ------------------------------------------------------------------ recipes

    static final class Recipes extends RecipeProvider {
        Recipes(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup) {
            super(out, lookup);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
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
            dropSelf(ModBlocks.REEF_ROCK.get());
            dropSelf(ModBlocks.LIVE_ROCK.get());
            dropSelf(ModBlocks.LARVA_TUB.get());
            dropSelf(ModBlocks.ARTEMIA_HATCHERY.get());
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
                                    .include(ModDataComponents.WOOD.get()))))));
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(ModBlocks.TANK.get(), ModBlocks.FEEDER.get(), ModBlocks.CLIP_FEEDER.get(), ModBlocks.TANK_LIGHT.get(), ModBlocks.EXTERNAL_FILTER.get(), ModBlocks.CABINET.get(),
                    ModBlocks.TANK_LID.get(), ModBlocks.LARVA_TUB.get(), ModBlocks.ARTEMIA_HATCHERY.get(), ModBlocks.SPAWNING_CAVE.get(),
                    ModBlocks.SPAWNING_MOP.get(), ModBlocks.PROTEIN_SKIMMER.get(), ModBlocks.REEF_ROCK.get(), ModBlocks.LIVE_ROCK.get());
        }
    }

    /** Mud mined in a betta biome sometimes drops mosquito larvae (PLAN 6.3). */
    static final class LootModifiers extends GlobalLootModifierProvider {
        LootModifiers(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup) {
            super(out, lookup, BettaCraft.MOD_ID);
        }

        @Override
        protected void start() {
            var biomes = registries.lookupOrThrow(Registries.BIOME).getOrThrow(ModTags.BETTA_SPAWN_BIOMES);
            add("mud_mosquito_larvae", new AddItemLootModifier(new LootItemCondition[]{
                    LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("blocks/mud")).build(),
                    LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiomes(biomes)).build(),
                    LootItemRandomChanceCondition.randomChance(0.04F).build()
            }, new ItemStack(ModItems.MOSQUITO_LARVAE.get())));
            // sand -> artemia cysts, chance from the server config (drops.artemiaFromSandChance)
            add("sand_artemia_cysts", new org.overengineer.bettacraft.world.ArtemiaSandLootModifier(new LootItemCondition[0],
                    new ItemStack(ModItems.ARTEMIA_CYSTS.get())));
            // M12: fishing in swamps / mangrove swamps sometimes brings up a floating plant (chance from the server config)
            add("fishing_floating_plants", new org.overengineer.bettacraft.world.FloatingPlantFishingModifier(new LootItemCondition[]{
                    LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("gameplay/fishing")).build()
            }));
            // M13: fishing in jungles, rivers and swamps sometimes brings up an aquarium plant
            add("fishing_aquatic_plants", new org.overengineer.bettacraft.world.AquaticPlantFishingModifier(new LootItemCondition[]{
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
            tag(ModTags.ARTEMIA_SAND).addTag(Tags.Blocks.SANDS);
            // M16: no tank inside a tank
            tag(ModTags.NOT_IN_TANK_INTERIOR).add(ModBlocks.TANK.get())
                    .addOptional(org.overengineer.bettacraft.compat.betterfishtanks.BetterFishTanksCompat.TANK_BLOCK);
            tag(ModTags.FLOATING_PLANTS).add(net.minecraft.world.level.block.Blocks.LILY_PAD)
                    .addOptional(Compat.rl(Compat.FINTASTIC, "duckweed")).addOptional(Compat.rl(Compat.FINTASTIC, "lotus_pad"));
            // Fintastic plants may stand in the dry interior like seagrass and kelp (M7)
            var plants = tag(ModTags.INTERIOR_WATER_PLANTS);
            for (String p : Compat.FINTASTIC_PLANTS) plants.addOptional(Compat.rl(Compat.FINTASTIC, p));
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
            tag(ModTags.PLANT_SUBSTRATE).addTag(BlockTags.SAND).addTag(BlockTags.DIRT).addTag(Tags.Blocks.GRAVELS)
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
            // M9: organic matter for the larva tub
            tag(ModTags.LARVA_TUB_FOOD).addTag(net.minecraft.tags.ItemTags.LEAVES).add(Items.WHEAT, Items.ROTTEN_FLESH, Items.BONE_MEAL);
            tag(ModTags.FRY_FOOD_START).add(ModItems.INFUSORIA.get(), ModItems.FRY_POWDER.get()); // M19: fry powder for all fry
            tag(ModTags.BOTTOM_FOOD).add(ModItems.ALGAE_WAFER.get());
            tag(ModTags.FRY_FOOD).add(ModItems.ARTEMIA_NAUPLII.get());
            tag(ModTags.ENTITY_WATER_BUCKETS).add(ModItems.BETTA_BUCKET.get());
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
            // M16: axolotls hunt small fish and fry
            var predators = tag(ModTags.PREDATORS).add(EntityType.PUFFERFISH, EntityType.AXOLOTL);
            for (var e : Compat.PREDATORS) predators.addOptional(e);
            var prey = tag(ModTags.BETTA_PREY);
            for (var e : Compat.PREY) prey.addOptional(e);
        }
    }

    static final class BiomeTagsGen extends TagsProvider<Biome> {
        BiomeTagsGen(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper efh) {
            super(out, Registries.BIOME, lookup, BettaCraft.MOD_ID, efh);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(ModTags.BETTA_SPAWN_BIOMES).addTag(Tags.Biomes.IS_SWAMP);
            tag(ModTags.SALT_WATER_BIOMES).addTag(Tags.Biomes.IS_OCEAN).addTag(Tags.Biomes.IS_BEACH); // M20: beaches too
        }
    }
}
