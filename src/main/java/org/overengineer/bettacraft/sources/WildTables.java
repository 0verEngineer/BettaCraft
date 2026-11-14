package org.overengineer.bettacraft.sources;

import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;

/**
 * M22.2: what the wild gives, as data. The fishing loot modifiers, the swamp water bottle and the net on a reef pick
 * from these tables; {@link Sources} turns them (with the config defaults) into the "Obtaining" sections of the handbook.
 * Later milestones add their sources here.
 */
public final class WildTables {
    private WildTables() {}

    /**
     * Fishing in jungles and rivers (M13): the epiphytes, "Blue" and "Brownie Red" rarely. M22.4: the base of the jungle
     * and the river table ({@link #FISHING_PLANTS_JUNGLE}, {@link #FISHING_PLANTS_RIVER}).
     */
    public static final WeightedTable<PlantSpecies> FISHING_PLANTS_TROPICAL = WeightedTable.<PlantSpecies>builder()
            .add(PlantSpecies.JAVA_FERN, 30)
            .add(PlantSpecies.ANUBIAS_NANA, 30)
            .add(PlantSpecies.BUCE_GREEN_WAVY, 15)
            .add(PlantSpecies.BUCE_MINI_NEEDLE, 13)
            .add(PlantSpecies.BUCE_BLUE, 6)
            .add(PlantSpecies.BUCE_BROWNIE_RED, 6)
            .build();

    /**
     * A cutting in a bottle of swamp water (M13), and fishing in swamps and mangrove swamps before M22.4. M22.4: fishing in a
     * mangrove swamp still brings exactly these (crypts take brackish water), the swamp more ({@link #FISHING_PLANTS_SWAMP}).
     */
    public static final WeightedTable<PlantSpecies> BOTTLE_CUTTINGS = WeightedTable.<PlantSpecies>builder()
            .add(PlantSpecies.CRYPT_GREEN, 35)
            .add(PlantSpecies.CRYPT_BROWN, 25)
            .add(PlantSpecies.LUDWIGIA, 20)
            .add(PlantSpecies.ROTALA, 20)
            .build();

    /** M22.4: fishing in jungle water: the epiphytes and, rarely, the Amazon sword (South America) and the red tiger lotus. */
    public static final WeightedTable<PlantSpecies> FISHING_PLANTS_JUNGLE = WeightedTable.<PlantSpecies>builder()
            .addAll(FISHING_PLANTS_TROPICAL)
            .add(PlantSpecies.AMAZON_SWORD, 10)
            .add(PlantSpecies.TIGER_LOTUS, 5)
            // M28: the Anubias of the West African rainforest streams; "White" and "Pinto" rarely
            .add(PlantSpecies.ANUBIAS_PETITE, 6)
            .add(PlantSpecies.ANUBIAS_COFFEEFOLIA, 5)
            .add(PlantSpecies.ANUBIAS_ROUND_LEAF, 5)
            .add(PlantSpecies.ANUBIAS_WHITE, 2)
            .add(PlantSpecies.ANUBIAS_PINTO, 2)
            // M28 part b: the red sword and Staurogyne of South America, the green lotus of tropical Africa
            .add(PlantSpecies.ECHINODORUS_RED_DIAMOND, 4)
            .add(PlantSpecies.NYMPHAEA_GREEN, 4)
            .add(PlantSpecies.STAUROGYNE, 3)
            .build();

