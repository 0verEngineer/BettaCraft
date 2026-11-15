package org.overengineer.bettacraft.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Per-world game logic (synced to clients). Keys follow PLAN section 11; the comments are what players and pack
 * makers read in {@code serverconfig/bettacraft-server.toml}.
 */
public final class ServerConfig {
    /** When players get the GuideME handbook (PLAN 9). Without GuideME there is no book and the option is ignored. */
    public enum GiveBook { ON_JOIN, ON_FIRST_CATCH_OR_CRAFT, NEVER }

    /** M20: how the salinity of a tank affects animals of other mods and vanilla fish. */
    public enum ForeignSalinity { LENIENT, REALISTIC }

    /** M16: what a tadpole in a tank does when it would turn into a frog. */
    public enum Tadpoles { LEAVE_AS_FROG, STAY_TADPOLE }
    /** M17: what happens to an animal that grew too big for a tank (inhabitants.maxHitbox) while it lived in it. */
    public enum Outgrown { BAG_IN_FRONT, KEEP }
    /** M24: how often the water must be changed (nitrate rises slowly): EASY 25 % every 20 days, NORMAL every 14, REALISTIC every 7. */
    public enum WaterChangeDifficulty { OFF, EASY, NORMAL, REALISTIC }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue PROTECT_FROM_EXPLOSIONS_AND_PISTONS;

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

    // animals of other mods in tanks (M16)
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> INHABITANTS_ALLOW;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> INHABITANTS_DENY;
    public static final ModConfigSpec.DoubleValue INHABITANTS_MAX_HITBOX;
    public static final ModConfigSpec.BooleanValue INHABITANTS_REFUSE_HOSTILE;
    public static final ModConfigSpec.EnumValue<Tadpoles> INHABITANTS_TADPOLES;
    public static final ModConfigSpec.EnumValue<Outgrown> INHABITANTS_OUTGROWN;

    // health
    public static final ModConfigSpec.DoubleValue HEALTH_DAYS_UNTIL_SICK;
    public static final ModConfigSpec.DoubleValue HEALTH_DAYS_SICK_UNTIL_DEATH;
    public static final ModConfigSpec.DoubleValue HEALTH_BOTH_BAD_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue HEALTH_RECOVERY_PER_DAY;
    public static final ModConfigSpec.DoubleValue HEALTH_HUNGRY_AFTER_DAYS;
    public static final ModConfigSpec.DoubleValue TEMPERATURE_TOLERATED_MIN;
    public static final ModConfigSpec.DoubleValue TEMPERATURE_TOLERATED_MAX;
    public static final ModConfigSpec.DoubleValue BREEDING_CONDITIONING_DAYS;
    public static final ModConfigSpec.DoubleValue FROGS_STARVE_DAYS;
    public static final ModConfigSpec.IntValue INTERIOR_PREPARATION_BUDGET_MS;
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
    public static final ModConfigSpec.ConfigValue<Integer> FOOD_CYST_STACK;
    public static final ModConfigSpec.DoubleValue FOOD_CULTURE_DAYS;

    // interior
    public static final ModConfigSpec.ConfigValue<Integer> INTERIOR_DEFAULT_SCALE;
    public static final ModConfigSpec.BooleanValue INTERIOR_SUBSTRATE_COSTS_BLOCK;
    public static final ModConfigSpec.IntValue INTERIOR_MAX_RENDERED_BLOCKS;
    public static final ModConfigSpec.IntValue INTERIOR_LEAVE_SNEAK_TICKS;
    /** M15: how steeply up a player has to look (degrees above the horizon) while sneaking to leave the interior. */
    public static final ModConfigSpec.IntValue INTERIOR_LEAVE_LOOK_UP;
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
    public static final ModConfigSpec.DoubleValue AQUATIC_GROWTH_SPEED;
    public static final ModConfigSpec.DoubleValue AQUATIC_WASTE_UPTAKE;
    public static final ModConfigSpec.DoubleValue AQUATIC_BREEDING_BONUS;
    public static final ModConfigSpec.DoubleValue AQUATIC_HIDING;
    public static final ModConfigSpec.IntValue AQUATIC_MAX_PER_TANK;
    public static final ModConfigSpec.BooleanValue AQUATIC_SPREAD;
    public static final ModConfigSpec.DoubleValue AQUATIC_FISHING_CHANCE;
    public static final ModConfigSpec.DoubleValue AQUATIC_BOTTLE_CHANCE;
    public static final ModConfigSpec.DoubleValue AQUATIC_MOSS_CHANCE;
    // M22.4: plants from the net and the fishing tables
    public static final ModConfigSpec.DoubleValue AQUATIC_NET_SHARE;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> NET_PLANTS_RIVER, NET_PLANTS_JUNGLE, NET_PLANTS_SWAMP;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> FISHING_PLANTS_JUNGLE, FISHING_PLANTS_RIVER, FISHING_PLANTS_SWAMP,
            FISHING_PLANTS_MANGROVE;
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
    // M26: live food cultures and the live food pump
    public static final ModConfigSpec.DoubleValue LIVE_FOOD_TUBIFEX_DAYS;
    public static final ModConfigSpec.IntValue LIVE_FOOD_TUBIFEX_ROOM;
    public static final ModConfigSpec.DoubleValue LIVE_FOOD_DAPHNIA_DAYS;
    public static final ModConfigSpec.IntValue LIVE_FOOD_DAPHNIA_ROOM;
    public static final ModConfigSpec.DoubleValue LIVE_FOOD_CROWDED_DAYS;
    public static final ModConfigSpec.DoubleValue LIVE_FOOD_TUBIFEX_CONDITIONING;
    public static final ModConfigSpec.DoubleValue LIVE_FOOD_POOR_STRESS;
    public static final ModConfigSpec.DoubleValue LIVE_FOOD_BOTTLE_DAPHNIA_CHANCE;
    public static final ModConfigSpec.IntValue LIVE_FOOD_PUMP_FE;
    // M26 part b: the food maker and the food variety
    public static final ModConfigSpec.IntValue FOOD_MAKER_TICKS;
    public static final ModConfigSpec.IntValue FOOD_MAKER_SPRING_TICKS;
    public static final ModConfigSpec.IntValue FOOD_MAKER_WINDS;
    public static final ModConfigSpec.IntValue FOOD_MAKER_FE;
    public static final ModConfigSpec.DoubleValue DIET_VARIETY_DAYS;
    public static final ModConfigSpec.IntValue DIET_VARIETY_GROUPS;
    public static final ModConfigSpec.DoubleValue DIET_VARIETY_RECOVERY;
    public static final ModConfigSpec.DoubleValue DIET_POOR_FED_DAYS;
    public static final ModConfigSpec.DoubleValue DIET_SHRIMP_FOOD_FACTOR;
    // M27: the monitors
    public static final ModConfigSpec.IntValue MONITOR_RANGE;
    public static final ModConfigSpec.IntValue MONITOR_FE_PER_BLOCK;

    // misc
    public static final ModConfigSpec.BooleanValue SIMULATE_UNLOADED_TIME;

    // M14: shrimp, crayfish, snails, dwarf clawed frogs, algae, stocking
    public static final ModConfigSpec.IntValue CRITTERS_MAX_PER_TANK;
    public static final ModConfigSpec.DoubleValue CRITTERS_SHRIMP_BREEDING;
    public static final ModConfigSpec.DoubleValue CRITTERS_CRAYFISH_BREEDING;
    public static final ModConfigSpec.DoubleValue CRITTERS_SNAIL_BREEDING;
    public static final ModConfigSpec.BooleanValue CRITTERS_BETTAS_HUNT;
    public static final ModConfigSpec.DoubleValue CRITTERS_BIOFILM;
    public static final ModConfigSpec.DoubleValue CRITTERS_NET_CHANCE;
    public static final ModConfigSpec.IntValue CRITTERS_NET_COOLDOWN;
    public static final ModConfigSpec.DoubleValue CRITTERS_MUD_CHANCE;
    public static final ModConfigSpec.DoubleValue FROGS_FOOD_SHARE;
    public static final ModConfigSpec.DoubleValue FROGS_AIR_MINUTES;
    public static final ModConfigSpec.IntValue STOCKING_SNAILS_PER_FISH;
    public static final ModConfigSpec.IntValue STOCKING_SHRIMP_PER_FISH;
    public static final ModConfigSpec.IntValue STOCKING_CRAYFISH_PER_FISH;
    public static final ModConfigSpec.DoubleValue STOCKING_FROGS_PER_FISH;
    // M18: young animals count in fractions of a fish
    public static final ModConfigSpec.DoubleValue STOCKING_EGG;
    public static final ModConfigSpec.DoubleValue STOCKING_LARVA;
    public static final ModConfigSpec.DoubleValue STOCKING_FRY;
    public static final ModConfigSpec.DoubleValue STOCKING_JUVENILE;
    public static final ModConfigSpec.DoubleValue STOCKING_SUBADULT;
    public static final ModConfigSpec.DoubleValue ALGAE_GROWTH_SPEED;
    public static final ModConfigSpec.DoubleValue ALGAE_CLEANER_STRENGTH;

