package org.overengineer.bettacraft;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;

/**
 * Every section and value of the three configs has a label ({@code bettacraft.configuration.<name>} or the translation key of the spec) for the NeoForge
 * config screen, and every value a tooltip; the German file gets them through {@link LangCompletenessTest}.
 */
class ConfigLangTest {
    /**
     * The label keys of every section and value: the translation key set in the spec (M33: the feature tree), otherwise
     * {@code bettacraft.configuration.<name>}; values also need {@code <key>.tooltip}.
     */
    private static void collect(ModConfigSpec spec, UnmodifiableConfig level, java.util.List<String> path, Set<String> labels, Set<String> values) {
        for (UnmodifiableConfig.Entry e : level.entrySet()) {
            java.util.List<String> p = new java.util.ArrayList<>(path);
            p.add(e.getKey());
            if (e.getValue() instanceof UnmodifiableConfig sub) {
                String key = spec.getLevelTranslationKey(p);
                labels.add(key != null ? key : "bettacraft.configuration." + e.getKey());
                collect(spec, sub, p, labels, values);
            } else {
                String key = e.getValue() instanceof ModConfigSpec.ValueSpec v && v.getTranslationKey() != null ? v.getTranslationKey()
                        : "bettacraft.configuration." + e.getKey();
                labels.add(key);
                values.add(key);
            }
        }
    }

    @Test
    void everyConfigEntryHasATranslation() throws IOException {
        Path en = Path.of(System.getProperty("bettacraft.projectDir", "."), "src/generated/resources/assets/bettacraft/lang/en_us.json");
        JsonObject lang = JsonParser.parseString(Files.readString(en, StandardCharsets.UTF_8)).getAsJsonObject();
        Set<String> labels = new TreeSet<>(), values = new TreeSet<>();
        for (ModConfigSpec spec : new ModConfigSpec[]{ServerConfig.SPEC, CommonConfig.SPEC, ClientConfig.SPEC}) {
            collect(spec, spec.getSpec(), java.util.List.of(), labels, values);
        }
        Set<String> missing = new TreeSet<>();
        for (String k : labels) if (!lang.has(k)) missing.add(k);
        for (String k : values) if (!lang.has(k + ".tooltip")) missing.add(k + ".tooltip");
        assertTrue(missing.isEmpty(), "config keys without translation: " + missing);
        assertTrue(lang.has("bettacraft.configuration.title"), "config screen title");
    }
}
