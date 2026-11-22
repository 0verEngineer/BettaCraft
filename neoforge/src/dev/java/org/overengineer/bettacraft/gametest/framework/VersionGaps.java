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
            "fintastic", "naturalist"); // aquarium mods of the dev runtime

    /**
     * M53: the map and render mods of the client test groups (maps, shaders, perf, vulkanmod) without a build for this
     * version (Modrinth, CurseForge and the FTB maven checked on 2026-11-21): ModernFix has no Fabric 26.1.2 build (NeoForge: tested).
     */
    public static final Set<String> RENDER_NO_BUILD = Set.of("modernfix-fabric");

    /**
     * M61: mods of our test groups with builds for this version on NeoForge only (Modrinth and CurseForge checked on
     * 2026-11-22): Modern Dynamics (-Ppipes) has had no Fabric build since Minecraft 1.20.4, so the Fabric client smoke M61
     * runs without its pipes.
     */
    public static final Set<String> FABRIC_NO_BUILD = Set.of("moderndynamics");

    /**
     * M59: EMI (-Pemi, Fabric) has no build after Minecraft 1.21.1 (Modrinth, CurseForge 580555 and maven.terraformersmc.com
     * checked on 2026-11-22); a gap of the dev mod manifest (dev-mods.json, "gaps"), like every group listed there.
     */
    public static final Set<String> RECIPE_VIEWER_NO_BUILD = Set.of("emi");

    /** Whether every one of these mods has no build for this version (then a test that needs them has nothing to test). */
    public static boolean allWithoutBuild(String... mods) {
        return mods.length > 0 && Arrays.stream(mods).allMatch(NO_BUILD::contains);
    }
}
