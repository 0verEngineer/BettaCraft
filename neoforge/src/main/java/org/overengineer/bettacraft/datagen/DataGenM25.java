package org.overengineer.bettacraft.datagen;

import java.util.List;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.overengineer.bettacraft.datagen.model.ItemModelProvider;
import org.overengineer.bettacraft.platform.Tags;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModItems;

/** M25 data: item models and recipes of the water chemistry items. */
final class DataGenM25 {
    private DataGenM25() {}

    static void itemModels(ItemModelProvider p) {
        for (String s : List.of("tds_meter", "ph_test_kit", "remineralising_salt", "almond_leaf")) {
            p.withExistingParent(s, p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/" + s));
        }
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.triggers.Criterion<?> hasTank) {
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.TOOLS, ModItems.TDS_METER.get())
                .requires(Tags.Items.INGOTS_IRON).requires(Tags.Items.INGOTS_COPPER).requires(Tags.Items.DUSTS_REDSTONE).requires(Tags.Items.GLASS_PANES)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("tds_meter")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.TOOLS, ModItems.PH_TEST_KIT.get())
                .requires(Items.GLASS_BOTTLE).requires(Items.PAPER).requires(Tags.Items.DYES_RED).requires(Tags.Items.DYES_YELLOW).requires(Tags.Items.DYES_BLUE)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("ph_test_kit")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.REMINERALISING_SALT.get(), 4)
                .requires(Items.CALCITE).requires(Items.BONE_MEAL).requires(Items.BONE_MEAL)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("remineralising_salt")));
        // dried almond (catappa) leaves: jungle leaves dried in a smoker, on a campfire or in a furnace
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Items.JUNGLE_LEAVES), RecipeCategory.MISC, net.minecraft.world.item.crafting.CookingBookCategory.MISC, ModItems.ALMOND_LEAF.get(), 0.05F, 200)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("almond_leaf")));
        SimpleCookingRecipeBuilder.smoking(Ingredient.of(Items.JUNGLE_LEAVES), RecipeCategory.MISC, ModItems.ALMOND_LEAF.get(), 0.05F, 100)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("almond_leaf_smoking")));
        SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(Items.JUNGLE_LEAVES), RecipeCategory.MISC, ModItems.ALMOND_LEAF.get(), 0.05F, 600)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("almond_leaf_campfire")));
    }
}
