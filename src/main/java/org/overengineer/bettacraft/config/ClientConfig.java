package org.overengineer.bettacraft.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Rendering and sound preferences (client only). */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue ANIMATION_DISTANCE;
    public static final ModConfigSpec.BooleanValue FILTER_SOUND;
    public static final ModConfigSpec.BooleanValue WATER_SURFACE_ANIMATION;
    public static final ModConfigSpec.IntValue MINIATURE_RENDER_DISTANCE;
    public static final ModConfigSpec.BooleanValue LOG_MINIATURE_STATS;
    public static final ModConfigSpec.BooleanValue CONNECTED_GLASS;
    public static final ModConfigSpec.IntValue TANK_EDGE_WIDTH;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Rendering of tanks, fish and interior miniatures").push("render");
        ANIMATION_DISTANCE = b.comment("Fish further away than this (blocks) are not animated").defineInRange("animationDistance", 48, 8, 256);
        WATER_SURFACE_ANIMATION = b.comment("Animated glitter on the tank water surface").define("waterSurfaceAnimation", true);
        MINIATURE_RENDER_DISTANCE = b.comment("Distance (blocks) up to which tank interiors are rendered as miniature").defineInRange("miniatureRenderDistance", 64, 8, 512);
        CONNECTED_GLASS = b.comment("Connected glass textures on textured tanks: the frame of the glass texture only along the outer edges of the whole tank (off: every block shows its own frame)")
                .define("connectedGlass", true);
        TANK_EDGE_WIDTH = b.comment("Width of the dark edge lines along the outer edges of a tank, in 1/64 block (0 = no edge lines)")
                .defineInRange("tankEdgeWidth", 2, 0, 4);
        LOG_MINIATURE_STATS = b.comment("Write mesh build and sync statistics of tank miniatures to the log (for debugging)").define("logMiniatureStats", false);
        b.pop();
        b.comment("Sounds").push("sound");
        FILTER_SOUND = b.comment("Play the filter bubbling sound").define("filterSound", true);
        b.pop();
        SPEC = b.build();
    }

    private ClientConfig() {}
}
