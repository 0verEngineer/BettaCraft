package org.overengineer.bettacraft.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.FeatureLootCondition;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.sources.TraderOffers;
import org.overengineer.bettacraft.sources.WildTables;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.terrarium.HerpSpawns;
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.overengineer.bettacraft.world.Wildlife;

/**
 * M33 part a: a feature that is off loses its content: its recipes are not loaded (a fresh {@link RecipeManager} reads our
 * recipe files with the config as it is, like a datapack reload), the wandering trader's offers of it make nothing, the net
 * and the wild no longer bring its animals, plants or frags, its loot conditions fail. Six representative features
 * (tankmates with a sub-switch, the small animals, corals, turtles, live food, bettas); part b adds one test per switch.
 * Every test restores the switches in {@code finally} and runs in its own batch.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FeatureSwitchGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private FeatureSwitchGameTests() {}

    // ------------------------------------------------------------------ helpers

    /** A RecipeManager that loads a given set of recipe files (the protected reload step of the real one). */
    private static final class Loader extends RecipeManager {
        Loader(HolderLookup.Provider registries) {
            super(registries);
        }

        void load(Map<Identifier, JsonElement> files, ResourceManager resources) {
            apply(files, resources, InactiveProfiler.INSTANCE);
        }
    }

    /** Our recipe files read with the config as it is now (conditions decide like on a datapack reload). */
    static RecipeManager recipes(ServerLevel level) {
        var server = level.getServer();
        ResourceManager rm = server.getResourceManager();
        Map<Identifier, JsonElement> files = new HashMap<>();
        for (var e : rm.listResources("recipe", p -> p.getNamespace().equals(BettaCraft.MOD_ID) && p.getPath().endsWith(".json")).entrySet()) {
            String path = e.getKey().getPath();
            try (Reader r = e.getValue().openAsReader()) {
                JsonElement json = JsonParser.parseReader(r);
                if (!LoaderHooks.get().recipeAllowed(json, server)) continue; // Fabric: the conditions are read with the file
                files.put(BettaCraft.id(path.substring("recipe/".length(), path.length() - ".json".length())), json);
            } catch (IOException ex) {
                throw new GameTestAssertException("cannot read " + e.getKey() + ": " + ex);
            }
        }
        Loader l = new Loader(server.registryAccess());
        LoaderHooks.get().prepareRecipeLoader(l, server);
        // M42: KubeJS mixes into RecipeManager.apply and reads the reload resources it keeps in the real manager (a field of
        // its mixin); a fresh manager lacks them (NullPointerException), so it gets those of the server's
        try {
            java.lang.reflect.Field kjs = RecipeManager.class.getDeclaredField("kjs$resources");
            kjs.setAccessible(true);
            kjs.set(l, kjs.get(server.getRecipeManager()));
        } catch (NoSuchFieldException ignored) {
            // no KubeJS
        } catch (IllegalAccessException e) {
            throw new GameTestAssertException("cannot prepare the recipe loader for KubeJS: " + e);
        }
        l.load(files, rm);
        return l;
    }

    private static boolean has(RecipeManager rm, String id) {
        return rm.byKey(BettaCraft.id(id)).isPresent();
    }

    /** Sets switches off and gives back their old values for {@link #restore}. */
    private static Map<Feature, Boolean> off(Feature... features) {
        Map<Feature, Boolean> saved = new EnumMap<>(Feature.class);
        for (Feature f : features) {
            saved.put(f, Features.own(f));
            Features.set(f, false);
        }
        return saved;
    }

    private static void restore(Map<Feature, Boolean> saved) {
        saved.forEach(Features::set);
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new GameTestAssertException(what);
    }

    /** Whether the offer makes anything in 40 tries. */
    private static boolean offers(GameTestHelper h, WanderingTrader trader, String offer) {
        for (TraderOffers.Offer o : TraderOffers.all()) {
            if (!o.id().equals(offer)) continue;
            for (int i = 0; i < 40; i++) if (o.gatedListing().getOffer(trader, h.getLevel().getRandom()) != null) return true;
            return false;
        }
        throw new GameTestAssertException("no offer " + offer);
    }

    /** A water column at sea level for the spawn rules; returns a spot in it. */
    private static BlockPos water(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos a = h.absolutePos(new BlockPos(5, 0, 5));
        BlockPos base = new BlockPos(a.getX(), level.getSeaLevel() - 3, a.getZ());
        for (int y = 0; y < 4; y++) level.setBlock(base.above(y), Blocks.WATER.defaultBlockState(), 2);
        level.setBlock(base.below(), Blocks.STONE.defaultBlockState(), 2);
        return base.above(1);
    }

    private static void clearWater(GameTestHelper h, BlockPos spot) {
        for (int y = -1; y < 3; y++) h.getLevel().setBlock(spot.above(y), Blocks.AIR.defaultBlockState(), 2);
    }

    // ------------------------------------------------------------------ the tests

    /** All on (default): every recipe file of ours loads; the real recipe manager has them too. */
    @GameTest(template = EMPTY, batch = "bettacraft_m33_features", timeoutTicks = 200)
    public static void everyRecipeLoadsWithAllFeaturesOn(GameTestHelper h) {
        RecipeManager rm = recipes(h.getLevel());
        for (String id : List.of("tank", "food_maker", "crucible", "stainless_steel_ingot_from_crucible", "frag_from_brain_coral",
                "larva_tub", "glass_pipe", "food_maker/frozen_daphnia")) {
            check(has(rm, id), "recipe " + id + " missing with everything on");
            check(h.getLevel().getServer().getRecipeManager().byKey(BettaCraft.id(id)).isPresent(), "server lacks " + id);
        }
        h.succeed();
    }

    /** Tankmates off: no tankmate from the trader or the net, none spawns wild; cichlids alone off: only they are gone. */
    @GameTest(template = EMPTY, batch = "bettacraft_m33_tankmates", timeoutTicks = 200)
    public static void tankmatesOff(GameTestHelper h) {
        WanderingTrader trader = h.spawn(EntityType.WANDERING_TRADER, new BlockPos(2, 1, 2));
        BlockPos spot = water(h);
        var level = h.getLevel();
        var neon = ModEntities.TANKMATES.get(TankmateSpecies.NEON_TETRA).get();
        check(offers(h, trader, "tankmates_1"), "tankmates on: the trader sells them");
        check(!NetCatching.fishTable(NetCatching.Water.JUNGLE).isEmpty(), "tankmates on: the net brings fish");
        check(Wildlife.checkFish(neon, level, EntitySpawnReason.NATURAL, spot, level.random), "tankmates on: wild ones spawn");
        Map<Feature, Boolean> saved = off(Feature.CICHLIDS);
        try {
            check(!NetCatching.fishTable(NetCatching.Water.JUNGLE).containsKey(TankmateSpecies.DISCUS), "cichlids off: no discus in the net");
            check(NetCatching.fishTable(NetCatching.Water.JUNGLE).containsKey(TankmateSpecies.NEON_TETRA), "cichlids off: neons stay");
            var discus = ModEntities.TANKMATES.get(TankmateSpecies.DISCUS).get();
            check(!Wildlife.checkFish(discus, level, EntitySpawnReason.NATURAL, spot, level.random), "cichlids off: no wild discus");
            restore(saved);
            saved = off(Feature.TANKMATES);
            check(!offers(h, trader, "tankmates_1") && !offers(h, trader, "tankmates_rare"), "off: the trader sells none");
            check(!offers(h, trader, "reef_fish"), "off: no reef fish either (they need the tankmates)");
            for (NetCatching.Water w : NetCatching.Water.values()) check(NetCatching.fishTable(w).isEmpty(), "off: fish in the net in " + w);
            check(!Wildlife.checkFish(neon, level, EntitySpawnReason.NATURAL, spot, level.random), "off: a wild neon spawns");
            check(Wildlife.checkFish(neon, level, EntitySpawnReason.SPAWN_ITEM_USE, spot, level.random), "off: spawn eggs still work");
            check(Wildlife.fishWeight(TankmateSpecies.NEON_TETRA, NetCatching.Water.JUNGLE) == 0, "off: spawn weight");
        } finally {
            restore(saved);
            clearWater(h, spot);
        }
        h.succeed();
    }

    /** Small animals off: their recipes (shrimp food), trader offers, net catches, the trumpet snail and wild spawns are gone. */
    @GameTest(template = EMPTY, batch = "bettacraft_m33_invertebrates", timeoutTicks = 200)
    public static void invertebratesOff(GameTestHelper h) {
        WanderingTrader trader = h.spawn(EntityType.WANDERING_TRADER, new BlockPos(2, 1, 2));
        BlockPos spot = water(h);
        var level = h.getLevel();
        check(has(recipes(level), "food_maker/shrimp_food"), "on: shrimp food recipe");
        check(offers(h, trader, "neocaridina"), "on: the trader sells shrimp");
        Map<Feature, Boolean> saved = off(Feature.INVERTEBRATES);
        try {
            RecipeManager rm = recipes(level);
            check(!has(rm, "food_maker/shrimp_food"), "off: shrimp food recipe still there");
            check(has(rm, "food_maker/fish_flakes"), "off: unrelated food maker recipes stay");
            for (String o : List.of("dwarf_crayfish", "apple_snail", "neocaridina", "ramshorn", "caridina", "assassin_snail", "reef_crew")) {
                check(!offers(h, trader, o), "off: the trader still offers " + o);
            }
            for (NetCatching.Water w : NetCatching.Water.values()) {
                check(NetCatching.table(w, true, false, false).stream().allMatch(c -> c.species() == null), "off: small animals in the net in " + w);
            }
            check(!CritterSpecies.TRUMPET_SNAIL.available(), "off: no trumpet snails from mud");
            check(!Wildlife.checkCritter(ModEntities.WILD_CRITTER.get(), level, EntitySpawnReason.NATURAL, spot, level.random), "off: wild critters spawn");
            check(NetCatching.table(NetCatching.Water.SWAMP, false, false, false).stream().anyMatch(c -> c.species() == null),
                    "off: the frog of the swamp stays in the net");
        } finally {
            restore(saved);
            clearWater(h, spot);
        }
        h.succeed();
    }

    /** Corals off: the frag recipes, the coral offer, the frags of the net and the wild corals are gone; sea salt stays. */
    @GameTest(template = EMPTY, batch = "bettacraft_m33_corals", timeoutTicks = 200)
    public static void coralsOff(GameTestHelper h) {
        WanderingTrader trader = h.spawn(EntityType.WANDERING_TRADER, new BlockPos(2, 1, 2));
        check(offers(h, trader, "coral_frags"), "on: coral frags");
        Map<Feature, Boolean> saved = off(Feature.CORALS);
        try {
            RecipeManager rm = recipes(h.getLevel());
            for (String f : List.of("frag_from_brain_coral", "frag_from_bubble_coral", "frag_from_fire_coral", "frag_from_horn_coral",
                    "frag_from_tube_coral", "dosing_pump")) {
                check(!has(rm, f), "off: recipe " + f);
            }
            check(has(rm, "sea_salt") && has(rm, "protein_skimmer"), "off: salt water stays");
            check(!offers(h, trader, "coral_frags"), "off: the trader sells frags");
            check(offers(h, trader, "sea_salt"), "off: sea salt still sold");
            var odds = NetCatching.odds(NetCatching.Water.WARM_OCEAN, false, false, false, 1, 0.5, 0.5, 0, WildTables.netPlants(NetCatching.Water.WARM_OCEAN));
            check(odds.keySet().stream().noneMatch(k -> k instanceof org.overengineer.bettacraft.plants.PlantSpecies p && p.marine()), "off: frags in the net");
            // salt water off takes the corals with it (they need it)
            restore(saved);
            saved = off(Feature.SALTWATER);
            check(!Features.enabled(Feature.CORALS), "corals need salt water");
            check(!has(recipes(h.getLevel()), "frag_from_brain_coral"), "salt water off: frags gone too");
        } finally {
            restore(saved);
        }
        h.succeed();
    }

    /** Turtles off: no turtle at the trader, no wild turtles; terrariums off takes the turtles with them. */
    @GameTest(template = EMPTY, batch = "bettacraft_m33_turtles", timeoutTicks = 200)
    public static void turtlesOff(GameTestHelper h) {
        WanderingTrader trader = h.spawn(EntityType.WANDERING_TRADER, new BlockPos(2, 1, 2));
        check(offers(h, trader, "musk_turtle"), "on: the trader sells musk turtles");
        check(HerpSpawns.enabled(HerpSpecies.MUSK), "on: wild musk turtles");
        Map<Feature, Boolean> saved = off(Feature.TURTLES);
        try {
            check(!offers(h, trader, "musk_turtle"), "off: musk turtle sold");
            for (HerpSpecies s : HerpSpecies.TURTLES) check(!HerpSpawns.enabled(s), "off: wild " + s.id());
            check(HerpSpawns.enabled(HerpSpecies.CORN_SNAKE), "off: the snakes stay");
            check(HerpSpawns.entries(HerpSpawns.Home.SWAMP).stream().noneMatch(e -> e.species().turtle()), "off: turtles in the swamp spawn table");
            restore(saved);
            saved = off(Feature.TERRARIUMS);
            check(!offers(h, trader, "musk_turtle"), "terrariums off: musk turtle sold");
            check(!HerpSpawns.enabled(HerpSpecies.CORN_SNAKE) && !HerpSpawns.enabled(HerpSpecies.MUSK), "terrariums off: wild herps");
            check(!has(recipes(h.getLevel()), "heat_mat"), "terrariums off: heat mat recipe");
        } finally {
            restore(saved);
        }
        h.succeed();
    }

    /** Live food off: the cultures and the food maker's frozen food lose their recipes, the mud and sand loot conditions fail. */
    @GameTest(template = EMPTY, batch = "bettacraft_m33_livefood", timeoutTicks = 200)
    public static void liveFoodOff(GameTestHelper h) {
        Map<Feature, Boolean> saved = off(Feature.LIVE_FOOD);
        try {
            RecipeManager rm = recipes(h.getLevel());
            for (String id : List.of("larva_tub", "artemia_hatchery", "tubifex_dish", "daphnia_barrel", "live_food_pump", "food_maker/frozen_daphnia",
                    "cricket_culture")) {
                check(!has(rm, id), "off: recipe " + id);
            }
            check(has(rm, "food_maker") && has(rm, "food_maker/fish_flakes"), "off: the food maker and its dry food stay");
            check(!new FeatureLootCondition(Feature.LIVE_FOOD).test(null), "off: the loot condition of the mud larvae");
            check(!new FeatureLootCondition(Feature.LIVE_FOOD_CULTURES).test(null), "off: the tubifex of the mud (a child)");
            check(new FeatureLootCondition(Feature.FLOATING_PLANTS).test(null), "off: other loot stays");
        } finally {
            restore(saved);
        }
        check(new FeatureLootCondition(Feature.LIVE_FOOD).test(null), "restored");
        h.succeed();
    }

    /** Bettas off (or only their wild spawns): no natural betta spawns, spawn eggs still work; the wild species go with them. */
    @GameTest(template = EMPTY, batch = "bettacraft_m33_bettas", timeoutTicks = 200)
    public static void bettaWildSpawnsOff(GameTestHelper h) {
        BlockPos spot = water(h);
        var level = h.getLevel();
        var betta = ModEntities.BETTA.get();
        Map<Feature, Boolean> saved = off(Feature.BETTAS_WILD);
        try {
            check(!SpawnPlacements.checkSpawnRules(betta, level, EntitySpawnReason.NATURAL, spot, level.random), "wild spawns off: a betta spawns");
            check(SpawnPlacements.checkSpawnRules(betta, level, EntitySpawnReason.SPAWN_ITEM_USE, spot, level.random), "wild spawns off: spawn eggs");
            check(Features.enabled(Feature.WILD_BETTAS), "a sibling stays on");
            restore(saved);
            saved = off(Feature.BETTAS);
            check(!Features.enabled(Feature.BETTAS_WILD) && !Features.enabled(Feature.WILD_BETTAS), "bettas off: their sub-switches");
            check(!SpawnPlacements.checkSpawnRules(betta, level, EntitySpawnReason.NATURAL, spot, level.random), "bettas off: a betta spawns");
            check(Wildlife.wildBettaSpecies(0.01) == org.overengineer.bettacraft.genetics.BettaSpecies.SPLENDENS, "bettas off: no wild species");
        } finally {
            restore(saved);
            clearWater(h, spot);
        }
        h.succeed();
    }
}
