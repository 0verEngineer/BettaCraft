package org.overengineer.bettacraft.datagen;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import org.overengineer.bettacraft.platform.Tags;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.plumbing.PipeBundleBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * M40 part b data: the fresh and waste water pipes (like the intake and return pipes, {@link DataGenM40}), the pipe bundle
 * (the steel manifold as its block state's model, and per line and side an arm and a T-piece collar that the bundle's model
 * adds, M68: made in code by {@code client.model.PipeQuads}; the geometry is {@link PipeBundleBlock#arm}), the fresh and waste
 * water ports of the filter, their item models, recipes and tags. The bundle has no loot table: it drops the pipes of its
 * lines itself.
 */
final class DataGenM40b {
    private DataGenM40b() {}

    static void blockStates(BlockStateProvider p) {
        bundle(p);
    }

    private static void bundle(BlockStateProvider p) {
        var hubTex = p.modLoc("block/filter_pipe_bundle");
        float a = PipeBundleBlock.HUB_MIN, b = PipeBundleBlock.HUB_MAX;
        BlockModelBuilder hub = p.models().getBuilder("filter_pipe_bundle").texture("h", hubTex).texture("particle", hubTex).renderType("solid");
        hub.element().from(a, a, a).to(b, b, b).allFaces((d, f) -> f.texture("#h")).end();
        p.simpleBlock(ModBlocks.FILTER_PIPE_BUNDLE.get(), hub);
        // M68: the arms of the bundle's pipes are made in code (client.model.PipeQuads: glass or steel, with the ring of their role)
    }

    static void blockTags(java.util.function.Function<TagKey<Block>, IntrinsicHolderTagsProvider.IntrinsicTagAppender<Block>> tag) {
        tag.apply(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.FILTER_PIPE_BUNDLE.get());
    }
}
