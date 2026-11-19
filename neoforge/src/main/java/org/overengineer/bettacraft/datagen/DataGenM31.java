package org.overengineer.bettacraft.datagen;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.overengineer.bettacraft.datagen.model.BlockModelBuilder;
import org.overengineer.bettacraft.datagen.model.BlockStateProvider;
import org.overengineer.bettacraft.datagen.model.ConfiguredModel;
import org.overengineer.bettacraft.datagen.model.ItemModelProvider;
import org.overengineer.bettacraft.platform.Tags;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.TankLightBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.terrarium.HeatMatBlock;
import org.overengineer.bettacraft.terrarium.MisterBlock;
import org.overengineer.bettacraft.terrarium.ThermoHygrometerBlock;

/**
 * M31 part a data: the terrarium devices (heat mat, basking lamp, mister, thermo-hygrometer): block states, element
 * models, 3D items, recipes. Textures: {@code tools/assetgen/terrarium.py}.
 */
final class DataGenM31 {
    private DataGenM31() {}

    static List<Block> blocks() {
        return List.of(ModBlocks.HEAT_MAT.get(), ModBlocks.BASKING_LAMP.get(), ModBlocks.MISTER.get(), ModBlocks.THERMO_HYGROMETER.get());
    }

    private static BlockModelBuilder base(BlockStateProvider p, String name) {
        return p.models().getBuilder(name).renderType("cutout");
    }

