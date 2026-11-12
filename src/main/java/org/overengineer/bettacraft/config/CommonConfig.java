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
        SPAWN_WEIGHT = b.defineInRange("weight", 8, 0, 1000);
        SPAWN_MIN_GROUP = b.defineInRange("minGroupSize", 1, 1, 16);
        SPAWN_MAX_GROUP = b.defineInRange("maxGroupSize", 2, 1, 16);
        b.pop();
        b.push("recipes");
        RECIPE_FISH_BAG = b.comment("Fish bag recipe: AUTO = plastic if #bettacraft:plastic_sheets is not empty, otherwise kelp")
                .defineEnum("fishBag", FishBagRecipe.AUTO);
        b.pop();
        SPEC = b.build();
    }

    private CommonConfig() {}
}
