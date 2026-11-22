package org.overengineer.bettacraft.client.interior;

import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.platform.client.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.plants.AquaticPlantBlock;
import org.overengineer.bettacraft.plants.PlantColors;
import org.overengineer.bettacraft.plants.PlantPart;
import org.overengineer.bettacraft.plants.PlantSpecies;

/**
 * M21 plant prototype: Rotala (stems with round leaves that sway in the current like kelp), Vallisneria (long ribbons
 * that sway and twist) and Anubias (thick rhizome, petioles, broad ovate leaves) drawn with real leaf shapes in the
 * tank interior. The blocks of the plant stay (invisible, {@link PlantMeshModel}); from its root this renderer collects
 * the blocks of the plant, builds a mesh in the plant's own coordinates and draws it every frame with the sway. Roots are
 * noted by the chunk renderer while it asks the model for its data, so plants in existing worlds work without a block
 * entity.
 * <p>
 * M21 follow-up: a mesh is rebuilt only when a block of its plant (or next to it) changes on the client (shears, growth,
 * breaking, placing; {@code ClientLevelMixin} on {@code sendBlockUpdated}) or its chunk is loaded again, in the next
 * frame; light changes (client light engine, light packets, a recompiled section) only refresh its light values
 * ({@link PlantMeshIndex}). No periodic rebuild. A plant whose root block is gone is dropped at once.
 * <p>
 * With an Iris shader pack the meshes are also drawn in the shadow pass (they cast shadows): Iris renders the terrain
 * layers of its shadow pass through {@code renderSectionLayer}, which fires {@code AFTER_CUTOUT_BLOCKS} with the shadow
 * model-view matrix; there the meshes are drawn without rebuilding anything, into their own buffer (Iris picks the
 * shadow program for the entity render type). Without Iris that stage never sees a shadow pass.
 * <p>
 * M21.1: the structure of a Rotala (shoots as chains of stem nodes, every leaf on a stem) and of a Vallisneria (one
 * continuous ribbon per leaf, round the bend along the surface) comes from {@link PlantSkeleton}. The same meshes are
 * built from the client copy of the interior for the miniature outside ({@link MiniaturePlants}, {@link Style#OUTSIDE}).
 */
public final class PlantMeshRenderer {
    /**
     * Set by {@link PlantMeshModel} for the miniature builder: there the plant keeps its block model (in buffers of its
     * own that are left out while the tank shows the meshes, M21.1).
     */
    public static final org.overengineer.bettacraft.platform.model.ModelKey<Boolean> MINIATURE = new org.overengineer.bettacraft.platform.model.ModelKey<>("miniature");
    /** How a quad moves besides the sway (M22.1, corals): see {@link #drawAt}. */
    static final int ANIM_NONE = 0, ANIM_WIGGLE = 1, ANIM_POLYP = 2, ANIM_PULSE = 3, ANIM_RISE = 4, ANIM_TUFT = 5;
    /** Floats per vertex: x, y, z, u, v, nx, ny, nz, height (sway), anchor x, y, z, weight, phase. */
    static final int STRIDE = 14;
    private static final int MAX_BLOCKS = 2000, REACH = 10, BUILDS_PER_FRAME = 6;
    private static final double DRAW_DISTANCE = 80, SHADOW_DISTANCE = 64;

    private static final Set<Long> ROOTS = ConcurrentHashMap.newKeySet();
    /** Sections the chunk compiler rebuilt with plant blocks in them (worker threads): their plants get a relight. */
    private static final Set<Long> COMPILED_SECTIONS = ConcurrentHashMap.newKeySet();
    private static final Long2ObjectOpenHashMap<Mesh> MESHES = new Long2ObjectOpenHashMap<>();
    static final PlantMeshIndex INDEX = new PlantMeshIndex();
    private static ClientLevel meshLevel;
    private static MultiBufferSource.BufferSource shadowBuffers;
    /** Frames drawn (main pass); block changes remember it to measure how soon the mesh follows. */
    private static long frame;
    /** Plants drawn in the last frame / in the last shadow pass (smoke tests). */
    public static int lastDrawn, lastShadowDrawn;
    /** Meshes built in total, and the most frames between a block change of a plant and its new mesh (smoke tests). */
    public static int builds;
    public static long maxRebuildDelay;
    /** Block changes reported without their states (M61: a foreign caller such as Modern Dynamics' pipes; smoke tests). */
    public static int statelessChanges;

