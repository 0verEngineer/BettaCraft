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

        @Override
        protected void registerStatesAndModels() {
            // block/tank.json is hand-written (custom geometry loader bettacraft:tank)
            ModelFile tank = models().getExistingFile(modLoc("block/tank"));
            getMultipartBuilder(ModBlocks.TANK.get()).part().modelFile(tank).addModel().end();
            simpleBlock(ModBlocks.INTERIOR_FLOOR.get(), cubeAll(ModBlocks.INTERIOR_FLOOR.get()));
            simpleBlock(ModBlocks.INTERIOR_WALL.get(), models().getBuilder("interior_wall")
                    .texture("particle", modLoc("block/interior_wall")));

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

            getVariantBuilder(ModBlocks.TANK_LIGHT.get()).forAllStatesExcept(state -> {
                boolean neg = state.getValue(TankLightBlock.NEG), pos = state.getValue(TankLightBlock.POS);
                boolean hanging = state.getValue(TankLightBlock.HANGING), lit = state.getValue(TankLightBlock.LIT);
                ModelFile m = lightModel(neg, pos, hanging, lit);
                return ConfiguredModel.builder().modelFile(m)
                        .rotationY(state.getValue(TankLightBlock.AXIS) == Direction.Axis.Z ? 90 : 0).build();
            }, TankLightBlock.POWERED);
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
                    .face(Direction.DOWN).uvs(a0, 6.5F, a1, 9.5F).texture("#panel").emissivity(lit ? 15 : 0, lit ? 15 : 0).end()
                    .face(Direction.UP).uvs(a0, 6.5F, a1, 9.5F).texture("#housing").end()
                    .face(Direction.NORTH).uvs(16 - a1, 14, 16 - a0, 16).texture("#housing").end()
                    .face(Direction.SOUTH).uvs(a0, 14, a1, 16).texture("#housing").end()
                    .face(Direction.WEST).uvs(6.5F, 14, 9.5F, 16).texture("#housing").end()
                    .face(Direction.EAST).uvs(6.5F, 14, 9.5F, 16).texture("#housing").end()
                    .end();
        }
    }

    static final class ItemModels extends ItemModelProvider {
        ItemModels(PackOutput out, ExistingFileHelper efh) {
            super(out, BettaCraft.MOD_ID, efh);
        }

        @Override
        protected void registerModels() {
            withExistingParent("tank", modLoc("block/tank"));
            withExistingParent("feeder", modLoc("block/feeder"));
            withExistingParent("tank_light", modLoc("block/tank_light_none_on"));
            // bucket and filled bag: fish overlay as layer1/layer2 (tinted via ItemColor)
            withExistingParent("betta_bucket", mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/betta_bucket")).texture("layer1", modLoc("item/betta_bucket_fish")).texture("layer2", modLoc("item/betta_bucket_fish_fins"));
            ModelFile filledBag = withExistingParent("fish_bag_filled", mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/fish_bag")).texture("layer1", modLoc("item/fish_bag_fish")).texture("layer2", modLoc("item/fish_bag_fish_fins"));
            withExistingParent("fish_bag", mcLoc("item/generated")).texture("layer0", modLoc("item/fish_bag"))
                    .override().predicate(BettaCraft.id("filled"), 1.0F).model(filledBag).end();
            withExistingParent("betta_spawn_egg", mcLoc("item/template_spawn_egg"));
            basicItem(ModItems.SWAMP_WATER_BOTTLE.get());
            basicItem(ModItems.BETTA_PELLETS.get());
            basicItem(ModItems.MOSQUITO_LARVAE.get());
            basicItem(ModItems.HEATER_SMALL.get());
            basicItem(ModItems.HEATER_MEDIUM.get());
            basicItem(ModItems.HEATER_LARGE.get());
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
                    .save(asTankRecipe(output), BettaCraft.id("tank"));

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
                    .save(output.withConditions(kelp), BettaCraft.id("fish_bag_kelp"));
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.FISH_BAG.get(), 4)
                    .pattern(" S ").pattern("P P").pattern(" P ")
                    .define('S', Tags.Items.STRINGS).define('P', ModTags.PLASTIC_SHEETS)
                    .unlockedBy("has_plastic", has(ModTags.PLASTIC_SHEETS))
                    .save(output.withConditions(plastic), BettaCraft.id("fish_bag_plastic"));

            ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.BETTA_PELLETS.get(), 8)
                    .requires(Ingredient.of(Items.COD, Items.SALMON))
                    .requires(ModItems.MOSQUITO_LARVAE.get())
                    .requires(Items.BONE_MEAL)
                    .requires(Items.DRIED_KELP)
                    .unlockedBy("has_larvae", has(ModItems.MOSQUITO_LARVAE.get()))
                    .save(output, BettaCraft.id("betta_pellets"));

            Ingredient swamp = Ingredient.of(ModItems.SWAMP_WATER_BOTTLE.get());
            SimpleCookingRecipeBuilder.smelting(swamp, RecipeCategory.FOOD, ModItems.MOSQUITO_LARVAE.get(), 0.1F, 200)
                    .unlockedBy("has_swamp_water", has(ModItems.SWAMP_WATER_BOTTLE.get())).save(output, BettaCraft.id("mosquito_larvae_smelting"));
            SimpleCookingRecipeBuilder.smoking(swamp, RecipeCategory.FOOD, ModItems.MOSQUITO_LARVAE.get(), 0.1F, 100)
                    .unlockedBy("has_swamp_water", has(ModItems.SWAMP_WATER_BOTTLE.get())).save(output, BettaCraft.id("mosquito_larvae_smoking"));
            SimpleCookingRecipeBuilder.campfireCooking(swamp, RecipeCategory.FOOD, ModItems.MOSQUITO_LARVAE.get(), 0.1F, 600)
                    .unlockedBy("has_swamp_water", has(ModItems.SWAMP_WATER_BOTTLE.get())).save(output, BettaCraft.id("mosquito_larvae_campfire"));

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.HEATER_SMALL.get())
                    .pattern("C").pattern("R").pattern("G")
                    .define('C', Tags.Items.INGOTS_COPPER).define('R', Tags.Items.DUSTS_REDSTONE).define('G', Tags.Items.GLASS_PANES)
                    .unlockedBy("has_copper", has(Tags.Items.INGOTS_COPPER))
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
                    .save(output, BettaCraft.id("feeder"));
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.TANK_LIGHT.get(), 3)
                    .pattern("III").pattern("GDG")
                    .define('I', Tags.Items.INGOTS_IRON).define('G', Tags.Items.GLASS_PANES).define('D', Tags.Items.DUSTS_GLOWSTONE)
                    .unlockedBy("has_glowstone", has(Tags.Items.DUSTS_GLOWSTONE))
                    .save(output, BettaCraft.id("tank_light"));
        }

        /** Turns the ShapedRecipe produced by the builder into our {@link TankRecipe} type. */
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
                                    .include(ModDataComponents.GLASS.get()))))));
            dropSelf(ModBlocks.FEEDER.get());
            dropSelf(ModBlocks.TANK_LIGHT.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(ModBlocks.TANK.get(), ModBlocks.FEEDER.get(), ModBlocks.TANK_LIGHT.get());
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
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.FEEDER.get(), ModBlocks.TANK_LIGHT.get());
        }
    }

    static final class ItemTagsGen extends ItemTagsProvider {
        ItemTagsGen(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup,
                    CompletableFuture<TagsProvider.TagLookup<Block>> blocks, ExistingFileHelper efh) {
            super(out, lookup, blocks, BettaCraft.MOD_ID, efh);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(ModTags.BETTA_FOOD).add(ModItems.BETTA_PELLETS.get(), ModItems.MOSQUITO_LARVAE.get());
            tag(ModTags.ENTITY_WATER_BUCKETS).add(ModItems.BETTA_BUCKET.get());
            // plastic of other mods (all optional): Industrial Foregoing, PneumaticCraft, Mekanism
            tag(ModTags.PLASTIC_SHEETS)
                    .addOptionalTag(ResourceLocation.fromNamespaceAndPath("c", "plastics"))
                    .addOptionalTag(ResourceLocation.fromNamespaceAndPath("pneumaticcraft", "plastic_sheets"))
                    .addOptional(ResourceLocation.fromNamespaceAndPath("mekanism", "hdpe_sheet"));
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
            tag(ModTags.BFT_CATCHABLE).addOptional(BettaCraft.id("betta"));
            tag(ModTags.TANK_INHABITANTS).add(EntityType.AXOLOTL, EntityType.TADPOLE);
            // giant fish of other mods
            tag(ModTags.TANK_BLACKLIST)
                    .addOptional(ResourceLocation.fromNamespaceAndPath("fintastic", "arapaima"))
                    .addOptional(ResourceLocation.fromNamespaceAndPath("fintastic", "coelacanth"));
            tag(ModTags.PREDATORS).add(EntityType.PUFFERFISH)
                    .addOptional(ResourceLocation.fromNamespaceAndPath("naturalist", "piranha"))
                    .addOptional(ResourceLocation.fromNamespaceAndPath("naturalist", "anglerfish"))
                    .addOptional(ResourceLocation.fromNamespaceAndPath("fishofthieves", "battlegill"))
                    .addOptional(ResourceLocation.fromNamespaceAndPath("fishofthieves", "wrecker"))
                    .addOptional(ResourceLocation.fromNamespaceAndPath("fishofthieves", "devilfish"));
        }
    }

    static final class BiomeTagsGen extends TagsProvider<Biome> {
        BiomeTagsGen(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper efh) {
            super(out, Registries.BIOME, lookup, BettaCraft.MOD_ID, efh);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(ModTags.BETTA_SPAWN_BIOMES).addTag(Tags.Biomes.IS_SWAMP);
        }
    }
}
