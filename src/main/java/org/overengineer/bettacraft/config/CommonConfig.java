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
    public static final ModConfigSpec.BooleanValue FEATURE_LIVEBEARER_GENETICS;
    public static final ModConfigSpec.BooleanValue FEATURE_STONE_CABINETS;
    public static final ModConfigSpec.BooleanValue FEATURE_SEAHORSES;
    public static final ModConfigSpec.BooleanValue FEATURE_SWAYING_FOREIGN_PLANTS;
    public static final ModConfigSpec.BooleanValue SWAYING_AUTO;
    // M22.3
    public static final ModConfigSpec.IntValue SPAWN_FISH_WEIGHT;
    public static final ModConfigSpec.IntValue SPAWN_CRITTER_WEIGHT;
    public static final ModConfigSpec.BooleanValue FEATURE_WILD_FISH;
    public static final ModConfigSpec.BooleanValue FEATURE_WILD_CRITTERS;
    public static final ModConfigSpec.BooleanValue FEATURE_WILD_CORALS;
    public static final ModConfigSpec.BooleanValue FEATURE_WILD_COLORS;
    public static final ModConfigSpec.BooleanValue FEATURE_MUTATIONS;
    public static final ModConfigSpec.BooleanValue FEATURE_MAGNIFIER;
    public static final ModConfigSpec.BooleanValue FEATURE_BIG_CONTAINERS;
    public static final ModConfigSpec.BooleanValue FEATURE_ANIMAL_LIST;
    // M22.4
    public static final ModConfigSpec.BooleanValue FEATURE_PLANT_NET_CATCHES;
    public static final ModConfigSpec.BooleanValue FEATURE_CABINET_OUTLET_SIDE;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> SWAYING_EXCLUDE;
    // M23
    public static final ModConfigSpec.BooleanValue FEATURE_OXYGEN;
    public static final ModConfigSpec.BooleanValue FEATURE_AERATION;
    public static final ModConfigSpec.BooleanValue FEATURE_INTERNAL_FILTERS;
    public static final ModConfigSpec.BooleanValue FEATURE_FRY_INTAKE;
    public static final ModConfigSpec.BooleanValue FEATURE_POWER_REGULATOR;
    public static final ModConfigSpec.BooleanValue FEATURE_LED_TIERS;
    public static final ModConfigSpec.BooleanValue FEATURE_LIGHT_TIMER;
    public static final ModConfigSpec.BooleanValue SOUNDS_USE_VANILLA;
    // M24
    public static final ModConfigSpec.BooleanValue FEATURE_STAINLESS_STEEL;
    public static final ModConfigSpec.BooleanValue FEATURE_PIPES;
    public static final ModConfigSpec.BooleanValue FEATURE_INTERIOR_PIPES;
    public static final ModConfigSpec.BooleanValue FEATURE_MULTIBLOCK_FILTER;
    public static final ModConfigSpec.BooleanValue FEATURE_WATER_CHEMISTRY;
    public static final ModConfigSpec.BooleanValue FEATURE_CO2;
    public static final ModConfigSpec.BooleanValue FEATURE_REVERSE_OSMOSIS;
    public static final ModConfigSpec.BooleanValue FEATURE_CARIDINA;
    public static final ModConfigSpec.BooleanValue FEATURE_SULAWESI;
    public static final ModConfigSpec.BooleanValue FEATURE_REEF_DOSING;
    public static final ModConfigSpec.BooleanValue FEATURE_LIVE_FOOD_CULTURES;
    public static final ModConfigSpec.BooleanValue FEATURE_LIVE_FOOD_PUMP;
    public static final ModConfigSpec.BooleanValue FEATURE_FOOD_MAKER;
    public static final ModConfigSpec.BooleanValue FEATURE_FOOD_VARIETY;
    public static final ModConfigSpec.BooleanValue FEATURE_MONITORS;
    public static final ModConfigSpec.BooleanValue FEATURE_MARIMO_ROLLING;
    public static final ModConfigSpec.BooleanValue FEATURE_DECOR_EFFECTS;
    public static final ModConfigSpec.BooleanValue FEATURE_AUTO_WATER_CHANGE;
    public static final ModConfigSpec.BooleanValue FEATURE_TOP_OFF;
    public static final ModConfigSpec.BooleanValue FEATURE_WILD_SPAWN_PASS;
    public static final ModConfigSpec.BooleanValue FEATURE_WILD_PLANTS;
    public static final ModConfigSpec.DoubleValue WILD_PLANT_FREQUENCY;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Natural spawning of wild bettas").push("spawn");
        SPAWN_WEIGHT = b.comment("Spawn weight of wild bettas in #bettacraft:betta_spawn_biomes (swamps); 0 disables natural spawning").defineInRange("weight", 8, 0, 1000);
        SPAWN_MIN_GROUP = b.comment("Smallest group of wild bettas that spawns together").defineInRange("minGroupSize", 1, 1, 16);
        SPAWN_MAX_GROUP = b.comment("Largest group of wild bettas that spawns together").defineInRange("maxGroupSize", 2, 1, 16);
        // M22.3
        SPAWN_FISH_WEIGHT = b.comment("Spawn weight of the wild tankmate fish and dwarf clawed frogs in their biomes, in tenths of their net catch weight",
                "(jungle: tetras, corydoras, plecos, otocinclus; swamps: rasboras, loaches, dwarf gourami, frogs; rivers: livebearers;",
                "warm and lukewarm oceans: reef fish and seahorses); 0 disables them (features.wildFish)").defineInRange("wildFishWeight", 5, 0, 1000);
        SPAWN_CRITTER_WEIGHT = b.comment("Spawn weight of wild shrimp, crayfish, snails and hermit crabs crawling on the bottom of rivers, swamps,",
                "mangroves, beaches, jungle water and warm oceans; 0 disables them (features.wildCritters)").defineInRange("wildCritterWeight", 6, 0, 1000);
        // M28.1
        WILD_PLANT_FREQUENCY = b.comment("How often wild aquarium and floating plants grow in new chunks: a factor on the chance of a group per chunk",
                "(1 = default, about one small group in every two or three chunks with water of their kind; 0 = none; features.wildPlants)")
                .defineInRange("wildPlantFrequency", 1.0, 0.0, 10.0);
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
        FEATURE_PLANT_MESHES = b.comment("New look of the aquarium plants and corals: real leaves, polyps and tentacles that move in the water, inside the tank "
                + "and in the miniature outside (off: the old blocks for everyone; each player can also turn it off for themselves: client render.plantMeshes)")
                .define("plantMeshes", true);
        // M22.1
        FEATURE_SWAYING_FOREIGN_PLANTS = b.comment("Kelp, seagrass, corals (#bettacraft:swaying_plants) and the water plants of other mods sway in the miniature "
                + "outside the tank like inside (off: they stand still; each player can also turn it off: client render.swayingPlants)")
                .define("swayingForeignPlants", true);
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
        // M21.6
        FEATURE_LIVEBEARER_GENETICS = b.comment("Guppy and endler genetics: every guppy and endler has its own genes (tail shape, body and tail colour, "
                + "patterns on the X and Y chromosome) that are inherited at birth, named strains, endler x guppy hybrids "
                + "(off: the old fixed varieties; the genes are kept)").define("livebearerGenetics", true);
        // M21.7
        FEATURE_STONE_CABINETS = b.comment("Stone cabinets: the aquarium cabinet can also be crafted from stone (also of other mods) with the slabs "
                + "of the same stone; it looks like that stone and is mined with a pickaxe (off: no new ones can be crafted; placed ones stay)")
                .define("stoneCabinets", true);
        // M21.8
        FEATURE_SEAHORSES = b.comment("Seahorses (lined seahorse Hippocampus erectus, dwarf seahorse H. zosterae) at the wandering trader and in the "
                + "aquarium net in warm and lukewarm oceans (off: no new ones; seahorses already caught stay)").define("seahorses", true);
        // M22.3
        FEATURE_WILD_FISH = b.comment("Wild tankmate fish, seahorses and dwarf clawed frogs spawn in their biomes, rarely and in small shoals",
                "(off: only the net, the trader and breeding bring them)").define("wildFish", true);
        FEATURE_WILD_CRITTERS = b.comment("Wild shrimp, crayfish, snails and hermit crabs crawl on the bottom of rivers, swamps, mangroves, beaches,",
                "jungle water and warm oceans; caught with the net, a fish bag or a bucket, and they can be let go again (off: they do not",
                "spawn, and let go into the world they are gone like before)").define("wildCritters", true);
        FEATURE_WILD_CORALS = b.comment("Every Bettacraft coral and the bubble tip anemone grow in the warm ocean coral reefs (new chunks); shears",
                "cut a frag off them (off: no new ones in new chunks; corals already there stay)").define("wildCorals", true);
        FEATURE_WILD_COLORS = b.comment("Now and then a wild animal shows a bred colour openly (wildlife.beautifulWildChance)",
                "(off: wild animals are wild-coloured, bred colours only hidden)").define("wildColors", true);
        FEATURE_MUTATIONS = b.comment("Spontaneous mutations also for shrimp, crayfish, snails and the tankmates without their own genetics",
                "(wildlife.otherMutationChance; off: only bettas, guppies and endlers mutate)").define("mutations", true);
        FEATURE_MAGNIFIER = b.comment("Magnifier: the hidden genes of a new animal are unknown until it is analysed in a tank (off: every animal",
                "shows its genes like before)").define("magnifier", true);
        FEATURE_BIG_CONTAINERS = b.comment("Several animals of one species per bucket and fish bag by their size (a bucket: 32 shrimp, 24 rasboras,",
                "1 betta; a bag half of that), shrimp and snails also in a plain bucket (off: one fish per bucket, bags as before)")
                .define("bigContainers", true);
        FEATURE_ANIMAL_LIST = b.comment("List of every single shrimp, crayfish and snail in the tank menu (sex, age, colour density, genes) to take",
                "out exactly that one, and the sex choice for the quick take-out (off: only the kinds like before)").define("animalList", true);
        // M22.4
        FEATURE_PLANT_NET_CATCHES = b.comment("The aquarium net now and then brings up an aquarium plant from the bottom of rivers, jungle water and",
                "swamps (interiorPlants.netPlantShare; off: plants only from fishing, bottles, shears and the trader)").define("plantNetCatches", true);
        FEATURE_CABINET_OUTLET_SIDE = b.comment("Setting in the cabinet menu: on which side of the tank the hose of the external filter in the cabinet",
                "goes up (back, left, right or front); jet, bubbles and the current in the tank follow it, and the current is",
                "stronger in front of the outlet (seahorses, corals) (off: always the back wall, the current evenly like before)")
                .define("cabinetOutletSide", true);
        // M23
        FEATURE_OXYGEN = b.comment("Oxygen as a water value: it drops with warm water, many animals and at night in planted tanks without light, and rises with",
                "air stones, sponge filters, filter outlets and flow pumps; too little stresses the animals (bettas and gouramis breathe air and",
                "hardly mind) (off: no oxygen value, nobody suffers)").define("oxygen", true);
        FEATURE_AERATION = b.comment("Air pump with air stones in the tank interior: bubble columns, more oxygen (off: they cannot be placed and placed ones do",
                "nothing)").define("aeration", true);
        FEATURE_INTERNAL_FILTERS = b.comment("More internal filters: the air-driven sponge filter (gentle, needs an air pump), the corner filter and the intake",
                "sponge for motor filters (off: they cannot be placed or fitted and placed ones do not clean)").define("moreInternalFilters", true);
        FEATURE_FRY_INTAKE = b.comment("Motor filters without an intake sponge suck in the fry of bettas and other fish now and then (filters.fryIntakePerDay)",
                "(off: no fry is sucked in)").define("fryIntake", true);
        FEATURE_POWER_REGULATOR = b.comment("Power regulator 0-100 % on filters, air stones, air pumps and flow pumps (menu or sneak + mouse wheel): less power",
                "cleans and moves less, needs less FE and is quieter (off: everything runs at 100 %)").define("powerRegulator", true);
        FEATURE_LED_TIERS = b.comment("Stronger aquarium lights: the LED panel (2 blocks wide) and the high-power LED (any size); stronger light makes red plants",
                "redder, lets the strong-light plants grow and brings more algae (off: the new lights count like the light bar)").define("ledTiers", true);
        FEATURE_LIGHT_TIMER = b.comment("Dimmer and daylight timer with sunrise and sunset for the lights of a tank (tank menu, tab Devices) (off: lights are on",
                "at full strength like before)").define("lightTimer", true);
        // M24
        FEATURE_STAINLESS_STEEL = b.comment("Crucible and stainless steel (also from the machines of Mekanism, EnderIO, Create and Thermal) (off: the crucible",
                "does not melt)").define("stainlessSteel", true);
        FEATURE_PIPES = b.comment("Glass and stainless steel pipes and the water pump: they carry water with its values between tanks, filters and the",
                "tanks and pipes of other mods (off: pipes carry nothing and pumps stand still)").define("pipes", true);
        FEATURE_INTERIOR_PIPES = b.comment("Bulkhead in the tank floor and pipes, intake strainer, lily pipe, nozzle and spray bar in the tank interior; their",
                "current acts where they sit (off: they cannot be placed and the bulkheads stay closed)").define("interiorPipes", true);
        FEATURE_MULTIBLOCK_FILTER = b.comment("Multiblock external filter from filter chambers, a pump head and connectors, with filter media that clog and",
                "must be cleaned (off: it does not clean)").define("multiblockFilter", true);
        FEATURE_AUTO_WATER_CHANGE = b.comment("Water change controller: changes a part of the water by itself at an interval (off: it does nothing)")
                .define("autoWaterChange", true);
        FEATURE_TOP_OFF = b.comment("Top-off: a level sensor tops off evaporated water from a reservoir or a pipe (off: it does nothing)").define("topOff", true);
        // M28.1
        FEATURE_WILD_SPAWN_PASS = b.comment("Own spawn pass for the wild animals: now and then a few wild fish, frogs or small animals spawn in fitting water near",
                "each player, outside the vanilla water-ambient mob cap that cod, salmon, tropical fish and the fish of other mods fill",
                "(limits: wildlife.spawnPass*; off: only the vanilla spawning)").define("wildSpawnPass", true);
        FEATURE_WILD_PLANTS = b.comment("Every aquarium plant and floating plant grows wild in the waters of its origin (new chunks; jungle water,",
                "swamps, rivers, mangroves, cold lakes, lush caves); shears cut a cutting off them, floating plants are picked up by hand",
                "or with the net (spawn.wildPlantFrequency; off: no new ones in new chunks, plants already there stay)").define("wildPlants", true);
        // M25
        FEATURE_WATER_CHEMISTRY = b.comment("Water chemistry: TDS, pH and hardness from the substrate, peat, almond leaves, wood, food and water changes;",
                "every animal has its range (off: the values stand still and no animal minds them)").define("waterChemistry", true);
        FEATURE_CO2 = b.comment("CO2 dosing: bio CO2, gas cylinders with pressure regulator and solenoid valve, the CO2 filling station; CO2 makes the",
                "plants grow faster, lowers the pH and too much harms the animals (off: the devices give no CO2)").define("co2", true);
        FEATURE_REVERSE_OSMOSIS = b.comment("Reverse osmosis unit: makes reverse osmosis water (nothing dissolved) from water, with a membrane and a prefilter",
                "that wear out (off: it does not run)").define("reverseOsmosis", true);
        FEATURE_CARIDINA = b.comment("Caridina bee shrimp (Crystal Red, Crystal Black, Taiwan bees, Tiger) for soft water, with their banding grades",
                "(off: no new ones from the net, the wild and the trader; those in tanks stay)").define("caridina", true);
        FEATURE_SULAWESI = b.comment("Sulawesi animals: cardinal shrimp and Tylomelania snails for hard, warm water (off: no new ones from the net, the",
                "wild and the trader; those in tanks stay)").define("sulawesi", true);
        FEATURE_REEF_DOSING = b.comment("Reef dosing: alkalinity (KH), magnesium and trace elements in salt water, used up by the stony corals, and the",
                "dosing pump with up to four canisters (off: the values stand still and the pump does not dose)").define("reefDosing", true);
        // M26
        FEATURE_LIVE_FOOD_CULTURES = b.comment("Live food cultures: the tubifex dish (mud, mulm or waste water) and the daphnia barrel (yeast or green water,",
                "light), harvested with the net or by hoppers and pumps (off: the cultures stand still)").define("liveFoodCultures", true);
        FEATURE_LIVE_FOOD_PUMP = b.comment("Live food pump: pumps daphnia, artemia and other live food from a breeder through the pipes into tanks on a",
                "schedule (off: it does not pump and tanks take no live food from pipes)").define("liveFoodPump", true);
        FEATURE_FOOD_MAKER = b.comment("Food maker: makes pellets, flakes, granules, wafers, fry powder, shrimp food and frozen food from vanilla",
                "ingredients and live food, with a wound spring or FE (off: it stands still)").define("foodMaker", true);
        FEATURE_FOOD_VARIETY = b.comment("Food variety: favourite and poor food per species, a varied diet speeds up the recovery and shows in the",
                "colours of the bettas (off: every food is the same)").define("foodVariety", true);
        // M27
        FEATURE_MONITORS = b.comment("Monitors: screens of monitor blocks that show the values of every tank in range, with graphs of the last",
                "days (off: the screens stay dark and record nothing)").define("monitors", true);
        // M28
        FEATURE_MARIMO_ROLLING = b.comment("Marimo balls roll slowly over the tank floor with the current of filters and outlets (off: they stay where they",
                "were put)").define("marimoRolling", true);
        FEATURE_DECOR_EFFECTS = b.comment("Almond leaves and alder cones on the floor of a tank interior give off tannins (lower the pH a little) and feed",
                "the shrimp; clay and terracotta (tubes, caves, dragon stone) hide young shrimp (off: they are only decoration)").define("decorEffects", true);
        b.pop();
        b.comment("Sounds (M23)").push("sounds");
        SOUNDS_USE_VANILLA = b.comment("Use Minecraft's own sounds for the devices (bubble columns, water, beacon hum, clicks) instead of the Bettacraft sounds",
                "of the filters, air pump, air stones, sponge filter, flow pump and feeder").define("useVanilla", false);
        b.pop();
        b.comment("Swaying water plants in the tank miniature (M22.1, features.swayingForeignPlants)").push("swaying");
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

    public static boolean pathfinding() {
        return getOr(FEATURE_PATHFINDING, true);
    }

    public static boolean plantMeshes() {
        return getOr(FEATURE_PLANT_MESHES, true);
    }

    /** M22.1: foreign water plants sway in the miniature. */
    public static boolean swayingForeignPlants() {
        return getOr(FEATURE_SWAYING_FOREIGN_PLANTS, true);
    }

    public static boolean smoothSwimming() {
        return getOr(FEATURE_SMOOTH_SWIMMING, true);
    }

    /** M21.8: seahorses at the trader and in the net. */
    public static boolean seahorses() {
        return getOr(FEATURE_SEAHORSES, true);
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

    public static boolean livebearerGenetics() {
        return getOr(FEATURE_LIVEBEARER_GENETICS, true);
    }

    public static boolean stoneCabinets() {
        return getOr(FEATURE_STONE_CABINETS, true);
    }

    // M22.3
    public static boolean wildFish() { return getOr(FEATURE_WILD_FISH, true); }
    public static boolean wildCritters() { return getOr(FEATURE_WILD_CRITTERS, true); }
    public static boolean wildCorals() { return getOr(FEATURE_WILD_CORALS, true); }
    public static boolean wildColors() { return getOr(FEATURE_WILD_COLORS, true); }
    public static boolean mutations() { return getOr(FEATURE_MUTATIONS, true); }
    public static boolean magnifier() { return getOr(FEATURE_MAGNIFIER, true); }
    public static boolean bigContainers() { return getOr(FEATURE_BIG_CONTAINERS, true); }
    public static boolean animalList() { return getOr(FEATURE_ANIMAL_LIST, true); }
    // M22.4
    public static boolean plantNetCatches() { return getOr(FEATURE_PLANT_NET_CATCHES, true); }
    public static boolean cabinetOutletSide() { return getOr(FEATURE_CABINET_OUTLET_SIDE, true); }

    // M23
    public static boolean oxygen() { return getOr(FEATURE_OXYGEN, true); }
    public static boolean aeration() { return getOr(FEATURE_AERATION, true); }
    public static boolean moreInternalFilters() { return getOr(FEATURE_INTERNAL_FILTERS, true); }
    public static boolean fryIntake() { return getOr(FEATURE_FRY_INTAKE, true); }
    public static boolean powerRegulator() { return getOr(FEATURE_POWER_REGULATOR, true); }
    public static boolean ledTiers() { return getOr(FEATURE_LED_TIERS, true); }
    public static boolean lightTimer() { return getOr(FEATURE_LIGHT_TIMER, true); }
    public static boolean vanillaSounds() { return getOr(SOUNDS_USE_VANILLA, false); }
    // M24
    public static boolean stainlessSteel() { return getOr(FEATURE_STAINLESS_STEEL, true); }
    public static boolean pipes() { return getOr(FEATURE_PIPES, true); }
    public static boolean interiorPipes() { return getOr(FEATURE_INTERIOR_PIPES, true); }
    public static boolean multiblockFilter() { return getOr(FEATURE_MULTIBLOCK_FILTER, true); }
    public static boolean autoWaterChange() { return getOr(FEATURE_AUTO_WATER_CHANGE, true); }
    public static boolean topOff() { return getOr(FEATURE_TOP_OFF, true); }
    public static boolean wildSpawnPass() { return getOr(FEATURE_WILD_SPAWN_PASS, true); } // M28.1
    public static boolean wildPlants() { return getOr(FEATURE_WILD_PLANTS, true); }
    public static double wildPlantFrequency() { return getOr(WILD_PLANT_FREQUENCY, 1.0); }
    // M25
    public static boolean waterChemistry() { return getOr(FEATURE_WATER_CHEMISTRY, true); }
    public static boolean co2() { return getOr(FEATURE_CO2, true); }
    public static boolean reverseOsmosis() { return getOr(FEATURE_REVERSE_OSMOSIS, true); }
    public static boolean caridina() { return getOr(FEATURE_CARIDINA, true); }
    public static boolean sulawesi() { return getOr(FEATURE_SULAWESI, true); }
    public static boolean reefDosing() { return getOr(FEATURE_REEF_DOSING, true); }
    public static boolean liveFoodCultures() { return getOr(FEATURE_LIVE_FOOD_CULTURES, true); }
    public static boolean liveFoodPump() { return getOr(FEATURE_LIVE_FOOD_PUMP, true); }
    public static boolean foodMaker() { return getOr(FEATURE_FOOD_MAKER, true); }
    public static boolean foodVariety() { return getOr(FEATURE_FOOD_VARIETY, true); }
    public static boolean monitors() { return getOr(FEATURE_MONITORS, true); }
    // M28
    public static boolean marimoRolling() { return getOr(FEATURE_MARIMO_ROLLING, true); }
    public static boolean decorEffects() { return getOr(FEATURE_DECOR_EFFECTS, true); }

    private CommonConfig() {}
}
