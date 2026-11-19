package org.overengineer.bettacraft.datagen;

import org.overengineer.bettacraft.util.NbtCompat;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.overengineer.bettacraft.datagen.model.ItemModelBuilder;
import org.overengineer.bettacraft.datagen.model.ItemModelProvider;
import org.overengineer.bettacraft.datagen.model.ModelBuilder;

/**
 * M29.1: every block of the mod shows as a 3D block in the inventory, the hotbar, JEI and the handbook, like vanilla
 * blocks. Block models with their own elements have no parent and so no display transforms: their items get the ones of
 * {@code minecraft:block/block}. The lights are seen from below (their coloured panel), the devices that are no block
 * item (heaters, coolers, internal filters) get a small 3D model of the whole device. Plants and crossed models keep a
 * flat icon (like vanilla saplings and seagrass).
 */
final class DataGenM291 {
    private DataGenM291() {}

    /** Item models whose parent is a block model of ours but which keep their own look (none so far). */
    static final Set<String> KEEP = Set.of();


    /**
     * GUI scale and offset (pixels) for the blocks smaller than a full block: they fill the slot about like a vanilla
     * block and sit in its middle (fitted from the model bounds seen at the GUI angle; full blocks keep 0.625).
     */
    static final Map<String, float[]> FIT = Map.ofEntries(
            Map.entry("air_pump", new float[]{1.00F, -0.4F, 4.9F}),
            Map.entry("air_stone", new float[]{1.00F, 0F, 3.9F}),
            Map.entry("artemia_hatchery", new float[]{0.80F, 0F, 1.0F}),
            Map.entry("bio_co2", new float[]{1.00F, 0F, 1.3F}),
            Map.entry("bulkhead", new float[]{1.00F, 0F, 3.5F}),
            Map.entry("clip_feeder", new float[]{1.00F, 4.5F, -5.5F}),
            Map.entry("co2_cylinder", new float[]{0.87F, 0.5F, 0.4F}),
            Map.entry("crucible", new float[]{0.71F, 0F, 0.9F}),
            Map.entry("daphnia_barrel", new float[]{0.69F, 0F, 0.3F}),
            Map.entry("dosing_pump", new float[]{0.83F, 0F, 2.2F}),
            Map.entry("external_filter", new float[]{0.78F, -0.4F, 0.7F}),
            Map.entry("feeder", new float[]{1.00F, 0F, 3.5F}),
            Map.entry("food_maker", new float[]{0.67F, 1.5F, 0.4F}),
            Map.entry("glass_pipe", new float[]{0.91F, 0F, 0F}),
            Map.entry("high_power_led", new float[]{0.67F, 0F, 4.0F}),
            Map.entry("intake_strainer", new float[]{0.81F, 0F, 0F}),
            Map.entry("interior_pipe", new float[]{1.00F, 0F, 0F}),
            Map.entry("larva_tub", new float[]{0.71F, 0F, 1.9F}),
            Map.entry("led_panel", new float[]{0.69F, 0F, 4.6F}),
            Map.entry("level_sensor", new float[]{1.00F, 0F, 2.6F}),
            Map.entry("lily_pipe", new float[]{0.88F, -1.9F, 1.3F}),
            Map.entry("live_food_pump", new float[]{1.00F, 0F, 3.0F}),
            Map.entry("nozzle", new float[]{1.00F, 0.4F, -0.2F}),
            Map.entry("protein_skimmer", new float[]{0.79F, -2.0F, 1.0F}),
            Map.entry("ro_unit", new float[]{0.76F, 0F, 0.7F}),
            Map.entry("spray_bar", new float[]{1.00F, 0F, 0F}),
            Map.entry("stainless_steel_pipe", new float[]{0.91F, 0F, 0F}),
            Map.entry("tank_lid", new float[]{0.625F, 0F, 3.9F}),
            Map.entry("tank_light", new float[]{1.00F, 0F, 6.5F}),
            Map.entry("tubifex_dish", new float[]{0.71F, 0F, 3.4F}),
            Map.entry("water_change_controller", new float[]{0.83F, 0F, 1.4F}));