    // M19: tankmates
    public static final ModConfigSpec.DoubleValue TANKMATES_BREEDING_SPEED;
    public static final ModConfigSpec.DoubleValue TANKMATES_CLUTCH_SPEED;
    public static final ModConfigSpec.DoubleValue TANKMATES_GESTATION_DAYS;
    // M21.8 seahorses
    public static final ModConfigSpec.DoubleValue SEAHORSE_MAX_FLOW, SEAHORSE_FEEDINGS, SEAHORSE_FAST_SHARE, SEAHORSE_POUCH_DAYS;
    public static final ModConfigSpec.BooleanValue SEAHORSE_NEED_POSTS, SEAHORSE_HARASSED;

    public static final ModConfigSpec.IntValue TANKMATES_MAX_FRY;
    public static final ModConfigSpec.IntValue TANKMATES_BOX_CAPACITY;
    public static final ModConfigSpec.DoubleValue TANKMATES_FRY_EATEN;
    public static final ModConfigSpec.DoubleValue TANKMATES_EGGS_EATEN;
    public static final ModConfigSpec.BooleanValue TANKMATES_SHOAL_STRESS;
    public static final ModConfigSpec.DoubleValue TANKMATES_STOCKING;
    public static final ModConfigSpec.DoubleValue TANKMATES_BETTA_AGGRESSION;
    public static final ModConfigSpec.DoubleValue TANKMATES_LOACH_SNAILS;
    /** M29: small fish and shrimp an adult angelfish eats per day; how hard cichlid parents drive intruders off their brood. */
    public static final ModConfigSpec.DoubleValue TANKMATES_PREDATION;
    public static final ModConfigSpec.DoubleValue TANKMATES_BROOD_DEFENCE;
    public static final ModConfigSpec.DoubleValue TANKMATES_BOTTOM_SHARE;
    public static final ModConfigSpec.BooleanValue TANKMATES_WATER_CHANGE_COOLS;
    public static final ModConfigSpec.DoubleValue TANKMATES_COOL_CHANGE_SHARE;
    public static final ModConfigSpec.DoubleValue TANKMATES_COOL_CHANGE_DROP;
    public static final ModConfigSpec.DoubleValue TANKMATES_NET_SHARE;

    // M20: salt water and reef tanks
    public static final ModConfigSpec.DoubleValue SALT_SHOCK_SPEED;
    public static final ModConfigSpec.BooleanValue SALT_REFUSE_DEADLY;
    public static final ModConfigSpec.BooleanValue SALT_OCEAN_BUCKETS;
    public static final ModConfigSpec.EnumValue<ForeignSalinity> SALT_FOREIGN;
    public static final ModConfigSpec.BooleanValue SALT_VANILLA_CORALS;
    public static final ModConfigSpec.IntValue REEF_SKIMMER_BLOCKS;
    public static final ModConfigSpec.DoubleValue REEF_LIVE_ROCK_BLOCKS;
    public static final ModConfigSpec.IntValue REEF_PUMP_BLOCKS;
    public static final ModConfigSpec.DoubleValue REEF_CORAL_GROWTH;
    public static final ModConfigSpec.DoubleValue REEF_CALCIUM_PER_STAGE;
    public static final ModConfigSpec.DoubleValue REEF_CALCIUM_DOSE;
    public static final ModConfigSpec.DoubleValue REEF_NET_SHARE;

    // guide (M6)
    public static final ModConfigSpec.EnumValue<GiveBook> GUIDE_GIVE_BOOK;

    // M22.3 wild animals, wild colours, mutations, the magnifier
    public static final ModConfigSpec.DoubleValue WILD_BEAUTIFUL_CHANCE;
    public static final ModConfigSpec.DoubleValue WILD_MUTATION_CHANCE;
    public static final ModConfigSpec.BooleanValue GENES_ALWAYS_VISIBLE;
    public static final ModConfigSpec.IntValue WILD_FISH_NEARBY;
    public static final ModConfigSpec.IntValue WILD_CRITTERS_NEARBY;
    // M28.1: the own spawn pass of the wild animals
    public static final ModConfigSpec.IntValue WILD_PASS_INTERVAL;
    public static final ModConfigSpec.IntValue WILD_PASS_PER_PLAYER;
    // M23: aeration, oxygen, internal filters, lights
    public static final ModConfigSpec.BooleanValue POWER_AIR_PUMP;
    public static final ModConfigSpec.DoubleValue POWER_AIR_PUMP_FE;
    public static final ModConfigSpec.IntValue AIR_OUTLETS_PER_PUMP;
    public static final ModConfigSpec.DoubleValue OXYGEN_AIR_STONE;
    public static final ModConfigSpec.DoubleValue OXYGEN_SPONGE_FILTER;
    public static final ModConfigSpec.DoubleValue OXYGEN_FILTER;
    public static final ModConfigSpec.DoubleValue OXYGEN_FLOW_PUMP;
    public static final ModConfigSpec.DoubleValue OXYGEN_SURFACE;
    public static final ModConfigSpec.DoubleValue OXYGEN_PER_ANIMAL;
    public static final ModConfigSpec.DoubleValue OXYGEN_PLANTS;
    public static final ModConfigSpec.DoubleValue OXYGEN_STRESS_BELOW;
    public static final ModConfigSpec.DoubleValue OXYGEN_LABYRINTH_STRESS_BELOW;
    public static final ModConfigSpec.IntValue FILTER_SPONGE_BLOCKS;
    public static final ModConfigSpec.IntValue FILTER_CORNER_BLOCKS;
    public static final ModConfigSpec.DoubleValue FILTER_FRY_INTAKE;
    public static final ModConfigSpec.DoubleValue LIGHT_PANEL_BOOST;
    public static final ModConfigSpec.DoubleValue LIGHT_HIGH_POWER_BOOST;
    // M24: nitrate and water changes, pipes and pumps, the crucible, the multiblock external filter
    public static final ModConfigSpec.EnumValue<WaterChangeDifficulty> WATER_CHANGE_DIFFICULTY;
    public static final ModConfigSpec.DoubleValue NITRATE_STRESS_FROM;
    public static final ModConfigSpec.DoubleValue NITRATE_LETHAL_FROM;
    public static final ModConfigSpec.DoubleValue NITRATE_PLANT_UPTAKE;
    public static final ModConfigSpec.DoubleValue NITRATE_REFERENCE_STOCKING;
    public static final ModConfigSpec.IntValue PIPE_GLASS_RATE;
    // M25: water chemistry
    public static final ModConfigSpec.DoubleValue CHEM_TDS_PER_DAY;
    public static final ModConfigSpec.DoubleValue CHEM_REMINERAL_PPM;
    public static final ModConfigSpec.DoubleValue CHEM_LEAF_TANNIN;
    public static final ModConfigSpec.DoubleValue CHEM_LEAF_HALF_LIFE;
    public static final ModConfigSpec.DoubleValue CO2_PER_BUBBLE, CO2_STRESS_FROM, CO2_LETHAL_FROM, CO2_BREATH, CO2_PLANT_UPTAKE, CO2_BIO_BUBBLES, CO2_BIO_DAYS,
            CO2_CYLINDER_DAYS;
    public static final ModConfigSpec.IntValue CO2_STATION_TICKS, CO2_STATION_FE, CO2_FOREIGN_MB;
    public static final ModConfigSpec.DoubleValue RO_RATE, RO_BOOST, RO_WASTE, RO_MEMBRANE, RO_PREFILTER;
    // M25 part c: reef dosing
    public static final ModConfigSpec.DoubleValue DOSING_CA_PER_ML, DOSING_KH_PER_ML, DOSING_MG_PER_ML, DOSING_TRACE_PER_ML, DOSING_MG_DROP,
            DOSING_TRACE_USE;
    public static final ModConfigSpec.IntValue DOSING_CANISTER_ML, DOSING_MAX_ML;
    public static final ModConfigSpec.IntValue RO_FE;
    public static final ModConfigSpec.IntValue PIPE_STEEL_RATE;
    public static final ModConfigSpec.IntValue PIPE_PUMP_RATE;
    public static final ModConfigSpec.BooleanValue POWER_PUMPS;
    public static final ModConfigSpec.IntValue POWER_PUMP_FE;
    public static final ModConfigSpec.IntValue CRUCIBLE_TICKS;
    public static final ModConfigSpec.IntValue CRUCIBLE_FE;
    public static final ModConfigSpec.IntValue MEDIA_SPONGE_BLOCKS;
    public static final ModConfigSpec.IntValue MEDIA_CERAMIC_BLOCKS;
    public static final ModConfigSpec.IntValue MEDIA_CARBON_BLOCKS;
    public static final ModConfigSpec.IntValue MEDIA_PEAT_BLOCKS;
    public static final ModConfigSpec.DoubleValue MEDIA_DIRT_PER_DAY;
    public static final ModConfigSpec.IntValue MULTIBLOCK_FILTER_MAX_CHAMBERS;

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
        // M21.5
        PROTECT_FROM_EXPLOSIONS_AND_PISTONS = b.comment("Break protection (switch in the tank menu) also stops explosions and pistons (false: it only stops players breaking the tank)")
                .define("protectFromExplosionsAndPistons", true);
        b.pop();

