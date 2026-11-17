package org.overengineer.bettacraft.sources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.world.AquaticPlantFishing;

/**
 * M22.4: the plant tables of the net and of fishing as server config lists ({@code "plant_id=weight"}), so modpacks can
 * change them; the defaults are the tables of {@link WildTables}, which the handbook prints. Unknown ids and broken entries
 * are skipped.
 * <p>
 * M28: a list that is still exactly the default of before the new species (a config file written by an older version and
 * never changed) counts as the default, so an updated mod brings up its new plants without editing the config; a list a
 * pack changed stays as it is.
 */
public final class PlantTables {
    private PlantTables() {}

    /** Parsed lists (the config hands out the same list object until it changes). */
    private static final Map<String, Parsed> CACHE = new ConcurrentHashMap<>();

    private record Parsed(List<? extends String> source, WeightedTable<PlantSpecies> table) {}

    public static List<String> toList(WeightedTable<PlantSpecies> table) {
        List<String> out = new ArrayList<>();
        for (var e : table.entries()) out.add(e.value().id() + "=" + e.weight());
        return out;
    }

    public static boolean validEntry(Object o) {
        return o instanceof String s && parse(s) != null;
    }

    /** One entry, or null when it is not "known_plant_id=weight" with a weight of 0 or more. */
    @javax.annotation.Nullable
    static WeightedTable.Entry<PlantSpecies> parse(String s) {
        int eq = s.indexOf('=');
        if (eq <= 0) return null;
        String id = s.substring(0, eq).trim();
        if (id.startsWith("bettacraft:")) id = id.substring("bettacraft:".length());
        PlantSpecies p = PlantSpecies.byIdOrNull(id);
        if (p == null) return null;
        try {
            int w = Integer.parseInt(s.substring(eq + 1).trim());
            return w < 0 ? null : new WeightedTable.Entry<>(p, w);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static WeightedTable<PlantSpecies> fromList(List<? extends String> list) {
        WeightedTable.Builder<PlantSpecies> b = WeightedTable.builder();
        for (String s : list) {
            var e = parse(s);
            if (e != null && e.weight() > 0) b.add(e.value(), e.weight());
        }
        return b.build();
    }

    /** The table of a config list (its default when the config is not loaded). */
    public static WeightedTable<PlantSpecies> configured(ModConfigSpec.ConfigValue<List<? extends String>> value, WeightedTable<PlantSpecies> def) {
        List<? extends String> list;
        try {
            list = value.get();
        } catch (IllegalStateException e) {
            return def;
        }
        String key = value.getPath().toString();
        Parsed p = CACHE.get(key);
        if (p != null && p.source() == list) return p.table();
        WeightedTable<PlantSpecies> t = fromList(list);
        if (sameEntries(t, older(def))) t = def;
        CACHE.put(key, new Parsed(list, t));
        return t;
    }

    /** M28: a default table without the species that came after the config lists (what an older version wrote). */
    public static WeightedTable<PlantSpecies> older(WeightedTable<PlantSpecies> def) {
        WeightedTable.Builder<PlantSpecies> b = WeightedTable.builder();
        for (var e : def.entries()) if (!e.value().newerThanConfigLists()) b.add(e.value(), e.weight());
        return b.build();
    }

    /** The same species with the same weights (the order does not matter). */
    static boolean sameEntries(WeightedTable<PlantSpecies> a, WeightedTable<PlantSpecies> b) {
        if (a.entries().size() != b.entries().size() || a.total() != b.total()) return false;
        Map<PlantSpecies, Integer> m = new java.util.EnumMap<>(PlantSpecies.class);
        for (var e : a.entries()) m.merge(e.value(), e.weight(), Integer::sum);
        for (var e : b.entries()) if (m.merge(e.value(), -e.weight(), Integer::sum) != 0) return false;
        return true;
    }

    /** The plants the net brings up in this water (server config). */
    public static WeightedTable<PlantSpecies> net(NetCatching.Water w) {
        return switch (w) {
            case RIVER -> configured(ServerConfig.NET_PLANTS_RIVER, WildTables.NET_PLANTS_RIVER);
            case JUNGLE -> configured(ServerConfig.NET_PLANTS_JUNGLE, WildTables.NET_PLANTS_JUNGLE);
            case SWAMP -> configured(ServerConfig.NET_PLANTS_SWAMP, WildTables.NET_PLANTS_SWAMP);
            default -> WeightedTable.empty();
        };
    }

    /** The plants fishing brings up in this water (server config). */
    public static WeightedTable<PlantSpecies> fishing(AquaticPlantFishing.Water w) {
        return switch (w) {
            case JUNGLE -> configured(ServerConfig.FISHING_PLANTS_JUNGLE, WildTables.FISHING_PLANTS_JUNGLE);
            case RIVER -> configured(ServerConfig.FISHING_PLANTS_RIVER, WildTables.FISHING_PLANTS_RIVER);
            case SWAMP -> configured(ServerConfig.FISHING_PLANTS_SWAMP, WildTables.FISHING_PLANTS_SWAMP);
            case MANGROVE -> configured(ServerConfig.FISHING_PLANTS_MANGROVE, WildTables.FISHING_PLANTS_MANGROVE);
        };
    }

    /** The default fishing table of a water (handbook). */
    public static WeightedTable<PlantSpecies> fishingDefault(AquaticPlantFishing.Water w) {
        return switch (w) {
            case JUNGLE -> WildTables.FISHING_PLANTS_JUNGLE;
            case RIVER -> WildTables.FISHING_PLANTS_RIVER;
            case SWAMP -> WildTables.FISHING_PLANTS_SWAMP;
            case MANGROVE -> WildTables.FISHING_PLANTS_MANGROVE;
        };
    }
}
