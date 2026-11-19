package org.overengineer.bettacraft.datagen;

import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.overengineer.bettacraft.datagen.model.BlockModelBuilder;
import org.overengineer.bettacraft.datagen.model.BlockStateProvider;
import org.overengineer.bettacraft.datagen.model.ConfiguredModel;
import org.overengineer.bettacraft.datagen.model.ItemModelProvider;
import org.overengineer.bettacraft.datagen.model.ModelFile;
import org.overengineer.bettacraft.platform.Tags;
import org.overengineer.bettacraft.platform.crafting.SizedIngredient;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.food.FoodDiet;
import org.overengineer.bettacraft.food.FoodMakerBlock;
import org.overengineer.bettacraft.food.FoodMakerRecipe;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M26 part b data: the food maker (block states with the crank, models, recipe), its recipes ({@code bettacraft:food_maker}),
 * the new foods (models, tags), the food groups and the diets (favourite / poor food).
 */
final class DataGenM26b {
    private DataGenM26b() {}

    static void blockStates(BlockStateProvider p) {
        ModelFile[] crank = new ModelFile[4];
        for (int i = 0; i < 4; i++) crank[i] = maker(p, i);
        p.getVariantBuilder(ModBlocks.FOOD_MAKER.get()).forAllStates(s -> ConfiguredModel.builder()
                .modelFile(crank[s.getValue(FoodMakerBlock.CRANK)])
                .rotationY(((int) s.getValue(FoodMakerBlock.FACING).toYRot() + 180) % 360).build());
    }

    /** A steel case (2..14 x 0..11) with a feed hopper on top and the crank on the east side, handle in one of four positions. */
    private static ModelFile maker(BlockStateProvider p, int crank) {
        BlockModelBuilder m = p.models().getBuilder("food_maker_" + crank)
                .texture("front", p.modLoc("block/food_maker_front")).texture("side", p.modLoc("block/food_maker_side"))
                .texture("top", p.modLoc("block/food_maker_top")).texture("hopper", p.modLoc("block/food_maker_hopper"))
                .texture("crank", p.modLoc("block/food_maker_crank")).texture("knob", p.modLoc("block/food_maker_knob"))
                .texture("particle", p.modLoc("block/food_maker_side"));
        m.element().from(2, 0, 2).to(14, 11, 14).allFaces((d, f) -> {
            f.texture(d == Direction.NORTH ? "#front" : d.getAxis().isVertical() ? "#top" : "#side");
            if (d == Direction.DOWN) f.cullface(d);
        }).end();
        // the feed hopper
        m.element().from(4, 11, 4).to(12, 15, 12).allFaces((d, f) -> f.texture("#hopper")).end();
        // axle and crank
        m.element().from(14, 5, 7).to(15, 7, 9).allFaces((d, f) -> f.texture("#crank")).end();
        float[][] arm = {{6, 10.5F, 7.5F, 8.5F}, {5.5F, 6.5F, 3.5F, 8.5F}, {1.5F, 6, 7.5F, 8.5F}, {5.5F, 6.5F, 7.5F, 12.5F}};
        float[][] knob = {{9, 11, 7, 9}, {5, 7, 3, 5}, {1, 3, 7, 9}, {5, 7, 11, 13}};
        float[] a = arm[crank], k = knob[crank];
        m.element().from(15, a[0], a[2]).to(16, a[1], a[3]).allFaces((d, f) -> f.texture("#crank")).end();
        m.element().from(16, k[0], k[2]).to(17.5F, k[1], k[3]).allFaces((d, f) -> f.texture("#knob")).end();
        return m;
    }

