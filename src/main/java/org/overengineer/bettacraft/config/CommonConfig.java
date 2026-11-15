package org.overengineer.bettacraft.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Settings that must be known before a world loads (spawns, recipe variants). */
public final class CommonConfig {
    public enum FishBagRecipe { AUTO, KELP, PLASTIC }

    public static final ModConfigSpec SPEC;
    /** M33: the switch of every feature ({@link Features#enabled} reads it with its parents). */
    public static final java.util.Map<Feature, ModConfigSpec.BooleanValue> FEATURES = new java.util.EnumMap<>(Feature.class);

    public static final ModConfigSpec.IntValue SPAWN_WEIGHT;
    public static final ModConfigSpec.IntValue SPAWN_MIN_GROUP;
    public static final ModConfigSpec.IntValue SPAWN_MAX_GROUP;
    public static final ModConfigSpec.EnumValue<FishBagRecipe> RECIPE_FISH_BAG;
    public static final ModConfigSpec.EnumValue<org.overengineer.bettacraft.compat.info.OverlayProvider.Choice> OVERLAY_PROVIDER;
    public static final ModConfigSpec.BooleanValue SWAYING_AUTO;
    // M22.3
    public static final ModConfigSpec.IntValue SPAWN_FISH_WEIGHT;
    public static final ModConfigSpec.IntValue SPAWN_CRITTER_WEIGHT;
    // M22.4
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> SWAYING_EXCLUDE;
    // M23
    public static final ModConfigSpec.BooleanValue SOUNDS_USE_VANILLA;
    // M30
    public static final ModConfigSpec.IntValue SPAWN_BIG_WEIGHT;
    public static final ModConfigSpec.DoubleValue WILD_PLANT_FREQUENCY;
    // M33 part c
    public static final ModConfigSpec.EnumValue<Preset> PRESET;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> PRESET_KEEP;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        // M33 part c: the presets (Presets): a second set of defaults over the feature switches and the server config
        PRESET = b.comment("Ready-made profile for the whole mod (feature switches here and the game values in the server config of each world):",
                        "CUSTOM = none, every value as in the files; SIMPLE = no water chemistry, CO2 or reverse osmosis, no forced water changes,",
                        "no evaporation, no power, gentler care; REALISTIC = everything on, strict values, the devices need FE, foreign animals",
                        "mind the salt; DECOR = no care (care.mode = DECOR: never hungry or sick, nothing dies), no chemistry, power, algae or",
                        "breeding. A value still at its default follows the preset, a value you changed wins (see presetKeep). Nothing is",
                        "written into the files. After a change in a running game: /reload for the recipes and loot (/bettacraft preset does it all)")
                .defineEnum("preset", Preset.CUSTOM);
        PRESET_KEEP = b.comment("Paths that keep their value from the files although the preset sets another one, also when it is the default",
                        "(e.g. \"features.breeding.enabled\" with the preset DECOR keeps the breeding, \"waterchange.difficulty\" a server value)")
                .defineListAllowEmpty("presetKeep", java.util.List.of(), () -> "features.breeding.enabled", o -> o instanceof String);
        b.comment("Natural spawning of wild bettas").push("spawn");
        SPAWN_WEIGHT = b.comment("Spawn weight of wild bettas in #bettacraft:betta_spawn_biomes (swamps); 0 disables natural spawning").defineInRange("weight", 8, 0, 1000);
        SPAWN_MIN_GROUP = b.comment("Smallest group of wild bettas that spawns together").defineInRange("minGroupSize", 1, 1, 16);
        SPAWN_MAX_GROUP = b.comment("Largest group of wild bettas that spawns together").defineInRange("maxGroupSize", 2, 1, 16);
        // M22.3
        SPAWN_FISH_WEIGHT = b.comment("Spawn weight of the wild tankmate fish and dwarf clawed frogs in their biomes, in tenths of their net catch weight",
                "(jungle: tetras, corydoras, plecos, otocinclus; swamps: rasboras, loaches, dwarf gourami, frogs; rivers: livebearers;",
                "warm and lukewarm oceans: reef fish and seahorses); 0 disables them (features.tankmates.wildSpawns, features.frogs.wildSpawns)").defineInRange("wildFishWeight", 5, 0, 1000);
        SPAWN_CRITTER_WEIGHT = b.comment("Spawn weight of wild shrimp, crayfish, snails and hermit crabs crawling on the bottom of rivers, swamps,",
                "mangroves, beaches, jungle water and warm oceans; 0 disables them (features.invertebrates.wildSpawns)").defineInRange("wildCritterWeight", 6, 0, 1000);
        // M28.1
        WILD_PLANT_FREQUENCY = b.comment("How often wild aquarium and floating plants grow in new chunks: a factor on the chance of a group per chunk",
                "(1 = default, about one small group in every two or three chunks with water of their kind; 0 = none; features.aquaticPlants.wildPlants, features.floatingPlants.wildPlants)")
                .defineInRange("wildPlantFrequency", 1.0, 0.0, 10.0);
        // M30
        SPAWN_BIG_WEIGHT = b.comment("Spawn weight of the wild big animals (giant catfish, sharks, rays) in their biomes per weight point of the species;",
                "they also need deep water and few of their kind nearby (bigAnimalRules.*); 0 disables them (features.bigTanks.bigAnimals)")
                .defineInRange("bigAnimalWeight", 1, 0, 1000);
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
        b.comment("Switches per feature (M33): every group has its switch \"enabled\"; a sub-switch is off when its group (or a feature it",
                "needs) is off. A feature that is off loses its recipes, wandering trader offers, loot, net and fishing catches and wild",
                "spawns; its items and blocks stay registered, so worlds keep everything they have").push("features");
        buildFeatures(b);
        b.pop();
        b.comment("Sounds (M23)").push("sounds");
        SOUNDS_USE_VANILLA = b.comment("Use Minecraft's own sounds for the devices (bubble columns, water, beacon hum, clicks) instead of the Bettacraft sounds",
                "of the filters, air pump, air stones, sponge filter, flow pump and feeder").define("useVanilla", false);
        b.pop();
        b.comment("Swaying water plants in the tank miniature (M22.1, features.visuals.swayingForeignPlants)").push("swaying");
        SWAYING_AUTO = b.comment("Besides #bettacraft:swaying_plants also every other underwater plant (plant material, waterlogged or a water plant, "
                + "drawn as a cross or a plant model) sways (off: only the tag)").define("autoDetect", true);
        SWAYING_EXCLUDE = b.comment("Blocks that never sway: block ids (\"modid:block\"), block tags (\"#modid:tag\") or whole mods (\"modid:*\")")
                .defineListAllowEmpty("exclude", java.util.List.of(), () -> "", o -> o instanceof String);
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

