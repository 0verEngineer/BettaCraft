package org.overengineer.bettacraft.client.interior;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorSnapshot;

/**
 * Client copy of an interior region's blocks plus its miniature meshes. Block data is copy-on-write: a delta replaces
 * the section map, so mesh jobs running on worker threads keep reading the (immutable) map they were started with.
 * Meshes are kept per cell of 2x2x2 sections (32^3 interior blocks).
 */
public final class ClientRegion {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    final int id;
    int version;
    final int sizeX, sizeY, sizeZ, scale;
    boolean truncated;
    /** Section key ({@link SectionPos#asLong} of region-relative sx, world sy, sz) -> section. Replaced, never mutated. */
    private volatile Long2ObjectOpenHashMap<InteriorSnapshot.Section> sections;
    int nonAir;

    final Long2ObjectOpenHashMap<MiniatureMesh> meshes = new Long2ObjectOpenHashMap<>();
    final LongOpenHashSet dirtyCells = new LongOpenHashSet();
    /** Generation of the newest job per cell; older results are dropped. */
    final Long2IntOpenHashMap cellGeneration = new Long2IntOpenHashMap();
    int pendingJobs;
    int lightKey = Integer.MIN_VALUE;
    boolean shaderPack;
    int idleTicks;
    /** Stats of the current rebuild wave (logged when it completes). */
    long waveStart, waveWorkerNanos, waveUploadNanos;
    int waveCells;
    boolean limitReached;

    ClientRegion(int id, int version, int sizeX, int sizeY, int sizeZ, int scale, boolean truncated, List<InteriorSnapshot.Section> list) {
        this.id = id;
        this.version = version;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.scale = scale;
        this.truncated = truncated;
        Long2ObjectOpenHashMap<InteriorSnapshot.Section> m = new Long2ObjectOpenHashMap<>();
        for (InteriorSnapshot.Section s : list) {
            if (s.nonAir() > 0) m.put(SectionPos.asLong(s.sx(), s.sy(), s.sz()), s);
        }
        this.sections = m;
        recount();
        markAllDirty();
    }

    /** Applies a delta: replaced sections (empty = removed). Marks the affected cells and their neighbours dirty. */
    void apply(int newVersion, List<InteriorSnapshot.Section> changed) {
        Long2ObjectOpenHashMap<InteriorSnapshot.Section> m = new Long2ObjectOpenHashMap<>(sections);
        for (InteriorSnapshot.Section s : changed) {
            long key = SectionPos.asLong(s.sx(), s.sy(), s.sz());
            if (s.nonAir() > 0) m.put(key, s);
            else m.remove(key);
            int cx = s.sx() >> 1, cy = Math.max(0, s.sy()) >> 1, cz = s.sz() >> 1;
            // neighbours for face culling across the border (cheap: only the touching ones are rebuilt)
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 1) continue;
                        int nsx = s.sx() + dx, nsy = Math.max(0, s.sy()) + dy, nsz = s.sz() + dz;
                        if (nsx < 0 || nsy < 0 || nsz < 0) continue;
                        dirtyCells.add(SectionPos.asLong(nsx >> 1, nsy >> 1, nsz >> 1));
                    }
            dirtyCells.add(SectionPos.asLong(cx, cy, cz));
        }
        sections = m;
        version = newVersion;
        recount();
    }

    private void recount() {
        int n = 0;
        for (InteriorSnapshot.Section s : sections.values()) n += s.nonAir();
        nonAir = n;
    }

    void markAllDirty() {
        for (InteriorSnapshot.Section s : sections.values()) dirtyCells.add(cellOf(s));
        dirtyCells.addAll(meshes.keySet());
    }

    static long cellOf(InteriorSnapshot.Section s) {
        return SectionPos.asLong(s.sx() >> 1, Math.max(0, s.sy()) >> 1, s.sz() >> 1);
    }

    Long2ObjectOpenHashMap<InteriorSnapshot.Section> sectionsSnapshot() {
        return sections;
    }

    /** Block at interior-local coordinates (0,0,0 = first block above the floor; y = -1 is the floor substrate). */
    public BlockState get(int lx, int ly, int lz) {
        return get(sections, lx, ly, lz, sizeX, sizeY, sizeZ);
    }

    static BlockState get(Long2ObjectOpenHashMap<InteriorSnapshot.Section> map, int lx, int ly, int lz, int sizeX, int sizeY, int sizeZ) {
        if (lx < 0 || ly < -1 || lz < 0 || lx >= sizeX || ly >= sizeY || lz >= sizeZ) return AIR;
        if (ly == -1) {
            InteriorSnapshot.Section s = map.get(SectionPos.asLong(lx >> 4, InteriorSnapshot.FLOOR_SY, lz >> 4));
            return s == null ? AIR : s.palette()[s.indices()[InteriorSnapshot.index(lx & 15, 0, lz & 15)]];
        }
        int wy = ly + InteriorRegion.BASE_Y; // world y in the interior dimension; x/z origins are chunk aligned
        InteriorSnapshot.Section s = map.get(SectionPos.asLong(lx >> 4, wy >> 4, lz >> 4));
        if (s == null) return AIR;
        return s.palette()[s.indices()[InteriorSnapshot.index(lx & 15, wy & 15, lz & 15)]];
    }

    @Nullable
    MiniatureMesh mesh(long cell) {
        return meshes.get(cell);
    }

    void close() {
        for (MiniatureMesh m : meshes.values()) m.close();
        meshes.clear();
    }
}
