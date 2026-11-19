package org.overengineer.bettacraft.datagen;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import org.overengineer.bettacraft.datagen.model.BlockModelBuilder;
import org.overengineer.bettacraft.datagen.model.BlockStateProvider;
import org.overengineer.bettacraft.datagen.model.ConfiguredModel;
import org.overengineer.bettacraft.datagen.model.ItemModelProvider;
import org.overengineer.bettacraft.datagen.model.ModelFile;
import org.overengineer.bettacraft.datagen.model.MultiPartBlockStateBuilder;
import org.overengineer.bettacraft.platform.Tags;
import org.overengineer.bettacraft.platform.crafting.SizedIngredient;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.crucible.CrucibleBlock;
import org.overengineer.bettacraft.crucible.CrucibleRecipe;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.plumbing.WaterDeviceBlock;
import org.overengineer.bettacraft.plumbing.WaterPumpBlock;
import org.overengineer.bettacraft.plumbing.interior.PlumbingBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M24 data: block states and models (pipes as multipart, pump, crucible, reservoir, sensor, water change controller,
 * filter parts, interior plumbing), item models and recipes (crucible melt, pipes, devices, media). The recipes for the
 * machines of other mods are hand-written in {@code data/bettacraft/recipe/compat/} (conditions on the mod).
 */
final class DataGenM24 {
    private DataGenM24() {}

    // ------------------------------------------------------------------ block states

    static void blockStates(BlockStateProvider p) {
        pipe(p, ModBlocks.GLASS_PIPE.get(), "glass_pipe", 5, 11, "translucent");
        pipe(p, ModBlocks.STAINLESS_STEEL_PIPE.get(), "stainless_steel_pipe", 5, 11, "solid");
        pipe(p, ModBlocks.INTERIOR_PIPE.get(), "interior_pipe", 6, 10, "translucent");
        waterPump(p);
        crucible(p);
        devices(p);
        filter(p);
        interior(p);
    }

    /** Multipart pipe: the core and one arm per joined side (the arm model points north). */
    private static void pipe(BlockStateProvider p, Block block, String name, float a, float b, String render) {
        String tex = name.equals("interior_pipe") ? "block/plumbing_glass" : "block/" + name;
        BlockModelBuilder core = p.models().getBuilder(name + "_core").texture("t", p.modLoc(tex)).texture("particle", p.modLoc(tex)).renderType(render);
        core.element().from(a, a, a).to(b, b, b).allFaces((d, f) -> f.texture("#t")).end();
        BlockModelBuilder arm = p.models().getBuilder(name + "_arm").texture("t", p.modLoc(tex)).texture("particle", p.modLoc(tex)).renderType(render);
        arm.element().from(a, a, 0).to(b, b, a)
                .face(Direction.UP).uvs(a, 0, b, a).texture("#t").end()
                .face(Direction.DOWN).uvs(a, 0, b, a).texture("#t").end()
                .face(Direction.EAST).uvs(0, a, a, b).texture("#t").end()
                .face(Direction.WEST).uvs(0, a, a, b).texture("#t").end()
                .face(Direction.NORTH).uvs(a, a, b, b).texture("#t").end()
                .end();
        MultiPartBlockStateBuilder mp = p.getMultipartBuilder(block);
        mp.part().modelFile(core).addModel().end();
        for (Direction d : Direction.values()) {
            var part = mp.part().modelFile(arm);
            switch (d) {
                case UP -> part.rotationX(270);
                case DOWN -> part.rotationX(90);
                default -> part.rotationY(((int) d.toYRot() + 180) % 360);
            }
            part.addModel().condition(PipeBlock.SIDES.get(d), true).end();
        }
        // item: a straight piece
        BlockModelBuilder item = p.models().getBuilder(name + "_item").texture("t", p.modLoc(tex)).texture("particle", p.modLoc(tex)).renderType(render);
        item.element().from(a, a, 0).to(b, b, 16).allFaces((d, f) -> f.texture("#t")).end();
    }

    private static int xRot(Direction f) {
        return f == Direction.UP ? 270 : f == Direction.DOWN ? 90 : 0;
    }

    private static int yRot(Direction f) {
        return f.getAxis().isVertical() ? 0 : ((int) f.toYRot() + 180) % 360;
    }