    /** M22.4: fishing in rivers: the epiphytes and, rarely, Vallisneria (river ribbons) and Monte Carlo from the banks. */
    public static final WeightedTable<PlantSpecies> FISHING_PLANTS_RIVER = WeightedTable.<PlantSpecies>builder()
            .addAll(FISHING_PLANTS_TROPICAL)
            .add(PlantSpecies.VALLISNERIA, 10)
            .add(PlantSpecies.MONTE_CARLO, 4)
            // M28: the Bucephalandras of the Borneo streams (the pink and white ones rarely), Hygrophila pinnatifida and
            // Downoi from the rocks of Indian and Thai streams
            .add(PlantSpecies.BUCE_KEDAGANG, 4)
            .add(PlantSpecies.BUCE_THEIA, 4)
            .add(PlantSpecies.BUCE_GODZILLA, 3)
            .add(PlantSpecies.BUCE_PINK_LADY, 2)
            .add(PlantSpecies.BUCE_MINI_PINK, 2)
            .add(PlantSpecies.BUCE_SNOW_WHITE, 1)
            .add(PlantSpecies.HYGROPHILA_PINNATIFIDA, 4)
            .add(PlantSpecies.POGOSTEMON_DOWNOI, 3)
            // M28 part b: Sagittaria and the parva from the banks, Aponogeton (Madagascar), the mosses on the stones and
            // rarely Hemianthus "Cuba" and a Marimo ball
            .add(PlantSpecies.SAGITTARIA, 4)
            .add(PlantSpecies.CRYPT_PARVA, 3)
            .add(PlantSpecies.APONOGETON, 3)
            .add(PlantSpecies.CHRISTMAS_MOSS, 3)
            .add(PlantSpecies.FLAME_MOSS, 3)
            .add(PlantSpecies.HC_CUBA, 1)
            .add(PlantSpecies.MARIMO, 1)
            .build();

    /**
     * M22.4: fishing in swamps: the crypts and stem plants of M13 and, rarely, Alternanthera, the red tiger lotus and the
     * pink Cryptocoryne "Flamingo" (a rare form of the wendtii).
     */
    public static final WeightedTable<PlantSpecies> FISHING_PLANTS_SWAMP = WeightedTable.<PlantSpecies>builder()
            .addAll(BOTTLE_CUTTINGS)
            .add(PlantSpecies.ALTERNANTHERA, 6)
            .add(PlantSpecies.TIGER_LOTUS, 5)
            .add(PlantSpecies.CRYPT_FLAMINGO, 2)
            // M28: the stem plants of swamps and paddy fields
            .add(PlantSpecies.LIMNOPHILA_SESSILIFLORA, 6)
            .add(PlantSpecies.BACOPA_CAROLINIANA, 6)
            .add(PlantSpecies.LUDWIGIA_SUPER_RED, 4)
            .add(PlantSpecies.ROTALA_HRA, 3)
            .add(PlantSpecies.ROTALA_WALLICHII, 3)
            .add(PlantSpecies.MYRIOPHYLLUM_RED, 3)
            // M28 part b: water clover, Riccia and hairgrass of the shallow swamps
            .add(PlantSpecies.MARSILEA, 4)
            .add(PlantSpecies.RICCIA, 4)
            .add(PlantSpecies.ELEOCHARIS, 3)
            .build();

    /** M22.4: fishing in mangrove swamps: the M13 swamp table (crypts and stem plants). */
    public static final WeightedTable<PlantSpecies> FISHING_PLANTS_MANGROVE = BOTTLE_CUTTINGS;

    /**
     * M22.4: what the aquarium net scoops up from the bottom with a plant (share {@code interiorPlants.netPlantShare} of its
     * catches, {@code features.plantNetCatches}): rivers Vallisneria, Monte Carlo from the banks, Java moss on the stones;
     * jungle water the Amazon sword (Amazon basin), Alternanthera (South America) and the tiger lotus (tropical Africa and
     * Asia); swamps Monte Carlo, Alternanthera, the tiger lotus and the crypts with the rare "Flamingo".
     */
    public static final WeightedTable<PlantSpecies> NET_PLANTS_RIVER = WeightedTable.<PlantSpecies>builder()
            .add(PlantSpecies.VALLISNERIA, 45)
            .add(PlantSpecies.MONTE_CARLO, 20)
            .add(PlantSpecies.JAVA_MOSS, 15)
            .add(PlantSpecies.ALTERNANTHERA, 10)
            .add(PlantSpecies.AMAZON_SWORD, 10)
            // M28
            .add(PlantSpecies.HYGROPHILA_PINNATIFIDA, 8)
            .add(PlantSpecies.BUCE_KEDAGANG, 6)
            .add(PlantSpecies.BUCE_THEIA, 6)
            .add(PlantSpecies.POGOSTEMON_DOWNOI, 5)
            // M28 part b
            .add(PlantSpecies.SAGITTARIA, 6)
            .add(PlantSpecies.CHRISTMAS_MOSS, 6)
            .add(PlantSpecies.FLAME_MOSS, 5)
            .add(PlantSpecies.CRYPT_PARVA, 4)
            .add(PlantSpecies.HC_CUBA, 2)
            .add(PlantSpecies.MARIMO, 2)
            .build();

