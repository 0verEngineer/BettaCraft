package org.overengineer.bettacraft.sources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M22.2: every way the world gives a Bettacraft thing, computed from the same tables and rules the game uses (net,
 * fishing, bottles, shears, shovel, block drops, spawns, wandering trader) with the default config values. The handbook
 * generator ({@code GuideGen} in the tests) writes the "Obtaining" section of every page from this list, and a test
 * fails when the book and this list differ. Recipes, breeding and cuttings come from the recipe files and the species
 * data. New sources: add them to {@link WildTables}, {@link TraderOffers} or here, then run {@code gradlew updateGuide}.
 */
public final class Sources {
    private Sources() {}

    private static <T> T def(ModConfigSpec.ConfigValue<T> v) {
        return v.getDefault();
    }

    /** The waters of the net and the place they stand for. */
    private static final Map<NetCatching.Water, Where> NET_WATERS = Map.of(
            NetCatching.Water.RIVER, Where.RIVER, NetCatching.Water.SWAMP, Where.SWAMP, NetCatching.Water.MANGROVE, Where.MANGROVE,
            NetCatching.Water.BEACH, Where.BEACH, NetCatching.Water.JUNGLE, Where.JUNGLE, NetCatching.Water.CAVE, Where.CAVE,
            NetCatching.Water.WARM_OCEAN, Where.WARM_OCEAN, NetCatching.Water.LUKEWARM_OCEAN, Where.LUKEWARM_OCEAN);

    public static List<Source> all() {
        List<Source> out = new ArrayList<>();
        net(out);
        fishing(out);
        bigAnimals(out); // M30
        world(out);
        trader(out);
        aquarist(out); // M47
        return out;
    }

    /** All sources of one target. */
    public static List<Source> of(String target) {
        List<Source> out = new ArrayList<>();
        for (Source s : all()) if (s.target().equals(target)) out.add(s);
        return out;
    }

    // ------------------------------------------------------------------ the aquarium net

    private static void net(List<Source> out) {
        double chance = def(ServerConfig.CRITTERS_NET_CHANCE), tank = def(ServerConfig.TANKMATES_NET_SHARE), reef = def(ServerConfig.REEF_NET_SHARE),
                plantShare = def(ServerConfig.AQUATIC_NET_SHARE);
        for (NetCatching.Water w : NetCatching.Water.values()) {
            if (w == NetCatching.Water.NONE) continue;
            boolean[] lushes = w == NetCatching.Water.CAVE ? new boolean[]{false, true} : new boolean[]{false};
            for (boolean lush : lushes) {
                Where where = lush ? Where.LUSH_CAVES : NET_WATERS.get(w);
                // the lowest chance (no plants, soft bottom) and the highest of the four combinations
                WeightedTable<PlantSpecies> plantTable = WildTables.netPlants(w);
                Map<Object, Double> base = NetCatching.odds(w, false, false, lush, chance, tank, reef, plantShare, plantTable);
                Map<Object, Double> best = new java.util.LinkedHashMap<>(base);
                for (boolean plants : new boolean[]{false, true}) {
                    for (boolean hard : new boolean[]{false, true}) {
                        NetCatching.odds(w, plants, hard, lush, chance, tank, reef, plantShare, plantTable).forEach((k, v) -> best.merge(k, v, Math::max));
                    }
                }
                for (var e : best.entrySet()) {
                    double lo = base.getOrDefault(e.getKey(), 0.0);
                    String target = key(e.getKey());
                    int[] n = netCount(e.getKey());
                    String cond = e.getKey() instanceof TankmateSpecies s && s.seahorse() ? Feature.SEAHORSES.key()
                            : e.getKey() instanceof CritterSpecies cs && cs.feature() != null ? cs.feature().key()
                            : e.getKey() instanceof PlantSpecies p && !p.marine() ? Feature.PLANT_NET_CATCHES.key() : null;
                    out.add(new Source(Source.Kind.NET, target, where, lo, e.getValue(), n[0], n[1], 0, false, cond, null));
                }
            }
        }
    }

    /** M30: the big animals of each water: the large net (per swing) and the steel leader (per catch with the rod). */
    private static void bigAnimals(List<Source> out) {
        double net = def(ServerConfig.BIG_NET_CHANCE), lead = def(ServerConfig.BIG_LEADER_CHANCE);
        for (NetCatching.Water w : NetCatching.Water.values()) {
            Where where = NET_WATERS.get(w);
            if (where == null || w == NetCatching.Water.CAVE) continue;
            Map<TankmateSpecies, Double> table = org.overengineer.bettacraft.world.Wildlife.bigTable(w);
            double total = 0;
            for (double v : table.values()) total += v;
            for (var e : table.entrySet()) {
                double share = e.getValue() / total;
                String target = Target.tankmate(e.getKey());
                out.add(new Source(Source.Kind.BIG_NET, target, where, net * share, net * share, 1, 1, 0, false, Feature.BIG_ANIMALS.key(), null));
                out.add(new Source(Source.Kind.STEEL_LEADER, target, where, lead * share, lead * share, 1, 1, 0, false, Feature.BIG_ANIMALS.key(), null));
            }
        }
    }

