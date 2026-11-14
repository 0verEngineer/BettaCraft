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
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> SWAYING_EXCLUDE;

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

    private CommonConfig() {}
}