    private PlantMeshRenderer() {}

    /**
     * The meshes are drawn: on for the pack ({@code features.visuals.plantMeshes}, common) and for this player
     * ({@code render.plantMeshes}, client, M22.1). Only the client draws them; the blocks and everything on the server
     * stay the same, so every player on a server may choose for themselves.
     */
    public static boolean active() {
        return Features.enabled(Feature.PLANT_MESHES) && org.overengineer.bettacraft.config.ClientConfig.plantMeshes();
    }

    static void noteRoot(BlockPos pos) {
        ROOTS.add(pos.asLong());
    }

    /** The chunk compiler meshed a plant block (any thread): the plants of that section refresh their light. */
    static void noteCompiled(BlockPos pos) {
        COMPILED_SECTIONS.add(net.minecraft.core.SectionPos.asLong(pos));
    }

    // ------------------------------------------------------------------ invalidation (client thread)

    /**
     * Whether {@code s} is a block of a meshed plant. M61: null is none: other mods call {@code sendBlockUpdated} without the
     * states (Modern Dynamics, {@code level.sendBlockUpdated(pos, null, null, 0)} while the client reads a pipe's block
     * entity data); vanilla never reads them there, so neither may we. The guard is here and not in the mixin so that every
     * caller of {@link #onBlockChanged} is covered; the early return below then comes before anything else is touched.
     */
    static boolean meshed(@javax.annotation.Nullable BlockState s) {
        return s != null && s.getBlock() instanceof AquaticPlantBlock pb && pb.species().meshed();
    }

    /**
     * {@code ClientLevel.sendBlockUpdated}: every block change on the client (packets, prediction, shears, growth). Either
     * state may be null (M61, a foreign caller): then that side is no plant.
     */
    public static void onBlockChanged(ClientLevel level, BlockPos pos, BlockState old, BlockState now) {
        if (old == null || now == null) statelessChanges++;
        boolean was = meshed(old), is = meshed(now);
        if (!was && !is) return;
        if (level != meshLevel && meshLevel != null) return;
        long key = pos.asLong();
        boolean rootNow = is && now.getValue(AquaticPlantBlock.PART) == PlantPart.ROOT;
        if (was && old.getValue(AquaticPlantBlock.PART) == PlantPart.ROOT && !rootNow) {
            // the root is gone: the plant goes with it
            ROOTS.remove(key);
            INDEX.remove(key);
            MESHES.remove(key);
        }
        if (rootNow) ROOTS.add(key);
        INDEX.blockChanged(pos.getX(), pos.getY(), pos.getZ(), frame);
    }

    /** Light of a section changed (client light engine or a light packet): only the light values are read again. */
    public static void onSectionLight(int sx, int sy, int sz) {
        INDEX.sectionChanged(sx, sy, sz, false, frame);
    }

    /** A chunk arrived (again): the plants reaching into it are built again (their blocks may differ). */
    public static void onChunkLoad(int cx, int cz) {
        INDEX.chunkChanged(cx, cz, true, frame);
    }

    /** A chunk left the client: its plants are forgotten (the chunk compiler notes them again when it comes back). */
    public static void onChunkUnload(int cx, int cz) {
        for (long k : INDEX.rootsInChunk(cx, cz, BlockPos::getX, BlockPos::getZ)) {
            INDEX.remove(k);
            MESHES.remove(k);
        }
        ROOTS.removeIf(k -> BlockPos.getX(k) >> 4 == cx && BlockPos.getZ(k) >> 4 == cz);
    }

    /** Blocks in the mesh of the plant at {@code root}, or -1 without a mesh (smoke tests). */
    public static int meshBlocks(BlockPos root) {
        Mesh m = MESHES.get(root.asLong());
        return m == null ? -1 : m.blocks;
    }

    /** Plants waiting for a rebuild (smoke tests). */
    public static int pendingRebuilds() {
        return INDEX.dirtyCount();
    }

