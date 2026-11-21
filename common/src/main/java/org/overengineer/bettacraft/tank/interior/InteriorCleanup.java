package org.overengineer.bettacraft.tank.interior;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.data.schema.Schemas;
import org.overengineer.bettacraft.mixin.interior.ChunkStorageAccessor;
import org.overengineer.bettacraft.mixin.interior.EntitySectionsAccessor;
import org.overengineer.bettacraft.mixin.interior.EntityStorageAccessor;
import org.overengineer.bettacraft.mixin.interior.ServerLevelEntitiesAccessor;
import org.overengineer.bettacraft.registry.ModDimensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M53c: deletes what removed interior regions left on the disk ({@link InteriorRegions}: dead ids) so their ids can be
 * handed out again, and the region files they leave at the next start. Before, every removed region (a tank broken, grown,
 * split, merged or turned) stayed in the region files for ever: 18 GB in the test world.
 * <p>
 * Each region lies in its own grid cell of 2048 x 2048 blocks; the cell's 4 x 4 region files ({@code region/},
 * {@code entities/}, {@code poi/}) hold no chunk of another cell. What is deleted and when:
 * <ul>
 *     <li><b>Running</b> ({@link #tick}): a dead region (removed before a complete save of the world, see
 *     {@link InteriorRegions#worldSaved}: a crash never goes back to a world whose tank still points to it), none of whose
 *     chunks or entity chunks is held and no player in it for 10 s, and whose tank does not travel: the chunk and entity
 *     storage finish their writes, a region whose shell is still there is
 *     kept for ever (in doubt: keep it), else every chunk the cell's region files hold is deleted through the storages'
 *     own write queues (the same way vanilla deletes chunks); with nothing left the id is free.</li>
 *     <li><b>At the start</b> ({@link #onServerStarting}, before the dimensions are loaded and any region file is open):
 *     the region files of dead and free ids (again not those with a shell) and region files without any chunk are
 *     deleted, after a log line with their count and size.</li>
 * </ul>
 * Living regions (in the list: standing, packed and travelling tanks), kept ones and every id from the counter on are never
 * touched. Switch: {@link Feature#INTERIOR_CLEANUP}; the op command {@code /bettacraft interior cleanup} shows what is
 * there and starts it by hand.
 */
public final class InteriorCleanup {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/interior-cleanup");
    /** Ticks a dead region must be quiet (nothing of it loaded, nobody in it) before its chunks are deleted. */
    static final int QUIET_TICKS = 200;
    /** Dead regions whose chunks are deleted in one round (regions without files are freed all at once). */
    static final int BATCH = 8;
    static final int INTERVAL = 20;
    private static final String FLOOR = "bettacraft:interior_floor", WALL = "bettacraft:interior_wall";

    private enum Stage { IDLE, SYNC, SHELL, CLEAR }

    private static Stage stage = Stage.IDLE;
    @Nullable
    private static CompletableFuture<?> pending;
    private static final IntArrayList batch = new IntArrayList();
    private static final Int2ObjectOpenHashMap<CompletableFuture<Optional<CompoundTag>>> origins = new Int2ObjectOpenHashMap<>();
    private static final Int2LongOpenHashMap quietSince = new Int2LongOpenHashMap();
    private static final it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap tries = new it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap();
    private static final IntOpenHashSet gaveUp = new IntOpenHashSet();
    /** The command started it (also with the switch off) until no dead region is left. */
    private static boolean forced;
    /** Regions freed and chunks deleted in this session (command, tests). */
    public static volatile int freedRegions, deletedChunks;
    /** Complete saves of the world noted in this session (tests). */
    public static volatile int worldSaves;

    private InteriorCleanup() {}

    public static boolean enabled() {
        return Features.enabled(Feature.INTERIOR_CLEANUP);
    }

    // ------------------------------------------------------------------ running

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % INTERVAL != 0) return;
        ServerLevel in = InteriorManager.interiorLevel(server);
        if (in == null) return;
        if (!forced && !enabled()) {
            reset();
            return;
        }
        InteriorRegions regions = InteriorRegions.get(server);
        if (regions.dead().isEmpty() && stage == Stage.IDLE) {
            forced = false;
            quietSince.clear();
            return;
        }
        try {
            step(server, in, regions);
        } catch (Exception e) {
            LOGGER.warn("Interior clean-up failed; the regions {} stay as they are in this session", batch, e);
            gaveUp.addAll(batch);
            reset();
        }
    }

    private static void step(MinecraftServer server, ServerLevel in, InteriorRegions regions) throws IOException {
        long now = server.getTickCount();
        IntSet busy = busyCells(in);
        switch (stage) {
            case IDLE -> {
                for (int id : regions.dead()) {
                    if (busy.contains(id)) quietSince.remove(id);
                    else if (!quietSince.containsKey(id)) quietSince.put(id, now);
                }
                batch.clear();
                for (int id : regions.dead()) {
                    if (gaveUp.contains(id) || busy.contains(id) || !regions.deletable(id) || org.overengineer.bettacraft.tank.TankMoves.isTravellingRegion(id)) continue;
                    if (now - quietSince.getOrDefault(id, now) < QUIET_TICKS) continue;
                    batch.add(id);
                }
                if (batch.isEmpty()) return;
                pending = syncStorages(in);
                stage = Stage.SYNC;
            }
            case SYNC -> {
                if (!pending.isDone()) return;
                pending.join();
                CellFiles files = CellFiles.of(dimensionFolder(server));
                IntArrayList withFiles = new IntArrayList();
                int freedNow = 0;
                for (int id : batch) {
                    if (busy.contains(id) || !regions.dead().contains(id)) continue;
                    if (!files.has(id)) {
                        regions.freed(id);
                        freedNow++;
                    } else if (withFiles.size() < BATCH) {
                        withFiles.add(id);
                    }
                }
                freedRegions += freedNow;
                if (freedNow > 0) LOGGER.debug("Freed {} removed interior region(s) without chunks on the disk", freedNow);
                batch.clear();
                batch.addAll(withFiles);
                origins.clear();
                for (int id : batch) {
                    ChunkPos o = new ChunkPos(InteriorRegion.originX(id) >> 4, InteriorRegion.originZ(id) >> 4);
                    // read only from a region file that is there (reading creates the file otherwise)
                    origins.put(id, files.region(id, o) ? in.getChunkSource().chunkMap.read(o) : CompletableFuture.completedFuture(Optional.empty()));
                }
                stage = batch.isEmpty() ? Stage.IDLE : Stage.SHELL;
            }
            case SHELL -> {
                for (var f : origins.values()) if (!f.isDone()) return;
                SimpleRegionStorage entities = entityStorage(in);
                Path dir = dimensionFolder(server);
                List<CompletableFuture<?>> writes = new ArrayList<>();
                IntArrayList clearing = new IntArrayList();
                for (int id : batch) {
                    if (busy.contains(id) || !regions.dead().contains(id)) continue;
                    CompoundTag origin = origins.get(id).join().orElse(null);
                    if (origin != null && hasShell(origin)) {
                        // in doubt: an interior that still has its shell was not removed by us (or not completely)
                        LOGGER.warn("Removed interior region {} still has its shell: it is kept and never handed out again", id);
                        regions.keep(id);
                        continue;
                    }
                    clearing.add(id);
                    for (ChunkPos p : storedChunks(dir.resolve("region"), id)) {
                        writes.add(in.getChunkSource().chunkMap.write(p, null));
                        // the map's note "a full chunk is stored here" goes as well
                        ((InteriorChunkMap) in.getChunkSource().chunkMap).bettacraft$forgetStored(p.toLong());
                        deletedChunks++;
                    }
                    if (entities != null) for (ChunkPos p : storedChunks(dir.resolve("entities"), id)) writes.add(entities.write(p, null));
                }
                origins.clear();
                batch.clear();
                batch.addAll(clearing);
                if (batch.isEmpty()) {
                    stage = Stage.IDLE;
                    return;
                }
                LOGGER.debug("Deleting the chunks of removed interior regions {}", batch);
                pending = CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new)).thenCompose(v -> syncStorages(in));
                stage = Stage.CLEAR;
            }
            case CLEAR -> {
                if (!pending.isDone()) return;
                pending.join();
                Path dir = dimensionFolder(server);
                for (int id : batch) {
                    if (!regions.dead().contains(id)) continue;
                    if (!busy.contains(id) && storedChunks(dir.resolve("region"), id).isEmpty() && storedChunks(dir.resolve("entities"), id).isEmpty()) {
                        regions.freed(id);
                        freedRegions++;
                        quietSince.remove(id);
                    } else {
                        // loaded again meanwhile (a player, a chunk loader): next round
                        int n = tries.addTo(id, 1) + 1;
                        if (n >= 3) gaveUp.add(id);
                        quietSince.remove(id);
                    }
                }
                batch.clear();
                stage = Stage.IDLE;
            }
        }
    }

    /**
     * The command: saves the world (as {@code /save-all}: the regions removed since the last save become dead), then starts
     * (also with the switch off) without waiting for the dead regions to be quiet that long.
     */
    public static void runNow(MinecraftServer server) {
        server.saveEverything(true, false, false);
        forced = true;
        gaveUp.clear();
        tries.clear();
        long now = server.getTickCount();
        for (int id : InteriorRegions.get(server).dead()) quietSince.put(id, now - QUIET_TICKS);
    }

    /**
     * After {@code MinecraftServer.saveAllChunks} (autosave, {@code /save-all}, the stop; mixin): when every dimension was
     * saved, the regions removed before are dead now.
     */
    public static void worldSaved(MinecraftServer server, boolean forcedSave) {
        try {
            for (ServerLevel l : server.getAllLevels()) if (l.noSave && !forcedSave) return; // /save-off: nothing was written
            if (server.overworld() == null) return;
            worldSaves++;
            InteriorRegions.get(server).worldSaved(server);
        } catch (RuntimeException e) {
            LOGGER.warn("Could not note the removed interior regions after the save", e);
        }
    }

    /** Whether a round is going on or dead regions wait for one (tests). */
    public static boolean idle() {
        return stage == Stage.IDLE;
    }

    public static void reset() {
        stage = Stage.IDLE;
        pending = null;
        batch.clear();
        origins.clear();
        forced = false;
    }

    /** The server stopped: nothing of this world is kept for the next one. */
    public static void clearAll() {
        reset();
        quietSince.clear();
        tries.clear();
        gaveUp.clear();
    }

    /** Grid cells with a chunk or entity chunk held (loading, loaded, waiting to be saved) or a player in them. */
    static IntSet busyCells(ServerLevel in) {
        IntOpenHashSet out = new IntOpenHashSet();
        for (long c : ((InteriorChunkMap) in.getChunkSource().chunkMap).bettacraft$heldChunks()) out.add(cellOf(c));
        EntitySectionsAccessor e = (EntitySectionsAccessor) (Object) ((ServerLevelEntitiesAccessor) in).bettacraft$entityManager();
        for (long c : e.bettacraft$chunkLoadStatuses().keySet()) out.add(cellOf(c));
        for (long c : e.bettacraft$chunksToUnload()) out.add(cellOf(c));
        for (ServerPlayer p : in.players()) out.add(InteriorRegion.idAt(p.getBlockX(), p.getBlockZ()));
        return out;
    }

    private static int cellOf(long chunk) {
        return InteriorRegion.idAt(ChunkPos.getX(chunk) << 4, ChunkPos.getZ(chunk) << 4);
    }

    @Nullable
    private static SimpleRegionStorage entityStorage(ServerLevel in) {
        Object storage = ((EntitySectionsAccessor) (Object) ((ServerLevelEntitiesAccessor) in).bettacraft$entityManager()).bettacraft$permanentStorage();
        return storage instanceof EntityStorage es ? ((EntityStorageAccessor) es).bettacraft$storage() : null;
    }

    /** Both storages have written everything they were given so far. */
    private static CompletableFuture<Void> syncStorages(ServerLevel in) {
        CompletableFuture<Void> chunks = ((ChunkStorageAccessor) in.getChunkSource().chunkMap).bettacraft$worker().synchronize(true);
        SimpleRegionStorage entities = entityStorage(in);
        return entities == null ? chunks : CompletableFuture.allOf(chunks, entities.synchronize(true));
    }

    static Path dimensionFolder(MinecraftServer server) {
        return DimensionType.getStorageFolder(ModDimensions.TANK_INTERIOR, server.getWorldPath(LevelResource.ROOT));
    }

    // ------------------------------------------------------------------ region files

    /** The region files in the interior's folders by grid cell. */
    record CellFiles(Int2ObjectOpenHashMap<List<Path>> byCell, it.unimi.dsi.fastutil.longs.LongOpenHashSet regionFiles) {
        static CellFiles of(Path dimension) throws IOException {
            Int2ObjectOpenHashMap<List<Path>> byCell = new Int2ObjectOpenHashMap<>();
            it.unimi.dsi.fastutil.longs.LongOpenHashSet regionFiles = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
            for (String folder : new String[]{"region", "entities", "poi"}) {
                Path dir = dimension.resolve(folder);
                if (!java.nio.file.Files.isDirectory(dir)) continue;
                try (Stream<Path> s = java.nio.file.Files.list(dir)) {
                    for (Path p : (Iterable<Path>) s::iterator) {
                        int cell = cellOfFile(p.getFileName().toString());
                        if (cell < 0) continue;
                        byCell.computeIfAbsent(cell, k -> new ArrayList<>()).add(p);
                        if (folder.equals("region")) {
                            int[] rc = regionCoords(p.getFileName().toString());
                            if (rc != null) regionFiles.add(ChunkPos.asLong(rc[0], rc[1]));
                        }
                    }
                }
            }
            return new CellFiles(byCell, regionFiles);
        }

        boolean has(int id) {
            List<Path> l = byCell.get(id);
            if (l == null) return false;
            for (Path p : l) {
                String dir = p.getParent().getFileName().toString();
                if (dir.equals("region") || dir.equals("entities")) return true;
            }
            return false;
        }

        boolean region(int id, ChunkPos c) {
            return regionFiles.contains(ChunkPos.asLong(c.getRegionX(), c.getRegionZ()));
        }

        List<Path> of(int id) {
            List<Path> l = byCell.get(id);
            return l == null ? List.of() : l;
        }
    }

    /** {rx, rz} of {@code r.<rx>.<rz>.mca}, else null. */
    @Nullable
    static int[] regionCoords(String name) {
        String[] p = name.split("\\.");
        if (p.length != 4 || !p[0].equals("r") || !p[3].equals("mca")) return null;
        try {
            return new int[]{Integer.parseInt(p[1]), Integer.parseInt(p[2])};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Grid cell of a region file {@code r.x.z.mca} or an external chunk {@code c.x.z.mcc}, or -1 (not a file of ours). */
    static int cellOfFile(String name) {
        String[] p = name.split("\\.");
        if (p.length != 4) return -1;
        int x, z;
        try {
            x = Integer.parseInt(p[1]);
            z = Integer.parseInt(p[2]);
        } catch (NumberFormatException e) {
            return -1;
        }
        int bx, bz, span;
        if (p[0].equals("r") && p[3].equals("mca")) {
            bx = x << 9;
            bz = z << 9;
            span = 511;
        } else if (p[0].equals("c") && p[3].equals("mcc")) {
            bx = x << 4;
            bz = z << 4;
            span = 15;
        } else {
            return -1;
        }
        int id = InteriorRegion.idAt(bx, bz);
        return id >= 0 && id == InteriorRegion.idAt(bx + span, bz + span) ? id : -1;
    }

    /** The chunks a region file holds by its header (an absent or broken file holds none). */
    static List<ChunkPos> storedChunks(Path mca) {
        List<ChunkPos> out = new ArrayList<>();
        int[] rc = regionCoords(mca.getFileName().toString());
        if (rc == null || !java.nio.file.Files.isRegularFile(mca)) return out;
        ByteBuffer header = ByteBuffer.allocate(4096);
        try (FileChannel ch = FileChannel.open(mca, StandardOpenOption.READ)) {
            while (header.hasRemaining() && ch.read(header) >= 0) {
                // read the whole header
            }
        } catch (IOException e) {
            LOGGER.debug("Cannot read {}", mca, e);
            return out;
        }
        header.flip();
        for (int i = 0; i < 1024 && header.remaining() >= 4; i++) {
            if (header.getInt() != 0) out.add(new ChunkPos((rc[0] << 5) + (i & 31), (rc[1] << 5) + (i >> 5)));
        }
        return out;
    }

    /** The stored chunks of a cell's region files in one folder ({@code region} or {@code entities}). */
    static List<ChunkPos> storedChunks(Path folder, int id) {
        List<ChunkPos> out = new ArrayList<>();
        int[] f = InteriorRegion.cellRegionFiles(id);
        for (int rx = f[0]; rx <= f[1]; rx++)
            for (int rz = f[2]; rz <= f[3]; rz++) out.addAll(storedChunks(folder.resolve("r." + rx + "." + rz + ".mca")));
        return out;
    }

    /** Whether a stored chunk holds a block of the interior shell (floor or invisible wall). */
    static boolean hasShell(CompoundTag chunk) {
        for (Tag t : chunk.getList("sections", Tag.TAG_COMPOUND)) {
            ListTag palette = ((CompoundTag) t).getCompound("block_states").getList("palette", Tag.TAG_COMPOUND);
            for (Tag b : palette) {
                String name = ((CompoundTag) b).getString("Name");
                if (name.equals(FLOOR) || name.equals(WALL)) return true;
            }
        }
        return false;
    }

    /**
     * One chunk of a region file read without the game's storage (the start, before the dimension is loaded); null when the
     * file or the chunk is not there. Throws when it is there but cannot be read (the caller then keeps the region).
     */
    @Nullable
    static CompoundTag readChunk(Path mca, ChunkPos pos) throws IOException {
        if (!java.nio.file.Files.isRegularFile(mca)) return null;
        try (FileChannel ch = FileChannel.open(mca, StandardOpenOption.READ)) {
            ByteBuffer head = ByteBuffer.allocate(4);
            ch.read(head, 4L * ((pos.x & 31) + (pos.z & 31) * 32));
            head.flip();
            if (head.remaining() < 4) return null;
            int loc = head.getInt();
            if (loc == 0) return null;
            long offset = (long) (loc >>> 8) * 4096;
            ByteBuffer lenBuf = ByteBuffer.allocate(5);
            ch.read(lenBuf, offset);
            lenBuf.flip();
            if (lenBuf.remaining() < 5) throw new IOException("chunk " + pos + " of " + mca + " is cut off");
            int length = lenBuf.getInt();
            int type = lenBuf.get() & 0xFF;
            InputStream raw;
            if ((type & 128) != 0) {
                Path ext = mca.resolveSibling("c." + pos.x + "." + pos.z + ".mcc");
                raw = java.nio.file.Files.newInputStream(ext);
                type &= 127;
            } else {
                if (length <= 1 || length > 1024 * 4096) throw new IOException("chunk " + pos + " of " + mca + " has a bad length " + length);
                ByteBuffer data = ByteBuffer.allocate(length - 1);
                ch.read(data, offset + 5);
                raw = new ByteArrayInputStream(data.array(), 0, data.position());
            }
            RegionFileVersion version = RegionFileVersion.fromId(type);
            if (version == null) throw new IOException("chunk " + pos + " of " + mca + " has an unknown compression " + type);
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(version.wrap(raw)))) {
                return NbtIo.read(in, NbtAccounter.unlimitedHeap());
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("cannot read chunk " + pos + " of " + mca, e);
        }
    }

    // ------------------------------------------------------------------ for tests and the command

    /** The chunks stored for a region id's cell in {@code region} or {@code entities} (header of the region files). */
    public static List<ChunkPos> storedChunks(MinecraftServer server, int id, String folder) {
        return storedChunks(dimensionFolder(server).resolve(folder), id);
    }

    /** A stored chunk of the interior read from its region file (tests), or null. */
    @Nullable
    public static CompoundTag storedChunk(MinecraftServer server, ChunkPos p) throws IOException {
        return readChunk(dimensionFolder(server).resolve("region").resolve("r." + p.getRegionX() + "." + p.getRegionZ() + ".mca"), p);
    }

    /** Bytes the chunks of a cell take in its region files (sectors by the headers; not the files' size). */
    public static long storedBytes(MinecraftServer server, int id) {
        long out = 0;
        Path dir = dimensionFolder(server).resolve("region");
        int[] f = InteriorRegion.cellRegionFiles(id);
        for (int rx = f[0]; rx <= f[1]; rx++)
            for (int rz = f[2]; rz <= f[3]; rz++) {
                Path mca = dir.resolve("r." + rx + "." + rz + ".mca");
                if (!java.nio.file.Files.isRegularFile(mca)) continue;
                ByteBuffer header = ByteBuffer.allocate(4096);
                try (FileChannel ch = FileChannel.open(mca, StandardOpenOption.READ)) {
                    ch.read(header, 0);
                } catch (IOException e) {
                    continue;
                }
                header.flip();
                while (header.remaining() >= 4) out += (header.getInt() & 0xFF) * 4096L;
            }
        return out;
    }

    /** Whether anything of a cell is held (chunks, entity chunks, players). */
    public static boolean held(MinecraftServer server, int id) {
        ServerLevel in = InteriorManager.interiorLevel(server);
        return in != null && busyCells(in).contains(id);
    }

    /** The chunk and entity storages of the interior have written everything given to them so far. */
    public static CompletableFuture<Void> sync(MinecraftServer server) {
        ServerLevel in = InteriorManager.interiorLevel(server);
        return in == null ? CompletableFuture.completedFuture(null) : syncStorages(in);
    }

    // ------------------------------------------------------------------ preview (command) and the start

    /** What the clean-up finds: regions of each kind and the size of their files. */
    public record Survey(int living, int dead, int free, int kept, int deadWithFiles, long deadBytes, int deadFiles,
                         int unknownCells, long unknownBytes, long totalBytes) {}

    /** The region list and the folders as they are now (the command's preview; the start before deleting). */
    public static Survey survey(MinecraftServer server) throws IOException {
        InteriorRegions r = InteriorRegions.get(server);
        IntOpenHashSet removed = new IntOpenHashSet(r.dead());
        removed.addAll(r.dying());
        return survey(CellFiles.of(dimensionFolder(server)), r::living, removed, r.free(), r.kept(), r.nextId(), r.count());
    }

    /** {@code dead}: the removed ids (dying and dead). */
    static Survey survey(CellFiles files, java.util.function.IntPredicate living, IntSet dead, IntSet free, IntSet kept, int nextId, int livingCount) {
        int deadWithFiles = 0, deadFiles = 0, unknown = 0;
        long deadBytes = 0, unknownBytes = 0, total = 0;
        for (var e : files.byCell().int2ObjectEntrySet()) {
            int id = e.getIntKey();
            long bytes = 0;
            for (Path p : e.getValue()) bytes += size(p);
            total += bytes;
            if (dead.contains(id) || free.contains(id)) {
                if (dead.contains(id)) deadWithFiles++;
                deadBytes += bytes;
                deadFiles += e.getValue().size();
            } else if (!living.test(id) && !kept.contains(id)) {
                unknown++;
                unknownBytes += bytes;
            }
        }
        return new Survey(livingCount, dead.size(), free.size(), kept.size(), deadWithFiles, deadBytes, deadFiles, unknown, unknownBytes, total);
    }

    private static long size(Path p) {
        try {
            return java.nio.file.Files.size(p);
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Before the dimensions are loaded (no region file is open): deletes the region files of dead and free ids (except a dead
     * one whose shell is still there) and every interior region file of a living region without a chunk. Reads the region
     * list from its file. Dying ids (removed after the last complete save) are not touched.
     */
    public static void onServerStarting(MinecraftServer server) {
        if (!enabled()) {
            LOGGER.info("Interior clean-up is off (features.interior.cleanup): nothing deleted");
            return;
        }
        if (server.getLevel(ModDimensions.TANK_INTERIOR) != null) return; // already loaded: the running clean-up does it
        Path root = server.getWorldPath(LevelResource.ROOT);
        Path list = root.resolve("data").resolve(InteriorRegions.NAME + ".dat");
        Path dim = DimensionType.getStorageFolder(ModDimensions.TANK_INTERIOR, root);
        if (!java.nio.file.Files.isRegularFile(list) || !java.nio.file.Files.isDirectory(dim)) return;
        try {
            cleanFolder(list, dim);
        } catch (Exception e) {
            LOGGER.warn("Interior clean-up at the start failed; nothing more is deleted now", e);
        }
    }

    /** The start's clean-up of one world (also for tests on a copy). Returns {files deleted, bytes deleted, regions kept}. */
    public static long[] cleanFolder(Path regionList, Path dim) throws IOException {
        CompoundTag file = NbtIo.readCompressed(regionList, NbtAccounter.unlimitedHeap());
        CompoundTag data = Schemas.INTERIOR_REGIONS.migrate(file.getCompound("data"));
        IntOpenHashSet living = new IntOpenHashSet();
        int next = data.getInt("next_id");
        for (Tag t : data.getList("regions", Tag.TAG_COMPOUND)) {
            int id = ((CompoundTag) t).getInt("id");
            living.add(id);
            next = Math.max(next, id + 1);
        }
        IntOpenHashSet kept = new IntOpenHashSet(data.getIntArray("kept"));
        kept.addAll(IntArrayList.wrap(data.getIntArray("dying"))); // removed after the last complete save: not touched
        IntOpenHashSet dead = new IntOpenHashSet(), free = new IntOpenHashSet();
        for (int id : data.getIntArray("dead")) if (id > 0 && id < next && !living.contains(id) && !kept.contains(id)) dead.add(id);
        for (int id : data.getIntArray("free")) if (id > 0 && id < next && !living.contains(id) && !kept.contains(id)) free.add(id);
        CellFiles files = CellFiles.of(dim);
        List<Path> delete = new ArrayList<>();
        IntArrayList shells = new IntArrayList();
        long bytes = 0;
        for (var e : files.byCell().int2ObjectEntrySet()) {
            int id = e.getIntKey();
            if (living.contains(id) || kept.contains(id) || id >= next) continue;
            if (!dead.contains(id) && !free.contains(id)) continue;
            if (dead.contains(id)) {
                ChunkPos o = new ChunkPos(InteriorRegion.originX(id) >> 4, InteriorRegion.originZ(id) >> 4);
                CompoundTag origin;
                try {
                    origin = readChunk(dim.resolve("region").resolve("r." + o.getRegionX() + "." + o.getRegionZ() + ".mca"), o);
                } catch (IOException ex) { // in doubt: keep it
                    LOGGER.warn("Interior clean-up: cannot read the first chunk of removed region {}: it stays", id, ex);
                    shells.add(id);
                    continue;
                }
                if (origin != null && hasShell(origin)) {
                    shells.add(id);
                    continue;
                }
            }
            for (Path p : e.getValue()) {
                delete.add(p);
                bytes += size(p);
            }
        }
        // region files of living regions without any chunk (left by reads and by deleted empty chunks): the game makes them again
        for (var e : files.byCell().int2ObjectEntrySet()) {
            if (!living.contains(e.getIntKey())) continue;
            for (Path p : e.getValue()) {
                if (regionCoords(p.getFileName().toString()) == null) continue;
                // only an empty file (made by a read) or one of its header alone (8 KB): a broken or unreadable one stays
                long sz = size(p);
                if (sz == 0 || sz == 8192 && storedChunks(p).isEmpty()) {
                    delete.add(p);
                    bytes += size(p);
                }
            }
        }
        if (!shells.isEmpty()) {
            LOGGER.warn("Interior clean-up: {} removed region(s) still have their shell and are kept: {}", shells.size(), shells);
        }
        if (delete.isEmpty()) return new long[]{0, 0, shells.size()};
        LOGGER.info("Interior clean-up: {} region file(s) of removed tank interiors, {} MB: deleting them (switch: features.interior.cleanup)",
                delete.size(), String.format(java.util.Locale.ROOT, "%.1f", bytes / 1048576.0));
        long t0 = System.nanoTime();
        int deleted = 0;
        long deletedBytes = 0;
        for (Path p : delete) {
            long s = size(p);
            try {
                if (java.nio.file.Files.deleteIfExists(p)) {
                    deleted++;
                    deletedBytes += s;
                }
            } catch (IOException e) {
                LOGGER.warn("Cannot delete {}", p, e);
            }
        }
        LOGGER.info("Interior clean-up: deleted {} file(s), {} MB in {} ms", deleted, String.format(java.util.Locale.ROOT, "%.1f", deletedBytes / 1048576.0),
                (System.nanoTime() - t0) / 1_000_000);
        return new long[]{deleted, deletedBytes, shells.size()};
    }
}
