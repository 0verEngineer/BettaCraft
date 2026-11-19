package org.overengineer.bettacraft.datagen;

import net.minecraft.core.Direction;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Items;
import org.overengineer.bettacraft.datagen.model.BlockModelBuilder;
import org.overengineer.bettacraft.datagen.model.BlockStateProvider;
import org.overengineer.bettacraft.datagen.model.ConfiguredModel;
import org.overengineer.bettacraft.datagen.model.ItemModelProvider;
import org.overengineer.bettacraft.datagen.model.ModelFile;
import org.overengineer.bettacraft.platform.Tags;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.AirPumpBlock;
import org.overengineer.bettacraft.equipment.DeviceSegmentBlock;
import org.overengineer.bettacraft.equipment.HighPowerLedBlock;
import org.overengineer.bettacraft.equipment.LedPanelBlock;
import org.overengineer.bettacraft.equipment.SegmentBlock;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.equipment.TankLightItem;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;

/** M23 data: models and block states of the air pump, air stone, sponge and corner filter, LED panel, high-power LED; item models; recipes. */
final class DataGenM23 {
    private DataGenM23() {}

    // ------------------------------------------------------------------ block states

    static void blockStates(BlockStateProvider p) {
        spongeFilter(p);
        cornerFilter(p);
        airStone(p);
        airPump(p);
        ledPanel(p);
        highPowerLed(p);
    }