    public static boolean vanillaSounds() { return getOr(SOUNDS_USE_VANILLA, false); }
    public static double wildPlantFrequency() { return getOr(WILD_PLANT_FREQUENCY, 1.0); }


    /** M33: the feature tree in the order of {@link Feature}: sections are pushed as the paths need them. */
    private static void buildFeatures(ModConfigSpec.Builder b) {
        java.util.List<String> open = new java.util.ArrayList<>();
        for (Feature f : Feature.values()) {
            java.util.List<String> parts = java.util.List.of(f.path().split("\\."));
            java.util.List<String> section = parts.subList(0, parts.size() - 1);
            int common = 0;
            while (common < open.size() && common < section.size() && open.get(common).equals(section.get(common))) common++;
            if (open.size() > common) b.pop(open.size() - common);
            while (open.size() > common) open.remove(open.size() - 1);
            for (int i = common; i < section.size(); i++) {
                open.add(section.get(i));
                String path = String.join(".", open);
                b.comment(Feature.SECTIONS.getOrDefault(path, path)).translation("bettacraft.configuration.features." + path).push(section.get(i));
            }
            FEATURES.put(f, b.comment(wrap(f.tooltip())).translation(f.translationKey()).define(parts.get(parts.size() - 1), true));
        }
        if (!open.isEmpty()) b.pop(open.size());
    }

    /** A long comment as lines of about 120 characters. */
    private static String[] wrap(String text) {
        java.util.List<String> out = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String w : text.split(" ")) {
            if (line.length() > 0 && line.length() + 1 + w.length() > 120) {
                out.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(w);
        }
        if (line.length() > 0) out.add(line.toString());
        return out.toArray(String[]::new);
    }

    private CommonConfig() {}
}
