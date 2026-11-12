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
 * Every section and value of the three configs has a label ({@code bettacraft.configuration.<name>}) for the NeoForge
 * config screen, and every value a tooltip; the German file gets them through {@link LangCompletenessTest}.
 */
class ConfigLangTest {
    private static void collect(UnmodifiableConfig spec, Set<String> labels, Set<String> values) {
        for (UnmodifiableConfig.Entry e : spec.entrySet()) {
            labels.add(e.getKey());
            if (e.getValue() instanceof UnmodifiableConfig sub) {
                collect(sub, labels, values);
            } else {
                values.add(e.getKey());
            }
        }
    }

    @Test
    void everyConfigEntryHasATranslation() throws IOException {
        Path en = Path.of(System.getProperty("bettacraft.projectDir", "."), "src/generated/resources/assets/bettacraft/lang/en_us.json");
        JsonObject lang = JsonParser.parseString(Files.readString(en, StandardCharsets.UTF_8)).getAsJsonObject();
        Set<String> labels = new TreeSet<>(), values = new TreeSet<>();
        for (ModConfigSpec spec : new ModConfigSpec[]{ServerConfig.SPEC, CommonConfig.SPEC, ClientConfig.SPEC}) {
            collect(spec.getSpec(), labels, values);
        }
        Set<String> missing = new TreeSet<>();
        for (String k : labels) if (!lang.has("bettacraft.configuration." + k)) missing.add(k);
        for (String k : values) if (!lang.has("bettacraft.configuration." + k + ".tooltip")) missing.add(k + ".tooltip");
        assertTrue(missing.isEmpty(), "config keys without translation: " + missing);
        assertTrue(lang.has("bettacraft.configuration.title"), "config screen title");
    }
}