    private static void waterPump(BlockStateProvider p) {
        ModelFile m = p.models().cube("water_pump", p.modLoc("block/water_pump_side"), p.modLoc("block/water_pump_side"), p.modLoc("block/water_pump_front"),
                p.modLoc("block/water_pump_back"), p.modLoc("block/water_pump_side"), p.modLoc("block/water_pump_side")).texture("particle", p.modLoc("block/water_pump_side"));
        p.getVariantBuilder(ModBlocks.WATER_PUMP.get()).forAllStatesExcept(s -> {
            Direction f = s.getValue(WaterPumpBlock.FACING);
            return ConfiguredModel.builder().modelFile(m).rotationX(xRot(f)).rotationY(yRot(f)).build();
        }, WaterPumpBlock.ACTIVE);
    }

    private static void crucible(BlockStateProvider p) {
        BlockModelBuilder[] models = new BlockModelBuilder[2];
        for (boolean lit : new boolean[]{false, true}) {
            BlockModelBuilder m = models[lit ? 1 : 0] = p.models().getBuilder(lit ? "crucible_lit" : "crucible")
                    .texture("side", p.modLoc("block/crucible_side")).texture("top", p.modLoc(lit ? "block/crucible_top_lit" : "block/crucible_top"))
                    .texture("particle", p.modLoc("block/crucible_side"));
            m.element().from(1, 0, 1).to(15, 3, 15).allFaces((d, f) -> f.texture(d == Direction.UP ? "#top" : "#side")).end();
            m.element().from(2, 3, 2).to(14, 13, 14).allFaces((d, f) -> {
                f.texture(d == Direction.UP ? "#top" : "#side");
                if (lit && d == Direction.UP) f.emissivity(15, 15);
            }).end();
        }
        ModelFile off = models[0], on = models[1];
        p.getVariantBuilder(ModBlocks.CRUCIBLE.get()).forAllStates(s -> ConfiguredModel.builder().modelFile(s.getValue(CrucibleBlock.LIT) ? on : off)
                .rotationY(yRot(s.getValue(CrucibleBlock.FACING))).build());
    }

    private static void devices(BlockStateProvider p) {
        ModelFile empty = p.models().cubeColumn("reservoir", p.modLoc("block/reservoir_side"), p.modLoc("block/reservoir_top")).renderType("solid");
        ModelFile full = p.models().cubeColumn("reservoir_full", p.modLoc("block/reservoir_side_full"), p.modLoc("block/reservoir_top"));
        p.getVariantBuilder(ModBlocks.RESERVOIR.get()).forAllStatesExcept(s -> ConfiguredModel.builder()
                .modelFile(s.getValue(WaterDeviceBlock.FILL) >= 4 ? full : empty).build(), WaterDeviceBlock.FACING);
        BlockModelBuilder sensor = p.models().getBuilder("level_sensor").texture("t", p.modLoc("block/level_sensor"))
                .texture("particle", p.modLoc("block/level_sensor"));
        sensor.element().from(4, 0, 4).to(12, 10, 12).allFaces((d, f) -> f.texture("#t")).end();
        p.getVariantBuilder(ModBlocks.LEVEL_SENSOR.get()).forAllStatesExcept(s -> ConfiguredModel.builder().modelFile(sensor)
                .rotationY(yRot(s.getValue(WaterDeviceBlock.FACING))).build(), WaterDeviceBlock.FILL);
        BlockModelBuilder ch = p.models().getBuilder("water_change_controller").texture("front", p.modLoc("block/water_change_controller_front"))
                .texture("side", p.modLoc("block/water_change_controller_side")).texture("particle", p.modLoc("block/water_change_controller_side"));
        ch.element().from(2, 0, 2).to(14, 12, 14).allFaces((d, f) -> f.texture(d == Direction.NORTH ? "#front" : "#side")).end();
        p.getVariantBuilder(ModBlocks.WATER_CHANGE_CONTROLLER.get()).forAllStatesExcept(s -> ConfiguredModel.builder().modelFile(ch)
                .rotationY(yRot(s.getValue(WaterDeviceBlock.FACING))).build(), WaterDeviceBlock.FILL);
    }