    static void blockStates(BlockStateProvider p) {
        // heat mat: a thin foil, on the north side of its block (wall, turned by facing) or at the top (under a tank)
        BlockModelBuilder[] wall = new BlockModelBuilder[2], under = new BlockModelBuilder[2];
        for (int i = 0; i < 2; i++) {
            String tex = i == 0 ? "block/heat_mat" : "block/heat_mat_on", sfx = i == 0 ? "" : "_on";
            wall[i] = base(p, "heat_mat_wall" + sfx).texture("mat", p.modLoc(tex)).texture("particle", p.modLoc(tex));
            wall[i].element().from(1, 1, 0).to(15, 15, 1).allFaces((d, f) -> f.texture("#mat").uvs(d.getAxis() == Direction.Axis.Z ? 1 : 0,
                    d.getAxis() == Direction.Axis.Z ? 1 : 0, d.getAxis() == Direction.Axis.Z ? 15 : 1, d.getAxis() == Direction.Axis.Z ? 15 : 14)).end();
            under[i] = base(p, "heat_mat_under" + sfx).texture("mat", p.modLoc(tex)).texture("particle", p.modLoc(tex));
            under[i].element().from(1, 15, 1).to(15, 16, 15).allFaces((d, f) -> f.texture("#mat").uvs(1, d.getAxis().isVertical() ? 1 : 14,
                    15, d.getAxis().isVertical() ? 15 : 15)).end();
        }
        p.getVariantBuilder(ModBlocks.HEAT_MAT.get()).forAllStates(s -> {
            int i = s.getValue(HeatMatBlock.HEATING) ? 1 : 0;
            Direction f = s.getValue(HeatMatBlock.FACING);
            if (f == Direction.UP || f == Direction.DOWN) return ConfiguredModel.builder().modelFile(under[i]).rotationX(f == Direction.DOWN ? 180 : 0).build();
            return ConfiguredModel.builder().modelFile(wall[i]).rotationY((int) f.toYRot() % 360 == 0 ? 180 : ((int) f.toYRot() + 180) % 360).build();
        });

        // basking lamp: a dark reflector dome with the bulb underneath (standing on the tank or hanging on its cord)
        BlockModelBuilder[][] lamp = new BlockModelBuilder[2][2];
        for (int h = 0; h < 2; h++) {
            for (int lit = 0; lit < 2; lit++) {
                String name = "basking_lamp" + (h == 1 ? "_hanging" : "") + (lit == 1 ? "_on" : "_off");
                BlockModelBuilder m = base(p, name).texture("shade", p.modLoc("block/basking_lamp_shade"))
                        .texture("bulb", p.modLoc(lit == 1 ? "block/basking_lamp_bulb" : "block/basking_lamp_bulb_off"))
                        .texture("cap", p.modLoc("block/mister_nozzle")).texture("particle", p.modLoc("block/basking_lamp_shade"));
                float y0 = h == 1 ? 9 : 1;
                // the rim on the glass (standing) and the dome
                if (h == 0) m.element().from(2, 0, 2).to(14, 1, 14).allFaces((d, f) -> f.texture("#cap").uvs(2, 2, 14, d.getAxis().isVertical() ? 14 : 3)).end();
                final int lt = lit;
                m.element().from(3, y0, 3).to(13, y0 + 3, 13).allFaces((d, f) -> {
                    if (d == Direction.DOWN) f.texture("#bulb").uvs(3, 3, 13, 13);
                    else f.texture("#shade").uvs(3, 2, 13, 5);
                    if (d == Direction.DOWN && lt == 1) f.emissivity(15, 15);
                }).end();
                m.element().from(4.5F, y0 + 3, 4.5F).to(11.5F, y0 + 5, 11.5F).allFaces((d, f) -> f.texture("#shade").uvs(4, 0, 12, 2)).end();
                m.element().from(6.5F, y0 + 5, 6.5F).to(9.5F, y0 + 6.5F, 9.5F).allFaces((d, f) -> f.texture("#cap").uvs(6, 6, 9, 8)).end();
                if (h == 1) m.element().from(7.5F, y0 + 6.5F, 7.5F).to(8.5F, 16, 8.5F).allFaces((d, f) -> f.texture("#cap").uvs(7, 0, 8, 1)).end();
                lamp[h][lit] = m;
            }
        }
        p.getVariantBuilder(ModBlocks.BASKING_LAMP.get()).forAllStatesExcept(s -> ConfiguredModel.builder()
                .modelFile(lamp[s.getValue(TankLightBlock.HANGING) ? 1 : 0][s.getValue(TankLightBlock.LIT) ? 1 : 0]).build(),
                TankLightBlock.AXIS, TankLightBlock.NEG, TankLightBlock.POS, TankLightBlock.POWERED, TankLightBlock.COLOR, TankLightBlock.DIM);

        // mister: a water bottle, the pump head on top and the nozzle towards the front (north before turning)
        BlockModelBuilder mister = base(p, "mister").texture("tank", p.modLoc("block/mister_tank")).texture("cap", p.modLoc("block/mister_cap"))
                .texture("nozzle", p.modLoc("block/mister_nozzle")).texture("particle", p.modLoc("block/mister_cap"));
        mister.element().from(5, 0, 5).to(11, 6, 11).allFaces((d, f) -> f.texture(d.getAxis().isVertical() ? "#cap" : "#tank").uvs(5, 2, 11, 16)).end();
        mister.element().from(4.5F, 6, 4.5F).to(11.5F, 8.5F, 11.5F).allFaces((d, f) -> f.texture("#cap").uvs(4, 6, 12, 9)).end();
        mister.element().from(7, 7, 1).to(9, 8.5F, 4.5F).allFaces((d, f) -> f.texture("#nozzle").uvs(0, 0, 4, 2)).end();
        mister.element().from(6.5F, 6.5F, 0.5F).to(9.5F, 9, 1.5F).allFaces((d, f) -> f.texture("#nozzle").uvs(0, 4, 3, 7)).end();
        p.getVariantBuilder(ModBlocks.MISTER.get()).forAllStatesExcept(s -> ConfiguredModel.builder().modelFile(mister)
                .rotationY(((int) s.getValue(MisterBlock.FACING).toYRot() + 180) % 360).build(), MisterBlock.MISTING);

        // thermo-hygrometer: a flat white case on the wall behind it, the LCD looking out (north before turning)
        BlockModelBuilder thermo = base(p, "thermo_hygrometer").texture("front", p.modLoc("block/thermo_hygrometer_front"))
                .texture("side", p.modLoc("block/thermo_hygrometer_side")).texture("particle", p.modLoc("block/thermo_hygrometer_side"));
        thermo.element().from(4, 4, 15).to(12, 12, 16).allFaces((d, f) -> {
            if (d == Direction.NORTH) f.texture("#front").uvs(0, 0, 16, 16);
            else f.texture("#side").uvs(4, 4, 12, d.getAxis() == Direction.Axis.Z ? 12 : 5);
        }).end();
        p.getVariantBuilder(ModBlocks.THERMO_HYGROMETER.get()).forAllStates(s -> ConfiguredModel.builder().modelFile(thermo)
                .rotationY(((int) s.getValue(ThermoHygrometerBlock.FACING).toYRot() + 180) % 360).build());
    }

