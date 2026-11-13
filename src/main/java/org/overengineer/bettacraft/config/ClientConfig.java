package org.overengineer.bettacraft.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Rendering and sound preferences (client only). */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue ANIMATION_DISTANCE;
    public static final ModConfigSpec.BooleanValue FILTER_SOUND;
    public static final ModConfigSpec.BooleanValue WATER_SURFACE_ANIMATION;
    public static final ModConfigSpec.IntValue MINIATURE_RENDER_DISTANCE;
    public static final ModConfigSpec.IntValue TANK_DETAIL_DISTANCE;
    /** M21.1: up to this distance the miniature shows the plant meshes (Rotala, Vallisneria, Anubias), further away voxels. */
    public static final ModConfigSpec.IntValue MINIATURE_PLANT_MESH_DISTANCE;
    public static final ModConfigSpec.BooleanValue LOG_MINIATURE_STATS;
    public static final ModConfigSpec.BooleanValue CONNECTED_GLASS;
    public static final ModConfigSpec.DoubleValue TANK_EDGE_WIDTH;
    /** M15: size of the tank name sign (factor on the default height of 0.12 blocks). */
    public static final ModConfigSpec.DoubleValue SIGN_SCALE;
    /** M15: the small hint "sneak + look up: leave" at the bottom left while in a tank interior. */
    public static final ModConfigSpec.BooleanValue INTERIOR_LEAVE_HINT;
    /** M18: how strongly coloured tank lights tint the water (0 = not at all). */
    public static final ModConfigSpec.DoubleValue LIGHT_TINT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Rendering of tanks, fish and interior miniatures").push("render");
        ANIMATION_DISTANCE = b.comment("Fish further away than this (blocks) are not animated").defineInRange("animationDistance", 48, 8, 256);
        WATER_SURFACE_ANIMATION = b.comment("Animated glitter on the tank water surface").define("waterSurfaceAnimation", true);
        MINIATURE_RENDER_DISTANCE = b.comment("Distance (blocks) up to which tank interiors are rendered as miniature").defineInRange("miniatureRenderDistance", 64, 8, 512);
        MINIATURE_PLANT_MESH_DISTANCE = b.comment("Distance (blocks) up to which the miniature of a tank shows the plants with real leaves (like inside, features.plantMeshes); further away they are drawn as pixels again. 0 = always as pixels")
                .defineInRange("miniaturePlantMeshDistance", 24, 0, 256);
        TANK_DETAIL_DISTANCE = b.comment("Distance (blocks) up to which the name sign, bubble nest, floating plants, small animals and algae of a tank are drawn")
                .defineInRange("tankDetailDistance", 48, 8, 256);
        CONNECTED_GLASS = b.comment("Connected glass textures on textured tanks: the frame of the glass texture only along the outer edges of the whole tank (off: every block shows its own frame)")
                .define("connectedGlass", true);
        TANK_EDGE_WIDTH = b.comment("Width of the dark edge lines along the outer edges of a tank, in pixels (1/16 block): 0 = no edge lines, otherwise 0.1 to 1")
                .defineInRange("tankEdgeWidth", 0.25, 0.0, 1.0);
        SIGN_SCALE = b.comment("Size of the tank name sign: factor on the default text height of 0.12 blocks (0.5 to 2)")
                .defineInRange("signScale", 1.0, 0.5, 2.0);
        LIGHT_TINT = b.comment("How strongly coloured aquarium lights tint the water of their tank: 0 = not at all (the light colour only shows at the lamp), 1 = water in the light colour")
                .defineInRange("lightTint", 0.6, 0.0, 1.0);
        LOG_MINIATURE_STATS = b.comment("Write mesh build and sync statistics of tank miniatures to the log (for debugging)").define("logMiniatureStats", false);
        b.pop();
        b.comment("On-screen hints").push("hud");
        INTERIOR_LEAVE_HINT = b.comment("Show the small hint how to leave (sneak + look up) at the bottom left while you are in a tank interior; the progress bar while you hold it is always shown")
                .define("interiorLeaveHint", true);
        b.pop();
        b.comment("Sounds").push("sound");
        FILTER_SOUND = b.comment("Play the filter bubbling sound").define("filterSound", true);
        b.pop();
        SPEC = b.build();
    }

    private ClientConfig() {}
}
