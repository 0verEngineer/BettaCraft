package org.overengineer.bettacraft.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.terrarium.HerpSpecies;

/**
 * M33: which features a thing of Bettacraft belongs to: an item (by its id path), a recipe (its result, its type, a few
 * by id) and the targets of the sources ({@code tankmate:<id>}, {@code critter:<id>} ...). All of them must be on for it
 * to be made or given (recipe conditions, trader, loot; part b: creative tab, JEI, handbook). An empty list is the core of
 * the mod (tanks, heaters, filters, the net, bags), which is always there. Works on ids only (no registry), so datagen and
 * the unit tests can use it.
 */
public final class FeatureContent {
    private FeatureContent() {}

    private static final Map<String, List<Feature>> ITEMS = new HashMap<>();

    private static void put(Feature f, String... paths) {
        for (String p : paths) ITEMS.merge(p, List.of(f), (a, b) -> {
            List<Feature> l = new ArrayList<>(a);
            l.addAll(b);
            return List.copyOf(l);
        });
    }

    static {
        put(Feature.BETTAS, "betta_bucket", "betta_spawn_egg");
        put(Feature.FROGS, "dwarf_frog_bucket", "dwarf_frog_spawn_egg");
        put(Feature.BREEDING, "breeding_box", "spawning_cave", "spawning_mop", "fry_powder", "decor_spawning_cave", "decor_spawning_mop");
        put(Feature.MAGNIFIER, "magnifier");
        put(Feature.INVERTEBRATES, "critter_bucket", "shrimp_food", "shell_food");
        put(Feature.ALGAE, "algae_scraper");
        put(Feature.SALTWATER, "sea_water_bucket", "sea_salt", "salt_water_bottle", "calcium_additive", "protein_skimmer", "reef_rock", "live_rock");
        put(Feature.REEF_DOSING, "dosing_pump", "dosing_canister", "calcium_canister", "alkalinity_canister", "magnesium_canister", "trace_canister");
        put(Feature.CLIP_FEEDER, "clip_feeder");
        put(Feature.LED_TIERS, "led_panel", "high_power_led");
        put(Feature.MORE_INTERNAL_FILTERS, "sponge_filter", "corner_filter", "intake_sponge");
        put(Feature.AERATION, "air_stone", "air_pump", "air_hose");
        put(Feature.MULTIBLOCK_FILTER, "filter_chamber", "filter_pump_head", "filter_inlet", "filter_outlet", "filter_foam", "ceramic_rings",
                "activated_carbon", "filter_peat");
        put(Feature.STAINLESS_STEEL, "crucible", "stainless_steel_ingot");
        put(Feature.PIPES, "glass_pipe", "stainless_steel_pipe", "water_pump", "reservoir");
        put(Feature.TOP_OFF, "level_sensor");
        put(Feature.AUTO_WATER_CHANGE, "water_change_controller");
        put(Feature.INTERIOR_PIPES, "bulkhead", "interior_pipe", "intake_strainer", "lily_pipe", "nozzle", "spray_bar");
        put(Feature.FILTER_LINES, "filter_inlet_pipe", "filter_outlet_pipe", "pipe_tee", "intake_guard", "outlet_guard"); // M40
        put(Feature.FILTER_WATER, "filter_fresh_pipe", "filter_waste_pipe", "filter_fresh_port", "filter_waste_port"); // M40 part b
        put(Feature.PIPE_COVERS, "pipe_cover"); // M40 part c
        put(Feature.WATER_CHEMISTRY, "tds_meter", "ph_test_kit", "remineralising_salt", "almond_leaf", "alder_cone", "active_soil");
        put(Feature.CO2, "co2_solenoid", "bio_co2", "co2_cylinder", "co2_station");
        put(Feature.OSMOSIS, "ro_membrane", "ro_prefilter", "ro_unit", "ro_water_bucket");
        put(Feature.LIVE_FOOD, "mosquito_larvae", "infusoria", "artemia_nauplii", "infusoria_culture", "artemia_culture", "artemia_cysts", "larva_tub",
                "artemia_hatchery", "frozen_mosquito_larvae", "frozen_artemia", "frozen_daphnia", "crickets", "fruit_flies", "cricket_culture",
                "fruit_fly_culture");
        put(Feature.LIVE_FOOD_CULTURES, "tubifex", "daphnia", "mulm", "yeast", "green_water_bottle", "green_water_culture", "tubifex_dish", "daphnia_barrel");
        put(Feature.LIVE_FOOD_PUMP, "live_food_pump");
        put(Feature.LARVA_REARING, "phytoplankton", "rotifers", "phytoplankton_culture", "rotifer_culture"); // M48 part b
        put(Feature.SUMP, "sump"); // M49 part a
        put(Feature.REFUGIUM, "chaetomorpha");
        put(Feature.CALCIUM_REACTOR, "calcium_reactor", "coral_rubble");
        put(Feature.MANGROVES, "decor_mangrove_stilt_roots"); // M49 part b
        put(Feature.FOOD_MACHINE, "food_maker", "fish_flakes", "food_granules", "shrimp_food", "shell_food", "frozen_mosquito_larvae", "frozen_artemia",
                "frozen_daphnia");
        put(Feature.FERTILIZER, "fertilizer_maker", "kelp_meal", "nutrient_salt", "liquid_fertilizer"); // M37 (the crank is core)
        put(Feature.FERTILIZER_DOSING, "fertilizer_canister");
        put(Feature.MONITORS, "monitor");
        put(Feature.BIG_ANIMALS, "large_net", "transport_tub", "transport_tub_filled", "steel_leader");
        put(Feature.TERRARIUM_DEVICES, "heat_mat", "basking_lamp", "mister", "thermo_hygrometer");
        put(Feature.AQUARIST_VILLAGER, "breeding_basin"); // M47
        put(Feature.HERPS, "terrarium_box");
        put(Feature.MOBS_IN_GLASS, "mob_jar", "mob_jar_filled");
        put(Feature.CREATE_DRIVE, "drive_adapter"); // M41 (only registered with Create)
    }

