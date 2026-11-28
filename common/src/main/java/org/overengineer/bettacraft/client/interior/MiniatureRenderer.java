package org.overengineer.bettacraft.client.interior;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.platform.client.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.InteriorSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders tank interiors as miniatures (PLAN 7.4).
 * <ul>
 *     <li>Meshes are built per cell (2x2x2 sections = 32^3 interior blocks) on worker threads with the
 *     {@link BlockRenderDispatcher} (face culling), like vanilla's section compiler. Only the upload to the GPU happens
 *     on the render thread, within a time budget per frame (M3; M0 built the whole region on the render thread).</li>
 *     <li>A delta from the server rebuilds only the touched cells (and neighbours for culling).</li>
 *     <li>At most {@code interior.maxRenderedBlocks} blocks per tank are meshed (cells in y/z/x order).</li>
 *     <li>Solid and cutout layers are drawn at {@code AFTER_BLOCK_ENTITIES}; the translucent layer at
 *     {@code AFTER_TRANSLUCENT_BLOCKS}, skipped during the Iris shadow pass (Iris analysis, M3). Meshes are rebuilt
 *     when an Iris shader pack is switched on or off, because Iris extends the vertex format while a pack is active.</li>
 *     <li>The floor substrate is drawn as a flattened layer just below the interior's floor.</li>
 *     <li>Internal filters get a water jet at their outlet ({@link MiniatureJets}, M4), drawn with the opaque pass
 *     (before the tank water, so it shows through the water) and skipped in the Iris shadow pass.</li>
 * </ul>
 */
public final class MiniatureRenderer {
    static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/miniature");
    private static final List<RenderType> OPAQUE = List.of(RenderType.solid(), RenderType.cutoutMipped(), RenderType.cutout());
    private static final List<RenderType> LAYERS = List.of(RenderType.solid(), RenderType.cutoutMipped(), RenderType.cutout(), RenderType.translucent());
    /** Keeps the miniature 0.5 px inside the glass. */
    static final float MARGIN = 0.5F / 16F;
    private static final int MAX_IN_FLIGHT = 6;
    private static final long UPLOAD_BUDGET_NANOS = 3_000_000L;

