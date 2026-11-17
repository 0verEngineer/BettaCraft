package org.overengineer.bettacraft.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlFormat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * M33: a common config of 1.25.0 (the flat {@code features.*} list, test resource {@code config/common-1.25.0.toml}) and
 * a hand-written older one move into the feature tree: every value is kept, switches that were off stay off, the old keys
 * are gone, a second run changes nothing, and the result is a valid common config.
 */
class FeatureMigrationTest {
    @TempDir
    Path dir;

    private static String fixture() throws IOException {
        try (InputStream in = FeatureMigrationTest.class.getResourceAsStream("/config/common-1.25.0.toml")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static CommentedConfig parse(String toml) {
        return TomlFormat.instance().createParser().parse(toml);
    }

    private static Object at(Config c, String key) {
        return c.get(List.of(key.split("\\.")));
    }

    /** The old 1.25.0 keys all have a home: every flat switch of the fixture is an old key of some feature. */
    @Test
    void everyOldKeyHasAFeature() throws IOException {
        Config features = parse(fixture()).get("features");
        Set<String> known = new TreeSet<>();
        for (Feature f : Feature.values()) known.addAll(f.oldKeys());
        Set<String> missing = new TreeSet<>();
        for (Config.Entry e : features.entrySet()) if (!known.contains(e.getKey())) missing.add(e.getKey());
        assertTrue(missing.isEmpty(), "old switches without a new key: " + missing);
        assertEquals(known.size(), features.size(), "the fixture has every old switch");
    }

    @Test
    void offStaysOffAndTheOldKeysAreGone() throws IOException {
        String old = fixture().replace("wildFish = true", "wildFish = false").replace("pipes = true", "pipes = false")
                .replace("cichlids = true", "cichlids = false").replace("wildPlants = true", "wildPlants = false")
                .replace("terrarium = true", "terrarium = false").replace("reverseOsmosis = true", "reverseOsmosis = false");
        CommentedConfig c = parse(old);
        List<String> done = FeatureMigration.migrate(c);
        int olds = 0;
        for (Feature f : Feature.values()) if (!f.oldKeys().isEmpty()) olds++;
        assertEquals(olds, done.size(), "one line per new key with an old one: " + done);
        // split switches give both new ones their value
        assertEquals(false, at(c, "features.tankmates.wildSpawns"));
        assertEquals(false, at(c, "features.frogs.wildSpawns"));
        assertEquals(false, at(c, "features.aquaticPlants.wildPlants"));
        assertEquals(false, at(c, "features.floatingPlants.wildPlants"));
        // a flat switch that became a group (pipes -> pipes.enabled) and renamed ones
        assertEquals(false, at(c, "features.pipes.enabled"));
        assertEquals(false, at(c, "features.terrariums.enabled"));
        assertEquals(false, at(c, "features.osmosis.enabled"));
        assertEquals(false, at(c, "features.tankmates.cichlids"));
        assertEquals(true, at(c, "features.tankmates.puffers"));
        assertEquals(true, at(c, "features.equipment.stainlessSteel"));
        // nothing of the old list is left, the rest of the file is untouched
        Config features = c.get("features");
        for (Feature f : Feature.values()) {
            for (String o : f.oldKeys()) assertFalse(features.get(List.of(o)) instanceof Boolean, "old key left: " + o);
        }
        assertEquals("AUTO", at(c, "compat.overlayProvider"));
        assertEquals(8, ((Number) at(c, "spawn.weight")).intValue());
        // a second run finds nothing
        assertTrue(FeatureMigration.migrate(c).isEmpty());
        // and the common config accepts it: the corrected config keeps the values
        CommonConfig.SPEC.correct(c);
        assertTrue(CommonConfig.SPEC.isCorrect(c), "migrated config is a valid common config");
        assertEquals(false, at(c, "features.tankmates.wildSpawns"));
        assertEquals(false, at(c, "features.pipes.enabled"));
        assertEquals(true, at(c, "features.bettas.enabled"), "new switches get their default");
    }

    /** An older config (M23 era: only some switches) keeps them; the later ones get their default. */
    @Test
    void anOlderConfigWithFewSwitches() {
        CommentedConfig c = parse("""
                [spawn]
                weight = 4
                [features]
                interiorPathfinding = false
                plantMeshes = true
                oxygen = false
                aeration = true
                """);
        List<String> done = FeatureMigration.migrate(c);
        assertEquals(4, done.size(), done.toString());
        assertEquals(false, at(c, "features.interior.pathfinding"));
        assertEquals(true, at(c, "features.visuals.plantMeshes"));
        assertEquals(false, at(c, "features.waterChemistry.oxygen"));
        assertEquals(true, at(c, "features.equipment.aeration"));
        assertEquals(true, at(c, "features.tankmates.cichlids"), "the later switches with their default");
        assertEquals(4, ((Number) at(c, "spawn.weight")).intValue());
    }

    /** A new key already in the file wins over a leftover old one; a file without features is left alone. */
    @Test
    void newKeysWinAndNothingToDo() {
        CommentedConfig c = parse("""
                [features]
                cichlids = false
                [features.tankmates]
                cichlids = true
                """);
        FeatureMigration.migrate(c);
        assertEquals(true, at(c, "features.tankmates.cichlids"));
        assertNull(((Config) c.get("features")).get(List.of("cichlids")));
        assertTrue(FeatureMigration.migrate(parse("[spawn]\nweight = 3\n")).isEmpty());
    }

    /** The file is rewritten in place, the old one kept as .pre-m33; a broken file is left alone without an exception. */
    @Test
    void migratesTheFile() throws IOException {
        Path file = dir.resolve("bettacraft-common.toml");
        Files.writeString(file, fixture().replace("monitors = true", "monitors = false"), StandardCharsets.UTF_8);
        FeatureMigration.migrateFile(file);
        assertTrue(Files.exists(dir.resolve("bettacraft-common.toml.pre-m33")));
        CommentedConfig c = parse(Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(false, at(c, "features.monitors.enabled"));
        assertNull(((Config) c.get("features")).get(List.of("wildFish")));
        // written in the order of the tree, the rest of the file (comments, other sections) kept
        String text = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(text.indexOf("[features.bettas]") < text.indexOf("[features.breeding]")
                && text.indexOf("[features.breeding]") < text.indexOf("[features.compat.create]"), "tree order");
        assertTrue(text.indexOf("[spawn]") < text.indexOf("[features") && text.contains("#Natural spawning of wild bettas"), "rest kept");
        Path broken = dir.resolve("broken.toml");
        Files.writeString(broken, "[features\nx = ", StandardCharsets.UTF_8);
        FeatureMigration.migrateFile(broken);
        FeatureMigration.migrateFile(dir.resolve("missing.toml"));
    }

    /** M35.1: the server config loses food.artemiaCystStackSize; the old default sand chance becomes 4x, a set one stays. */
    @Test
    void serverConfigLosesTheCystStackSize() throws IOException {
        String toml = "[drops]\nartemiaFromSandChance = 0.02\n[food]\nfryPerItem = 20\nartemiaCystStackSize = 32\n";
        Path file = dir.resolve("bettacraft-server.toml");
        Files.writeString(file, toml, StandardCharsets.UTF_8);
        FeatureMigration.migrateServerFile(file);
        CommentedConfig c = parse(Files.readString(file, StandardCharsets.UTF_8));
        assertNull(at(c, "food.artemiaCystStackSize"), "the old key is gone");
        assertEquals(20, ((Number) at(c, "food.fryPerItem")).intValue(), "the rest stays");
        assertEquals(0.04, ((Number) at(c, "drops.artemiaFromSandChance")).doubleValue(), 1e-9, "the old default chance becomes the current one");
        assertTrue(Files.exists(dir.resolve("bettacraft-server.toml.pre-m35.1")), "the old file is kept");
        // a second run changes nothing
        String once = Files.readString(file, StandardCharsets.UTF_8);
        FeatureMigration.migrateServerFile(file);
        assertEquals(once, Files.readString(file, StandardCharsets.UTF_8), "nothing left to migrate");
        // a chance someone set stays; without the old key (a new file) nothing is touched
        CommentedConfig own = parse("[drops]\nartemiaFromSandChance = 0.05\n[food]\nartemiaCystStackSize = 16\n");
        assertFalse(FeatureMigration.migrateServer(own).isEmpty());
        assertEquals(0.05, ((Number) at(own, "drops.artemiaFromSandChance")).doubleValue(), 1e-9, "a set chance stays");
        CommentedConfig fresh = parse("[drops]\nartemiaFromSandChance = 0.02\n");
        assertTrue(FeatureMigration.migrateServer(fresh).isEmpty(), "no old key: nothing to do");
        assertEquals(0.02, ((Number) at(fresh, "drops.artemiaFromSandChance")).doubleValue(), 1e-9);
    }

    /** M46: 0.08 with the 1.36.0 comment becomes 0.04 once; 0.10, a later 0.08 and a hand-written 0.08 stay. */
    @Test
    void serverConfigHalvesTheOldSandChance() throws IOException {
        String toml = """
                [drops]
                	#Chance that breaking sand drops a jar
                	# Default: 0.08
                	# Range: 0.0 ~ 1.0
                	artemiaFromSandChance = 0.08
                [food]
                fryPerItem = 20
                """;
        Path file = dir.resolve("bettacraft-server.toml");
        Files.writeString(file, toml, StandardCharsets.UTF_8);
        FeatureMigration.migrateServerFile(file);
        CommentedConfig c = parse(Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(0.04, ((Number) at(c, "drops.artemiaFromSandChance")).doubleValue(), 1e-9, "the 1.36.0 default is halved");
        assertEquals(20, ((Number) at(c, "food.fryPerItem")).intValue(), "the rest stays");
        assertTrue(c.getComment(List.of("drops", "artemiaFromSandChance")).contains("Default: 0.04"), "comment follows");
        assertTrue(Files.exists(dir.resolve("bettacraft-server.toml.pre-m46")), "the old file is kept");
        String once = Files.readString(file, StandardCharsets.UTF_8);
        FeatureMigration.migrateServerFile(file);
        assertEquals(once, Files.readString(file, StandardCharsets.UTF_8), "only once");
        // a set chance stays
        CommentedConfig own = parse("""
                [drops]
                # Default: 0.08
                artemiaFromSandChance = 0.1
                """);
        assertTrue(FeatureMigration.migrateServerM46(own).isEmpty());
        assertEquals(0.10, ((Number) at(own, "drops.artemiaFromSandChance")).doubleValue(), 1e-9, "0.10 stays");
        // 0.08 chosen after the update (the comment already names the new default) stays, so does one without a comment
        CommentedConfig later = parse("""
                [drops]
                # Default: 0.04
                artemiaFromSandChance = 0.08
                """);
        assertTrue(FeatureMigration.migrateServerM46(later).isEmpty(), "0.08 set after the update stays");
        CommentedConfig bare = parse("""
                [drops]
                artemiaFromSandChance = 0.08
                """);
        assertTrue(FeatureMigration.migrateServerM46(bare).isEmpty(), "no default comment: someone wrote it, it stays");
        assertEquals(0.04, ServerConfig.ARTEMIA_SAND_CHANCE, 1e-9, "the new default");
    }
}
