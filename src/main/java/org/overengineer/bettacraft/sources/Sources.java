package org.overengineer.bettacraft.sources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.overengineer.bettacraft.config.CommonConfig;
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
        world(out);
        trader(out);
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
        double chance = def(ServerConfig.CRITTERS_NET_CHANCE), tank = def(ServerConfig.TANKMATES_NET_SHARE), reef = def(ServerConfig.REEF_NET_SHARE);
        for (NetCatching.Water w : NetCatching.Water.values()) {
            if (w == NetCatching.Water.NONE) continue;
            boolean[] lushes = w == NetCatching.Water.CAVE ? new boolean[]{false, true} : new boolean[]{false};
            for (boolean lush : lushes) {
                Where where = lush ? Where.LUSH_CAVES : NET_WATERS.get(w);
                // the lowest chance (no plants, soft bottom) and the highest of the four combinations
                Map<Object, Double> base = NetCatching.odds(w, false, false, lush, chance, tank, reef);
                Map<Object, Double> best = new java.util.LinkedHashMap<>(base);
                for (boolean plants : new boolean[]{false, true}) {
                    for (boolean hard : new boolean[]{false, true}) {
                        NetCatching.odds(w, plants, hard, lush, chance, tank, reef).forEach((k, v) -> best.merge(k, v, Math::max));
                    }
                }
                for (var e : best.entrySet()) {
                    double lo = base.getOrDefault(e.getKey(), 0.0);
                    String target = key(e.getKey());
                    int[] n = netCount(e.getKey());
                    String cond = e.getKey() instanceof TankmateSpecies s && s.seahorse() ? "features.seahorses"
                            : e.getKey() == CritterSpecies.YELLOW_NERITE ? "features.snailColors" : null;
                    out.add(new Source(Source.Kind.NET, target, where, lo, e.getValue(), n[0], n[1], 0, false, cond, null));
                }
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
                        || s == CritterSpecies.YELLOW_NERITE ? new int[]{1, 1} : new int[]{1, 2};
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
        for (var e : WildTables.FISHING_PLANTS_TROPICAL.entries()) {
            out.add(src(Source.Kind.FISHING, Target.plant(e.value()), Where.JUNGLE_OR_RIVER, aquatic * WildTables.FISHING_PLANTS_TROPICAL.share(e.value())));
        }
        for (var e : WildTables.FISHING_PLANTS_SWAMP.entries()) {
            out.add(src(Source.Kind.FISHING, Target.plant(e.value()), Where.SWAMP_OR_MANGROVE,
                    (1 - floating) * aquatic * WildTables.FISHING_PLANTS_SWAMP.share(e.value())));
        }
    }

    // ------------------------------------------------------------------ bottles, shears, shovel, blocks, spawns

    private static void world(List<Source> out) {
        out.add(src(Source.Kind.BOTTLE, Target.item("swamp_water_bottle"), Where.SWAMP, 1.0));
        out.add(src(Source.Kind.BOTTLE, Target.item("salt_water_bottle"), Where.SALT_WATER, 1.0));
        double duck = def(ServerConfig.PLANTS_BOTTLE_CHANCE), cut = def(ServerConfig.AQUATIC_BOTTLE_CHANCE);
        out.add(src(Source.Kind.BOTTLE, Target.floating(FloatingPlantType.DUCKWEED), Where.SWAMP, duck));
        for (var e : WildTables.FISHING_PLANTS_SWAMP.entries()) {
            out.add(src(Source.Kind.BOTTLE, Target.plant(e.value()), Where.SWAMP, (1 - duck) * cut * WildTables.FISHING_PLANTS_SWAMP.share(e.value())));
        }
        out.add(src(Source.Kind.SHEARS, Target.plant(PlantSpecies.JAVA_MOSS), Where.NEAR_WATER, def(ServerConfig.AQUATIC_MOSS_CHANCE), "minecraft:moss_block"));
        out.add(src(Source.Kind.SHOVEL, Target.critter(CritterSpecies.TRUMPET_SNAIL), Where.SWAMP, def(ServerConfig.CRITTERS_MUD_CHANCE), "minecraft:mud"));
        out.add(src(Source.Kind.SHOVEL, Target.critter(CritterSpecies.TRUMPET_SNAIL), Where.MANGROVE, def(ServerConfig.CRITTERS_MUD_CHANCE), "minecraft:mud"));
        out.add(src(Source.Kind.BLOCK_DROP, Target.item("mosquito_larvae"), Where.SWAMP, WildTables.MUD_LARVAE_CHANCE, "minecraft:mud"));
        out.add(src(Source.Kind.BLOCK_DROP, Target.item("artemia_cysts"), Where.SAND, def(ServerConfig.DROPS_ARTEMIA_FROM_SAND_CHANCE), "#c:sands"));
        int weight = def(CommonConfig.SPAWN_WEIGHT), min = def(CommonConfig.SPAWN_MIN_GROUP), max = Math.max(min, def(CommonConfig.SPAWN_MAX_GROUP));
        out.add(new Source(Source.Kind.SPAWN, Target.BETTA, Where.SWAMP, weight, weight, min, max, 0, false, null, null));
    }

    private static Source src(Source.Kind kind, String target, Where where, double chance) {
        return src(kind, target, where, chance, null);
    }

    private static Source src(Source.Kind kind, String target, Where where, double chance, String block) {
        return new Source(kind, target, where, chance, chance, 1, 1, 0, false, null, block);
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
