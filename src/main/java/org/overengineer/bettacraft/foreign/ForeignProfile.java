package org.overengineer.bettacraft.foreign;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import org.overengineer.bettacraft.chem.ChemTolerance;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.terrarium.TerrariumNeeds;

/**
 * M32 part a (question A): how an animal of another mod (or of vanilla) lives in a Bettacraft tank: our care needs
 * (hunger, temperature, water quality, salinity, pH/TDS, stress from overcrowding, sickness and death) and our breeding
 * in place of its own. One profile comes from one datapack file {@code data/<ns>/bettacraft/foreign_animals/<name>.json}
 * (loaded by {@link ForeignProfiles}) and can stand for several entity types. Pure data, parsed by {@link #parse}
 * (unit-tested).
 * <p>
 * A profile can take its values from one of our species ({@code "species": "guppy"}: temperature and breeding range,
 * water quality, salinity, pH/TDS, labyrinth, breeding type and brood size); every field given in the file wins over it.
 *
 * @param entities  the entity types it stands for (types that are not registered are skipped: their mod is missing)
 * @param required  warn about missing entity types (default false: a profile for a mod that is not installed is quietly skipped)
 * @param group     a label (handbook, logs): {@code livebearer}, {@code gourami}, {@code pond_fish}, {@code food_fish} ...
 * @param species   the id of our species it borrows its values from, or null
 * @param tempMin   lowest water temperature it bears (°C)
 * @param tempMax   highest water temperature it bears (°C)
 * @param minQuality lowest water quality (0..1) it bears
 * @param salinity  the salinity range it lives in
 * @param chemistry its pH/TDS range, or null when pH and TDS do not matter to it
 * @param labyrinth breathes air at the surface (low oxygen does not harm it)
 * @param hungerDays days after a meal from which it is hungry (our bettas: 1)
 * @param stock     how much an adult counts for the stocking (NaN: by the size, like before)
 * @param food      what it eats: {@code normal}, {@code live}, {@code fry} (food kinds), {@code #ns:tag} (item tag) or an item id
 * @param breeding  how it breeds in our tanks
 * @param suppressOwn whether the animal's own breeding (vanilla love, the mod's breeding goal) is suppressed in our tanks
 * @param terrarium the terrarium needs (M31) if it lives in a terrarium or paludarium instead of an aquarium, or null
 */
