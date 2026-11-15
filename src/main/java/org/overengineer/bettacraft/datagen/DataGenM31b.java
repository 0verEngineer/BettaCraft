package org.overengineer.bettacraft.datagen;

import java.util.function.Function;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.terrarium.TurtleEggsBlock;

/**
 * M31 part b data: the turtles. The eggs block (one to four half-buried eggs, larger than the block: an egg is about a
 * fifth of a grown musk turtle), the buckets, the terrarium box and its recipe, the spawn eggs, the food tag
 * {@code #bettacraft:turtle_greens} (dandelion; pink petals for clover; short grass and fern for the herbs; oxeye daisy),
 * the nest ground {@code #bettacraft:turtle_nest_ground} (sand and soil) and the tortoise biomes
 * {@code #bettacraft:tortoise_lands}. Textures: {@code tools/assetgen/terrarium.py} (eggs) and {@code buckets.py}.
 */
final class DataGenM31b {
    private DataGenM31b() {}

    /** Egg centres (x, z in pixels of the block) for one to four eggs; the long axis alternates. */
    private static final float[][][] EGGS = {
            {{8, 8}},
            {{3.5F, 8}, {12.5F, 9}},
            {{3, 4.5F}, {13, 5.5F}, {8, 13.5F}},
            {{3, 3.5F}, {13, 4}, {3.5F, 13}, {12.5F, 13.5F}},
    };

    static void blockStates(BlockStateProvider p) {
        BlockModelBuilder[] models = new BlockModelBuilder[4];
        for (int n = 1; n <= 4; n++) {
            BlockModelBuilder m = p.models().getBuilder("turtle_eggs_" + n).ao(false).texture("egg", p.modLoc("block/turtle_eggs"))
                    .texture("particle", p.modLoc("block/turtle_eggs"));
            float[][] at = EGGS[n - 1];
            for (int i = 0; i < at.length; i++) egg(m, at[i][0], at[i][1], i % 2 == 0);
            models[n - 1] = m;
        }
        p.getVariantBuilder(ModBlocks.TURTLE_EGGS.get()).forAllStates(s -> ConfiguredModel.builder().modelFile(models[s.getValue(TurtleEggsBlock.EGGS) - 1]).build());
    }

    /** One egg lying half in the ground: three boxes round it off (length 11, width 8, 5.5 above the ground). */
    private static void egg(BlockModelBuilder m, float cx, float cz, boolean alongZ) {
        float[][] boxes = {{8, 8, -1.5F, 4.0F}, {6, 11, -1.0F, 3.4F}, {5, 6.5F, -1.5F, 5.5F}};
        for (float[] b : boxes) {
            float w = alongZ ? b[0] : b[1], l = alongZ ? b[1] : b[0];
            m.element().from(cx - w / 2, b[2], cz - l / 2).to(cx + w / 2, b[3], cz + l / 2)
                    .allFaces((d, f) -> f.texture("#egg").uvs(2, 2, 2 + Math.min(12, w), 2 + Math.min(12, d.getAxis().isVertical() ? l : b[3] - b[2]))).end();
        }
    }

    static void itemModels(ItemModelProvider p) {
        for (String b : new String[]{"slider_turtle_bucket", "reeves_turtle_bucket", "hermann_tortoise_box", "terrarium_box"}) {
            p.withExistingParent(b, p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/" + b));
        }
        for (String e : new String[]{"slider_turtle_spawn_egg", "hermann_tortoise_spawn_egg", "reeves_turtle_spawn_egg"}) {
            p.withExistingParent(e, p.mcLoc("item/template_spawn_egg"));
        }
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.Criterion<?> hasTank) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TERRARIUM_BOX.get(), 2)
                .pattern("SHS").pattern("S S").pattern("SSS")
                .define('S', ItemTags.WOODEN_SLABS).define('H', Items.IRON_BARS)
                .unlockedBy("has_tank", hasTank).save(out, BettaCraft.id("terrarium_box"));
    }

    static void blockTags(Function<TagKey<Block>, IntrinsicHolderTagsProvider.IntrinsicTagAppender<Block>> tag) {
        tag.apply(ModTags.TURTLE_NEST_GROUND).addTag(BlockTags.SAND).addTag(BlockTags.DIRT).addTag(net.neoforged.neoforge.common.Tags.Blocks.SANDS)
                .add(Blocks.FARMLAND, Blocks.DIRT_PATH);
    }

    static void itemTags(Function<TagKey<Item>, IntrinsicHolderTagsProvider.IntrinsicTagAppender<Item>> tag) {
        tag.apply(ModTags.TURTLE_GREENS).add(Items.DANDELION, Items.PINK_PETALS, Items.SHORT_GRASS, Items.FERN, Items.OXEYE_DAISY);
    }
}
