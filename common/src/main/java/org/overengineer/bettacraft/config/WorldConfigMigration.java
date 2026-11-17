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
 */
public final class WorldConfigMigration {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("bettacraft/config");
    public static final String ARTEMIA_M46 = "artemia_sand_chance_m46";
    static final LevelResource SERVERCONFIG = new LevelResource("serverconfig");

    private WorldConfigMigration() {}


    /**
     * Runs the migrations not yet done in this world. {@code worldFile}: the world's own server config file, or null when the
     * world uses the global one (already migrated by {@link FeatureMigration}); the migrations are then only recorded.
     * Returns whether a value changed.
     */
    public static boolean run(MinecraftServer server, @Nullable Path worldFile) {
        Done done = Done.get(server);
        if (done.has(ARTEMIA_M46)) return false;
        boolean changed = false;
        if (worldFile != null && Math.abs(ServerConfig.DROPS_ARTEMIA_FROM_SAND_CHANCE.get() - ServerConfig.ARTEMIA_SAND_CHANCE_M35) < 1e-9) {
            try {
                Path backup = worldFile.resolveSibling(worldFile.getFileName() + ".pre-m46");
                if (Files.isRegularFile(worldFile) && !Files.exists(backup)) Files.copy(worldFile, backup);
            } catch (Exception e) {
                LOGGER.warn("Could not keep {} before the M46 migration: {}", worldFile, e.toString());
            }
            ServerConfig.DROPS_ARTEMIA_FROM_SAND_CHANCE.set(ServerConfig.ARTEMIA_SAND_CHANCE);
            ServerConfig.DROPS_ARTEMIA_FROM_SAND_CHANCE.save();
            LOGGER.info("Migrated the world's server config {} (M46): drops.artemiaFromSandChance 0.08 -> {} (the 1.36.0 default, halved in 1.36.1)",
                    worldFile, ServerConfig.ARTEMIA_SAND_CHANCE);
            changed = true;
        }
        done.add(ARTEMIA_M46);
        return changed;
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
