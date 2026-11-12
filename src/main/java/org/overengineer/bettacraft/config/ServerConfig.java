package org.overengineer.bettacraft.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Per-world game logic (synced to clients). Keys follow PLAN section 11.
 * Many values are not used yet in M0 but are defined now so the file layout stays stable.
 */
public final class ServerConfig {
    public static final ModConfigSpec SPEC;

    // tank
    public static final ModConfigSpec.IntValue TANK_MAX_SIZE_X;
    public static final ModConfigSpec.IntValue TANK_MAX_SIZE_Y;
    public static final ModConfigSpec.IntValue TANK_MAX_SIZE_Z;
    public static final ModConfigSpec.IntValue TANK_MAX_ENTITIES;
    public static final ModConfigSpec.IntValue TANK_HARD_CAP_PER_WATER_BLOCK;
    public static final ModConfigSpec.IntValue TANK_OVERCROWDED_PER_WATER_BLOCK;
    public static final ModConfigSpec.DoubleValue TANK_MIN_FILL_TO_INSERT;

    // health
    public static final ModConfigSpec.DoubleValue HEALTH_DAYS_UNTIL_SICK;
    public static final ModConfigSpec.DoubleValue HEALTH_DAYS_SICK_UNTIL_DEATH;
    public static final ModConfigSpec.DoubleValue HEALTH_BOTH_BAD_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue HEALTH_RECOVERY_PER_DAY;
    public static final ModConfigSpec.BooleanValue HEALTH_OLD_AGE_ENABLED;
    public static final ModConfigSpec.DoubleValue HEALTH_OLD_AGE_YEARS;
    public static final ModConfigSpec.IntValue HEALTH_OLD_AGE_DAYS_PER_YEAR;

    // stages (in-game days)
    public static final ModConfigSpec.DoubleValue STAGE_EGG_DAYS;
    public static final ModConfigSpec.DoubleValue STAGE_LARVA_DAYS;
    public static final ModConfigSpec.DoubleValue STAGE_FRY_DAYS;
    public static final ModConfigSpec.DoubleValue STAGE_JUVENILE_DAYS;
    public static final ModConfigSpec.DoubleValue STAGE_SUBADULT_DAYS;

    // temperature
    public static final ModConfigSpec.DoubleValue TEMPERATURE_TAU_DAYS;

    // breeding (M2)
    public static final ModConfigSpec.DoubleValue BREEDING_MIN_TEMP;
    public static final ModConfigSpec.DoubleValue BREEDING_MAX_TEMP;
    public static final ModConfigSpec.DoubleValue BREEDING_READY_DAYS;
    public static final ModConfigSpec.DoubleValue BREEDING_NEST_BUILD_DAYS;
    public static final ModConfigSpec.IntValue BREEDING_MIN_EGGS;
    public static final ModConfigSpec.IntValue BREEDING_MAX_EGGS;
    public static final ModConfigSpec.DoubleValue BREEDING_MIN_SURVIVAL;
    public static final ModConfigSpec.DoubleValue BREEDING_MAX_SURVIVAL;
    public static final ModConfigSpec.DoubleValue BREEDING_FEMALE_COOLDOWN_DAYS;
    public static final ModConfigSpec.DoubleValue BREEDING_FEMALE_DAMAGE_PER_DAY;
    public static final ModConfigSpec.DoubleValue BREEDING_MALE_EATS_FRY_PER_DAY;
    public static final ModConfigSpec.DoubleValue BREEDING_MUTATION_CHANCE;

    // combat (M2)
    public static final ModConfigSpec.BooleanValue COMBAT_MALES_FIGHT;
    public static final ModConfigSpec.DoubleValue COMBAT_FIGHT_DAYS;
    public static final ModConfigSpec.DoubleValue COMBAT_PREDATOR_ATTACKS_PER_DAY;
    public static final ModConfigSpec.DoubleValue HEALTH_HEAL_PER_DAY;

    // food (M2)
    public static final ModConfigSpec.IntValue FOOD_FRY_PER_ITEM;
    public static final ModConfigSpec.DoubleValue FOOD_CULTURE_DAYS;

    // interior
    public static final ModConfigSpec.ConfigValue<Integer> INTERIOR_DEFAULT_SCALE;
    public static final ModConfigSpec.BooleanValue INTERIOR_SUBSTRATE_COSTS_BLOCK;
    public static final ModConfigSpec.IntValue INTERIOR_MAX_RENDERED_BLOCKS;
    public static final ModConfigSpec.IntValue INTERIOR_LEAVE_SNEAK_TICKS;
    public static final ModConfigSpec.IntValue INTERIOR_SYNC_RANGE;

    // drops
    public static final ModConfigSpec.DoubleValue DROPS_ARTEMIA_FROM_SAND_CHANCE;

