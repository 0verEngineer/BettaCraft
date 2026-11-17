package org.overengineer.bettacraft.genetics;

import org.overengineer.bettacraft.util.NbtCompat;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;

/**
 * Data-driven genetics (PLAN 5.2, M5): colours, the alleles of the gene loci (dominance rank and frequency) and the
 * named varieties. The built-in defaults come from the generated tables ({@link BettaColorTable},
 * {@link BettaVarietyTable}) and {@link #DEFAULT_GENES}; on a server they are replaced by the datapack files
 * <pre>
 *   data/&lt;ns&gt;/bettacraft/colors/&lt;id&gt;.json     {"color":"#c8202a", "iridescent":false, "weight":1, "rank":..., "name":"..."}
 *   data/&lt;ns&gt;/bettacraft/genes/&lt;locus&gt;.json  {"replace":false, "alleles":{"round":{"rank":80,"weight":10,"name":"..."}, ...}}
 *   data/&lt;ns&gt;/bettacraft/varieties/&lt;id&gt;.json {"priority":65, "include_form":true, "when":{"body_color":[...], ...},
 *                                             "example":{...}, "name":"...", "disabled":false}
 * </pre>
 * and sent to every client ({@link #toNbt()}). Ids of the {@code bettacraft} namespace are plain ({@code red}), other
 * namespaces keep theirs ({@code mypack:teal_glow}). Pure logic, no Minecraft bootstrap needed (unit tests).
 * <p>
 * Datapacks cannot add new fin models or pattern masks: unknown tail shapes render as roundtail, unknown patterns are
 * not drawn (but both are named and inherited normally).
 */
public final class GeneticsData {
    public record ColorDef(String id, int rgb, Integer rank, boolean iridescent, int weight, @Nullable String name) {}

    public record AlleleDef(String id, int rank, int weight, @Nullable String name) {}

    public record VarietyDef(String id, int priority, boolean includeForm, Map<String, Set<String>> conditions,
                             Map<String, String> example, @Nullable String name) {
        public boolean matches(BettaGenome g) {
            for (Map.Entry<String, Set<String>> c : conditions.entrySet()) {
                String v = g.value(c.getKey());
                if (v == null || !c.getValue().contains(v)) return false;
            }
            return true;
        }
    }

    /** Built-in alleles per gene locus: id, dominance rank, frequency weight (0 = never chosen at random). */
    public static final Map<String, List<AlleleDef>> DEFAULT_GENES = defaultGenes();

    private static Map<String, List<AlleleDef>> defaultGenes() {
        Map<String, List<AlleleDef>> m = new LinkedHashMap<>();
        m.put("finform", List.of(a("round", 80, 10), a("spade", 70, 6), a("veil", 60, 8), a("delta", 50, 6), a("halfmoon", 40, 8),
                a("rosetail", 35, 3), a("crowntail", 30, 6), a("doubletail", 20, 4)));
        m.put("fin_length", List.of(a("short", 10, 5), a("long", 0, 5)));
        m.put("pectoral", List.of(a("normal", 10, 5), a("dumbo", 0, 2)));
        m.put("pattern", List.of(a("marble", 90, 5), a("dragon", 80, 5), a("mask", 70, 4), a("koi", 60, 5), a("galaxy", 55, 4),
                a("grizzle", 50, 4), a("piebald", 45, 3), a("dalmatian", 40, 3), a("butterfly", 30, 6), a("none", 20, 24),
                a("fade", 10, 4), a("bars", 10, 0), a("tipped", 10, 4)));
        m.put("iridescence", List.of(a("high", 20, 3), a("low", 10, 4), a("none", 0, 3)));
        return Collections.unmodifiableMap(m);
    }

    private static AlleleDef a(String id, int rank, int weight) {
        return new AlleleDef(id, rank, weight, null);
    }

    // ------------------------------------------------------------------ snapshot

    private final Map<String, ColorDef> colors;
    private final Map<String, Map<String, AlleleDef>> genes;
    private final List<VarietyDef> varieties;
    // caches
    private final List<String> colorPool, sheenPool;
    private final Map<String, List<String>> genePools = new LinkedHashMap<>();

