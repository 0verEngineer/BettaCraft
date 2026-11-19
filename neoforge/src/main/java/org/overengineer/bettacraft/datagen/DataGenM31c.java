package org.overengineer.bettacraft.datagen;

import java.util.function.Function;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
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
 * M31 part c data: the amphibians and reptiles. The dart frog clutch block (one to four blobs of jelly with dark embryos),
 * the terrarium boxes and spawn eggs, the feeder insects (crickets from a cricket culture: wheat, short grass and paper;
 * fruit flies from a fruit fly culture: a glass bottle with an apple, sweet berries or a melon slice), the food tags
 * ({@code #bettacraft:feeder_insects}, {@code small_feeder_insects}, {@code gecko_fruit}, {@code snake_prey}), the nest
 * ground of the frogs ({@code #bettacraft:frog_nest_ground}: moss, leaves, soil) and the biome tags
 * {@code #bettacraft:arid_lands} (deserts, badlands) and {@code #bettacraft:snake_lands} (temperate forests and plains).
 * Textures: {@code tools/assetgen/terrarium.py} (frog eggs), {@code buckets.py} (boxes, insects, cultures).
 */
final class DataGenM31c {
    private DataGenM31c() {}

    /** Blob centres (x, z in pixels) for one to four egg blobs of a clutch. */
    private static final float[][][] BLOBS = {
            {{8, 8}},
            {{5, 7}, {11, 9}},
            {{5, 6}, {11, 7}, {8, 11}},
            {{5, 5}, {11, 6}, {5, 11}, {11, 11}},
    };

    static void blockStates(BlockStateProvider p) {
        BlockModelBuilder[] models = new BlockModelBuilder[4];
        for (int n = 1; n <= 4; n++) {
            BlockModelBuilder m = p.models().getBuilder("frog_eggs_" + n).ao(false).texture("egg", p.modLoc("block/frog_eggs"))
                    .texture("particle", p.modLoc("block/frog_eggs")).renderType("cutout");
            for (float[] b : BLOBS[n - 1]) {
                // a blob of jelly: a flat round clump on the leaf or the moss
                m.element().from(b[0] - 3, 0, b[1] - 2).to(b[0] + 3, 2.5F, b[1] + 2).allFaces((d, f) -> f.texture("#egg").uvs(0, 0, 6, 4)).end();
                m.element().from(b[0] - 2, 0, b[1] - 3).to(b[0] + 2, 3, b[1] + 3).allFaces((d, f) -> f.texture("#egg").uvs(6, 0, 10, 6)).end();
            }
            models[n - 1] = m;
        }
        p.getVariantBuilder(ModBlocks.FROG_EGGS.get()).forAllStates(s -> ConfiguredModel.builder().modelFile(models[s.getValue(TurtleEggsBlock.EGGS) - 1]).build());
    }

    static void itemModels(ItemModelProvider p) {
        for (var s : ModItems.HERP_BOXES.keySet()) p.withExistingParent(s.containerId(), p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/" + s.containerId()));
        for (var s : ModItems.HERP_SPAWN_EGGS.keySet()) p.withExistingParent(s.spawnEggId(), p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/" + s.spawnEggId()));
        for (String i : new String[]{"crickets", "fruit_flies", "cricket_culture", "fruit_fly_culture"}) {
            p.withExistingParent(i, p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/" + i));
        }
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.Criterion<?> hasTank) {
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.FOOD, ModItems.CRICKET_CULTURE.get(), 3)
                .requires(Items.WHEAT).requires(Items.SHORT_GRASS).requires(Items.PAPER)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("cricket_culture")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.FOOD, ModItems.FRUIT_FLY_CULTURE.get(), 2)
                .requires(Items.GLASS_BOTTLE).requires(Ingredient.of(Items.APPLE, Items.SWEET_BERRIES, Items.MELON_SLICE))
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("fruit_fly_culture")));
    }

    static void blockTags(Function<TagKey<Block>, net.minecraft.data.tags.TagAppender<Block, Block>> tag) {
        tag.apply(ModTags.FROG_NEST_GROUND).addTag(BlockTags.LEAVES).addTag(BlockTags.DIRT)
                .add(Blocks.MOSS_BLOCK, Blocks.MOSS_CARPET, Blocks.MUD, Blocks.MUDDY_MANGROVE_ROOTS);
    }

    static void itemTags(Function<TagKey<Item>, net.minecraft.data.tags.TagAppender<Item, Item>> tag) {
        tag.apply(ModTags.FEEDER_INSECTS).add(ModItems.CRICKETS.get());
        tag.apply(ModTags.SMALL_FEEDER_INSECTS).add(ModItems.FRUIT_FLIES.get());
        tag.apply(ModTags.GECKO_FRUIT).add(Items.SWEET_BERRIES, Items.GLOW_BERRIES, Items.MELON_SLICE);
        tag.apply(ModTags.SNAKE_PREY).add(Items.RABBIT, Items.CHICKEN);
    }
}