    private static final AtomicInteger THREADS = new AtomicInteger();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors() / 4)), r -> {
        Thread t = new Thread(r, "Bettacraft Miniature Mesher " + THREADS.incrementAndGet());
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });
    private static final ConcurrentLinkedQueue<Result> DONE = new ConcurrentLinkedQueue<>();
    private static int inFlight;
    /** Render-thread time spent on miniatures in the last frame (scheduling + uploads + draws), for the bench. */
    public static volatile long lastFrameNanos;
    /** M63b: a dev scene hides the miniatures for a picture without them (the check that none is drawn outside its tank). */
    public static volatile boolean hiddenForTest;

    private record Result(int regionId, long cell, int generation, Map<RenderType, MeshData> data, Map<RenderType, MeshData> plantData,
                          Map<RenderType, MeshData> swayData, MiniatureSway.Data sway,
                          List<ByteBufferBuilder> memory, int quads, int blocks, long workerNanos, int minX, int minY, int minZ) {
        void free() {
            for (ByteBufferBuilder m : memory) m.close();
        }
    }

    /** M32 part b: a travelling tank drawn this frame by its block entity renderer (pose at its controller block). */
    private record Travelling(TankData tank, BlockPos origin, Matrix4f pose) {}

    private static final List<Travelling> TRAVELLING = new java.util.ArrayList<>();

    private MiniatureRenderer() {}

    /**
     * M32 part b: the miniature of a tank on a Create contraption or an airship, at the pose its block entity was drawn with
     * (camera-relative, at the controller block {@code origin}); drawn in the next miniature passes.
     */
    public static void queueTravelling(TankData t, BlockPos origin, Matrix4f pose) {
        if (TRAVELLING.size() < 64) TRAVELLING.add(new Travelling(t, origin.immutable(), pose));
    }

    /** M17 (logout): finished meshes nobody will upload any more give their native memory back now, not in the next world. */
    public static void drainResults() {
        Result r;
        while ((r = DONE.poll()) != null) {
            inFlight = Math.max(0, inFlight - 1);
            r.free();
        }
    }

    public static void onRenderStage(RenderLevelStageEvent event) {
        RenderLevelStageEvent.Stage stage = event.getStage();
        boolean opaque = stage == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES;
        if (!opaque && stage != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!opaque && IrisCompat.renderingShadowPass()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || hiddenForTest) return;
        long t0 = System.nanoTime();
        Vec3 cam = event.getCamera().getPosition();
        double maxDist = ClientConfig.MINIATURE_RENDER_DISTANCE.get();
        if (opaque) {
            MiniaturePlants.beginFrame();
            MiniatureSway.beginFrame();
            uploadResults();
            boolean pack = IrisCompat.shaderPackInUse();
            for (TankBlockEntity be : TankBlockEntity.CLIENT_CONTROLLERS) {
                TankData t = be.getTank();
                if (t == null || t.regionId() < 0 || be.getLevel() != mc.level) continue;
                if (org.overengineer.bettacraft.client.render.TravellingTankRenderer.travelling(be)) continue; // M32 part b: drawn at the ship's pose
                ClientRegion region = ClientInteriorCache.get(t.regionId());
                if (region == null) continue;
                BlockPos min = t.min();
                AABB box = new AABB(min.getX(), min.getY(), min.getZ(), min.getX() + t.sizeX(), min.getY() + t.sizeY(), min.getZ() + t.sizeZ());
                if (box.getCenter().distanceToSqr(cam) > maxDist * maxDist) continue;
                int lightKey = lightKey(mc, t, region);
                if (region.lightKey != lightKey || region.shaderPack != pack) {
                    region.lightKey = lightKey;
                    region.shaderPack = pack;
                    region.markAllDirty();
                }
                schedule(mc.level, region, t);
            }
        }
        for (TankBlockEntity be : TankBlockEntity.CLIENT_CONTROLLERS) {
            TankData t = be.getTank();
            if (t == null || t.regionId() < 0 || be.getLevel() != mc.level) continue;
            if (org.overengineer.bettacraft.client.render.TravellingTankRenderer.travelling(be)) continue;
            ClientRegion region = ClientInteriorCache.get(t.regionId());
            if (region == null || region.meshes.isEmpty()) continue;
            BlockPos min = t.min();
            AABB box = new AABB(min.getX(), min.getY(), min.getZ(), min.getX() + t.sizeX(), min.getY() + t.sizeY(), min.getZ() + t.sizeZ());
            double distSq = box.getCenter().distanceToSqr(cam);
            if (distSq > maxDist * maxDist || !event.getFrustum().isVisible(box)) {
                if (opaque) region.plantMeshesShown = false;
                continue;
            }
            // M21.1: close enough, the plants with real leaves instead of their voxels (once every plant has its mesh)
            if (opaque) region.plantMeshesShown = MiniaturePlants.prepare(mc.level, region, t, distSq);
            // M22.1: close enough, kelp, seagrass, corals and the water plants of other mods sway
            if (opaque) region.swaying = MiniatureSway.wanted(distSq);
            draw(event, region, t, cam, opaque ? OPAQUE : List.of(RenderType.translucent()), region.plantMeshesShown, region.swaying);
            float time = mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
            if (opaque && region.plantMeshesShown) MiniaturePlants.draw(event.getPoseStack(), cam, region, t, mc.renderBuffers().bufferSource(), time);
            if (opaque && region.swaying) MiniatureSway.draw(event.getPoseStack(), cam, region, t, mc.renderBuffers().bufferSource(), time);
            // jets: before the translucent terrain, so the tank water is blended over the part below the surface
            // (drawn after it, the water's depth would hide it); never in the Iris shadow pass
            if (opaque && !IrisCompat.renderingShadowPass()) MiniatureJets.render(event, be, region, cam);
        }
        // M32 part b: travelling tanks (no plant meshes, no sway, no jets: their voxels; lit like open daylight)
        for (Travelling tr : TRAVELLING) {
            ClientRegion region = ClientInteriorCache.get(tr.tank().regionId());
            if (region == null) continue;
            if (opaque) schedule(mc.level, region, tr.tank(), true);
            if (!region.meshes.isEmpty()) {
                drawTravelling(region, tr, new Matrix4f(event.getModelViewMatrix()), event.getProjectionMatrix(),
                        opaque ? OPAQUE : List.of(RenderType.translucent()));
            }
        }
        if (!opaque) TRAVELLING.clear();
        if (opaque) MiniaturePlants.endFrame(mc.renderBuffers().bufferSource());
        if (opaque && MiniatureSway.lastQuads > 0) mc.renderBuffers().bufferSource().endBatch(PlantMeshRenderer.Mini.TYPE);
        long dt = System.nanoTime() - t0;
        lastFrameNanos = opaque ? dt : lastFrameNanos + dt;
    }

    /**
     * When the light around the tank changes, the miniature is lit again. Three tank blocks are checked every frame; M30 part b:
     * once a second the light of every tank block (a hash): the meshes keep the light of the moment they were built, so a cell
     * built before the world's light reached its tank block (a tank built in freshly loaded chunks, where the client gets the
     * light later) stayed dark for good when the three checked blocks were lit already (the dark floor stripes).
     */
    private static int lightKey(Minecraft mc, TankData t, ClientRegion region) {
        BlockPos c = t.min().offset(t.sizeX() / 2, t.sizeY() / 2, t.sizeZ() / 2);
        long now = mc.level.getGameTime();
        if (region.lightHashAt < 0 || now - region.lightHashAt >= 20 || now < region.lightHashAt) {
            region.lightHashAt = now;
            region.lightHash = lightHash(mc, t);
        }
        return mc.level.getBrightness(LightLayer.BLOCK, c) * 31 + mc.level.getBrightness(LightLayer.SKY, c)
                + mc.level.getBrightness(LightLayer.BLOCK, t.min()) * 961 + mc.level.getBrightness(LightLayer.SKY, t.max()) * 29791
                + region.lightHash * 923521;
    }

    /** M30 part b: a hash of the block and sky light of all tank blocks. */
    static int lightHash(Minecraft mc, TankData t) {
        int h = 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockPos min = t.min();
        for (int y = 0; y < t.sizeY(); y++)
            for (int z = 0; z < t.sizeZ(); z++)
                for (int x = 0; x < t.sizeX(); x++) {
                    p.set(min.getX() + x, min.getY() + y, min.getZ() + z);
                    h = h * 31 + (mc.level.getBrightness(LightLayer.BLOCK, p) << 4 | mc.level.getBrightness(LightLayer.SKY, p));
                }
        return h;
    }

    // ------------------------------------------------------------------ scheduling

    /** Cells allowed by the render limit (y, z, x order), with their block counts. */
    private static LongOpenHashSet allowedCells(ClientRegion region, Long2ObjectOpenHashMap<InteriorSnapshot.Section> map) {
        Long2IntOpenHashMap counts = new Long2IntOpenHashMap();
        for (InteriorSnapshot.Section s : map.values()) counts.addTo(ClientRegion.cellOf(s), s.nonAir());
        LongArrayList keys = new LongArrayList(counts.keySet());
        keys.sort((a, b) -> {
            int c = Integer.compare(SectionPos.y(a), SectionPos.y(b));
            if (c != 0) return c;
            c = Integer.compare(SectionPos.z(a), SectionPos.z(b));
            return c != 0 ? c : Integer.compare(SectionPos.x(a), SectionPos.x(b));
        });
        int limit = ServerConfig.getOr(ServerConfig.INTERIOR_MAX_RENDERED_BLOCKS, 262144);
        LongOpenHashSet out = new LongOpenHashSet();
        long sum = 0;
        region.limitReached = false;
        for (long k : keys) {
            sum += counts.get(k);
            if (sum > limit) {
                region.limitReached = true;
                break;
            }
            out.add(k);
        }
        return out;
    }

    private static void schedule(ClientLevel level, ClientRegion region, TankData t) {
        schedule(level, region, t, false);
    }

    /** {@code daylight}: a travelling tank (its blocks are not where its data says), meshed as if in open daylight. */
    private static void schedule(ClientLevel level, ClientRegion region, TankData t, boolean daylight) {
        if (region.dirtyCells.isEmpty() || inFlight >= MAX_IN_FLIGHT) return;
        Long2ObjectOpenHashMap<InteriorSnapshot.Section> map = region.sectionsSnapshot();
        LongOpenHashSet allowed = allowedCells(region, map);
        byte[] light;
        if (daylight) {
            light = new byte[t.sizeX() * t.sizeY() * t.sizeZ()];
            java.util.Arrays.fill(light, (byte) 0x0F);
        } else {
            light = RegionBlockGetter.captureLight(level, t.min(), t.sizeX(), t.sizeY(), t.sizeZ());
        }
        if (region.pendingJobs == 0) {
            region.waveStart = System.nanoTime();
            region.waveWorkerNanos = 0;
            region.waveUploadNanos = 0;
            region.waveCells = 0;
        }
        var it = region.dirtyCells.iterator();
        while (it.hasNext() && inFlight < MAX_IN_FLIGHT) {
            long cell = it.nextLong();
            it.remove();
            int gen = region.cellGeneration.addTo(cell, 1) + 1;
            if (!allowed.contains(cell)) {
                MiniatureMesh old = region.meshes.remove(cell);
                if (old != null) old.close();
                continue;
            }
            inFlight++;
            region.pendingJobs++;
            RegionBlockGetter getter = new RegionBlockGetter(map, region.sizeX, region.sizeY, region.sizeZ, region.scale, level, t.min(),
                    t.sizeX(), t.sizeY(), t.sizeZ(), light);
            int id = region.id;
            POOL.execute(() -> {
                try {
                    DONE.add(build(id, cell, gen, map, getter));
                } catch (Throwable e) {
                    org.overengineer.bettacraft.debug.LogArea.RENDER.errorThrottled("mesh_job", "Miniature mesh job failed for region {} cell {}", id, cell, e); // M57: at most once a minute
                    DONE.add(new Result(id, cell, gen, Map.of(), Map.of(), Map.of(), null, List.of(), 0, 0, 0, 0, 0, 0));
                }
            });
        }
    }

    /** Worker thread: meshes all blocks of one cell. */
    private static Result build(int regionId, long cell, int gen, Long2ObjectOpenHashMap<InteriorSnapshot.Section> map, RegionBlockGetter getter) {
        long t0 = System.nanoTime();
        BlockRenderDispatcher brd = Minecraft.getInstance().getBlockRenderer();
        RandomSource rand = RandomSource.create();
        PoseStack ps = new PoseStack();
        Map<RenderType, ByteBufferBuilder> memory = new IdentityHashMap<>();
        Map<RenderType, BufferBuilder> builders = new IdentityHashMap<>();
        // M21.1: the mesh plants into buffers of their own (left out while the tank shows their meshes)
        Map<RenderType, ByteBufferBuilder> plantMemory = new IdentityHashMap<>();
        Map<RenderType, BufferBuilder> plantBuilders = new IdentityHashMap<>();
        // M22.1: the swaying water plants into buffers of their own, and their quads once more for drawing them swaying
        Map<RenderType, ByteBufferBuilder> swayMemory = new IdentityHashMap<>();
        Map<RenderType, BufferBuilder> swayBuilders = new IdentityHashMap<>();
        MiniatureSway.Data swayData = null;
        MiniatureSway.Capture capture = null;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int cx = SectionPos.x(cell), cy = SectionPos.y(cell), cz = SectionPos.z(cell);
        int blocks = 0;
        List<InteriorSnapshot.Section> list = new ArrayList<>();
        for (int sx = cx * 2; sx < cx * 2 + 2; sx++)
            for (int sz = cz * 2; sz < cz * 2 + 2; sz++) {
                for (int sy = cy * 2; sy < cy * 2 + 2; sy++) {
                    InteriorSnapshot.Section s = map.get(SectionPos.asLong(sx, sy, sz));
                    if (s != null) list.add(s);
                }
                if (cy == 0) {
                    InteriorSnapshot.Section f = map.get(SectionPos.asLong(sx, InteriorSnapshot.FLOOR_SY, sz));
                    if (f != null) list.add(f);
                }
            }
        for (InteriorSnapshot.Section s : list) {
            boolean floor = s.sy() == InteriorSnapshot.FLOOR_SY;
            for (int i = 0; i < InteriorSnapshot.SECTION_VOLUME; i++) {
                int pi = s.indices()[i];
                if (pi == 0) continue;
                BlockState state = s.palette()[pi];
                if (state.getRenderShape() != RenderShape.MODEL) continue;
                int lx = (s.sx() << 4) + (i & 15);
                int ly = floor ? -1 : (s.sy() << 4) + ((i >> 8) & 15) - InteriorRegion.BASE_Y;
                int lz = (s.sz() << 4) + ((i >> 4) & 15);
                pos.set(lx, ly, lz);
                try {
                    BakedModel model = brd.getBlockModel(state);
                    Object md = org.overengineer.bettacraft.platform.client.ClientPlatform.get().modelData(model, getter, pos, state);
                    rand.setSeed(state.getSeed(pos));
                    boolean plant = !floor && MiniaturePlants.meshBlock(state);
                    boolean sway = !floor && !plant && MiniatureSway.candidate(state);
                    if (sway) {
                        if (capture == null) capture = new MiniatureSway.Capture(swayData = new MiniatureSway.Data());
                        capture.footY = MiniatureSway.footOf(getter, lx, ly, lz);
                        capture.phase = (float) ((Math.floorMod(lx, 64) * 0.37 + Math.floorMod(lz, 64) * 0.61) % (Math.PI * 2));
                    }
                    for (RenderType rt : org.overengineer.bettacraft.platform.client.ClientPlatform.get().renderTypes(model, state, rand, md)) {
                        RenderType layer = LAYERS.contains(rt) ? rt : RenderType.translucent();
                        Map<RenderType, ByteBufferBuilder> mems = plant ? plantMemory : sway ? swayMemory : memory;
                        BufferBuilder bb = (plant ? plantBuilders : sway ? swayBuilders : builders).computeIfAbsent(layer, k -> {
                            ByteBufferBuilder mem = new ByteBufferBuilder(plant || sway ? 1 << 12 : 1 << 16);
                            mems.put(k, mem);
                            return new BufferBuilder(mem, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                        });
                        if (sway && layer != RenderType.translucent()) {
                            ps.pushPose();
                            ps.translate(lx, ly, lz);
                            rand.setSeed(state.getSeed(pos));
                            org.overengineer.bettacraft.platform.client.ClientPlatform.get().renderBatched(brd, state, pos, getter, ps, capture, true, rand, md, rt);
                            ps.popPose();
                            rand.setSeed(state.getSeed(pos));
                        }
                        ps.pushPose();
                        if (floor) {
                            // substrate: flattened to 1/16 of an interior block just below the floor
                            ps.translate(lx, -1F / 16F, lz);
                            ps.scale(1F, 1F / 16F, 1F);
                        } else {
                            ps.translate(lx, ly, lz);
                        }
                        org.overengineer.bettacraft.platform.client.ClientPlatform.get().renderBatched(brd, state, pos, getter, ps, bb, true, rand, md, rt);
                        ps.popPose();
                    }
                    blocks++;
                } catch (RuntimeException e) {
                    // a broken model of another mod must not kill the whole miniature
                }
            }
        }
        Map<RenderType, MeshData> data = new IdentityHashMap<>();
        Map<RenderType, MeshData> plantData = new IdentityHashMap<>();
        Map<RenderType, MeshData> swayStatic = new IdentityHashMap<>();
        int quads = 0;
        for (RenderType layer : LAYERS) {
            for (int pass = 0; pass < 3; pass++) {
                BufferBuilder bb = (pass == 0 ? builders : pass == 1 ? plantBuilders : swayBuilders).get(layer);
                if (bb == null) continue;
                MeshData d = bb.build();
                if (d == null) continue;
                (pass == 0 ? data : pass == 1 ? plantData : swayStatic).put(layer, d);
                quads += d.drawState().vertexCount() / 4;
            }
        }
        List<ByteBufferBuilder> mem = new ArrayList<>(memory.values());
        mem.addAll(plantMemory.values());
        mem.addAll(swayMemory.values());
        return new Result(regionId, cell, gen, data, plantData, swayStatic, swayData != null && swayData.quads() > 0 ? swayData : null, mem, quads,
                blocks, System.nanoTime() - t0, cx * 32, cy * 32 - InteriorRegion.BASE_Y, cz * 32);
    }

    /** Render thread: uploads finished meshes within the frame budget. */
    private static void uploadResults() {
        long start = System.nanoTime();
        Result r;
        while (System.nanoTime() - start < UPLOAD_BUDGET_NANOS && (r = DONE.poll()) != null) {
            inFlight = Math.max(0, inFlight - 1);
            long u0 = System.nanoTime();
            ClientRegion region = ClientInteriorCache.get(r.regionId);
            try {
                if (region == null) continue;
                region.pendingJobs = Math.max(0, region.pendingJobs - 1);
                region.waveWorkerNanos += r.workerNanos;
                if (region.cellGeneration.get(r.cell) != r.generation) continue; // superseded by a newer job
                MiniatureMesh old = region.meshes.remove(r.cell);
                if (old != null) old.close();
                if (!r.data.isEmpty() || !r.plantData.isEmpty() || !r.swayData.isEmpty()) {
                    MiniatureMesh mesh = new MiniatureMesh();
                    mesh.minX = r.minX;
                    mesh.minY = r.minY;
                    mesh.minZ = r.minZ;
                    mesh.extent = 32;
                    mesh.sway = r.sway;
                    for (int pass = 0; pass < 3; pass++) {
                        for (RenderType layer : LAYERS) {
                            MeshData d = (pass == 0 ? r.data : pass == 1 ? r.plantData : r.swayData).get(layer);
                            if (d == null) continue;
                            mesh.quads += d.drawState().vertexCount() / 4;
                            mesh.vertexBytes += d.vertexBuffer().remaining();
                            VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
                            vb.bind();
                            vb.upload(d); // closes the mesh data
                            VertexBuffer.unbind();
                            (pass == 0 ? mesh.buffers : pass == 1 ? mesh.plantBuffers : mesh.swayBuffers).put(layer, vb);
                        }
                    }
                    region.meshes.put(r.cell, mesh);
                }
                region.waveCells++;
            } finally {
                r.free();
                if (region != null) {
                    region.waveUploadNanos += System.nanoTime() - u0;
                    if (region.pendingJobs == 0 && region.dirtyCells.isEmpty() && region.waveCells > 0) logWave(region);
                }
            }
        }
    }

    private static void logWave(ClientRegion region) {
        boolean stats = ClientConfig.LOG_MINIATURE_STATS.get();
        if (!stats && !org.overengineer.bettacraft.debug.LogArea.RENDER.debug) return; // M57: also with the render area of the debug log
        long quads = 0, bytes = 0;
        for (MiniatureMesh m : region.meshes.values()) {
            quads += m.quads;
            bytes += m.vertexBytes;
        }
        logStats(stats, "Miniature region {} ({}x{}x{}, scale {}) v{}: {} cell(s) rebuilt in {} ms (worker threads {} ms, render thread uploads {} ms); "
                        + "{} cells, {} blocks, {} quads, {} KiB VRAM{}",
                region.id, region.sizeX, region.sizeY, region.sizeZ, region.scale, region.version, region.waveCells,
                fmt((System.nanoTime() - region.waveStart) / 1e6), fmt(region.waveWorkerNanos / 1e6), fmt(region.waveUploadNanos / 1e6),
                region.meshes.size(), region.nonAir, quads, bytes / 1024, region.limitReached ? " (render limit reached)" : "");
        region.waveCells = 0;
    }

    /** M57: the statistics into the normal log ({@code logMiniatureStats}) or the debug log (render area). */
    static void logStats(boolean stats, String message, Object... args) {
        if (stats) LOGGER.info(message, args);
        else if (org.overengineer.bettacraft.debug.LogArea.RENDER.debug) org.overengineer.bettacraft.debug.LogArea.RENDER.debug(message, args);
    }

    private static String fmt(double d) {
        return String.format(java.util.Locale.ROOT, "%.1f", d);
    }

    // ------------------------------------------------------------------ drawing

    private static void draw(RenderLevelStageEvent event, ClientRegion region, TankData t, Vec3 cam, List<RenderType> layers, boolean noPlants,
                             boolean noSway) {
        BlockPos min = t.min();
        float fx = (t.sizeX() - 2 * MARGIN) / region.sizeX;
        float fy = (t.sizeY() - 2 * MARGIN) / region.sizeY;
        float fz = (t.sizeZ() - 2 * MARGIN) / region.sizeZ;
        // M67.5: the translation goes into ChunkOffset (in mesh units), so the fog sees the camera distance (see FogSafe)
        FogSafe fog = new FogSafe(new Matrix4f(event.getModelViewMatrix()), null,
                (float) (min.getX() - cam.x) + MARGIN, (float) (min.getY() - cam.y) + MARGIN, (float) (min.getZ() - cam.z) + MARGIN, fx, fy, fz);
        Matrix4f proj = event.getProjectionMatrix();
        for (RenderType rt : layers) {
            boolean any = false;
            for (MiniatureMesh m : region.meshes.values()) {
                if (m.buffers.containsKey(rt) || !noPlants && m.plantBuffers.containsKey(rt) || !noSway && m.swayBuffers.containsKey(rt)) {
                    any = true;
                    break;
                }
            }
            if (!any) continue;
            rt.setupRenderState();
            ShaderInstance shader = RenderSystem.getShader();
            if (shader != null) {
                fog.apply(shader, proj);
                shader.apply();
                for (MiniatureMesh m : region.meshes.values()) {
                    VertexBuffer vb = m.buffers.get(rt);
                    VertexBuffer pb = noPlants ? null : m.plantBuffers.get(rt);
                    VertexBuffer sb = noSway ? null : m.swayBuffers.get(rt);
                    if (vb == null && pb == null && sb == null) continue;
                    double x0 = min.getX() + m.minX * fx, y0 = min.getY() + Math.max(-1, m.minY) * fy, z0 = min.getZ() + m.minZ * fz;
                    if (!event.getFrustum().isVisible(new AABB(x0, y0, z0, x0 + m.extent * fx + 0.1, y0 + (m.extent + 1) * fy + 0.1, z0 + m.extent * fz + 0.1))) {
                        continue;
                    }
                    if (vb != null) {
                        vb.bind();
                        vb.draw();
                    }
                    if (pb != null) {
                        pb.bind();
                        pb.draw();
                    }
                    if (sb != null) {
                        sb.bind();
                        sb.draw();
                    }
                }
                shader.clear();
                VertexBuffer.unbind();
            }
            rt.clearRenderState();
        }
    }

    /** M32 part b: a travelling tank's meshes at its recorded pose (no culling per mesh: the pose is not the world's). */
    /**
     * M32 part b: the miniature of a tank on a Create contraption, drawn at once from its block entity renderer (the
     * entity pass): Create (Flywheel) draws the contraption's blocks, the tank water among them, before the miniature
     * passes; drawn there, the water would hide the miniature behind it.
     */
    public static void drawTravellingNow(TankData t, BlockPos origin, Matrix4f pose) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || t.regionId() < 0) return;
        ClientRegion region = ClientInteriorCache.get(t.regionId());
        if (region == null) return;
        schedule(mc.level, region, t, true);
        if (region.meshes.isEmpty()) return;
        drawTravelling(region, new Travelling(t, origin, pose), new Matrix4f(RenderSystem.getModelViewMatrix()), RenderSystem.getProjectionMatrix(),
                LAYERS);
    }

    private static void drawTravelling(ClientRegion region, Travelling tr, Matrix4f modelView, Matrix4f proj, List<RenderType> layers) {
        TankData t = tr.tank();
        BlockPos min = t.min(), o = tr.origin();
        float fx = (t.sizeX() - 2 * MARGIN) / region.sizeX;
        float fy = (t.sizeY() - 2 * MARGIN) / region.sizeY;
        float fz = (t.sizeZ() - 2 * MARGIN) / region.sizeZ;
        FogSafe fog = new FogSafe(modelView, tr.pose(), min.getX() - o.getX() + MARGIN, min.getY() - o.getY() + MARGIN, min.getZ() - o.getZ() + MARGIN, fx, fy, fz);
        for (RenderType rt : layers) {
            boolean any = false;
            for (MiniatureMesh m : region.meshes.values()) {
                if (m.buffers.containsKey(rt) || m.plantBuffers.containsKey(rt) || m.swayBuffers.containsKey(rt)) {
                    any = true;
                    break;
                }
            }
            if (!any) continue;
            rt.setupRenderState();
            ShaderInstance shader = RenderSystem.getShader();
            if (shader != null) {
                fog.apply(shader, proj);
                shader.apply();
                for (MiniatureMesh m : region.meshes.values()) {
                    for (VertexBuffer vb : new VertexBuffer[]{m.buffers.get(rt), m.plantBuffers.get(rt), m.swayBuffers.get(rt)}) {
                        if (vb == null) continue;
                        vb.bind();
                        vb.draw();
                    }
                }
                shader.clear();
                VertexBuffer.unbind();
            }
            rt.clearRenderState();
        }
    }

    /**
     * M67.5: the uniforms of a miniature draw. The meshes are in region units (0..region size) and the vanilla terrain shaders
     * work out the fog from {@code Position + ChunkOffset} before the model-view matrix: with the translation and the scale in
     * that matrix (as before) the fog saw the distance from the region's corner in region blocks, so the far half of a big
     * tank (scale 8: 192 region blocks) lay in the sky fog (the white-blue haze over the miniature on NeoForge) and, under
     * water, in the water fog (the blue inside a big tank). Now the model-view matrix keeps only rotation and scale; the
     * translation, divided by the scale, is the ChunkOffset, so {@code Position + ChunkOffset} is the camera vector in region
     * units, and the fog distances are divided by the scale too. A rotated pose (a travelling tank) keeps its rotation in the
     * matrix (the fog distance does not change under a rotation). With a shader pack (its own programs, which may not use
     * ChunkOffset) and for a shader without ChunkOffset the full matrix is used as before.
     */
    static final class FogSafe {
        private final Matrix4f full, linear;
        private final float ox, oy, oz, scale;

        /** {@code view} the camera rotation, {@code pose} the travelling tank's pose (or null), (tx, ty, tz) the mesh origin in that frame. */
        FogSafe(Matrix4f view, @org.jetbrains.annotations.Nullable Matrix4f pose, float tx, float ty, float tz, float fx, float fy, float fz) {
            Matrix4f base = pose == null ? new Matrix4f(view) : new Matrix4f(view).mul(pose);
            full = new Matrix4f(base).translate(tx, ty, tz).scale(fx, fy, fz);
            // the pose's own translation (camera to the pose origin) moved into the tank frame: pose = T(c) * L, so
            // L * (p + t) + c = L * (p + t + L^-1 c)
            float cx = 0, cy = 0, cz = 0;
            if (pose != null) {
                org.joml.Vector3f c = pose.getTranslation(new org.joml.Vector3f());
                Matrix4f lin = new Matrix4f(pose).setTranslation(0, 0, 0);
                lin.invert().transformDirection(c);
                cx = c.x; cy = c.y; cz = c.z;
                base = new Matrix4f(view).mul(new Matrix4f(pose).setTranslation(0, 0, 0));
            }
            linear = new Matrix4f(base).scale(fx, fy, fz);
            ox = (tx + cx) / fx;
            oy = (ty + cy) / fy;
            oz = (tz + cz) / fz;
            scale = (fx + fy + fz) / 3F;
        }

        void apply(ShaderInstance shader, Matrix4f proj) {
            boolean offset = shader.CHUNK_OFFSET != null && !IrisCompat.shaderPackInUse();
            shader.setDefaultUniforms(VertexFormat.Mode.QUADS, offset ? linear : full, proj, Minecraft.getInstance().getWindow());
            if (shader.CHUNK_OFFSET != null) shader.CHUNK_OFFSET.set(offset ? ox : 0F, offset ? oy : 0F, offset ? oz : 0F);
            if (!offset || scale <= 0) return;
            if (shader.FOG_START != null) shader.FOG_START.set(fogScaled(RenderSystem.getShaderFogStart()));
            if (shader.FOG_END != null) shader.FOG_END.set(fogScaled(RenderSystem.getShaderFogEnd()));
        }

        private float fogScaled(float d) {
            return d >= 1e30F || d <= -1e30F ? d : Math.max(-1e30F, Math.min(1e30F, d / scale));
        }
    }

    /** Releases everything of a region (render thread). */
    static void release(ClientRegion region) {
        region.close();
    }
}
