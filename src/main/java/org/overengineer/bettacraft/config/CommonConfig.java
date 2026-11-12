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

    private CommonConfig() {}
}
