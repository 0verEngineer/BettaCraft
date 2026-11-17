package org.overengineer.bettacraft.datagen;

import java.util.List;
import java.util.function.Function;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import org.overengineer.bettacraft.datagen.model.BlockStateProvider;
import org.overengineer.bettacraft.datagen.model.ConfiguredModel;
import org.overengineer.bettacraft.datagen.model.ItemModelProvider;
import org.overengineer.bettacraft.datagen.model.ModelFile;
import org.overengineer.bettacraft.platform.Tags;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.decor.DecorType;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M28 part c data: the leaf litter blocks (almond leaves and alder cones on the floor), the alder cone, the recipes of
 * the new decorations and the shelter tag for the shrimp.
 *
 * <p>Decoration recipes: one item each from the materials the build is mostly made of. Like the older decorations
 * (open point "decor yield") a placed build is more blocks than its recipe costs; the new ones use cheap blocks
 * (stone, planks, terracotta, no ores or metal blocks) so mining them gives nothing valuable.
 */
final class DataGenM28 {
    private DataGenM28() {}

    static List<Block> blocks() {
        return List.of(ModBlocks.ALMOND_LEAF_LITTER.get(), ModBlocks.ALDER_CONES.get());
    }

    static void blockStates(BlockStateProvider p) {
        ModelFile leaf = p.models().withExistingParent("almond_leaf_litter", p.mcLoc("block/carpet"))
                .texture("wool", p.modLoc("block/almond_leaf_litter")).texture("particle", p.modLoc("block/almond_leaf_litter")).renderType("cutout");
        p.getVariantBuilder(ModBlocks.ALMOND_LEAF_LITTER.get()).partialState().setModels(
                new ConfiguredModel(leaf), new ConfiguredModel(leaf, 0, 90, false), new ConfiguredModel(leaf, 0, 180, false), new ConfiguredModel(leaf, 0, 270, false));
        ModelFile cones = p.models().withExistingParent("alder_cones", p.mcLoc("block/cross"))
                .texture("cross", p.modLoc("block/alder_cones")).texture("particle", p.modLoc("block/alder_cones")).renderType("cutout");
        p.getVariantBuilder(ModBlocks.ALDER_CONES.get()).partialState().setModels(new ConfiguredModel(cones), new ConfiguredModel(cones, 0, 90, false));
    }

