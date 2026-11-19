package org.overengineer.bettacraft.client.interior;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.plants.AquaticPlantBlock;
import org.overengineer.bettacraft.plants.PlantPart;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorSnapshot;

/**
 * M21.1: the plant meshes of Rotala, Vallisneria and Anubias also in the miniature outside the tank. The miniature
 * builder puts the blocks of these plants into buffers of their own ({@link MiniatureMesh#plantBuffers}); while a tank is
 * closer than {@code render.miniaturePlantMeshDistance} (and {@code features.visuals.plantMeshes} is on) those voxels are left
 * out and the tank draws the same meshes as inside ({@link PlantMeshRenderer}, built from the client copy of the
 * interior), scaled down with the miniature and swaying the same way. Further away the voxels come back (LOD).
 * <p>
 * Built only on changes: the first time a region is shown (all its roots), and then only the plants a delta of the
 * interior touches (their box one block larger, {@link PlantMeshIndex}); the light is read again when the tank's light
 * changes. The voxels only give way once every plant of the tank has its mesh. With an Iris shader pack the meshes shown
 * cast shadows ({@link PlantMeshRenderer} draws them in the shadow pass through {@link #renderShadow}).
 */
public final class MiniaturePlants {
    private static final int BUILDS_PER_FRAME = 4;
    /** Plants drawn outside in the last frame, tanks showing meshes, meshes built in total (smoke tests, bench). */
    public static int lastDrawn, lastTanks, builds;
    /** Render-thread time of the last frame for the meshes outside (building and drawing), for the bench. */
    public static long lastNanos;
    private static long frame;
    private static int budget;
    private static final List<Shown> SHOWN = new ArrayList<>();

    /** A miniature showing its meshes in the last main frame: where it is drawn (for the shadow pass). */
    private record Shown(State state, double x, double y, double z, float fx, float fy, float fz) {}

    /** Meshes of the plants of one client region (lives as long as the region copy). */
    static final class State {
        final Long2ObjectOpenHashMap<PlantMeshRenderer.Mesh> meshes = new Long2ObjectOpenHashMap<>();
        final LongOpenHashSet roots = new LongOpenHashSet();
        final PlantMeshIndex index = new PlantMeshIndex();
        boolean scanned;
        int lightKey = Integer.MIN_VALUE;
        /** M22.1: a mesh with polyps that react to the fish; when the fish were last looked for. */
        boolean reactive;
        long lastSense = -1000;
    }

    private MiniaturePlants() {}

    private static boolean meshedRoot(BlockState s) {
        return s.getBlock() instanceof AquaticPlantBlock pb && pb.species().meshed() && s.getValue(AquaticPlantBlock.PART) == PlantPart.ROOT;
    }

    /** A block the meshes draw instead of the miniature (the planted cutting stays a voxel). */
    static boolean meshBlock(BlockState s) {
        return s.getBlock() instanceof AquaticPlantBlock pb && pb.species().meshed() && s.getValue(AquaticPlantBlock.PART) != PlantPart.CUTTING;
    }

    private static void scan(State st, InteriorSnapshot.Section s) {
        if (s.sy() == InteriorSnapshot.FLOOR_SY) return;
        boolean any = false;
        for (BlockState b : s.palette()) any |= meshedRoot(b);
        if (!any) return;
        short[] idx = s.indices();
        for (int i = 0; i < InteriorSnapshot.SECTION_VOLUME; i++) {
            if (!meshedRoot(s.palette()[idx[i]])) continue;
            int lx = (s.sx() << 4) + (i & 15), ly = (s.sy() << 4) + ((i >> 8) & 15) - InteriorRegion.BASE_Y, lz = (s.sz() << 4) + ((i >> 4) & 15);
            st.roots.add(BlockPos.asLong(lx, ly, lz));
        }
    }

    /** {@link ClientRegion#apply}: these sections were replaced; plants touching them are built again in the next frame. */
    static void sectionsChanged(ClientRegion region, List<InteriorSnapshot.Section> changed) {
        State st = region.plants;
        if (st == null || !st.scanned) return;
        for (InteriorSnapshot.Section s : changed) {
            if (s.sy() == InteriorSnapshot.FLOOR_SY) continue;
            scan(st, s);
            int x0 = s.sx() << 4, y0 = (s.sy() << 4) - InteriorRegion.BASE_Y, z0 = s.sz() << 4;
            st.index.areaChanged(x0 - 1, y0 - 1, z0 - 1, x0 + 16, y0 + 16, z0 + 16, frame);
        }
    }

