package org.overengineer.bettacraft.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

/**
 * M33 part c: the presets. Precedence ({@link Presets#resolve}: the preset over a default, a changed value and a kept path
 * over the preset), the tables (every value valid for its key, not its default, the key known), the promised effects of each
 * preset on the feature tree and the game values, and that every value a preset sets is read through the preset.
 */
class PresetsTest {
    @Test
    void precedence() {
        // at the default: the preset's value
        assertEquals(14.0, Presets.resolve(7.0, 7.0, 14.0, false));
        assertEquals(false, Presets.resolve(true, true, false, false));
        // changed in the file: the file wins, also when it equals the preset's value
        assertEquals(10.0, Presets.resolve(10.0, 7.0, 14.0, false));
        assertEquals(14.0, Presets.resolve(14.0, 7.0, 14.0, false));
        // kept (presetKeep): the file value, even at its default
        assertEquals(true, Presets.resolve(true, true, false, true));
        // the preset does not set it
        assertEquals(7.0, Presets.resolve(7.0, 7.0, null, false));
        assertEquals(ServerConfig.CareMode.DECOR,
                Presets.resolve(ServerConfig.CareMode.NORMAL, ServerConfig.CareMode.NORMAL, ServerConfig.CareMode.DECOR, false));
    }

    @Test
    void outsideAGameNoPresetIsActive() {
        // the common config is not loaded in unit tests: CUSTOM, every value its file's
        assertSame(Preset.CUSTOM, Presets.active());
        assertEquals(7.0, Presets.effective(ServerConfig.HEALTH_DAYS_UNTIL_SICK, 7.0));
        assertTrue(Presets.entries(Preset.CUSTOM).isEmpty());
    }

    @Test
    void keepPaths() {
        assertEquals("features.breeding.enabled", Presets.normalize("breeding.enabled"));
        assertEquals("features.breeding.enabled", Presets.normalize(" features.breeding.enabled "));
        assertEquals("waterchange.difficulty", Presets.normalize("waterchange.difficulty"));
        assertTrue(Presets.setRemote("decor", List.of("breeding.enabled")));
        try {
            assertSame(Preset.DECOR, Presets.active());
            assertTrue(Presets.keep().contains("features.breeding.enabled"));
            // kept: the default (on) stays; not kept: the preset's off
            assertTrue(Presets.effective(CommonConfig.FEATURES.get(Feature.BREEDING), true));
            assertFalse(Presets.effective(CommonConfig.FEATURES.get(Feature.ALGAE), true));
            assertEquals(ServerConfig.CareMode.DECOR, Presets.effective(ServerConfig.CARE_MODE, ServerConfig.CareMode.NORMAL));
            // changed in the file: wins
            assertEquals(ServerConfig.CareMode.NO_DEATH, Presets.effective(ServerConfig.CARE_MODE, ServerConfig.CareMode.NO_DEATH));
        } finally {
            Presets.setRemote(null, null);
        }
        assertSame(Preset.CUSTOM, Presets.active());
    }

    @Test
    void tablesAreValid() {
        Set<String> serverPaths = new HashSet<>();
        for (ModConfigSpec.ConfigValue<?> v : values(ServerConfig.class)) serverPaths.add(Presets.path(v));
        for (Preset p : Preset.values()) {
            Set<String> seen = new HashSet<>();
            for (Presets.Entry e : Presets.entries(p)) {
                assertTrue(seen.add(e.path()), p + ": twice " + e.path());
                Object def = e.config().getDefault();
                assertNotNull(def, e.path());
                assertSame(def.getClass(), e.value().getClass(), p + " " + e.path() + ": type");
                assertNotEquals(def, e.value(), p + " " + e.path() + ": the preset sets the default");
                assertTrue(e.config().getSpec().test(e.value()), p + " " + e.path() + ": " + e.value() + " out of range");
                assertEquals(e.server(), serverPaths.contains(e.path()), p + " " + e.path() + ": file");
                if (!e.server()) assertTrue(e.path().startsWith("features."), e.path());
            }
        }
        assertFalse(serverPaths.stream().anyMatch(s -> s.startsWith("features.")), "a server path would collide with the feature paths");
    }