    static void itemModels(ItemModelProvider p) {
        p.withExistingParent("alder_cone", p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/alder_cone"));
    }

    private static Item decor(DecorType t) {
        return ModItems.DECOR.get(t).get();
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.Criterion<?> hasTank) {
        // alder cones: birch leaves dried like the almond leaves (alder and birch are one family)
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Items.BIRCH_LEAVES), RecipeCategory.MISC, ModItems.ALDER_CONE.get(), 0.05F, 200)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("alder_cone")));
        SimpleCookingRecipeBuilder.smoking(Ingredient.of(Items.BIRCH_LEAVES), RecipeCategory.MISC, ModItems.ALDER_CONE.get(), 0.05F, 100)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("alder_cone_smoking")));
        SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(Items.BIRCH_LEAVES), RecipeCategory.MISC, ModItems.ALDER_CONE.get(), 0.05F, 600)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("alder_cone_campfire")));
        // wrecks
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.DECORATIONS, decor(DecorType.ROWING_BOAT))
                .requires(ItemTags.BOATS).requires(ItemTags.PLANKS).requires(Items.MOSS_CARPET)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("decor_rowing_boat")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.DECORATIONS, decor(DecorType.SHIP_BOW))
                .pattern(" SW").pattern("PCP").pattern("LPL")
                .define('S', Tags.Items.RODS_WOODEN).define('W', Items.WHITE_WOOL).define('P', ItemTags.PLANKS).define('L', ItemTags.LOGS)
                .define('C', ModItems.SPAWNING_CAVE.get())
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("decor_ship_bow")));
        // stones
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.DECORATIONS, decor(DecorType.HOLEY_ROCK))
                .requires(Items.CALCITE).requires(Items.CALCITE).requires(Items.CALCITE).requires(Items.DRIPSTONE_BLOCK)
                .unlockedBy("has_calcite", net.minecraft.advancements.criterion.InventoryChangeTrigger.TriggerInstance.hasItems(Items.CALCITE)).save(out, RecipeCompat.key(BettaCraft.id("decor_holey_rock")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.DECORATIONS, decor(DecorType.SLATE_CAVE))
                .requires(Items.DEEPSLATE_TILES).requires(Items.DEEPSLATE_TILES).requires(Items.POLISHED_DEEPSLATE).requires(Items.COBBLED_DEEPSLATE)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("decor_slate_cave")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.DECORATIONS, decor(DecorType.SEIRYU_STONE))
                .requires(Items.STONE).requires(Items.STONE).requires(Items.ANDESITE).requires(Items.TUFF).requires(Items.CALCITE)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("decor_seiryu_stone")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.DECORATIONS, decor(DecorType.DRAGON_STONE))
                .requires(Items.TERRACOTTA).requires(Items.TERRACOTTA).requires(Items.BROWN_TERRACOTTA).requires(Items.PACKED_MUD)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("decor_dragon_stone")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.DECORATIONS, decor(DecorType.LAVA_ROCK))
                .requires(Items.BLACKSTONE).requires(Items.BLACKSTONE).requires(Items.RED_TERRACOTTA).requires(Items.MAGMA_BLOCK)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("decor_lava_rock")));
        // roots
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.DECORATIONS, decor(DecorType.MANGROVE_ROOT))
                .requires(Items.MANGROVE_ROOTS).requires(Items.MANGROVE_ROOTS).requires(Items.MANGROVE_ROOTS).requires(Items.MANGROVE_LOG)
                .unlockedBy("has_mangrove_roots", net.minecraft.advancements.criterion.InventoryChangeTrigger.TriggerInstance.hasItems(Items.MANGROVE_ROOTS)).save(out, RecipeCompat.key(BettaCraft.id("decor_mangrove_root")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.DECORATIONS, decor(DecorType.SPIDERWOOD))
                .requires(Items.STRIPPED_OAK_LOG).requires(Items.OAK_LOG).requires(Tags.Items.RODS_WOODEN).requires(Tags.Items.RODS_WOODEN)
                .requires(Tags.Items.RODS_WOODEN)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("decor_spiderwood")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.DECORATIONS, decor(DecorType.BOGWOOD_BRIDGE))
                .requires(Items.DARK_OAK_LOG).requires(Items.DARK_OAK_LOG).requires(Items.DARK_OAK_LOG).requires(Items.MUD)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("decor_bogwood_bridge")));
        // caves and tubes
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.DECORATIONS, decor(DecorType.COCONUT_CAVE))
                .requires(Items.BROWN_TERRACOTTA).requires(Items.BROWN_TERRACOTTA).requires(Items.BROWN_WOOL).requires(Items.BOWL)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("decor_coconut_cave")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.DECORATIONS, decor(DecorType.CLAY_TUBES))
                .pattern("CCC").pattern("C C").pattern("CCC")
                .define('C', ModItems.SPAWNING_CAVE.get())
                .unlockedBy("has_spawning_cave", net.minecraft.advancements.criterion.InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.SPAWNING_CAVE.get())).save(out, RecipeCompat.key(BettaCraft.id("decor_clay_tubes")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.DECORATIONS, decor(DecorType.SHRIMP_TUBES))
                .requires(Items.TERRACOTTA).requires(Items.ORANGE_TERRACOTTA).requires(Items.WHITE_TERRACOTTA).requires(Items.CLAY_BALL)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("decor_shrimp_tubes")));
        // kitsch
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.DECORATIONS, decor(DecorType.RUIN_COLUMN))
                .requires(Items.POLISHED_DIORITE).requires(Items.POLISHED_DIORITE).requires(Items.DIORITE).requires(Items.MOSS_CARPET)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("decor_ruin_column")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.DECORATIONS, decor(DecorType.DIVER_HELMET))
                .pattern("TTT").pattern("GCG").pattern("TTT")
                .define('T', Items.ORANGE_TERRACOTTA).define('G', Tags.Items.GLASS_PANES).define('C', Tags.Items.INGOTS_COPPER)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("decor_diver_helmet")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.DECORATIONS, decor(DecorType.TREASURE_CHEST))
                .requires(Items.CHEST).requires(ModItems.AIR_STONE.get()).requires(Tags.Items.NUGGETS_GOLD).requires(Items.YELLOW_CONCRETE)
                .unlockedBy("has_air_stone", net.minecraft.advancements.criterion.InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.AIR_STONE.get())).save(out, RecipeCompat.key(BettaCraft.id("decor_treasure_chest")));
    }

    static void blockTags(Function<TagKey<Block>, net.minecraft.data.tags.TagAppender<Block, Block>> tag) {
        tag.apply(ModTags.SHRIMP_SHELTER).addTag(BlockTags.TERRACOTTA).add(ModBlocks.SPAWNING_CAVE.get());
        // M29: the coconut shell and the slate cave are spawning caves of the Apistogramma
        tag.apply(ModTags.SMALL_CAVES).add(net.minecraft.world.level.block.Blocks.BROWN_TERRACOTTA, net.minecraft.world.level.block.Blocks.BROWN_WOOL,
                net.minecraft.world.level.block.Blocks.DEEPSLATE_TILES, net.minecraft.world.level.block.Blocks.POLISHED_DEEPSLATE,
                net.minecraft.world.level.block.Blocks.COBBLED_DEEPSLATE, net.minecraft.world.level.block.Blocks.DEEPSLATE_BRICKS,
                net.minecraft.world.level.block.Blocks.DEEPSLATE_TILE_SLAB, net.minecraft.world.level.block.Blocks.POLISHED_DEEPSLATE_SLAB);
    }
}
