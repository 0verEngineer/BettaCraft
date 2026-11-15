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

    /** Migrates the common config file in place (if it exists and has old keys); never throws. */
    public static void migrateFile(Path file) {
        if (!Files.isRegularFile(file)) return;
        try {
            CommentedConfig cfg = TomlFormat.newConfig(java.util.LinkedHashMap::new); // keeps the order of the file
            TomlFormat.instance().createParser().parse(new java.io.StringReader(Files.readString(file, StandardCharsets.UTF_8)), cfg,
                    com.electronwill.nightconfig.core.io.ParsingMode.REPLACE);
            List<String> done = migrate(cfg);
            if (done.isEmpty()) return;
            Files.copy(file, file.resolveSibling(file.getFileName() + ".pre-m33"), StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(file, TomlFormat.instance().createWriter().writeToString(cfg), StandardCharsets.UTF_8);
            LOGGER.info("Moved {} feature switches of {} into the M33 feature tree:", done.size(), file.getFileName());
            for (String line : done) LOGGER.info("  {}", line);
        } catch (Exception e) {
            LOGGER.warn("Could not migrate the feature switches of {}: {}", file, e.toString());
        }
    }
}