        b.comment("Which animals live in tanks (M16). Order: deny, allow, tag #bettacraft:tank_blacklist, tag #bettacraft:tank_inhabitants,",
                "bosses (#c:bosses), size, hostile mobs, no water animal").push("inhabitants");
        INHABITANTS_ALLOW = b.comment("Entity ids that always go into tanks, whatever their size: \"modid:name\", \"modid:*\" for all animals of a mod")
                .defineListAllowEmpty("allow", java.util.List.of(), () -> "modid:name", org.overengineer.bettacraft.tank.InhabitantRules::validPattern);
        INHABITANTS_DENY = b.comment("Entity ids that never go into tanks (wins over everything): \"modid:name\", \"modid:*\"")
                .defineListAllowEmpty("deny", java.util.List.of(), () -> "modid:name", org.overengineer.bettacraft.tank.InhabitantRules::validPattern);
        INHABITANTS_MAX_HITBOX = b.comment("Largest hitbox width and height in blocks of an animal in a tank (unless listed in allow or the tag)")
                .defineInRange("maxHitbox", 0.8, 0.1, 4.0);
        INHABITANTS_REFUSE_HOSTILE = b.comment("Refuse hostile mobs (spawn category monster, e.g. the Cataclysm lionfish) unless they are allowed")
                .define("refuseHostile", true);
        INHABITANTS_TADPOLES = b.comment("A tadpole in a tank that grows up: LEAVE_AS_FROG = the frog hops out in front of the tank, STAY_TADPOLE = it",
                        "never grows up while it lives in a tank")
                .defineEnum("tadpoles", Tadpoles.LEAVE_AS_FROG);
        INHABITANTS_OUTGROWN = b.comment("An animal of another mod that grew bigger than maxHitbox while it lived in a tank (a young animal of a big",
                        "species): BAG_IN_FRONT = it is put into a fish bag that drops in front of the tank, KEEP = it stays")
                .defineEnum("outgrown", Outgrown.BAG_IN_FRONT);
        b.pop();

        b.comment("Health of bettas").push("health");
        HEALTH_DAYS_UNTIL_SICK = b.comment("Days without food, or with a wrong temperature or overcrowding, until a betta gets sick").defineInRange("daysUntilSick", 7.0, 0.1, 1000.0);
        HEALTH_DAYS_SICK_UNTIL_DEATH = b.comment("Days of sickness until death").defineInRange("daysSickUntilDeath", 14.0, 0.1, 1000.0);
        HEALTH_HUNGRY_AFTER_DAYS = b.comment("Days after a meal until a betta is hungry again (and eats from the feeder or the player's hand)")
                .defineInRange("hungryAfterDays", 1.0, 0.1, 30.0);
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
        TEMPERATURE_TOLERATED_MIN = b.comment("Lowest water temperature (C) bettas tolerate; colder water stresses them (the best range is breeding.minTemp to maxTemp)")
                .defineInRange("toleratedMin", 24.0, 0.0, 50.0);
        TEMPERATURE_TOLERATED_MAX = b.comment("Highest water temperature (C) bettas tolerate; warmer water stresses them")
                .defineInRange("toleratedMax", 30.0, 0.0, 50.0);
        b.pop();

        b.comment("Breeding in Bettacraft tanks: conditioning, bubble nest, spawning, eggs and fry").push("breeding");
        BREEDING_MIN_TEMP = b.comment("Lowest water temperature for nest building and spawning").defineInRange("minTemp", 26.0, 0.0, 50.0);
        BREEDING_MAX_TEMP = b.comment("Highest water temperature for nest building and spawning").defineInRange("maxTemp", 28.0, 0.0, 50.0);
        BREEDING_CONDITIONING_DAYS = b.comment("Days a betta stays conditioned (ready for breeding sooner) after a portion of live food")
                .defineInRange("conditioningDays", 2.0, 0.0, 100.0);
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
        // M21.8
        FOOD_CYST_STACK = b.comment("How many jars of artemia cysts stack in one slot: 16, 32 or 64 (each jar keeps its 16 uses; only jars with the same uses left stack; "
                + "stacks bigger than a smaller new limit stay as they are)").defineInList("artemiaCystStackSize", 16, java.util.Arrays.asList(16, 32, 64));
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
        INTERIOR_LEAVE_LOOK_UP = b.comment("While sneaking you also have to look up at least this many degrees above the horizon to leave the interior (0 = sneaking alone is enough)")
                .defineInRange("leaveLookUpDegrees", 60, 0, 89);
        INTERIOR_SYNC_RANGE = b.comment("Players within this many blocks of a tank receive its miniature").defineInRange("syncRange", 96, 16, 512);
        INTERIOR_PREPARATION_BUDGET_MS = b.comment("Server time per tick (ms) for building new interiors in the background (more = ready sooner, less = smoother ticks)")
                .defineInRange("preparationBudgetMs", 8, 1, 50);
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

