package org.overengineer.bettacraft.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.overengineer.bettacraft.datagen.model.BlockModelBuilder;
import org.overengineer.bettacraft.datagen.model.BlockStateProvider;
import org.overengineer.bettacraft.datagen.model.ConfiguredModel;
import org.overengineer.bettacraft.datagen.model.ItemModelProvider;
import org.overengineer.bettacraft.platform.Tags;
import org.overengineer.bettacraft.datagen.model.ExistingFileHelper;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.co2.BioCo2Block;
import org.overengineer.bettacraft.co2.Co2CylinderBlock;
import org.overengineer.bettacraft.co2.Co2StationBlock;
import org.overengineer.bettacraft.osmosis.RoUnitBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M25 part b data: block states and models (bio CO2 bottle, CO2 cylinder, filling station, reverse osmosis unit), item
 * models, recipes and the fluid tag of other mods' CO2.
 */
final class DataGenM25b {
    private DataGenM25b() {}

    static List<Block> blocks() {
        return List.of(ModBlocks.BIO_CO2.get(), ModBlocks.CO2_CYLINDER.get(), ModBlocks.CO2_STATION.get(), ModBlocks.RO_UNIT.get());
    }

    private static int yRot(Direction f) {
        return ((int) f.toYRot() + 180) % 360;
    }

    static void blockStates(BlockStateProvider p) {
        // bio CO2: a glass bottle with the brew (its colour per stage) and a cap with the hose
        String[] stage = {"empty", "mixed", "fermenting", "spent"};
        BlockModelBuilder[] bio = new BlockModelBuilder[4];
        for (int i = 0; i < 4; i++) {
            BlockModelBuilder m = bio[i] = p.models().getBuilder("bio_co2_" + stage[i]).texture("side", p.modLoc("block/bio_co2_" + stage[i]))
                    .texture("cap", p.modLoc("block/bio_co2_cap")).texture("particle", p.modLoc("block/bio_co2_" + stage[i])).renderType("cutout");
            m.element().from(5, 0, 5).to(11, 10, 11).allFaces((d, f) -> f.texture(d.getAxis().isVertical() ? "#cap" : "#side")).end();
            m.element().from(6.5F, 10, 6.5F).to(9.5F, 12, 9.5F).allFaces((d, f) -> f.texture("#cap")).end();
            m.element().from(7.5F, 12, 7.5F).to(8.5F, 13, 11).allFaces((d, f) -> f.texture("#cap")).end();
        }
        p.getVariantBuilder(ModBlocks.BIO_CO2.get()).forAllStates(s -> ConfiguredModel.builder().modelFile(bio[s.getValue(BioCo2Block.STAGE)])
                .rotationY(yRot(s.getValue(BioCo2Block.FACING))).build());
        // the cylinder: body, regulator with gauge, bubble counter; the solenoid valve on top of the regulator
        BlockModelBuilder[] cyl = new BlockModelBuilder[2];
        for (int i = 0; i < 2; i++) {
            BlockModelBuilder m = cyl[i] = p.models().getBuilder(i == 0 ? "co2_cylinder" : "co2_cylinder_solenoid")
                    .texture("side", p.modLoc("block/co2_cylinder_side")).texture("top", p.modLoc("block/co2_cylinder_top"))
                    .texture("reg", p.modLoc("block/co2_regulator")).texture("counter", p.modLoc("block/co2_bubble_counter"))
                    .texture("valve", p.modLoc("block/co2_solenoid_block")).texture("particle", p.modLoc("block/co2_cylinder_side")).renderType("cutout");
            m.element().from(5, 0, 5).to(11, 11, 11).allFaces((d, f) -> f.texture(d.getAxis().isVertical() ? "#top" : "#side")).end();
            m.element().from(6.5F, 11, 6.5F).to(9.5F, 12.5F, 9.5F).allFaces((d, f) -> f.texture("#top")).end();
            m.element().from(5.5F, 12.5F, 6).to(10.5F, 15, 10).allFaces((d, f) -> f.texture("#reg")).end();
            m.element().from(10.5F, 11.5F, 6.5F).to(12.5F, 15.5F, 8.5F).allFaces((d, f) -> f.texture("#counter")).end();
            if (i == 1) m.element().from(6.5F, 15, 6.5F).to(9.5F, 16, 9.5F).allFaces((d, f) -> f.texture("#valve")).end();
        }
        p.getVariantBuilder(ModBlocks.CO2_CYLINDER.get()).forAllStatesExcept(s -> ConfiguredModel.builder()
                .modelFile(cyl[s.getValue(Co2CylinderBlock.SOLENOID) ? 1 : 0]).rotationY(yRot(s.getValue(Co2CylinderBlock.FACING))).build(), Co2CylinderBlock.ACTIVE);
        // the filling station: a steel box with a flue hood; its front shows an empty bay, a cylinder or a filling cylinder
        String[] front = {"empty", "loaded", "active"};
        BlockModelBuilder[] st = new BlockModelBuilder[3];
        for (int i = 0; i < 3; i++) {
            st[i] = p.models().orientable("co2_station_" + front[i], p.modLoc("block/co2_station_side"), p.modLoc("block/co2_station_front_" + front[i]),
                    p.modLoc("block/co2_station_top"));
        }
        p.getVariantBuilder(ModBlocks.CO2_STATION.get()).forAllStates(s -> ConfiguredModel.builder()
                .modelFile(st[s.getValue(Co2StationBlock.ACTIVE) ? 2 : s.getValue(Co2StationBlock.LOADED) ? 1 : 0])
                .rotationY(((int) s.getValue(Co2StationBlock.FACING).toYRot() + 180) % 360).build());
        // the reverse osmosis unit: a wall board with three cartridges and the membrane housing
        BlockModelBuilder[] ro = new BlockModelBuilder[2];
        for (int i = 0; i < 2; i++) {
            BlockModelBuilder m = ro[i] = p.models().getBuilder(i == 0 ? "ro_unit" : "ro_unit_active")
                    .texture("front", p.modLoc(i == 0 ? "block/ro_unit_front" : "block/ro_unit_front_active")).texture("side", p.modLoc("block/ro_unit_side"))
                    .texture("top", p.modLoc("block/ro_unit_top")).texture("particle", p.modLoc("block/ro_unit_side"));
            m.element().from(1, 0, 3).to(15, 14, 13).allFaces((d, f) -> f.texture(d == Direction.NORTH ? "#front" : d.getAxis().isVertical() ? "#top" : "#side")).end();
        }
        p.getVariantBuilder(ModBlocks.RO_UNIT.get()).forAllStates(s -> ConfiguredModel.builder().modelFile(ro[s.getValue(RoUnitBlock.ACTIVE) ? 1 : 0])
                .rotationY(yRot(s.getValue(RoUnitBlock.FACING))).build());
    }

