package org.overengineer.bettacraft.datagen;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.overengineer.bettacraft.datagen.model.BlockModelBuilder;
import org.overengineer.bettacraft.datagen.model.BlockStateProvider;
import org.overengineer.bettacraft.datagen.model.ConfiguredModel;
import org.overengineer.bettacraft.datagen.model.ItemModelProvider;
import org.overengineer.bettacraft.platform.Tags;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.reef.DosingPumpBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;

/** M25 part c data: the dosing pump (block state, model) and its canisters (item models, recipes). */
final class DataGenM25c {
    private DataGenM25c() {}

    static List<Block> blocks() {
        return List.of(ModBlocks.DOSING_PUMP.get());
    }

    static void blockStates(BlockStateProvider p) {
        // a low white case with the four pump heads on the front
        BlockModelBuilder m = p.models().getBuilder("dosing_pump").texture("front", p.modLoc("block/dosing_pump_front"))
                .texture("side", p.modLoc("block/dosing_pump_side")).texture("top", p.modLoc("block/dosing_pump_top"))
                .texture("particle", p.modLoc("block/dosing_pump_side"));
        m.element().from(1, 0, 3).to(15, 10, 13).allFaces((d, f) -> f.texture(d == Direction.NORTH ? "#front" : d.getAxis().isVertical() ? "#top" : "#side")).end();
        p.getVariantBuilder(ModBlocks.DOSING_PUMP.get()).forAllStates(s -> ConfiguredModel.builder().modelFile(m)
                .rotationY(((int) s.getValue(DosingPumpBlock.FACING).toYRot() + 180) % 360).build());
    }

    static void itemModels(ItemModelProvider p) {
        for (String s : List.of("dosing_canister", "calcium_canister", "alkalinity_canister", "magnesium_canister", "trace_canister")) {
            p.withExistingParent(s, p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/" + s));
        }
        p.withExistingParent("dosing_pump", p.modLoc("block/dosing_pump"));
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.Criterion<?> hasTank) {
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.DOSING_PUMP.get())
                .pattern("TTT").pattern("IRI").pattern("III")
                .define('T', Items.DRIED_KELP).define('R', Tags.Items.DUSTS_REDSTONE).define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("dosing_pump")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.DOSING_CANISTER.get(), 2)
                .pattern(" N ").pattern("G G").pattern("GGG")
                .define('N', Tags.Items.NUGGETS_IRON).define('G', Tags.Items.GLASS_PANES)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("dosing_canister")));
        // the solutions: calcium additive; calcite and soda from dried kelp; sea salt with prismarine; a glowing mix
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.CALCIUM_CANISTER.get())
                .requires(ModItems.DOSING_CANISTER.get()).requires(ModItems.CALCIUM_ADDITIVE.get()).requires(ModItems.CALCIUM_ADDITIVE.get())
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("calcium_canister")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.ALKALINITY_CANISTER.get())
                .requires(ModItems.DOSING_CANISTER.get()).requires(Items.CALCITE).requires(Items.DRIED_KELP).requires(Items.DRIED_KELP)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("alkalinity_canister")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.MAGNESIUM_CANISTER.get())
                .requires(ModItems.DOSING_CANISTER.get()).requires(ModItems.SEA_SALT.get()).requires(ModItems.SEA_SALT.get()).requires(Items.PRISMARINE_SHARD)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("magnesium_canister")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.TRACE_CANISTER.get())
                .requires(ModItems.DOSING_CANISTER.get()).requires(ModItems.SEA_SALT.get()).requires(Items.PRISMARINE_CRYSTALS).requires(Items.GLOW_INK_SAC)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("trace_canister")));
    }

    static void blockTags(java.util.function.Function<net.minecraft.tags.TagKey<Block>, net.minecraft.data.tags.TagAppender<Block, Block>> tag) {
        tag.apply(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.DOSING_PUMP.get());
    }
}
