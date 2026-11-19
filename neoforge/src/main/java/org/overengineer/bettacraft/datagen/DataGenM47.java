package org.overengineer.bettacraft.datagen;

import net.minecraft.core.Direction;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.overengineer.bettacraft.datagen.model.BlockModelBuilder;
import org.overengineer.bettacraft.datagen.model.BlockStateProvider;
import org.overengineer.bettacraft.datagen.model.ConfiguredModel;
import org.overengineer.bettacraft.datagen.model.ItemModelProvider;
import org.overengineer.bettacraft.platform.Tags;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M47 part a data: the breeding basin (the workplace of the aquarist villager): a glass basin with gravel, water and plants
 * on a dark wooden stand, its recipe (glass panes, a water plant, wooden slabs) and its mining tag. Textures:
 * {@code tools/assetgen/aquarist.py} (the glass is vanilla's).
 */
final class DataGenM47 {
    private DataGenM47() {}

    static void blockStates(BlockStateProvider p) {
        BlockModelBuilder m = p.models().getBuilder("breeding_basin").renderType("translucent")
                .texture("wood", p.modLoc("block/breeding_basin_wood")).texture("glass", p.mcLoc("block/glass"))
                .texture("water", p.modLoc("block/breeding_basin_water")).texture("gravel", p.modLoc("block/breeding_basin_gravel"))
                .texture("plant", p.modLoc("block/breeding_basin_plant")).texture("particle", p.modLoc("block/breeding_basin_wood"));
        // the stand: a top board on four short legs
        m.element().from(1, 2, 1).to(15, 4, 15).allFaces((d, f) -> f.texture("#wood").uvs(1, d.getAxis().isVertical() ? 1 : 12, 15,
                d.getAxis().isVertical() ? 15 : 14)).end();
        for (int[] l : new int[][]{{1, 1}, {13, 1}, {1, 13}, {13, 13}}) {
            m.element().from(l[0], 0, l[1]).to(l[0] + 2, 2, l[1] + 2).allFaces((d, f) -> f.texture("#wood").uvs(0, 0, 2, 2)).end();
        }
        // the gravel bed, the water, the plants
        m.element().from(2.5F, 4, 2.5F).to(13.5F, 5.5F, 13.5F).allFaces((d, f) -> f.texture("#gravel").uvs(2, 2, 13, d.getAxis().isVertical() ? 13 : 3.5F)).end();
        for (int i = 0; i < 2; i++) {
            float a = i == 0 ? 45 : -45;
            m.element().from(4, 5.5F, 8).to(12, 13, 8).rotation().angle(a).axis(Direction.Axis.Y).origin(8, 8, 8).rescale(true).end()
                    .face(Direction.NORTH).texture("#plant").uvs(0, 4, 16, 16).end()
                    .face(Direction.SOUTH).texture("#plant").uvs(0, 4, 16, 16).end().end();
        }
        var water = m.element().from(2.5F, 5.5F, 2.5F).to(13.5F, 12.5F, 13.5F);
        water.face(Direction.UP).texture("#water").uvs(2, 2, 13, 13).end();
        for (Direction d : Direction.Plane.HORIZONTAL) water.face(d).texture("#water").uvs(2, 3.5F, 13, 10.5F).end();
        water.end();
        // the glass walls (the panes of the basin, no lid) and the dark rim on top
        var glass = m.element().from(2, 4, 2).to(14, 14, 14);
        for (Direction d : Direction.Plane.HORIZONTAL) glass.face(d).texture("#glass").uvs(2, 2, 14, 12).end();
        glass.end();
        m.element().from(2, 14, 2).to(14, 14.5F, 3).allFaces((d, f) -> f.texture("#wood").uvs(0, 0, 12, 1)).end();
        m.element().from(2, 14, 13).to(14, 14.5F, 14).allFaces((d, f) -> f.texture("#wood").uvs(0, 0, 12, 1)).end();
        m.element().from(2, 14, 3).to(3, 14.5F, 13).allFaces((d, f) -> f.texture("#wood").uvs(0, 0, 1, 10)).end();
        m.element().from(13, 14, 3).to(14, 14.5F, 13).allFaces((d, f) -> f.texture("#wood").uvs(0, 0, 1, 10)).end();
        p.getVariantBuilder(ModBlocks.BREEDING_BASIN.get()).forAllStates(s -> ConfiguredModel.builder().modelFile(m)
                .rotationY(((int) s.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 180) % 360).build());
    }

    static void itemModels(ItemModelProvider p) {
        p.withExistingParent("breeding_basin", p.modLoc("block/breeding_basin"));
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.triggers.Criterion<?> has) {
        Ingredient plant = CompoundIngredient.of(RecipeCompat.tag(ModTags.AQUATIC_PLANT_ITEMS), Ingredient.of(Items.SEAGRASS, Items.KELP, Items.LILY_PAD));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.DECORATIONS, ModItems.BREEDING_BASIN.get())
                .pattern("G G").pattern("GPG").pattern("SSS")
                .define('G', Tags.Items.GLASS_PANES).define('P', plant).define('S', ItemTags.WOODEN_SLABS)
                .unlockedBy("has_pane", has).save(out, RecipeCompat.key(BettaCraft.id("breeding_basin")));
    }

    static void blockTags(java.util.function.Function<net.minecraft.tags.TagKey<Block>, ObjTags.Appender<Block>> tag) {
        tag.apply(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.BREEDING_BASIN.get());
    }
}