    static void itemModels(ItemModelProvider p) {
        for (String s : List.of("yeast", "co2_solenoid", "ro_membrane", "ro_prefilter", "ro_water_bucket")) {
            p.withExistingParent(s, p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/" + s));
        }
        p.withExistingParent("bio_co2", p.modLoc("block/bio_co2_fermenting"));
        p.withExistingParent("co2_cylinder", p.modLoc("block/co2_cylinder"));
        p.withExistingParent("co2_station", p.modLoc("block/co2_station_loaded"));
        p.withExistingParent("ro_unit", p.modLoc("block/ro_unit"));
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.Criterion<?> hasTank) {
        // yeast: brown mushrooms and sugar set up in a bowl
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.YEAST.get(), 2)
                .requires(Items.BROWN_MUSHROOM).requires(Items.SUGAR).requires(Items.BOWL)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("yeast")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.BIO_CO2.get())
                .pattern(" K ").pattern("G G").pattern("GGG")
                .define('K', Items.DRIED_KELP).define('G', Tags.Items.GLASS_PANES)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("bio_co2")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.CO2_CYLINDER.get())
                .pattern(" C ").pattern("I I").pattern("III")
                .define('C', Tags.Items.INGOTS_COPPER).define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("co2_cylinder")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.CO2_SOLENOID.get())
                .requires(Tags.Items.INGOTS_COPPER).requires(Tags.Items.DUSTS_REDSTONE).requires(Tags.Items.INGOTS_IRON)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("co2_solenoid")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.CO2_STATION.get())
                .pattern("IHI").pattern("CPC").pattern("III")
                .define('I', Tags.Items.INGOTS_IRON).define('H', Items.HOPPER).define('C', Tags.Items.INGOTS_COPPER).define('P', Items.PISTON)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("co2_station")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.RO_UNIT.get())
                .pattern("PGP").pattern("IBI").pattern("III")
                .define('P', ModItems.GLASS_PIPE.get()).define('G', Tags.Items.GLASS_PANES).define('B', Items.BUCKET).define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("ro_unit")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.RO_MEMBRANE.get())
                .requires(Items.PAPER).requires(Items.PAPER).requires(Items.PAPER).requires(Items.PAPER).requires(Tags.Items.SLIME_BALLS).requires(Items.DRIED_KELP)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("ro_membrane")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.RO_PREFILTER.get())
                .requires(Items.CHARCOAL).requires(net.minecraft.tags.ItemTags.WOOL).requires(Items.PAPER).requires(Items.PAPER)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("ro_prefilter")));
    }

    static void blockTags(java.util.function.Function<net.minecraft.tags.TagKey<Block>, net.minecraft.data.tags.TagAppender<Block, Block>> tag) {
        for (Block b : List.of(ModBlocks.CO2_CYLINDER.get(), ModBlocks.CO2_STATION.get(), ModBlocks.RO_UNIT.get()))
            tag.apply(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE).add(b);
        tag.apply(ModTags.FLUE_GAS_SOURCES).add(Blocks.FURNACE, Blocks.SMOKER, Blocks.BLAST_FURNACE, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE);
    }

    /** The CO2 gas of other mods (none in Minecraft): the common tags, if a mod fills them. */
    static final class FluidTagsGen extends net.minecraft.data.tags.FluidTagsProvider {
        FluidTagsGen(PackOutput out, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper efh) {
            super(out, lookup, BettaCraft.MOD_ID);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(ModTags.CO2_GAS).add(net.minecraft.tags.TagEntry.optionalTag(Identifier.fromNamespaceAndPath("c", "carbon_dioxide")))
                    .add(net.minecraft.tags.TagEntry.optionalTag(Identifier.fromNamespaceAndPath("c", "co2")));
        }
    }
}