    static void itemModels(ItemModelProvider p) {
        for (String s : List.of("fish_flakes", "food_granules", "shrimp_food", "frozen_mosquito_larvae", "frozen_artemia", "frozen_daphnia", "shell_food")) {
            p.withExistingParent(s, p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/" + s));
        }
        DataGenM291.foodMaker(p); // M29.1: a 3D icon with a funnel hopper and a big crank
    }

    private static SizedIngredient in(ItemLike item, int n) {
        return new SizedIngredient(Ingredient.of(item), n);
    }

    private static SizedIngredient in(TagKey<Item> tag, int n) {
        return new SizedIngredient(RecipeCompat.tag(tag), n);
    }

    private static void make(RecipeOutput out, String id, ItemLike result, int n, SizedIngredient... ingredients) {
        out.accept(RecipeCompat.key(BettaCraft.id("food_maker/" + id)), new FoodMakerRecipe(List.of(ingredients), new net.minecraft.world.item.ItemStackTemplate(result.asItem(), n)), null);
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.Criterion<?> hasTank) {
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.FOOD_MAKER.get())
                .pattern("IHI").pattern("IGI").pattern("III")
                .define('I', Tags.Items.INGOTS_IRON).define('H', Items.HOPPER).define('G', Items.GRINDSTONE)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("food_maker")));
        // dry food from vanilla ingredients (fish and spider eyes are the protein, wheat binds, kelp and vegetables are the plants)
        make(out, "betta_pellets", ModItems.BETTA_PELLETS.get(), 12, in(ModTags.PELLET_FISH, 1), in(Items.WHEAT, 1), in(Items.DRIED_KELP, 1));
        make(out, "fish_flakes", ModItems.FISH_FLAKES.get(), 12, in(ModTags.PELLET_FISH, 1), in(Items.WHEAT, 1), in(Items.KELP, 1), in(Items.SUGAR, 1));
        make(out, "food_granules", ModItems.FOOD_GRANULES.get(), 12, in(Items.SPIDER_EYE, 1), in(Items.WHEAT, 2), in(Items.DRIED_KELP, 1));
        make(out, "algae_wafer", ModItems.ALGAE_WAFER.get(), 8, in(Items.DRIED_KELP, 2), in(ModTags.FOOD_VEGETABLES, 1), in(Items.WHEAT, 1));
        make(out, "shrimp_food", ModItems.SHRIMP_FOOD.get(), 8, in(Items.KELP, 1), in(ModTags.FOOD_VEGETABLES, 1), in(Items.BONE_MEAL, 1));
        make(out, "fry_powder", ModItems.FRY_POWDER.get(), 8, in(Items.EGG, 1), in(Items.WHEAT, 1));
        // frozen food: live food frozen with ice (it keeps, stacks and is eaten like live food)
        make(out, "frozen_mosquito_larvae", ModItems.FROZEN_MOSQUITO_LARVAE.get(), 4, in(ModItems.MOSQUITO_LARVAE.get(), 4), in(ModTags.FREEZING_ICE, 1));
        make(out, "frozen_artemia", ModItems.FROZEN_ARTEMIA.get(), 4, in(ModItems.ARTEMIA_NAUPLII.get(), 4), in(ModTags.FREEZING_ICE, 1));
        make(out, "frozen_daphnia", ModItems.FROZEN_DAPHNIA.get(), 4, in(ModItems.DAPHNIA.get(), 4), in(ModTags.FREEZING_ICE, 1));
        // M29 part b: hard food for the puffers: crushed shells (bone meal) bound with fish and kelp
        make(out, "shell_food", ModItems.SHELL_FOOD.get(), 8, in(Items.BONE_MEAL, 2), in(ModTags.PELLET_FISH, 1), in(Items.DRIED_KELP, 1));
    }

    private static TagKey<Item> c(String path) {
        return TagKey.create(net.minecraft.core.registries.Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
    }

    static void itemTags(Function<TagKey<Item>, net.minecraft.data.tags.TagAppender<Item, Item>> tag) {
        Item flakes = ModItems.FISH_FLAKES.get(), granules = ModItems.FOOD_GRANULES.get(), shrimp = ModItems.SHRIMP_FOOD.get();
        Item[] frozen = {ModItems.FROZEN_MOSQUITO_LARVAE.get(), ModItems.FROZEN_ARTEMIA.get(), ModItems.FROZEN_DAPHNIA.get()};
        tag.apply(ModTags.BETTA_FOOD).add(flakes, granules, shrimp).add(frozen);
        tag.apply(ModTags.FROZEN_FOOD).add(frozen);
        tag.apply(ModTags.CRITTER_FOOD).add(shrimp);
        tag.apply(ModTags.SINKING_FOOD).add(granules, ModItems.ALGAE_WAFER.get(), ModItems.SHELL_FOOD.get());
        // M29 part b: shell food, hard food for the puffers' teeth (sinks like granules)
        tag.apply(ModTags.BETTA_FOOD).add(ModItems.SHELL_FOOD.get());
        tag.apply(ModTags.HARD_FOOD).add(ModItems.SHELL_FOOD.get());
        tag.apply(ModTags.FOOD_VEGETABLES).add(Items.CARROT, Items.PUMPKIN, Items.MELON_SLICE, Items.BEETROOT).addOptionalTag(c("foods/vegetable"));
        tag.apply(ModTags.FREEZING_ICE).add(Items.ICE, Items.PACKED_ICE, Items.BLUE_ICE, Items.SNOW_BLOCK);
        // the food groups
        tag.apply(FoodDiet.Group.PELLETS.tag).add(ModItems.BETTA_PELLETS.get());
        tag.apply(FoodDiet.Group.FLAKES.tag).add(flakes);
        tag.apply(FoodDiet.Group.GRANULES.tag).add(granules, ModItems.SHELL_FOOD.get());
        tag.apply(FoodDiet.Group.WAFERS.tag).add(ModItems.ALGAE_WAFER.get());
        tag.apply(FoodDiet.Group.FROZEN.tag).addTag(ModTags.FROZEN_FOOD);
        tag.apply(FoodDiet.Group.LIVE.tag).addTag(ModTags.LIVE_FOOD);
        // what each diet likes and what suits it badly
        diet(tag, FoodDiet.Diet.BETTA, List.of(FoodDiet.Group.LIVE, FoodDiet.Group.FROZEN), List.of(FoodDiet.Group.WAFERS));
        diet(tag, FoodDiet.Diet.OMNIVORE, List.of(FoodDiet.Group.FLAKES, FoodDiet.Group.FROZEN), List.of(FoodDiet.Group.WAFERS));
        diet(tag, FoodDiet.Diet.BOTTOM, List.of(FoodDiet.Group.GRANULES, FoodDiet.Group.WAFERS, FoodDiet.Group.FROZEN), List.of(FoodDiet.Group.FLAKES));
        diet(tag, FoodDiet.Diet.GRAZER, List.of(FoodDiet.Group.WAFERS), List.of(FoodDiet.Group.LIVE, FoodDiet.Group.FROZEN, FoodDiet.Group.FLAKES));
        diet(tag, FoodDiet.Diet.SEAHORSE, List.of(FoodDiet.Group.FROZEN), List.of());
        // M29 part b: puffers like hard, frozen and live food and take flakes and pellets badly
        diet(tag, FoodDiet.Diet.PUFFER, List.of(FoodDiet.Group.FROZEN, FoodDiet.Group.LIVE), List.of(FoodDiet.Group.FLAKES, FoodDiet.Group.PELLETS));
        tag.apply(FoodDiet.Diet.PUFFER.favourite).addTag(ModTags.HARD_FOOD);
        // M30: big predators like frozen and live food, take flakes and wafers badly
        diet(tag, FoodDiet.Diet.CARNIVORE, List.of(FoodDiet.Group.FROZEN, FoodDiet.Group.LIVE), List.of(FoodDiet.Group.FLAKES, FoodDiet.Group.WAFERS));
    }

    private static void diet(Function<TagKey<Item>, net.minecraft.data.tags.TagAppender<Item, Item>> tag, FoodDiet.Diet d,
                             List<FoodDiet.Group> favourite, List<FoodDiet.Group> poor) {
        var f = tag.apply(d.favourite);
        for (FoodDiet.Group g : favourite) f.addTag(g.tag);
        var p = tag.apply(d.poor);
        for (FoodDiet.Group g : poor) p.addTag(g.tag);
    }
}
