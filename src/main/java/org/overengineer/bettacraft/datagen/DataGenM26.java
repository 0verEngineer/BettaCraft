package org.overengineer.bettacraft.datagen;

import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.Tags;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.food.LiveCultureBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M26 part a data: the tubifex dish, the daphnia barrel and the live food pump (block states, models, recipes, tags),
 * the live food items (models, tags) and the green water culture.
 */
final class DataGenM26 {
    private DataGenM26() {}

    static List<Block> blocks() {
        return List.of(ModBlocks.TUBIFEX_DISH.get(), ModBlocks.DAPHNIA_BARREL.get(), ModBlocks.LIVE_FOOD_PUMP.get(), ModBlocks.FOOD_MAKER.get());
    }

    static void blockStates(BlockStateProvider p) {
        p.getVariantBuilder(ModBlocks.TUBIFEX_DISH.get()).forAllStates(s -> ConfiguredModel.builder().modelFile(dish(p,
                s.getValue(LiveCultureBlock.STAGE), s.getValue(LiveCultureBlock.WATER), s.getValue(LiveCultureBlock.MUD))).build());
        p.getVariantBuilder(ModBlocks.DAPHNIA_BARREL.get()).forAllStates(s -> ConfiguredModel.builder().modelFile(barrel(p,
                s.getValue(LiveCultureBlock.STAGE), s.getValue(LiveCultureBlock.WATER))).build());
        BlockModelBuilder pump = p.models().getBuilder("live_food_pump").texture("front", p.modLoc("block/live_food_pump_front"))
                .texture("side", p.modLoc("block/live_food_pump_side")).texture("top", p.modLoc("block/live_food_pump_top"))
                .texture("particle", p.modLoc("block/live_food_pump_side"));
        pump.element().from(3, 0, 3).to(13, 9, 13).allFaces((d, f) -> f.texture(d == Direction.NORTH || d == Direction.SOUTH ? "#front"
                : d.getAxis().isVertical() ? "#top" : "#side")).end();
        p.simpleBlock(ModBlocks.LIVE_FOOD_PUMP.get(), pump);
        DataGenM26b.blockStates(p); // M26 part b
    }

    /** A shallow terracotta dish (1..15 x 0..5) with mud, stale water and the worms on the mud. */
    private static ModelFile dish(BlockStateProvider p, int stage, boolean water, boolean mud) {
        String id = "tubifex_dish" + (mud ? "_mud" : "") + (water ? "_water" : "") + (stage > 0 ? "_" + stage : "");
        BlockModelBuilder m = p.models().getBuilder(id).renderType("translucent")
                .texture("side", p.modLoc("block/tubifex_dish_side")).texture("base", p.modLoc("block/tubifex_dish_bottom"))
                .texture("mud", p.mcLoc("block/mud")).texture("water", p.mcLoc("block/water_still"))
                .texture("worms", p.modLoc("block/culture_tubifex_" + Math.max(1, stage))).texture("particle", p.modLoc("block/tubifex_dish_side"));
        m.element().from(2, 0, 2).to(14, 1, 14).allFaces((d, f) -> {
            f.texture("#base");
            if (d == Direction.DOWN) f.cullface(d);
        }).end();
        walls(m, 1, 15, 0, 5);
        float floor = 1;
        if (mud) {
            m.element().from(2, 1, 2).to(14, 2.5F, 14).face(Direction.UP).uvs(2, 2, 14, 14).texture("#mud").end().end();
            floor = 2.5F;
        }
        if (stage > 0) {
            m.element().from(2, floor + 0.05F, 2).to(14, floor + 0.06F, 14).face(Direction.UP).uvs(2, 2, 14, 14).texture("#worms").end().end();
        }
        if (water) {
            m.element().from(2, 3.99F, 2).to(14, 4, 14).face(Direction.UP).uvs(2, 2, 14, 14).texture("#water").tintindex(0).end().end();
        }
        return m;
    }

    /** An open wooden barrel (1..15 x 0..15) with green water and the daphnia at the surface. */
    private static ModelFile barrel(BlockStateProvider p, int stage, boolean water) {
        String id = "daphnia_barrel" + (water ? "_water" : "") + (stage > 0 ? "_" + stage : "");
        BlockModelBuilder m = p.models().getBuilder(id).renderType("translucent")
                .texture("side", p.modLoc("block/daphnia_barrel_side")).texture("base", p.modLoc("block/daphnia_barrel_bottom"))
                .texture("water", p.mcLoc("block/water_still")).texture("fleas", p.modLoc("block/culture_daphnia_" + Math.max(1, stage)))
                .texture("particle", p.modLoc("block/daphnia_barrel_side"));
        m.element().from(2, 0, 2).to(14, 1, 14).allFaces((d, f) -> {
            f.texture("#base");
            if (d == Direction.DOWN) f.cullface(d);
        }).end();
        walls(m, 1, 15, 0, 15);
        if (water) {
            m.element().from(2, 12.99F, 2).to(14, 13, 14).face(Direction.UP).uvs(2, 2, 14, 14).texture("#water").tintindex(0).end().end();
            if (stage > 0) m.element().from(2, 13.05F, 2).to(14, 13.06F, 14).face(Direction.UP).uvs(2, 2, 14, 14).texture("#fleas").end().end();
        }
        return m;
    }