    /** The display transforms of {@code minecraft:block/block}, with the GUI view scaled and moved. */
    static ItemModelBuilder block(ItemModelBuilder b, float gui, float guiX, float guiY, float guiRotX) {
        return b.transforms()
                .transform(ItemDisplayContext.GUI).rotation(guiRotX, 225, 0).translation(guiX, guiY, 0).scale(gui).end()
                .transform(ItemDisplayContext.GROUND).rotation(0, 0, 0).translation(0, 3, 0).scale(0.25F).end()
                .transform(ItemDisplayContext.FIXED).rotation(0, 0, 0).translation(0, 0, 0).scale(0.5F).end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(75, 45, 0).translation(0, 2.5F, 0).scale(0.375F).end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(75, 45, 0).translation(0, 2.5F, 0).scale(0.375F).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0, 45, 0).translation(0, 0, 0).scale(0.4F).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0, 225, 0).translation(0, 0, 0).scale(0.4F).end()
                .end();
    }

    /** Block display for the item {@code name}, fitted as {@link #FIT} says. */
    static ItemModelBuilder block(ItemModelBuilder b, String name) {
        float[] f = FIT.getOrDefault(name, new float[]{0.625F, 0, 0});
        return block(b, f[0], f[1], f[2], 30);
    }

    /** A block item with the 3D model of its block. */
    static ItemModelBuilder block(ItemModelProvider p, String item, String blockModel) {
        return block(p.withExistingParent(item, p.modLoc("block/" + blockModel)), item);
    }

    /** After all item models: those with a block model of ours as parent and no display get the vanilla block one. */
    static void blockDisplays(ItemModelProvider p, java.util.Map<Identifier, ItemModelBuilder> models) {
        for (var e : models.entrySet()) {
            JsonObject j = e.getValue().toJson();
            String parent = j.has("parent") ? j.get("parent").getAsString() : "";
            String name = e.getKey().getPath().substring(e.getKey().getPath().lastIndexOf('/') + 1); // "item/feeder" -> "feeder"
            if (parent.startsWith("bettacraft:block/") && !j.has("display") && !KEEP.contains(name)) block(e.getValue(), name);
        }
    }

    // ------------------------------------------------------------------ devices that are no block item

    private static ModelBuilder<ItemModelBuilder>.ElementBuilder box(ItemModelBuilder b, float x0, float y0, float z0, float x1, float y1, float z1) {
        return b.element().from(x0, y0, z0).to(x1, y1, z1);
    }

    /** All four sides with one texture and the given uv rectangle; top and bottom with their own. */
    private static ItemModelBuilder sides(ModelBuilder<ItemModelBuilder>.ElementBuilder e, String side, float[] uv, String top, String bottom) {
        for (Direction d : Direction.Plane.HORIZONTAL) e.face(d).uvs(uv[0], uv[1], uv[2], uv[3]).texture(side).end();
        if (top != null) e.face(Direction.UP).uvs(4, 4, 12, 12).texture(top).end();
        if (bottom != null) e.face(Direction.DOWN).uvs(4, 4, 12, 12).texture(bottom).end();
        return e.end();
    }

    /** Heater / cooler: a glass tube with the coil (the middle segment) and the dark cap with the lamp; taller by size. */
    static void tube(ItemModelProvider p, String item, String kind, float bodyHeight) {
        ItemModelBuilder b = p.getBuilder(item).parent(new org.overengineer.bettacraft.datagen.model.ModelFile.UncheckedModelFile("minecraft:block/block"))
                .texture("body", p.modLoc("block/" + kind + "_middle")).texture("foot", p.modLoc("block/" + kind + "_anchor"))
                .texture("cap", p.modLoc("block/" + kind + "_top")).texture("particle", p.modLoc("block/" + kind + "_middle"));
        float y0 = (16 - bodyHeight - 3) / 2F;
        sides(box(b, 6, y0, 6, 10, y0 + bodyHeight, 10), "#body", new float[]{4, 0, 12, 16}, null, "#foot");
        sides(box(b, 5.5F, y0 + bodyHeight, 5.5F, 10.5F, y0 + bodyHeight + 3, 10.5F), "#cap", new float[]{3, 3, 13, 9}, "#cap", "#cap");
        block(b, 0.8F, 0, 0, 30);
    }

    /** Internal / corner filter: a box with the intake slots, the cap on top and the outlet towards the front. */
    static void filter(ItemModelProvider p, String item, String body, String foot, String cap, String outlet) {
        ItemModelBuilder b = p.getBuilder(item).parent(new org.overengineer.bettacraft.datagen.model.ModelFile.UncheckedModelFile("minecraft:block/block"))
                .texture("body", p.modLoc("block/" + body)).texture("foot", p.modLoc("block/" + foot))
                .texture("cap", p.modLoc("block/" + cap)).texture("outlet", p.modLoc("block/" + outlet)).texture("particle", p.modLoc("block/" + body));
        sides(box(b, 4.5F, 1, 5, 11.5F, 6, 11), "#foot", new float[]{2, 2, 14, 10}, null, "#cap");
        sides(box(b, 4.5F, 6, 5, 11.5F, 12, 11), "#body", new float[]{2, 2, 14, 12}, null, null);
        var top = box(b, 4.5F, 12, 5, 11.5F, 15, 11);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            top.face(d).uvs(d == Direction.NORTH ? 3 : 2, d == Direction.NORTH ? 5 : 2, d == Direction.NORTH ? 13 : 14, d == Direction.NORTH ? 11 : 6)
                    .texture(d == Direction.NORTH ? "#outlet" : "#body").end();
        }
        top.face(Direction.UP).uvs(3, 3, 13, 13).texture("#cap").end().end();
        block(b, 0.8F, 0, 0, 30);
    }

    /**
     * Food maker: the green case with the window towards the viewer, a funnel hopper full of food on top and the crank
     * with its knob standing up on the side, a bit bigger than on the block so the icon reads as a hand-cranked machine.
     */
    static void foodMaker(ItemModelProvider p) {
        ItemModelBuilder b = p.getBuilder("food_maker").parent(new org.overengineer.bettacraft.datagen.model.ModelFile.UncheckedModelFile("minecraft:block/block"))
                .texture("front", p.modLoc("block/food_maker_front")).texture("side", p.modLoc("block/food_maker_side"))
                .texture("top", p.modLoc("block/food_maker_top")).texture("hopper", p.modLoc("block/food_maker_hopper"))
                .texture("crank", p.modLoc("block/food_maker_crank")).texture("knob", p.modLoc("block/food_maker_knob"))
                .texture("particle", p.modLoc("block/food_maker_side"));
        b.element().from(2, 0, 3).to(14, 10, 15).allFaces((d, f) -> f.texture(d == Direction.NORTH ? "#front" : d.getAxis().isVertical() ? "#top" : "#side")).end();
        // the funnel: narrow neck, wide mouth full of food
        b.element().from(5.5F, 10, 6.5F).to(10.5F, 12, 11.5F).allFaces((d, f) -> f.texture("#hopper")).end();
        b.element().from(3.5F, 12, 4.5F).to(12.5F, 15.5F, 13.5F).allFaces((d, f) -> f.texture("#hopper")).end();
        // axle, arm standing up, knob
        b.element().from(14, 4.5F, 8).to(15.5F, 6.5F, 10).allFaces((d, f) -> f.texture("#crank")).end();
        b.element().from(15.5F, 4.5F, 8.5F).to(16.5F, 12, 9.5F).allFaces((d, f) -> f.texture("#crank")).end();
        b.element().from(16.5F, 10, 8).to(18.5F, 12.5F, 10).allFaces((d, f) -> f.texture("#knob")).end();
        block(b, "food_maker");
    }

    /**
     * An aquarium light: the housing with its panel and a soft beam of light below it (translucent), panel and beam
     * tinted in the light colour (tint 0, like the panel of the block).
     */
    static void lamp(ItemModelProvider p, String item, String housing, String top, float x0, float x1, float z0, float z1, float h) {
        ItemModelBuilder b = p.getBuilder(item).parent(new org.overengineer.bettacraft.datagen.model.ModelFile.UncheckedModelFile("minecraft:block/block"))
                .texture("housing", p.modLoc("block/" + housing)).texture("top", p.modLoc("block/" + top))
                .texture("panel", p.modLoc("block/" + item + "_panel_on")).texture("cone", p.modLoc("block/light_cone"))
                .texture("particle", p.modLoc("block/" + housing)).renderType("minecraft:translucent");
        float y0 = 4;
        b.element().from(x0, y0, z0).to(x1, y0 + h, z1).allFaces((d, f) -> {
            if (d == Direction.DOWN) f.texture("#panel").tintindex(0);
            else f.texture(d == Direction.UP ? "#top" : "#housing");
        }).end();
        b.element().from(x0 + 0.5F, y0 - 9, z0 + 0.5F).to(x1 - 0.5F, y0, z1 - 0.5F).allFacesExcept((d, f) -> f.texture("#cone").uvs(0, 0, 16, 16).tintindex(0),
                java.util.Set.of(Direction.UP, Direction.DOWN)).end();
        block(b, item);
    }

    /** Sponge filter: the round sponge with the lift tube on top. */
    static void spongeFilter(ItemModelProvider p) {
        ItemModelBuilder b = p.getBuilder("sponge_filter").parent(new org.overengineer.bettacraft.datagen.model.ModelFile.UncheckedModelFile("minecraft:block/block"))
                .texture("sponge", p.modLoc("block/sponge_filter_sponge")).texture("tube", p.modLoc("block/sponge_filter_top_side"))
                .texture("top", p.modLoc("block/sponge_filter_top")).texture("particle", p.modLoc("block/sponge_filter_sponge"));
        sides(box(b, 4, 0.5F, 4, 12, 10.5F, 12), "#sponge", new float[]{0, 0, 16, 16}, "#sponge", "#sponge");
        sides(box(b, 7, 10.5F, 7, 9, 15.5F, 9), "#tube", new float[]{6, 0, 10, 16}, "#top", null);
        block(b, 0.8F, 0, 0, 30);
    }
}
