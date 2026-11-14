package org.overengineer.bettacraft.datagen;

import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.Tags;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.monitor.MonitorBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * M27 data: the monitor block. A multipart block state: the case with the glass one pixel behind the front, and a frame
 * piece on each side where the screen does not go on (the flags of {@link MonitorBlock}), so a screen of many blocks has
 * one frame around it. Models are made for a screen facing north and turned.
 */
final class DataGenM27 {
    private DataGenM27() {}

    static List<Block> blocks() {
        return List.of(ModBlocks.MONITOR.get());
    }

    private static BlockModelBuilder textured(BlockStateProvider p, String id) {
        return p.models().getBuilder(id).texture("screen", p.modLoc("block/monitor_screen")).texture("side", p.modLoc("block/monitor_side"))
                .texture("back", p.modLoc("block/monitor_back")).texture("bezel", p.modLoc("block/monitor_bezel"))
                .texture("particle", p.modLoc("block/monitor_side"));
    }

    private static void body(BlockModelBuilder m) {
        m.element().from(0, 0, 1).to(16, 16, 16).allFaces((d, f) -> {
            f.texture(d == Direction.NORTH ? "#screen" : d == Direction.SOUTH ? "#back" : "#side");
            if (d != Direction.NORTH) f.cullface(d);
        }).end();
    }

    /** A frame piece along one edge of the front (the viewer looks south at a north-facing screen: his left is east). */
    private static void bezel(BlockModelBuilder m, String edge) {
        float[] b = switch (edge) {
            case "up" -> new float[]{0, 15, 16, 16};
            case "down" -> new float[]{0, 0, 16, 1};
            case "left" -> new float[]{15, 0, 16, 16};
            default -> new float[]{0, 0, 1, 16};
        };
        m.element().from(b[0], b[1], 0).to(b[2], b[3], 1).allFaces((d, f) -> {
            f.texture("#bezel");
            if (d == Direction.UP && "up".equals(edge) || d == Direction.DOWN && "down".equals(edge)
                    || d == Direction.EAST && "left".equals(edge) || d == Direction.WEST && "right".equals(edge)) f.cullface(d);
        }).end();
    }

    static void blockStates(BlockStateProvider p) {
        BlockModelBuilder body = textured(p, "monitor");
        body(body);
        String[] edges = {"up", "down", "left", "right"};
        ModelFile[] frame = new ModelFile[4];
        for (int i = 0; i < 4; i++) {
            BlockModelBuilder m = textured(p, "monitor_frame_" + edges[i]);
            bezel(m, edges[i]);
            frame[i] = m;
        }
        BlockModelBuilder single = textured(p, "monitor_single");
        body(single);
        for (String e : edges) bezel(single, e);
        MultiPartBlockStateBuilder mp = p.getMultipartBuilder(ModBlocks.MONITOR.get());
        for (Direction f : Direction.Plane.HORIZONTAL) {
            int rot = ((int) f.toYRot() + 180) % 360;
            mp.part().modelFile(body).rotationY(rot).addModel().condition(MonitorBlock.FACING, f).end();
            mp.part().modelFile(frame[0]).rotationY(rot).addModel().condition(MonitorBlock.FACING, f).condition(MonitorBlock.UP, false).end();
            mp.part().modelFile(frame[1]).rotationY(rot).addModel().condition(MonitorBlock.FACING, f).condition(MonitorBlock.DOWN, false).end();
            mp.part().modelFile(frame[2]).rotationY(rot).addModel().condition(MonitorBlock.FACING, f).condition(MonitorBlock.LEFT, false).end();
            mp.part().modelFile(frame[3]).rotationY(rot).addModel().condition(MonitorBlock.FACING, f).condition(MonitorBlock.RIGHT, false).end();
        }
    }

    static void itemModels(ItemModelProvider p) {
        p.withExistingParent("monitor", p.modLoc("block/monitor_single"));
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.Criterion<?> hasTank) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MONITOR.get(), 4)
                .pattern("IGI").pattern("GRG").pattern("IGI")
                .define('I', Tags.Items.INGOTS_IRON).define('G', Tags.Items.GLASS_PANES).define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_tank", hasTank).save(out, BettaCraft.id("monitor"));
    }

    static void blockTags(Function<TagKey<Block>, IntrinsicHolderTagsProvider.IntrinsicTagAppender<Block>> tag) {
        tag.apply(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.MONITOR.get());
    }
}
