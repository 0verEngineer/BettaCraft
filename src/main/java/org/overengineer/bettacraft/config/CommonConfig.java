package org.overengineer.bettacraft.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Settings that must be known before a world loads (spawns, recipe variants). */
public final class CommonConfig {
    public enum FishBagRecipe { AUTO, KELP, PLASTIC }

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue SPAWN_WEIGHT;
    public static final ModConfigSpec.IntValue SPAWN_MIN_GROUP;
    public static final ModConfigSpec.IntValue SPAWN_MAX_GROUP;
    public static final ModConfigSpec.EnumValue<FishBagRecipe> RECIPE_FISH_BAG;
    public static final ModConfigSpec.EnumValue<org.overengineer.bettacraft.compat.info.OverlayProvider.Choice> OVERLAY_PROVIDER;
    public static final ModConfigSpec.BooleanValue FEATURE_PATHFINDING;
    public static final ModConfigSpec.BooleanValue FEATURE_PLANT_MESHES;
    public static final ModConfigSpec.BooleanValue FEATURE_SMOOTH_SWIMMING;
    public static final ModConfigSpec.BooleanValue FEATURE_GREEN_NEON;
    public static final ModConfigSpec.BooleanValue FEATURE_SNAIL_COLORS;
    public static final ModConfigSpec.BooleanValue FEATURE_DETAILED_DECOR;
    public static final ModConfigSpec.BooleanValue FEATURE_BREAK_PROTECTION;
    public static final ModConfigSpec.BooleanValue FEATURE_CLIP_FEEDER;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Natural spawning of wild bettas").push("spawn");
        SPAWN_WEIGHT = b.comment("Spawn weight of wild bettas in #bettacraft:betta_spawn_biomes (swamps); 0 disables natural spawning").defineInRange("weight", 8, 0, 1000);
        SPAWN_MIN_GROUP = b.comment("Smallest group of wild bettas that spawns together").defineInRange("minGroupSize", 1, 1, 16);
        SPAWN_MAX_GROUP = b.comment("Largest group of wild bettas that spawns together").defineInRange("maxGroupSize", 2, 1, 16);
        b.pop();
        b.comment("Recipes").push("recipes");
        RECIPE_FISH_BAG = b.comment("Fish bag recipe: AUTO = plastic if #bettacraft:plastic_sheets is not empty, otherwise dried kelp; KELP or PLASTIC force one")
                .defineEnum("fishBag", FishBagRecipe.AUTO);
        b.pop();
        b.comment("Integration with other mods").push("compat");
        OVERLAY_PROVIDER = b.comment("Which overlay mod shows the Bettacraft lines (tank, betta, filter, culture): AUTO = only the first installed one "
                        + "in the order Jade, WTHIT, The One Probe (no doubled boxes with two overlay mods); JADE, TOP or WTHIT = only that one; ALL = every installed one")
                .defineEnum("overlayProvider", org.overengineer.bettacraft.compat.info.OverlayProvider.Choice.AUTO);
        b.pop();
        b.comment("Switches for single features (M21 on; older features follow in a later version)").push("features");
        FEATURE_PATHFINDING = b.comment("Path finding in the tank interior: dwarf frogs walk around decoration, small animals go under overhangs and into caves, "
                + "plecos swim into their spawning cave (off: straight lines like before)").define("interiorPathfinding", true);
        FEATURE_PLANT_MESHES = b.comment("New look of Rotala, Vallisneria and Anubias in the tank interior (prototype): real leaves that sway in the water "
                + "instead of leaf blocks; the miniature outside keeps the block look (off: the old leaf blocks everywhere)").define("plantMeshes", true);
        // M21.5
        FEATURE_SMOOTH_SWIMMING = b.comment("Smooth swimming: fish turn in curves, tilt and right themselves smoothly, kuhli loaches undulate slowly like eels "
                + "and hide by day, plecos and otocinclus only stand upright once they reach the glass (off: the old, jerkier movement)").define("smoothSwimming", true);
        FEATURE_GREEN_NEON = b.comment("Green neon tetra (Paracheirodon simulans, blue and green form) at the wandering trader and in the aquarium net "
                + "(off: no new ones; fish already caught stay)").define("greenNeonTetra", true);
        FEATURE_SNAIL_COLORS = b.comment("Snail colours: every nerite gets its own random shade (red and yellow racer nerites), apple snails are mostly gold "
                + "with shades and bands, ramshorn snails also come in red (off: one fixed colour per variety, brown apple snails, no new red ramshorns)").define("snailColors", true);
        FEATURE_DETAILED_DECOR = b.comment("Bigger, more detailed dino skull and shipwreck decoration (off: the small old shapes for newly placed ones)").define("detailedDecor", true);
        FEATURE_BREAK_PROTECTION = b.comment("Break protection switch in the tank menu: nobody can break a protected tank, its lid, light, feeders and the cabinet below "
                + "(off: the switch is hidden and does nothing)").define("breakProtection", true);
        FEATURE_CLIP_FEEDER = b.comment("Clip-on feeder: a small automatic feeder that hangs on the side of the glass (for tanks whose top is taken by a light) "
                + "(off: it cannot be placed and placed ones do not feed)").define("clipOnFeeder", true);
        b.pop();
        SPEC = b.build();
    }

    /** Safe accessor (the config may not be loaded yet, e.g. during datagen). */
    public static <T> T getOr(ModConfigSpec.ConfigValue<T> value, T fallback) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }

    public static boolean pathfinding() {
        return getOr(FEATURE_PATHFINDING, true);
    }

    public static boolean plantMeshes() {
        return getOr(FEATURE_PLANT_MESHES, true);
    }

    public static boolean smoothSwimming() {
        return getOr(FEATURE_SMOOTH_SWIMMING, true);
    }

    public static boolean greenNeon() {
        return getOr(FEATURE_GREEN_NEON, true);
    }

    public static boolean snailColors() {
        return getOr(FEATURE_SNAIL_COLORS, true);
    }

    public static boolean detailedDecor() {
        return getOr(FEATURE_DETAILED_DECOR, true);
    }

    public static boolean breakProtection() {
        return getOr(FEATURE_BREAK_PROTECTION, true);
    }

    public static boolean clipFeeder() {
        return getOr(FEATURE_CLIP_FEEDER, true);
    }

    private CommonConfig() {}
}
