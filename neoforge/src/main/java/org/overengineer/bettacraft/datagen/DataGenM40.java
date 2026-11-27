package org.overengineer.bettacraft.datagen;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import org.overengineer.bettacraft.datagen.model.BlockModelBuilder;
import org.overengineer.bettacraft.datagen.model.BlockStateProvider;
import org.overengineer.bettacraft.datagen.model.ConfiguredModel;
import org.overengineer.bettacraft.datagen.model.ItemModelProvider;
import org.overengineer.bettacraft.datagen.model.MultiPartBlockStateBuilder;
import org.overengineer.bettacraft.platform.Tags;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.plumbing.interior.PipeAnchorBlock;
import org.overengineer.bettacraft.plumbing.interior.PlumbingBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M40 part a data: the filter pipes (multipart: core or T-piece collar and one arm per joined side), the pipe dock of the
 * interior (one model turned to its side), the intake and outlet guards, their item models, the recipes and the tags.
 * The loot of the pipes (a T-piece gives its collar back) is in {@code ModDataGen}.
 */
final class DataGenM40 {
    private DataGenM40() {}

    /** Blocks that drop themselves (the pipes have their own loot: pipe and T-piece). */
    static List<Block> blocks() {
        return List.of(ModBlocks.INTAKE_GUARD.get(), ModBlocks.OUTLET_GUARD.get());
    }

    // ------------------------------------------------------------------ block states

    static void blockStates(BlockStateProvider p) {
        anchor(p);
        guards(p);
    }

    /** The dock: a flange on the floor with a stub up, turned to the side of the tank it sits on. */
    private static void anchor(BlockStateProvider p) {
        var s = p.modLoc("block/plumbing_steel");
        BlockModelBuilder m = p.models().getBuilder("pipe_anchor").texture("f", p.modLoc("block/pipe_anchor")).texture("s", s)
                .texture("particle", s);
        m.element().from(2, 0, 2).to(14, 2, 14).allFaces((d, f) -> f.texture(d.getAxis() == Direction.Axis.Y ? "#f" : "#s")).end();
        m.element().from(5, 2, 5).to(11, 8, 11).allFaces((d, f) -> f.texture("#s")).end();
        // M65: multipart, with the short arms of DataGenM24#interiorArms towards the pieces it joins
        MultiPartBlockStateBuilder mp = p.getMultipartBuilder(ModBlocks.PIPE_ANCHOR.get());
        for (Direction f : Direction.values()) {
            int x = switch (f) {
                case DOWN -> 0;
                case UP -> 180;
                default -> 270;
            };
            int y = switch (f) {
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0;
            };
            mp.part().modelFile(m).rotationX(x).rotationY(y).addModel().condition(PipeAnchorBlock.MOUNT, f).end();
        }
        DataGenM24.interiorArms(p, mp);
        DataGenM68.anchorRings(p, mp); // M68 part C: the ring colour of its line
    }

    private static void guards(BlockStateProvider p) {
        var s = p.modLoc("block/plumbing_steel");
        BlockModelBuilder in = p.models().getBuilder("intake_guard").texture("m", p.modLoc("block/intake_guard")).texture("s", s)
                .texture("particle", p.modLoc("block/intake_guard"));
        in.element().from(3, 3, 3).to(13, 13, 13).allFaces((d, f) -> f.texture("#m")).end();
        BlockModelBuilder out = p.models().getBuilder("outlet_guard").texture("m", p.modLoc("block/outlet_guard")).texture("s", s)
                .texture("particle", p.modLoc("block/outlet_guard"));
        out.element().from(4, 4, 4).to(12, 12, 12).allFaces((d, f) -> f.texture(d == Direction.NORTH ? "#m" : "#s")).end();
        out.element().from(5, 5, 2).to(11, 11, 4).allFaces((d, f) -> f.texture("#m")).end();
        for (var e : List.of(java.util.Map.entry(ModBlocks.INTAKE_GUARD.get(), in), java.util.Map.entry(ModBlocks.OUTLET_GUARD.get(), out))) {
            // M65: multipart, with short arms towards the joined pieces (the guard beside a dock is visibly connected to it)
            MultiPartBlockStateBuilder mp = p.getMultipartBuilder(e.getKey());
            for (Direction f : Direction.Plane.HORIZONTAL)
                mp.part().modelFile(e.getValue()).rotationY(((int) f.toYRot() + 180) % 360).addModel().condition(PlumbingBlock.FACING, f).end();
            DataGenM24.interiorArms(p, mp);
        }
    }

    private static net.minecraft.world.level.block.state.properties.Property<?>[] sidesAndActive() {
        List<net.minecraft.world.level.block.state.properties.Property<?>> l = new java.util.ArrayList<>(PipeBlock.SIDES.values());
        l.add(PlumbingBlock.ACTIVE);
        return l.toArray(new net.minecraft.world.level.block.state.properties.Property<?>[0]);
    }

    private static net.minecraft.world.level.block.state.properties.Property<?>[] sidesActiveFacing() {
        List<net.minecraft.world.level.block.state.properties.Property<?>> l = new java.util.ArrayList<>(PipeBlock.SIDES.values());
        l.add(PlumbingBlock.ACTIVE);
        l.add(PlumbingBlock.FACING);
        return l.toArray(new net.minecraft.world.level.block.state.properties.Property<?>[0]);
    }

    // ------------------------------------------------------------------ item models

    static void itemModels(ItemModelProvider p) {
        DataGenM291.block(p, "intake_guard", "intake_guard");
        DataGenM291.block(p, "outlet_guard", "outlet_guard");
    }

    // ------------------------------------------------------------------ recipes

    static void recipes(RecipeOutput out, net.minecraft.advancements.Criterion<?> hasFilter) {
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.INTAKE_GUARD.get(), 2)
                .requires(Items.IRON_BARS).requires(Tags.Items.NUGGETS_IRON).requires(Tags.Items.STRINGS)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("intake_guard")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.OUTLET_GUARD.get(), 2)
                .requires(Items.IRON_BARS).requires(Tags.Items.NUGGETS_IRON).requires(ModItems.GLASS_PIPE.get())
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("outlet_guard")));
    }

    static void blockTags(java.util.function.Function<TagKey<Block>, net.minecraft.data.tags.TagAppender<Block, Block>> tag) {
        for (Block b : blocks()) tag.apply(BlockTags.MINEABLE_WITH_PICKAXE).add(b);
    }
}
