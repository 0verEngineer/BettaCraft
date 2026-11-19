package org.overengineer.bettacraft.gametest.framework;

import java.util.Arrays;
import java.util.Set;

/**
 * M52c: the mods of our test groups (power, aquatic, biomes, create, integration) that have no build for the Minecraft version
 * of this branch (26.1.2; checked on Modrinth, CurseForge and in ATM11). Tests that need one of them pass with a log line
 * instead of failing on an incomplete group: a documented gap of this version (report, CHANGELOG, handbook), not a missing jar.
 */
public final class VersionGaps {
    private VersionGaps() {}

    public static final String MINECRAFT = "26.1.2";

    public static final Set<String> NO_BUILD = Set.of(
            "mekanism", "mekanismcovers", "fluxnetworks", "industrialforegoing", // power, integration
            "alexscaves", "cataclysm", "crittersandcompanions", "deeperdarker", "shiny", "fisheggs", // aquatic
            "biomeswevegone", // biomes
            "create", "sable", // create
            "betterfishtanks", "fintastic", "naturalist"); // aquarium mods of the dev runtime

    /** Whether every one of these mods has no build for this version (then a test that needs them has nothing to test). */
    public static boolean allWithoutBuild(String... mods) {
        return mods.length > 0 && Arrays.stream(mods).allMatch(NO_BUILD::contains);
    }
}
