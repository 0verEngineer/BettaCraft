package org.overengineer.bettacraft.datagen;

import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.overengineer.bettacraft.datagen.model.BlockModelBuilder;
import org.overengineer.bettacraft.datagen.model.BlockStateProvider;
import org.overengineer.bettacraft.datagen.model.ConfiguredModel;
import org.overengineer.bettacraft.datagen.model.ItemModelProvider;
import org.overengineer.bettacraft.platform.Tags;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.reef.CalciumReactorBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M49 data: part a the sump, the Chaetomorpha of its refugium, the calcium reactor and its coral rubble (block state, models,
 * recipes, tags). Textures: {@code tools/assetgen/sump_brackish.py}.
 */
final class DataGenM49 {
    private DataGenM49() {}

    static List<Block> blocks() {
        return List.of(ModBlocks.CALCIUM_REACTOR.get());
    }

    static void blockStates(BlockStateProvider p) {
        // a dark pump foot and a clear column of coral rubble on it (bubbles while it runs)
        BlockModelBuilder[] m = new BlockModelBuilder[2];
        for (int i = 0; i < 2; i++) {
            String col = i == 0 ? "block/calcium_reactor_column" : "block/calcium_reactor_column_on";
            m[i] = p.models().getBuilder(i == 0 ? "calcium_reactor" : "calcium_reactor_on").renderType("cutout")
                    .texture("base", p.modLoc("block/calcium_reactor_base")).texture("front", p.modLoc("block/calcium_reactor_front"))
                    .texture("column", p.modLoc(col)).texture("top", p.modLoc("block/calcium_reactor_top")).texture("particle", p.modLoc("block/calcium_reactor_base"));
            m[i].element().from(3, 0, 3).to(13, 4, 13).allFaces((d, f) -> f.texture(d == Direction.NORTH ? "#front" : "#base")).end();
            m[i].element().from(4, 4, 4).to(12, 14, 12).allFaces((d, f) -> f.texture(d.getAxis().isVertical() ? "#top" : "#column")).end();
            m[i].element().from(5, 14, 5).to(11, 15, 11).allFaces((d, f) -> f.texture("#base")).end();
        }
        p.getVariantBuilder(ModBlocks.CALCIUM_REACTOR.get()).forAllStates(s -> ConfiguredModel.builder()
                .modelFile(m[s.getValue(CalciumReactorBlock.ACTIVE) ? 1 : 0])
                .rotationY(((int) s.getValue(CalciumReactorBlock.FACING).toYRot() + 180) % 360).build());
    }

    static void itemModels(ItemModelProvider p) {
        p.basicItem(ModItems.SUMP.get());
        p.basicItem(ModItems.CHAETOMORPHA.get());
        p.basicItem(ModItems.CORAL_RUBBLE.get());
        p.withExistingParent("calcium_reactor", p.modLoc("block/calcium_reactor"));
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.Criterion<?> hasCabinet, net.minecraft.advancements.Criterion<?> hasSaltWater) {
        // the sump: a glass box with a glass baffle (it goes into a cabinet)
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.SUMP.get())
                .pattern("G G").pattern("GPG").pattern("GGG")
                .define('G', Tags.Items.GLASS_BLOCKS).define('P', Tags.Items.GLASS_PANES)
                .unlockedBy("has_cabinet", hasCabinet).save(out, RecipeCompat.key(BettaCraft.id("sump")));
        // the calcium reactor: a glass column on a small pump
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.CALCIUM_REACTOR.get())
                .pattern("IGI").pattern("GBG").pattern("IRI")
                .define('I', Tags.Items.INGOTS_IRON).define('G', Tags.Items.GLASS_PANES).define('B', Items.BUCKET).define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_salt_water", hasSaltWater).save(out, RecipeCompat.key(BettaCraft.id("calcium_reactor")));
        // coral rubble: broken dead coral blocks, or calcite (both calcium carbonate)
        for (Item dead : List.of(Items.DEAD_TUBE_CORAL_BLOCK, Items.DEAD_BRAIN_CORAL_BLOCK, Items.DEAD_BUBBLE_CORAL_BLOCK, Items.DEAD_FIRE_CORAL_BLOCK,
                Items.DEAD_HORN_CORAL_BLOCK)) {
            String name = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(dead).getPath();
            ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.CORAL_RUBBLE.get(), 4).requires(dead)
                    .unlockedBy("has_salt_water", hasSaltWater).save(out, RecipeCompat.key(BettaCraft.id("coral_rubble_from_" + name)));
        }
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.CORAL_RUBBLE.get(), 2).requires(Items.CALCITE)
                .unlockedBy("has_salt_water", hasSaltWater).save(out, RecipeCompat.key(BettaCraft.id("coral_rubble_from_calcite")));
        // part b: the tall mangrove stilt roots (decoration)
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.DECORATIONS, ModItems.DECOR.get(org.overengineer.bettacraft.decor.DecorType.MANGROVE_STILT_ROOTS).get())
                .requires(Items.MANGROVE_ROOTS).requires(Items.MANGROVE_ROOTS).requires(Items.MANGROVE_ROOTS).requires(Items.MANGROVE_ROOTS)
                .requires(Items.MANGROVE_LOG).requires(Items.MANGROVE_LOG).requires(Items.MUD)
                .unlockedBy("has_mangrove_roots", net.minecraft.advancements.criterion.InventoryChangeTrigger.TriggerInstance.hasItems(Items.MANGROVE_ROOTS))
                .save(out, RecipeCompat.key(BettaCraft.id("decor_mangrove_stilt_roots")));
    }

    static void blockTags(Function<TagKey<Block>, net.minecraft.data.tags.TagAppender<Block, Block>> tag) {
        tag.apply(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.CALCIUM_REACTOR.get());
    }

    static void itemTags(Function<TagKey<Item>, net.minecraft.data.tags.TagAppender<Item, Item>> tag) {
        // the harvested Chaetomorpha: food for the shrimp and snails, a vegetable for the food maker
        tag.apply(ModTags.CRITTER_FOOD).add(ModItems.CHAETOMORPHA.get());
        tag.apply(ModTags.FOOD_VEGETABLES).add(ModItems.CHAETOMORPHA.get());
    }
}