    /** Start of a main frame (before the tanks are drawn). */
    static void beginFrame() {
        frame++;
        budget = BUILDS_PER_FRAME;
        lastDrawn = 0;
        lastTanks = 0;
        lastNanos = 0;
        SHOWN.clear();
    }

    /** True when the tank is close enough for the meshes (LOD) and the feature is on. */
    static boolean wanted(double distSq) {
        int lod = ClientConfig.MINIATURE_PLANT_MESH_DISTANCE.get();
        return lod > 0 && PlantMeshRenderer.active() && distSq <= (double) lod * lod;
    }

    /**
     * Gets the meshes of the tank ready (builds what is missing or changed, within the frame's budget) and says whether
     * they are shown instead of the plant voxels: only when every plant of the tank has its mesh.
     */
    static boolean prepare(ClientLevel level, ClientRegion region, TankData t, double distSq) {
        if (!wanted(distSq)) return false;
        long t0 = System.nanoTime();
        State st = region.plants;
        if (st == null) st = region.plants = new State();
        if (!st.scanned) {
            for (InteriorSnapshot.Section s : region.sectionsSnapshot().values()) scan(st, s);
            st.scanned = true;
        }
        boolean relight = st.lightKey != region.lightKey;
        st.lightKey = region.lightKey;
        boolean ready = true;
        var it = st.roots.iterator();
        while (it.hasNext()) {
            long key = it.nextLong();
            int x = BlockPos.getX(key), y = BlockPos.getY(key), z = BlockPos.getZ(key);
            BlockState s = region.get(x, y, z);
            if (!meshedRoot(s)) {
                it.remove();
                st.meshes.remove(key);
                st.index.remove(key);
                continue;
            }
            PlantMeshRenderer.Mesh m = st.meshes.get(key);
            if ((m == null || st.index.needsRebuild(key)) && budget > 0) {
                budget--;
                m = PlantMeshRenderer.build(region::get, new BlockPos(x, y, z), ((AquaticPlantBlock) s.getBlock()).species(), PlantMeshRenderer.Style.OUTSIDE);
                st.index.put(key, m.minX, m.minY, m.minZ, m.maxX, m.maxY, m.maxZ);
                st.meshes.put(key, m);
                st.reactive |= m.reactive;
                light(level, region, t, m);
                builds++;
            } else if (m != null && relight) {
                light(level, region, t, m);
            }
            if (m == null) ready = false;
        }
        lastNanos += System.nanoTime() - t0;
        return ready;
    }