        b.comment("Aquarium plants in the tank interior (M13: Java fern, Anubias, Bucephalandra, Java moss, Cryptocoryne, Amazon sword,",
                "stem plants, Vallisneria, Monte Carlo, tiger lotus). They grow from a cutting into plants of many blocks with light,",
                "nutrients (the waste in the water) and a fitting temperature, also while nobody is in the interior.").push("interiorPlants");
        AQUATIC_GROWTH_SPEED = b.comment("Growth speed of aquarium plants (1 = normal, 0 = they never grow or spread)")
                .defineInRange("plantGrowthSpeed", 1.0, 0.0, 50.0);
        AQUATIC_WASTE_UPTAKE = b.comment("Water quality per in-game day the plants take up per tank block of plant mass (4096 plant blocks",
                        "at scale 16) and water block, while they have light (0 = plants do not clean the water)")
                .defineInRange("plantWasteUptake", 0.5, 0.0, 10.0);
        AQUATIC_BREEDING_BONUS = b.comment("A well planted tank (plants of at least 3% of the water volume) makes a male ready for a nest",
                        "this many times faster (1 = off)")
                .defineInRange("plantBreedingBonus", 1.1, 1.0, 4.0);
        AQUATIC_HIDING = b.comment("Share of the chases after spawning a female escapes by hiding in dense plants (reached at 8% of",
                        "the water volume; 0 = plants give no hiding spots)")
                .defineInRange("hidingSpots", 0.6, 0.0, 1.0);
        AQUATIC_MAX_PER_TANK = b.comment("Most aquarium plants (roots) one tank interior holds; side shoots and runners stop there")
                .defineInRange("maxPlantsPerTank", 48, 1, 1024);
        AQUATIC_SPREAD = b.comment("Whether full-grown stem plants, Vallisneria, Monte Carlo and Cryptocoryne form side shoots and runners")
                .define("plantsSpread", true);
        AQUATIC_FISHING_CHANCE = b.comment("Chance that fishing in a jungle, river or swamp brings up an aquarium plant instead of the catch")
                .defineInRange("plantFishingChance", 0.05, 0.0, 1.0);
        AQUATIC_BOTTLE_CHANCE = b.comment("Chance that filling a glass bottle with swamp water also brings up a cutting (Cryptocoryne,",
                        "Ludwigia, Rotala)")
                .defineInRange("bottleCuttingChance", 0.05, 0.0, 1.0);
        AQUATIC_MOSS_CHANCE = b.comment("Chance that shears on a moss block next to water give Java moss")
                .defineInRange("mossChance", 0.5, 0.0, 1.0);
        // M22.4: no plant only at the wandering trader
        AQUATIC_NET_SHARE = b.comment("Share of the aquarium net's catches in rivers, jungle water and swamps that is an aquarium plant from the",
                        "bottom (times critters.netChance; features.plantNetCatches; 0 = the net brings up no plants)")
                .defineInRange("netPlantShare", org.overengineer.bettacraft.sources.WildTables.NET_PLANT_SHARE, 0.0, 1.0);
        NET_PLANTS_RIVER = plantList(b, "netPlantsRiver", "Plants the net brings up in rivers", org.overengineer.bettacraft.sources.WildTables.NET_PLANTS_RIVER);
        NET_PLANTS_JUNGLE = plantList(b, "netPlantsJungle", "Plants the net brings up in jungle water", org.overengineer.bettacraft.sources.WildTables.NET_PLANTS_JUNGLE);
        NET_PLANTS_SWAMP = plantList(b, "netPlantsSwamp", "Plants the net brings up in swamps", org.overengineer.bettacraft.sources.WildTables.NET_PLANTS_SWAMP);
        FISHING_PLANTS_JUNGLE = plantList(b, "fishingPlantsJungle", "Plants fishing brings up in jungles (plantFishingChance)",
                org.overengineer.bettacraft.sources.WildTables.FISHING_PLANTS_JUNGLE);
        FISHING_PLANTS_RIVER = plantList(b, "fishingPlantsRiver", "Plants fishing brings up in rivers (plantFishingChance)",
                org.overengineer.bettacraft.sources.WildTables.FISHING_PLANTS_RIVER);
        FISHING_PLANTS_SWAMP = plantList(b, "fishingPlantsSwamp", "Plants fishing brings up in swamps (plantFishingChance)",
                org.overengineer.bettacraft.sources.WildTables.FISHING_PLANTS_SWAMP);
        FISHING_PLANTS_MANGROVE = plantList(b, "fishingPlantsMangrove", "Plants fishing brings up in mangrove swamps (plantFishingChance)",
                org.overengineer.bettacraft.sources.WildTables.FISHING_PLANTS_MANGROVE);
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

        // M23
        b.comment("Air pumps, air stones and the oxygen in the water (features.aeration, features.oxygen)").push("aeration");
        POWER_AIR_PUMP = b.comment("Air pumps only run while the tank has enough FE (power.airPumpFePerOutlet for every air stone and sponge filter they",
                "drive; off: they run without power like the filters without power mods)").define("airPumpRequiresEnergy", false);
        POWER_AIR_PUMP_FE = b.comment("FE per tick an air pump needs for each air stone or sponge filter it drives (times its power regulator)")
                .defineInRange("airPumpFePerOutlet", 2.0, 0.0, 10000.0);
        AIR_OUTLETS_PER_PUMP = b.comment("Air stones and sponge filters one air pump drives at full strength (more share its air and bubble less)")
                .defineInRange("outletsPerPump", 4, 1, 64);
        OXYGEN_AIR_STONE = b.comment("Oxygen exchange of one air stone at 100 %, in water blocks per day (like the filters' cleaning: a stone keeps that much water",
                "near saturation)").defineInRange("airStoneExchange", 30.0, 0.0, 10000.0);
        OXYGEN_SPONGE_FILTER = b.comment("Oxygen exchange of one running sponge filter (its air lift), in water blocks per day")
                .defineInRange("spongeFilterExchange", 15.0, 0.0, 10000.0);
        OXYGEN_FILTER = b.comment("Oxygen exchange of one motor filter (its outlet moves the surface), in water blocks per day")
                .defineInRange("filterExchange", 6.0, 0.0, 10000.0);
        OXYGEN_FLOW_PUMP = b.comment("Oxygen exchange of one flow pump, in water blocks per day").defineInRange("flowPumpExchange", 8.0, 0.0, 10000.0);
        OXYGEN_SURFACE = b.comment("Oxygen exchange of the still water surface per day, times top blocks / water blocks")
                .defineInRange("surfaceExchange", 2.0, 0.0, 1000.0);
        OXYGEN_PER_ANIMAL = b.comment("Oxygen one adult fish uses per day, in mg/L for one water block (at 25 °C; warmer water doubles it every 10 °C)")
                .defineInRange("oxygenPerAnimal", 6.0, 0.0, 1000.0);
        OXYGEN_PLANTS = b.comment("Oxygen a densely planted tank makes per day in full light, in mg/L (at night the plants use a quarter of it)")
                .defineInRange("plantOxygen", 8.0, 0.0, 1000.0);
        OXYGEN_STRESS_BELOW = b.comment("Below this oxygen (mg/L) fish, shrimp and crayfish are stressed (snails a little, frogs not at all)")
                .defineInRange("oxygenStressBelow", 3.5, 0.0, 20.0);
        OXYGEN_LABYRINTH_STRESS_BELOW = b.comment("Below this oxygen (mg/L) bettas and dwarf gouramis are stressed (labyrinth fish breathe air at the surface)")
                .defineInRange("labyrinthStressBelow", 1.0, 0.0, 20.0);
        b.pop();
        b.comment("Internal filters (features.moreInternalFilters, features.fryIntake)").push("internalFilters");
        FILTER_SPONGE_BLOCKS = b.comment("Water blocks one sponge filter keeps clean (it needs air from an air pump)")
                .defineInRange("spongeFilterBlocks", 12, 0, 10000);
        FILTER_CORNER_BLOCKS = b.comment("Water blocks one corner filter keeps clean").defineInRange("cornerFilterBlocks", 20, 0, 10000);
        FILTER_FRY_INTAKE = b.comment("Share of the fry a running motor filter without intake sponge sucks in per day (internal, corner and external filters;",
                "the sponge filter never does; 0 = never)").defineInRange("fryIntakePerDay", 0.15, 0.0, 1000.0);
        b.pop();
        b.comment("Light strength of the aquarium lights (features.ledTiers): the light bar gives light 15, the stronger lights more").push("lights");
        LIGHT_PANEL_BOOST = b.comment("Extra light of an LED panel over the light bar (light 15 + this, on the share of the tank it covers)")
                .defineInRange("panelBoost", 2.5, 0.0, 15.0);
        LIGHT_HIGH_POWER_BOOST = b.comment("Extra light of a high-power LED over the light bar (light 15 + this, on the share of the tank it covers)")
                .defineInRange("highPowerBoost", 5.0, 0.0, 15.0);
        b.pop();

