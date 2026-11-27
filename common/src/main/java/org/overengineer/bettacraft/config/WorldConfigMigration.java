package org.overengineer.bettacraft.config;

import org.overengineer.bettacraft.util.NbtCompat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import org.overengineer.bettacraft.BettaCraft;

/**
 * M46: migrations of the server config of a world that keeps its own file {@code <world>/serverconfig/bettacraft-server.toml}
 * (NeoForge loads that file before any hook of the mod, so {@link FeatureMigration} cannot reach it). When the server has
 * started, every migration not yet done in this world runs once and is recorded in the world's saved data
 * ({@code bettacraft_migrations}), so a value the player sets later is never touched again.
 * <p>
 * {@link #ARTEMIA_M46}: the 1.36.0 default sand chance 0.08 becomes 0.04 (the file is kept as {@code .pre-m46} first).
 * <p>
 * {@link #POWER_COSTS_M67}: the old switches {@code *RequiresEnergy} of the world's file become {@code power.costs}. The loader
 * has already dropped the unknown old keys when it loaded the file, but it kept the file before that correction as
 * {@code bettacraft-server-1.toml.bak} (FML / Forge Config API Port): the newest backup without {@code power.costs} tells
 * whether power was on; then {@code power.costs} becomes HARD.
 */
public final class WorldConfigMigration {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("bettacraft/config");
    public static final String ARTEMIA_M46 = "artemia_sand_chance_m46";
    public static final String POWER_COSTS_M67 = "power_costs_m67";
    static final LevelResource SERVERCONFIG = new LevelResource("serverconfig");

    private WorldConfigMigration() {}


    /**
     * Runs the migrations not yet done in this world. {@code worldFile}: the world's own server config file, or null when the
     * world uses the global one (already migrated by {@link FeatureMigration}); the migrations are then only recorded.
     * Returns whether a value changed.
     */
    public static boolean run(MinecraftServer server, @Nullable Path worldFile) {
        return run(server, worldFile, true);
    }

    /**
     * Like {@link #run(MinecraftServer, Path)}; {@code save} false (GameTests) changes the values only in memory: a saved config
     * file is read again later by the loader's config watcher, and that reload put back the file's values over the ones the
     * tests after it had set in memory (frogWithoutAirDrowns got the default air of two minutes in the middle of its run).
     */
    public static boolean run(MinecraftServer server, @Nullable Path worldFile, boolean save) {
        Done done = Done.get(server);
        boolean changed = false;
        if (!done.has(POWER_COSTS_M67)) {
            changed |= powerCostsM67(worldFile, save);
            done.add(POWER_COSTS_M67);
        }
        if (done.has(ARTEMIA_M46)) return changed;
        if (worldFile != null && Math.abs(ServerConfig.DROPS_ARTEMIA_FROM_SAND_CHANCE.get() - ServerConfig.ARTEMIA_SAND_CHANCE_M35) < 1e-9) {
            try {
                Path backup = worldFile.resolveSibling(worldFile.getFileName() + ".pre-m46");
                if (Files.isRegularFile(worldFile) && !Files.exists(backup)) Files.copy(worldFile, backup);
            } catch (Exception e) {
                LOGGER.warn("Could not keep {} before the M46 migration: {}", worldFile, e.toString());
            }
            ServerConfig.DROPS_ARTEMIA_FROM_SAND_CHANCE.set(ServerConfig.ARTEMIA_SAND_CHANCE);
            if (save) ServerConfig.DROPS_ARTEMIA_FROM_SAND_CHANCE.save();
            LOGGER.info("Migrated the world's server config {} (M46): drops.artemiaFromSandChance 0.08 -> {} (the 1.36.0 default, halved in 1.36.1)",
                    worldFile, ServerConfig.ARTEMIA_SAND_CHANCE);
            changed = true;
        }
        done.add(ARTEMIA_M46);
        return changed;
    }

    /**
     * M67: the world's own file had {@code *RequiresEnergy} on before M67 (read from the backup the loader made before it dropped
     * them): {@code power.costs} becomes HARD. Returns whether it changed.
     */
    public static boolean powerCostsM67(@Nullable Path worldFile, boolean save) {
        if (worldFile == null || ServerConfig.getOr(ServerConfig.POWER_COSTS, ServerConfig.PowerCosts.OFF) != ServerConfig.PowerCosts.OFF) return false;
        Boolean on = null;
        long newest = Long.MIN_VALUE;
        String base = worldFile.getFileName().toString().replaceFirst("\\.toml$", "");
        try (var files = Files.list(worldFile.getParent())) {
            for (Path p : (Iterable<Path>) files::iterator) {
                String n = p.getFileName().toString();
                if (!(n.startsWith(base + "-") && n.endsWith(".toml.bak")) && !n.equals(base + ".toml.pre-m67")) continue;
                Boolean o = FeatureMigration.oldPowerOn(p);
                if (o == null) continue;
                long t = Files.getLastModifiedTime(p).toMillis();
                if (t > newest) {
                    newest = t;
                    on = o;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Could not look for the old power switches of {}: {}", worldFile, e.toString());
            return false;
        }
        if (on == null || !on) return false;
        ServerConfig.POWER_COSTS.set(ServerConfig.PowerCosts.HARD);
        if (save) ServerConfig.POWER_COSTS.save();
        LOGGER.info("Migrated the world's server config {} (M67): a *RequiresEnergy switch was on -> power.costs = HARD", worldFile);
        return true;
    }

    /** The migrations done in a world (saved with the overworld). */
    public static final class Done extends SavedData {
        static final String NAME = "bettacraft_migrations";
        private final Set<String> ids = new LinkedHashSet<>();

        public static Done get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(org.overengineer.bettacraft.util.NbtCompat.savedData(NAME, Done::new, Done::load, Done::save));
        }

        public boolean has(String id) {
            return ids.contains(id);
        }

        public void add(String id) {
            if (ids.add(id)) setDirty();
        }

        /** For tests: forget a migration. */
        public void remove(String id) {
            if (ids.remove(id)) setDirty();
        }

        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            for (String id : ids) list.add(StringTag.valueOf(id));
            tag.put("done", list);
            return tag;
        }

        public static Done load(CompoundTag tag, HolderLookup.Provider registries) {
            Done d = new Done();
            for (Tag t : tag.getListOrEmpty("done")) d.ids.add(NbtCompat.asString(t));
            return d;
        }
    }
}
