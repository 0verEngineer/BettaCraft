package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.overengineer.bettacraft.platform.Tags;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.command.DevBiomeMods;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.world.AquaticPlantFishing;
import org.overengineer.bettacraft.world.BiomeWaters;
import org.overengineer.bettacraft.world.Wildlife;

/**
 * M25.1: the waters by biome tags instead of fixed vanilla biomes. Every vanilla biome keeps exactly the water it had
 * before (the old checks are kept here as the reference), the tags hold the vanilla biomes, the wild corals are only in
 * the warm ocean; with {@code -Pbiomes} a handful of biomes of biome mods map to their waters.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BiomeWatersGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private BiomeWatersGameTests() {}

    private static void check(boolean ok, String what) {
        if (!ok) throw new GameTestAssertException(what);
    }

    // ------------------------------------------------------------------ the checks before M25.1 (reference)

    private static NetCatching.Water oldNet(Holder<Biome> b) {
        if (b.is(Biomes.MANGROVE_SWAMP)) return NetCatching.Water.MANGROVE;
        if (b.is(Biomes.SWAMP) || b.is(Tags.Biomes.IS_SWAMP)) return NetCatching.Water.SWAMP;
        if (b.is(Biomes.LUSH_CAVES)) return NetCatching.Water.CAVE;
        return oldSpawn(b);
    }

    private static NetCatching.Water oldSpawn(Holder<Biome> b) {
        if (b.is(Biomes.MANGROVE_SWAMP)) return NetCatching.Water.MANGROVE;
        if (b.is(Biomes.SWAMP) || b.is(Tags.Biomes.IS_SWAMP)) return NetCatching.Water.SWAMP;
        if (b.is(BiomeTags.IS_RIVER)) return NetCatching.Water.RIVER;
        if (b.is(BiomeTags.IS_BEACH)) return NetCatching.Water.BEACH;
        if (b.is(Biomes.WARM_OCEAN)) return NetCatching.Water.WARM_OCEAN;
        if (b.is(Biomes.LUKEWARM_OCEAN) || b.is(Biomes.DEEP_LUKEWARM_OCEAN)) return NetCatching.Water.LUKEWARM_OCEAN;
        if (b.is(BiomeTags.IS_JUNGLE)) return NetCatching.Water.JUNGLE;
        return NetCatching.Water.NONE;
    }

    private static AquaticPlantFishing.Water oldPlants(Holder<Biome> b) {
        if (b.is(Biomes.MANGROVE_SWAMP)) return AquaticPlantFishing.Water.MANGROVE;
        if (b.is(ModTags.BETTA_SPAWN_BIOMES)) return AquaticPlantFishing.Water.SWAMP;
        if (b.is(BiomeTags.IS_JUNGLE)) return AquaticPlantFishing.Water.JUNGLE;
        if (b.is(BiomeTags.IS_RIVER)) return AquaticPlantFishing.Water.RIVER;
        return null;
    }

    /** Every vanilla biome: net (also deep under ground), spawns, plant fishing, floating plants, mud, corals as before. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void vanillaBiomesKeepTheirWaters(GameTestHelper h) {
        var reg = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        List<String> bad = new ArrayList<>();
        int n = 0;
        for (var b : reg.holders().toList()) {
            var id = b.key().location();
            if (!id.getNamespace().equals("minecraft")) continue;
            n++;
            NetCatching.Water old = oldNet(b);
            if (NetCatching.water(b, false) != old) bad.add(id + " net " + NetCatching.water(b, false) + " was " + old);
            NetCatching.Water oldDeep = old == NetCatching.Water.NONE ? NetCatching.Water.CAVE : old;
            if (NetCatching.water(b, true) != oldDeep) bad.add(id + " deep net " + NetCatching.water(b, true) + " was " + oldDeep);
            if (Wildlife.water(b) != oldSpawn(b)) bad.add(id + " spawns " + Wildlife.water(b) + " was " + oldSpawn(b));
            if (AquaticPlantFishing.water(b) != oldPlants(b)) bad.add(id + " plants " + AquaticPlantFishing.water(b) + " was " + oldPlants(b));
            if (BiomeWaters.mangrove(b) != b.is(Biomes.MANGROVE_SWAMP)) bad.add(id + " floating plants (mangrove)");
            if (BiomeWaters.swampOrBettaBiome(b) != b.is(ModTags.BETTA_SPAWN_BIOMES)) bad.add(id + " floating plants (swamp)");
            if (BiomeWaters.lushCave(b) != b.is(Biomes.LUSH_CAVES)) bad.add(id + " lush cave");
            boolean mud = b.is(Biomes.SWAMP) || b.is(Biomes.MANGROVE_SWAMP) || b.is(Tags.Biomes.IS_SWAMP);
            if ((BiomeWaters.swamp(b) || BiomeWaters.mangrove(b)) != mud) bad.add(id + " mud snails");
            boolean warm = b.is(Biomes.WARM_OCEAN);
            if (DevBiomeMods.hasCorals(b) != warm || BiomeWaters.coralReef(b) != warm) bad.add(id + " wild corals " + DevBiomeMods.hasCorals(b));
        }
        check(n >= 60, "only " + n + " vanilla biomes");
        check(bad.isEmpty(), "vanilla biomes changed: " + bad);
        h.succeed();
    }

    /** The tags hold the vanilla biomes (modpacks extend them). */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void waterTagsHoldTheVanillaBiomes(GameTestHelper h) {
        var reg = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        record Want(net.minecraft.tags.TagKey<Biome> tag, ResourceKey<Biome> biome) {}
        for (Want w : List.of(new Want(ModTags.WARM_OCEAN_WATERS, Biomes.WARM_OCEAN), new Want(ModTags.LUKEWARM_OCEAN_WATERS, Biomes.LUKEWARM_OCEAN),
                new Want(ModTags.LUKEWARM_OCEAN_WATERS, Biomes.DEEP_LUKEWARM_OCEAN), new Want(ModTags.MANGROVE_WATERS, Biomes.MANGROVE_SWAMP),
                new Want(ModTags.LUSH_CAVE_WATERS, Biomes.LUSH_CAVES), new Want(ModTags.SWAMP_WATERS, Biomes.SWAMP),
                new Want(ModTags.RIVER_WATERS, Biomes.RIVER), new Want(ModTags.RIVER_WATERS, Biomes.FROZEN_RIVER),
                new Want(ModTags.BEACH_WATERS, Biomes.BEACH), new Want(ModTags.JUNGLE_WATERS, Biomes.JUNGLE),
                new Want(ModTags.JUNGLE_WATERS, Biomes.BAMBOO_JUNGLE), new Want(ModTags.CORAL_REEF_BIOMES, Biomes.WARM_OCEAN))) {
            check(reg.getHolderOrThrow(w.biome()).is(w.tag()), w.biome().location() + " not in #" + w.tag().location());
        }
        check(!reg.getHolderOrThrow(Biomes.OCEAN).is(ModTags.WARM_OCEAN_WATERS), "the ocean is no warm ocean");
        check(!reg.getHolderOrThrow(Biomes.DEEP_LUKEWARM_OCEAN).is(ModTags.CORAL_REEF_BIOMES), "no reef in the deep lukewarm ocean");
        h.succeed();
    }

    /** {@code -Pbiomes}: biomes of Regions Unexplored and Oh The Biomes We've Gone map to their waters (skipped without them). */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void biomeModBiomesMapToTheirWaters(GameTestHelper h) {
        if (!DevBiomeMods.present()) {
            check(!DevBiomeMods.REQUIRED, "-Pbiomes but the biome mods are not loaded");
            h.succeed();
            return;
        }
        var problems = DevBiomeMods.problems(h.getLevel().registryAccess());
        check(problems.isEmpty(), "biome mods: " + problems);
        h.succeed();
    }
}