    private GeneticsData(Map<String, ColorDef> colors, Map<String, Map<String, AlleleDef>> genes, List<VarietyDef> varieties) {
        this.colors = colors;
        this.genes = genes;
        List<VarietyDef> v = new ArrayList<>(varieties);
        v.sort(Comparator.comparingInt(VarietyDef::priority).reversed().thenComparing(VarietyDef::id));
        this.varieties = List.copyOf(v);
        this.colorPool = colors.values().stream().filter(c -> c.weight() > 0).map(ColorDef::id).toList();
        this.sheenPool = colors.values().stream().filter(c -> c.weight() > 0 && c.iridescent()).map(ColorDef::id).toList();
        genes.forEach((k, m) -> genePools.put(k, m.values().stream().filter(x -> x.weight() > 0).map(AlleleDef::id).toList()));
    }

    private static volatile GeneticsData current = defaults();

    public static GeneticsData get() {
        return current;
    }

    public static void set(GeneticsData d) {
        current = d;
    }

    /** Back to the built-in defaults (client disconnect). */
    public static void reset() {
        current = defaults();
    }

    public static GeneticsData defaults() {
        Map<String, ColorDef> c = new LinkedHashMap<>();
        for (BettaColorTable.Entry e : BettaColorTable.COLORS) {
            c.put(e.id(), new ColorDef(e.id(), e.rgb(), null, BettaColorTable.IRIDESCENT.contains(e.id()), 1, null));
        }
        Map<String, Map<String, AlleleDef>> g = new LinkedHashMap<>();
        DEFAULT_GENES.forEach((k, list) -> {
            Map<String, AlleleDef> m = new LinkedHashMap<>();
            for (AlleleDef d : list) m.put(d.id(), d);
            g.put(k, m);
        });
        List<VarietyDef> v = new ArrayList<>();
        for (BettaVarietyTable.Entry e : BettaVarietyTable.VARIETIES) {
            Map<String, Set<String>> cond = new LinkedHashMap<>();
            e.conditions().forEach((k, l) -> cond.put(k, Set.copyOf(l)));
            v.add(new VarietyDef(e.id(), e.priority(), e.includeForm(), cond, Map.copyOf(e.example()), null));
        }
        return new GeneticsData(c, g, v);
    }

    // ------------------------------------------------------------------ queries

    public Map<String, ColorDef> colors() {
        return Collections.unmodifiableMap(colors);
    }

    public Map<String, AlleleDef> alleles(String locus) {
        return Collections.unmodifiableMap(genes.getOrDefault(locus, Map.of()));
    }

    public List<VarietyDef> varieties() {
        return varieties;
    }

    @Nullable
    public VarietyDef variety(BettaGenome g) {
        for (VarietyDef v : varieties) if (v.matches(g)) return v;
        return null;
    }

    public boolean isColor(String id) {
        return colors.containsKey(id);
    }

    public int rgb(String id, int fallback) {
        ColorDef c = colors.get(id);
        return c == null ? fallback : c.rgb();
    }