    // misc
    public static final ModConfigSpec.BooleanValue SIMULATE_UNLOADED_TIME;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Aquarium tank (multiblock) settings").push("tank");
        TANK_MAX_SIZE_X = b.comment("Maximum tank size along X in blocks").defineInRange("maxSizeX", 16, 1, 64);
        TANK_MAX_SIZE_Y = b.comment("Maximum tank size along Y in blocks").defineInRange("maxSizeY", 16, 1, 64);
        TANK_MAX_SIZE_Z = b.comment("Maximum tank size along Z in blocks").defineInRange("maxSizeZ", 16, 1, 64);
        TANK_MAX_ENTITIES = b.comment("Maximum number of inhabitant entities per tank").defineInRange("maxEntities", 64, 1, 1024);
        TANK_HARD_CAP_PER_WATER_BLOCK = b.comment("Hard limit of adult animals per water block").defineInRange("hardCapPerWaterBlock", 6, 1, 64);
        TANK_OVERCROWDED_PER_WATER_BLOCK = b.comment("Adults per water block from which health starts to drop").defineInRange("overcrowdedPerWaterBlock", 2, 1, 64);
        TANK_MIN_FILL_TO_INSERT = b.comment("Minimum fill fraction before inhabitants can be inserted").defineInRange("minFillToInsert", 0.5, 0.0, 1.0);
        b.pop();

        b.comment("Health of bettas").push("health");
        HEALTH_DAYS_UNTIL_SICK = b.comment("Days without food or with wrong temperature until a fish gets sick").defineInRange("daysUntilSick", 7.0, 0.1, 1000.0);
        HEALTH_DAYS_SICK_UNTIL_DEATH = b.comment("Days of sickness until death").defineInRange("daysSickUntilDeath", 14.0, 0.1, 1000.0);
        HEALTH_BOTH_BAD_MULTIPLIER = b.comment("Timer speed multiplier when food AND temperature are bad").defineInRange("bothBadMultiplier", 2.0, 1.0, 10.0);
        HEALTH_RECOVERY_PER_DAY = b.comment("Sick days removed per good day").defineInRange("recoveryPerDay", 2.0, 0.0, 100.0);
        b.push("oldAge");
        HEALTH_OLD_AGE_ENABLED = b.comment("Whether fish die of old age").define("enabled", false);
        HEALTH_OLD_AGE_YEARS = b.comment("Lifespan in in-game years").defineInRange("years", 2.0, 0.1, 100.0);
        HEALTH_OLD_AGE_DAYS_PER_YEAR = b.comment("In-game days per year (Serene Seasons default: 96)").defineInRange("daysPerYear", 96, 1, 10000);
        b.pop();
        b.pop();

        b.comment("Water temperature").push("temperature");
        TEMPERATURE_TAU_DAYS = b.comment("Time constant (in-game days) of the exponential approach of the water to its equilibrium temperature")
                .defineInRange("tauDays", 0.1, 0.0, 10.0);
        b.pop();

        b.comment("Breeding in Bettacraft tanks (PLAN 5.4)").push("breeding");
        BREEDING_MIN_TEMP = b.comment("Lowest water temperature for nest building and spawning").defineInRange("minTemp", 26.0, 0.0, 50.0);
        BREEDING_MAX_TEMP = b.comment("Highest water temperature for nest building and spawning").defineInRange("maxTemp", 28.0, 0.0, 50.0);
        BREEDING_READY_DAYS = b.comment("Days a healthy, fed pair needs in breeding conditions before the male starts a nest (live food halves it)")
                .defineInRange("readyDays", 0.25, 0.0, 100.0);
        BREEDING_NEST_BUILD_DAYS = b.comment("Days the male needs to build the bubble nest").defineInRange("nestBuildDays", 0.25, 0.0, 100.0);
        BREEDING_MIN_EGGS = b.comment("Minimum eggs per spawning").defineInRange("minEggs", 8, 1, 64);
        BREEDING_MAX_EGGS = b.comment("Maximum eggs per spawning").defineInRange("maxEggs", 20, 1, 64);
        BREEDING_MIN_SURVIVAL = b.comment("Share of larvae that become fry with poor care").defineInRange("minSurvival", 0.6, 0.0, 1.0);
        BREEDING_MAX_SURVIVAL = b.comment("Share of larvae that become fry with perfect care (temperature, stocking, fed male)").defineInRange("maxSurvival", 0.9, 0.0, 1.0);
        BREEDING_FEMALE_COOLDOWN_DAYS = b.comment("Days before a female can spawn again").defineInRange("femaleCooldownDays", 3.0, 0.0, 100.0);
        BREEDING_FEMALE_DAMAGE_PER_DAY = b.comment("Health points per day a female loses while the male guards the nest and chases her (bettas have 6)")
                .defineInRange("femaleDamagePerDay", 2.0, 0.0, 100.0);
        BREEDING_MALE_EATS_FRY_PER_DAY = b.comment("Chance per day and free-swimming fry that an adult male in the tank eats it")
                .defineInRange("maleEatsFryPerDay", 0.3, 0.0, 10.0);
        BREEDING_MUTATION_CHANCE = b.comment("Chance per allele that it mutates into a random allele").defineInRange("mutationChance", 0.01, 0.0, 1.0);
        b.pop();