        // M24
        b.comment("Water changes (M24): nitrate (dissolved waste) rises slowly even with a good filter; only water changes and plants lower it").push("waterchange");
        WATER_CHANGE_DIFFICULTY = b.comment("How often the water must be changed: OFF = never (no nitrate), EASY = 25 % about every 20 days, NORMAL = 25 % about",
                "every 14 days, REALISTIC = 25 % about every 7 days (for a normally stocked tank without plants; more fish need more, plants less)")
                .defineEnum("difficulty", WaterChangeDifficulty.NORMAL);
        NITRATE_STRESS_FROM = b.comment("From this nitrate (mg/L) the fish breed less, the algae grow faster and the tank log warns (the recommended water",
                "changes keep a normally stocked tank below 40)").defineInRange("nitrateStressFrom", 50.0, 1.0, 10000.0);
        NITRATE_LETHAL_FROM = b.comment("From this nitrate (mg/L) the water counts as bad: the animals get sick and in the end die (only after many weeks",
                "without a water change)").defineInRange("nitrateLethalFrom", 120.0, 1.0, 10000.0);
        NITRATE_PLANT_UPTAKE = b.comment("How much nitrate a densely planted tank in good light takes up, times the nitrate a normally stocked tank makes",
                "(1.2: dense plants keep such a tank clean on their own)").defineInRange("plantUptake", 1.2, 0.0, 100.0);
        NITRATE_REFERENCE_STOCKING = b.comment("Adult fish per water block of a normally stocked tank (more fish make nitrate faster, fewer slower)")
                .defineInRange("referenceFishPerBlock", 0.5, 0.01, 100.0);
        b.pop();
        // M25
        b.comment("Water chemistry (M25, features.waterChemistry): TDS, pH and hardness of the water").push("chemistry");
        CHEM_TDS_PER_DAY = b.comment("TDS (ppm) a normally stocked tank gains per day from food and waste (water changes bring it back towards the normal",
                "water's 250)").defineInRange("tdsPerDay", org.overengineer.bettacraft.chem.WaterChemistry.WASTE_TDS_PER_DAY, 0.0, 1000.0);
        CHEM_REMINERAL_PPM = b.comment("TDS (ppm) one remineralising salt adds to 1000 mB of water").defineInRange("remineraliserPpm", 400.0, 1.0, 100000.0);
        CHEM_LEAF_TANNIN = b.comment("Water blocks one almond leaf colours fully with its tannins (they lower the pH)").defineInRange("leafBlocks", 1.0, 0.01, 1000.0);
        CHEM_LEAF_HALF_LIFE = b.comment("Days until half of the tannins of the almond leaves have faded").defineInRange("leafHalfLifeDays", 7.0, 0.1, 1000.0);
        b.pop();
        b.comment("CO2 dosing (M25, features.co2): bio CO2, gas cylinders and the filling station").push("co2Dosing");
        CO2_PER_BUBBLE = b.comment("mg/L per day one bubble per second gives one block of water (a bigger tank needs more bubbles)")
                .defineInRange("co2PerBubble", 150.0, 0.0, 100000.0);
        CO2_STRESS_FROM = b.comment("CO2 (mg/L) from which the animals are stressed").defineInRange("co2StressFrom", 35.0, 1.0, 200.0);
        CO2_LETHAL_FROM = b.comment("CO2 (mg/L) from which the water is bad for the animals (sick, in the end dead; bettas and gouramis breathe air and",
                "bear a little more, shrimp less)").defineInRange("co2LethalFrom", 50.0, 1.0, 200.0);
        CO2_BREATH = b.comment("mg/L per day one adult animal per water block breathes out").defineInRange("co2Breath", 2.0, 0.0, 1000.0);
        CO2_PLANT_UPTAKE = b.comment("mg/L per day a densely planted tank in strong light takes up").defineInRange("co2PlantUptake", 30.0, 0.0, 10000.0);
        CO2_BIO_BUBBLES = b.comment("Bubbles per second of a bio CO2 bottle at its best (it starts slowly, varies and fades at the end)")
                .defineInRange("bioCo2Bubbles", 1.0, 0.0, 10.0);
        CO2_BIO_DAYS = b.comment("Days a bio CO2 bottle with four sugar ferments (less sugar: shorter)").defineInRange("bioCo2Days", 4.0, 0.1, 1000.0);
        CO2_CYLINDER_DAYS = b.comment("Days a full CO2 cylinder lasts at one bubble per second").defineInRange("cylinderBubbleDays", 60.0, 0.1, 100000.0);
        CO2_STATION_TICKS = b.comment("Ticks a CO2 filling station needs to fill a cylinder from the flue gas of one burning furnace, smoker, blast",
                "furnace or campfire (more burning fires fill faster)").defineInRange("stationTicksPerCylinder", 24000, 20, 10000000);
        CO2_STATION_FE = b.comment("FE per tick of the compressor of the CO2 filling station while it fills (only while a power option is on)")
                .defineInRange("stationFe", 20, 0, 100000);
        CO2_FOREIGN_MB = b.comment("mB of another mod's CO2 gas (fluid tag #bettacraft:co2_gas) for a full cylinder").defineInRange("foreignCo2Mb", 1000, 1, 1000000);
        b.pop();
        b.comment("Reverse osmosis unit (M25, features.reverseOsmosis)").push("osmosis");
        RO_RATE = b.comment("mB of reverse osmosis water per tick without a pressure pump (it simply runs on the water it gets)")
                .defineInRange("roRate", 1.0, 0.01, 10000.0);
        RO_BOOST = b.comment("How much faster the FE pressure pump makes it (only while a power option is on and it has FE)")
                .defineInRange("roPumpBoost", 4.0, 1.0, 100.0);
        RO_FE = b.comment("FE per tick of the pressure pump").defineInRange("roPumpFe", 20, 0, 100000);
        RO_WASTE = b.comment("Parts of waste water for each part of reverse osmosis water (it leaves through a pipe or the drain)")
                .defineInRange("roWastePerPart", 3.0, 0.0, 100.0);
        RO_MEMBRANE = b.comment("Buckets of reverse osmosis water a membrane makes before it is worn out").defineInRange("roMembraneBuckets", 128.0, 1.0, 100000.0);
        RO_PREFILTER = b.comment("Buckets of reverse osmosis water a prefilter cartridge lasts").defineInRange("roPrefilterBuckets", 32.0, 1.0, 100000.0);
        b.pop();
        b.comment("Reef dosing (M25, features.reefDosing): alkalinity, magnesium and trace elements of salt water and the dosing pump").push("dosing");
        DOSING_CA_PER_ML = b.comment("Calcium (mg/L in one water block) one mL of calcium solution adds (a bigger tank needs more)")
                .defineInRange("calciumPerMl", 2.0, 0.0, 10000.0);
        DOSING_KH_PER_ML = b.comment("Alkalinity (dKH in one water block) one mL of alkalinity solution adds (2 mg/L calcium match 0.07 dKH)")
                .defineInRange("alkalinityPerMl", 0.07, 0.0, 1000.0);
        DOSING_MG_PER_ML = b.comment("Magnesium (mg/L in one water block) one mL of magnesium solution adds").defineInRange("magnesiumPerMl", 4.0, 0.0, 10000.0);
        DOSING_TRACE_PER_ML = b.comment("Trace elements (share of the full level in one water block) one mL of trace element solution adds")
                .defineInRange("tracePerMl", 0.15, 0.0, 100.0);
        DOSING_MG_DROP = b.comment("Magnesium (mg/L) a salt water tank loses per day besides what the stony corals use (it falls out with the",
                "calcium; 0 = only the corals use it)").defineInRange("magnesiumDropPerDay", 1.0, 0.0, 1000.0);
        DOSING_TRACE_USE = b.comment("Trace elements (share of the full level) a salt water tank uses up per day").defineInRange("traceUsePerDay", 0.04, 0.0, 1.0);
        DOSING_CANISTER_ML = b.comment("mL of solution in one canister of the dosing pump").defineInRange("canisterMl", 500, 1, 1000000);
        DOSING_MAX_ML = b.comment("Most mL per day one channel of the dosing pump can be set to").defineInRange("maxMlPerDay", 100, 1, 100000);
        b.pop();
        b.comment("Pipes and pumps (M24, features.pipes): they carry water with all its values (temperature, salt, quality, nitrate)").push("pipes");
        PIPE_GLASS_RATE = b.comment("mB per tick a glass pipe network carries").defineInRange("glassPipeRate", 50, 1, 1000000);
        PIPE_STEEL_RATE = b.comment("mB per tick a stainless steel pipe network carries (a network of both kinds carries what its weakest pipe carries)")
                .defineInRange("steelPipeRate", 250, 1, 1000000);
        PIPE_PUMP_RATE = b.comment("mB per tick a water pump moves at 100 %").defineInRange("pumpRate", 200, 1, 1000000);
        POWER_PUMPS = b.comment("Water pumps, the pump head of the multiblock filter, the water change controller and the level sensor need FE",
                "(off: they run without power)").define("pumpsRequireEnergy", false);
        POWER_PUMP_FE = b.comment("FE per tick of a running water pump or filter pump head at 100 % (a water change costs this per 100 mB)")
                .defineInRange("pumpFePerTick", 8, 0, 100000);
        b.pop();
        b.comment("Crucible (M24, features.stainlessSteel): stainless steel from iron, coal, nether quartz and blaze powder").push("crucible");
        CRUCIBLE_TICKS = b.comment("Ticks one melt takes").defineInRange("meltTicks", 200, 1, 100000);
        CRUCIBLE_FE = b.comment("FE per tick the crucible uses instead of fuel while FE is available (power mods)").defineInRange("fePerTick", 20, 0, 100000);
        b.pop();
        b.comment("Multiblock external filter (M24, features.multiblockFilter): water blocks one filter chamber keeps clean with its medium").push("multiblockFilter");
        MEDIA_SPONGE_BLOCKS = b.comment("Water blocks a chamber with a filter sponge (mechanical) keeps clean").defineInRange("spongeBlocks", 12, 0, 10000);
        MEDIA_CERAMIC_BLOCKS = b.comment("Water blocks a chamber with ceramic rings (bacteria) keeps clean").defineInRange("ceramicBlocks", 24, 0, 10000);
        MEDIA_CARBON_BLOCKS = b.comment("Water blocks a chamber with activated carbon keeps clean (it is used up, not cleaned)").defineInRange("carbonBlocks", 16, 0, 10000);
        MEDIA_PEAT_BLOCKS = b.comment("Water blocks a chamber with filter peat keeps clean (peat softens the water later, M25)").defineInRange("peatBlocks", 6, 0, 10000);
        MEDIA_DIRT_PER_DAY = b.comment("How fast the media clog per day in a normally stocked tank (1 = clogged; sponges fastest, ceramic at half the rate)")
                .defineInRange("dirtPerDay", 0.04, 0.0, 100.0);
        MULTIBLOCK_FILTER_MAX_CHAMBERS = b.comment("Most filter chambers one multiblock filter may have").defineInRange("maxChambers", 16, 1, 64);
        b.pop();