    /** Sponge filter: a black foam sponge (anchor .. middle) and the lift tube cap on top. */
    private static void spongeFilter(BlockStateProvider p) {
        ModelFile sponge = p.models().cubeColumn("sponge_filter_segment_sponge", p.modLoc("block/sponge_filter_sponge"), p.modLoc("block/sponge_filter_sponge"));
        ModelFile top = p.models().cubeColumn("sponge_filter_segment_top", p.modLoc("block/sponge_filter_top_side"), p.modLoc("block/sponge_filter_top"));
        p.getVariantBuilder(ModBlocks.SPONGE_FILTER_SEGMENT.get()).forAllStatesExcept(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(SegmentBlock.PART) == SegmentBlock.Part.TOP ? top : sponge).build(),
                SegmentBlock.SIZE, SegmentBlock.FACING, DeviceSegmentBlock.POWER, DeviceSegmentBlock.SPONGE, DeviceSegmentBlock.ACTIVE);
    }

    /** Corner filter: grey box with a dark slotted intake; the outlet on the facing side at the top; an intake sponge at the bottom. */
    private static void cornerFilter(BlockStateProvider p) {
        String k = "corner_filter_segment";
        ModelFile body = p.models().cubeColumn(k + "_middle", p.modLoc("block/" + k + "_side"), p.modLoc("block/" + k + "_top"));
        ModelFile foot = p.models().cubeColumn(k + "_anchor", p.modLoc("block/" + k + "_intake"), p.modLoc("block/" + k + "_top"));
        ModelFile sponge = p.models().cubeColumn(k + "_sponge", p.modLoc("block/intake_sponge_side"), p.modLoc("block/intake_sponge_top"));
        ModelFile top = p.models().cube(k + "_top_outlet", p.modLoc("block/" + k + "_side"), p.modLoc("block/" + k + "_top"), p.modLoc("block/" + k + "_outlet"),
                p.modLoc("block/" + k + "_side"), p.modLoc("block/" + k + "_side"), p.modLoc("block/" + k + "_side")).texture("particle", p.modLoc("block/" + k + "_side"));
        p.getVariantBuilder(ModBlocks.CORNER_FILTER_SEGMENT.get()).forAllStatesExcept(state -> {
            var part = state.getValue(SegmentBlock.PART);
            if (part == SegmentBlock.Part.TOP) return ConfiguredModel.builder().modelFile(top)
                    .rotationY(((int) state.getValue(SegmentBlock.FACING).toYRot() + 180) % 360).build();
            if (part == SegmentBlock.Part.ANCHOR || part == SegmentBlock.Part.BASE)
                return ConfiguredModel.builder().modelFile(state.getValue(DeviceSegmentBlock.SPONGE) ? sponge : foot).build();
            return ConfiguredModel.builder().modelFile(body).build();
        }, SegmentBlock.SIZE, DeviceSegmentBlock.POWER, DeviceSegmentBlock.ACTIVE);
    }

    /** The internal filter with a fitted intake sponge at its foot (M23); the rest like before. */
    static ModelFile filterSponge(BlockStateProvider p) {
        return p.models().cubeColumn("filter_segment_sponge", p.modLoc("block/intake_sponge_side"), p.modLoc("block/intake_sponge_top"));
    }

    /** Air stone: a round grey stone on a small foot with the hose connector. */
    private static void airStone(BlockStateProvider p) {
        BlockModelBuilder m = p.models().getBuilder("air_stone")
                .texture("side", p.modLoc("block/air_stone_side")).texture("top", p.modLoc("block/air_stone_top"))
                .texture("foot", p.modLoc("block/air_pump_side")).texture("particle", p.modLoc("block/air_stone_side")).renderType("cutout");
        m.element().from(5, 0, 5).to(11, 1, 11).allFaces((d, f) -> f.texture("#foot")).end();
        m.element().from(4, 1, 4).to(12, 5, 12)
                .face(Direction.NORTH).uvs(4, 11, 12, 15).texture("#side").end()
                .face(Direction.SOUTH).uvs(4, 11, 12, 15).texture("#side").end()
                .face(Direction.EAST).uvs(4, 11, 12, 15).texture("#side").end()
                .face(Direction.WEST).uvs(4, 11, 12, 15).texture("#side").end()
                .face(Direction.UP).uvs(4, 4, 12, 12).texture("#top").end()
                .face(Direction.DOWN).uvs(4, 4, 12, 12).texture("#top").end()
                .end();
        m.element().from(7, 5, 7).to(9, 7, 9).allFaces((d, f) -> f.texture("#foot")).end();
        p.getVariantBuilder(ModBlocks.AIR_STONE.get()).forAllStatesExcept(state -> ConfiguredModel.builder().modelFile(m).build(),
                DeviceSegmentBlock.POWER, DeviceSegmentBlock.ACTIVE);
    }

    /** Air pump: a small box on rubber feet with a grille on top and the hose nipple at the front (north in the model). */
    private static void airPump(BlockStateProvider p) {
        BlockModelBuilder m = p.models().getBuilder("air_pump")
                .texture("side", p.modLoc("block/air_pump_side")).texture("top", p.modLoc("block/air_pump_top"))
                .texture("front", p.modLoc("block/air_pump_front")).texture("particle", p.modLoc("block/air_pump_side"));
        m.element().from(2.5F, 1, 4).to(13.5F, 5, 12)
                .face(Direction.NORTH).uvs(2.5F, 8, 13.5F, 12).texture("#front").end()
                .face(Direction.SOUTH).uvs(2.5F, 8, 13.5F, 12).texture("#side").end()
                .face(Direction.EAST).uvs(4, 8, 12, 12).texture("#side").end()
                .face(Direction.WEST).uvs(4, 8, 12, 12).texture("#side").end()
                .face(Direction.UP).uvs(2.5F, 4, 13.5F, 12).texture("#top").end()
                .face(Direction.DOWN).uvs(2.5F, 4, 13.5F, 12).texture("#side").end()
                .end();
        for (float[] f : new float[][]{{3, 4.5F}, {12, 4.5F}, {3, 10.5F}, {12, 10.5F}}) {
            m.element().from(f[0], 0, f[1]).to(f[0] + 1, 1, f[1] + 1).allFaces((d, b) -> b.texture("#front")).end();
        }
        m.element().from(7.5F, 2.5F, 3).to(8.5F, 3.5F, 4).allFaces((d, b) -> b.texture("#top")).end();
        p.getVariantBuilder(ModBlocks.AIR_PUMP.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(m)
                .rotationY(((int) state.getValue(AirPumpBlock.FACING).toYRot() + 180) % 360).build());
    }

    private static void ledPanel(BlockStateProvider p) {
        p.getVariantBuilder(ModBlocks.LED_PANEL.get()).forAllStatesExcept(state -> {
            boolean axisZ = state.getValue(TankLightBlock.AXIS) == Direction.Axis.Z;
            // the model runs along X with the other half towards +Z; turned by 90 degrees +Z becomes -X, so the side flips for Z
            boolean partner = axisZ != state.getValue(LedPanelBlock.PARTNER_POS);
            ModelFile m = panelModel(p, state.getValue(TankLightBlock.NEG), state.getValue(TankLightBlock.POS), partner,
                    state.getValue(TankLightBlock.HANGING), state.getValue(TankLightBlock.LIT));
            return ConfiguredModel.builder().modelFile(m).rotationY(axisZ ? 90 : 0).build();
        }, TankLightBlock.POWERED, TankLightBlock.COLOR, TankLightBlock.DIM);
    }

    private static ModelFile panelModel(BlockStateProvider p, boolean neg, boolean pos, boolean partnerPos, boolean hanging, boolean lit) {
        String name = "led_panel_" + (neg ? "n" : "") + (pos ? "p" : "") + (partnerPos ? "_r" : "_l") + (hanging ? "_hanging" : "") + (lit ? "_on" : "_off");
        float x0 = neg ? 0 : 1, x1 = pos ? 16 : 15, z0 = partnerPos ? 1 : 0, z1 = partnerPos ? 16 : 15, y0 = hanging ? 14.5F : 0;
        return p.models().getBuilder(name)
                .texture("housing", p.modLoc("block/led_panel")).texture("panel", p.modLoc(lit ? "block/led_panel_panel_on" : "block/led_panel_panel_off"))
                .texture("particle", p.modLoc("block/led_panel"))
                .element().from(x0, y0, z0).to(x1, y0 + 1.5F, z1)
                .face(Direction.DOWN).uvs(x0, z0, x1, z1).texture("#panel").tintindex(0).emissivity(lit ? 15 : 0, lit ? 15 : 0).end()
                .face(Direction.UP).uvs(x0, z0, x1, z1).texture("#housing").end()
                .face(Direction.NORTH).uvs(16 - x1, 14.5F, 16 - x0, 16).texture("#housing").end()
                .face(Direction.SOUTH).uvs(x0, 14.5F, x1, 16).texture("#housing").end()
                .face(Direction.WEST).uvs(z0, 14.5F, z1, 16).texture("#housing").end()
                .face(Direction.EAST).uvs(16 - z1, 14.5F, 16 - z0, 16).texture("#housing").end()
                .end();
    }

    private static void highPowerLed(BlockStateProvider p) {
        p.getVariantBuilder(ModBlocks.HIGH_POWER_LED.get()).forAllStatesExcept(state -> ConfiguredModel.builder().modelFile(
                hpModel(p, state.getValue(TankLightBlock.NEG), state.getValue(TankLightBlock.POS), state.getValue(HighPowerLedBlock.NORTH),
                        state.getValue(HighPowerLedBlock.SOUTH), state.getValue(TankLightBlock.HANGING), state.getValue(TankLightBlock.LIT))).build(),
                TankLightBlock.AXIS, TankLightBlock.POWERED, TankLightBlock.COLOR, TankLightBlock.DIM);
    }

    private static ModelFile hpModel(BlockStateProvider p, boolean w, boolean e, boolean n, boolean s, boolean hanging, boolean lit) {
        String name = "high_power_led_" + (w ? "w" : "") + (e ? "e" : "") + (n ? "n" : "") + (s ? "s" : "") + (hanging ? "_hanging" : "") + (lit ? "_on" : "_off");
        float x0 = w ? 0 : 0.5F, x1 = e ? 16 : 15.5F, z0 = n ? 0 : 0.5F, z1 = s ? 16 : 15.5F, y0 = hanging ? 13 : 0;
        return p.models().getBuilder(name)
                .texture("housing", p.modLoc("block/high_power_led")).texture("top", p.modLoc("block/high_power_led_top"))
                .texture("panel", p.modLoc(lit ? "block/high_power_led_panel_on" : "block/high_power_led_panel_off"))
                .texture("particle", p.modLoc("block/high_power_led"))
                .element().from(x0, y0, z0).to(x1, y0 + 3, z1)
                .face(Direction.DOWN).uvs(x0, z0, x1, z1).texture("#panel").tintindex(0).emissivity(lit ? 15 : 0, lit ? 15 : 0).end()
                .face(Direction.UP).uvs(x0, z0, x1, z1).texture("#top").end()
                .face(Direction.NORTH).uvs(16 - x1, 13, 16 - x0, 16).texture("#housing").end()
                .face(Direction.SOUTH).uvs(x0, 13, x1, 16).texture("#housing").end()
                .face(Direction.WEST).uvs(z0, 13, z1, 16).texture("#housing").end()
                .face(Direction.EAST).uvs(16 - z1, 13, 16 - z0, 16).texture("#housing").end()
                .end();
    }

    // ------------------------------------------------------------------ item models

    static void itemModels(ItemModelProvider p) {
        // M29.1: the lights seen from below (panel tinted, tint 0), the air pump and the filters in 3D
        DataGenM291.lamp(p, "led_panel", "led_panel", "led_panel", 1, 15, 0.5F, 15.5F, 1.5F);
        DataGenM291.lamp(p, "high_power_led", "high_power_led", "high_power_led_top", 0.5F, 15.5F, 0.5F, 15.5F, 3);
        DataGenM291.block(p, "air_pump", "air_pump");
        DataGenM291.block(p, "air_stone", "air_stone");
        DataGenM291.spongeFilter(p);
        DataGenM291.filter(p, "corner_filter", "corner_filter_segment_side", "corner_filter_segment_intake", "corner_filter_segment_top",
                "corner_filter_segment_outlet");
        for (String s : new String[]{"air_hose", "intake_sponge"}) {
            p.withExistingParent(s, p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/" + s));
        }
    }

    // ------------------------------------------------------------------ recipes

    static void recipes(RecipeOutput out, net.minecraft.advancements.triggers.Criterion<?> hasFilter, net.minecraft.advancements.triggers.Criterion<?> hasLight) {
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.AIR_HOSE.get(), 4)
                .pattern("K").pattern("K").pattern("K").define('K', Items.DRIED_KELP)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("air_hose")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.AIR_HOSE.get(), 8)
                .pattern("S").pattern("S").pattern("S").define('S', Tags.Items.SLIME_BALLS)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("air_hose_from_slime")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.AIR_PUMP.get())
                .pattern("IRI").pattern("ILI").pattern("H H")
                .define('I', Tags.Items.INGOTS_IRON).define('R', Tags.Items.DUSTS_REDSTONE).define('L', Items.LEATHER).define('H', ModItems.AIR_HOSE.get())
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("air_pump")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.AIR_STONE.get(), 2)
                .requires(Items.FLINT).requires(Tags.Items.SANDS).requires(ModItems.AIR_HOSE.get())
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("air_stone")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.INTAKE_SPONGE.get(), 2)
                .requires(Items.SPONGE).requires(Items.DYE.pick(net.minecraft.world.item.DyeColor.BLACK))
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("intake_sponge")));
        ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.MISC, ModItems.INTAKE_SPONGE.get(), 1)
                .requires(ItemTagsWool.WOOL).requires(Items.DYE.pick(net.minecraft.world.item.DyeColor.BLACK)).requires(Items.STRING)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("intake_sponge_from_wool")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.SPONGE_FILTER.get())
                .pattern(" H ").pattern("SGS").pattern("SSS")
                .define('H', ModItems.AIR_HOSE.get()).define('S', ModItems.INTAKE_SPONGE.get()).define('G', Tags.Items.GLASS_PANES)
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("sponge_filter")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.CORNER_FILTER.get())
                .pattern("GIG").pattern("GRG").pattern("GSG")
                .define('G', Tags.Items.GLASS_PANES).define('I', Tags.Items.INGOTS_IRON).define('R', Tags.Items.DUSTS_REDSTONE)
                .define('S', ModItems.INTAKE_SPONGE.get())
                .unlockedBy("has_filter", hasFilter).save(out, RecipeCompat.key(BettaCraft.id("corner_filter")));
        // the light tiers: three bars into a 2-wide panel (one item = both halves), panels and glowstone into a high-power LED
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.DECORATIONS, ModItems.LED_PANEL.get())
                .pattern("LLL").pattern("III")
                .define('L', ModItems.TANK_LIGHT.get()).define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_light", hasLight).save(out, RecipeCompat.key(BettaCraft.id("led_panel")));
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.DECORATIONS, ModItems.HIGH_POWER_LED.get(), 2)
                .pattern("CCC").pattern("PGP")
                .define('C', Tags.Items.INGOTS_COPPER).define('P', ModItems.LED_PANEL.get()).define('G', Tags.Items.DUSTS_GLOWSTONE)
                .unlockedBy("has_light", hasLight).save(out, RecipeCompat.key(BettaCraft.id("high_power_led")));
        // colour like the bar (M18): one item and a dye
        for (DyeColor c : DyeColor.values()) {
            net.minecraft.world.item.Item dye = RecipeCompat.dye(c);
            for (var led : java.util.List.of(ModItems.LED_PANEL, ModItems.HIGH_POWER_LED)) {
                String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(led.get()).getPath();
                ShapelessRecipeBuilder.shapeless(RecipeCompat.items(), RecipeCategory.DECORATIONS, TankLightItem.template(led.get(), c, 1))
                        .requires(led.get()).requires(dye)
                        .group("bettacraft:" + id + "_dye")
                        .unlockedBy("has_light", hasLight)
                        .save(out, RecipeCompat.key(BettaCraft.id(id + "_" + c.getName())));
            }
        }
    }

    /** Wool tag holder (vanilla item tag). */
    private static final class ItemTagsWool {
        static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> WOOL = net.minecraft.tags.ItemTags.WOOL;
    }
}