    private static String key(Object o) {
        if (o instanceof TankmateSpecies s) return Target.tankmate(s);
        if (o instanceof CritterSpecies s) return Target.critter(s);
        if (o instanceof PlantSpecies p) return Target.plant(p);
        return Target.FROG;
    }

    /** How many animals one catch holds (see {@code NetCatching.catchItem} and the fish branch of {@code tryCatch}). */
    private static int[] netCount(Object o) {
        if (o instanceof TankmateSpecies s) return s.shoaling() ? new int[]{1, 4} : new int[]{1, 1};
        if (o instanceof CritterSpecies s) {
            return switch (s.group()) {
                case SHRIMP -> new int[]{1, 3};
                case CRAYFISH -> new int[]{1, 1};
                case SNAIL -> s == CritterSpecies.APPLE_SNAIL || s == CritterSpecies.ASSASSIN_SNAIL || s == CritterSpecies.RED_NERITE
                        || s == CritterSpecies.YELLOW_NERITE || s == CritterSpecies.TYLOMELANIA ? new int[]{1, 1} : new int[]{1, 2};
            };
        }
        return new int[]{1, 1};
    }

    // ------------------------------------------------------------------ fishing with a rod

    private static void fishing(List<Source> out) {
        double aquatic = def(ServerConfig.AQUATIC_FISHING_CHANCE), floating = def(ServerConfig.PLANTS_FISHING_CHANCE);
        // floating plants first (M12): in swamps and mangrove swamps they replace the catch; the aquarium plant modifier
        // leaves a catch alone that already became a floating plant
        for (var e : WildTables.FISHING_FLOATING_SWAMP.entries()) {
            out.add(src(Source.Kind.FISHING, Target.floating(e.value()), Where.SWAMP, floating * WildTables.FISHING_FLOATING_SWAMP.share(e.value())));
        }
        for (var e : WildTables.FISHING_FLOATING_MANGROVE.entries()) {
            out.add(src(Source.Kind.FISHING, Target.floating(e.value()), Where.MANGROVE, floating * WildTables.FISHING_FLOATING_MANGROVE.share(e.value())));
        }
        // M22.4: per water (jungle, river, swamp, mangrove); in swamps and mangroves after the floating plants
        for (var w : org.overengineer.bettacraft.world.AquaticPlantFishingModifier.Water.values()) {
            WeightedTable<PlantSpecies> t = PlantTables.fishingDefault(w);
            Where where = switch (w) {
                case JUNGLE -> Where.JUNGLE;
                case RIVER -> Where.RIVER;
                case SWAMP -> Where.SWAMP;
                case MANGROVE -> Where.MANGROVE;
            };
            double before = w == org.overengineer.bettacraft.world.AquaticPlantFishingModifier.Water.SWAMP
                    || w == org.overengineer.bettacraft.world.AquaticPlantFishingModifier.Water.MANGROVE ? 1 - floating : 1;
            java.util.Set<PlantSpecies> seen = new java.util.HashSet<>();
            for (var e : t.entries()) {
                if (seen.add(e.value())) out.add(src(Source.Kind.FISHING, Target.plant(e.value()), where, before * aquatic * t.share(e.value())));
            }
        }
    }

    // ------------------------------------------------------------------ bottles, shears, shovel, blocks, spawns

