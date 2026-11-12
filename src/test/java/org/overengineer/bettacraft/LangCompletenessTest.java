package org.overengineer.bettacraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * en_us is generated (runData), de_de is maintained by hand: both must have exactly the same keys and the same
 * number of format placeholders per key.
 */
class LangCompletenessTest {
    private static final Pattern PLACEHOLDER = Pattern.compile("%(\\d+\\$)?[sd]");

    private static Path root() {
        return Path.of(System.getProperty("bettacraft.projectDir", "."));
    }

    private static JsonObject read(Path p) throws IOException {
        assertTrue(Files.exists(p), "missing " + p + " (run gradlew runData)");
        return JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static int placeholders(String s) {
        Matcher m = PLACEHOLDER.matcher(s);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    @Test
    void germanMatchesEnglish() throws IOException {
        JsonObject en = read(root().resolve("src/generated/resources/assets/bettacraft/lang/en_us.json"));
        JsonObject de = read(root().resolve("src/main/resources/assets/bettacraft/lang/de_de.json"));
        TreeSet<String> missing = new TreeSet<>(en.keySet());
        missing.removeAll(de.keySet());
        TreeSet<String> extra = new TreeSet<>(de.keySet());
        extra.removeAll(en.keySet());
        assertTrue(missing.isEmpty(), "de_de is missing keys: " + missing);
        assertTrue(extra.isEmpty(), "de_de has keys that en_us does not have: " + extra);
        for (Map.Entry<String, com.google.gson.JsonElement> e : en.entrySet()) {
            String key = e.getKey();
            assertEquals(placeholders(e.getValue().getAsString()), placeholders(de.get(key).getAsString()),
                    "placeholder count differs for " + key);
        }
    }
}
