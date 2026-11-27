package org.overengineer.bettacraft.datagen;

import net.minecraft.core.Direction;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import org.overengineer.bettacraft.datagen.model.BlockModelBuilder;
import org.overengineer.bettacraft.datagen.model.BlockStateProvider;
import org.overengineer.bettacraft.datagen.model.ItemModelProvider;
import org.overengineer.bettacraft.datagen.model.MultiPartBlockStateBuilder;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.plumbing.PipeRing;
import org.overengineer.bettacraft.plumbing.interior.InteriorBundleBlock;
import org.overengineer.bettacraft.plumbing.interior.PipeAnchorBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M68 data: the pipe wrench (item model, recipe). The pipes' models with the rings of their roles are in {@link PipeModels}
 * (called by {@link DataGenM24}); the filter pipes, their T-piece, the interior pipe item and the filter's water ports are gone.
 */
final class DataGenM68 {
    private DataGenM68() {}

    // ------------------------------------------------------------------ part C: the interior bundles, the ring of a pipe dock

    static void blockStates(BlockStateProvider p) {
        interiorBundle(p, ModBlocks.INTERIOR_PIPE_BUNDLE.get(), "interior_pipe_bundle", "block/plumbing_glass", "translucent", true);
        interiorBundle(p, ModBlocks.INTERIOR_STEEL_PIPE_BUNDLE.get(), "interior_steel_pipe_bundle", "block/plumbing_steel", "solid", false);
    }

    /**
     * M68 part C: an interior bundle: per axis and lane its rod, the rod's ends where the run goes on and the arm to its side
     * ({@link InteriorBundleBlock#box}, the same boxes as its shape). Glass leaves out the faces where pieces meet (they would show
     * through it, like the glass pipe's ends).
     */
    private static void interiorBundle(BlockStateProvider p, InteriorBundleBlock block, String name, String tex, String render, boolean glass) {
        MultiPartBlockStateBuilder mp = p.getMultipartBuilder(block);
        String[] pieces = {"rod", "end_neg", "end_pos", "arm"};
        for (Direction.Axis axis : Direction.Axis.values()) {
            Direction neg = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE), pos = neg.getOpposite();
            for (int lane = 0; lane < 4; lane++) {
                Integer[] lanes = lane < 2 ? new Integer[]{2, 3, 4} : lane == 2 ? new Integer[]{3, 4} : new Integer[]{4};
                for (int piece = 0; piece < 4; piece++) {
                    if (piece == InteriorBundleBlock.ARM && lane == 0) continue; // the middle lane has no side
                    float[] b = InteriorBundleBlock.box(axis, lane, piece);
                    Direction side = lane == 0 ? null : InteriorBundleBlock.laneSide(axis, lane);
                    BlockModelBuilder m = p.models().getBuilder(name + "_" + axis.getSerializedName() + lane + "_" + pieces[piece])
                            .texture("t", p.modLoc(tex)).texture("particle", p.modLoc(tex)).renderType(render);
                    var el = m.element().from(b[0], b[1], b[2]).to(b[3], b[4], b[5]);
                    for (Direction d : Direction.values()) {
                        // glass: no faces where it meets the next piece (along the run, or the arm's two ends)
                        boolean inner = piece == InteriorBundleBlock.ARM ? d.getAxis() == side.getAxis() : d.getAxis() == axis;
                        if (!glass || !inner) el.face(d).texture("#t").end();
                    }
                    el.end();
                    var part = mp.part().modelFile(m).addModel().condition(InteriorBundleBlock.AXIS, axis).condition(InteriorBundleBlock.LANES, lanes);
                    if (piece == InteriorBundleBlock.END_NEG) part.condition(PipeBlock.SIDES.get(neg), true);
                    else if (piece == InteriorBundleBlock.END_POS) part.condition(PipeBlock.SIDES.get(pos), true);
                    else if (piece == InteriorBundleBlock.ARM) part.condition(PipeBlock.SIDES.get(side), true);
                    part.end();
                }
            }
        }
    }

    /**
     * M68 part C: the ring of a pipe dock in its line's colour: a band around its stub (the colour rows of the ring pipe textures),
     * one part per mount and ring, turned like the dock.
     */
    static void anchorRings(BlockStateProvider p, MultiPartBlockStateBuilder mp) {
        for (PipeRing ring : PipeRing.values()) {
            if (ring == PipeRing.NONE) continue;
            var t = p.modLoc("block/stainless_steel_pipe_" + ring.getSerializedName());
            BlockModelBuilder m = p.models().getBuilder("pipe_anchor_ring_" + ring.getSerializedName()).texture("r", t).texture("particle", t).renderType("solid");
            m.element().from(4.5F, 4F, 4.5F).to(11.5F, 6F, 11.5F).allFaces((d, f) -> f.texture("#r").uvs(0, 0, 16, 1)).end();
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
                mp.part().modelFile(m).rotationX(x).rotationY(y).addModel().condition(PipeAnchorBlock.MOUNT, f).condition(PipeAnchorBlock.RING, ring).end();
            }
        }
    }

    static void itemModels(ItemModelProvider p) {
        p.withExistingParent("pipe_wrench", p.mcLoc("item/handheld")).texture("layer0", p.modLoc("item/pipe_wrench"));
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.Criterion<?> hasPipe) {
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.TOOLS, ModItems.PIPE_WRENCH.get())
                .pattern(" S ").pattern(" IS").pattern("I  ").define('S', ModTags.STAINLESS_STEEL_INGOTS).define('I', Items.IRON_INGOT)
                .unlockedBy("has_pipe", hasPipe).save(out, RecipeCompat.key(BettaCraft.id("pipe_wrench")));
    }
}
