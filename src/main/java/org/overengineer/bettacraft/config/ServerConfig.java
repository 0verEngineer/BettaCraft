package org.overengineer.bettacraft.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Per-world game logic (synced to clients). Keys follow PLAN section 11; the comments are what players and pack
 * makers read in {@code serverconfig/bettacraft-server.toml}.
 */
public final class ServerConfig {
    /** When players get the GuideME handbook (PLAN 9). Without GuideME there is no book and the option is ignored. */
    public enum GiveBook { ON_JOIN, ON_FIRST_CATCH_OR_CRAFT, NEVER }

    public static final ModConfigSpec SPEC;

    // tank
    public static final ModConfigSpec.IntValue TANK_MAX_SIZE_X;
    public static final ModConfigSpec.IntValue TANK_MAX_SIZE_Y;
    public static final ModConfigSpec.IntValue TANK_MAX_SIZE_Z;
    public static final ModConfigSpec.IntValue TANK_MAX_ENTITIES;
    public static final ModConfigSpec.BooleanValue TANK_FREE_SHAPES;
    public static final ModConfigSpec.IntValue TANK_HARD_CAP_PER_WATER_BLOCK;
    public static final ModConfigSpec.IntValue TANK_OVERCROWDED_PER_WATER_BLOCK;
    public static final ModConfigSpec.DoubleValue TANK_MIN_FILL_TO_INSERT;
    public static final ModConfigSpec.IntValue TANK_WATER_HEADROOM;

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
    public static final ModConfigSpec.DoubleValue BREEDING_MIRROR_BONUS;

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

    // water quality (M9)
    public static final ModConfigSpec.BooleanValue WATER_QUALITY_ENABLED;

    // floating plants (M12)
    public static final ModConfigSpec.DoubleValue PLANTS_GROWTH_SPEED;
    public static final ModConfigSpec.DoubleValue PLANTS_MAX_COVERAGE;
    public static final ModConfigSpec.DoubleValue PLANTS_DENSE_FROM;
    public static final ModConfigSpec.DoubleValue PLANTS_WASTE_UPTAKE;
    public static final ModConfigSpec.DoubleValue PLANTS_BREEDING_BONUS;
    public static final ModConfigSpec.DoubleValue PLANTS_PELLETS_ON_LEAVES;
    public static final ModConfigSpec.IntValue PLANTS_MAX_PER_TANK;
    public static final ModConfigSpec.DoubleValue PLANTS_BOTTLE_CHANCE;
    public static final ModConfigSpec.DoubleValue PLANTS_FISHING_CHANCE;
    public static final ModConfigSpec.DoubleValue WATER_QUALITY_BASE_DECLINE;
    public static final ModConfigSpec.DoubleValue WATER_QUALITY_PER_ANIMAL;
    public static final ModConfigSpec.DoubleValue WATER_QUALITY_PER_OVERFED;
    public static final ModConfigSpec.DoubleValue WATER_QUALITY_FILTER_EFFICIENCY;
    public static final ModConfigSpec.IntValue WATER_QUALITY_EXTERNAL_FILTER_BLOCKS;
    public static final ModConfigSpec.IntValue WATER_QUALITY_INTERNAL_FILTER_BLOCKS;
    public static final ModConfigSpec.DoubleValue WATER_QUALITY_BAD_BELOW;

    // evaporation (M9)
    public static final ModConfigSpec.BooleanValue EVAPORATION_ENABLED;
    public static final ModConfigSpec.DoubleValue EVAPORATION_MB_PER_DAY;

    // power (M9)
    public static final ModConfigSpec.BooleanValue POWER_HEATER;
    public static final ModConfigSpec.BooleanValue POWER_FILTER;
    public static final ModConfigSpec.BooleanValue POWER_LIGHT;
    public static final ModConfigSpec.DoubleValue POWER_HEATER_FE_PER_BLOCK;
    public static final ModConfigSpec.DoubleValue POWER_COOLER_FE_PER_BLOCK;
    public static final ModConfigSpec.IntValue POWER_FILTER_FE;
    public static final ModConfigSpec.IntValue POWER_LIGHT_FE;
    public static final ModConfigSpec.IntValue POWER_BUFFER;

    // food breeding (M9)
    public static final ModConfigSpec.DoubleValue CULTURE_LARVA_DAYS;
    public static final ModConfigSpec.IntValue CULTURE_LARVA_YIELD;
    public static final ModConfigSpec.DoubleValue CULTURE_ARTEMIA_DAYS;
    public static final ModConfigSpec.IntValue CULTURE_ARTEMIA_YIELD;