    private static void world(List<Source> out) {
        out.add(src(Source.Kind.BOTTLE, Target.item("swamp_water_bottle"), Where.SWAMP, 1.0));
        out.add(src(Source.Kind.BOTTLE, Target.item("salt_water_bottle"), Where.SALT_WATER, 1.0));
        double duck = def(ServerConfig.PLANTS_BOTTLE_CHANCE), cut = def(ServerConfig.AQUATIC_BOTTLE_CHANCE);
        out.add(src(Source.Kind.BOTTLE, Target.floating(FloatingPlantType.DUCKWEED), Where.SWAMP, duck));
        for (var e : WildTables.BOTTLE_CUTTINGS.entries()) {
            out.add(src(Source.Kind.BOTTLE, Target.plant(e.value()), Where.SWAMP, (1 - duck) * cut * WildTables.BOTTLE_CUTTINGS.share(e.value())));
        }
        out.add(src(Source.Kind.SHEARS, Target.plant(PlantSpecies.JAVA_MOSS), Where.NEAR_WATER, def(ServerConfig.AQUATIC_MOSS_CHANCE), "minecraft:moss_block"));
        out.add(src(Source.Kind.SHOVEL, Target.critter(CritterSpecies.TRUMPET_SNAIL), Where.SWAMP, def(ServerConfig.CRITTERS_MUD_CHANCE), "minecraft:mud"));
        out.add(src(Source.Kind.SHOVEL, Target.critter(CritterSpecies.TRUMPET_SNAIL), Where.MANGROVE, def(ServerConfig.CRITTERS_MUD_CHANCE), "minecraft:mud"));
        out.add(src(Source.Kind.BLOCK_DROP, Target.item("mosquito_larvae"), Where.SWAMP, WildTables.MUD_LARVAE_CHANCE, "minecraft:mud"));
        // M26: the starters of the live food cultures
        out.add(src(Source.Kind.BLOCK_DROP, Target.item("tubifex"), Where.SWAMP, WildTables.MUD_TUBIFEX_CHANCE, "minecraft:mud"));
        out.add(src(Source.Kind.BOTTLE, Target.item("daphnia"), Where.SWAMP, def(ServerConfig.LIVE_FOOD_BOTTLE_DAPHNIA_CHANCE)));
        out.add(src(Source.Kind.BLOCK_DROP, Target.item("artemia_cysts"), Where.SAND, def(ServerConfig.DROPS_ARTEMIA_FROM_SAND_CHANCE), "#c:sands"));
        int weight = def(CommonConfig.SPAWN_WEIGHT), min = def(CommonConfig.SPAWN_MIN_GROUP), max = Math.max(min, def(CommonConfig.SPAWN_MAX_GROUP));
        out.add(new Source(Source.Kind.SPAWN, Target.BETTA, Where.SWAMP, weight, weight, min, max, 0, false, null, null));
        // M29 part c: some of the wild bettas are Betta imbellis or Betta smaragdina (chanceMax: their share among the wild bettas)
        out.add(new Source(Source.Kind.SPAWN, Target.betta(org.overengineer.bettacraft.genetics.BettaSpecies.IMBELLIS), Where.SWAMP, weight,
                org.overengineer.bettacraft.world.Wildlife.IMBELLIS_SHARE, min, max, 0, false, Feature.WILD_BETTAS.key(), null));
        out.add(new Source(Source.Kind.SPAWN, Target.betta(org.overengineer.bettacraft.genetics.BettaSpecies.SMARAGDINA), Where.SWAMP, weight,
                org.overengineer.bettacraft.world.Wildlife.SMARAGDINA_SHARE, min, max, 0, false, Feature.WILD_BETTAS.key(), null));
        wildlife(out);
        wildPlants(out);
    }

    /**
     * M28.1: the wild aquarium plants under water (shears cut a cutting off the wild block; stem plants give two) and the
     * wild floating plants on the surface (broken by hand, with shears or the net: always the plant), per water.
     */
    private static void wildPlants(List<Source> out) {
        for (WildTables.PlantWater w : WildTables.PlantWater.values()) {
            for (var e : WildTables.wildPlants(w).entries()) {
                PlantSpecies s = e.value();
                int n = s.form() == PlantSpecies.Form.STEM ? 2 : 1;
                out.add(new Source(Source.Kind.SHEARS, Target.plant(s), w.where(), 1.0, 1.0, n, n, 0, false, Feature.AQUATIC_WILD.key(),
                        "bettacraft:" + org.overengineer.bettacraft.plants.wild.WildPlantKind.of(s).id()));
            }
            for (var e : WildTables.wildFloating(w).entries()) {
                FloatingPlantType t = e.value();
                String target = t.plantItem() ? Target.plant(PlantSpecies.byId(t.id())) : Target.floating(t);
                out.add(new Source(Source.Kind.BLOCK_DROP, target, w.where(), 1.0, 1.0, 1, 1, 0, false,
                        (t.plantItem() ? Feature.AQUATIC_WILD : Feature.FLOATING_WILD).key(), WILD_FLOATING_BLOCK));
            }
        }
    }

    /** M28.1: the block id of the wild floating plants (the handbook names it). */
    public static final String WILD_FLOATING_BLOCK = "bettacraft:wild_floating_plant";

