package org.overengineer.bettacraft.command;

import org.overengineer.bettacraft.util.NbtCompat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.compat.info.InfoData;
import org.overengineer.bettacraft.data.schema.Schemas;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.menu.TankView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development-only check of a world saved by an older version ({@code gradlew runServer -PsmoketestOldWorld}, active
 * with {@code -Dbettacraft.smoketest.oldworld=true}): the server runs on a copy of a saved world (default the M11 test
 * world from {@code run/saves}), this reads every tank controller straight from the region files (as the old version
 * saved it), then loads its chunk and compares: the tank loads with the same water, blocks, name and animals, has no
 * floating plants, saves as the current schema, its menu data and info overlay data build, and its animals come back.
 * Logs "[smoketest] OLD WORLD PASSED" and stops the server.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class DevSmokeOldWorld {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final boolean ENABLED = Boolean.getBoolean("bettacraft.smoketest.oldworld");
    private static final Pattern REGION = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");
    private static int ticks;
    private static int haltAt = -1;
    private static final List<Old> tanks = new ArrayList<>();

    /** A tank controller as the old version saved it. */
    private record Old(BlockPos pos, int schema, long water, long[] members, java.util.Set<java.util.UUID> inhabitants, String name, boolean hadPlants) {}

    /** UUIDs of all entities saved in the entities/ region files (an animal listed by a tank may have died or left). */
    private static final java.util.Set<java.util.UUID> savedEntities = new java.util.HashSet<>();
    private static final java.util.Set<ChunkPos> forced = new java.util.HashSet<>();

    private DevSmokeOldWorld() {}

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (!ENABLED) return;
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        ticks++;
        try {
            if (ticks == 20) {
                scan(level);
                int old = 0;
                for (Old o : tanks) if (o.schema < Schemas.TANK_BLOCK_ENTITY.currentVersion()) old++;
                LOGGER.info("[smoketest] old world {}: {} tank controller(s) in the region files, {} saved with an older schema",
                        server.getWorldData().getLevelName(), tanks.size(), old);
                if (tanks.isEmpty()) throw new IllegalStateException("no tanks in the old world");
                if (old == 0) throw new IllegalStateException("every tank already has the current schema: not an old world");
                // keep every chunk of every tank loaded: the animals are entities of their own, loaded a little later
                for (Old o : tanks) {
                    for (long m : o.members) {
                        ChunkPos cp = ChunkPos.containing(BlockPos.of(m));
                        if (forced.add(cp)) level.setChunkForced(cp.x(), cp.z(), true);
                    }
                }
                for (Old o : tanks) checkLoaded(level, o);
            } else if (ticks == 300) {
                int listed = 0, expected = 0, found = 0;
                for (Old o : tanks) {
                    TankBlockEntity c = controller(level, o.pos);
                    int saved = 0;
                    for (java.util.UUID u : o.inhabitants) if (savedEntities.contains(u)) saved++;
                    int live = TankInhabitants.liveInhabitants(level, c.getTank()).size();
                    listed += o.inhabitants.size();
                    expected += saved;
                    found += live;
                    if (live < saved) throw new IllegalStateException("tank " + o.pos.toShortString() + " '" + o.name + "': " + live + " of its " + saved
                            + " saved animals loaded");
                }
                LOGGER.info("[smoketest] old world animals: {} listed by the tanks, {} of them saved as entities, {} loaded in their tanks", listed, expected, found);
                for (ChunkPos cp : forced) level.setChunkForced(cp.x(), cp.z(), false);
                LOGGER.info("[smoketest] OLD WORLD PASSED");
                haltAt = ticks + 10;
            } else if (ticks == haltAt) {
                server.halt(false);
            }
        } catch (RuntimeException e) {
            LOGGER.error("[smoketest] OLD WORLD FAILED", e);
            server.halt(false);
        }
    }

    private static TankBlockEntity controller(ServerLevel level, BlockPos pos) {
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        if (!(level.getBlockEntity(pos) instanceof TankBlockEntity c) || !c.isController() || c.getTank() == null) {
            throw new IllegalStateException("no tank controller loaded at " + pos);
        }
        return c;
    }

    private static void checkLoaded(ServerLevel level, Old o) {
        TankBlockEntity c = controller(level, o.pos);
        TankData t = c.getTank();
        String where = o.pos.toShortString() + " (saved as schema " + o.schema + ")";
        if (t.waterMb() != o.water) throw new IllegalStateException(where + ": water " + o.water + " -> " + t.waterMb());
        if (t.memberCount() != o.members.length) throw new IllegalStateException(where + ": blocks " + o.members.length + " -> " + t.memberCount());
        if (!t.name().equals(o.name)) throw new IllegalStateException(where + ": name '" + o.name + "' -> '" + t.name() + "'");
        if (!t.inhabitants().equals(o.inhabitants)) throw new IllegalStateException(where + ": animals " + o.inhabitants.size() + " -> " + t.inhabitants().size());
        if (!o.hadPlants && !t.plants().isEmpty()) throw new IllegalStateException(where + ": plants out of nowhere");
        CompoundTag saved = c.saveWithFullMetadata(level.registryAccess());
        if (saved.getIntOr("schema", 0) != Schemas.TANK_BLOCK_ENTITY.currentVersion()) throw new IllegalStateException(where + ": saves as " + saved.getIntOr("schema", 0));
        if (!saved.getCompoundOrEmpty("tank").contains("plants")) throw new IllegalStateException(where + ": no plant list saved");
        // what the clients get: the block entity view, the menu data (plants tab) and the info overlay data
        TankData view = TankData.loadClientView(t.saveClientView());
        if (view.waterMb() != t.waterMb() || !view.plants().isEmpty() && o.schema < 7) throw new IllegalStateException(where + ": client view");
        TankView menu = TankView.read(TankView.build(FakePlayerFactory.getMinecraft(level), c));
        if (menu.plants.total != t.plants().size()) throw new IllegalStateException(where + ": menu plants " + menu.plants.total);
        CompoundTag info = InfoData.tank(level, o.pos);
        if (info == null || info.getIntOr("plants", 0) != t.plants().size()) throw new IllegalStateException(where + ": info data " + info);
        LOGGER.info("[smoketest] old tank {} '{}': {} blocks, {} mB, {} animal(s), {} plant(s) -> loads unchanged, saves as schema {}",
                where, t.name(), t.memberCount(), t.waterMb(), t.inhabitants().size(), t.plants().size(), saved.getIntOr("schema", 0));
    }

    /** Every tank controller in the overworld region files and every saved entity, read without loading the chunks. */
    private static void scan(ServerLevel level) {
        Path root = level.getServer().getWorldPath(LevelResource.ROOT);
        int chunks = 0;
        for (CompoundTag chunk : chunks(root.resolve("region"))) {
            chunks++;
            var list = chunk.getListOrEmpty("block_entities");
            for (int i = 0; i < list.size(); i++) {
                CompoundTag be = list.getCompoundOrEmpty(i);
                if (!"bettacraft:tank".equals(be.getStringOr("id", "")) || !NbtCompat.has(be, "tank", Tag.TAG_COMPOUND)) continue;
                CompoundTag t = be.getCompoundOrEmpty("tank");
                java.util.Set<java.util.UUID> inh = new java.util.HashSet<>();
                for (Tag u : t.getListOrEmpty("inhabitants")) inh.add(org.overengineer.bettacraft.util.NbtCompat.loadUUID(u));
                tanks.add(new Old(new BlockPos(be.getIntOr("x", 0), be.getIntOr("y", 0), be.getIntOr("z", 0)), be.getIntOr("schema", 0), t.getLongOr("water_mb", 0L),
                        t.getLongArray("members").orElse(new long[0]), inh, t.getStringOr("name", ""), t.contains("plants")));
            }
        }
        for (CompoundTag chunk : chunks(root.resolve("entities"))) {
            for (Tag e : chunk.getListOrEmpty("Entities")) {
                CompoundTag et = (CompoundTag) e;
                if (NbtCompat.hasUUID(et, "UUID")) savedEntities.add(NbtCompat.getUUID(et, "UUID"));
            }
        }
        LOGGER.info("[smoketest] old world: {} chunks and {} saved entities read from the region files", chunks, savedEntities.size());
    }

    /** All chunk tags of the region files in {@code dir} (anvil format: 4 KiB sector table, zlib / gzip / plain chunks). */
    private static List<CompoundTag> chunks(Path dir) {
        List<CompoundTag> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) return out;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.toList()) {
                if (!REGION.matcher(f.getFileName().toString()).matches()) continue;
                byte[] data = Files.readAllBytes(f);
                if (data.length < 8192) continue;
                for (int i = 0; i < 1024; i++) {
                    int entry = ((data[i * 4] & 0xFF) << 24) | ((data[i * 4 + 1] & 0xFF) << 16) | ((data[i * 4 + 2] & 0xFF) << 8) | (data[i * 4 + 3] & 0xFF);
                    if (entry == 0) continue;
                    int off = (entry >>> 8) * 4096;
                    if (off + 5 > data.length) continue;
                    int len = ((data[off] & 0xFF) << 24) | ((data[off + 1] & 0xFF) << 16) | ((data[off + 2] & 0xFF) << 8) | (data[off + 3] & 0xFF);
                    int type = data[off + 4] & 0x7F;
                    if (len <= 1 || off + 4 + len > data.length || (data[off + 4] & 0x80) != 0) continue; // external .mcc chunks: skipped
                    InputStream in = new java.io.ByteArrayInputStream(data, off + 5, len - 1);
                    if (type == 1) in = new java.util.zip.GZIPInputStream(in);
                    else if (type == 2) in = new java.util.zip.InflaterInputStream(in);
                    else if (type != 3) continue;
                    try (var din = new java.io.DataInputStream(new java.io.BufferedInputStream(in))) {
                        out.add(net.minecraft.nbt.NbtIo.read(din));
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot read the region files in " + dir, e);
        }
        return out;
    }
}
