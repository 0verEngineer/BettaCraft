package org.overengineer.bettacraft.gametest.framework;

import java.util.Arrays;
import java.util.Set;

/**
 * M52c: the mods of our test groups (power, aquatic, biomes, create, integration) that have no build for the Minecraft version
 * of this branch. Tests that need one of them pass with a log line instead of failing on an incomplete group: a documented gap
 * of this version (report, CHANGELOG, handbook), not a missing jar. M52d: the list for 26.2 (Modrinth, CurseForge and the FTB
 * maven checked on 2026-11-19).
 */
public final class VersionGaps {
    private VersionGaps() {}

    public static final String MINECRAFT = "26.2";

    public static final Set<String> NO_BUILD = Set.of(
            "mekanism", "mekanismcovers", "fluxnetworks", "industrialforegoing", "oritech", "enderio", "enderio_endergy", "athena", // power, integration
            "refinedstorage", "laserio", // storage, integration
            "alexscaves", "cataclysm", "crittersandcompanions", "deeperdarker", "shiny", "fisheggs", "undergarden", "iceandfire",
            "sushigocrafting", "titanium", "jupiter", "uranus", "patchouli", // aquatic
            "biomeswevegone", "regions_unexplored", "twilightforest", // biomes
            "create", "sable", // create
            "ftbquests", "ftblibrary", "ftbteams", "kubejs", "rhino", "cable_facades", // quests, kubejs, facades
            "aether_ii", "productivebees", "thaumaturge", "mahoutsukai", "the_bumblezone", // zoo
            "fintastic"); // M52-final: Better Fish Tanks has builds (CurseForge) // aquarium mods of the dev runtime

    /**
     * M53: the map and render mods of the client test groups (maps, shaders, perf, vulkanmod) without a build for this
     * version (Modrinth, CurseForge and the FTB maven checked on 2026-11-21): FTB Chunks (and FTB Library/Teams), ModernFix and VulkanMod have no 26.2 build; the Vulkan backend of 26.2 itself is tested.
     */
    public static final Set<String> RENDER_NO_BUILD = Set.of("ftbchunks", "modernfix", "vulkanmod");

    /** Whether every one of these mods has no build for this version (then a test that needs them has nothing to test). */
    public static boolean allWithoutBuild(String... mods) {
        return mods.length > 0 && Arrays.stream(mods).allMatch(NO_BUILD::contains);
    }
}
