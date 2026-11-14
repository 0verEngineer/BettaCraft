package org.overengineer.bettacraft.world;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M25.1: the kind of water of a biome, from the biome tags of the mod ({@code #bettacraft:*_waters}) instead of fixed
 * vanilla biomes, so the biomes of biome mods count too. Defaults: the vanilla biomes, the common tags and the known
 * biome mods (optional entries, see the datagen). Two kinds also take the intersection of two common tags, which a tag
 * file cannot express: a warm ocean is also any biome in both {@code #c:is_ocean} and {@code #c:is_hot}, a lush cave any
 * biome in both {@code #c:is_lush} and {@code #c:is_cave} (in vanilla exactly the warm ocean and the lush caves).
 * The checks run in the order of {@link #water}: mangroves before swamps, rivers and beaches before the oceans, the
 * jungle last.
 */
public final class BiomeWaters {
    private BiomeWaters() {}

    public static boolean mangrove(Holder<Biome> b) {
        return b.is(ModTags.MANGROVE_WATERS);
    }

    public static boolean swamp(Holder<Biome> b) {
        return b.is(ModTags.SWAMP_WATERS);
    }

    public static boolean lushCave(Holder<Biome> b) {
        return b.is(ModTags.LUSH_CAVE_WATERS) || b.is(Tags.Biomes.IS_LUSH) && b.is(Tags.Biomes.IS_CAVE);
    }

    public static boolean river(Holder<Biome> b) {
        return b.is(ModTags.RIVER_WATERS);
    }

    public static boolean beach(Holder<Biome> b) {
        return b.is(ModTags.BEACH_WATERS);
    }

    public static boolean warmOcean(Holder<Biome> b) {
        return b.is(ModTags.WARM_OCEAN_WATERS) || b.is(Tags.Biomes.IS_OCEAN) && b.is(Tags.Biomes.IS_HOT);
    }

    public static boolean lukewarmOcean(Holder<Biome> b) {
        return b.is(ModTags.LUKEWARM_OCEAN_WATERS);
    }

    public static boolean jungle(Holder<Biome> b) {
        return b.is(ModTags.JUNGLE_WATERS);
    }

    /** The wild corals grow here ({@code #bettacraft:coral_reef_biomes} or a warm ocean by the common tags). */
    public static boolean coralReef(Holder<Biome> b) {
        return b.is(ModTags.CORAL_REEF_BIOMES) || b.is(Tags.Biomes.IS_OCEAN) && b.is(Tags.Biomes.IS_HOT);
    }

    /**
     * M28.1: cold fresh water ({@code #bettacraft:cold_waters}, or a land biome in {@code #c:is_cold/overworld}: no ocean,
     * no beach).
     */
    public static boolean coldWater(Holder<Biome> b) {
        return b.is(ModTags.COLD_WATERS) || b.is(Tags.Biomes.IS_COLD_OVERWORLD) && !b.is(Tags.Biomes.IS_OCEAN) && !b.is(Tags.Biomes.IS_BEACH)
                && !b.is(net.minecraft.tags.BiomeTags.IS_OCEAN);
    }

    /**
     * M28.1: the water of the wild plants in a biome, or null where none grow (oceans, beaches, plains ...). Mangroves before
     * swamps, the lush caves, the cold waters before the rivers (a frozen river is cold), the jungle last.
     */
    @javax.annotation.Nullable
    public static org.overengineer.bettacraft.sources.WildTables.PlantWater plantWater(Holder<Biome> b) {
        if (mangrove(b)) return org.overengineer.bettacraft.sources.WildTables.PlantWater.MANGROVE;
        if (swamp(b)) return org.overengineer.bettacraft.sources.WildTables.PlantWater.SWAMP;
        if (lushCave(b)) return org.overengineer.bettacraft.sources.WildTables.PlantWater.LUSH_CAVES;
        if (coldWater(b)) return org.overengineer.bettacraft.sources.WildTables.PlantWater.COLD;
        if (river(b)) return org.overengineer.bettacraft.sources.WildTables.PlantWater.RIVER;
        if (beach(b) || b.is(Tags.Biomes.IS_OCEAN)) return null;
        if (jungle(b)) return org.overengineer.bettacraft.sources.WildTables.PlantWater.JUNGLE;
        return null;
    }

    /** Swamps for the fishing of plants: the swamp waters and the betta biomes. */
    public static boolean swampOrBettaBiome(Holder<Biome> b) {
        return swamp(b) || b.is(ModTags.BETTA_SPAWN_BIOMES);
    }

    /**
     * The kind of water of a biome. {@code caves}: the lush caves count (the net); the spawns leave them out.
     * {@link NetCatching.Water#NONE} for every other biome (the net then still checks for deep cave water).
     */
    public static NetCatching.Water water(Holder<Biome> b, boolean caves) {
        if (mangrove(b)) return NetCatching.Water.MANGROVE;
        if (swamp(b)) return NetCatching.Water.SWAMP;
        if (caves && lushCave(b)) return NetCatching.Water.CAVE;
        if (river(b)) return NetCatching.Water.RIVER;
        if (beach(b)) return NetCatching.Water.BEACH;
        if (warmOcean(b)) return NetCatching.Water.WARM_OCEAN;
        if (lukewarmOcean(b)) return NetCatching.Water.LUKEWARM_OCEAN;
        if (jungle(b)) return NetCatching.Water.JUNGLE;
        return NetCatching.Water.NONE;
    }
}
