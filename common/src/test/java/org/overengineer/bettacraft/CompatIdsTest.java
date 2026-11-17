package org.overengineer.bettacraft;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Better Fish Tanks can remove the recipes and creative tab entries of "other mods' fish tanks": every item or recipe
 * id whose path contains one of its keywords (SONSTIGES 1.4, config {@code otherModFishtankKeywords}). None of our
 * item, block or recipe ids may contain them, otherwise packs with that option lose our aquarium.
 */
class CompatIdsTest {
    /** Default keywords of Better Fish Tanks 1.4.2 ({@code ModConfig}). */
    private static final List<String> BFT_KEYWORDS = List.of("fishtank", "fish_tank", "aquarium", "fishbowl", "fish_bowl");
    private static final Pattern REGISTER = Pattern.compile("\\.register(?:Block|Item|SimpleItem)?\\(\\s*\"([a-z0-9_/.]+)\"");

    private static Path root() {
        return Path.of(System.getProperty("bettacraft.projectDir", "."));
    }

    private static void names(Path dir, TreeSet<String> out) throws IOException {
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> s = Files.walk(dir)) {
            s.filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> out.add(dir.relativize(p).toString().replace('\\', '/').replace(".json", "")));
        }
    }

    @Test
    void noIdContainsBetterFishTanksKeywords() throws IOException {
        TreeSet<String> ids = new TreeSet<>();
        Path gen = root().resolve("src/generated/resources");
        Path main = root().resolve("src/main/resources");
        names(gen.resolve("assets/bettacraft/models/item"), ids);
        names(gen.resolve("assets/bettacraft/blockstates"), ids);
        names(gen.resolve("data/bettacraft/recipe"), ids);
        names(main.resolve("data/bettacraft/recipe"), ids);
        names(gen.resolve("data/bettacraft/loot_table/blocks"), ids);
        // registry names in code (items, blocks, entities, block entities, menus, ...)
        try (Stream<Path> s = Files.walk(root().resolve("src/main/java/org/overengineer/bettacraft/registry"))) {
            for (Path p : s.filter(f -> f.toString().endsWith(".java")).toList()) {
                Matcher m = REGISTER.matcher(Files.readString(p, StandardCharsets.UTF_8));
                while (m.find()) ids.add(m.group(1));
            }
        }
        assertTrue(ids.size() > 50, "found too few ids (run gradlew runData?): " + ids.size());
        assertTrue(ids.contains("tank") && ids.contains("fish_bag"), "the scan must see the tank and the fish bag");
        TreeSet<String> bad = new TreeSet<>();
        for (String id : ids) {
            String lower = id.toLowerCase(Locale.ROOT);
            for (String k : BFT_KEYWORDS) if (lower.contains(k)) bad.add(id + " (" + k + ")");
        }
        assertFalse(!bad.isEmpty(), "ids that Better Fish Tanks would treat as another mod's fish tank: " + bad);
    }
}