    // misc
    public static final ModConfigSpec.BooleanValue SIMULATE_UNLOADED_TIME;

    // guide (M6)
    public static final ModConfigSpec.EnumValue<GiveBook> GUIDE_GIVE_BOOK;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Aquarium tank (multiblock) settings").push("tank");
        TANK_MAX_SIZE_X = b.comment("Maximum tank size along X in blocks").defineInRange("maxSizeX", 16, 1, 64);
        TANK_MAX_SIZE_Y = b.comment("Maximum tank size along Y in blocks").defineInRange("maxSizeY", 16, 1, 64);
        TANK_MAX_SIZE_Z = b.comment("Maximum tank size along Z in blocks").defineInRange("maxSizeZ", 16, 1, 64);
        TANK_FREE_SHAPES = b.comment("Allow any face-connected tank shape (L, T, steps ...) within the maximum size; false = cuboids only (as before 1.2)").define("freeShapes", true);
        TANK_MAX_ENTITIES = b.comment("Maximum number of inhabitant entities per tank").defineInRange("maxEntities", 64, 1, 1024);
        TANK_HARD_CAP_PER_WATER_BLOCK = b.comment("Hard limit of adult animals per water block").defineInRange("hardCapPerWaterBlock", 6, 1, 64);
        TANK_OVERCROWDED_PER_WATER_BLOCK = b.comment("Adults per water block from which health starts to drop").defineInRange("overcrowdedPerWaterBlock", 2, 1, 64);
        TANK_MIN_FILL_TO_INSERT = b.comment("Minimum fill fraction before inhabitants can be inserted").defineInRange("minFillToInsert", 0.5, 0.0, 1.0);
        TANK_WATER_HEADROOM = b.comment("Air gap below the top glass of a full tank in 1/16 block (the top layer holds the same water, it only looks less full); 0 = water up to the rim").defineInRange("waterHeadroom", 1, 0, 8);
        b.pop();

        b.comment("Health of bettas").push("health");
        HEALTH_DAYS_UNTIL_SICK = b.comment("Days without food, or with a wrong temperature or overcrowding, until a betta gets sick").defineInRange("daysUntilSick", 7.0, 0.1, 1000.0);
        HEALTH_DAYS_SICK_UNTIL_DEATH = b.comment("Days of sickness until death").defineInRange("daysSickUntilDeath", 14.0, 0.1, 1000.0);
        HEALTH_BOTH_BAD_MULTIPLIER = b.comment("Speed multiplier of all health timers while two or more conditions (food, temperature, stocking) are bad").defineInRange("bothBadMultiplier", 2.0, 1.0, 10.0);
        HEALTH_RECOVERY_PER_DAY = b.comment("Sick days removed per good day").defineInRange("recoveryPerDay", 2.0, 0.0, 100.0);
        b.push("oldAge");
        HEALTH_OLD_AGE_ENABLED = b.comment("Whether bettas die of old age").define("enabled", false);
        HEALTH_OLD_AGE_YEARS = b.comment("Lifespan in in-game years").defineInRange("years", 2.0, 0.1, 100.0);
        HEALTH_OLD_AGE_DAYS_PER_YEAR = b.comment("In-game days per year (Serene Seasons default: 96)").defineInRange("daysPerYear", 96, 1, 10000);
        b.pop();
        b.pop();

        b.comment("Water temperature").push("temperature");
        TEMPERATURE_TAU_DAYS = b.comment("Time constant (in-game days) of the exponential approach of the water to its equilibrium temperature")
                .defineInRange("tauDays", 0.1, 0.0, 10.0);
        b.pop();

        b.comment("Breeding in Bettacraft tanks: conditioning, bubble nest, spawning, eggs and fry").push("breeding");
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
        BREEDING_MIRROR_BONUS = b.comment("Mirror training: a male that flared at a mirror stick during the last in-game day gets ready for a nest this many times faster (1 = no bonus, the mirror is then purely cosmetic)")
                .defineInRange("mirrorReadinessBonus", 1.25, 1.0, 4.0);
        b.pop();

