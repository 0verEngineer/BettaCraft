package org.overengineer.bettacraft.sources;

/**
 * M22.2: the places of the wild sources (biomes and blocks), in the words of the handbook. The handbook generator writes
 * them in English and German; the biomes behind each one are named here for both the code and the reader. M25.1: the
 * biomes are the tags of {@link org.overengineer.bettacraft.world.BiomeWaters} ({@code #bettacraft:*_waters}), so the
 * biomes of biome mods count too.
 */
public enum Where {
    /** {@code #bettacraft:river_waters} ({@code #minecraft:is_river}). */
    RIVER,
    /** {@code #bettacraft:swamp_waters} ({@code minecraft:swamp}, {@code #c:is_swamp}). */
    SWAMP,
    /** {@code #bettacraft:mangrove_waters} ({@code minecraft:mangrove_swamp}). */
    MANGROVE,
    /** {@code #bettacraft:beach_waters} ({@code #minecraft:is_beach}). */
    BEACH,
    /** {@code #bettacraft:jungle_waters} ({@code #minecraft:is_jungle}; water that is not a river or a beach). */
    JUNGLE,
    /** Water deep in caves (8 blocks below sea level, no sky). */
    CAVE,
    /** {@code #bettacraft:lush_cave_waters} ({@code minecraft:lush_caves}). */
    LUSH_CAVES,
    /** {@code #bettacraft:warm_ocean_waters} ({@code minecraft:warm_ocean}, the coral reefs). */
    WARM_OCEAN,
    /** {@code #bettacraft:lukewarm_ocean_waters} ({@code minecraft:lukewarm_ocean}, {@code minecraft:deep_lukewarm_ocean}). */
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
    SAND,
    /** M28.1: {@code #bettacraft:cold_waters} (frozen rivers, the lakes of taigas and snowy plains, cold land biomes by {@code #c:is_cold/overworld}). */
    COLD_WATER
}
