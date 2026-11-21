package org.overengineer.bettacraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * en_us is generated (runData), de_de is maintained by hand: both must have exactly the same keys and the same
 * number of format placeholders per key (M26.1: and no German value may still be English).
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

    /** Common English words that do not occur in German: two of them in one German value mean it is still English. */
    private static final Pattern ENGLISH = Pattern.compile(
            "\\b(the|and|with|of|your|you|this|when|from|into|only|which|while|without|after|until|right-click|sneak)\\b");
    /** Keys whose values are trade names (varieties, strains, patterns, colour forms): the same in German. */
    private static final List<String> TRADE_NAME_PREFIXES = List.of("variety.", "strain.", "pattern.", "form.", "shape.", "type.",
            "color.", "lb.bettacraft.pattern.", "lb.bettacraft.body.", "lb.bettacraft.locus.", "lb.bettacraft.tail_", "critter.bettacraft.look.",
            "betta.bettacraft.species."); // M29 part c: Latin species names
    /** Values that are the same word in German (names, Latin genera, loan words used by German aquarists). */
    private static final Set<String> SAME_IN_GERMAN = Set.of(
            "bettacraft.configuration.journeyMap", "bettacraft.configuration.xaerosMinimap", "bettacraft.configuration.ftbChunks", // M53: mod names
            "advancements.bettacraft.root.title", "quest.bettacraft.chapter.title", "item.bettacraft.decor_spiderwood", "monitor.bettacraft.alarm", "monitor.bettacraft.device.co2", "monitor.bettacraft.redstone",
            "monitor.bettacraft.state.alarm", "monitor.bettacraft.tile.alarm", "monitor.bettacraft.value.magnesium", "itemGroup.bettacraft", "block.bettacraft.acropora", "item.bettacraft.acropora",
            "block.bettacraft.lily_pipe", "block.bettacraft.lobophyllia", "item.bettacraft.lobophyllia", "block.bettacraft.monte_carlo",
            "item.bettacraft.monte_carlo", "block.bettacraft.vallisneria", "item.bettacraft.vallisneria", "item.bettacraft.mulm",
            "entity.bettacraft.betta", "entity.bettacraft.guppy", "entity.bettacraft.platy", "entity.bettacraft.black_molly", "entity.bettacraft.pangasius", "entity.bettacraft.arapaima", "gui.bettacraft.cabinet.filter",
            "gui.bettacraft.chem.ph.neutral", "gui.bettacraft.devices.dimmer", "gui.bettacraft.dosing.additive.magnesium",
            "gui.bettacraft.dumbo", "gui.bettacraft.fish.generation", "gui.bettacraft.fish.name", "gui.bettacraft.fish.rename",
            "gui.bettacraft.reef.nitrate", "gui.bettacraft.tank.title", "jade.bettacraft.betta.type", "jade.bettacraft.tank.nest",
            "lb.bettacraft.albino.albino", "lb.bettacraft.blond.blond", "lb.bettacraft.gold.gold", "pectoral.bettacraft.dumbo",
            "pectoral.bettacraft.normal", "stage.bettacraft.adult", "stage.bettacraft.subadult",
            "block.bettacraft.monitor", "monitor.bettacraft.value.co2",
            "gui.bettacraft.terrarium.mode.aquarium", "gui.bettacraft.terrarium.mode.paludarium", "gui.bettacraft.terrarium.mode.terrarium", // M31
            "bettacraft.configuration.size.normal", // M36
            "gui.bettacraft.tank.name_hint", // M56: "Name"
            "gui.bettacraft.tank_mode.aquarium", "gui.bettacraft.tank_mode.paludarium", "gui.bettacraft.tank_mode.terrarium", // M33.1
            "block.bettacraft.thermo_hygrometer", "monitor.bettacraft.value.uv",
            "bettacraft.configuration.features.sump.refugium", "gui.bettacraft.sump.refugium", "gui.bettacraft.tab.sump", "item.bettacraft.chaetomorpha", // M49
            // M33: the switches of other mods are named after the mod
            "bettacraft.configuration.features.compat.betterFishTanks", "bettacraft.configuration.features.compat.create",
            "bettacraft.configuration.features.compat.create.enabled", "bettacraft.configuration.features.compat.enderio",
            "bettacraft.configuration.features.compat.jei", "bettacraft.configuration.features.compat.mekanism",
            "bettacraft.configuration.features.compat.overlays", "bettacraft.configuration.features.compat.thermal");
    private static final Pattern NO_WORDS = Pattern.compile("%(\\d+\\$)?[sd]|\\b(TDS|ppm|pH|FE|KH|Ca|NO|mB|t)\\b");

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

    @Test
    void germanIsTranslated() throws IOException {
        JsonObject en = read(root().resolve("src/generated/resources/assets/bettacraft/lang/en_us.json"));
        JsonObject de = read(root().resolve("src/main/resources/assets/bettacraft/lang/de_de.json"));
        TreeSet<String> english = new TreeSet<>();
        TreeSet<String> untranslated = new TreeSet<>();
        for (String key : de.keySet()) {
            String value = de.get(key).getAsString();
            Matcher m = ENGLISH.matcher(value);
            int hits = 0;
            while (m.find()) hits++;
            if (hits >= 2) english.add(key);
            if (!en.has(key) || !value.equals(en.get(key).getAsString())) continue;
            if (!NO_WORDS.matcher(value).replaceAll("").matches("(?s).*[A-Za-z]{2,}.*")) continue;
            if (TRADE_NAME_PREFIXES.stream().anyMatch(key::startsWith) || SAME_IN_GERMAN.contains(key)) continue;
            untranslated.add(key);
        }
        assertTrue(english.isEmpty(), "de_de values that are still English: " + english);
        assertTrue(untranslated.isEmpty(), "de_de values identical to en_us (translate them, or list trade names in SAME_IN_GERMAN): "
                + untranslated);
    }
}