        b.comment("Fights between males and predators (#bettacraft:predators) in tanks").push("combat");
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
        STAGE_EGG_DAYS = b.comment("Egg in the bubble nest").defineInRange("eggDays", 0.5, 0.01, 100.0);
        STAGE_LARVA_DAYS = b.comment("Larva hanging in the bubble nest").defineInRange("larvaDays", 0.5, 0.01, 100.0);
        STAGE_FRY_DAYS = b.comment("Free-swimming fry (needs fry food)").defineInRange("fryDays", 2.0, 0.01, 100.0);
        STAGE_JUVENILE_DAYS = b.comment("Juvenile").defineInRange("juvenileDays", 2.5, 0.01, 100.0);
        STAGE_SUBADULT_DAYS = b.comment("Subadult, then the betta is adult").defineInRange("subadultDays", 3.5, 0.01, 100.0);
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

        b.comment("Drops").push("drops");
        DROPS_ARTEMIA_FROM_SAND_CHANCE = b.comment("Chance that breaking sand (#bettacraft:artemia_sand) drops artemia cysts").defineInRange("artemiaFromSandChance", 0.02, 0.0, 1.0);
        b.pop();

        b.comment("Water quality of tanks: waste of the animals and of uneaten food lowers it, filters slow that down, fresh water",
                "(a water change: drain buckets, pour fresh water in) brings it back. 100% = clean water.").push("waterQuality");
        WATER_QUALITY_ENABLED = b.comment("Whether the water quality changes at all and stresses bettas when it is bad").define("qualityEnabled", true);
        WATER_QUALITY_BASE_DECLINE = b.comment("Water quality lost per in-game day in every tank, even without animals (0.01 = 1%)")
                .defineInRange("baseDeclinePerDay", 0.01, 0.0, 1.0);
        WATER_QUALITY_PER_ANIMAL = b.comment("Additional water quality lost per in-game day for each animal per water block",
                        "(two bettas in four water blocks: 0.5 x this value)")
                .defineInRange("declinePerAnimalPerDay", 0.04, 0.0, 10.0);
        WATER_QUALITY_PER_OVERFED = b.comment("Water quality lost per water block for each portion a fish gets that is not hungry",
                        "(fed again within half a day: the food rots)")
                .defineInRange("declinePerOverfedPortion", 0.01, 0.0, 1.0);
        WATER_QUALITY_FILTER_EFFICIENCY = b.comment("Share of the waste that a filter removes when it is large enough for the tank",
                        "(smaller filters remove a matching part of it)")
                .defineInRange("filterEfficiency", 0.8, 0.0, 1.0);
        WATER_QUALITY_EXTERNAL_FILTER_BLOCKS = b.comment("Water blocks one external filter (block or in a cabinet) keeps clean")
                .defineInRange("externalFilterBlocks", 32, 0, 4096);
        WATER_QUALITY_INTERNAL_FILTER_BLOCKS = b.comment("Water blocks one internal filter (in the interior) keeps clean")
                .defineInRange("internalFilterBlocks", 16, 0, 4096);
        WATER_QUALITY_BAD_BELOW = b.comment("Below this water quality bettas are stressed (like by a wrong temperature)")
                .defineInRange("badBelow", 0.35, 0.0, 1.0);
        b.pop();

        b.comment("Floating plants on the water surface of tanks (duckweed, Amazon frogbit, salvinia, red root floater; lily pads stay",
                "as they are). With light and nutrients (the waste in the water) they grow, form offshoots and take up waste.").push("floatingPlants");
        PLANTS_GROWTH_SPEED = b.comment("Growth speed of floating plants (1 = normal, 0 = they never grow or multiply)")
                .defineInRange("growthSpeed", 1.0, 0.0, 20.0);
        PLANTS_MAX_COVERAGE = b.comment("Share of the water surface covered by leaves above which no new offshoots form (0.6 = 60%)")
                .defineInRange("maxCoverage", 0.6, 0.0, 1.0);
        PLANTS_DENSE_FROM = b.comment("From this coverage the cover is dense: the menu warns, the plants shade each other (slower growth)",
                        "and some pellets of the feeder stay on the leaves")
                .defineInRange("denseCoverFrom", 0.5, 0.0, 1.0);
        PLANTS_WASTE_UPTAKE = b.comment("Water quality per in-game day the plants take up for each block of leaf area per water block,",
                        "while they have light (0 = plants do not clean the water)")
                .defineInRange("wasteUptake", 0.04, 0.0, 1.0);
        PLANTS_BREEDING_BONUS = b.comment("Floating plants (at least 10% cover) make a male ready for a nest this many times faster (1 = off)")
                .defineInRange("breedingBonus", 1.15, 1.0, 4.0);
        PLANTS_PELLETS_ON_LEAVES = b.comment("Share of the feeder's food that stays on the leaves under a full cover (rots in the water;",
                        "starts at denseCoverFrom, 0 = off)")
                .defineInRange("pelletsOnLeaves", 0.25, 0.0, 1.0);
        PLANTS_MAX_PER_TANK = b.comment("Most floating plants one tank holds").defineInRange("maxPerTank", 256, 1, 4096);
        PLANTS_BOTTLE_CHANCE = b.comment("Chance that filling a glass bottle with swamp water also brings up duckweed")
                .defineInRange("bottleDuckweedChance", 0.15, 0.0, 1.0);
        PLANTS_FISHING_CHANCE = b.comment("Chance that fishing in a swamp or mangrove swamp brings up a floating plant instead of the catch")
                .defineInRange("fishingChance", 0.06, 0.0, 1.0);
        b.pop();