    public static final WeightedTable<PlantSpecies> NET_PLANTS_JUNGLE = WeightedTable.<PlantSpecies>builder()
            .add(PlantSpecies.AMAZON_SWORD, 45)
            .add(PlantSpecies.ALTERNANTHERA, 20)
            .add(PlantSpecies.TIGER_LOTUS, 20)
            .add(PlantSpecies.JAVA_MOSS, 15)
            // M28
            .add(PlantSpecies.ANUBIAS_COFFEEFOLIA, 8)
            .add(PlantSpecies.ANUBIAS_ROUND_LEAF, 8)
            .add(PlantSpecies.ANUBIAS_PETITE, 6)
            // M28 part b
            .add(PlantSpecies.STAUROGYNE, 6)
            .add(PlantSpecies.ECHINODORUS_RED_DIAMOND, 5)
            .build();

    public static final WeightedTable<PlantSpecies> NET_PLANTS_SWAMP = WeightedTable.<PlantSpecies>builder()
            .add(PlantSpecies.MONTE_CARLO, 25)
            .add(PlantSpecies.ALTERNANTHERA, 20)
            .add(PlantSpecies.TIGER_LOTUS, 20)
            .add(PlantSpecies.CRYPT_GREEN, 15)
            .add(PlantSpecies.CRYPT_BROWN, 15)
            .add(PlantSpecies.CRYPT_FLAMINGO, 5)
            // M28
            .add(PlantSpecies.LIMNOPHILA_SESSILIFLORA, 10)
            .add(PlantSpecies.BACOPA_CAROLINIANA, 10)
            .add(PlantSpecies.ROTALA_HRA, 5)
            .add(PlantSpecies.MYRIOPHYLLUM_RED, 5)
            // M28 part b
            .add(PlantSpecies.MARSILEA, 8)
            .add(PlantSpecies.ELEOCHARIS, 6)
            .add(PlantSpecies.RICCIA, 6)
            .add(PlantSpecies.APONOGETON, 3)
            .add(PlantSpecies.NYMPHAEA_GREEN, 3)
            .build();

    /** M22.4: the default net plant table of a water (empty where the net brings up no plants). */
    public static WeightedTable<PlantSpecies> netPlants(org.overengineer.bettacraft.critters.NetCatching.Water w) {
        return switch (w) {
            case RIVER -> NET_PLANTS_RIVER;
            case JUNGLE -> NET_PLANTS_JUNGLE;
            case SWAMP -> NET_PLANTS_SWAMP;
            default -> WeightedTable.empty();
        };
    }

    /** M22.4: the default share of the net's catches that is a plant (where the water has plants). */
    public static final double NET_PLANT_SHARE = 0.2;

    /** Floating plants from fishing in a swamp (M12). */
    public static final WeightedTable<FloatingPlantType> FISHING_FLOATING_SWAMP = WeightedTable.<FloatingPlantType>builder()
            .add(FloatingPlantType.DUCKWEED, 45)
            .add(FloatingPlantType.SALVINIA, 30)
            .add(FloatingPlantType.FROGBIT, 25)
            // M28 part b (after the old weights: the rolls below 100 pick as before)
            .add(FloatingPlantType.HORNWORT, 14)
            .add(FloatingPlantType.WATER_LETTUCE, 12)
            .build();

    /** Floating plants from fishing in a mangrove swamp (M12): also the red root floater. */
    public static final WeightedTable<FloatingPlantType> FISHING_FLOATING_MANGROVE = WeightedTable.<FloatingPlantType>builder()
            .add(FloatingPlantType.DUCKWEED, 35)
            .add(FloatingPlantType.SALVINIA, 25)
            .add(FloatingPlantType.FROGBIT, 25)
            .add(FloatingPlantType.RED_ROOT_FLOATER, 15)
            // M28 part b
            .add(FloatingPlantType.WATER_LETTUCE, 12)
            .add(FloatingPlantType.HORNWORT, 8)
            .build();

