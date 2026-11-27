package org.overengineer.bettacraft.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * M33 part c: the presets of the common config key {@code preset} (simple, realistic, decor). A preset is a second set of
 * defaults over the feature switches (common config) and the game values (server config of each world):
 * <ul>
 *     <li>a value that stands at Bettacraft's default in its file follows the preset;</li>
 *     <li>a value that was changed in the file wins (the modpack's or the player's choice);</li>
 *     <li>a value whose path ({@code features.breeding.enabled}, {@code waterchange.difficulty}) is listed in
 *     {@code presetKeep} keeps its file value, also when that is the default.</li>
 * </ul>
 * Nothing is written into the files: switched back to {@code CUSTOM} every value is the file's again. Every read of a
 * value a preset sets goes through {@link ServerConfig#getOr} or {@link Features#own}, which ask {@link #effective}. On
 * a client of a remote server the server's preset and keep list count ({@link #setRemote}, sent with the feature switches);
 * the server config itself reaches the client through NeoForge.
 */
public final class Presets {
    private Presets() {}

    /** One value of a preset: the config value, its path in its file (also the form of {@code presetKeep}), the value, the file. */
    public record Entry(ModConfigSpec.ConfigValue<?> config, String path, Object value, boolean server) {}

    private static volatile Map<Preset, Map<ModConfigSpec.ConfigValue<?>, Entry>> tables;

    /** On a client of a remote server: the server's preset and keep list; null elsewhere. */
    @Nullable
    private static volatile Remote remote;

    private record Remote(Preset preset, Set<String> keep) {}

    /** The keep list of the common config, parsed once per list instance. */
    private static volatile List<? extends String> keepSource;
    private static volatile Set<String> keepCache = Set.of();

    // ------------------------------------------------------------------ the tables

    private static Map<Preset, Map<ModConfigSpec.ConfigValue<?>, Entry>> tables() {
        Map<Preset, Map<ModConfigSpec.ConfigValue<?>, Entry>> t = tables;
        if (t == null) {
            synchronized (Presets.class) {
                if (tables == null) tables = build();
                t = tables;
            }
        }
        return t;
    }

    private static Map<Preset, Map<ModConfigSpec.ConfigValue<?>, Entry>> build() {
        Map<Preset, Map<ModConfigSpec.ConfigValue<?>, Entry>> out = new EnumMap<>(Preset.class);
        for (Preset p : Preset.values()) out.put(p, new IdentityHashMap<>());

        // simple: no water chemistry, no forced water changes, no power; gentler care
        Map<ModConfigSpec.ConfigValue<?>, Entry> s = out.get(Preset.SIMPLE);
        feature(s, Feature.WATER_CHEMISTRY, false);
        feature(s, Feature.CO2, false);
        feature(s, Feature.OSMOSIS, false);
        feature(s, Feature.POWER, false);
        server(s, ServerConfig.WATER_CHANGE_DIFFICULTY, ServerConfig.WaterChangeDifficulty.OFF);
        server(s, ServerConfig.EVAPORATION_ENABLED, false);
        server(s, ServerConfig.HEALTH_DAYS_UNTIL_SICK, 14.0);
        server(s, ServerConfig.HEALTH_DAYS_SICK_UNTIL_DEATH, 28.0);
        server(s, ServerConfig.HEALTH_HUNGRY_AFTER_DAYS, 2.0);
        server(s, ServerConfig.HEALTH_RECOVERY_PER_DAY, 3.0);
        server(s, ServerConfig.TEMPERATURE_TOLERATED_MIN, 22.0);
        server(s, ServerConfig.TEMPERATURE_TOLERATED_MAX, 32.0);
        server(s, ServerConfig.SALT_SHOCK_SPEED, 0.5);
        server(s, ServerConfig.TANKMATES_SHOAL_STRESS, false);
        server(s, ServerConfig.FROGS_STARVE_DAYS, 8.0);
        server(s, ServerConfig.BIG_DOLPHIN_HUNGRY_DAYS, 4.0);
        server(s, ServerConfig.TERRA_HARM_DAYS, 2.0);
        server(s, ServerConfig.FOREIGN_HEALTH_SPEED, 0.5);

        // realistic: every feature on (their default), strict values, the devices need FE
        Map<ModConfigSpec.ConfigValue<?>, Entry> r = out.get(Preset.REALISTIC);
        server(r, ServerConfig.WATER_CHANGE_DIFFICULTY, ServerConfig.WaterChangeDifficulty.REALISTIC);
        server(r, ServerConfig.SALT_FOREIGN, ServerConfig.ForeignSalinity.REALISTIC);
        server(r, ServerConfig.SALT_VANILLA_CORALS, true);
        server(r, ServerConfig.SALT_REFUSE_DEADLY, false);
        server(r, ServerConfig.POWER_COSTS, ServerConfig.PowerCosts.HARD); // M67 (before: every *RequiresEnergy on)
        server(r, ServerConfig.HEALTH_DAYS_UNTIL_SICK, 5.0);
        server(r, ServerConfig.HEALTH_DAYS_SICK_UNTIL_DEATH, 10.0);
        server(r, ServerConfig.TEMPERATURE_TOLERATED_MIN, 25.0);
        server(r, ServerConfig.TEMPERATURE_TOLERATED_MAX, 29.0);

        // decor: no care (nothing hungry, sick or dying), no chemistry, power, algae or breeding
        Map<ModConfigSpec.ConfigValue<?>, Entry> d = out.get(Preset.DECOR);
        feature(d, Feature.WATER_CHEMISTRY, false);
        feature(d, Feature.CO2, false);
        feature(d, Feature.OSMOSIS, false);
        feature(d, Feature.POWER, false);
        feature(d, Feature.ALGAE, false);
        feature(d, Feature.BREEDING, false);
        server(d, ServerConfig.CARE_MODE, ServerConfig.CareMode.DECOR);
        server(d, ServerConfig.WATER_CHANGE_DIFFICULTY, ServerConfig.WaterChangeDifficulty.OFF);
        server(d, ServerConfig.WATER_QUALITY_ENABLED, false);
        server(d, ServerConfig.EVAPORATION_ENABLED, false);
        server(d, ServerConfig.COMBAT_MALES_FIGHT, false);
        server(d, ServerConfig.CRITTERS_BETTAS_HUNT, false);
        return Collections.unmodifiableMap(out);
    }

    private static void feature(Map<ModConfigSpec.ConfigValue<?>, Entry> m, Feature f, boolean on) {
        ModConfigSpec.BooleanValue v = CommonConfig.FEATURES.get(f);
        m.put(v, new Entry(v, path(v), on, false));
    }

    private static <T> void server(Map<ModConfigSpec.ConfigValue<?>, Entry> m, ModConfigSpec.ConfigValue<T> v, T value) {
        m.put(v, new Entry(v, path(v), value, true));
    }

    public static String path(ModConfigSpec.ConfigValue<?> v) {
        return String.join(".", v.getPath());
    }

    /** The values of a preset in the order of the table (CUSTOM: none). */
    public static List<Entry> entries(Preset p) {
        List<Entry> out = new ArrayList<>(tables().get(p).values());
        out.sort(java.util.Comparator.comparing((Entry e) -> e.server()).thenComparing(Entry::path));
        return out;
    }

    /** The value a preset sets for this config value, or null. */
    @Nullable
    public static Object value(Preset p, ModConfigSpec.ConfigValue<?> v) {
        Entry e = tables().get(p).get(v);
        return e == null ? null : e.value();
    }

    // ------------------------------------------------------------------ the active preset

    /** The active preset: the server's on a client of a remote server, otherwise the common config's. */
    public static Preset active() {
        Remote r = remote;
        if (r != null) return r.preset();
        return CommonConfig.getOr(CommonConfig.PRESET, Preset.CUSTOM);
    }

    /** The paths that keep their file values ({@code presetKeep}). */
    public static Set<String> keep() {
        Remote r = remote;
        if (r != null) return r.keep();
        List<? extends String> list = CommonConfig.getOr(CommonConfig.PRESET_KEEP, List.of());
        if (list != keepSource) {
            Set<String> set = new LinkedHashSet<>();
            for (String k : list) if (k != null && !k.isBlank()) set.add(normalize(k));
            keepCache = Collections.unmodifiableSet(set);
            keepSource = list;
        }
        return keepCache;
    }

    /** {@code power.enabled} of the features may be written without the section. */
    static String normalize(String key) {
        String k = key.trim();
        if (k.startsWith("features.")) return k;
        String[] parts = k.split("\\.");
        if (parts.length > 0 && Feature.byPath(k) != null) return "features." + k;
        return k;
    }

    /** The effective value of a config value: its file value, or the active preset's (see the class comment). */
    public static <T> T effective(ModConfigSpec.ConfigValue<T> v, T raw) {
        Preset p = active();
        if (p == Preset.CUSTOM) return raw;
        Entry e = tables().get(p).get(v);
        if (e == null) return raw;
        Set<String> keep = keep();
        return resolve(raw, v.getDefault(), cast(e.value()), !keep.isEmpty() && keep.contains(e.path()));
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }

    /**
     * The precedence (pure, unit tests): kept or changed from the default, the file value wins; otherwise the preset's
     * value (null = the preset does not set it).
     */
    public static <T> T resolve(T raw, T defaultValue, @Nullable T preset, boolean kept) {
        if (preset == null || kept || !Objects.equals(raw, defaultValue)) return raw;
        return preset;
    }

    /** The entries of the active preset that are in effect now (not kept, not changed in the file). */
    public static List<Entry> inEffect() {
        List<Entry> out = new ArrayList<>();
        Preset p = active();
        if (p == Preset.CUSTOM) return out;
        for (Entry e : entries(p)) {
            Object raw;
            try {
                raw = e.config().get();
            } catch (IllegalStateException ex) {
                continue; // the server config is not loaded (no world)
            }
            if (Objects.equals(resolve(raw, e.config().getDefault(), e.value(), keep().contains(e.path())), e.value())
                    && !Objects.equals(raw, e.value())) out.add(e);
        }
        return out;
    }

    /** The entries of the active preset that do not apply because the file changed the value or {@code presetKeep} lists it. */
    public static List<Entry> overridden() {
        List<Entry> out = new ArrayList<>();
        Preset p = active();
        if (p == Preset.CUSTOM) return out;
        for (Entry e : entries(p)) {
            try {
                if (keep().contains(e.path()) || !Objects.equals(e.config().get(), e.config().getDefault())) out.add(e);
            } catch (IllegalStateException ex) {
                // the server config is not loaded (no world)
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ remote

    /** Client of a remote server: its preset and keep list; null back to the local config. Returns whether it changed. */
    public static boolean setRemote(@Nullable String preset, @Nullable Collection<String> keepPaths) {
        Remote before = remote;
        Remote next = null;
        if (preset != null) {
            Preset p = Preset.byId(preset);
            Set<String> k = new LinkedHashSet<>();
            if (keepPaths != null) for (String s : keepPaths) k.add(normalize(s));
            next = new Remote(p == null ? Preset.CUSTOM : p, Collections.unmodifiableSet(k));
        }
        remote = next;
        return !Objects.equals(before, next);
    }
}