    /** The feature tree as a preset leaves it (every other switch at its default, on). */
    private static boolean on(Preset p, Feature f) {
        return Features.resolve(f, x -> {
            Object v = Presets.value(p, CommonConfig.FEATURES.get(x));
            return v == null || (Boolean) v;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T value(Preset p, ModConfigSpec.ConfigValue<T> v) {
        Object o = Presets.value(p, v);
        return o == null ? v.getDefault() : (T) o;
    }

    @Test
    void simple() {
        Preset p = Preset.SIMPLE;
        for (Feature f : List.of(Feature.WATER_CHEMISTRY, Feature.OXYGEN, Feature.CO2, Feature.OSMOSIS, Feature.POWER, Feature.POWER_REGULATOR)) {
            assertFalse(on(p, f), f.path());
        }
        assertTrue(on(p, Feature.TANKMATES) && on(p, Feature.BREEDING) && on(p, Feature.CORALS) && on(p, Feature.ALGAE));
        assertEquals(ServerConfig.WaterChangeDifficulty.OFF, value(p, ServerConfig.WATER_CHANGE_DIFFICULTY));
        assertFalse(value(p, ServerConfig.EVAPORATION_ENABLED));
        assertTrue(value(p, ServerConfig.HEALTH_DAYS_UNTIL_SICK) > ServerConfig.HEALTH_DAYS_UNTIL_SICK.getDefault());
        assertTrue(value(p, ServerConfig.HEALTH_DAYS_SICK_UNTIL_DEATH) > ServerConfig.HEALTH_DAYS_SICK_UNTIL_DEATH.getDefault());
        assertEquals(ServerConfig.CareMode.NORMAL, value(p, ServerConfig.CARE_MODE));
    }

    @Test
    void realistic() {
        Preset p = Preset.REALISTIC;
        for (Feature f : Feature.values()) assertTrue(on(p, f), "everything on: " + f.path());
        assertEquals(ServerConfig.ForeignSalinity.REALISTIC, value(p, ServerConfig.SALT_FOREIGN));
        assertEquals(ServerConfig.WaterChangeDifficulty.REALISTIC, value(p, ServerConfig.WATER_CHANGE_DIFFICULTY));
        for (var power : List.of(ServerConfig.POWER_HEATER, ServerConfig.POWER_FILTER, ServerConfig.POWER_LIGHT, ServerConfig.POWER_AIR_PUMP,
                ServerConfig.POWER_PUMPS, ServerConfig.POWER_MISTER)) {
            assertTrue(value(p, power), Presets.path(power));
        }
        assertTrue(value(p, ServerConfig.HEALTH_DAYS_UNTIL_SICK) < ServerConfig.HEALTH_DAYS_UNTIL_SICK.getDefault());
    }

    @Test
    void decor() {
        Preset p = Preset.DECOR;
        assertEquals(ServerConfig.CareMode.DECOR, value(p, ServerConfig.CARE_MODE));
        for (Feature f : List.of(Feature.WATER_CHEMISTRY, Feature.CO2, Feature.POWER, Feature.ALGAE, Feature.BREEDING, Feature.FOREIGN_BREEDING)) {
            assertFalse(on(p, f), f.path());
        }
        assertTrue(on(p, Feature.BETTAS) && on(p, Feature.TANKMATES) && on(p, Feature.INVERTEBRATES) && on(p, Feature.AQUATIC_PLANTS));
        assertFalse(value(p, ServerConfig.WATER_QUALITY_ENABLED));
        assertEquals(ServerConfig.WaterChangeDifficulty.OFF, value(p, ServerConfig.WATER_CHANGE_DIFFICULTY));
        assertFalse(value(p, ServerConfig.COMBAT_MALES_FIGHT));
    }

    /** The static config values of a class. */
    private static List<ModConfigSpec.ConfigValue<?>> values(Class<?> c) {
        List<ModConfigSpec.ConfigValue<?>> out = new ArrayList<>();
        for (Field f : c.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || !ModConfigSpec.ConfigValue.class.isAssignableFrom(f.getType())) continue;
            try {
                out.add((ModConfigSpec.ConfigValue<?>) f.get(null));
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
        return out;
    }

    /**
     * Every server value a preset sets is read through {@link ServerConfig#getOr} (which asks the preset), never with
     * {@code .get()} directly, in the mod sources.
     */
    @Test
    void presetValuesAreReadThroughThePreset() throws IOException, IllegalAccessException {
        Set<Object> preset = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (Preset p : Preset.values()) for (Presets.Entry e : Presets.entries(p)) if (e.server()) preset.add(e.config());
        List<String> names = new ArrayList<>();
        for (Field f : ServerConfig.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && ModConfigSpec.ConfigValue.class.isAssignableFrom(f.getType()) && preset.contains(f.get(null))) {
                names.add(f.getName());
            }
        }
        assertEquals(preset.size(), names.size(), "every preset value is a field of ServerConfig");
        java.util.regex.Pattern direct = java.util.regex.Pattern.compile("\\b(" + String.join("|", names) + ")\\s*\\.\\s*(get|getAsBoolean|getAsInt|getAsDouble|getRaw)\\s*\\(");
        Path root = Path.of(System.getProperty("bettacraft.projectDir", "."), "src");
        List<String> bad = new ArrayList<>();
        // the GameTests read the raw values to restore them; M52: the common code and the loader modules' main code
        for (Path dir : List.of(root.resolve("main").resolve("java"), root.resolve("../../neoforge/src/main/java").normalize(),
                root.resolve("../../fabric/src/main/java").normalize())) {
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> files = Files.walk(dir)) {
                for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java"))::iterator) {
                    String text = Files.readString(f, StandardCharsets.UTF_8);
                    var m = direct.matcher(text);
                    while (m.find()) bad.add(root.relativize(f) + ": " + m.group());
                }
            }
        }
        assertTrue(bad.isEmpty(), "preset values read past the preset: " + bad);
    }
}
