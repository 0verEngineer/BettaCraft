package org.overengineer.bettacraft.villager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.genetics.GeneticsData;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateItems;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M47 part a: what the aquarist pays for a betta or a bred tankmate, by its type and the rarity of its genes (C1). The
 * table is data: {@code data/<ns>/bettacraft/aquarist_prices.json} (ours in {@code data/bettacraft/...}; a datapack of a
 * modpack overrides single values, the last pack wins). Only the visible genes count (the aquarist cannot see hidden ones).
 * <ul>
 *     <li><b>Bettas</b> (grown up, in a bucket or a fish bag): {@code base}, plus {@code bred} for a betta bred in a tank,
 *     {@code per_generation} for every generation up to {@code max_generations}, {@code named_variety} for a named variety
 *     (Mustard Gas, Koi ...), {@code rare_allele} for every visible fin form, pectoral fin, pattern or sheen that is rare
 *     (its frequency weight at most {@code rare_weight}) and every colour that never comes at random (weight 0), {@code long_fins} for long fins, {@code wild_species} for Betta imbellis
 *     or smaragdina; at most {@code max}.</li>
 *     <li><b>Tankmates</b> (grown up, in a fish bag; a bag of several pays for each): only bred ones ({@code only_bred}: wild
 *     caught fish are refused), {@code common} / {@code rare} / {@code other} by the trade group of the species, plus
 *     {@code bred_variety} for a visible bred colour variety, plus the value of the species under {@code species}; at most
 *     {@code max} for one fish, 64 for a bag.</li>
 * </ul>
 */
public final class AquaristPrices {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/aquarist");
    public static final String FILE = "bettacraft/aquarist_prices.json";

    /** The built-in table (also the file in our data). */
    public static final Map<String, Double> DEFAULTS = defaults();
    private static volatile Map<String, Double> table = DEFAULTS;

    private AquaristPrices() {}

    private static Map<String, Double> defaults() {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("betta.base", 2.0);
        m.put("betta.bred", 1.0);
        m.put("betta.per_generation", 0.5);
        m.put("betta.max_generations", 6.0);
        m.put("betta.named_variety", 3.0);
        m.put("betta.rare_allele", 1.0);
        m.put("betta.rare_weight", 3.0);
        m.put("betta.long_fins", 1.0);
        m.put("betta.wild_species", 4.0);
        m.put("betta.max", 24.0);
        m.put("tankmate.only_bred", 1.0);
        m.put("tankmate.common", 1.0);
        m.put("tankmate.rare", 3.0);
        m.put("tankmate.other", 2.0);
        m.put("tankmate.bred_variety", 3.0);
        m.put("tankmate.max", 16.0);
        m.put("species.discus", 5.0);
        m.put("species.zebra_pleco", 5.0);
        m.put("species.angelfish", 2.0);
        m.put("species.lined_seahorse", 4.0);
        m.put("species.dwarf_seahorse", 3.0);
        return java.util.Collections.unmodifiableMap(m);
    }

    public static double get(String key) {
        Double v = table.get(key);
        return v != null ? v : DEFAULTS.getOrDefault(key, 0.0);
    }

    /** The table now in use (GameTests, the handbook). */
    public static Map<String, Double> table() {
        return table;
    }

    /** GameTests: use this table (null: the defaults). */
    public static void setForTest(@Nullable Map<String, Double> t) {
        table = t == null ? DEFAULTS : t;
    }

    /** The table of a JSON file over {@code base}: sections of numbers (booleans count 1 / 0). */
    public static Map<String, Double> parse(JsonObject json, Map<String, Double> base) {
        Map<String, Double> m = new LinkedHashMap<>(base);
        for (var sec : json.entrySet()) {
            if (!sec.getValue().isJsonObject()) continue;
            for (var e : sec.getValue().getAsJsonObject().entrySet()) {
                JsonElement v = e.getValue();
                if (!v.isJsonPrimitive()) continue;
                double d = v.getAsJsonPrimitive().isBoolean() ? (v.getAsBoolean() ? 1 : 0) : v.getAsDouble();
                if (Double.isFinite(d)) m.put(sec.getKey() + "." + e.getKey(), d);
            }
        }
        return m;
    }

    // ------------------------------------------------------------------ the prices