    /** The features of a Bettacraft item ({@code neon_tetra_bucket}); empty = always there. */
    public static List<Feature> ofItem(String path) {
        Set<Feature> out = new LinkedHashSet<>(ITEMS.getOrDefault(path, List.of()));
        if (path.startsWith("decor_")) out.add(Feature.DECOR);
        for (String suffix : new String[]{"_bucket", "_spawn_egg", "_box"}) {
            if (!path.endsWith(suffix)) continue;
            String id = path.substring(0, path.length() - suffix.length());
            TankmateSpecies t = TankmateSpecies.byId(id);
            if (t != null) out.add(t.feature());
            for (HerpSpecies h : HerpSpecies.ALL) if (h.id().equals(id)) out.add(h.feature()); // M49: the mudskipper its own
        }
        String plant = path.startsWith("wild_") ? path.substring(5) : path;
        PlantSpecies p = PlantSpecies.byIdOrNull(plant);
        if (p != null) out.add(p.marine() ? Feature.CORALS : Feature.AQUATIC_PLANTS);
        FloatingPlantType f = FloatingPlantType.byIdOrNull(path);
        if (f != null && p == null) out.add(f.plantItem() ? Feature.AQUATIC_PLANTS : Feature.FLOATING_PLANTS);
        return minimal(out);
    }

    /** The features of a source target ({@link org.overengineer.bettacraft.sources.Target}). */
    public static List<Feature> ofTarget(String target) {
        if (target.equals("betta")) return List.of(Feature.BETTAS);
        if (target.startsWith("betta:")) return List.of(Feature.WILD_BETTAS);
        if (target.equals("frog")) return List.of(Feature.FROGS);
        int c = target.indexOf(':');
        if (c < 0) return List.of();
        String kind = target.substring(0, c), id = target.substring(c + 1);
        return switch (kind) {
            case "tankmate" -> {
                TankmateSpecies s = TankmateSpecies.byId(id);
                yield s == null ? List.of() : List.of(s.feature());
            }
            case "critter" -> {
                CritterSpecies s = critter(id);
                yield s == null ? List.of() : ofCritter(s);
            }
            case "plant", "floating", "item" -> ofItem(id);
            default -> List.of();
        };
    }

    /** The features of a small animal: the small animals, salt water for the reef ones, its own switch. */
    public static List<Feature> ofCritter(CritterSpecies s) {
        Set<Feature> out = new LinkedHashSet<>();
        out.add(Feature.INVERTEBRATES);
        if (s.marine()) out.add(Feature.SALTWATER);
        if (s.feature() != null) out.add(s.feature());
        return minimal(out);
    }

    /**
     * The features of a recipe of ours: its result item, its type (the food maker, the crucible) and a few by id (the stone
     * cabinet, the coral frags of vanilla corals).
     */
    public static List<Feature> ofRecipe(String id, @Nullable String resultPath, @Nullable String type) {
        Set<Feature> out = new LinkedHashSet<>();
        if (resultPath != null) out.addAll(ofItem(resultPath));
        if (type != null) {
            if (type.equals("bettacraft:food_maker")) out.add(Feature.FOOD_MACHINE);
            if (type.equals("bettacraft:crucible")) out.add(Feature.STAINLESS_STEEL);
        }
        if (id.equals("stone_cabinet")) out.add(Feature.STONE_CABINETS);
        if (id.startsWith("frag_from_")) out.add(Feature.CORALS);
        return minimal(out);
    }

    /** Leaves out the features already implied by another one of the list (its parents and requirements). */
    public static List<Feature> minimal(Iterable<Feature> features) {
        Set<Feature> all = new LinkedHashSet<>();
        for (Feature f : features) all.add(f);
        List<Feature> out = new ArrayList<>();
        for (Feature f : all) {
            boolean implied = false;
            for (Feature g : all) if (g != f && implies(g, f)) implied = true;
            if (!implied) out.add(f);
        }
        return List.copyOf(out);
    }

    /** {@code a} on means {@code b} on (b is a parent or requirement of a, at any depth). */
    public static boolean implies(Feature a, Feature b) {
        if (a.parent() != null && (a.parent() == b || implies(a.parent(), b))) return true;
        for (Feature r : a.requires()) if (r == b || implies(r, b)) return true;
        return false;
    }

    @Nullable
    private static CritterSpecies critter(String id) {
        for (CritterSpecies s : CritterSpecies.values()) if (s.id().equals(id)) return s;
        return null;
    }
}