        b.comment("Evaporation: open tanks slowly lose water. A lid, the feeder or a light on a top block covers that block.").push("evaporation");
        EVAPORATION_ENABLED = b.comment("Whether open tanks lose water").define("evaporationEnabled", true);
        EVAPORATION_MB_PER_DAY = b.comment("Water (mB) one uncovered top block loses per in-game day at 25 C (warmer water evaporates faster)")
                .defineInRange("mbPerBlockPerDay", 20.0, 0.0, 1000.0);
        b.pop();

        b.comment("Power: heaters, coolers, filters and lights can need Forge Energy (FE). A tank stores FE for its equipment;",
                "connect any FE cable or generator to a tank block, an external filter, a cabinet under the tank or a light on it.").push("power");
        POWER_HEATER = b.comment("Heaters and coolers only work while the tank has enough FE").define("heaterRequiresEnergy", false);
        POWER_FILTER = b.comment("Filters only clean the water while the tank has enough FE").define("filterRequiresEnergy", false);
        POWER_LIGHT = b.comment("Lights on a tank only shine while the tank has enough FE").define("lightRequiresEnergy", false);
        POWER_HEATER_FE_PER_BLOCK = b.comment("FE per tick a heater needs for each water block it heats (only while it heats;",
                        "heating water costs far more than running a filter or a light)")
                .defineInRange("heaterFePerBlock", 8.0, 0.0, 1000.0);
        POWER_COOLER_FE_PER_BLOCK = b.comment("FE per tick a cooler needs for each water block it cools (only while it cools)")
                .defineInRange("coolerFePerBlock", 8.0, 0.0, 1000.0);
        POWER_FILTER_FE = b.comment("FE per tick of each filter").defineInRange("filterFePerTick", 10, 0, 100000);
        POWER_LIGHT_FE = b.comment("FE per tick of each lit light block on a tank").defineInRange("lightFePerTick", 4, 0, 100000);
        POWER_BUFFER = b.comment("FE a tank can store").defineInRange("bufferFe", 100000, 1000, 100000000);
        b.pop();

        b.comment("Breeding live food: larva tub (mosquito larvae) and artemia hatchery (artemia nauplii)").push("foodBreeding");
        CULTURE_LARVA_DAYS = b.comment("In-game days a larva tub needs for one batch (faster at night, in swamps and in the rain)")
                .defineInRange("larvaTubDays", 2.0, 0.01, 100.0);
        CULTURE_LARVA_YIELD = b.comment("Mosquito larvae per batch (uses one piece of organic matter and 250 mB of water)").defineInRange("larvaTubYield", 3, 1, 64);
        CULTURE_ARTEMIA_DAYS = b.comment("In-game days an artemia hatchery needs for one batch (needs light)").defineInRange("hatcheryDays", 1.0, 0.01, 100.0);
        CULTURE_ARTEMIA_YIELD = b.comment("Bottles of artemia nauplii per batch (uses one use of cysts, 250 mB of salt water and one glass bottle per bottle)")
                .defineInRange("hatcheryYield", 2, 1, 64);
        b.pop();

        b.comment("Handbook (needs the GuideME mod; without it there is no book)").push("guide");
        GUIDE_GIVE_BOOK = b.comment("When each player gets the handbook, once: ON_JOIN = on the first join, ON_FIRST_CATCH_OR_CRAFT = with the",
                        "first caught betta or the first crafted Bettacraft item, NEVER = not at all (craft it from a book and betta pellets)")
                .defineEnum("giveBook", GiveBook.ON_FIRST_CATCH_OR_CRAFT);
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