        b.comment("Fights and predators (PLAN 5.6, 4.3)").push("combat");
        COMBAT_MALES_FIGHT = b.comment("Adult males in the same tank fight until one dies").define("malesFight", true);
        COMBAT_FIGHT_DAYS = b.comment("In-game days a fight between two males lasts (until the loser dies)").defineInRange("fightDays", 1.0, 0.01, 100.0);
        COMBAT_PREDATOR_ATTACKS_PER_DAY = b.comment("Attacks per in-game day of a predator (#bettacraft:predators) on small inhabitants")
                .defineInRange("predatorAttacksPerDay", 4.0, 0.0, 1000.0);
        HEALTH_HEAL_PER_DAY = b.comment("Health points per day a fed, healthy betta in a tank regains (wounds from fights and chasing)")
                .defineInRange("healPerDay", 2.0, 0.0, 100.0);
        b.pop();

        b.comment("Food").push("food");
        FOOD_FRY_PER_ITEM = b.comment("Fry fed by one portion of fry food (infusoria, artemia nauplii)").defineInRange("fryPerItem", 20, 1, 256);
        FOOD_CULTURE_DAYS = b.comment("Days an infusoria or artemia culture needs to become ready").defineInRange("cultureDays", 1.0, 0.0, 100.0);
        b.pop();

        b.comment("Duration of the life stages in in-game days").push("stages");
        STAGE_EGG_DAYS = b.defineInRange("eggDays", 0.5, 0.01, 100.0);
        STAGE_LARVA_DAYS = b.defineInRange("larvaDays", 0.5, 0.01, 100.0);
        STAGE_FRY_DAYS = b.defineInRange("fryDays", 2.0, 0.01, 100.0);
        STAGE_JUVENILE_DAYS = b.defineInRange("juvenileDays", 2.5, 0.01, 100.0);
        STAGE_SUBADULT_DAYS = b.defineInRange("subadultDays", 3.5, 0.01, 100.0);
        b.pop();

        b.comment("Tank interior dimension").push("interior");
        INTERIOR_DEFAULT_SCALE = b.comment("Interior blocks per tank block for NEW tanks (4, 8 or 16). Existing tanks keep their scale.")
                .defineInList("defaultScale", 16, java.util.Arrays.asList(4, 8, 16)); // not List.of: NPE on null check
        INTERIOR_SUBSTRATE_COSTS_BLOCK = b.comment("Setting a floor texture consumes one block").define("substrateCostsBlock", true);
        INTERIOR_MAX_RENDERED_BLOCKS = b.comment("Maximum number of non-air interior blocks synced/rendered as miniature per tank")
                .defineInRange("maxRenderedBlocks", 262144, 0, 16777216);
        INTERIOR_LEAVE_SNEAK_TICKS = b.comment("Ticks you have to hold sneak to leave the interior").defineInRange("leaveSneakTicks", 60, 10, 1200);
        INTERIOR_SYNC_RANGE = b.comment("Players within this many blocks of a tank receive its miniature").defineInRange("syncRange", 96, 16, 512);
        b.pop();

        b.push("drops");
        DROPS_ARTEMIA_FROM_SAND_CHANCE = b.comment("Chance that breaking sand drops artemia cysts").defineInRange("artemiaFromSandChance", 0.02, 0.0, 1.0);
        b.pop();

        SIMULATE_UNLOADED_TIME = b.comment("If true, time passes for tanks in unloaded chunks (catch-up on load). Default: time stands still.")
                .define("simulateUnloadedTime", false);

        SPEC = b.build();
    }

    /** Safe accessor: config values are unavailable before the server config is loaded (e.g. in GameTests early). */
    public static int defaultScale() {
        try {
            int s = INTERIOR_DEFAULT_SCALE.get();
            return (s == 4 || s == 8 || s == 16) ? s : 16;
        } catch (IllegalStateException e) {
            return 16;
        }
    }

    public static int maxSize(int axis) {
        try {
            return switch (axis) {
                case 0 -> TANK_MAX_SIZE_X.get();
                case 1 -> TANK_MAX_SIZE_Y.get();
                default -> TANK_MAX_SIZE_Z.get();
            };
        } catch (IllegalStateException e) {
            return 16;
        }
    }

    public static <T> T getOr(ModConfigSpec.ConfigValue<T> value, T fallback) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }

    private ServerConfig() {}
}
