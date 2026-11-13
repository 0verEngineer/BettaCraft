package org.overengineer.bettacraft.client.interior;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * Bookkeeping for the event-driven rebuild of the plant meshes ({@link PlantMeshRenderer}, M21 follow-up; before, every
 * mesh was rebuilt every 5 s). For every built mesh the box of its blocks (world block coordinates, inclusive) is kept
 * under its root key ({@code BlockPos.asLong}). A block change marks every plant whose box, one block larger (growth adds
 * blocks next to the plant), contains it for a rebuild; a light change of a section marks the plants touching it for a
 * relight (cheap: only the light values), a chunk (re)load marks them for a rebuild. The render thread takes the flags
 * when it draws the plant. Plain Java (unit tested), every method synchronized: hooks run on the client thread, the
 * chunk compiler's workers only report sections through {@link PlantMeshRenderer}.
 */
public final class PlantMeshIndex {
    private final Long2ObjectOpenHashMap<int[]> boxes = new Long2ObjectOpenHashMap<>();
    /** root -> frame of the first invalidation not yet rebuilt. */
    private final Long2LongOpenHashMap rebuild = new Long2LongOpenHashMap();
    private final LongOpenHashSet relight = new LongOpenHashSet();

    /** The mesh of {@code root} was (re)built with blocks in this box: its flags are cleared. */
    public synchronized void put(long root, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        boxes.put(root, new int[] {minX, minY, minZ, maxX, maxY, maxZ});
        rebuild.remove(root);
        relight.remove(root);
    }

    public synchronized void remove(long root) {
        boxes.remove(root);
        rebuild.remove(root);
        relight.remove(root);
    }

    public synchronized boolean contains(long root) {
        return boxes.containsKey(root);
    }

    public synchronized int size() {
        return boxes.size();
    }

    public synchronized void clear() {
        boxes.clear();
        rebuild.clear();
        relight.clear();
    }

    /** A block at (x, y, z) changed: plants within one block of it need a rebuild. Returns how many were marked. */
    public synchronized int blockChanged(int x, int y, int z, long frame) {
        int n = 0;
        for (var e : boxes.long2ObjectEntrySet()) {
            int[] b = e.getValue();
            if (x >= b[0] - 1 && x <= b[3] + 1 && y >= b[1] - 1 && y <= b[4] + 1 && z >= b[2] - 1 && z <= b[5] + 1) {
                mark(e.getLongKey(), frame);
                n++;
            }
        }
        return n;
    }

    /**
     * The section (sx, sy, sz) changed: plants touching it (or the blocks next to it) get a relight, or a rebuild with
     * {@code rebuildToo}. Returns how many were marked.
     */
    public synchronized int sectionChanged(int sx, int sy, int sz, boolean rebuildToo, long frame) {
        int x0 = sx << 4, y0 = sy << 4, z0 = sz << 4;
        return boxChanged(x0 - 1, y0 - 1, z0 - 1, x0 + 16, y0 + 16, z0 + 16, rebuildToo, frame);
    }

    /** The chunk (cx, cz) was (re)loaded or relit as a whole: like {@link #sectionChanged} for every height. */
    public synchronized int chunkChanged(int cx, int cz, boolean rebuildToo, long frame) {
        int x0 = cx << 4, z0 = cz << 4;
        return boxChanged(x0 - 1, Integer.MIN_VALUE, z0 - 1, x0 + 16, Integer.MAX_VALUE, z0 + 16, rebuildToo, frame);
    }

    /** Blocks in the box (inclusive) changed: plants within one block of it need a rebuild. Returns how many were marked. */
    public synchronized int areaChanged(int x0, int y0, int z0, int x1, int y1, int z1, long frame) {
        return boxChanged(x0 - 1, y0 - 1, z0 - 1, x1 + 1, y1 + 1, z1 + 1, true, frame);
    }

    private int boxChanged(int x0, int y0, int z0, int x1, int y1, int z1, boolean rebuildToo, long frame) {
        int n = 0;
        for (var e : boxes.long2ObjectEntrySet()) {
            int[] b = e.getValue();
            if (b[3] < x0 || b[0] > x1 || b[4] < y0 || b[1] > y1 || b[5] < z0 || b[2] > z1) continue;
            if (rebuildToo) mark(e.getLongKey(), frame);
            else relight.add(e.getLongKey());
            n++;
        }
        return n;
    }

    private void mark(long root, long frame) {
        if (!rebuild.containsKey(root)) rebuild.put(root, frame);
    }

    /** Roots (of built meshes) whose root block lies in the chunk; {@code rootX/rootZ} decode the keys. */
    public synchronized LongList rootsInChunk(int cx, int cz, java.util.function.LongToIntFunction rootX, java.util.function.LongToIntFunction rootZ) {
        LongArrayList out = new LongArrayList();
        for (long k : boxes.keySet()) if (rootX.applyAsInt(k) >> 4 == cx && rootZ.applyAsInt(k) >> 4 == cz) out.add(k);
        return out;
    }

    public synchronized boolean needsRebuild(long root) {
        return rebuild.containsKey(root);
    }

    /** Frame of the first invalidation since the last build, or -1 when the plant is not marked. */
    public synchronized long dirtySince(long root) {
        return rebuild.containsKey(root) ? rebuild.get(root) : -1;
    }

    /** True (once) when the plant was marked for a relight. */
    public synchronized boolean takeRelight(long root) {
        return relight.remove(root);
    }

    /** Plants marked for a rebuild right now. */
    public synchronized int dirtyCount() {
        return rebuild.size();
    }
}
