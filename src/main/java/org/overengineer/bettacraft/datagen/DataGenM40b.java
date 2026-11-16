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
import net.neoforged.neoforge.common.Tags;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.plumbing.FilterPipeBundleBlock;
import org.overengineer.bettacraft.plumbing.PipeLine;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * M40 part b data: the fresh and waste water pipes (like the intake and return pipes, {@link DataGenM40}), the pipe bundle
 * (the steel manifold as its block state's model, and per line and side an arm and a T-piece collar that the bundle's model
 * adds, {@code client.model.FilterBundleModel}; the geometry is {@link FilterPipeBundleBlock#arm}), the fresh and waste
 * water ports of the filter, their item models, recipes and tags. The bundle has no loot table: it drops the pipes of its
 * lines itself.
 */
final class DataGenM40b {
    private DataGenM40b() {}

    /** Blocks that drop themselves. */
    static List<Block> blocks() {
        return List.of(ModBlocks.FILTER_FRESH_PORT.get(), ModBlocks.FILTER_WASTE_PORT.get());
    }

    /** The pipes with the loot of a filter pipe (pipe and T-piece). */
    static List<Block> pipes() {
        return List.of(ModBlocks.FILTER_FRESH_PIPE.get(), ModBlocks.FILTER_WASTE_PIPE.get());
    }

    static void blockStates(BlockStateProvider p) {
        DataGenM40.pipe(p, ModBlocks.FILTER_FRESH_PIPE.get(), "filter_fresh_pipe");
        DataGenM40.pipe(p, ModBlocks.FILTER_WASTE_PIPE.get(), "filter_waste_pipe");
        bundle(p);
        for (Block b : blocks()) {
            String n = b == ModBlocks.FILTER_FRESH_PORT.get() ? "filter_fresh_port" : "filter_waste_port";
            var top = p.modLoc("block/filter_chamber_top");
            ModelFile m = p.models().cube(n, top, top, p.modLoc("block/" + n), top, top, top).texture("particle", top);
            p.getVariantBuilder(b).forAllStatesExcept(s -> {
                Direction f = s.getValue(FilterPartBlock.FACING);
                int x = f == Direction.UP ? 270 : f == Direction.DOWN ? 90 : 0;
                int y = f.getAxis().isVertical() ? 0 : ((int) f.toYRot() + 180) % 360;
                return ConfiguredModel.builder().modelFile(m).rotationX(x).rotationY(y).build();
            }, FilterPartBlock.MEDIUM, FilterPartBlock.ACTIVE);
        }
    }

    private static void bundle(BlockStateProvider p) {
        var hubTex = p.modLoc("block/filter_pipe_bundle");
        float a = FilterPipeBundleBlock.HUB_MIN, b = FilterPipeBundleBlock.HUB_MAX;
        BlockModelBuilder hub = p.models().getBuilder("filter_pipe_bundle").texture("h", hubTex).texture("particle", hubTex).renderType("solid");
        hub.element().from(a, a, a).to(b, b, b).allFaces((d, f) -> f.texture("#h")).end();
        p.simpleBlock(ModBlocks.FILTER_PIPE_BUNDLE.get(), hub);
        for (PipeLine l : PipeLine.values()) {
            var tex = p.modLoc("block/" + l.blockId());
            for (Direction d : Direction.values()) {
                String name = "filter_pipe_bundle_" + l.getSerializedName() + "_" + d.getSerializedName();
                float[] arm = FilterPipeBundleBlock.arm(l, d, FilterPipeBundleBlock.ARM, 0);
                BlockModelBuilder m = p.models().getBuilder(name).texture("t", tex).texture("particle", tex).renderType("solid");
                m.element().from(arm[0], arm[1], arm[2]).to(arm[3], arm[4], arm[5]).allFaces((dd, f) -> f.texture("#t")).end();
                float[] c = FilterPipeBundleBlock.arm(l, d, FilterPipeBundleBlock.COLLAR, 1.5F);
                BlockModelBuilder t = p.models().getBuilder(name + "_tee").texture("c", p.modLoc("block/pipe_tee")).texture("particle", tex).renderType("solid");
                t.element().from(c[0], c[1], c[2]).to(c[3], c[4], c[5]).allFaces((dd, f) -> f.texture("#c")).end();
            }
        }
    }

    static void itemModels(ItemModelProvider p) {
        p.withExistingParent("filter_fresh_pipe", p.modLoc("block/filter_fresh_pipe_item"));
        p.withExistingParent("filter_waste_pipe", p.modLoc("block/filter_waste_pipe_item"));
        p.withExistingParent("filter_fresh_port", p.modLoc("block/filter_fresh_port"));
        p.withExistingParent("filter_waste_port", p.modLoc("block/filter_waste_port"));
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.Criterion<?> hasFilter) {
        Ingredient pipe = Ingredient.of(ModItems.GLASS_PIPE.get(), ModItems.STAINLESS_STEEL_PIPE.get());
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FILTER_FRESH_PIPE.get(), 8)
                .pattern("PPP").pattern("PDP").pattern("PPP").define('P', pipe).define('D', Tags.Items.DYES_LIME)
                .unlockedBy("has_filter", hasFilter).save(out, BettaCraft.id("filter_fresh_pipe"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FILTER_WASTE_PIPE.get(), 8)
                .pattern("PPP").pattern("PDP").pattern("PPP").define('P', pipe).define('D', Tags.Items.DYES_BROWN)
                .unlockedBy("has_filter", hasFilter).save(out, BettaCraft.id("filter_waste_pipe"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.FILTER_FRESH_PORT.get())
                .requires(ModItems.FILTER_INLET.get()).requires(Tags.Items.DYES_LIME)
                .unlockedBy("has_filter", hasFilter).save(out, BettaCraft.id("filter_fresh_port"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.FILTER_WASTE_PORT.get())
                .requires(ModItems.FILTER_OUTLET.get()).requires(Tags.Items.DYES_BROWN)
                .unlockedBy("has_filter", hasFilter).save(out, BettaCraft.id("filter_waste_port"));
    }

    static void blockTags(java.util.function.Function<TagKey<Block>, IntrinsicHolderTagsProvider.IntrinsicTagAppender<Block>> tag) {
        for (Block b : pipes()) tag.apply(BlockTags.MINEABLE_WITH_PICKAXE).add(b);
        for (Block b : blocks()) tag.apply(BlockTags.MINEABLE_WITH_PICKAXE).add(b);
        tag.apply(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.FILTER_PIPE_BUNDLE.get());
    }
}