    /** Four walls, 1 px thick, around x0..x1 (both axes) from y0 to y1. */
    private static void walls(BlockModelBuilder m, float x0, float x1, float y0, float y1) {
        m.element().from(x0, y0, x0).to(x1, y1, x0 + 1).allFaces((d, f) -> f.texture("#side")).end();
        m.element().from(x0, y0, x1 - 1).to(x1, y1, x1).allFaces((d, f) -> f.texture("#side")).end();
        m.element().from(x0, y0, x0 + 1).to(x0 + 1, y1, x1 - 1).allFaces((d, f) -> f.texture("#side")).end();
        m.element().from(x1 - 1, y0, x0 + 1).to(x1, y1, x1 - 1).allFaces((d, f) -> f.texture("#side")).end();
    }

    static void itemModels(ItemModelProvider p) {
        for (String s : List.of("tubifex", "daphnia", "mulm", "green_water_bottle", "green_water_culture")) {
            p.withExistingParent(s, p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/" + s));
        }
        p.withExistingParent("tubifex_dish", p.modLoc("block/tubifex_dish_mud_water_2"));
        p.withExistingParent("daphnia_barrel", p.modLoc("block/daphnia_barrel_water_3")); // M29.1: full of water fleas
        p.withExistingParent("live_food_pump", p.modLoc("block/live_food_pump"));
        DataGenM26b.itemModels(p); // M26 part b
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.Criterion<?> hasTank) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TUBIFEX_DISH.get())
                .pattern("B B").pattern("BBB").define('B', Items.BRICK)
                .unlockedBy("has_tank", hasTank).save(out, BettaCraft.id("tubifex_dish"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DAPHNIA_BARREL.get())
                .pattern("P P").pattern("P P").pattern("SSS").define('P', ItemTags.PLANKS).define('S', ItemTags.WOODEN_SLABS)
                .unlockedBy("has_tank", hasTank).save(out, BettaCraft.id("daphnia_barrel"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.LIVE_FOOD_PUMP.get())
                .pattern("IPI").pattern("IRI").define('I', Tags.Items.INGOTS_IRON).define('P', ModItems.GLASS_PIPE.get()).define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_tank", hasTank).save(out, BettaCraft.id("live_food_pump"));
        // green water: swamp water fertilised with bone meal, ripens in the light (like the artemia culture)
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.GREEN_WATER_CULTURE.get())
                .requires(ModItems.SWAMP_WATER_BOTTLE.get()).requires(Items.BONE_MEAL)
                .unlockedBy("has_swamp_water", net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.SWAMP_WATER_BOTTLE.get()))
                .save(out, BettaCraft.id("green_water_culture"));
        DataGenM26b.recipes(out, hasTank); // M26 part b
    }

    static void blockTags(Function<TagKey<Block>, IntrinsicHolderTagsProvider.IntrinsicTagAppender<Block>> tag) {
        tag.apply(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.TUBIFEX_DISH.get(), ModBlocks.LIVE_FOOD_PUMP.get(), ModBlocks.FOOD_MAKER.get());
        tag.apply(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.DAPHNIA_BARREL.get());
    }

    static void itemTags(Function<TagKey<Item>, IntrinsicHolderTagsProvider.IntrinsicTagAppender<Item>> tag) {
        tag.apply(ModTags.BETTA_FOOD).add(ModItems.TUBIFEX.get(), ModItems.DAPHNIA.get());
        tag.apply(ModTags.LIVE_FOOD).add(ModItems.TUBIFEX.get(), ModItems.DAPHNIA.get());
        tag.apply(ModTags.CONDITIONING_FOOD).add(ModItems.TUBIFEX.get());
        tag.apply(ModTags.TUBIFEX_FOOD).add(ModItems.MULM.get());
        tag.apply(ModTags.DAPHNIA_FOOD).add(ModItems.YEAST.get(), ModItems.GREEN_WATER_BOTTLE.get());
        DataGenM26b.itemTags(tag); // M26 part b
    }
}