    /**
     * Coral frags the net brings up on a reef: the soft corals and the anemone (M20, equally likely) and, M22.4, rarely a
     * broken-off piece of the leather coral and the stony corals.
     */
    public static final WeightedTable<PlantSpecies> NET_REEF_FRAGS = WeightedTable.<PlantSpecies>builder()
            .add(PlantSpecies.ZOANTHUS, 20)
            .add(PlantSpecies.MUSHROOM_CORAL, 20)
            .add(PlantSpecies.BUBBLE_TIP_ANEMONE, 20)
            .add(PlantSpecies.GREEN_STAR_POLYPS, 20)
            .add(PlantSpecies.LEATHER_CORAL, 8)
            .add(PlantSpecies.HAMMER_CORAL, 5)
            .add(PlantSpecies.LOBOPHYLLIA, 4)
            .add(PlantSpecies.ACROPORA, 3)
            .build();

    /** Mud broken in a swamp ({@code #bettacraft:betta_spawn_biomes}) drops mosquito larvae (loot modifier, datagen). */
    public static final float MUD_LARVAE_CHANCE = 0.04F;

    /** M26: mud broken in a swamp drops tubifex worms (the start of a tubifex dish; loot modifier, datagen). */
    public static final float MUD_TUBIFEX_CHANCE = 0.05F;

    /**
     * M22.3: the wild coral colonies of the warm ocean reefs (one species per colony; the common soft corals more often than
     * the stony ones). Shears cut a frag off them.
     */
    public static final WeightedTable<PlantSpecies> WILD_CORALS = WeightedTable.<PlantSpecies>builder()
            .add(PlantSpecies.ZOANTHUS, 16)
            .add(PlantSpecies.GREEN_STAR_POLYPS, 14)
            .add(PlantSpecies.MUSHROOM_CORAL, 16)
            .add(PlantSpecies.LEATHER_CORAL, 12)
            .add(PlantSpecies.HAMMER_CORAL, 10)
            .add(PlantSpecies.LOBOPHYLLIA, 10)
            .add(PlantSpecies.ACROPORA, 10)
            .add(PlantSpecies.BUBBLE_TIP_ANEMONE, 12)
            .build();

    /** M20: share of the net's catch chance on a reef that brings up a frag (times {@code reef.netShare}). */
    public static final double NET_FRAG_FACTOR = 0.25;

    // ------------------------------------------------------------------ M28.1 wild plants in the world

    /**
     * M28.1: the waters where aquarium and floating plants grow wild ({@code plants/wild/WildPlantFeature}), by their real
     * origin. The biomes are the tags of {@link org.overengineer.bettacraft.world.BiomeWaters} (biome mods count too).
     */
    public enum PlantWater {
        /** Jungle water: the shade plants of tropical streams (Anubias, Bucephalandra, Java fern), crypts, swords, lotus. */
        JUNGLE(Where.JUNGLE),
        /** Swamps: stem plants, carpets and lotus of shallow, sunny swamps; the floating plants of still water. */
        SWAMP(Where.SWAMP),
        /** Rivers: Vallisneria, Sagittaria, hairgrass and the mosses of running water. */
        RIVER(Where.RIVER),
        /** Mangrove swamps: the brackish-tolerant crypts, Java fern and moss; many floating plants in the calm. */
        MANGROVE(Where.MANGROVE),
        /** Cold lakes and frozen rivers: the Marimo balls, hairgrass and hornwort. */
        COLD(Where.COLD_WATER),
        /** The pools of the lush caves: Java moss and its relatives on the stone. */
        LUSH_CAVES(Where.LUSH_CAVES);

        private final Where where;

        PlantWater(Where where) {
            this.where = where;
        }

        public Where where() {
            return where;
        }
    }

