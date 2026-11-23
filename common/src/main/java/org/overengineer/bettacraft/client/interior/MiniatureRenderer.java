package org.overengineer.bettacraft.client.interior;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.vertex.VertexFormat;
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
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
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
 *     block renderer (face culling), like vanilla's section compiler. Only the upload to the GPU happens
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
    private static final List<ChunkSectionLayer> OPAQUE = List.of(ChunkSectionLayer.SOLID, ChunkSectionLayer.CUTOUT);
    private static final List<ChunkSectionLayer> LAYERS = List.of(ChunkSectionLayer.SOLID, ChunkSectionLayer.CUTOUT, ChunkSectionLayer.TRANSLUCENT);
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

    /**
     * M63c: whether no mesh job is waiting or running (a dev scene waits for this before a picture, so a freshly loaded
     * tank is not taken without its miniature). Only the tanks the miniatures are drawn for count (within their distance).
     */
    public static boolean idleForTest() {
        return busyForTest() == null;
    }

    /** M63c: why the miniatures are not idle (for the log of a dev scene), null when they are. */
    public static @org.jetbrains.annotations.Nullable String busyForTest() {
        if (inFlight > 0 || !DONE.isEmpty()) return inFlight + " mesh jobs running, " + DONE.size() + " results waiting";
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        double max = ClientConfig.MINIATURE_RENDER_DISTANCE.get();
        for (TankBlockEntity be : TankBlockEntity.CLIENT_CONTROLLERS) {
            if (be.getLevel() != mc.level) continue;
            TankData t = be.getTank();
            if (t == null || t.regionId() < 0) continue;
            double d = Vec3.atLowerCornerOf(t.min()).add(t.sizeX() / 2.0, t.sizeY() / 2.0, t.sizeZ() / 2.0).distanceToSqr(cam);
            if (d > max * max) continue;
            ClientRegion region = ClientInteriorCache.get(t.regionId());
            // a tank close by not synced yet (its chunks just came back; farther ones may never be synced), or not meshed yet
            if (region == null && d < 16 * 16) return "region " + t.regionId() + " of the tank at " + t.min() + " not synced";
            if (region == null) continue;
            if (region.pendingJobs > 0 || !region.dirtyCells.isEmpty()) return "region " + region.id + " at " + t.min() + ": " + region.pendingJobs
                    + " jobs, " + region.dirtyCells.size() + " dirty cells";
        }
        return null;
    }

    private record Result(int regionId, long cell, int generation, Map<ChunkSectionLayer, MeshData> data, Map<ChunkSectionLayer, MeshData> plantData,
                          Map<ChunkSectionLayer, MeshData> swayData, MiniatureSway.Data sway,
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
        Vec3 cam = event.getCamera().position();
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
            draw(event, region, t, cam, opaque ? OPAQUE : List.of(ChunkSectionLayer.TRANSLUCENT), region.plantMeshesShown, region.swaying);
            float time = mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
            if (opaque && region.plantMeshesShown) MiniaturePlants.draw(event.getPoseStack(), cam, region, t, org.overengineer.bettacraft.client.render.ImmediateBuffers.shared(), time);
            if (opaque && region.swaying) MiniatureSway.draw(event.getPoseStack(), cam, region, t, org.overengineer.bettacraft.client.render.ImmediateBuffers.shared(), time);
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
                drawTravelling(region, tr, new Matrix4f(event.getModelViewMatrix()), opaque ? OPAQUE : List.of(ChunkSectionLayer.TRANSLUCENT));
            }
        }
        if (!opaque) TRAVELLING.clear();
        if (opaque) MiniaturePlants.endFrame(org.overengineer.bettacraft.client.render.ImmediateBuffers.shared());
        if (opaque && MiniatureSway.lastQuads > 0) org.overengineer.bettacraft.client.render.ImmediateBuffers.shared().endBatch(PlantMeshRenderer.Mini.TYPE);
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
            Builders out = new Builders();
            POOL.execute(() -> {
                try {
                    DONE.add(build(id, cell, gen, map, getter, out));
                } catch (Throwable e) {
                    org.overengineer.bettacraft.debug.LogArea.RENDER.errorThrottled("mesh_job", "Miniature mesh job failed for region {} cell {}", id, cell, e); // M57
                    DONE.add(new Result(id, cell, gen, Map.of(), Map.of(), Map.of(), null, out.memory(), 0, 0, 0, 0, 0, 0));
                }
            });
        }
    }

    /**
     * M53b: the buffer builders of one cell job (blocks, mesh plants, swaying plants; one per layer), made on the render thread
     * while the level is drawn. Iris gives a builder of the block format its extended vertex format only when it is made while
     * the level renders with a shader pack ({@code ImmediateState.isRenderingLevel}, a global flag); made on the mesher threads,
     * most cells got the plain format at random and the terrain pipeline (extended format with a pack) read them with the wrong
     * stride: garbage triangles that the pack's deferred passes turned into large black rectangles (NeoForge 26.1.2 / 26.2).
     */
    private static final class Builders {
        final Map<ChunkSectionLayer, ByteBufferBuilder> memory = new java.util.EnumMap<>(ChunkSectionLayer.class);
        final Map<ChunkSectionLayer, BufferBuilder> builders = new java.util.EnumMap<>(ChunkSectionLayer.class);
        final Map<ChunkSectionLayer, ByteBufferBuilder> plantMemory = new java.util.EnumMap<>(ChunkSectionLayer.class);
        final Map<ChunkSectionLayer, BufferBuilder> plantBuilders = new java.util.EnumMap<>(ChunkSectionLayer.class);
        final Map<ChunkSectionLayer, ByteBufferBuilder> swayMemory = new java.util.EnumMap<>(ChunkSectionLayer.class);
        final Map<ChunkSectionLayer, BufferBuilder> swayBuilders = new java.util.EnumMap<>(ChunkSectionLayer.class);

        Builders() {
            for (ChunkSectionLayer layer : LAYERS) {
                add(memory, builders, layer, 1 << 16);
                add(plantMemory, plantBuilders, layer, 1 << 12);
                add(swayMemory, swayBuilders, layer, 1 << 12);
            }
        }

        private static void add(Map<ChunkSectionLayer, ByteBufferBuilder> mems, Map<ChunkSectionLayer, BufferBuilder> bbs, ChunkSectionLayer layer, int size) {
            ByteBufferBuilder mem = new ByteBufferBuilder(size);
            mems.put(layer, mem);
            bbs.put(layer, new BufferBuilder(mem, com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS, DefaultVertexFormat.BLOCK));
        }

        List<ByteBufferBuilder> memory() {
            List<ByteBufferBuilder> all = new ArrayList<>(memory.values());
            all.addAll(plantMemory.values());
            all.addAll(swayMemory.values());
            return all;
        }
    }

    /** Worker thread: meshes all blocks of one cell into the builders made for it on the render thread. */
    private static Result build(int regionId, long cell, int gen, Long2ObjectOpenHashMap<InteriorSnapshot.Section> map, RegionBlockGetter getter,
                                Builders out) {
        long t0 = System.nanoTime();
        // M52c (26.1): the block render dispatcher is gone; the models come from the model manager
        var models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        RandomSource rand = RandomSource.create();
        PoseStack ps = new PoseStack();
        Map<ChunkSectionLayer, BufferBuilder> builders = out.builders;
        // M21.1: the mesh plants into buffers of their own (left out while the tank shows their meshes)
        Map<ChunkSectionLayer, BufferBuilder> plantBuilders = out.plantBuilders;
        // M22.1: the swaying water plants into buffers of their own, and their quads once more for drawing them swaying
        Map<ChunkSectionLayer, BufferBuilder> swayBuilders = out.swayBuilders;
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
                    BlockStateModel model = models.get(state);
                    Object md = org.overengineer.bettacraft.platform.client.ClientPlatform.get().modelData(model, getter, pos, state);
                    boolean plant = !floor && MiniaturePlants.meshBlock(state);
                    boolean sway = !floor && !plant && MiniatureSway.candidate(state);
                    if (sway) {
                        if (capture == null) capture = new MiniatureSway.Capture(swayData = new MiniatureSway.Data());
                        capture.footY = MiniatureSway.footOf(getter, lx, ly, lz);
                        capture.phase = (float) ((Math.floorMod(lx, 64) * 0.37 + Math.floorMod(lz, 64) * 0.61) % (Math.PI * 2));
                    }
                    Map<ChunkSectionLayer, BufferBuilder> bbs = plant ? plantBuilders : sway ? swayBuilders : builders;
                    java.util.function.Function<ChunkSectionLayer, com.mojang.blaze3d.vertex.VertexConsumer> buffers =
                            rt -> bbs.get(LAYERS.contains(rt) ? rt : ChunkSectionLayer.TRANSLUCENT);
                    if (sway) {
                        // the swaying copy (everything but the translucent parts, drawn with their sway)
                        MiniatureSway.Capture cap = capture;
                        ps.pushPose();
                        ps.translate(lx, ly, lz);
                        rand.setSeed(state.getSeed(pos));
                        org.overengineer.bettacraft.platform.client.ClientPlatform.get().tesselate(state, pos, getter, ps,
                                rt -> rt == ChunkSectionLayer.TRANSLUCENT ? org.overengineer.bettacraft.client.render.ImmediateBuffers.NULL : cap, true, rand, md, null);
                        ps.popPose();
                    }
                    rand.setSeed(state.getSeed(pos));
                    ps.pushPose();
                    if (floor) {
                        // substrate: flattened to 1/16 of an interior block just below the floor
                        ps.translate(lx, -1F / 16F, lz);
                        ps.scale(1F, 1F / 16F, 1F);
                    } else {
                        ps.translate(lx, ly, lz);
                    }
                    org.overengineer.bettacraft.platform.client.ClientPlatform.get().tesselate(state, pos, getter, ps, buffers, true, rand, md, null);
                    ps.popPose();
                    blocks++;
                } catch (RuntimeException e) {
                    // a broken model of another mod must not kill the whole miniature
                }
            }
        }
        Map<ChunkSectionLayer, MeshData> data = new java.util.EnumMap<>(ChunkSectionLayer.class);
        Map<ChunkSectionLayer, MeshData> plantData = new java.util.EnumMap<>(ChunkSectionLayer.class);
        Map<ChunkSectionLayer, MeshData> swayStatic = new java.util.EnumMap<>(ChunkSectionLayer.class);
        int quads = 0;
        for (ChunkSectionLayer layer : LAYERS) {
            for (int pass = 0; pass < 3; pass++) {
                BufferBuilder bb = (pass == 0 ? builders : pass == 1 ? plantBuilders : swayBuilders).get(layer);
                if (bb == null) continue;
                MeshData d = bb.build();
                if (d == null) continue;
                (pass == 0 ? data : pass == 1 ? plantData : swayStatic).put(layer, d);
                quads += d.drawState().vertexCount() / 4;
            }
        }
        List<ByteBufferBuilder> mem = out.memory();
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
                        for (ChunkSectionLayer layer : LAYERS) {
                            MeshData d = (pass == 0 ? r.data : pass == 1 ? r.plantData : r.swayData).get(layer);
                            if (d == null) continue;
                            mesh.quads += d.drawState().vertexCount() / 4;
                            mesh.vertexBytes += d.vertexBuffer().remaining();
                            MiniatureMesh.Buffer vb = MiniatureMesh.Buffer.upload(d, "Bettacraft miniature " + r.regionId); // closes the mesh data
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

    private static void draw(RenderLevelStageEvent event, ClientRegion region, TankData t, Vec3 cam, List<ChunkSectionLayer> layers, boolean noPlants,
                             boolean noSway) {
        BlockPos min = t.min();
        float fx = (t.sizeX() - 2 * MARGIN) / region.sizeX;
        float fy = (t.sizeY() - 2 * MARGIN) / region.sizeY;
        float fz = (t.sizeZ() - 2 * MARGIN) / region.sizeZ;
        List<MiniatureMesh.Buffer> todo = new ArrayList<>();
        java.util.EnumMap<ChunkSectionLayer, List<MiniatureMesh.Buffer>> byLayer = new java.util.EnumMap<>(ChunkSectionLayer.class);
        for (ChunkSectionLayer rt : layers) {
            List<MiniatureMesh.Buffer> l = new ArrayList<>();
            com.mojang.renderpearl.api.vertex.VertexFormat format = rt.vertexFormat();
            for (var me : region.meshes.long2ObjectEntrySet()) {
                MiniatureMesh m = me.getValue();
                if (!m.formatMatches(rt, format)) {
                    // M53b: built in another vertex format (a shader pack switched on or off meanwhile): not drawn, rebuilt once
                    if (!m.formatStale) region.dirtyCells.add(me.getLongKey());
                    m.formatStale = true;
                    continue;
                }
                MiniatureMesh.Buffer vb = m.buffers.get(rt);
                MiniatureMesh.Buffer pb = noPlants ? null : m.plantBuffers.get(rt);
                MiniatureMesh.Buffer sb = noSway ? null : m.swayBuffers.get(rt);
                if (vb == null && pb == null && sb == null) continue;
                double x0 = min.getX() + m.minX * fx, y0 = min.getY() + Math.max(-1, m.minY) * fy, z0 = min.getZ() + m.minZ * fz;
                if (!event.getFrustum().isVisible(new AABB(x0, y0, z0, x0 + m.extent * fx + 0.1, y0 + (m.extent + 1) * fy + 0.1, z0 + m.extent * fz + 0.1))) {
                    continue;
                }
                if (vb != null) l.add(vb);
                if (pb != null) l.add(pb);
                if (sb != null) l.add(sb);
            }
            if (!l.isEmpty()) byLayer.put(rt, l);
        }
        if (byLayer.isEmpty()) return;
        drawBuffers(event.getModelViewMatrix(), cam, min.getX(), min.getY(), min.getZ(), fx, fy, fz, byLayer);
    }

    /**
     * M52b: draws miniature buffers with the terrain pipelines of 1.21.11, like {@code ChunkSectionsToRender}: the
     * terrain shader computes {@code Position + (ChunkPosition - CameraBlockPos) + CameraOffset}; with the chunk position at
     * the camera's block that is the interior-local position plus the camera's fraction, which the model-view matrix takes
     * back out before scaling the interior into the tank (the fog then sees interior-local distances, as in 1.21.1).
     */
    static void drawBuffers(org.joml.Matrix4fc view, Vec3 cam, double originX, double originY, double originZ, float fx, float fy, float fz,
                            java.util.Map<ChunkSectionLayer, List<MiniatureMesh.Buffer>> byLayer) {
        int bx = net.minecraft.util.Mth.floor(cam.x), by = net.minecraft.util.Mth.floor(cam.y), bz = net.minecraft.util.Mth.floor(cam.z);
        float ox = (float) (bx - cam.x), oy = (float) (by - cam.y), oz = (float) (bz - cam.z);
        Matrix4f mv = new Matrix4f(view)
                .translate((float) (originX - cam.x) + MARGIN, (float) (originY - cam.y) + MARGIN, (float) (originZ - cam.z) + MARGIN)
                .scale(fx, fy, fz)
                .translate(-ox, -oy, -oz);
        drawWithModelView(mv, bx, by, bz, byLayer);
    }

    /**
     * The draw itself: one render pass on the main target, a terrain pipeline per layer. M52e (26.3): the terrain shader
     * reads the model-view and atlas size from {@code TerrainUniform} and the section position from {@code ChunkSection};
     * textures are uniforms of the pass and pipelines are compiled ones, as in {@code ChunkSectionsToRender$DrawSeparate}.
     */
    private static net.minecraft.client.renderer.DynamicGpuDataStorageMapped<net.minecraft.client.renderer.DynamicGpuData.ChunkSectionInfo> chunkSections;
    private static long chunkSectionsFrame = -1;

    /** M52e: the chunk section uniforms of the miniature draws (a ring buffer of its own, recycled every frame). */
    private static net.minecraft.client.renderer.DynamicGpuDataStorageMapped<net.minecraft.client.renderer.DynamicGpuData.ChunkSectionInfo> chunkSections() {
        if (chunkSections == null) chunkSections = new net.minecraft.client.renderer.DynamicGpuDataStorageMapped<>("Bettacraft miniature chunk sections",
                net.minecraft.client.renderer.DynamicGpuData.CHUNK_SECTION_UBO_SIZE, com.mojang.renderpearl.api.buffers.GpuBuffer.USAGE_UNIFORM, 8);
        long frame = org.overengineer.bettacraft.client.render.LevelDraws.frame();
        if (frame != chunkSectionsFrame) {
            chunkSectionsFrame = frame;
            chunkSections.endFrame();
        }
        return chunkSections;
    }

    static void drawWithModelView(Matrix4f mv, int chunkX, int chunkY, int chunkZ, java.util.Map<ChunkSectionLayer, List<MiniatureMesh.Buffer>> byLayer) {
        Minecraft mc = Minecraft.getInstance();
        var atlas = mc.getTextureManager().getTexture(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).getTextureView();
        var gpuData = RenderSystem.getDynamicUniforms();
        var terrain = gpuData.writeTerrainTransform(mv, atlas.getWidth(0), atlas.getHeight(0));
        // our own chunk section storage: vanilla's switches between a uniform and an instanced vertex storage (multi-draw
        // indirect) and closes the other one, which would take away vanilla's (or ours) in the middle of the frame
        var section = chunkSections().writeData(new net.minecraft.client.renderer.DynamicGpuData.ChunkSectionInfo(chunkX, chunkY, chunkZ, 1.0F));
        // M63b: Iris draws the terrain pipelines with a shader pack's programs, which take the model-view matrix and the chunk
        // offset from DynamicTransforms (ModelViewMat, ModelOffset) and ignore the terrain transform and ChunkSection: with the
        // default transforms the interior was drawn at full size at the camera (a castle in the sky) and nothing in the tank.
        // The same transform for the plain positions: mv with the camera fraction (chunk position - camera) taken back out.
        com.mojang.renderpearl.api.buffers.GpuBufferSlice irisTransforms = null;
        if (IrisCompat.shaderPackInUse()) {
            Vec3 cam = mc.gameRenderer.mainCamera().position();
            Matrix4f full = new Matrix4f(mv).translate((float) (chunkX - cam.x), (float) (chunkY - cam.y), (float) (chunkZ - cam.z));
            irisTransforms = gpuData.writeTransform(full, new org.joml.Vector4f(1F, 1F, 1F, 1F), new org.joml.Vector3f(), new Matrix4f());
        }
        var iris = irisTransforms;
        int maxIndices = 0;
        for (List<MiniatureMesh.Buffer> l : byLayer.values()) for (MiniatureMesh.Buffer b : l) maxIndices = Math.max(maxIndices, b.indexCount());
        RenderSystem.AutoStorageIndexBuffer quads = RenderSystem.getSequentialBuffer(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS);
        quads.getBuffer(maxIndices); // sized now, before the pass (a later tank may grow it again: the op takes the current one)
        var sampler = RenderSystem.getSamplerCache().getSampler(com.mojang.renderpearl.api.textures.AddressMode.CLAMP_TO_EDGE,
                com.mojang.renderpearl.api.textures.AddressMode.CLAMP_TO_EDGE, com.mojang.renderpearl.api.textures.FilterMode.LINEAR,
                com.mojang.renderpearl.api.textures.FilterMode.LINEAR, true);
        // M52e: a draw op (into the main pass of 26.3, see LevelDraws); everything above is uploaded before the pass
        var lightmap = mc.gameRenderer.lightmap();
        var lightSampler = RenderSystem.getSamplerCache().getClampToEdge(com.mojang.renderpearl.api.textures.FilterMode.LINEAR);
        org.overengineer.bettacraft.client.render.LevelDraws.add(pass -> {
            if (iris == null) {
                pass.setUniform("TerrainUniform", terrain);
                pass.setUniform("ChunkSection", section);
            } else {
                pass.setUniform("DynamicTransforms", iris);
            }
            pass.setUniform("Sampler0", atlas, sampler);
            pass.setUniform("Sampler2", lightmap, lightSampler);
            pass.setIndexBuffer(quads.getBuffer(), quads.type());
            for (var e : byLayer.entrySet()) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(iris != null ? blockPipeline(e.getKey()) : e.getKey().pipeline(false)));
                for (MiniatureMesh.Buffer b : e.getValue()) {
                    pass.setVertexBuffer(0, b.vertices().slice());
                    pass.drawIndexed(b.indexCount(), 1, 0, 0, 0);
                }
            }
        });
    }

    /**
     * M63c: the pipeline of a layer while an Iris shader pack is in use: the block pipelines (moving and falling blocks:
     * {@code DynamicTransforms}, no chunk sections), not the terrain ones. Iris 1.11.7 (26.3) draws the terrain pipelines
     * outside its own chunk rendering (Sodium does that) so that only the depth of our draws reached the pack: with BSL the miniature
     * was a black, dark blue or bright green cut-out of its tank (the floor, the plants and every block alike), and a colour
     * modulator changed nothing. The block pipelines take the same vertices and the same transform (the one Iris reads
     * anyway, see the M63b note in drawWithModelView) and come out in colour with every pack.
     */
    private static com.mojang.renderpearl.api.pipeline.RenderPipeline blockPipeline(ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID -> net.minecraft.client.renderer.RenderPipelines.SOLID_BLOCK;
            case CUTOUT -> net.minecraft.client.renderer.RenderPipelines.CUTOUT_BLOCK;
            default -> net.minecraft.client.renderer.RenderPipelines.TRANSLUCENT_BLOCK;
        };
    }

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
        drawTravelling(region, new Travelling(t, origin, pose), new Matrix4f(RenderSystem.getModelViewMatrixCopy()), LAYERS);
    }

    /** M32 part b: a travelling tank's meshes at its recorded pose (no culling per mesh: the pose is not the world's). */
    private static void drawTravelling(ClientRegion region, Travelling tr, Matrix4f modelView, List<ChunkSectionLayer> layers) {
        TankData t = tr.tank();
        BlockPos min = t.min(), o = tr.origin();
        float fx = (t.sizeX() - 2 * MARGIN) / region.sizeX;
        float fy = (t.sizeY() - 2 * MARGIN) / region.sizeY;
        float fz = (t.sizeZ() - 2 * MARGIN) / region.sizeZ;
        java.util.EnumMap<ChunkSectionLayer, List<MiniatureMesh.Buffer>> byLayer = new java.util.EnumMap<>(ChunkSectionLayer.class);
        for (ChunkSectionLayer rt : layers) {
            List<MiniatureMesh.Buffer> l = new ArrayList<>();
            com.mojang.renderpearl.api.vertex.VertexFormat format = rt.vertexFormat();
            for (var me : region.meshes.long2ObjectEntrySet()) {
                MiniatureMesh m = me.getValue();
                if (!m.formatMatches(rt, format)) {
                    if (!m.formatStale) region.dirtyCells.add(me.getLongKey());
                    m.formatStale = true;
                    continue;
                }
                for (MiniatureMesh.Buffer vb : new MiniatureMesh.Buffer[]{m.buffers.get(rt), m.plantBuffers.get(rt), m.swayBuffers.get(rt)}) {
                    if (vb != null) l.add(vb);
                }
            }
            if (!l.isEmpty()) byLayer.put(rt, l);
        }
        if (byLayer.isEmpty()) return;
        // the pose is camera-relative at the controller block: the chunk position is 0, the camera fraction is in the pose
        Matrix4f mv = modelView.mul(tr.pose())
                .translate(min.getX() - o.getX() + MARGIN, min.getY() - o.getY() + MARGIN, min.getZ() - o.getZ() + MARGIN)
                .scale(fx, fy, fz);
        Vec3 cam = Minecraft.getInstance().gameRenderer.mainCamera().position();
        int bx = net.minecraft.util.Mth.floor(cam.x), by = net.minecraft.util.Mth.floor(cam.y), bz = net.minecraft.util.Mth.floor(cam.z);
        mv.translate((float) (cam.x - bx), (float) (cam.y - by), (float) (cam.z - bz));
        drawWithModelView(mv, bx, by, bz, byLayer);
    }

    /** Releases everything of a region (render thread). */
    static void release(ClientRegion region) {
        region.close();
    }
}