        b.comment("Breeding live food: larva tub (mosquito larvae) and artemia hatchery (artemia nauplii)").push("foodBreeding");
        CULTURE_LARVA_DAYS = b.comment("In-game days a larva tub needs for one batch (faster at night, in swamps and in the rain)")
                .defineInRange("larvaTubDays", 2.0, 0.01, 100.0);
        CULTURE_LARVA_YIELD = b.comment("Mosquito larvae per batch (uses one piece of organic matter and 250 mB of water)").defineInRange("larvaTubYield", 3, 1, 16);
        CULTURE_ARTEMIA_DAYS = b.comment("In-game days an artemia hatchery needs for one batch (needs light)").defineInRange("hatcheryDays", 1.0, 0.01, 100.0);
        CULTURE_ARTEMIA_YIELD = b.comment("Bottles of artemia nauplii per batch (uses one use of cysts, 250 mB of salt water and one glass bottle per bottle)")
                .defineInRange("hatcheryYield", 2, 1, 16);
        b.pop();

        b.comment("M26: live food cultures (tubifex dish, daphnia barrel, features.liveFoodCultures) and the live food pump (features.liveFoodPump)")
                .push("liveFood");
        LIVE_FOOD_TUBIFEX_DAYS = b.comment("In-game days a fed tubifex dish needs to grow from a tenth to nine tenths of its room")
                .defineInRange("tubifexDays", 4.0, 0.1, 100.0);
        LIVE_FOOD_TUBIFEX_ROOM = b.comment("Portions of tubifex a full dish holds").defineInRange("tubifexRoom", 8, 2, 64);
        LIVE_FOOD_DAPHNIA_DAYS = b.comment("In-game days a fed and lit daphnia barrel needs to grow from a tenth to nine tenths of its room")
                .defineInRange("daphniaDays", 3.0, 0.1, 100.0);
        LIVE_FOOD_DAPHNIA_ROOM = b.comment("Portions of daphnia a full barrel holds").defineInRange("daphniaRoom", 16, 2, 64);
        LIVE_FOOD_CROWDED_DAYS = b.comment("In-game days a daphnia barrel stays crowded (90 % of its room) before the culture collapses (harvest in time)")
                .defineInRange("crowdedDays", 1.0, 0.05, 100.0);
        LIVE_FOOD_TUBIFEX_CONDITIONING = b.comment("How many times as long as other live food tubifex condition a breeding fish")
                .defineInRange("tubifexConditioning", 2.0, 1.0, 10.0);
        LIVE_FOOD_POOR_STRESS = b.comment("Days of bad-water stress a fish gets from one meal of tubifex from a foul dish (0 = harmless)")
                .defineInRange("poorTubifexStress", 0.5, 0.0, 10.0);
        LIVE_FOOD_BOTTLE_DAPHNIA_CHANCE = b.comment("Chance that a bottle of swamp water brings up a few daphnia as well")
                .defineInRange("bottleDaphniaChance", 0.08, 0.0, 1.0);
        LIVE_FOOD_PUMP_FE = b.comment("FE the live food pump needs per portion while pipes.pumpsRequireEnergy is on")
                .defineInRange("pumpFePerPortion", 200, 0, 100000);
        b.pop();

        b.comment("M26 part b: the food maker (features.foodMaker): a spring wound by hand, or FE while a power option is on")
                .push("foodMaking");
        FOOD_MAKER_TICKS = b.comment("Ticks the food maker needs for one batch").defineInRange("ticksPerBatch", 200, 1, 72000);
        FOOD_MAKER_SPRING_TICKS = b.comment("Ticks of work a fully wound spring gives (24000 = one in-game day)")
                .defineInRange("springTicks", 24000, 20, 1000000);
        FOOD_MAKER_WINDS = b.comment("Sneak + right-clicks with an empty hand that wind the spring fully").defineInRange("windsToFull", 8, 1, 64);
        FOOD_MAKER_FE = b.comment("FE per tick the food maker takes while it works and a power option is on (then it has no spring)")
                .defineInRange("makerFePerTick", 10, 0, 100000);
        b.pop();

        b.comment("M26 part b: food variety (features.foodVariety): food groups, favourite and poor food of each diet (item tags",
                "#bettacraft:diet/<diet>/favourite and /poor)").push("diet");
        DIET_VARIETY_DAYS = b.comment("In-game days a fish remembers what it ate").defineInRange("varietyDays", 3.0, 0.1, 30.0);
        DIET_VARIETY_GROUPS = b.comment("Food groups (pellets, flakes, granules, wafers, frozen, live) a fish must have eaten in that time for a varied diet")
                .defineInRange("varietyGroups", 3, 1, 6);
        DIET_VARIETY_RECOVERY = b.comment("How many times as fast a fish with a varied diet recovers from stress and sickness")
                .defineInRange("varietyRecovery", 1.5, 1.0, 10.0);
        DIET_POOR_FED_DAYS = b.comment("A meal of a food that suits the species badly counts as eaten this many days ago (it is hungry again sooner)")
                .defineInRange("poorFoodFedDays", 0.5, 0.0, 10.0);
        DIET_SHRIMP_FOOD_FACTOR = b.comment("Shrimp food gives the shrimp, crayfish and snails this many times the food of a leftover pellet")
                .defineInRange("shrimpFoodFactor", 2.0, 0.1, 20.0);
        b.pop();

        b.comment("M27: the monitors (features.monitors): screens of monitor blocks that show every tank in range").push("monitor");
        MONITOR_RANGE = b.comment("Blocks from a monitor within which it finds the tanks (nearest point of the tank)")
                .defineInRange("range", 32, 4, 128);
        MONITOR_FE_PER_BLOCK = b.comment("FE per tick of each monitor block while a power option is on (power.* or pipes.pumpsRequireEnergy)")
                .defineInRange("fePerBlock", 1, 0, 10000);
        b.pop();