    /** Roots known right now (smoke tests). */
    public static int knownRoots() {
        return ROOTS.size();
    }

    public static void clear() {
        ROOTS.clear();
        COMPILED_SECTIONS.clear();
        MESHES.clear();
        INDEX.clear();
        meshLevel = null;
    }

    // ------------------------------------------------------------------ mesh

    /**
     * How a mesh is built. Inside: the leaf textures of their own. Outside (M21.1, the miniature at 1/16): the "mini"
     * leaf textures from the block atlas (mipmapped like terrain, see {@link Mini#TYPE}), leaves a little longer and wider,
     * stems thicker and the colours a little stronger, so the tiny leaves do not flicker or vanish.
     */
    record Style(float leafLong, float leafWide, float stem, float ribbon, float saturate, boolean atlas, float detail) {
        static final Style INSIDE = new Style(1F, 1F, 1F, 1F, 1F, false, 1F);
        /** M22.1: polyps, tufts and bubble tips of the corals a little larger outside ({@code detail}). */
        static final Style OUTSIDE = new Style(1.25F, 1.5F, 1.8F, 1.35F, 1.18F, true, 1.3F);
    }

    /**
     * Quads of one texture: 4 vertices of {@link #STRIDE} floats and a colour each, per quad the index of its light and
     * how it moves ({@code ANIM_*}).
     */
    static final class Layer {
        final FloatArrayList v = new FloatArrayList();
        final IntArrayList color = new IntArrayList();
        final IntArrayList light = new IntArrayList();
        final IntArrayList mode = new IntArrayList();
    }

    static final class Mesh {
        final Layer[] layers = new Layer[PlantTex.ALL.length];
        final LongArrayList lightPos = new LongArrayList();
        final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap lightIndex = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
        int[] lights = new int[0];
        AABB box;
        int blocks, quads;
        /** Box of the plant's blocks (inclusive), in the coordinates it was built in. */
        int minX, minY, minZ, maxX, maxY, maxZ;
        double swayPerBlock, swayMax;
        float phase;
        /** M22.1: polyps that close when something swims by (zoanthids, star polyps). */
        boolean reactive;
        /** Disturbances (x, y, z relative to the root corner, time) of the last seconds; at most {@link #MAX_DIST}. */
        final float[] dist = new float[MAX_DIST * 4];
        int distCount;
        long lastCheck = -1000;
        static final int MAX_DIST = 6;
        /** Ticks to close, ticks to open again, reach of a disturbance (interior blocks). */
        static final float CLOSE = 4, OPEN = 70, RANGE = 7;
        BlockPos rootPos = BlockPos.ZERO;

        /**
         * Something swam by at (x, y, z), relative to the root corner: the polyps around close. A disturbance near a
         * recent one renews it (a fish that stays keeps them closed).
         */
        void disturb(float x, float y, float z, float time) {
            float rx = rootPos.getX(), ry = rootPos.getY(), rz = rootPos.getZ();
            if (x < minX - rx - RANGE || x > maxX - rx + 1 + RANGE || z < minZ - rz - RANGE || z > maxZ - rz + 1 + RANGE
                    || y < minY - ry - RANGE || y > maxY - ry + 1 + RANGE) return;
            int oldest = 0;
            for (int i = 0; i < distCount; i++) {
                float dx = dist[i * 4] - x, dy = dist[i * 4 + 1] - y, dz = dist[i * 4 + 2] - z;
                float age = time - dist[i * 4 + 3];
                if (dx * dx + dy * dy + dz * dz < 4 && age < OPEN) {
                    dist[i * 4] = x;
                    dist[i * 4 + 1] = y;
                    dist[i * 4 + 2] = z;
                    // stays closed: renewed as if it had closed just now, without opening in between
                    if (age >= CLOSE) dist[i * 4 + 3] = time - CLOSE;
                    return;
                }
                if (dist[i * 4 + 3] < dist[oldest * 4 + 3]) oldest = i;
            }
            int i = distCount < MAX_DIST ? distCount++ : oldest;
            dist[i * 4] = x;
            dist[i * 4 + 1] = y;
            dist[i * 4 + 2] = z;
            dist[i * 4 + 3] = time;
        }