    private static void filter(BlockStateProvider p) {
        p.getVariantBuilder(ModBlocks.FILTER_CHAMBER.get()).forAllStatesExcept(s -> {
            String m = s.getValue(FilterPartBlock.MEDIUM).getSerializedName();
            return ConfiguredModel.builder().modelFile(p.models().cubeColumn("filter_chamber_" + m, p.modLoc("block/filter_chamber_" + m),
                    p.modLoc("block/filter_chamber_top"))).build();
        }, FilterPartBlock.FACING, FilterPartBlock.ACTIVE);
        p.getVariantBuilder(ModBlocks.FILTER_PUMP_HEAD.get()).forAllStatesExcept(s -> ConfiguredModel.builder().modelFile(
                p.models().cubeColumn(s.getValue(FilterPartBlock.ACTIVE) ? "filter_pump_head_on" : "filter_pump_head",
                        p.modLoc(s.getValue(FilterPartBlock.ACTIVE) ? "block/filter_pump_head_side_on" : "block/filter_pump_head_side"),
                        p.modLoc("block/filter_pump_head_top"))).build(), FilterPartBlock.FACING, FilterPartBlock.MEDIUM);
        for (Block b : List.of(ModBlocks.FILTER_INLET.get(), ModBlocks.FILTER_OUTLET.get())) {
            String n = b == ModBlocks.FILTER_INLET.get() ? "filter_inlet" : "filter_outlet";
            ModelFile m = p.models().cube(n, p.modLoc("block/filter_chamber_top"), p.modLoc("block/filter_chamber_top"), p.modLoc("block/" + n),
                    p.modLoc("block/filter_chamber_top"), p.modLoc("block/filter_chamber_top"), p.modLoc("block/filter_chamber_top"))
                    .texture("particle", p.modLoc("block/filter_chamber_top"));
            p.getVariantBuilder(b).forAllStatesExcept(s -> {
                Direction f = s.getValue(FilterPartBlock.FACING);
                return ConfiguredModel.builder().modelFile(m).rotationX(xRot(f)).rotationY(yRot(f)).build();
            }, FilterPartBlock.MEDIUM, FilterPartBlock.ACTIVE);
        }
    }

    private static void interior(BlockStateProvider p) {
        BlockModelBuilder port = p.models().getBuilder("bulkhead").texture("s", p.modLoc("block/plumbing_steel")).texture("g", p.modLoc("block/plumbing_glass"))
                .texture("particle", p.modLoc("block/plumbing_steel")).renderType("translucent");
        port.element().from(3, 0, 3).to(13, 2, 13).allFaces((d, f) -> f.texture("#s")).end();
        port.element().from(6, 2, 6).to(10, 8, 10).allFaces((d, f) -> f.texture("#g")).end();
        simple(p, ModBlocks.BULKHEAD.get(), port, false);
        BlockModelBuilder strainer = p.models().getBuilder("intake_strainer").texture("t", p.modLoc("block/intake_strainer")).texture("g", p.modLoc("block/plumbing_glass"))
                .texture("particle", p.modLoc("block/intake_strainer")).renderType("translucent");
        strainer.element().from(4, 0, 4).to(12, 10, 12).allFaces((d, f) -> f.texture("#t")).end();
        strainer.element().from(6, 10, 6).to(10, 16, 10).allFaces((d, f) -> f.texture("#g")).end();
        simple(p, ModBlocks.INTAKE_STRAINER.get(), strainer, true);
        // lily pipe: a glass stem up to a mouth that opens towards the facing (north in the model)
        BlockModelBuilder lily = p.models().getBuilder("lily_pipe").texture("g", p.modLoc("block/plumbing_glass"))
                .texture("particle", p.modLoc("block/plumbing_glass")).renderType("translucent");
        lily.element().from(7, 0, 7).to(9, 14, 9).allFaces((d, f) -> f.texture("#g")).end();
        lily.element().from(7, 12, 3).to(9, 14, 7).allFaces((d, f) -> f.texture("#g")).end();
        lily.element().from(5, 11, 1).to(11, 15, 3).allFaces((d, f) -> f.texture("#g")).end();
        simple(p, ModBlocks.LILY_PIPE.get(), lily, true);
        BlockModelBuilder nozzle = p.models().getBuilder("nozzle").texture("s", p.modLoc("block/plumbing_steel")).texture("n", p.modLoc("block/nozzle"))
                .texture("particle", p.modLoc("block/plumbing_steel"));
        nozzle.element().from(6, 6, 6).to(10, 10, 14).allFaces((d, f) -> f.texture(d == Direction.NORTH ? "#n" : "#s")).end();
        nozzle.element().from(7, 7, 3).to(9, 9, 6).allFaces((d, f) -> f.texture("#s")).end();
        simple(p, ModBlocks.NOZZLE.get(), nozzle, true);
        BlockModelBuilder bar = p.models().getBuilder("spray_bar").texture("s", p.modLoc("block/plumbing_steel")).texture("n", p.modLoc("block/nozzle"))
                .texture("particle", p.modLoc("block/plumbing_steel"));
        bar.element().from(0, 6, 6).to(16, 10, 10).allFaces((d, f) -> f.texture(d == Direction.NORTH ? "#n" : "#s")).end();
        simple(p, ModBlocks.SPRAY_BAR.get(), bar, true);
    }

