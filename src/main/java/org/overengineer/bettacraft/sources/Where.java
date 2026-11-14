package org.overengineer.bettacraft.sources;

/**
 * M22.2: the places of the wild sources (biomes and blocks), in the words of the handbook. The handbook generator writes
 * them in English and German; the biomes behind each one are named here for both the code and the reader.
 */
public enum Where {
    /** {@code #minecraft:is_river}. */
    RIVER,
    /** {@code minecraft:swamp}, {@code #c:is_swamp}. */
    SWAMP,
    /** {@code minecraft:mangrove_swamp}. */
    MANGROVE,
    /** {@code #minecraft:is_beach}. */
    BEACH,
    /** {@code #minecraft:is_jungle} (water that is not a river or a beach). */
    JUNGLE,
    /** Water deep in caves (8 blocks below sea level, no sky). */
    CAVE,
    /** {@code minecraft:lush_caves}. */
    LUSH_CAVES,
    /** {@code minecraft:warm_ocean} (the coral reefs). */
    WARM_OCEAN,
    /** {@code minecraft:lukewarm_ocean}, {@code minecraft:deep_lukewarm_ocean}. */
    LUKEWARM_OCEAN,
    /** Fishing: {@code #minecraft:is_jungle} or {@code #minecraft:is_river}. */
    JUNGLE_OR_RIVER,
    /** Fishing: {@code #bettacraft:betta_spawn_biomes} (swamps) or the mangrove swamp. */
    SWAMP_OR_MANGROVE,
    /** {@code #bettacraft:salt_water_biomes} (oceans and beaches). */
    SALT_WATER,
    /** Next to water anywhere. */
    NEAR_WATER,
    /** Any sand ({@code #bettacraft:artemia_sand}). */
    SAND
}