        /** How far the polyp anchored at (x, y, z) is closed (0 open .. 1 closed). */
        float closed(float x, float y, float z, float time) {
            float c = 0;
            for (int i = 0; i < distCount; i++) {
                float age = time - dist[i * 4 + 3];
                if (age < 0 || age > CLOSE + OPEN) continue;
                float env = age < CLOSE ? age / CLOSE : 1 - (age - CLOSE) / OPEN;
                float dx = dist[i * 4] - x, dy = dist[i * 4 + 1] - y, dz = dist[i * 4 + 2] - z;
                float d = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                c = Math.max(c, env * Math.clamp(1.4F - d / RANGE, 0F, 1F));
            }
            return c;
        }

        /** Polyps of this mesh closed right now (smoke tests). */
        boolean anyClosed(float time) {
            for (int i = 0; i < distCount; i++) if (time - dist[i * 4 + 3] <= CLOSE + OPEN) return true;
            return false;
        }

        Mesh() {
            for (int i = 0; i < layers.length; i++) layers[i] = new Layer();
        }
    }

    /** Plants inside whose polyps are closed right now (smoke tests). */
    public static int closedMeshes(float time) {
        int n = 0;
        for (Mesh m : MESHES.values()) if (m.anyClosed(time)) n++;
        return n;
    }

    /** Blocks the mesh is built from: the client level (inside) or the copy of the interior for the miniature (outside). */
    @FunctionalInterface
    interface BlockSource {
        BlockState get(int x, int y, int z);
    }

    private static Mesh build(ClientLevel level, BlockPos root, PlantSpecies sp) {
        Mesh m = build((x, y, z) -> level.getBlockState(new BlockPos(x, y, z)), root, sp, Style.INSIDE);
        INDEX.put(root.asLong(), m.minX, m.minY, m.minZ, m.maxX, m.maxY, m.maxZ);
        return m;
    }