    /** M22.3: the wild tankmates, frogs and small animals (spawns per water) and the wild corals of the reefs (shears). */
    private static void wildlife(List<Source> out) {
        for (NetCatching.Water w : NetCatching.Water.values()) {
            Where where = NET_WATERS.get(w);
            if (where == null || w == NetCatching.Water.CAVE) continue;
            for (TankmateSpecies s : TankmateSpecies.values()) {
                int weight = org.overengineer.bettacraft.world.Wildlife.fishWeight(s, w);
                if (weight <= 0) continue;
                int[] g = org.overengineer.bettacraft.world.Wildlife.group(s);
                out.add(new Source(Source.Kind.SPAWN, Target.tankmate(s), where, weight, weight, g[0], g[1], 0, false,
                        s.featureKey(), null));
            }
            int frog = org.overengineer.bettacraft.world.Wildlife.frogWeight(w);
            if (frog > 0) out.add(new Source(Source.Kind.SPAWN, Target.FROG, where, frog, frog, 1, 2, 0, false, Feature.FROGS_WILD.key(), null));
            int critters = org.overengineer.bettacraft.world.Wildlife.critterWeight(w);
            if (critters <= 0) continue;
            var table = org.overengineer.bettacraft.world.Wildlife.critterSpawnTable(w, false, false); // M46: the apple snail damped
            double total = 0;
            for (var c : table) total += c.weight();
            for (var c : table) {
                int wt = (int) Math.max(1, Math.round(critters * c.weight() / total));
                out.add(new Source(Source.Kind.SPAWN, Target.critter(c.species()), where, wt, wt, 2, 5, 0, false,
                        c.species().feature() != null ? c.species().feature().key() : Feature.INVERTEBRATES_WILD.key(), null));
            }
        }
        // M31 part b: the wild turtles (on the shore of swamps and rivers; the tortoise on dry land); caught in a bucket or a box;
        // part c: the amphibians and reptiles (jungles, dry lands, forests), caught in a box
        for (var e : org.overengineer.bettacraft.terrarium.HerpSpawns.TABLE) {
            Where where = switch (e.home()) {
                case SWAMP -> Where.SWAMP;
                case RIVER -> Where.RIVER;
                case DRY_LAND -> Where.DRY_LAND;
                case JUNGLE -> Where.JUNGLE_FLOOR;
                case ARID -> Where.ARID;
                case WOODLAND -> Where.WOODLAND;
            };
            int[] g = org.overengineer.bettacraft.terrarium.HerpSpawns.group(e.species());
            out.add(new Source(Source.Kind.SPAWN, Target.item(e.species().containerId()), where, e.weight(), e.weight(), g[0], g[1], 0, false,
                    org.overengineer.bettacraft.terrarium.HerpSpawns.featureKey(e.species()), null));
        }
        for (var e : WildTables.WILD_CORALS.entries()) {
            out.add(new Source(Source.Kind.SHEARS, Target.plant(e.value()), Where.WARM_OCEAN, 1.0, 1.0, 1, 1, 0, false, Feature.WILD_CORALS.key(),
                    "bettacraft:wild_" + e.value().id()));
        }
    }

    private static Source src(Source.Kind kind, String target, Where where, double chance) {
        return src(kind, target, where, chance, null);
    }

    private static Source src(Source.Kind kind, String target, Where where, double chance, String block) {
        return new Source(kind, target, where, chance, chance, 1, 1, 0, false, null, block);
    }

    // ------------------------------------------------------------------ M47: the aquarist villager

    private static void aquarist(List<Source> out) {
        String key = Feature.AQUARIST_VILLAGER.key();
        for (var o : org.overengineer.bettacraft.villager.AquaristTrades.all()) {
            if (o.buys()) continue;
            for (var p : o.products().get()) {
                boolean dup = false;
                for (Source s : out) if (s.kind() == Source.Kind.AQUARIST && s.target().equals(p.target()) && (int) s.chance() == o.level()) dup = true;
                if (dup) continue;
                out.add(new Source(Source.Kind.AQUARIST, p.target(), null, o.level(), p.share(), p.count(), p.count(), p.emeralds(), false,
                        p.condition() != null ? p.condition() : key, null));
            }
        }
    }

    // ------------------------------------------------------------------ wandering trader

    private static void trader(List<Source> out) {
        for (TraderOffers.Offer o : TraderOffers.all()) {
            Source.Kind kind = o.slot() == TraderOffers.Slot.USUAL ? Source.Kind.TRADER_USUAL : Source.Kind.TRADER_RARE;
            for (TraderOffers.Product p : o.products().get()) {
                // the same product in two offers (two usual tankmate offers): listed once
                boolean dup = false;
                for (Source s : out) {
                    if (s.kind() == kind && s.target().equals(p.target()) && s.min() == p.count() && s.emeralds() == p.emeralds()) dup = true;
                }
                if (dup) continue;
                out.add(new Source(kind, p.target(), null, p.share(), p.share(), p.count(), p.count(), p.emeralds(), p.moreSometimes(), p.condition(), null));
            }
        }
    }
}