    /**
     * M28.1: the aquarium plants growing wild under water (one species per group). Strong-light and soft-water species are
     * rarer (weights 1-3), the robust ones common. Every freshwater species stands in at least one table (test
     * {@code WildPlantSourcesTest}).
     */
    public static WeightedTable<PlantSpecies> wildPlants(PlantWater w) {
        return switch (w) {
            case JUNGLE -> WILD_PLANTS_JUNGLE;
            case SWAMP -> WILD_PLANTS_SWAMP;
            case RIVER -> WILD_PLANTS_RIVER;
            case MANGROVE -> WILD_PLANTS_MANGROVE;
            case COLD -> WILD_PLANTS_COLD;
            case LUSH_CAVES -> WILD_PLANTS_LUSH_CAVES;
        };
    }

    /** M28.1: the floating plants on the calm water surface (like lily pads); none in the dark cave pools. */
    public static WeightedTable<FloatingPlantType> wildFloating(PlantWater w) {
        return switch (w) {
            case JUNGLE -> WILD_FLOATING_JUNGLE;
            case SWAMP -> WILD_FLOATING_SWAMP;
            case RIVER -> WILD_FLOATING_RIVER;
            case MANGROVE -> WILD_FLOATING_MANGROVE;
            case COLD -> WILD_FLOATING_COLD;
            case LUSH_CAVES -> WeightedTable.empty();
        };
    }

    public static final WeightedTable<PlantSpecies> WILD_PLANTS_JUNGLE = WeightedTable.<PlantSpecies>builder()
            .add(PlantSpecies.JAVA_FERN, 10)
            .add(PlantSpecies.ANUBIAS_NANA, 10)
            .add(PlantSpecies.ANUBIAS_ROUND_LEAF, 6)
            .add(PlantSpecies.ANUBIAS_PETITE, 5)
            .add(PlantSpecies.ANUBIAS_COFFEEFOLIA, 4)
            .add(PlantSpecies.ANUBIAS_PINTO, 2)
            .add(PlantSpecies.ANUBIAS_WHITE, 2)
            .add(PlantSpecies.BUCE_GREEN_WAVY, 6)
            .add(PlantSpecies.BUCE_MINI_NEEDLE, 4)
            .add(PlantSpecies.BUCE_BROWNIE_RED, 4)
            .add(PlantSpecies.BUCE_THEIA, 4)
            .add(PlantSpecies.BUCE_BLUE, 3)
            .add(PlantSpecies.BUCE_KEDAGANG, 3)
            .add(PlantSpecies.BUCE_GODZILLA, 3)
            .add(PlantSpecies.BUCE_PINK_LADY, 2)
            .add(PlantSpecies.BUCE_MINI_PINK, 2)
            .add(PlantSpecies.BUCE_SNOW_WHITE, 2)
            .add(PlantSpecies.CRYPT_GREEN, 8)
            .add(PlantSpecies.CRYPT_BROWN, 6)
            .add(PlantSpecies.CRYPT_PARVA, 4)
            .add(PlantSpecies.CRYPT_FLAMINGO, 2)
            .add(PlantSpecies.AMAZON_SWORD, 6)
            .add(PlantSpecies.ECHINODORUS_RED_DIAMOND, 3)
            .add(PlantSpecies.HYGROPHILA_PINNATIFIDA, 4)
            .add(PlantSpecies.APONOGETON, 4)
            .add(PlantSpecies.TIGER_LOTUS, 3)
            .add(PlantSpecies.POGOSTEMON_DOWNOI, 2)
            .add(PlantSpecies.ROTALA_WALLICHII, 1)
            .build();

    public static final WeightedTable<PlantSpecies> WILD_PLANTS_SWAMP = WeightedTable.<PlantSpecies>builder()
            .add(PlantSpecies.ROTALA, 3)
            .add(PlantSpecies.LUDWIGIA, 8)
            .add(PlantSpecies.LIMNOPHILA_SESSILIFLORA, 8)
            .add(PlantSpecies.BACOPA_CAROLINIANA, 8)
            .add(PlantSpecies.MARSILEA, 6)
            .add(PlantSpecies.STAUROGYNE, 4)
            .add(PlantSpecies.NYMPHAEA_GREEN, 4)
            .add(PlantSpecies.TIGER_LOTUS, 3)
            .add(PlantSpecies.ROTALA_HRA, 2)
            .add(PlantSpecies.LUDWIGIA_SUPER_RED, 2)
            .add(PlantSpecies.MONTE_CARLO, 2)
            .add(PlantSpecies.HC_CUBA, 1)
            .add(PlantSpecies.ALTERNANTHERA, 1)
            .add(PlantSpecies.MYRIOPHYLLUM_RED, 1)
            .build();

