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
            .build();

    /** M22.4: fishing in rivers: the epiphytes and, rarely, Vallisneria (river ribbons) and Monte Carlo from the banks. */
    public static final WeightedTable<PlantSpecies> FISHING_PLANTS_RIVER = WeightedTable.<PlantSpecies>builder()
            .addAll(FISHING_PLANTS_TROPICAL)
            .add(PlantSpecies.VALLISNERIA, 10)
            .add(PlantSpecies.MONTE_CARLO, 4)
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
            .build();

    public static final WeightedTable<PlantSpecies> NET_PLANTS_JUNGLE = WeightedTable.<PlantSpecies>builder()
            .add(PlantSpecies.AMAZON_SWORD, 45)
            .add(PlantSpecies.ALTERNANTHERA, 20)
            .add(PlantSpecies.TIGER_LOTUS, 20)
            .add(PlantSpecies.JAVA_MOSS, 15)
            .build();

    public static final WeightedTable<PlantSpecies> NET_PLANTS_SWAMP = WeightedTable.<PlantSpecies>builder()
            .add(PlantSpecies.MONTE_CARLO, 25)
            .add(PlantSpecies.ALTERNANTHERA, 20)
            .add(PlantSpecies.TIGER_LOTUS, 20)
            .add(PlantSpecies.CRYPT_GREEN, 15)
            .add(PlantSpecies.CRYPT_BROWN, 15)
            .add(PlantSpecies.CRYPT_FLAMINGO, 5)
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
            .build();

    /** Floating plants from fishing in a mangrove swamp (M12): also the red root floater. */
    public static final WeightedTable<FloatingPlantType> FISHING_FLOATING_MANGROVE = WeightedTable.<FloatingPlantType>builder()
            .add(FloatingPlantType.DUCKWEED, 35)
            .add(FloatingPlantType.SALVINIA, 25)
            .add(FloatingPlantType.FROGBIT, 25)
            .add(FloatingPlantType.RED_ROOT_FLOATER, 15)
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
}