    public int rank(Locus l, String allele) {
        if (l.kind() == Locus.Kind.GENE) {
            AlleleDef d = genes.getOrDefault(l.id(), Map.of()).get(allele);
            return d == null ? -1 : d.rank();
        }
        ColorDef c = colors.get(allele);
        if (c == null) return -1;
        if (c.rank() != null) return c.rank();
        int rgb = c.rgb();
        double luma = 0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF);
        return 1000 - (int) Math.round(luma);
    }

    public List<String> pool(Locus l) {
        return switch (l.kind()) {
            case GENE -> genePools.getOrDefault(l.id(), List.of());
            case COLOR -> colorPool;
            case SHEEN -> sheenPool;
        };
    }

    private int weight(Locus l, String allele) {
        if (l.kind() == Locus.Kind.GENE) {
            AlleleDef d = genes.getOrDefault(l.id(), Map.of()).get(allele);
            return d == null ? 0 : d.weight();
        }
        ColorDef c = colors.get(allele);
        return c == null ? 0 : c.weight();
    }

    /** Weighted random allele of the locus pool (falls back to the built-in default allele). */
    public String pick(Locus l, RandomSource r) {
        List<String> pool = pool(l);
        if (pool.isEmpty()) return BettaGenotype.phenotypeValue(BettaGenome.DEFAULT, l);
        int total = 0;
        for (String s : pool) total += weight(l, s);
        if (total <= 0) return pool.get(r.nextInt(pool.size()));
        int x = r.nextInt(total);
        for (String s : pool) {
            x -= weight(l, s);
            if (x < 0) return s;
        }
        return pool.getLast();
    }

    // ------------------------------------------------------------------ names

    /** Translation key of an allele: {@code <prefix>.<namespace>.<path>} (prefix per locus). */
    public static String translationKey(Locus l, String allele) {
        String prefix = switch (l.id()) {
            case "finform" -> "shape";
            case "fin_length" -> "fin_length";
            case "pectoral" -> "pectoral";
            case "pattern" -> "pattern";
            case "iridescence" -> "iridescence";
            default -> "color";
        };
        return prefix + "." + nsPath(allele);
    }

    /** "red" -> "bettacraft.red", "mypack:glow" -> "mypack.glow". */
    public static String nsPath(String id) {
        int i = id.indexOf(':');
        return i < 0 ? "bettacraft." + id : id.substring(0, i) + "." + id.substring(i + 1);
    }

    /** Name of an allele; datapack entries without a translation use their "name" (or the id). */
    public MutableComponent name(Locus l, String allele) {
        String fallback = null;
        if (l.kind() == Locus.Kind.GENE) {
            AlleleDef d = genes.getOrDefault(l.id(), Map.of()).get(allele);
            if (d != null) fallback = d.name();
        } else {
            ColorDef c = colors.get(allele);
            if (c != null) fallback = c.name();
        }
        return Component.translatableWithFallback(translationKey(l, allele), fallback != null ? fallback : allele);
    }

    public static String varietyKey(String id) {
        return "variety." + nsPath(id);
    }

    // ------------------------------------------------------------------ datapack JSON

    /** Builds the data from datapack files; categories without any file keep the built-in defaults. */
    public static GeneticsData fromJson(Map<String, JsonElement> colorFiles, Map<String, JsonElement> geneFiles,
                                        Map<String, JsonElement> varietyFiles, java.util.function.Consumer<String> warn) {
        GeneticsData def = defaults();
        Map<String, ColorDef> colors = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : colorFiles.entrySet()) {
            try {
                JsonObject o = e.getValue().getAsJsonObject();
                int w = o.has("weight") ? o.get("weight").getAsInt() : 1;
                if (o.has("disabled") && o.get("disabled").getAsBoolean()) continue;
                colors.put(e.getKey(), new ColorDef(e.getKey(), parseRgb(o.get("color").getAsString()),
                        o.has("rank") ? o.get("rank").getAsInt() : null, o.has("iridescent") && o.get("iridescent").getAsBoolean(), w,
                        o.has("name") ? o.get("name").getAsString() : null));
            } catch (RuntimeException ex) {
                warn.accept("colour " + e.getKey() + ": " + ex.getMessage());
            }
        }
        if (colors.isEmpty()) colors.putAll(def.colors);
        Map<String, Map<String, AlleleDef>> genes = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : geneFiles.entrySet()) {
            String locus = e.getKey().contains(":") ? e.getKey().substring(e.getKey().indexOf(':') + 1) : e.getKey();
            if (BettaLoci.byId(locus) == null || BettaLoci.byId(locus).kind() != Locus.Kind.GENE) {
                warn.accept("genes " + e.getKey() + ": unknown gene locus " + locus);
                continue;
            }
            try {
                JsonObject o = e.getValue().getAsJsonObject();
                Map<String, AlleleDef> m = genes.computeIfAbsent(locus, k -> new LinkedHashMap<>());
                if (o.has("replace") && o.get("replace").getAsBoolean()) m.clear();
                for (Map.Entry<String, JsonElement> al : o.getAsJsonObject("alleles").entrySet()) {
                    JsonObject ao = al.getValue().getAsJsonObject();
                    m.put(al.getKey(), new AlleleDef(al.getKey(), ao.get("rank").getAsInt(), ao.has("weight") ? ao.get("weight").getAsInt() : 1,
                            ao.has("name") ? ao.get("name").getAsString() : null));
                }
            } catch (RuntimeException ex) {
                warn.accept("genes " + e.getKey() + ": " + ex.getMessage());
            }
        }
        for (Locus l : BettaLoci.GENES) {
            if (!genes.containsKey(l.id()) || genes.get(l.id()).isEmpty()) genes.put(l.id(), new LinkedHashMap<>(def.genes.get(l.id())));
        }
        List<VarietyDef> varieties = new ArrayList<>();
        for (Map.Entry<String, JsonElement> e : varietyFiles.entrySet()) {
            try {
                JsonObject o = e.getValue().getAsJsonObject();
                if (o.has("disabled") && o.get("disabled").getAsBoolean()) continue;
                Map<String, Set<String>> cond = new LinkedHashMap<>();
                if (o.has("when")) {
                    for (Map.Entry<String, JsonElement> c : o.getAsJsonObject("when").entrySet()) {
                        if (BettaLoci.byId(c.getKey()) == null) throw new IllegalArgumentException("unknown locus " + c.getKey());
                        Set<String> s = new LinkedHashSet<>();
                        if (c.getValue().isJsonArray()) c.getValue().getAsJsonArray().forEach(x -> s.add(x.getAsString()));
                        else s.add(c.getValue().getAsString());
                        cond.put(c.getKey(), Set.copyOf(s));
                    }
                }
                Map<String, String> example = new LinkedHashMap<>();
                if (o.has("example")) o.getAsJsonObject("example").entrySet().forEach(x -> example.put(x.getKey(), x.getValue().getAsString()));
                varieties.add(new VarietyDef(e.getKey(), o.has("priority") ? o.get("priority").getAsInt() : 0,
                        !o.has("include_form") || o.get("include_form").getAsBoolean(), cond, example,
                        o.has("name") ? o.get("name").getAsString() : null));
            } catch (RuntimeException ex) {
                warn.accept("variety " + e.getKey() + ": " + ex.getMessage());
            }
        }
        if (varietyFiles.isEmpty()) varieties.addAll(def.varieties);
        return new GeneticsData(colors, genes, varieties);
    }

    static int parseRgb(String s) {
        String h = s.startsWith("#") ? s.substring(1) : s.startsWith("0x") ? s.substring(2) : s;
        if (h.length() != 6) throw new IllegalArgumentException("colour must be #rrggbb: " + s);
        return Integer.parseInt(h, 16);
    }

    // ------------------------------------------------------------------ JSON export (datagen) and sync NBT

    public static JsonObject colorJson(ColorDef c) {
        JsonObject o = new JsonObject();
        o.addProperty("color", String.format(java.util.Locale.ROOT, "#%06x", c.rgb()));
        o.addProperty("iridescent", c.iridescent());
        o.addProperty("weight", c.weight());
        if (c.rank() != null) o.addProperty("rank", c.rank());
        return o;
    }

    public static JsonObject genesJson(List<AlleleDef> alleles) {
        JsonObject o = new JsonObject();
        o.addProperty("replace", false);
        JsonObject al = new JsonObject();
        for (AlleleDef d : alleles) {
            JsonObject x = new JsonObject();
            x.addProperty("rank", d.rank());
            x.addProperty("weight", d.weight());
            al.add(d.id(), x);
        }
        o.add("alleles", al);
        return o;
    }

    public static JsonObject varietyJson(VarietyDef v) {
        JsonObject o = new JsonObject();
        o.addProperty("priority", v.priority());
        o.addProperty("include_form", v.includeForm());
        JsonObject when = new JsonObject();
        for (String locus : orderedLoci(v.conditions().keySet())) {
            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            v.conditions().get(locus).stream().sorted().forEach(arr::add);
            when.add(locus, arr);
        }
        o.add("when", when);
        JsonObject ex = new JsonObject();
        for (String locus : orderedLoci(v.example().keySet())) ex.addProperty(locus, v.example().get(locus));
        o.add("example", ex);
        return o;
    }

    private static List<String> orderedLoci(Set<String> keys) {
        List<String> out = new ArrayList<>();
        for (Locus l : BettaLoci.ALL) if (keys.contains(l.id())) out.add(l.id());
        for (String k : keys) if (!out.contains(k)) out.add(k);
        return out;
    }

    public CompoundTag toNbt() {
        CompoundTag t = new CompoundTag();
        ListTag cl = new ListTag();
        for (ColorDef c : colors.values()) {
            CompoundTag x = new CompoundTag();
            x.putString("id", c.id());
            x.putInt("rgb", c.rgb());
            if (c.rank() != null) x.putInt("rank", c.rank());
            x.putBoolean("iri", c.iridescent());
            x.putInt("w", c.weight());
            if (c.name() != null) x.putString("name", c.name());
            cl.add(x);
        }
        t.put("colors", cl);
        CompoundTag gt = new CompoundTag();
        genes.forEach((locus, m) -> {
            ListTag l = new ListTag();
            for (AlleleDef d : m.values()) {
                CompoundTag x = new CompoundTag();
                x.putString("id", d.id());
                x.putInt("rank", d.rank());
                x.putInt("w", d.weight());
                if (d.name() != null) x.putString("name", d.name());
                l.add(x);
            }
            gt.put(locus, l);
        });
        t.put("genes", gt);
        ListTag vl = new ListTag();
        for (VarietyDef v : varieties) {
            CompoundTag x = new CompoundTag();
            x.putString("id", v.id());
            x.putInt("prio", v.priority());
            x.putBoolean("form", v.includeForm());
            CompoundTag when = new CompoundTag();
            v.conditions().forEach((k, s) -> {
                ListTag l = new ListTag();
                s.stream().sorted().forEach(a -> l.add(StringTag.valueOf(a)));
                when.put(k, l);
            });
            x.put("when", when);
            CompoundTag ex = new CompoundTag();
            v.example().forEach(ex::putString);
            x.put("example", ex);
            if (v.name() != null) x.putString("name", v.name());
            vl.add(x);
        }
        t.put("varieties", vl);
        return t;
    }

    public static GeneticsData fromNbt(CompoundTag t) {
        Map<String, ColorDef> colors = new LinkedHashMap<>();
        ListTag cl = t.getListOrEmpty("colors");
        for (int i = 0; i < cl.size(); i++) {
            CompoundTag x = cl.getCompoundOrEmpty(i);
            colors.put(x.getStringOr("id", ""), new ColorDef(x.getStringOr("id", ""), x.getIntOr("rgb", 0), x.contains("rank") ? x.getIntOr("rank", 0) : null,
                    x.getBooleanOr("iri", false), x.getIntOr("w", 0), x.contains("name") ? x.getStringOr("name", "") : null));
        }
        Map<String, Map<String, AlleleDef>> genes = new LinkedHashMap<>();
        CompoundTag gt = t.getCompoundOrEmpty("genes");
        for (String locus : gt.keySet()) {
            Map<String, AlleleDef> m = new LinkedHashMap<>();
            ListTag l = gt.getListOrEmpty(locus);
            for (int i = 0; i < l.size(); i++) {
                CompoundTag x = l.getCompoundOrEmpty(i);
                m.put(x.getStringOr("id", ""), new AlleleDef(x.getStringOr("id", ""), x.getIntOr("rank", 0), x.getIntOr("w", 0),
                        x.contains("name") ? x.getStringOr("name", "") : null));
            }
            genes.put(locus, m);
        }
        List<VarietyDef> varieties = new ArrayList<>();
        ListTag vl = t.getListOrEmpty("varieties");
        for (int i = 0; i < vl.size(); i++) {
            CompoundTag x = vl.getCompoundOrEmpty(i);
            Map<String, Set<String>> cond = new LinkedHashMap<>();
            CompoundTag when = x.getCompoundOrEmpty("when");
            for (String k : when.keySet()) {
                Set<String> s = new LinkedHashSet<>();
                ListTag l = when.getListOrEmpty(k);
                for (int j = 0; j < l.size(); j++) s.add(l.getStringOr(j, ""));
                cond.put(k, Set.copyOf(s));
            }
            Map<String, String> ex = new LinkedHashMap<>();
            CompoundTag et = x.getCompoundOrEmpty("example");
            for (String k : et.keySet()) ex.put(k, et.getStringOr(k, ""));
            varieties.add(new VarietyDef(x.getStringOr("id", ""), x.getIntOr("prio", 0), x.getBooleanOr("form", false), cond, ex,
                    x.contains("name") ? x.getStringOr("name", "") : null));
        }
        GeneticsData def = defaults();
        if (colors.isEmpty()) colors.putAll(def.colors);
        for (Locus l : BettaLoci.GENES) if (!genes.containsKey(l.id())) genes.put(l.id(), new LinkedHashMap<>(def.genes.get(l.id())));
        return new GeneticsData(colors, genes, varieties);
    }
}
