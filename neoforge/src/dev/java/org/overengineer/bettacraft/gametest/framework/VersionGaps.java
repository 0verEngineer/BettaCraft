package org.overengineer.bettacraft.gametest.framework;

import java.util.Arrays;
import java.util.Set;

/**
 * M52 (as on 26.1.2 and later): the mods of our test groups (power, storage, aquatic, biomes, create, integration, quests,
 * kubejs, facades, zoo) that have no build for the Minecraft version of this branch (1.21.11; checked on Modrinth and
 * CurseForge, 2026-11-20). Tests that need only such mods pass with a log line instead of failing on an incomplete group: a
 * documented gap of this version (report, CHANGELOG, handbook), not a missing jar.
 */
public final class VersionGaps {
    private VersionGaps() {}

    public static final String MINECRAFT = "1.21.11";

    public static final Set<String> NO_BUILD = Set.of(
            "mekanism", "mekanismcovers", "fluxnetworks", "industrialforegoing", "oritech", "enderio", "enderio_endergy", "athena",
            "powah", // power, integration
            "ae2", "refinedstorage", "laserio", "xnet", "rftoolsbase", "mcjtylib", "modularrouters", // storage, integration
            "alexscaves", "cataclysm", "crittersandcompanions", "deeperdarker", "shiny", "fisheggs", "undergarden", "iceandfire",
            "sushigocrafting", "titanium", "jupiter", "uranus", "patchouli", "naturalist", // aquatic
            "regions_unexplored", "twilightforest", // biomes (M52-final: Oh The Biomes We've Gone has a 1.21.11 build, CurseForge)
            "create", "sable", "aeronautics", // create
            "kubejs", "rhino", "cable_facades", // kubejs, facades
            "aether", "ars_nouveau", "ars_elemental", "productivebees", "occultism", "thaumaturge", "mahoutsukai",
            "actuallyadditions", "psychadelicchem", "fungidelight", "farmersdelight", "oritechthings", "garnished", // zoo
            "betterfishtanks", "fintastic"); // aquarium mods of the dev runtime

    /**
     * M53: the map and render mods of the client test groups (maps, shaders, perf, vulkanmod) without a build for this
     * version (Modrinth, CurseForge and the FTB maven checked on 2026-11-21): ModernFix has no 1.21.11 build; FTB Chunks 1.21.11 is a beta (tested).
     */
    public static final Set<String> RENDER_NO_BUILD = Set.of("modernfix");

    /** Whether every one of these mods has no build for this version (then a test that needs them has nothing to test). */
    public static boolean allWithoutBuild(String... mods) {
        return mods.length > 0 && Arrays.stream(mods).allMatch(NO_BUILD::contains);
    }
}