    /**
     * The mesh of the plant whose root block is at {@code root} in the coordinates of {@code src}: every block of this
     * species connected to the root (26-neighbourhood), other roots stop the search.
     */
    static Mesh build(BlockSource src, BlockPos root, PlantSpecies sp, Style style) {
        Long2ObjectOpenHashMap<BlockState> blocks = new Long2ObjectOpenHashMap<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(root);
        java.util.HashSet<Long> seen = new java.util.HashSet<>();
        seen.add(root.asLong());
        BlockState rs = src.get(root.getX(), root.getY(), root.getZ());
        while (!q.isEmpty() && blocks.size() < MAX_BLOCKS) {
            BlockPos p = q.poll();
            BlockState s = src.get(p.getX(), p.getY(), p.getZ());
            if (s.getBlock() != rs.getBlock()) continue;
            if (!p.equals(root) && s.getValue(AquaticPlantBlock.PART) == PlantPart.ROOT) continue;
            blocks.put(BlockPos.asLong(p.getX() - root.getX(), p.getY() - root.getY(), p.getZ() - root.getZ()), s);
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++)
                    for (int dx = -1; dx <= 1; dx++) {
                        BlockPos n = p.offset(dx, dy, dz);
                        if (Math.abs(n.getX() - root.getX()) > REACH || Math.abs(n.getZ() - root.getZ()) > REACH || n.getY() < root.getY() - 4) continue;
                        if (seen.add(n.asLong())) q.add(n);
                    }
        }
        Long2ObjectOpenHashMap<PlantPart> parts = new Long2ObjectOpenHashMap<>(blocks.size());
        it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap tints = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap(blocks.size());
        for (var e : blocks.long2ObjectEntrySet()) {
            parts.put(e.getLongKey(), e.getValue().getValue(AquaticPlantBlock.PART));
            tints.put(e.getLongKey(), (int) e.getValue().getValue(AquaticPlantBlock.TINT));
        }
        PlantMeshBuilder b = new PlantMeshBuilder(root, sp, parts, tints, style);
        b.build();
        Mesh m = b.m;
        m.rootPos = root.immutable();
        int minX = 0, minY = 0, minZ = 0, maxX = 1, maxY = 1, maxZ = 1;
        for (long k : blocks.keySet()) {
            minX = Math.min(minX, BlockPos.getX(k));
            minY = Math.min(minY, BlockPos.getY(k));
            minZ = Math.min(minZ, BlockPos.getZ(k));
            maxX = Math.max(maxX, BlockPos.getX(k) + 1);
            maxY = Math.max(maxY, BlockPos.getY(k) + 1);
            maxZ = Math.max(maxZ, BlockPos.getZ(k) + 1);
        }
        m.box = new AABB(root.getX() + minX, root.getY() + minY, root.getZ() + minZ, root.getX() + maxX, root.getY() + maxY, root.getZ() + maxZ)
                .inflate(m.swayMax + 1.5);
        m.blocks = blocks.size();
        m.minX = root.getX() + minX;
        m.minY = root.getY() + minY;
        m.minZ = root.getZ() + minZ;
        m.maxX = root.getX() + maxX - 1;
        m.maxY = root.getY() + maxY - 1;
        m.maxZ = root.getZ() + maxZ - 1;
        // the same phase inside and outside: interior x/z origins are chunk aligned
        m.phase = (float) ((Math.floorMod(root.getX(), 16) * 0.37 + Math.floorMod(root.getZ(), 16) * 0.61) % (Math.PI * 2));
        return m;
    }

    static net.minecraft.client.renderer.texture.TextureAtlasSprite[] miniSprites() {
        var atlas = org.overengineer.bettacraft.client.ClientCompat.blockAtlas();
        var out = new net.minecraft.client.renderer.texture.TextureAtlasSprite[PlantTex.ALL.length];
        for (int i = 0; i < out.length; i++) out[i] = atlas.getSprite(PlantTex.ALL[i].sprite);
        return out;
    }

    /**
     * Outside (the miniature): the block atlas with its mipmaps (the "mini" leaf textures are in it, being under
     * textures/block), drawn with the entity cut-out shader like the meshes inside (so shader packs treat both the same).
     */
    // a holder: the render type is made on first use (the class itself loads without the render system, JUnit M61)
    static final class Mini {
        private Mini() {}

        static final RenderType TYPE = RenderType.create("bettacraft_plant_mesh_mini",
                net.minecraft.client.renderer.rendertype.RenderSetup.builder(net.minecraft.client.renderer.RenderPipelines.ENTITY_CUTOUT_NO_CULL)
                        .withTexture("Sampler0", net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS) // the atlas sampler: mipmapped
                        .useLightmap()
                        .useOverlay()
                        .affectsCrumbling()
                        .bufferSize(1 << 18)
                        .setOutline(net.minecraft.client.renderer.rendertype.RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                        .createRenderSetup());
    }

    // ------------------------------------------------------------------ drawing

    public static void onRenderStage(RenderLevelStageEvent event) {
        RenderLevelStageEvent.Stage stage = event.getStage();
        if (stage == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            if (shadows && IrisCompat.renderingShadowPass()) renderShadow(event);
            return;
        }
        if (stage != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || IrisCompat.renderingShadowPass()) return;
        if (level != meshLevel) {
            MESHES.clear();
            INDEX.clear();
            if (meshLevel != null) ROOTS.clear();
            meshLevel = level;
        }
        frame++;
        lastDrawn = 0;
        if (!COMPILED_SECTIONS.isEmpty()) {
            for (Long k : COMPILED_SECTIONS.toArray(new Long[0])) {
                COMPILED_SECTIONS.remove(k);
                INDEX.sectionChanged(net.minecraft.core.SectionPos.x(k), net.minecraft.core.SectionPos.y(k), net.minecraft.core.SectionPos.z(k), false, frame);
            }
        }
        if (!active() || ROOTS.isEmpty()) return;
        float time = level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().position();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        int budget = BUILDS_PER_FRAME;
        for (Long key : ROOTS.toArray(new Long[0])) {
            BlockPos pos = BlockPos.of(key);
            if (pos.distToCenterSqr(cam) > DRAW_DISTANCE * DRAW_DISTANCE) continue;
            BlockState s = level.getBlockState(pos);
            if (!meshed(s) || s.getValue(AquaticPlantBlock.PART) != PlantPart.ROOT) {
                if (level.isLoaded(pos)) {
                    ROOTS.remove(key);
                    INDEX.remove(key);
                    MESHES.remove((long) key);
                }
                continue;
            }
            Mesh m = MESHES.get((long) key);
            long since = m == null ? -1 : INDEX.dirtySince(key);
            if ((m == null || since >= 0) && budget > 0) {
                budget--;
                m = build(level, pos, ((AquaticPlantBlock) s.getBlock()).species());
                MESHES.put((long) key, m);
                builds++;
                if (since >= 0) maxRebuildDelay = Math.max(maxRebuildDelay, frame - since);
            }
            if (m == null) continue;
            if (INDEX.takeRelight(key) || m.lights.length != m.lightPos.size()) {
                int[] l = new int[m.lightPos.size()];
                for (int i = 0; i < l.length; i++) l[i] = LevelRenderer.getLightColor(level, BlockPos.of(m.lightPos.getLong(i)));
                m.lights = l;
            }
            if (!event.getFrustum().isVisible(m.box)) continue;
            if (m.reactive) senseInside(level, m, pos, time);
            draw(m, pos, cam, ps, buffers, time);
            lastDrawn++;
        }
        for (PlantTex t : PlantTex.ALL) buffers.endBatch(RenderTypes.entityCutoutNoCull(t.texture));
    }

    /** M22.1: everything alive near the polyps of a mesh (inside: the player, other players) closes them for a moment. */
    private static void senseInside(ClientLevel level, Mesh m, BlockPos root, float time) {
        long now = (long) time;
        if (now - m.lastCheck < 4) return;
        m.lastCheck = now;
        AABB box = new AABB(m.minX, m.minY, m.minZ, m.maxX + 1, m.maxY + 1, m.maxZ + 1).inflate(Mesh.RANGE);
        for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box)) {
            if (e.isSpectator()) continue;
            Vec3 p = e.position();
            m.disturb((float) (p.x - root.getX()), (float) (p.y + e.getBbHeight() * 0.5 - root.getY()), (float) (p.z - root.getZ()), time);
        }
    }

    /**
     * The Iris shadow pass ({@code AFTER_CUTOUT_BLOCKS} while Iris renders the shadow map): the meshes built for the main
     * pass, transformed by the shadow model-view matrix of the event (the pass's own render-system model-view is the
     * identity, like for the entities Iris draws there). The main camera's frustum does not apply, only the distance.
     */
    private static void renderShadow(RenderLevelStageEvent event) {
        lastShadowDrawn = 0;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !active()) return;
        if (shadowBuffers == null) shadowBuffers = MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.ByteBufferBuilder(1 << 18));
        float time = level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().position();
        PoseStack ps = new PoseStack();
        ps.mulPose(event.getModelViewMatrix());
        for (var e : MESHES.long2ObjectEntrySet()) {
            BlockPos pos = BlockPos.of(e.getLongKey());
            if (pos.distToCenterSqr(cam) > SHADOW_DISTANCE * SHADOW_DISTANCE) continue;
            if (level != meshLevel) break;
            draw(e.getValue(), pos, cam, ps, shadowBuffers, time);
            lastShadowDrawn++;
        }
        // M21.1: the meshes of the miniatures shown right now cast shadows too
        lastShadowDrawn += MiniaturePlants.renderShadow(ps, cam, shadowBuffers, time, SHADOW_DISTANCE);
        // M22.1: and the swaying water plants of the miniatures
        lastShadowDrawn += MiniatureSway.renderShadow(ps, cam, shadowBuffers, time, SHADOW_DISTANCE);
        shadowBuffers.endBatch();
        shadowPasses++;
    }

    /** Shadow passes the meshes were drawn in (smoke tests). */
    public static int shadowPasses;
    /** Off: the meshes cast no shadow, as before (the smoke test's before/after picture). */
    public static boolean shadows = true;

    private static void draw(Mesh m, BlockPos root, Vec3 cam, PoseStack ps, MultiBufferSource buffers, float time) {
        ps.pushPose();
        ps.translate(root.getX() - cam.x, root.getY() - cam.y, root.getZ() - cam.z);
        drawAt(m, ps, buffers, time, null);
        ps.popPose();
    }

    /**
     * Draws a mesh with its sway; {@code ps} is at the corner of the root block. {@code all}: one render type for every
     * layer (the atlas-mapped meshes of the miniature), else each layer with its own texture.
     * <p>
     * M22.1, besides the sway (by the height above the root, like kelp) every quad moves as its {@code ANIM_*} says,
     * around its anchor (the base of its polyp or tentacle) and by its weight (0 at the anchor, 1 at its reach):
     * {@link #ANIM_WIGGLE} and {@link #ANIM_TUFT} wave a little, {@link #ANIM_POLYP} waves and closes towards its base when
     * something swims by ({@link Mesh#closed}), {@link #ANIM_PULSE} swells and shrinks slowly and sways, {@link #ANIM_RISE}
     * lifts and lowers a mushroom disc. Everything at draw time; the mesh is built once.
     */
    static void drawAt(Mesh m, PoseStack ps, MultiBufferSource buffers, float time, @javax.annotation.Nullable RenderType all) {
        PoseStack.Pose pose = ps.last();
        Matrix4f mat = pose.pose();
        double w = 0.045 * Math.PI * 2;
        for (int t = 0; t < PlantTex.ALL.length; t++) {
            Layer l = m.layers[t];
            if (l.light.isEmpty()) continue;
            VertexConsumer vc = buffers.getBuffer(all != null ? all : RenderTypes.entityCutoutNoCull(PlantTex.ALL[t].texture));
            float[] v = l.v.elements();
            int[] col = l.color.elements();
            int quads = l.light.size();
            for (int q = 0; q < quads; q++) {
                int light = m.lights.length > l.light.getInt(q) ? m.lights[l.light.getInt(q)] : net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
                int mode = l.mode.getInt(q);
                for (int k = 0; k < 4; k++) {
                    int o = (q * 4 + k) * STRIDE;
                    float h = v[o + 8];
                    float x = v[o], y = v[o + 1], z = v[o + 2];
                    if (mode != ANIM_NONE) {
                        float ax = v[o + 9], ay = v[o + 10], az = v[o + 11], wt = v[o + 12], ph = v[o + 13];
                        float scale = 1, amp = 0, f = 0.3F;
                        switch (mode) {
                            case ANIM_WIGGLE -> amp = 0.12F;
                            case ANIM_TUFT -> {
                                amp = 0.06F;
                                f = 0.45F;
                            }
                            case ANIM_POLYP -> {
                                amp = 0.08F;
                                scale = 1 - 0.8F * m.closed(ax, ay, az, time);
                            }
                            case ANIM_PULSE -> {
                                amp = 0.22F;
                                f = 0.09F;
                                scale = 1 + 0.12F * wt * (float) Math.sin(time * 0.06F + ph);
                            }
                            case ANIM_RISE -> {
                                y += wt * 0.28F * (float) Math.sin(time * 0.025F + ph);
                                x += wt * 0.05F * (float) Math.sin(time * 0.02F + ph * 2);
                            }
                            default -> { }
                        }
                        if (scale != 1) {
                            x = ax + (x - ax) * scale;
                            y = ay + (y - ay) * scale;
                            z = az + (z - az) * scale;
                        }
                        if (amp > 0) {
                            x += wt * amp * (float) Math.sin(time * f + ph);
                            z += wt * amp * (float) Math.cos(time * f * 0.83F + ph * 1.3F);
                            y += wt * amp * 0.3F * (float) Math.sin(time * f * 1.3F + ph);
                        }
                    }
                    if (m.swayMax > 0) {
                        // like kelp: the higher, the further the plant swings in the current
                        double amp = Math.min(m.swayMax, m.swayPerBlock * Math.pow(h, 1.25));
                        x += (float) (amp * Math.sin(time * w + m.phase + h * 0.12));
                        z += (float) (amp * 0.6 * Math.cos(time * w * 0.8 + m.phase * 1.3 + h * 0.1));
                    }
                    vc.addVertex(mat, x, y, z).setColor(col[q * 4 + k]).setUv(v[o + 3], v[o + 4])
                            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, v[o + 5], v[o + 6], v[o + 7]);
                }
            }
        }
    }
}
