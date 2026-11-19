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
import org.overengineer.bettacraft.datagen.model.BlockModelBuilder;
import org.overengineer.bettacraft.datagen.model.BlockStateProvider;
import org.overengineer.bettacraft.datagen.model.ConfiguredModel;
import org.overengineer.bettacraft.datagen.model.ItemModelProvider;
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

    /**
     * Egg centres (x, z in pixels of the block) for one to four eggs; the long axis alternates. An egg is 26 px long (1.6
     * interior blocks, about a seventh of a grown slider as in nature), so the clutch reaches beyond its block (models may
     * span -16..32).
     */
    private static final float[][][] EGGS = {
            {{8, 8}},
            {{-1, 8}, {17, 9}},
            {{-2, -1}, {18, 0}, {8, 18}},
            {{-3, -2}, {19, -1}, {-2, 18}, {19, 18}},
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

    /** One egg lying half in the ground: three boxes round it off (length 26, width 19, 13 above the ground). */
    private static void egg(BlockModelBuilder m, float cx, float cz, boolean alongZ) {
        float[][] boxes = {{19, 19, -3.5F, 9.5F}, {14, 26, -2.5F, 8.0F}, {12, 15, -3.5F, 13.0F}};
        for (float[] b : boxes) {
            float w = alongZ ? b[0] : b[1], l = alongZ ? b[1] : b[0];
            m.element().from(cx - w / 2, b[2], cz - l / 2).to(cx + w / 2, b[3], cz + l / 2)
                    .allFaces((d, f) -> f.texture("#egg").uvs(0, 0, 16, 16)).end();
        }
    }

    static void itemModels(ItemModelProvider p) {
        for (String b : new String[]{"slider_turtle_bucket", "reeves_turtle_bucket", "hermann_tortoise_box", "terrarium_box"}) {
            p.withExistingParent(b, p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/" + b));
        }
        for (String e : new String[]{"slider_turtle_spawn_egg", "hermann_tortoise_spawn_egg", "reeves_turtle_spawn_egg"}) {
            p.withExistingParent(e, p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/" + e));
        }
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.Criterion<?> hasTank) {
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.TERRARIUM_BOX.get(), 2)
                .pattern("SHS").pattern("S S").pattern("SSS")
                .define('S', ItemTags.WOODEN_SLABS).define('H', Items.IRON_BARS)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("terrarium_box")));
    }

    static void blockTags(Function<TagKey<Block>, net.minecraft.data.tags.TagAppender<Block, Block>> tag) {
        tag.apply(ModTags.TURTLE_NEST_GROUND).addTag(BlockTags.SAND).addTag(BlockTags.DIRT).addTag(org.overengineer.bettacraft.platform.Tags.Blocks.SANDS)
                .add(Blocks.FARMLAND, Blocks.DIRT_PATH);
    }

    static void itemTags(Function<TagKey<Item>, net.minecraft.data.tags.TagAppender<Item, Item>> tag) {
        tag.apply(ModTags.TURTLE_GREENS).add(Items.DANDELION, Items.PINK_PETALS, Items.SHORT_GRASS, Items.FERN, Items.OXEYE_DAISY);
    }
}