public record ForeignProfile(List<ResourceLocation> entities, boolean required, String group, @Nullable String species,
                             double tempMin, double tempMax, double minQuality, Salinity.Range salinity, @Nullable ChemTolerance.Range chemistry,
                             boolean labyrinth, double hungerDays, double stock, List<String> food, Breeding breeding, boolean suppressOwn,
                             @Nullable TerrariumNeeds terrarium) {

    /** How the animal breeds in our tanks. */
    public enum Type {
        /** It does not breed there (its own breeding is still suppressed). */
        NONE,
        /** A female carries the young for {@link Breeding#days} and then gives birth to free-swimming young (guppies). */
        LIVEBEARER,
        /** A pair spawns; the eggs hatch after {@link Breeding#days} (on plants if {@link Breeding#needsPlants}). */
        EGGS
    }

    /**
     * @param type          livebearer, eggs or none
     * @param tempMin       lowest water temperature for breeding
     * @param tempMax       highest water temperature for breeding
     * @param minWaterBlocks water blocks the tank needs before a pair breeds (M30: food fish only spawn in big tanks)
     * @param needsPlants   the eggs need plants in the tank
     * @param days          days from the mating to the young
     * @param youngMin      fewest young per brood
     * @param youngMax      most young per brood
     * @param pauseDays     days a mother rests after a brood
     * @param rate          broods per pair and day in good conditions (a chance per day)
     * @param offspring     the entity type of the young (null: the parent's type)
     * @param offspringNbt  NBT merged into every young (null: none)
     * @param inherit       NBT keys every young takes from one of its parents at random (its variants: the mod's own
     *                      genetics, e.g. the fin and tail colours of a Fintastic guppy)
     * @param baby          the young are born as babies of the type (they grow up by the mod's own timer)
     */
    public record Breeding(Type type, double tempMin, double tempMax, double minWaterBlocks, boolean needsPlants, double days,
                           int youngMin, int youngMax, double pauseDays, double rate, @Nullable ResourceLocation offspring,
                           @Nullable CompoundTag offspringNbt, List<String> inherit, boolean baby) {
        public static final Breeding NONE = new Breeding(Type.NONE, Double.NaN, Double.NaN, 0, false, 1, 0, 0, 0, 0, null, null, List.of(), true);

        public boolean breeds() {
            return type != Type.NONE && youngMax > 0 && rate > 0;
        }

        public boolean breedingTemp(double celsius) {
            return (Double.isNaN(tempMin) || celsius >= tempMin - 1e-4) && (Double.isNaN(tempMax) || celsius <= tempMax + 1e-4);
        }
    }

    /** Default days a livebearer carries its young and eggs need to hatch. */
    public static final double LIVEBEARER_DAYS = 3.0, EGG_DAYS = 2.0;

    public boolean tolerates(double celsius) {
        return celsius >= tempMin - 1e-4 && celsius <= tempMax + 1e-4;
    }

    /** Whether it eats food of this kind name ({@code normal}, {@code live}, {@code fry}); tags and items: see {@link ForeignCare}. */
    public boolean eatsKind(String kind) {
        return food.contains(kind);
    }

    // ------------------------------------------------------------------ parsing

    /** Thrown for a file that cannot be used at all. */
    public static final class Invalid extends RuntimeException {
        public Invalid(String msg) {
            super(msg);
        }
    }

    /**
     * Parses one profile file. Unknown or odd values are reported to {@code warn} and fall back to the defaults; a
     * file without entity ids or with a broken value type throws {@link Invalid}.
     */
    public static ForeignProfile parse(JsonObject o, Consumer<String> warn) {
        List<ResourceLocation> entities = new ArrayList<>();
        JsonElement ent = o.get("entity");
        if (ent == null) ent = o.get("entities");
        if (ent == null) throw new Invalid("no \"entity\"");
        if (ent.isJsonArray()) {
            for (JsonElement e : ent.getAsJsonArray()) entities.add(id(e.getAsString()));
        } else {
            entities.add(id(ent.getAsString()));
        }
        if (entities.isEmpty()) throw new Invalid("empty \"entity\" list");
        boolean required = bool(o, "required", false);
        String speciesId = str(o, "species", null);
        TankmateSpecies sp = null;
        if (speciesId != null) {
            sp = TankmateSpecies.byId(speciesId.startsWith("bettacraft:") ? speciesId.substring("bettacraft:".length()) : speciesId);
            if (sp == null) warn.accept("unknown species \"" + speciesId + "\" (ignored)");
        }
        String group = str(o, "group", sp != null ? sp.group().name().toLowerCase(Locale.ROOT) : "other");

        JsonObject care = o.has("care") ? o.getAsJsonObject("care") : new JsonObject();
        double[] temp = range(care, "temperature", sp != null ? new double[]{sp.tempMin(), sp.tempMax()} : new double[]{18, 28});
        double quality = num(care, "min_quality", sp != null ? sp.minQuality() : 0.4);
        Salinity.Range salt = sp != null ? sp.salinity() : Salinity.Range.FRESH;
        if (care.has("salinity")) salt = salinity(care.get("salinity"), salt, warn);
        ChemTolerance.Range chem = sp != null ? ChemTolerance.tankmate(sp) : null;
        if (care.has("ph") || care.has("tds")) {
            double[] ph = range(care, "ph", chem != null ? new double[]{chem.phMin(), chem.phMax()} : new double[]{6.0, 8.4});
            double[] tds = range(care, "tds", chem != null ? new double[]{chem.tdsMin(), chem.tdsMax()} : new double[]{0, 700});
            chem = salt.min() >= 20 ? new ChemTolerance.Range((float) ph[0], (float) ph[1], 0, 0, true) : ChemTolerance.Range.fresh(ph[0], ph[1], tds[0], tds[1]);
        }
        boolean labyrinth = bool(care, "labyrinth", sp != null && sp.labyrinth());
        double hunger = Math.max(0.1, num(care, "hunger_days", 1.0));
        double stock = num(care, "stock", sp != null ? sp.stock() : Double.NaN);

        List<String> food = new ArrayList<>();
        if (o.has("food")) {
            for (JsonElement e : array(o.get("food"))) {
                String f = e.getAsString().trim();
                if (f.isEmpty()) continue;
                if (!f.startsWith("#") && !f.contains(":") && !List.of("normal", "live", "fry").contains(f)) {
                    warn.accept("unknown food \"" + f + "\" (normal, live, fry, #tag or item id)");
                    continue;
                }
                food.add(f);
            }
        } else {
            food.add("normal");
            food.add("live");
        }

        Breeding breeding = breeding(o.has("breeding") ? o.getAsJsonObject("breeding") : null, sp, temp, warn);
        boolean suppress = bool(o, "suppress_own_breeding", true);
        TerrariumNeeds terra = o.has("terrarium") ? terrarium(o.getAsJsonObject("terrarium"), warn) : null;
        return new ForeignProfile(List.copyOf(entities), required, group, sp != null ? sp.id() : null, temp[0], temp[1], quality, salt, chem,
                labyrinth, hunger, stock, List.copyOf(food), breeding, suppress, terra);
    }

    private static Breeding breeding(@Nullable JsonObject b, @Nullable TankmateSpecies sp, double[] temp, Consumer<String> warn) {
        Type type = Type.NONE;
        if (sp != null) type = typeOf(sp.breeding());
        if (b == null && sp == null) return Breeding.NONE;
        if (b == null) b = new JsonObject();
        if (b.has("type")) {
            String t = b.get("type").getAsString().toUpperCase(Locale.ROOT);
            try {
                type = Type.valueOf(t);
            } catch (IllegalArgumentException ex) {
                warn.accept("unknown breeding type \"" + t.toLowerCase(Locale.ROOT) + "\" (livebearer, eggs, none)");
                type = Type.NONE;
            }
        }
        if (type == Type.NONE) return Breeding.NONE;
        double[] bt = range(b, "temperature", sp != null ? new double[]{sp.breedMin(), sp.breedMax()} : temp);
        double days = Math.max(0.05, num(b, "days", type == Type.LIVEBEARER ? LIVEBEARER_DAYS : EGG_DAYS));
        int[] young = {sp != null ? Math.max(1, Math.min(sp.broodMin(), 4)) : 2, sp != null ? Math.max(1, Math.min(sp.broodMax(), 6)) : 4};
        if (b.has("young")) {
            double[] y = range(b, "young", new double[]{young[0], young[1]});
            young = new int[]{(int) Math.max(0, Math.round(y[0])), (int) Math.max(0, Math.round(y[1]))};
        }
        if (young[1] < young[0]) young = new int[]{young[1], young[0]};
        ResourceLocation offspring = b.has("offspring") ? id(b.get("offspring").getAsString()) : null;
        CompoundTag nbt = null;
        if (b.has("offspring_nbt")) {
            JsonElement n = b.get("offspring_nbt");
            try {
                nbt = TagParser.parseTag(n.isJsonPrimitive() ? n.getAsString() : n.toString());
            } catch (Exception ex) {
                warn.accept("offspring_nbt is no valid SNBT: " + ex.getMessage());
            }
        }
        List<String> inherit = new ArrayList<>();
        if (b.has("inherit")) for (JsonElement e : array(b.get("inherit"))) inherit.add(e.getAsString());
        boolean needsPlants = bool(b, "needs_plants", type == Type.EGGS && sp != null && sp.breeding() == TankmateSpecies.Breeding.SCATTER);
        return new Breeding(type, bt[0], bt[1], Math.max(0, num(b, "min_water_blocks", 0)), needsPlants, days, young[0], young[1],
                Math.max(0, num(b, "pause_days", 3.0)), Math.max(0, num(b, "rate", 0.5)), offspring, nbt, List.copyOf(inherit), bool(b, "baby", true));
    }

    /** Our breeding types in the three of the foreign animals: young carried in the body or mouth are born free; the rest lay eggs. */
    public static Type typeOf(TankmateSpecies.Breeding b) {
        return switch (b) {
            case LIVEBEARER, POUCH, MOUTH -> Type.LIVEBEARER;
            case NONE -> Type.NONE;
            default -> Type.EGGS;
        };
    }

    private static TerrariumNeeds terrarium(JsonObject t, Consumer<String> warn) {
        TerrariumNeeds.Habitat h = TerrariumNeeds.Habitat.ANY;
        if (t.has("habitat")) {
            try {
                h = TerrariumNeeds.Habitat.valueOf(t.get("habitat").getAsString().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                warn.accept("unknown habitat \"" + t.get("habitat").getAsString() + "\" (dry, paludarium, any, wet, all)");
            }
        }
        TerrariumNeeds.Builder b = TerrariumNeeds.builder(h);
        if (t.has("air")) {
            double[] r = range(t, "air", null);
            b.air(r[0], r[1]);
        }
        if (t.has("humidity")) {
            double[] r = range(t, "humidity", null);
            b.humidity(r[0], r[1]);
        }
        if (t.has("land")) {
            double[] r = range(t, "land", null);
            b.land(r[0], r[1]);
        }
        if (t.has("water")) {
            double[] r = range(t, "water", null);
            b.water(r[0], r[1]);
        }
        if (t.has("uv")) b.uv(num(t, "uv", 0));
        if (bool(t, "basking", false)) b.basking();
        return b.build();
    }

    static Salinity.Range salinity(JsonElement e, Salinity.Range fallback, Consumer<String> warn) {
        if (e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            return new Salinity.Range(a.get(0).getAsFloat(), a.get(1).getAsFloat());
        }
        return switch (e.getAsString().toLowerCase(Locale.ROOT)) {
            case "fresh" -> Salinity.Range.FRESH;
            case "brackish" -> Salinity.Range.BRACKISH;
            case "brackish_only" -> Salinity.Range.BRACKISH_ONLY;
            case "marine" -> Salinity.Range.MARINE;
            case "hardy_marine" -> Salinity.Range.HARDY_MARINE;
            case "any" -> Salinity.Range.ANY;
            default -> {
                warn.accept("unknown salinity \"" + e.getAsString() + "\" (fresh, brackish, brackish_only, marine, hardy_marine, any or [min, max])");
                yield fallback;
            }
        };
    }

    private static ResourceLocation id(String s) {
        ResourceLocation r = ResourceLocation.tryParse(s);
        if (r == null) throw new Invalid("bad id \"" + s + "\"");
        return r;
    }

    private static JsonArray array(JsonElement e) {
        if (e.isJsonArray()) return e.getAsJsonArray();
        JsonArray a = new JsonArray();
        a.add(e);
        return a;
    }

    private static double[] range(JsonObject o, String key, @Nullable double[] fallback) {
        if (!o.has(key)) {
            if (fallback == null) throw new Invalid("no \"" + key + "\"");
            return fallback;
        }
        JsonElement e = o.get(key);
        if (e.isJsonArray() && e.getAsJsonArray().size() == 2) {
            double a = e.getAsJsonArray().get(0).getAsDouble(), b = e.getAsJsonArray().get(1).getAsDouble();
            return new double[]{Math.min(a, b), Math.max(a, b)};
        }
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) return new double[]{e.getAsDouble(), e.getAsDouble()};
        throw new Invalid("\"" + key + "\" must be [min, max]");
    }

    private static double num(JsonObject o, String key, double fallback) {
        return o.has(key) ? o.get(key).getAsDouble() : fallback;
    }

    private static boolean bool(JsonObject o, String key, boolean fallback) {
        return o.has(key) ? o.get(key).getAsBoolean() : fallback;
    }

    @Nullable
    private static String str(JsonObject o, String key, @Nullable String fallback) {
        return o.has(key) ? o.get(key).getAsString() : fallback;
    }
}