        b.comment("Shrimp, dwarf crayfish and snails: small animals that live in a tank as data (no entities)").push("invertebrates");
        CRITTERS_MAX_PER_TANK = b.comment("Most shrimp, crayfish and snails one tank holds (they stop breeding there)")
                .defineInRange("maxCrittersPerTank", 200, 1, 2000);
        CRITTERS_SHRIMP_BREEDING = b.comment("Breeding speed of the Neocaridina shrimp (1 = normal, 0 = they never breed)")
                .defineInRange("shrimpBreeding", 1.0, 0.0, 10.0);
        CRITTERS_CRAYFISH_BREEDING = b.comment("Breeding speed of the dwarf crayfish (1 = normal, 0 = they never breed)")
                .defineInRange("crayfishBreeding", 1.0, 0.0, 10.0);
        CRITTERS_SNAIL_BREEDING = b.comment("Breeding speed of the snails that breed in fresh water (apple, ramshorn, bladder, trumpet and",
                        "assassin snails; nerites never breed in fresh water)")
                .defineInRange("snailBreeding", 1.0, 0.0, 10.0);
        CRITTERS_BETTAS_HUNT = b.comment("Whether bettas hunt young shrimp and crayfish (now and then an adult shrimp); plants and Java moss hide them")
                .define("bettasHuntShrimp", true);
        CRITTERS_BIOFILM = b.comment("Food (portions per day) one water block grows on its own as biofilm for the small animals; plants and",
                        "algae add to it, leftovers of the fish food too")
                .defineInRange("biofilmPerWaterBlock", 0.03, 0.0, 10.0);
        CRITTERS_NET_CHANCE = b.comment("Chance that the aquarium net catches something alive in fitting water (river, swamp, mangrove,",
                        "beach, jungle, lush caves); the catch goes into an empty fish bag")
                .defineInRange("netCatchChance", 0.3, 0.0, 1.0);
        CRITTERS_NET_COOLDOWN = b.comment("Ticks the aquarium net needs before the next try in open water")
                .defineInRange("netCooldownTicks", 40, 0, 1200);
        CRITTERS_MUD_CHANCE = b.comment("Chance that a shovel on mud in a swamp turns up a Malaysian trumpet snail (into an empty fish bag)")
                .defineInRange("mudSnailChance", 0.08, 0.0, 1.0);
        b.pop();

        b.comment("Dwarf clawed frogs (entities): they breathe at the surface and are slow eaters").push("frogs");
        FROGS_FOOD_SHARE = b.comment("Chance that a frog gets its share of the food while bettas are in the tank (they snatch it away);",
                        "sneak + right-click with food feeds the frogs directly")
                .defineInRange("frogFoodShareWithBettas", 0.4, 0.0, 1.0);
        FROGS_AIR_MINUTES = b.comment("Real minutes a frog can stay under water before it has to breathe at the surface")
                .defineInRange("frogAirMinutes", 2.0, 0.25, 60.0);
        FROGS_STARVE_DAYS = b.comment("Days without food after which a frog starts to starve (loses health now and then)")
                .defineInRange("frogStarveDays", 4.0, 0.5, 100.0);
        b.pop();

        b.comment("Stocking: how many small animals count as one fish for the stocking limits (tank.hardCapPerWaterBlock ...)").push("stocking");
        STOCKING_SNAILS_PER_FISH = b.comment("Snails that count as one fish").defineInRange("snailsPerFish", 50, 1, 10000);
        STOCKING_SHRIMP_PER_FISH = b.comment("Shrimp that count as one fish").defineInRange("shrimpPerFish", 25, 1, 10000);
        STOCKING_CRAYFISH_PER_FISH = b.comment("Dwarf crayfish that count as one fish (they are twice the size of a shrimp)")
                .defineInRange("crayfishPerFish", 12, 1, 10000);
        STOCKING_FROGS_PER_FISH = b.comment("Dwarf clawed frogs that count as one fish").defineInRange("frogsPerFish", 1.0, 0.1, 100.0);
        STOCKING_EGG = b.comment("M18: how much of a fish one egg in the bubble nest counts").defineInRange("eggStocking", 0.0, 0.0, 1.0);
        STOCKING_LARVA = b.comment("How much of a fish one larva in the bubble nest counts").defineInRange("larvaStocking", 0.0, 0.0, 1.0);
        STOCKING_FRY = b.comment("How much of a fish one fry counts").defineInRange("fryStocking", 0.02, 0.0, 1.0);
        STOCKING_JUVENILE = b.comment("How much of a fish one juvenile counts (also young animals of other mods, and young",
                "shrimp, crayfish and snails in their share of a fish)").defineInRange("juvenileStocking", 0.1, 0.0, 1.0);
        STOCKING_SUBADULT = b.comment("How much of a fish one subadult counts").defineInRange("subadultStocking", 0.3, 0.0, 1.0);
        b.pop();

        b.comment("Green algae on the inside of the glass").push("algae");
        ALGAE_GROWTH_SPEED = b.comment("How fast algae grow on the glass with light and waste (1 face fully green in about 4 days at 0.25;",
                        "0 = no algae)")
                .defineInRange("algaeGrowthSpeed", 0.25, 0.0, 10.0);
        ALGAE_CLEANER_STRENGTH = b.comment("How much algae the snails and shrimp graze (1 = normal: a few nerites keep a tank clean, 0 = they",
                        "eat none)")
                .defineInRange("cleanerStrength", 1.0, 0.0, 10.0);
        b.pop();

        b.comment("M19: tankmate fish (tetras, rasboras, corydoras, plecos, otocinclus, loaches, livebearers, dwarf gourami)").push("tankmates");
        b.comment("Breeding").push("breeding");
        TANKMATES_BREEDING_SPEED = b.comment("How readily the tankmates spawn or get pregnant (1 = normal, 0 = never)")
                .defineInRange("tankmateBreedingSpeed", 1.0, 0.0, 20.0);
        TANKMATES_CLUTCH_SPEED = b.comment("How fast eggs and larvae develop (2 = twice as fast; corydoras eggs need 1.5 days at 1)")
                .defineInRange("clutchSpeed", 1.0, 0.05, 20.0);
        TANKMATES_GESTATION_DAYS = b.comment("Days a guppy, endler or platy female carries her young")
                .defineInRange("gestationDays", 3.0, 0.1, 60.0);
        TANKMATES_MAX_FRY = b.comment("Most fry that swim free from one clutch (the rest do not make it; tank.maxEntities still applies)")
                .defineInRange("maxFryPerClutch", 20, 1, 200);
        TANKMATES_BOX_CAPACITY = b.comment("Young livebearers that fit into a breeding box").defineInRange("breedingBoxCapacity", 20, 1, 200);
        TANKMATES_WATER_CHANGE_COOLS = b.comment("Fresh water has the ambient temperature: a water change cools a heated tank down a little")
                .define("waterChangeCools", true);
        TANKMATES_COOL_CHANGE_SHARE = b.comment("Share of the tank volume of fresh water (within a quarter of a day) that counts as a large water change",
                        "for the corydoras").defineInRange("coolChangeShare", 0.25, 0.01, 1.0);
        TANKMATES_COOL_CHANGE_DROP = b.comment("Degrees the water change must cool the tank by to set the corydoras off")
                .defineInRange("coolChangeDrop", 1.5, 0.0, 20.0);
        b.pop();
        b.comment("Eating and getting along").push("behaviour");
        TANKMATES_FRY_EATEN = b.comment("How fast the adults (bettas and tankmates) hunt the fry of the tankmates; plants, moss and floating",
                        "plants hide them (0 = never)").defineInRange("fryEatenPerDay", 0.5, 0.0, 20.0);
        TANKMATES_EGGS_EATEN = b.comment("How fast the adults eat eggs they can reach (scattered eggs, eggs on the glass); a spawning mop and",
                        "dense plants protect them (0 = never)").defineInRange("eggsEatenPerDay", 1.5, 0.0, 50.0);
        TANKMATES_SHOAL_STRESS = b.comment("Shoaling fish in a group smaller than their species' minimum are stressed (and get sick after a while)")
                .define("shoalStress", true);
        TANKMATES_BETTA_AGGRESSION = b.comment("How hard male bettas go for male guppies and dwarf gouramis (hit points per day; 0 = peaceful)")
                .defineInRange("bettaAggression", 1.0, 0.0, 20.0);
        TANKMATES_LOACH_SNAILS = b.comment("Snails one clown loach eats per day (not the big apple snails; nerites are hard to crack)")
                .defineInRange("clownLoachSnailsPerDay", 0.5, 0.0, 50.0);
        TANKMATES_PREDATION = b.comment("Small fish (neons, embers, mosquito rasboras ...) and shrimp one adult angelfish eats per day; plants hide them",
                "(0 = never)").defineInRange("angelfishPreyPerDay", 0.3, 0.0, 20.0);
        TANKMATES_BROOD_DEFENCE = b.comment("How hard cichlid parents drive other fish away from their eggs and young (hit points per day; 0 = they",
                "only chase)").defineInRange("broodDefence", 1.0, 0.0, 20.0);
        TANKMATES_BOTTOM_SHARE = b.comment("Chance that a bottom fish (corydoras, pleco, otocinclus, loach) gets its share of food that sinks past",
                        "faster fish; algae wafers go to the bottom fish directly").defineInRange("bottomFoodShare", 0.6, 0.0, 1.0);
        TANKMATES_STOCKING = b.comment("Multiplies how much each tankmate counts for the stocking (a neon 0.35, a bristlenose 1.2, a clown loach",
                        "2.5 fish at 1)").defineInRange("tankmateStockingFactor", 1.0, 0.0, 10.0);
        TANKMATES_NET_SHARE = b.comment("Share of the catches of the aquarium net in jungle, swamp and river water that are tankmate fish",
                        "instead of small animals (0 = no fish)").defineInRange("netFishShare", 0.4, 0.0, 1.0);
        b.pop();
        b.pop();

