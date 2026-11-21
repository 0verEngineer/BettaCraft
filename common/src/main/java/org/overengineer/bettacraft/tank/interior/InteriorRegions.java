package org.overengineer.bettacraft.tank.interior;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.overengineer.bettacraft.data.schema.Schemas;

/**
 * SavedData with all interior regions (stored in the overworld's data folder).
 * <p>
 * M53c: region ids are reused. An id goes through these sets:
 * <ul>
 *     <li>{@link #regions}: living, a tank (standing, packed into an item or travelling on a contraption) has it;</li>
 *     <li>{@link #dying}: its region was removed (tank broken, grown, split, merged, turned) after the last complete save
 *     of the world: the world on the disk may still have the tank that points to it (a crash now goes back to that save),
 *     so nothing of it is touched;</li>
 *     <li>{@link #dead}: removed before a complete save of the world (players, every dimension's chunks and entities, see
 *     {@link #worldSaved}): nothing on the disk points to it any more; its chunks may still be there, {@link InteriorCleanup}
 *     deletes them (at runtime chunk by chunk, at the next start the whole region files);</li>
 *     <li>{@link #free}: nothing of it is on disk any more (checked): handed out again, lowest first;</li>
 *     <li>{@link #kept}: never handed out again, never deleted: an old interior whose region the list forgot (M42 part b,
 *     the list older than the world) or a dead region whose shell is still there (in doubt: keep it).</li>
 * </ul>
 * Ids at or above {@link #nextId} that have chunks on disk (a list older than the world after a crash) are left alone.
 */
public final class InteriorRegions extends SavedData {
    public static final String NAME = "bettacraft_interior_regions";

    private final Int2ObjectOpenHashMap<InteriorRegion> regions = new Int2ObjectOpenHashMap<>();
    private int nextId = 0;
    private final IntSortedSet dying = new IntRBTreeSet();
    private final IntSortedSet dead = new IntRBTreeSet();
    private final IntSortedSet free = new IntRBTreeSet();
    private final IntSortedSet kept = new IntRBTreeSet();
    /** M53c: serial number of the next region (a reused id gets a new serial; players logged out in its old interior leave). */
    private long nextSerial = 1;
    /** M53c: server tick an id became dead in this session (ids read as dead from the disk are not in it: dead long ago). */
    private final it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap deadSince = new it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap();
    long nowTick;
    /** Whether the last {@link #allocate} handed out a reused id (the list is then written at once, see InteriorManager). */
    boolean lastReused;

