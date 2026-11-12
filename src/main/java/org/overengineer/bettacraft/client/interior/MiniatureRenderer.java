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
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
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
 * </ul>
 */
public final class MiniatureRenderer {
    static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/miniature");
    private static final List<RenderType> OPAQUE = List.of(RenderType.solid(), RenderType.cutoutMipped(), RenderType.cutout());
    private static final List<RenderType> LAYERS = List.of(RenderType.solid(), RenderType.cutoutMipped(), RenderType.cutout(), RenderType.translucent());
    /** Keeps the miniature 0.5 px inside the glass. */
    private static final float MARGIN = 0.5F / 16F;
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

    private record Result(int regionId, long cell, int generation, Map<RenderType, MeshData> data, List<ByteBufferBuilder> memory,
                          int quads, int blocks, long workerNanos, int minX, int minY, int minZ) {
        void free() {
            for (ByteBufferBuilder m : memory) m.close();
        }
    }

    private MiniatureRenderer() {}

    public static void onRenderStage(RenderLevelStageEvent event) {
        RenderLevelStageEvent.Stage stage = event.getStage();
        boolean opaque = stage == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES;
        if (!opaque && stage != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!opaque && IrisCompat.renderingShadowPass()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long t0 = System.nanoTime();
        Vec3 cam = event.getCamera().getPosition();
        double maxDist = ClientConfig.MINIATURE_RENDER_DISTANCE.get();
        if (opaque) {
            uploadResults();
            boolean pack = IrisCompat.shaderPackInUse();
            for (TankBlockEntity be : TankBlockEntity.CLIENT_CONTROLLERS) {
                TankData t = be.getTank();
                if (t == null || t.regionId() < 0 || be.getLevel() != mc.level) continue;
                ClientRegion region = ClientInteriorCache.get(t.regionId());
                if (region == null) continue;
                BlockPos min = t.min();
                AABB box = new AABB(min.getX(), min.getY(), min.getZ(), min.getX() + t.sizeX(), min.getY() + t.sizeY(), min.getZ() + t.sizeZ());
                if (box.getCenter().distanceToSqr(cam) > maxDist * maxDist) continue;
                int lightKey = lightKey(mc, t);
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
            ClientRegion region = ClientInteriorCache.get(t.regionId());
            if (region == null || region.meshes.isEmpty()) continue;
            BlockPos min = t.min();
            AABB box = new AABB(min.getX(), min.getY(), min.getZ(), min.getX() + t.sizeX(), min.getY() + t.sizeY(), min.getZ() + t.sizeZ());
            if (box.getCenter().distanceToSqr(cam) > maxDist * maxDist || !event.getFrustum().isVisible(box)) continue;
            draw(event, region, t, cam, opaque ? OPAQUE : List.of(RenderType.translucent()));
        }
        long dt = System.nanoTime() - t0;
        lastFrameNanos = opaque ? dt : lastFrameNanos + dt;
    }

    private static int lightKey(Minecraft mc, TankData t) {
        BlockPos c = t.min().offset(t.sizeX() / 2, t.sizeY() / 2, t.sizeZ() / 2);
        return mc.level.getBrightness(LightLayer.BLOCK, c) * 31 + mc.level.getBrightness(LightLayer.SKY, c)
                + mc.level.getBrightness(LightLayer.BLOCK, t.min()) * 961 + mc.level.getBrightness(LightLayer.SKY, t.max()) * 29791;
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
        if (region.dirtyCells.isEmpty() || inFlight >= MAX_IN_FLIGHT) return;
        Long2ObjectOpenHashMap<InteriorSnapshot.Section> map = region.sectionsSnapshot();
        LongOpenHashSet allowed = allowedCells(region, map);
        byte[] light = RegionBlockGetter.captureLight(level, t.min(), t.sizeX(), t.sizeY(), t.sizeZ());
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
                    LOGGER.error("Miniature mesh job failed for region {} cell {}", id, cell, e);
                    DONE.add(new Result(id, cell, gen, Map.of(), List.of(), 0, 0, 0, 0, 0, 0));
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
                    ModelData md = model.getModelData(getter, pos, state, ModelData.EMPTY);
                    rand.setSeed(state.getSeed(pos));
                    for (RenderType rt : model.getRenderTypes(state, rand, md)) {
                        RenderType layer = LAYERS.contains(rt) ? rt : RenderType.translucent();
                        BufferBuilder bb = builders.computeIfAbsent(layer, k -> {
                            ByteBufferBuilder mem = new ByteBufferBuilder(1 << 16);
                            memory.put(k, mem);
                            return new BufferBuilder(mem, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                        });
                        ps.pushPose();
                        if (floor) {
                            // substrate: flattened to 1/16 of an interior block just below the floor
                            ps.translate(lx, -1F / 16F, lz);
                            ps.scale(1F, 1F / 16F, 1F);
                        } else {
                            ps.translate(lx, ly, lz);
                        }
                        brd.renderBatched(state, pos, getter, ps, bb, true, rand, md, rt);
                        ps.popPose();
                    }
                    blocks++;
                } catch (RuntimeException e) {
                    // a broken model of another mod must not kill the whole miniature
                }
            }
        }
        Map<RenderType, MeshData> data = new IdentityHashMap<>();
        int quads = 0;
        for (RenderType layer : LAYERS) {
            BufferBuilder bb = builders.get(layer);
            if (bb == null) continue;
            MeshData d = bb.build();
            if (d == null) continue;
            data.put(layer, d);
            quads += d.drawState().vertexCount() / 4;
        }
        return new Result(regionId, cell, gen, data, new ArrayList<>(memory.values()), quads, blocks, System.nanoTime() - t0,
                cx * 32, cy * 32 - InteriorRegion.BASE_Y, cz * 32);
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
                if (!r.data.isEmpty()) {
                    MiniatureMesh mesh = new MiniatureMesh();
                    mesh.minX = r.minX;
                    mesh.minY = r.minY;
                    mesh.minZ = r.minZ;
                    mesh.extent = 32;
                    for (RenderType layer : LAYERS) {
                        MeshData d = r.data.get(layer);
                        if (d == null) continue;
                        mesh.quads += d.drawState().vertexCount() / 4;
                        mesh.vertexBytes += d.vertexBuffer().remaining();
                        VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
                        vb.bind();
                        vb.upload(d); // closes the mesh data
                        VertexBuffer.unbind();
                        mesh.buffers.put(layer, vb);
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
        if (!ClientConfig.LOG_MINIATURE_STATS.get()) return;
        long quads = 0, bytes = 0;
        for (MiniatureMesh m : region.meshes.values()) {
            quads += m.quads;
            bytes += m.vertexBytes;
        }
        LOGGER.info("Miniature region {} ({}x{}x{}, scale {}) v{}: {} cell(s) rebuilt in {} ms (worker threads {} ms, render thread uploads {} ms); "
                        + "{} cells, {} blocks, {} quads, {} KiB VRAM{}",
                region.id, region.sizeX, region.sizeY, region.sizeZ, region.scale, region.version, region.waveCells,
                fmt((System.nanoTime() - region.waveStart) / 1e6), fmt(region.waveWorkerNanos / 1e6), fmt(region.waveUploadNanos / 1e6),
                region.meshes.size(), region.nonAir, quads, bytes / 1024, region.limitReached ? " (render limit reached)" : "");
        region.waveCells = 0;
    }

    private static String fmt(double d) {
        return String.format(java.util.Locale.ROOT, "%.1f", d);
    }

    // ------------------------------------------------------------------ drawing

    private static void draw(RenderLevelStageEvent event, ClientRegion region, TankData t, Vec3 cam, List<RenderType> layers) {
        BlockPos min = t.min();
        float fx = (t.sizeX() - 2 * MARGIN) / region.sizeX;
        float fy = (t.sizeY() - 2 * MARGIN) / region.sizeY;
        float fz = (t.sizeZ() - 2 * MARGIN) / region.sizeZ;
        Matrix4f mv = new Matrix4f(event.getModelViewMatrix())
                .translate((float) (min.getX() - cam.x) + MARGIN, (float) (min.getY() - cam.y) + MARGIN, (float) (min.getZ() - cam.z) + MARGIN)
                .scale(fx, fy, fz);
        Matrix4f proj = event.getProjectionMatrix();
        for (RenderType rt : layers) {
            boolean any = false;
            for (MiniatureMesh m : region.meshes.values()) {
                if (m.buffers.containsKey(rt)) {
                    any = true;
                    break;
                }
            }
            if (!any) continue;
            rt.setupRenderState();
            ShaderInstance shader = RenderSystem.getShader();
            if (shader != null) {
                shader.setDefaultUniforms(VertexFormat.Mode.QUADS, mv, proj, Minecraft.getInstance().getWindow());
                if (shader.CHUNK_OFFSET != null) shader.CHUNK_OFFSET.set(0F, 0F, 0F);
                shader.apply();
                for (MiniatureMesh m : region.meshes.values()) {
                    VertexBuffer vb = m.buffers.get(rt);
                    if (vb == null) continue;
                    double x0 = min.getX() + m.minX * fx, y0 = min.getY() + Math.max(-1, m.minY) * fy, z0 = min.getZ() + m.minZ * fz;
                    if (!event.getFrustum().isVisible(new AABB(x0, y0, z0, x0 + m.extent * fx + 0.1, y0 + (m.extent + 1) * fy + 0.1, z0 + m.extent * fz + 0.1))) {
                        continue;
                    }
                    vb.bind();
                    vb.draw();
                }
                shader.clear();
                VertexBuffer.unbind();
            }
            rt.clearRenderState();
        }
    }

    /** Releases everything of a region (render thread). */
    static void release(ClientRegion region) {
        region.close();
    }
}