    /**
     * M46: the heat mat item lies flat like the mat under a terrarium (it stood upright like the wall mat before): the glowing
     * foil in the middle of the item space, a carpet-like view in the inventory, flat in the hand and on the ground, facing
     * out of an item frame. Only the item; the wall mat still stands upright when placed.
     */
    private static void heatMatItem(ItemModelProvider p) {
        var b = p.getBuilder("heat_mat").parent(new org.overengineer.bettacraft.datagen.model.ModelFile.UncheckedModelFile("minecraft:block/block"))
                .texture("mat", p.modLoc("block/heat_mat_on")).texture("particle", p.modLoc("block/heat_mat_on"));
        b.element().from(1, 7.5F, 1).to(15, 8.5F, 15).allFaces((d, f) -> f.texture("#mat").uvs(1, d.getAxis().isVertical() ? 1 : 14, 15, 15)).end();
        b.transforms()
                .transform(net.minecraft.world.item.ItemDisplayContext.GUI).rotation(30, 225, 0).translation(0, 0, 0).scale(0.8F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.GROUND).rotation(0, 0, 0).translation(0, 2, 0).scale(0.4F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.FIXED).rotation(-90, 0, 0).translation(0, 0, 0).scale(0.75F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(75, 45, 0).translation(0, 2.5F, 0).scale(0.375F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(75, 45, 0).translation(0, 2.5F, 0).scale(0.375F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(10, 45, 0).translation(0, 1.5F, 0).scale(0.45F).end()
                .transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(10, 225, 0).translation(0, 1.5F, 0).scale(0.45F).end()
                .end();
    }

    static void itemModels(ItemModelProvider p) {
        heatMatItem(p);
        DataGenM291.block(p.withExistingParent("basking_lamp", p.modLoc("block/basking_lamp_on")), 1.0F, 0, 3.5F, 30);
        DataGenM291.block(p.withExistingParent("mister", p.modLoc("block/mister")), 1.0F, 0, 3.0F, 30);
        DataGenM291.block(p.withExistingParent("thermo_hygrometer", p.modLoc("block/thermo_hygrometer")), 1.3F, 0, 0, 0);
        // the musk turtle: a bucket of water with the turtle, the spawn egg
        p.withExistingParent("musk_turtle_bucket", p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/musk_turtle_bucket"));
        p.withExistingParent("musk_turtle_spawn_egg", p.mcLoc("item/generated")).texture("layer0", p.modLoc("item/musk_turtle_spawn_egg"));
    }

    static void recipes(RecipeOutput out, net.minecraft.advancements.Criterion<?> hasTank) {
        // heat mat: copper heating trace laminated between paper, a redstone thermostat lead
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.HEAT_MAT.get(), 2)
                .pattern("PPP").pattern("CRC").pattern("PPP")
                .define('P', Items.PAPER).define('C', Tags.Items.INGOTS_COPPER).define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("heat_mat")));
        // basking lamp: a reflector of iron around a glowstone bulb, wired with redstone
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.BASKING_LAMP.get())
                .pattern("III").pattern("IGI").pattern(" R ")
                .define('I', Tags.Items.INGOTS_IRON).define('G', Tags.Items.DUSTS_GLOWSTONE).define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("basking_lamp")));
        // mister: a bottle as the water tank, a little pump (redstone) and an iron nozzle
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.MISTER.get())
                .pattern(" N ").pattern("GBG").pattern("GRG")
                .define('N', Tags.Items.NUGGETS_IRON).define('G', Tags.Items.GLASS_PANES).define('B', Items.GLASS_BOTTLE).define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("mister")));
        // thermo-hygrometer: a small display (glass pane) in an iron case with the sensor electronics (redstone)
        ShapedRecipeBuilder.shaped(RecipeCompat.items(), RecipeCategory.MISC, ModItems.THERMO_HYGROMETER.get())
                .pattern("NGN").pattern("NRN")
                .define('N', Tags.Items.NUGGETS_IRON).define('G', Tags.Items.GLASS_PANES).define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_tank", hasTank).save(out, RecipeCompat.key(BettaCraft.id("thermo_hygrometer")));
    }

    static void blockTags(java.util.function.Function<net.minecraft.tags.TagKey<Block>, net.minecraft.data.tags.TagAppender<Block, Block>> tag) {
        tag.apply(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.BASKING_LAMP.get(), ModBlocks.MISTER.get(), ModBlocks.THERMO_HYGROMETER.get());
    }
}
