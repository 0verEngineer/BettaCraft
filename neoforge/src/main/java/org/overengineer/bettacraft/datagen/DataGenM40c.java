package org.overengineer.bettacraft.datagen;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.overengineer.bettacraft.datagen.model.ItemModelProvider;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M40 part c data: the pipe cover (item model, recipe) and the block tag of the Cable Facades mod that lets its facades on
 * our filter pipes and bundles ({@code cable_facades:supports_facade}; only a tag file of its namespace in our data, its config
 * is not touched; see {@code compat.facades.CableFacadesCompat}).
 */
final class DataGenM40c {
    private DataGenM40c() {}

    /** The tag Cable Facades reads (CFItemTags.SUPPORTS_FACADE in its jar). */
    static final TagKey<Block> CABLE_FACADES_SUPPORTS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("cable_facades", "supports_facade"));

    static void itemModels(ItemModelProvider p) {
        p.withExistingParent("pipe_cover", p.mcLoc("item/handheld")).texture("layer0", p.modLoc("item/pipe_cover"));
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.triggers.Criterion<?> hasPipe) {
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.TOOLS, ModItems.PIPE_COVER.get())
                .pattern("SS ").pattern("SB ").pattern("  T").define('S', ModTags.STAINLESS_STEEL_INGOTS).define('B', Items.BRICK)
                .define('T', Items.STICK).unlockedBy("has_filter_pipe", hasPipe).save(out, RecipeCompat.key(BettaCraft.id("pipe_cover")));
    }

    static void blockTags(java.util.function.Function<TagKey<Block>, ObjTags.Appender<Block>> tag) {
        tag.apply(CABLE_FACADES_SUPPORTS).add(ModBlocks.FILTER_INLET_PIPE.get(), ModBlocks.FILTER_OUTLET_PIPE.get(), ModBlocks.FILTER_FRESH_PIPE.get(),
                ModBlocks.FILTER_WASTE_PIPE.get(), ModBlocks.FILTER_PIPE_BUNDLE.get());
    }
}
