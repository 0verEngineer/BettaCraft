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
    /** M22.1: this player sees the plants and corals with real shapes (with features.visuals.plantMeshes on), else the old blocks. */
    public static final ModConfigSpec.BooleanValue PLANT_MESHES;
    /** M22.1: Minecraft's and other mods' water plants sway in the miniature outside the tank (with features.visuals.swayingForeignPlants). */
    public static final ModConfigSpec.BooleanValue SWAYING_PLANTS;
    public static final ModConfigSpec.BooleanValue CONNECTED_GLASS;
    public static final ModConfigSpec.DoubleValue TANK_EDGE_WIDTH;
    /** M15: size of the tank name sign (factor on the default height of 0.12 blocks). */
    public static final ModConfigSpec.DoubleValue SIGN_SCALE;
    /** M15: the small hint "sneak + look up: leave" at the bottom left while in a tank interior. */
    public static final ModConfigSpec.BooleanValue INTERIOR_LEAVE_HINT;
    /** M18: how strongly coloured tank lights tint the water (0 = not at all). */
    public static final ModConfigSpec.DoubleValue LIGHT_TINT;
    /** M50: how strongly the water of a travelling tank sloshes (0 = flat like a standing tank). */
    public static final ModConfigSpec.DoubleValue TRAVEL_SLOSHING;
    /** M23: volume of the device sounds per kind (times the power regulator of the device). */
    public static final ModConfigSpec.DoubleValue VOLUME_FILTER, VOLUME_AIR_PUMP, VOLUME_AIR_STONE, VOLUME_FLOW_PUMP, VOLUME_FEEDER;
    /**
     * M29.1: the steady motor sounds of the air pumps and flow pumps are off by default (new keys, so existing client configs
     * get the new default {@code false} without a migration); the volumes above only apply once a switch is on.
     */
    public static final ModConfigSpec.BooleanValue AIR_PUMP_SOUND, FLOW_PUMP_SOUND;
    /** M32 part d: waypoints at tanks with a monitor alarm on this player's map (JourneyMap), with features.alarmWaypoints. */
    public static final ModConfigSpec.BooleanValue ALARM_WAYPOINTS;
    /** M44: the walk animation of mobs in tanks: "auto", a factor, 0 = vanilla ({@code terrarium.WalkAnimation}). */
    public static final ModConfigSpec.ConfigValue<String> WALK_ANIMATION_BOOST;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Rendering of tanks, fish and interior miniatures").push("render");
        ANIMATION_DISTANCE = b.comment("Fish further away than this (blocks) are not animated").defineInRange("animationDistance", 48, 8, 256);
        WATER_SURFACE_ANIMATION = b.comment("Animated glitter on the tank water surface").define("waterSurfaceAnimation", true);
        MINIATURE_RENDER_DISTANCE = b.comment("Distance (blocks) up to which tank interiors are rendered as miniature").defineInRange("miniatureRenderDistance", 64, 8, 512);
        MINIATURE_PLANT_MESH_DISTANCE = b.comment("Distance (blocks) up to which the miniature of a tank shows the plants with real leaves (like inside, features.visuals.plantMeshes); further away they are drawn as pixels again. 0 = always as pixels")
                .defineInRange("miniaturePlantMeshDistance", 24, 0, 256);
        PLANT_MESHES = b.comment("Aquarium plants and corals with real leaves, polyps and tentacles that move in the water, inside the tank and in the "
                + "miniature (off: the old block look, only for you; the pack switch features.visuals.plantMeshes turns it off for everyone)").define("plantMeshes", true);
        SWAYING_PLANTS = b.comment("Kelp, seagrass, corals and the water plants of other mods sway in the miniature outside the tank (off: they stand still, only "
                + "for you; the pack switch features.visuals.swayingForeignPlants turns it off for everyone)").define("swayingPlants", true);
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
        TRAVEL_SLOSHING = b.comment("How strongly the water of a tank on a Create contraption or an airship sloshes when it speeds up, brakes or takes",
                        "a curve: the surface tilts and waves run over it (0 = off: flat water as in a standing tank, 1 = natural, 2 = twice as much)")
                .defineInRange("travelSloshing", 1.0, 0.0, 2.0);
        WALK_ANIMATION_BOOST = b.comment("Leg movement of mobs and animals walking in tanks and terraria: they walk slowly there and the mobs in glass are shrunk, "
                + "so vanilla barely moves their legs (they glide). auto = matched to their size (1.5 for shrunk mobs, 3 for animals in real size), "
                + "a number from 0.1 to 10 = that factor for all, 0 = like vanilla")
                .define("walkAnimationBoost", "auto", o -> o instanceof String str && org.overengineer.bettacraft.terrarium.WalkAnimation.parse(str) != null);
        LOG_MINIATURE_STATS = b.comment("Write mesh build and sync statistics of tank miniatures to the log (for debugging)").define("logMiniatureStats", false);
        b.pop();
        b.comment("On-screen hints").push("hud");
        INTERIOR_LEAVE_HINT = b.comment("Show the small hint how to leave (sneak + look up) at the bottom left while you are in a tank interior; the progress bar while you hold it is always shown")
                .define("interiorLeaveHint", true);
        b.pop();
        b.comment("Sounds").push("sound");
        FILTER_SOUND = b.comment("Play the filter bubbling sound").define("filterSound", true);
        VOLUME_FILTER = b.comment("Volume of the filters (external, cabinet, internal, corner filter) from 0 to 1").defineInRange("filterVolume", 1.0, 0.0, 1.0);
        VOLUME_AIR_PUMP = b.comment("Volume of the air pump hum from 0 to 1").defineInRange("airPumpVolume", 1.0, 0.0, 1.0);
        VOLUME_AIR_STONE = b.comment("Volume of the air stones and sponge filters from 0 to 1").defineInRange("airStoneVolume", 1.0, 0.0, 1.0);
        VOLUME_FLOW_PUMP = b.comment("Volume of the flow pumps from 0 to 1").defineInRange("flowPumpVolume", 1.0, 0.0, 1.0);
        VOLUME_FEEDER = b.comment("Volume of the feeder clicks from 0 to 1").defineInRange("feederVolume", 1.0, 0.0, 1.0);
        AIR_PUMP_SOUND = b.comment("Play the hum of the air pumps (off by default; its volume is airPumpVolume)").define("airPumpSound", false);
        FLOW_PUMP_SOUND = b.comment("Play the whir of the flow pumps (off by default; its volume is flowPumpVolume)").define("flowPumpSound", false);
        b.pop();
        b.comment("Map mods (M32 part d)").push("map");
        ALARM_WAYPOINTS = b.comment("A waypoint at a tank whose monitor alarm went off, until it clears (JourneyMap; the pack switch",
                "features.compat.journeymap turns it off for everyone)").define("showAlarmWaypoints", true);
        b.pop();
        SPEC = b.build();
    }

    /** Safe accessors (the client config may not be loaded yet, e.g. in unit tests). */
    public static boolean plantMeshes() {
        return CommonConfig.getOr(PLANT_MESHES, true);
    }

    public static boolean swayingPlants() {
        return CommonConfig.getOr(SWAYING_PLANTS, true);
    }

    private static String boostText;
    private static float boostValue = org.overengineer.bettacraft.terrarium.WalkAnimation.AUTO;

    /** M44: the walk animation setting ({@code WalkAnimation.AUTO}, a factor, 0 = vanilla); auto while the config is not loaded. */
    public static float walkAnimationBoost() {
        String t = CommonConfig.getOr(WALK_ANIMATION_BOOST, "auto");
        if (!t.equals(boostText)) {
            Float v = org.overengineer.bettacraft.terrarium.WalkAnimation.parse(t);
            boostValue = v == null ? org.overengineer.bettacraft.terrarium.WalkAnimation.AUTO : v;
            boostText = t;
        }
        return boostValue;
    }

    public static boolean alarmWaypoints() {
        return CommonConfig.getOr(ALARM_WAYPOINTS, true);
    }

    private ClientConfig() {}
}