    /** The emeralds for a betta (0: not bought, e.g. a fry or a juvenile). */
    public static int betta(BettaData d) {
        if (!LifeStage.ADULT.id().equals(d.stage()) && !"adult".equals(d.stage())) return 0;
        BettaGenome g = d.genome();
        double p = get("betta.base");
        if (d.generation() > 0) {
            p += get("betta.bred");
            p += get("betta.per_generation") * Math.min(d.generation(), get("betta.max_generations"));
        }
        var variety = GeneticsData.get().variety(g);
        if (variety != null && variety.includeForm()) p += get("betta.named_variety");
        p += get("betta.rare_allele") * rareAlleles(g, get("betta.rare_weight"));
        if (!g.isShortFinned()) p += get("betta.long_fins");
        if (!BettaGenome.SPECIES_BETTA.equals(g.species())) p += get("betta.wild_species");
        return clamp(p, get("betta.max"));
    }

    /** Visible alleles of a betta that are rare (frequency weight at most {@code rareWeight}) and colours of weight 0. */
    public static int rareAlleles(BettaGenome g, double rareWeight) {
        GeneticsData data = GeneticsData.get();
        int n = 0;
        for (String locus : List.of("finform", "pectoral", "pattern", "iridescence")) {
            var a = data.alleles(locus).get(g.value(locus));
            if (a != null && a.weight() <= rareWeight && !(locus.equals("pattern") && "none".equals(g.pattern()))) n++;
        }
        for (String locus : List.of("body_color", "fin_color")) {
            // every colour has the weight 1 by default; a colour a datapack made rare (weight 0: never at random) counts
            var c = data.colors().get(g.value(locus));
            if (c != null && c.weight() <= 0) n++;
        }
        return n;
    }

    /** The emeralds for one tankmate (its bucket data: the fish tag or a tag holding it under "tankmate"). */
    public static int tankmate(CompoundTag tag) {
        CompoundTag fish = tag.contains("tankmate") ? tag.getCompound("tankmate") : tag;
        TankmateSpecies s = TankmateSpecies.byId(fish.getString("species"));
        if (s == null) return 0;
        String stage = fish.getString("stage");
        if (!stage.isEmpty() && !LifeStage.ADULT.id().equals(stage)) return 0;
        int generation = fish.getInt("gen");
        if (generation <= 0 && get("tankmate.only_bred") > 0) return 0;
        double p = switch (s.trade()) {
            case COMMON -> get("tankmate.common");
            case RARE -> get("tankmate.rare");
            default -> get("tankmate.other");
        };
        TankmateGenes genes = TankmateGenes.load(fish.getCompound("genes"), s);
        if (!genes.phenotype(s).equals(s.variety(0))) p += get("tankmate.bred_variety");
        p += get("species." + s.id());
        return clamp(p, get("tankmate.max"));
    }

    private static int clamp(double p, double max) {
        return (int) Math.max(0, Math.min(Math.min(64, max), Math.round(p)));
    }

    /** What the aquarist pays for this bucket or bag (0: he does not take it). */
    public static int price(ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() != 1) return 0;
        if (stack.is(ModItems.BETTA_BUCKET.get())
                || stack.getItem() instanceof FishBagItem && FishBagItem.containedType(stack) == ModEntities.BETTA.get()) {
            BettaData d = BettaBucketItem.data(stack);
            return d == null ? 0 : betta(d);
        }
        if (stack.getItem() instanceof FishBagItem) {
            int sum = 0;
            for (CompoundTag f : TankmateItems.bagFish(stack)) {
                int p = tankmate(f);
                if (p <= 0) return 0; // a bag with one fish he does not want: the whole bag stays
                sum += p;
            }
            return Math.min(64, sum);
        }
        return 0;
    }

    // ------------------------------------------------------------------ the data file


    static final class Listener extends SimplePreparableReloadListener<Map<String, Double>> {
        @Override
        protected Map<String, Double> prepare(ResourceManager rm, ProfilerFiller profiler) {
            Map<String, Double> m = DEFAULTS;
            // ours first, then the other namespaces (a modpack's datapack overrides single values)
            for (String ns : rm.getNamespaces().stream().sorted(java.util.Comparator.comparing((String n) -> !n.equals(BettaCraft.MOD_ID)).thenComparing(n -> n)).toList()) {
                for (Resource r : rm.getResourceStack(ResourceLocation.fromNamespaceAndPath(ns, FILE))) {
                    try (Reader reader = r.openAsReader()) {
                        JsonElement json = JsonParser.parseReader(reader);
                        if (json.isJsonObject()) m = parse(json.getAsJsonObject(), m);
                    } catch (Exception e) {
                        LOGGER.warn("Could not read {}:{}: {}", ns, FILE, e.toString());
                    }
                }
            }
            return m;
        }

        @Override
        protected void apply(Map<String, Double> m, ResourceManager rm, ProfilerFiller profiler) {
            table = java.util.Collections.unmodifiableMap(m);
        }
    }
}
