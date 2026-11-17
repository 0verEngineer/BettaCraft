package org.overengineer.bettacraft.datagen;

import java.util.function.Function;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M48 part b data: the food chain of the clownfish larvae: phytoplankton and rotifer cultures (bottles that ripen like the
 * infusoria), their recipes and tags. Textures: {@code tools/assetgen/breeding.py}.
 */
final class DataGenM48 {
    private DataGenM48() {}

    static void itemModels(ItemModelProvider p) {
        p.basicItem(ModItems.PHYTOPLANKTON_CULTURE.get());
        p.basicItem(ModItems.PHYTOPLANKTON.get());
        p.basicItem(ModItems.ROTIFER_CULTURE.get());
        p.basicItem(ModItems.ROTIFERS.get());
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.Criterion<?> hasSaltWater) {
        // phytoplankton: salt water, a sea plant and some nutrients; ripens in light
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.PHYTOPLANKTON_CULTURE.get())
                .requires(ModItems.SALT_WATER_BOTTLE.get()).requires(Items.SEAGRASS).requires(Items.BONE_MEAL)
                .unlockedBy("has_salt_water", hasSaltWater).save(out, BettaCraft.id("phytoplankton_culture_seagrass"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.PHYTOPLANKTON_CULTURE.get())
                .requires(ModItems.SALT_WATER_BOTTLE.get()).requires(Items.KELP).requires(Items.BONE_MEAL)
                .unlockedBy("has_salt_water", hasSaltWater).save(out, BettaCraft.id("phytoplankton_culture_kelp"));
        // rotifers: a starter from an infusoria culture (rotifers live in it) on phytoplankton ...
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.ROTIFER_CULTURE.get())
                .requires(ModItems.PHYTOPLANKTON.get()).requires(ModItems.INFUSORIA.get())
                .unlockedBy("has_phytoplankton", net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.PHYTOPLANKTON.get()))
                .save(out, BettaCraft.id("rotifer_culture_start"));
        // ... then the culture goes on: a bottle of rotifers on two of phytoplankton makes two cultures
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.ROTIFER_CULTURE.get(), 2)
                .requires(ModItems.ROTIFERS.get()).requires(ModItems.PHYTOPLANKTON.get(), 2)
                .unlockedBy("has_rotifers", net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.ROTIFERS.get()))
                .save(out, BettaCraft.id("rotifer_culture"));
    }

    static void itemTags(Function<TagKey<Item>, IntrinsicHolderTagsProvider.IntrinsicTagAppender<Item>> tag) {
        // rotifers are the finest fry food (from the first day, like infusoria)
        tag.apply(ModTags.FRY_FOOD_START).add(ModItems.ROTIFERS.get());
        tag.apply(ModTags.BETTA_FOOD).add(ModItems.ROTIFERS.get());
    }
}