        b.comment("M20: salt water tanks: salinity, protein skimmers, flow pumps, live rock, corals").push("saltwater");
        SALT_SHOCK_SPEED = b.comment("How fast the wrong salinity harms animals (1 = a freshwater fish dies within a few hours in sea water,",
                        "a few ppt too much only stress it; 0 = salinity does no harm)").defineInRange("salinityShock", 1.0, 0.0, 20.0);
        SALT_REFUSE_DEADLY = b.comment("Refuse to put an animal into water whose salinity would kill it (a message tells why)")
                .define("refuseDeadlyWater", true);
        SALT_OCEAN_BUCKETS = b.comment("An empty bucket filled in an ocean or at a beach gives a bucket of sea water (for reef tanks; it pours",
                        "like water)").define("oceanWaterIsSalty", true);
        SALT_FOREIGN = b.comment("Fish and water animals of vanilla and other mods: LENIENT = the salinity does not matter to them,",
                        "REALISTIC = vanilla sea animals (cod, tropical fish, pufferfish, squid) need salt water, axolotls and tadpoles",
                        "fresh water, salmon both; animals of other mods stay lenient").defineEnum("foreignAnimals", ForeignSalinity.LENIENT);
        SALT_VANILLA_CORALS = b.comment("Vanilla corals and coral fans in a tank interior die in fresh water (off: they live in every tank, as",
                        "before)").define("vanillaCoralsNeedSalt", false);
        REEF_SKIMMER_BLOCKS = b.comment("Water blocks one protein skimmer keeps clean in salt water (it does nothing in fresh water)")
                .defineInRange("skimmerBlocks", 48, 0, 4096);
        REEF_LIVE_ROCK_BLOCKS = b.comment("Water blocks that live rock keeps clean in salt water, per tank block of live rock (16x16x16 interior",
                        "blocks at scale 16)").defineInRange("liveRockBlocks", 24.0, 0.0, 4096.0);
        REEF_PUMP_BLOCKS = b.comment("Water blocks one flow pump moves enough for the corals that need strong flow")
                .defineInRange("pumpFlowBlocks", 24, 1, 4096);
        REEF_CORAL_GROWTH = b.comment("How fast the corals grow (1 = normal, 0 = they do not grow)").defineInRange("coralGrowthSpeed", 1.0, 0.0, 20.0);
        REEF_CALCIUM_PER_STAGE = b.comment("Calcium (mg/L in one water block) a stony coral uses per growth stage; in bigger tanks it is shared",
                        "by more water").defineInRange("calciumPerStage", 40.0, 0.0, 10000.0);
        REEF_CALCIUM_DOSE = b.comment("Calcium (mg/L in one water block) one calcium additive adds").defineInRange("calciumPerDose", 200.0, 0.0, 10000.0);
        REEF_NET_SHARE = b.comment("Share of the catches of the aquarium net in warm and lukewarm oceans that are reef fish instead of small",
                        "animals (0 = no fish)").defineInRange("reefNetFishShare", 0.4, 0.0, 1.0);
        b.pop();

        b.comment("M21.8: seahorses (lined and dwarf seahorse)").push("seahorses");
        SEAHORSE_MAX_FLOW = b.comment("Water movement above which seahorses are stressed (1 = the strong flow stony corals need; one external",
                        "filter in a tank of 16 water blocks gives 0.5; 0 = flow never stresses them)").defineInRange("maxFlow", 0.6, 0.0, 3.0);
        SEAHORSE_NEED_POSTS = b.comment("Seahorses are stressed without a hitching post (aquarium plants, corals or decoration that stick up in the",
                        "tank interior) to hold on to with their tail").define("needHitchingPosts", true);
        SEAHORSE_FEEDINGS = b.comment("Feedings a day a seahorse needs (they have no stomach; 1 = like the other fish)")
                .defineInRange("feedingsPerDay", 2.0, 1.0, 8.0);
        SEAHORSE_FAST_SHARE = b.comment("Chance that a slow seahorse gets its share of the food while fast fish are in the tank")
                .defineInRange("foodShareWithFastFish", 0.35, 0.0, 1.0);
        SEAHORSE_HARASSED = b.comment("Fast or pushy reef fish (tangs, damselfish, chromis, clownfish) stress seahorses").define("harassedByFastFish", true);
        SEAHORSE_POUCH_DAYS = b.comment("Days a male seahorse carries the eggs in his brood pouch before the fry are born")
                .defineInRange("pouchDays", 4.0, 0.05, 100.0);
        b.pop();

        b.comment("M22.3: wild animals in the world, their colours, mutations and the magnifier").push("wildlife");
        WILD_BEAUTIFUL_CHANCE = b.comment("Chance that a wild animal (spawned in the world or caught with the net) shows a bred colour openly (pure-bred);",
                        "most are wild-coloured, some carry a bred colour hidden (features.wildColors)")
                .defineInRange("beautifulWildChance", 0.03, 0.0, 1.0);
        WILD_MUTATION_CHANCE = b.comment("Chance per gene and birth that a shrimp, crayfish, snail or a tankmate without its own genetics mutates",
                        "(bettas: breeding.mutationChance, guppies and endlers 0.4 %; features.mutations)")
                .defineInRange("otherMutationChance", 0.003, 0.0, 1.0);
        GENES_ALWAYS_VISIBLE = b.comment("Hidden genes of every animal are always visible, without analysing them with the magnifier")
                .define("genesAlwaysVisible", false);
        WILD_FISH_NEARBY = b.comment("Wild tankmate fish and frogs only spawn while fewer than this many of them live within 32 blocks",
                        "(keeps them rare, in small shoals; features.wildFish)")
                .defineInRange("wildFishNearby", 6, 0, 256);
        WILD_CRITTERS_NEARBY = b.comment("Wild shrimp, crayfish, snails and hermit crabs only spawn while fewer than this many of them live within",
                        "32 blocks (features.wildCritters)")
                .defineInRange("wildCrittersNearby", 8, 0, 256);
        WILD_PASS_INTERVAL = b.comment("Ticks between two tries of the own spawn pass per player (20 ticks = 1 second; features.wildSpawnPass)")
                .defineInRange("spawnPassInterval", 200, 20, 72000);
        WILD_PASS_PER_PLAYER = b.comment("The own spawn pass only spawns while fewer than this many wild animals of the mod live within 48 blocks of",
                        "the player (features.wildSpawnPass; 0 = it spawns nothing)")
                .defineInRange("spawnPassPerPlayer", 10, 0, 256);
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

    /** M22.4: a list of "plant_id=weight" entries (an aquarium plant and its weight), default from a table of {@code WildTables}. */
    private static ModConfigSpec.ConfigValue<java.util.List<? extends String>> plantList(ModConfigSpec.Builder b, String key, String what,
                                                                                        org.overengineer.bettacraft.sources.WeightedTable<org.overengineer.bettacraft.plants.PlantSpecies> def) {
        return b.comment(what + ": entries \"plant_id=weight\" (the item ids of the aquarium plants without \"bettacraft:\");",
                        "an empty list brings up none")
                .defineListAllowEmpty(key, org.overengineer.bettacraft.sources.PlantTables.toList(def), () -> "amazon_sword=10",
                        org.overengineer.bettacraft.sources.PlantTables::validEntry);
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