    public static InteriorRegions get(MinecraftServer server) {
        InteriorRegions d = server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(InteriorRegions::new, InteriorRegions::load, null), NAME);
        d.nowTick = server.getTickCount();
        return d;
    }

    @Nullable
    public InteriorRegion get(int id) {
        return id < 0 ? null : regions.get(id);
    }

    public InteriorRegion allocate(int sizeX, int sizeY, int sizeZ, int scale, ResourceKey<Level> dim, BlockPos tankMin,
                                   @Nullable UUID tankId, @Nullable UUID owner) {
        return allocate(sizeX, sizeY, sizeZ, scale, dim, tankMin, tankId, owner, null);
    }

    /** As above with the tank's shape (M10, null = full box). M53c: a free id first (lowest), else a new one. */
    public InteriorRegion allocate(int sizeX, int sizeY, int sizeZ, int scale, ResourceKey<Level> dim, BlockPos tankMin,
                                   @Nullable UUID tankId, @Nullable UUID owner, @Nullable byte[] cells) {
        // id 0 is skipped so that no region touches the world origin of the dimension
        if (nextId == 0) nextId = 1;
        int id;
        lastReused = !free.isEmpty();
        if (lastReused) {
            id = free.firstInt();
            free.remove(id);
        } else {
            // never an id that is known in any way (a counter set back by a list older than the world)
            do {
                id = nextId++;
            } while (regions.containsKey(id) || dying.contains(id) || dead.contains(id) || kept.contains(id) || free.contains(id));
        }
        InteriorRegion r = new InteriorRegion(id, sizeX, sizeY, sizeZ, scale, dim, tankMin, tankId, owner, cells);
        r.setSerial(nextSerial++);
        regions.put(id, r);
        setDirty();
        return r;
    }

    /**
     * M53c: the region is gone; it is dying until the next complete save of the world, then dead: its chunks are deleted by
     * {@link InteriorCleanup} and the id is used again.
     */
    public void remove(int id) {
        if (regions.remove(id) != null) {
            if (id > 0 && !kept.contains(id)) dying.add(id);
            setDirty();
        }
    }

    /**
     * M53c: the whole world was saved (players, every dimension; called after {@code MinecraftServer.saveAllChunks}): the ids
     * removed before are dead now (nothing saved points to them) and the list saying so is written at once. Returns whether
     * an id became dead.
     */
    public boolean worldSaved(MinecraftServer server) {
        if (dying.isEmpty()) return false;
        for (int id : dying) {
            dead.add(id);
            deadSince.put(id, nowTick);
        }
        dying.clear();
        saveNow(server);
        return true;
    }

    /** M53c: the region is gone but its interior must never be deleted nor handed out (see the class comment). */
    public void keep(int id) {
        regions.remove(id);
        dying.remove(id);
        dead.remove(id);
        free.remove(id);
        if (id > 0) kept.add(id);
        setDirty();
    }

    /** M53c: everything of this dead id is off the disk: it may be handed out again. */
    void freed(int id) {
        if (regions.containsKey(id) || !dead.remove(id)) return;
        deadSince.remove(id);
        free.add(id);
        setDirty();
    }

    public Iterable<InteriorRegion> all() {
        return regions.values();
    }

    public int count() {
        return regions.size();
    }

    public boolean living(int id) {
        return regions.containsKey(id);
    }

    /** Removed after the last complete save of the world (never touched yet). */
    public IntSortedSet dying() {
        return it.unimi.dsi.fastutil.ints.IntSortedSets.unmodifiable(dying);
    }

    /** Removed before a complete save of the world: their chunks may be deleted. */
    public IntSortedSet dead() {
        return it.unimi.dsi.fastutil.ints.IntSortedSets.unmodifiable(dead);
    }

    public IntSortedSet free() {
        return it.unimi.dsi.fastutil.ints.IntSortedSets.unmodifiable(free);
    }

    public IntSortedSet kept() {
        return it.unimi.dsi.fastutil.ints.IntSortedSets.unmodifiable(kept);
    }

    public int nextId() {
        return Math.max(1, nextId);
    }

    /**
     * Whether the chunks of a dead id may be deleted now: it became dead at least 5 seconds ago (the writes of the save that
     * made it dead are through the storages' queues by then).
     */
    boolean deletable(int id) {
        return dead.contains(id) && nowTick - deadSince.getOrDefault(id, Long.MIN_VALUE / 2) >= 100;
    }

    /**
     * M53c: writes the list to disk now (before a reused id gets its interior; when ids became dead): a crash afterwards
     * finds a list that is never older than the interiors on disk.
     */
    public void saveNow(MinecraftServer server) {
        setDirty();
        java.nio.file.Path file = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data").resolve(NAME + ".dat");
        try {
            java.nio.file.Files.createDirectories(file.getParent());
            save(file.toFile(), server.registryAccess());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("bettacraft/interior").error("Could not write the interior region list", e);
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        Schemas.INTERIOR_REGIONS.stamp(tag);
        tag.putInt("next_id", nextId);
        tag.putLong("next_serial", nextSerial);
        ListTag list = new ListTag();
        for (InteriorRegion r : regions.values()) list.add(r.save());
        tag.put("regions", list);
        tag.putIntArray("dying", dying.toIntArray());
        tag.putIntArray("dead", dead.toIntArray());
        tag.putIntArray("free", free.toIntArray());
        tag.putIntArray("kept", kept.toIntArray());
        return tag;
    }

    private static InteriorRegions load(CompoundTag raw, HolderLookup.Provider registries) {
        CompoundTag tag = Schemas.INTERIOR_REGIONS.migrate(raw);
        InteriorRegions d = new InteriorRegions();
        d.nextId = tag.getInt("next_id");
        d.nextSerial = Math.max(1, tag.getLong("next_serial"));
        for (Tag t : tag.getList("regions", Tag.TAG_COMPOUND)) {
            InteriorRegion r = InteriorRegion.load((CompoundTag) t);
            d.regions.put(r.id(), r);
            d.nextId = Math.max(d.nextId, r.id() + 1);
            d.nextSerial = Math.max(d.nextSerial, r.serial() + 1);
        }
        // a living id is never dead, free or kept (in doubt the region list wins)
        for (int id : tag.getIntArray("kept")) if (id > 0 && !d.regions.containsKey(id)) d.kept.add(id);
        // a dying id stays dying until the first complete save of this session (the world may have gone back to an older save)
        for (int id : tag.getIntArray("dying")) if (id > 0 && id < d.nextId && !d.regions.containsKey(id) && !d.kept.contains(id)) d.dying.add(id);
        for (int id : tag.getIntArray("dead")) if (id > 0 && id < d.nextId && !d.regions.containsKey(id) && !d.kept.contains(id) && !d.dying.contains(id)) d.dead.add(id);
        for (int id : tag.getIntArray("free")) if (id > 0 && id < d.nextId && !d.regions.containsKey(id) && !d.kept.contains(id) && !d.dying.contains(id) && !d.dead.contains(id)) d.free.add(id);
        return d;
    }
}
