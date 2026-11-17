package org.overengineer.bettacraft.datagen;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.overengineer.bettacraft.datagen.model.BlockModelBuilder;
import org.overengineer.bettacraft.datagen.model.BlockStateProvider;
import org.overengineer.bettacraft.datagen.model.ConfiguredModel;
import org.overengineer.bettacraft.datagen.model.ItemModelProvider;
import org.overengineer.bettacraft.datagen.model.ModelFile;
import org.overengineer.bettacraft.platform.Tags;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.fertilizer.FertilizerMakerBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * M37 data: the crank, the fertilizer maker (block states with the crank, models, recipe, loot, tags), the
 * intermediates kelp meal and nutrient salt, the liquid fertilizer bottle and the fertilizer canister of the dosing pump.
 */
final class DataGenM37 {
    private DataGenM37() {}

    static List<Block> blocks() {
        return List.of(ModBlocks.FERTILIZER_MAKER.get());
    }

    static void blockStates(BlockStateProvider p) {
        ModelFile[] crank = new ModelFile[4];
        for (int i = 0; i < 4; i++) crank[i] = maker(p, i);
        p.getVariantBuilder(ModBlocks.FERTILIZER_MAKER.get()).forAllStates(s -> ConfiguredModel.builder()
                .modelFile(crank[s.getValue(FertilizerMakerBlock.CRANK)])
                .rotationY(((int) s.getValue(FertilizerMakerBlock.FACING).toYRot() + 180) % 360).build());
    }

    /**
     * A steel case (2..14 x 0..10) with the gauge window in front, a glass mixing tank with a lid on top (the green
     * fertilizer inside) and the crank on the east side, handle in one of four positions.
     */
    private static ModelFile maker(BlockStateProvider p, int crank) {
        BlockModelBuilder m = p.models().getBuilder("fertilizer_maker_" + crank)
                .texture("front", p.modLoc("block/fertilizer_maker_front")).texture("side", p.modLoc("block/fertilizer_maker_side"))
                .texture("top", p.modLoc("block/fertilizer_maker_top")).texture("tank", p.modLoc("block/fertilizer_maker_tank"))
                .texture("lid", p.modLoc("block/fertilizer_maker_lid"))
                .texture("crank", p.modLoc("block/food_maker_crank")).texture("knob", p.modLoc("block/food_maker_knob"))
                .texture("particle", p.modLoc("block/fertilizer_maker_side"));
        m.element().from(2, 0, 2).to(14, 10, 14).allFaces((d, f) -> {
            f.texture(d == Direction.NORTH ? "#front" : d.getAxis().isVertical() ? "#top" : "#side");
            if (d == Direction.DOWN) f.cullface(d);
        }).end();
        // the mixing tank and its lid
        m.element().from(3.5F, 10, 3.5F).to(12.5F, 14, 12.5F).allFaces((d, f) -> f.texture(d.getAxis().isVertical() ? "#lid" : "#tank")).end();
        m.element().from(4.5F, 14, 4.5F).to(11.5F, 15, 11.5F).allFaces((d, f) -> f.texture("#lid")).end();
        // axle and crank (as on the food maker)
        m.element().from(14, 4, 7).to(15, 6, 9).allFaces((d, f) -> f.texture("#crank")).end();
        float[][] arm = {{5, 9.5F, 7.5F, 8.5F}, {4.5F, 5.5F, 3.5F, 8.5F}, {0.5F, 5, 7.5F, 8.5F}, {4.5F, 5.5F, 7.5F, 12.5F}};
        float[][] knob = {{8, 10, 7, 9}, {4, 6, 3, 5}, {0, 2, 7, 9}, {4, 6, 11, 13}};
        float[] a = arm[crank], k = knob[crank];
        m.element().from(15, a[0], a[2]).to(16, a[1], a[3]).allFaces((d, f) -> f.texture("#crank")).end();
        m.element().from(16, k[0], k[2]).to(17.5F, k[1], k[3]).allFaces((d, f) -> f.texture("#knob")).end();
        return m;
    }

    static void itemModels(ItemModelProvider p) {
        for (String s : List.of("crank", "kelp_meal", "nutrient_salt", "liquid_fertilizer", "fertilizer_canister")) {
            p.withExistingParent(s, p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/" + s));
        }
        p.withExistingParent("fertilizer_maker", p.modLoc("block/fertilizer_maker_0"));
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.Criterion<?> hasTank) {
        // the crank: an iron arm with a knob on a stick
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.TOOLS, ModItems.CRANK.get())
                .pattern("IN").pattern("S ")
                .define('I', Tags.Items.INGOTS_IRON).define('N', Tags.Items.NUGGETS_IRON).define('S', Tags.Items.RODS_WOODEN)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("crank")));
        // the intermediates: kelp meal (organic: dried kelp ground with bone meal) and nutrient salt (phosphate from bone meal,
        // potash from charcoal, iron)
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.KELP_MEAL.get(), 2)
                .requires(Items.DRIED_KELP).requires(Items.DRIED_KELP).requires(Items.BONE_MEAL)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("kelp_meal")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.NUTRIENT_SALT.get(), 2)
                .requires(Items.BONE_MEAL).requires(Items.CHARCOAL).requires(Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("nutrient_salt")));
        // the fertilizer maker: an iron case with a bucket on top, the cauldron as mixing tank and its crank
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.FERTILIZER_MAKER.get())
                .pattern("IBI").pattern("ICI").pattern("IKI")
                .define('I', Tags.Items.INGOTS_IRON).define('B', Items.BUCKET).define('C', Items.CAULDRON).define('K', ModItems.CRANK.get())
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("fertilizer_maker")));
        // the canister of the dosing pump: two full bottles (500 mL; the bottles come back)
        var full = DataComponentIngredient.of(false, DataComponents.DAMAGE, 0, ModItems.LIQUID_FERTILIZER.get());
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.FERTILIZER_CANISTER.get())
                .requires(ModItems.DOSING_CANISTER.get()).requires(full).requires(full)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("fertilizer_canister")));
    }

    static void blockTags(java.util.function.Function<TagKey<Block>, net.minecraft.data.tags.TagAppender<Block, Block>> tag) {
        tag.apply(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.FERTILIZER_MAKER.get());
    }
}
