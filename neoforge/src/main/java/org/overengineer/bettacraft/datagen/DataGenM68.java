package org.overengineer.bettacraft.datagen;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import org.overengineer.bettacraft.datagen.model.ItemModelProvider;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M68 data: the pipe wrench (item model, recipe). The pipes' models with the rings of their roles are in {@link PipeModels}
 * (called by {@link DataGenM24}); the filter pipes, their T-piece, the interior pipe item and the filter's water ports are gone.
 */
final class DataGenM68 {
    private DataGenM68() {}

    static void itemModels(ItemModelProvider p) {
        p.withExistingParent("pipe_wrench", p.mcLoc("item/handheld")).texture("layer0", p.modLoc("item/pipe_wrench"));
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.Criterion<?> hasPipe) {
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.TOOLS, ModItems.PIPE_WRENCH.get())
                .pattern(" S ").pattern(" IS").pattern("I  ").define('S', ModTags.STAINLESS_STEEL_INGOTS).define('I', Items.IRON_INGOT)
                .unlockedBy("has_pipe", hasPipe).save(out, RecipeCompat.key(BettaCraft.id("pipe_wrench")));
    }
}