    /** Light of every block of the mesh: the light of the tank block it lies in (the miniature inherits the tank's light). */
    private static void light(ClientLevel level, ClientRegion region, TankData t, PlantMeshRenderer.Mesh m) {
        int[] l = new int[m.lightPos.size()];
        BlockPos min = t.min();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < l.length; i++) {
            long k = m.lightPos.getLong(i);
            int x = Math.clamp(Math.floorDiv(BlockPos.getX(k), region.scale), 0, t.sizeX() - 1);
            int y = Math.clamp(Math.floorDiv(Math.max(0, BlockPos.getY(k)), region.scale), 0, t.sizeY() - 1);
            int z = Math.clamp(Math.floorDiv(BlockPos.getZ(k), region.scale), 0, t.sizeZ() - 1);
            p.set(min.getX() + x, min.getY() + y, min.getZ() + z);
            l[i] = net.minecraft.util.LightCoordsUtil.pack(level.getBrightness(LightLayer.BLOCK, p), level.getBrightness(LightLayer.SKY, p));
        }
        m.lights = l;
    }

    /** Draws the meshes of a tank (main pass); {@code ps} is the event's pose stack. */
    static void draw(PoseStack ps, Vec3 cam, ClientRegion region, TankData t, MultiBufferSource buffers, float time) {
        State st = region.plants;
        if (st == null || st.meshes.isEmpty()) return;
        long t0 = System.nanoTime();
        BlockPos min = t.min();
        float m = MiniatureRenderer.MARGIN;
        float fx = (t.sizeX() - 2 * m) / region.sizeX, fy = (t.sizeY() - 2 * m) / region.sizeY, fz = (t.sizeZ() - 2 * m) / region.sizeZ;
        Shown sh = new Shown(st, min.getX() + m, min.getY() + m, min.getZ() + m, fx, fy, fz);
        SHOWN.add(sh);
        if (st.reactive) sense(t, sh, time);
        lastTanks++;
        lastDrawn += drawAll(ps, cam, sh, buffers, time);
        lastNanos += System.nanoTime() - t0;
    }

    /**
     * M22.1: the fish of the tank (real entities in the tank) close the polyps they swim by: their position in the
     * interior's coordinates, every 4 ticks.
     */
    private static void sense(TankData t, Shown sh, float time) {
        long now = (long) time;
        if (now - sh.state.lastSense < 4) return;
        sh.state.lastSense = now;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        BlockPos min = t.min();
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(min.getX(), min.getY(), min.getZ(), min.getX() + t.sizeX(),
                min.getY() + t.sizeY(), min.getZ() + t.sizeZ());
        for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box)) {
            if (e instanceof net.minecraft.world.entity.player.Player) continue;
            Vec3 p = e.position();
            float lx = (float) ((p.x - sh.x) / sh.fx), ly = (float) ((p.y + e.getBbHeight() * 0.5 - sh.y) / sh.fy), lz = (float) ((p.z - sh.z) / sh.fz);
            for (var me : sh.state.meshes.long2ObjectEntrySet()) {
                PlantMeshRenderer.Mesh mesh = me.getValue();
                if (!mesh.reactive) continue;
                long k = me.getLongKey();
                mesh.disturb(lx - BlockPos.getX(k), ly - BlockPos.getY(k), lz - BlockPos.getZ(k), time);
            }
        }
    }

    /** Polyps of a region's miniature closed right now (smoke tests). */
    public static int closedMeshes(int regionId, float time) {
        ClientRegion r = ClientInteriorCache.get(regionId);
        if (r == null || r.plants == null) return 0;
        int n = 0;
        for (PlantMeshRenderer.Mesh m : r.plants.meshes.values()) if (m.anyClosed(time)) n++;
        return n;
    }

    private static int drawAll(PoseStack ps, Vec3 cam, Shown sh, MultiBufferSource buffers, float time) {
        ps.pushPose();
        ps.translate(sh.x - cam.x, sh.y - cam.y, sh.z - cam.z);
        ps.scale(sh.fx, sh.fy, sh.fz);
        int n = 0;
        for (var e : sh.state.meshes.long2ObjectEntrySet()) {
            long k = e.getLongKey();
            ps.pushPose();
            ps.translate(BlockPos.getX(k), BlockPos.getY(k), BlockPos.getZ(k));
            PlantMeshRenderer.drawAt(e.getValue(), ps, buffers, time, PlantMeshRenderer.MINI);
            ps.popPose();
            n++;
        }
        ps.popPose();
        return n;
    }

    /** The Iris shadow pass: the meshes of the miniatures shown in the last main frame. Returns the plants drawn. */
    static int renderShadow(PoseStack ps, Vec3 cam, MultiBufferSource buffers, float time, double maxDist) {
        int n = 0;
        for (Shown sh : SHOWN) {
            double dx = sh.x - cam.x, dy = sh.y - cam.y, dz = sh.z - cam.z;
            if (dx * dx + dy * dy + dz * dz > maxDist * maxDist) continue;
            n += drawAll(ps, cam, sh, buffers, time);
        }
        return n;
    }

    /** Flushes the meshes drawn this frame (one render type for all of them). */
    static void endFrame(MultiBufferSource.BufferSource buffers) {
        if (lastDrawn > 0) buffers.endBatch(PlantMeshRenderer.MINI);
    }

    /** Meshes known for a region (smoke tests): plants with a mesh, or -1 without any state. */
    public static int meshes(int regionId) {
        ClientRegion r = ClientInteriorCache.get(regionId);
        return r == null || r.plants == null ? -1 : r.plants.meshes.size();
    }

    /** Blocks in the mesh of the plant whose root is at the interior-local position, or -1 (smoke tests). */
    public static int meshBlocks(int regionId, int lx, int ly, int lz) {
        ClientRegion r = ClientInteriorCache.get(regionId);
        if (r == null || r.plants == null) return -1;
        PlantMeshRenderer.Mesh m = r.plants.meshes.get(BlockPos.asLong(lx, ly, lz));
        return m == null ? -1 : m.blocks;
    }

    /** Roots of mesh plants known for a region, or -1 without any state (smoke tests). */
    public static int roots(int regionId) {
        ClientRegion r = ClientInteriorCache.get(regionId);
        return r == null || r.plants == null ? -1 : r.plants.roots.size();
    }
}
