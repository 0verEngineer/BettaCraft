package org.overengineer.bettacraft.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * M33: moves the feature switches of a common config written before M33 ({@code features.<old> = true/false}, one flat
 * list) into the feature tree ({@code features.<group>.<switch>}), once, before NeoForge loads the file (it would drop
 * the unknown old keys). A switch that was off stays off; an old switch split into several new ones (wildFish, wildPlants)
 * gives each of them its value; when several old switches fold into one new one, the new one is on only if all were on.
 * New keys already in the file win. The switch tree is written whole, in the order of {@link Feature} (the ones without an
 * old key with their default), so the file reads like a new one. The old file is kept as {@code bettacraft-common.toml.pre-m33}.
 * <p>
 * M35.1: the same for the server config ({@link #migrateServer}: the artemia cyst stack size is gone), old file kept as
 * {@code bettacraft-server.toml.pre-m35.1}.
 */
public final class FeatureMigration {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("bettacraft/config");

    private FeatureMigration() {}

    /**
     * Moves the old keys of {@code config} (the whole common config) into the new tree and removes them; returns one line
     * per new key that got a value ({@code features.tankmates.wildSpawns -> features.tankmates.wildSpawns = false}), empty if there
     * was nothing to move.
     */
    public static List<String> migrate(Config config) {
        List<String> done = new ArrayList<>();
        if (!(config.get("features") instanceof Config features)) return done;
        Map<String, Boolean> found = new LinkedHashMap<>();
        for (Feature f : Feature.values()) {
            for (String old : f.oldKeys()) {
                if (features.get(List.of(old)) instanceof Boolean b) found.put(old, b);
            }
        }
        if (found.isEmpty()) return done;
        for (String old : found.keySet()) features.remove(List.of(old));
        // the whole tree in the order of Feature (missing switches with their default), then whatever else was there
        Config ordered = config.createSubConfig();
        for (Feature f : Feature.values()) {
            Boolean value = null;
            List<String> from = new ArrayList<>();
            for (String old : f.oldKeys()) {
                Boolean b = found.get(old);
                if (b == null) continue;
                value = value == null ? b : value && b;
                from.add("features." + old);
            }
            List<String> path = Arrays.asList(f.path().split("\\."));
            if (features.get(path) instanceof Boolean already) {
                ordered.set(path, already); // already written in the new form: it wins
            } else if (value != null) {
                ordered.set(path, value);
                done.add(String.join(" + ", from) + " -> " + f.key() + " = " + value);
            } else {
                ordered.set(path, true);
            }
        }
        for (Config.Entry e : features.entrySet()) if (!ordered.contains(List.of(e.getKey()))) ordered.set(List.of(e.getKey()), e.getRawValue());
        config.set("features", ordered);
        return done;
    }

    /** M35.1: the server config key of the cyst stack size (jars always stack to 64 now). */
    static final List<String> OLD_CYST_STACK = List.of("food", "artemiaCystStackSize");
    static final List<String> SAND_CHANCE = List.of("drops", "artemiaFromSandChance");

    /**
     * M35.1: removes {@code food.artemiaCystStackSize} from a server config written before M35.1; such a file still holding
     * the old default sand chance (0.02) gets the new one (0.08, one jar is one culture now), a chance someone set stays.
     * Returns one line per change, empty if the file had no old key.
     */
    public static List<String> migrateServer(Config config) {
        List<String> done = new ArrayList<>();
        if (!config.contains(OLD_CYST_STACK)) return done;
        Object old = config.remove(OLD_CYST_STACK);
        done.add("food.artemiaCystStackSize = " + old + " removed (artemia cyst jars always stack to 64)");
        if (config.get(SAND_CHANCE) instanceof Number n && Math.abs(n.doubleValue() - ServerConfig.ARTEMIA_SAND_CHANCE_OLD) < 1e-9) {
            config.set(SAND_CHANCE, ServerConfig.ARTEMIA_SAND_CHANCE);
            done.add("drops.artemiaFromSandChance " + n + " -> " + ServerConfig.ARTEMIA_SAND_CHANCE + " (the old default, now 4x)");
        }
        return done;
    }

    /** Migrates the server config file in place (if it exists and has old keys, M35.1); never throws. */
    public static void migrateServerFile(Path file) {
        migrateFile(file, FeatureMigration::migrateServer, ".pre-m35.1", "Migrated {} settings of {} (M35.1):");
    }

    /** Migrates the common config file in place (if it exists and has old keys); never throws. */
    public static void migrateFile(Path file) {
        migrateFile(file, FeatureMigration::migrate, ".pre-m33", "Moved {} feature switches of {} into the M33 feature tree:");
    }

    private static void migrateFile(Path file, java.util.function.Function<Config, List<String>> step, String backup, String message) {
        if (!Files.isRegularFile(file)) return;
        try {
            CommentedConfig cfg = TomlFormat.newConfig(java.util.LinkedHashMap::new); // keeps the order of the file
            TomlFormat.instance().createParser().parse(new java.io.StringReader(Files.readString(file, StandardCharsets.UTF_8)), cfg,
                    com.electronwill.nightconfig.core.io.ParsingMode.REPLACE);
            List<String> done = step.apply(cfg);
            if (done.isEmpty()) return;
            Files.copy(file, file.resolveSibling(file.getFileName() + backup), StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(file, TomlFormat.instance().createWriter().writeToString(cfg), StandardCharsets.UTF_8);
            LOGGER.info(message, done.size(), file.getFileName());
            for (String line : done) LOGGER.info("  {}", line);
        } catch (Exception e) {
            LOGGER.warn("Could not migrate {}: {}", file, e.toString());
        }
    }
}
