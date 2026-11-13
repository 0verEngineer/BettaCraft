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

    private CommonConfig() {}
}
