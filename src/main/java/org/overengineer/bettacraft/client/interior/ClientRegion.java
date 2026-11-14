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
    /** Outlets of internal filters (M4), interior-local block coordinates; recomputed when sections change. */
    volatile List<Outlet> outlets = List.of();

    /** Outlet of an internal filter: centre of the outlet face (interior blocks) and its direction. */
    public record Outlet(double x, double y, double z, net.minecraft.core.Direction facing, double share, boolean pump) {
        public Outlet(double x, double y, double z, net.minecraft.core.Direction facing) {
            this(x, y, z, facing, 1.0, false);
        }
    }

    /** M23: an air stone or a sponge filter that gets air: where its bubbles start (interior blocks) and its share 0..1. */
    public record Bubbler(double x, double y, double z, double share, boolean sponge) {}

    /** M23: the running air stones and sponge filters; recomputed with the outlets. */
    volatile List<Bubbler> bubblers = List.of();

    public List<Bubbler> bubblers() {
        return bubblers;
    }

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
    /** M21.1: the plant meshes of the miniature ({@link MiniaturePlants}), made the first time they are shown. */
    MiniaturePlants.State plants;
    /** The meshes stand in for the plant voxels in this frame (set by the opaque pass, read by the translucent one). */
    boolean plantMeshesShown;
    /** M22.1: the swaying water plants of this tank sway right now (else their static buffers are drawn). */
    boolean swaying;

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
        findOutlets();
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
        findOutlets();
        MiniaturePlants.sectionsChanged(this, changed);
    }

    /** Scans the sections that contain an internal filter anchor (cheap: palettes are checked first). */
    private void findOutlets() {
        java.util.ArrayList<Outlet> out = new java.util.ArrayList<>();
        int[] shape = org.overengineer.bettacraft.equipment.SegmentBlock.filterShape(scale);
        int[] pump = org.overengineer.bettacraft.equipment.SegmentBlock.pumpShape(scale); // M20: flow pumps blow a jet too
        int[] corner = org.overengineer.bettacraft.equipment.SegmentBlock.cornerShape(scale);
        int[] spongeShape = org.overengineer.bettacraft.equipment.SegmentBlock.spongeShape(scale);
        java.util.ArrayList<Bubbler> bub = new java.util.ArrayList<>();
        for (InteriorSnapshot.Section s : sections.values()) {
            if (s.sy() == InteriorSnapshot.FLOOR_SY) continue;
            boolean any = false;
            for (BlockState st : s.palette()) any |= isFilterAnchor(st) || isBubbler(st);
            if (!any) continue;
            short[] idx = s.indices();
            for (int i = 0; i < InteriorSnapshot.SECTION_VOLUME; i++) {
                BlockState st = s.palette()[idx[i]];
                if (isBubbler(st)) {
                    if (bub.size() >= 128) continue;
                    int bx = (s.sx() << 4) + (i & 15), by = (s.sy() << 4) + ((i >> 8) & 15) - InteriorRegion.BASE_Y, bz = (s.sz() << 4) + ((i >> 4) & 15);
                    boolean sp = st.is(org.overengineer.bettacraft.registry.ModBlocks.SPONGE_FILTER_SEGMENT.get());
                    double share = org.overengineer.bettacraft.equipment.Regulator.share(st);
                    if (sp) bub.add(new Bubbler(bx + spongeShape[0] / 2.0, by + spongeShape[1], bz + spongeShape[0] / 2.0, 1.0, true));
                    else if (share > 0) bub.add(new Bubbler(bx + 0.5, by + 0.4, bz + 0.5, share, false));
                    continue;
                }
                if (!isFilterAnchor(st)) continue;
                double share = org.overengineer.bettacraft.equipment.Regulator.share(st);
                if (share <= 0) continue; // M23: switched to 0 %: no jet
                boolean isPump = st.is(org.overengineer.bettacraft.registry.ModBlocks.PUMP_SEGMENT.get());
                boolean isCorner = st.is(org.overengineer.bettacraft.registry.ModBlocks.CORNER_FILTER_SEGMENT.get());
                int foot = isPump ? pump[0] : isCorner ? corner[0] : shape[0], height = isPump ? pump[1] : isCorner ? corner[1] : shape[1];
                int lx = (s.sx() << 4) + (i & 15), ly = (s.sy() << 4) + ((i >> 8) & 15) - InteriorRegion.BASE_Y, lz = (s.sz() << 4) + ((i >> 4) & 15);
                net.minecraft.core.Direction f = st.getValue(org.overengineer.bettacraft.equipment.SegmentBlock.FACING);
                double cx = lx + foot / 2.0 + f.getStepX() * (foot / 2.0 + 0.05);
                double cz = lz + foot / 2.0 + f.getStepZ() * (foot / 2.0 + 0.05);
                double cy = ly + (isPump ? height / 2.0 : Math.max(1, height - 1.5));
                out.add(new Outlet(cx, cy, cz, f, share, isPump));
                if (out.size() >= 64) break;
            }
        }
        outlets = List.copyOf(out);
        bubblers = List.copyOf(bub);
    }

    /** M23: an air stone or the anchor of a sponge filter that gets air. */
    private static boolean isBubbler(BlockState s) {
        if (!s.hasProperty(org.overengineer.bettacraft.equipment.DeviceSegmentBlock.ACTIVE) || !s.getValue(org.overengineer.bettacraft.equipment.DeviceSegmentBlock.ACTIVE)) return false;
        if (s.is(org.overengineer.bettacraft.registry.ModBlocks.AIR_STONE.get())) return true;
        return s.is(org.overengineer.bettacraft.registry.ModBlocks.SPONGE_FILTER_SEGMENT.get())
                && s.getValue(org.overengineer.bettacraft.equipment.SegmentBlock.PART) == org.overengineer.bettacraft.equipment.SegmentBlock.Part.ANCHOR;
    }

    private static boolean isFilterAnchor(BlockState s) {
        return (s.is(org.overengineer.bettacraft.registry.ModBlocks.FILTER_SEGMENT.get()) || s.is(org.overengineer.bettacraft.registry.ModBlocks.PUMP_SEGMENT.get())
                || s.is(org.overengineer.bettacraft.registry.ModBlocks.CORNER_FILTER_SEGMENT.get()))
                && s.getValue(org.overengineer.bettacraft.equipment.SegmentBlock.PART) == org.overengineer.bettacraft.equipment.SegmentBlock.Part.ANCHOR;
    }

    public List<Outlet> outlets() {
        return outlets;
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
