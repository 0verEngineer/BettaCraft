package org.overengineer.bettacraft.sources;

import java.util.List;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;

/**
 * M22.2: what the wild gives, as data. The fishing loot modifiers, the swamp water bottle and the net on a reef pick
 * from these tables; {@link Sources} turns them (with the config defaults) into the "Obtaining" sections of the handbook.
 * Later milestones add their sources here.
 */
public final class WildTables {
    private WildTables() {}

    /** Fishing in jungles and rivers (M13): the epiphytes, "Blue" and "Brownie Red" rarely. */
    public static final WeightedTable<PlantSpecies> FISHING_PLANTS_TROPICAL = WeightedTable.<PlantSpecies>builder()
            .add(PlantSpecies.JAVA_FERN, 30)
            .add(PlantSpecies.ANUBIAS_NANA, 30)
            .add(PlantSpecies.BUCE_GREEN_WAVY, 15)
            .add(PlantSpecies.BUCE_MINI_NEEDLE, 13)
            .add(PlantSpecies.BUCE_BLUE, 6)
            .add(PlantSpecies.BUCE_BROWNIE_RED, 6)
            .build();

    /** Fishing in swamps and mangrove swamps, and a cutting in a bottle of swamp water (M13). */
    public static final WeightedTable<PlantSpecies> FISHING_PLANTS_SWAMP = WeightedTable.<PlantSpecies>builder()
            .add(PlantSpecies.CRYPT_GREEN, 35)
            .add(PlantSpecies.CRYPT_BROWN, 25)
            .add(PlantSpecies.LUDWIGIA, 20)
            .add(PlantSpecies.ROTALA, 20)
            .build();

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

    /** Soft coral frags and the anemone the net brings up on a reef (M20), all equally likely. */
    public static final List<PlantSpecies> NET_REEF_FRAGS = List.of(PlantSpecies.ZOANTHUS, PlantSpecies.MUSHROOM_CORAL,
            PlantSpecies.BUBBLE_TIP_ANEMONE, PlantSpecies.GREEN_STAR_POLYPS);

    /** Mud broken in a swamp ({@code #bettacraft:betta_spawn_biomes}) drops mosquito larvae (loot modifier, datagen). */
    public static final float MUD_LARVAE_CHANCE = 0.04F;

    /** M20: share of the net's catch chance on a reef that brings up a frag (times {@code reef.netShare}). */
    public static final double NET_FRAG_FACTOR = 0.25;
}
