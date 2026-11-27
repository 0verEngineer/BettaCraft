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
 * <p>
 * M46: a server config still holding the 1.36.0 default sand chance (0.08, written by a version whose default it was) gets
 * the new one (0.04), once; old file kept as {@code bettacraft-server.toml.pre-m46}.
 * <p>
 * M67: the switches {@code *RequiresEnergy} of a server config become {@code power.costs} ({@link #migrateServerM67}); old file
 * kept as {@code bettacraft-server.toml.pre-m67}.
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
            done.add("drops.artemiaFromSandChance " + n + " -> " + ServerConfig.ARTEMIA_SAND_CHANCE + " (the old default, now the M46 default)");
        }
        return done;
    }

    /**
     * M46: the default sand chance went from 0.08 to 0.04. A file whose chance is still 0.08 and whose comment of that key
     * still says {@code Default: 0.08} (NeoForge writes the default into the comment and rewrites it with the new one when it
     * loads the file) gets 0.04, once: after the first load the comment says 0.04, so a chance set to 0.08 later stays, and
     * every other value stays. Returns one line per change.
     */
    public static List<String> migrateServerM46(CommentedConfig config) {
        List<String> done = new ArrayList<>();
        if (!(config.get(SAND_CHANCE) instanceof Number n) || Math.abs(n.doubleValue() - ServerConfig.ARTEMIA_SAND_CHANCE_M35) > 1e-9) return done;
        String comment = config.getComment(SAND_CHANCE);
        if (comment == null || !java.util.regex.Pattern.compile("Default:\\s*0?\\.08\\b").matcher(comment).find()) return done;
        config.set(SAND_CHANCE, ServerConfig.ARTEMIA_SAND_CHANCE);
        config.setComment(SAND_CHANCE, comment.replaceAll("Default:(\\s*)0?\\.08\\b", "Default:$1" + ServerConfig.ARTEMIA_SAND_CHANCE));
        done.add("drops.artemiaFromSandChance " + n + " -> " + ServerConfig.ARTEMIA_SAND_CHANCE + " (the 1.36.0 default, halved in 1.36.1)");
        return done;
    }

    /** M67: the power switches of each device group before M67 (section, key), replaced by {@code power.costs}. */
    public static final List<List<String>> OLD_POWER_SWITCHES = List.of(List.of("power", "heaterRequiresEnergy"), List.of("power", "filterRequiresEnergy"),
            List.of("power", "lightRequiresEnergy"), List.of("aeration", "airPumpRequiresEnergy"), List.of("pipes", "pumpsRequireEnergy"),
            List.of("terrarium", "misterRequiresEnergy"));
    /** M67: the keys gone with the one store per tank (the fixed buffer, the springs of cabinets and external filters, the shaft's spring rate). */
    static final List<List<String>> OLD_STORE_KEYS = List.of(List.of("power", "bufferFe"), List.of("winding", "cabinetCharges"),
            List.of("winding", "externalFilterCharges"), List.of("createDrive", "fePerSpringTick"));
    static final List<String> POWER_COSTS = List.of("power", "costs");

    /**
     * M67: the switches {@code *RequiresEnergy} become the one setting {@code power.costs}: any of them on gives HARD (the
     * consumption they had), none on OFF (the default). A file that already has {@code power.costs} keeps it. The keys of the
     * fixed buffer and of the cabinet and filter springs are removed. Returns one line per change, empty if the file had none.
     */
    public static List<String> migrateServerM67(Config config) {
        List<String> done = new ArrayList<>();
        List<String> on = new ArrayList<>();
        boolean any = false;
        for (List<String> k : OLD_POWER_SWITCHES) {
            if (!config.contains(k)) continue;
            any = true;
            Object v = config.remove(k);
            if (Boolean.TRUE.equals(v) || "true".equals(String.valueOf(v))) on.add(String.join(".", k));
        }
        for (List<String> k : OLD_STORE_KEYS) {
            if (!config.contains(k)) continue;
            Object v = config.remove(k);
            done.add(String.join(".", k) + " = " + v + " removed (M67: one energy store per tank)");
        }
        if (any && !config.contains(POWER_COSTS)) {
            String costs = on.isEmpty() ? ServerConfig.PowerCosts.OFF.name() : ServerConfig.PowerCosts.HARD.name();
            config.set(POWER_COSTS, costs);
            done.add((on.isEmpty() ? "every *RequiresEnergy off" : String.join(" + ", on) + " on") + " -> power.costs = " + costs);
        } else if (any) {
            done.add("the old *RequiresEnergy switches removed (power.costs was already set)");
        }
        return done;
    }

    /** Whether a server config file of before M67 asked for power (any {@code *RequiresEnergy} on); null if it has none of them. */
    @javax.annotation.Nullable
    public static Boolean oldPowerOn(Path file) {
        if (!Files.isRegularFile(file)) return null;
        try {
            CommentedConfig cfg = TomlFormat.newConfig(java.util.LinkedHashMap::new);
            TomlFormat.instance().createParser().parse(new java.io.StringReader(Files.readString(file, StandardCharsets.UTF_8)), cfg,
                    com.electronwill.nightconfig.core.io.ParsingMode.REPLACE);
            if (cfg.contains(POWER_COSTS)) return null;
            Boolean out = null;
            for (List<String> k : OLD_POWER_SWITCHES) {
                if (!cfg.contains(k)) continue;
                Object v = cfg.get(k);
                out = (out != null && out) || Boolean.TRUE.equals(v) || "true".equals(String.valueOf(v));
            }
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    /** Migrates the server config file in place (if it exists and has old keys, M35.1); never throws. */
    public static void migrateServerFile(Path file) {
        migrateFile(file, FeatureMigration::migrateServer, ".pre-m35.1", "Migrated {} settings of {} (M35.1):");
        migrateFile(file, c -> migrateServerM46((CommentedConfig) c), ".pre-m46", "Migrated {} settings of {} (M46):");
        migrateFile(file, FeatureMigration::migrateServerM67, ".pre-m67", "Migrated {} settings of {} (M67, power.costs):");
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