    private static void simple(BlockStateProvider p, PlumbingBlock block, ModelFile m, boolean facing) {
        p.getVariantBuilder(block).forAllStatesExcept(s -> ConfiguredModel.builder().modelFile(m)
                .rotationY(facing ? yRot(s.getValue(PlumbingBlock.FACING)) : 0).build(), sidesAndActive());
    }

    private static net.minecraft.world.level.block.state.properties.Property<?>[] sidesAndActive() {
        List<net.minecraft.world.level.block.state.properties.Property<?>> l = new java.util.ArrayList<>(PipeBlock.SIDES.values());
        l.add(PlumbingBlock.ACTIVE);
        return l.toArray(new net.minecraft.world.level.block.state.properties.Property<?>[0]);
    }

    // ------------------------------------------------------------------ item models

    static void itemModels(ItemModelProvider p) {
        for (String s : List.of("stainless_steel_ingot", "filter_foam", "ceramic_rings", "activated_carbon", "filter_peat")) {
            p.withExistingParent(s, p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/" + s));
        }
        // M29.1: the interior plumbing in 3D
        DataGenM291.block(p, "bulkhead", "bulkhead");
        DataGenM291.block(p, "interior_pipe", "interior_pipe_item");
        for (String s : List.of("intake_strainer", "lily_pipe", "nozzle", "spray_bar")) DataGenM291.block(p, s, s);
        p.withExistingParent("glass_pipe", p.modLoc("block/glass_pipe_item"));
        p.withExistingParent("stainless_steel_pipe", p.modLoc("block/stainless_steel_pipe_item"));
        p.withExistingParent("crucible", p.modLoc("block/crucible_lit")); // M29.1: the glowing melt makes the icon readable
        for (String s : List.of("water_pump", "reservoir", "level_sensor", "water_change_controller", "filter_pump_head", "filter_inlet", "filter_outlet")) {
            p.withExistingParent(s, p.modLoc("block/" + s));
        }
        p.withExistingParent("filter_chamber", p.modLoc("block/filter_chamber_none"));
    }

    // ------------------------------------------------------------------ recipes

    private static Ingredient tag(String c) {
        return RecipeCompat.tag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, Identifier.fromNamespaceAndPath("c", c)));
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.Criterion<?> hasFilter) {
        // the crucible melt (M29.1: no blaze powder any more, one iron more instead): 4 iron + coal (carbon) + nether quartz
        // (stands in for chromium) -> 3 stainless steel; blaze rods and blaze powder stay fuel
        out.accept(RecipeCompat.key(BettaCraft.id("stainless_steel_ingot_from_crucible")), new CrucibleRecipe(List.of(
                new SizedIngredient(RecipeCompat.tag(Tags.Items.INGOTS_IRON), 4),
                new SizedIngredient(RecipeCompat.tag(net.minecraft.tags.ItemTags.COALS), 1),
                new SizedIngredient(RecipeCompat.tag(Tags.Items.GEMS_QUARTZ), 1)), new net.minecraft.world.item.ItemStackTemplate(ModItems.STAINLESS_STEEL_INGOT.get(), 3)), null);
        var steel = ModTags.STAINLESS_STEEL_INGOTS;
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.CRUCIBLE.get())
                .pattern("N N").pattern("NCN").pattern("NNN")
                .define('N', Items.NETHER_BRICK).define('C', Items.CAULDRON)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("crucible")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.GLASS_PIPE.get(), 8)
                .pattern("GGG").define('G', Tags.Items.GLASS_PANES)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("glass_pipe")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.STAINLESS_STEEL_PIPE.get(), 8)
                .pattern("SSS").define('S', steel)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("stainless_steel_pipe")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.WATER_PUMP.get())
                .pattern(" S ").pattern("PRP").pattern(" S ")
                .define('S', steel).define('P', ModItems.GLASS_PIPE.get()).define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("water_pump")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.BULKHEAD.get())
                .requires(steel).requires(ModItems.GLASS_PIPE.get()).requires(Items.DRIED_KELP)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("bulkhead")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.INTERIOR_PIPE.get(), 8)
                .requires(ModItems.GLASS_PIPE.get())
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("interior_pipe")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.INTAKE_STRAINER.get())
                .requires(ModItems.INTERIOR_PIPE.get()).requires(Items.IRON_BARS)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("intake_strainer")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.LILY_PIPE.get())
                .requires(ModItems.INTERIOR_PIPE.get()).requires(ModItems.INTERIOR_PIPE.get()).requires(Items.GLASS_BOTTLE)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("lily_pipe")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.NOZZLE.get(), 2)
                .requires(ModItems.INTERIOR_PIPE.get()).requires(Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("nozzle")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.SPRAY_BAR.get())
                .requires(ModItems.INTERIOR_PIPE.get()).requires(ModItems.INTERIOR_PIPE.get()).requires(ModItems.INTERIOR_PIPE.get()).requires(Tags.Items.NUGGETS_IRON)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("spray_bar")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.FILTER_CHAMBER.get(), 2)
                .pattern("IGI").pattern("G G").pattern("IGI")
                .define('I', Tags.Items.INGOTS_IRON).define('G', Tags.Items.GLASS_PANES)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("filter_chamber")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.FILTER_PUMP_HEAD.get())
                .pattern("SRS").pattern("SPS")
                .define('S', steel).define('R', Tags.Items.DUSTS_REDSTONE).define('P', ModItems.WATER_PUMP.get())
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("filter_pump_head")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.FILTER_INLET.get(), 2)
                .pattern("P").pattern("S").define('P', ModItems.GLASS_PIPE.get()).define('S', steel)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("filter_inlet")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.FILTER_OUTLET.get(), 2)
                .pattern("S").pattern("P").define('P', ModItems.GLASS_PIPE.get()).define('S', steel)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("filter_outlet")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.FILTER_FOAM.get(), 1)
                .requires(net.minecraft.tags.ItemTags.WOOL).requires(Items.DRIED_KELP)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("filter_foam")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.CERAMIC_RINGS.get(), 1)
                .pattern(" C ").pattern("CBC").pattern(" C ")
                .define('C', Items.CLAY_BALL).define('B', Items.BONE_MEAL)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("ceramic_rings")));
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Items.CHARCOAL), RecipeCategory.MISC, net.minecraft.world.item.crafting.CookingBookCategory.MISC, ModItems.ACTIVATED_CARBON.get(), 0.1F, 200)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("activated_carbon")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.FILTER_PEAT.get(), 1)
                .requires(Items.MUD).requires(Items.MOSS_CARPET)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("filter_peat")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.RESERVOIR.get())
                .pattern("IGI").pattern("G G").pattern("IGI")
                .define('I', Tags.Items.INGOTS_IRON).define('G', Tags.Items.GLASS_BLOCKS_CHEAP)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("reservoir")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.LEVEL_SENSOR.get())
                .pattern("C").pattern("S").pattern("P")
                .define('C', Items.COMPARATOR).define('S', steel).define('P', ModItems.GLASS_PIPE.get())
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("level_sensor")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.WATER_CHANGE_CONTROLLER.get())
                .pattern("SCS").pattern("SPS")
                .define('S', steel).define('C', Items.CLOCK).define('P', ModItems.WATER_PUMP.get())
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("water_change_controller")));
    }
}