    public static final WeightedTable<PlantSpecies> WILD_PLANTS_RIVER = WeightedTable.<PlantSpecies>builder()
            .add(PlantSpecies.VALLISNERIA, 10)
            .add(PlantSpecies.SAGITTARIA, 10)
            .add(PlantSpecies.ELEOCHARIS, 6)
            .add(PlantSpecies.JAVA_MOSS, 6)
            .add(PlantSpecies.CHRISTMAS_MOSS, 4)
            .add(PlantSpecies.FLAME_MOSS, 4)
            .add(PlantSpecies.AMAZON_SWORD, 3)
            .add(PlantSpecies.RICCIA, 2)
            .build();

    public static final WeightedTable<PlantSpecies> WILD_PLANTS_MANGROVE = WeightedTable.<PlantSpecies>builder()
            .add(PlantSpecies.CRYPT_GREEN, 6)
            .add(PlantSpecies.CRYPT_BROWN, 4)
            .add(PlantSpecies.JAVA_FERN, 6)
            .add(PlantSpecies.JAVA_MOSS, 6)
            .build();

    public static final WeightedTable<PlantSpecies> WILD_PLANTS_COLD = WeightedTable.<PlantSpecies>builder()
            .add(PlantSpecies.MARIMO, 10)
            .add(PlantSpecies.ELEOCHARIS, 4)
            .build();

    public static final WeightedTable<PlantSpecies> WILD_PLANTS_LUSH_CAVES = WeightedTable.<PlantSpecies>builder()
            .add(PlantSpecies.JAVA_MOSS, 10)
            .add(PlantSpecies.CHRISTMAS_MOSS, 4)
            .add(PlantSpecies.FLAME_MOSS, 4)
            .build();

    public static final WeightedTable<FloatingPlantType> WILD_FLOATING_SWAMP = WeightedTable.<FloatingPlantType>builder()
            .add(FloatingPlantType.DUCKWEED, 10)
            .add(FloatingPlantType.SALVINIA, 6)
            .add(FloatingPlantType.FROGBIT, 6)
            .add(FloatingPlantType.HORNWORT, 4)
            .add(FloatingPlantType.RICCIA, 2)
            .build();

    public static final WeightedTable<FloatingPlantType> WILD_FLOATING_MANGROVE = WeightedTable.<FloatingPlantType>builder()
            .add(FloatingPlantType.DUCKWEED, 6)
            .add(FloatingPlantType.WATER_LETTUCE, 6)
            .add(FloatingPlantType.SALVINIA, 6)
            .add(FloatingPlantType.FROGBIT, 4)
            .add(FloatingPlantType.RED_ROOT_FLOATER, 4)
            .build();

    public static final WeightedTable<FloatingPlantType> WILD_FLOATING_JUNGLE = WeightedTable.<FloatingPlantType>builder()
            .add(FloatingPlantType.WATER_LETTUCE, 6)
            .add(FloatingPlantType.SALVINIA, 3)
            .add(FloatingPlantType.RED_ROOT_FLOATER, 3)
            .build();

    /** Rivers run: only now and then hornwort or duckweed in a calm bay. */
    public static final WeightedTable<FloatingPlantType> WILD_FLOATING_RIVER = WeightedTable.<FloatingPlantType>builder()
            .add(FloatingPlantType.HORNWORT, 3)
            .add(FloatingPlantType.DUCKWEED, 2)
            .build();

    public static final WeightedTable<FloatingPlantType> WILD_FLOATING_COLD = WeightedTable.<FloatingPlantType>builder()
            .add(FloatingPlantType.HORNWORT, 6)
            .build();
}
