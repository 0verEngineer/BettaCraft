package org.overengineer.bettacraft.client.interior;

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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelProperty;
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
    public static final ModelProperty<Boolean> MINIATURE = new ModelProperty<>();
    private static final ResourceLocation TEX_ROUND = BettaCraft.id("textures/block/plant_mesh/leaf_round.png");
    private static final ResourceLocation TEX_OVATE = BettaCraft.id("textures/block/plant_mesh/leaf_ovate.png");
    private static final ResourceLocation TEX_RIBBON = BettaCraft.id("textures/block/plant_mesh/ribbon.png");
    private static final ResourceLocation TEX_STEM = BettaCraft.id("textures/block/plant_mesh/stem.png");
    private static final ResourceLocation[] TEXTURES = {TEX_ROUND, TEX_OVATE, TEX_RIBBON, TEX_STEM};
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

    private PlantMeshRenderer() {}

    public static boolean active() {
        return CommonConfig.plantMeshes();
    }

    static void noteRoot(BlockPos pos) {
        ROOTS.add(pos.asLong());
    }

    /** The chunk compiler meshed a plant block (any thread): the plants of that section refresh their light. */
    static void noteCompiled(BlockPos pos) {
        COMPILED_SECTIONS.add(net.minecraft.core.SectionPos.asLong(pos));
    }

    // ------------------------------------------------------------------ invalidation (client thread)

    private static boolean prototype(BlockState s) {
        return s.getBlock() instanceof AquaticPlantBlock pb && pb.species().meshPrototype();
    }

    /** {@code ClientLevel.sendBlockUpdated}: every block change on the client (packets, prediction, shears, growth). */
    public static void onBlockChanged(ClientLevel level, BlockPos pos, BlockState old, BlockState now) {
        boolean was = prototype(old), is = prototype(now);
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
     * leaf textures from the block atlas (mipmapped like terrain, see {@link #MINI}), leaves a little longer and wider,
     * stems thicker and the colours a little stronger, so the tiny leaves do not flicker or vanish.
     */
    record Style(float leafLong, float leafWide, float stem, float ribbon, float saturate, boolean atlas) {
        static final Style INSIDE = new Style(1F, 1F, 1F, 1F, 1F, false);
        static final Style OUTSIDE = new Style(1.25F, 1.5F, 1.8F, 1.35F, 1.18F, true);
    }

    /** Quads of one texture: 4 vertices of 9 floats (x, y, z, u, v, nx, ny, nz, height for the sway) per quad. */
    static final class Layer {
        final FloatArrayList v = new FloatArrayList();
        final IntArrayList color = new IntArrayList();
        final IntArrayList light = new IntArrayList();
    }

    static final class Mesh {
        final Layer[] layers = new Layer[TEXTURES.length];
        final LongArrayList lightPos = new LongArrayList();
        final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap lightIndex = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
        int[] lights = new int[0];
        AABB box;
        int blocks, quads;
        /** Box of the plant's blocks (inclusive), in the coordinates it was built in. */
        int minX, minY, minZ, maxX, maxY, maxZ;
        double swayPerBlock, swayMax;
        float phase;

        Mesh() {
            for (int i = 0; i < layers.length; i++) layers[i] = new Layer();
        }
    }

    /** Blocks the mesh is built from: the client level (inside) or the copy of the interior for the miniature (outside). */
    @FunctionalInterface
    interface BlockSource {
        BlockState get(int x, int y, int z);
    }

    /** Builds the quads of a plant; positions relative to the corner of the root block. */
    private static final class Builder {
        final Mesh m = new Mesh();
        final BlockPos root;
        final PlantSpecies sp;
        final Long2ObjectOpenHashMap<BlockState> blocks;
        final Style style;
        final net.minecraft.client.renderer.texture.TextureAtlasSprite[] sprites;
        int curLight;
        int curColor;

        Builder(BlockPos root, PlantSpecies sp, Long2ObjectOpenHashMap<BlockState> blocks, Style style) {
            this.root = root;
            this.sp = sp;
            this.blocks = blocks;
            this.style = style;
            this.sprites = style.atlas() ? miniSprites() : null;
        }

        PlantPart part(int x, int y, int z) {
            BlockState s = blocks.get(BlockPos.asLong(x, y, z));
            return s == null ? null : s.getValue(AquaticPlantBlock.PART);
        }

        boolean has(int x, int y, int z) {
            return blocks.containsKey(BlockPos.asLong(x, y, z));
        }

        void use(int x, int y, int z, PlantPart part) {
            BlockState s = blocks.get(BlockPos.asLong(x, y, z));
            int tint = s == null ? 0 : s.getValue(AquaticPlantBlock.TINT);
            curColor = 0xFF000000 | saturate(PlantColors.color(sp, part, tint), style.saturate());
            long key = root.offset(x, y, z).asLong();
            int i = m.lightIndex.getOrDefault(key, -1);
            if (i < 0) {
                i = m.lightPos.size();
                m.lightPos.add(key);
                m.lightIndex.put(key, i);
            }
            curLight = i;
        }

        void quad(int tex, Vector3f a, Vector3f b, Vector3f c, Vector3f d, float u0, float v0, float u1, float v1, Vector3f n) {
            Layer l = m.layers[tex];
            if (sprites != null) {
                // the atlas does not repeat: tiled stems and ribbons are folded back into one tile per quad
                float shift = (float) Math.floor(Math.min(v0, v1));
                v0 = Math.clamp(v0 - shift, 0F, 1F);
                v1 = Math.clamp(v1 - shift, 0F, 1F);
                var sprite = sprites[tex];
                u0 = sprite.getU(u0);
                u1 = sprite.getU(u1);
                v0 = sprite.getV(v0);
                v1 = sprite.getV(v1);
            }
            vert(l, a, u0, v1, n);
            vert(l, b, u1, v1, n);
            vert(l, c, u1, v0, n);
            vert(l, d, u0, v0, n);
            l.color.add(curColor);
            l.light.add(curLight);
            m.quads++;
        }

        private static void vert(Layer l, Vector3f p, float u, float v, Vector3f n) {
            l.v.add(p.x);
            l.v.add(p.y);
            l.v.add(p.z);
            l.v.add(u);
            l.v.add(v);
            l.v.add(n.x);
            l.v.add(n.y);
            l.v.add(n.z);
            l.v.add(Math.max(0, p.y));
        }

        /** A thin square tube from a to b. */
        void tube(Vector3f a, Vector3f b, float r) {
            Vector3f d = new Vector3f(b).sub(a);
            float len = d.length();
            if (len < 1e-4F) return;
            d.div(len);
            Vector3f s = Math.abs(d.y) > 0.9F ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0).cross(d).normalize();
            Vector3f t = new Vector3f(d).cross(s).normalize();
            Vector3f[] off = {new Vector3f(s).mul(r), new Vector3f(t).mul(r), new Vector3f(s).mul(-r), new Vector3f(t).mul(-r)};
            for (int i = 0; i < 4; i++) {
                Vector3f o0 = off[i], o1 = off[(i + 1) % 4];
                Vector3f n = new Vector3f(o0).add(o1).normalize();
                quad(3, new Vector3f(a).add(o0), new Vector3f(a).add(o1), new Vector3f(b).add(o1), new Vector3f(b).add(o0), i * 0.25F, 0, i * 0.25F + 0.25F, len, n);
            }
        }

        /**
         * A leaf blade from {@code base} along {@code dir} ({@code len} long, {@code width} wide), the blade in the plane of
         * dir and {@code across}, arched a little towards its upper side.
         */
        void leaf(int tex, Vector3f base, Vector3f dir, Vector3f across, float len, float width, float arch) {
            Vector3f d = new Vector3f(dir).normalize();
            Vector3f s = new Vector3f(across).normalize();
            Vector3f n = new Vector3f(s).cross(d).normalize();
            if (n.y < 0) {
                n.negate();
                s.negate();
            }
            Vector3f half = new Vector3f(s).mul(width / 2);
            Vector3f mid = new Vector3f(base).add(new Vector3f(d).mul(len / 2)).add(new Vector3f(n).mul(arch * len));
            Vector3f tip = new Vector3f(base).add(new Vector3f(d).mul(len));
            // base half (v 1 .. 0.5), tip half (v 0.5 .. 0)
            quad(tex, new Vector3f(base).sub(half), new Vector3f(base).add(half), new Vector3f(mid).add(half), new Vector3f(mid).sub(half), 0, 0.5F, 1, 1, n);
            quad(tex, new Vector3f(mid).sub(half), new Vector3f(mid).add(half), new Vector3f(tip).add(half), new Vector3f(tip).sub(half), 0, 0, 1, 0.5F, n);
        }

        static Vector3f c(int x, int y, int z) {
            return new Vector3f(x + 0.5F, y + 0.5F, z + 0.5F);
        }

        // -------------------------------------------------------------- Rotala

        /** The blocks of the plant as parts, keyed for {@link PlantSkeleton}. */
        Long2ObjectOpenHashMap<PlantPart> parts() {
            Long2ObjectOpenHashMap<PlantPart> out = new Long2ObjectOpenHashMap<>();
            for (var e : blocks.long2ObjectEntrySet()) {
                long k = e.getLongKey();
                out.put(PlantSkeleton.key(BlockPos.getX(k), BlockPos.getY(k), BlockPos.getZ(k)), e.getValue().getValue(AquaticPlantBlock.PART));
            }
            return out;
        }

        static Vector3f c(long skeletonKey) {
            return c(PlantSkeleton.x(skeletonKey), PlantSkeleton.y(skeletonKey), PlantSkeleton.z(skeletonKey));
        }

        /**
         * M21.1: the shoots as chains of stem nodes ({@link PlantSkeleton#stems}): a stem from every node to its parent
         * (the floor for the lowest ones), a bud of young leaves on every tip, and every leaf block a pair of round leaves
         * from the node it hangs on, fanned out towards the block.
         */
        void stems() {
            PlantSkeleton.Stems sk = PlantSkeleton.stems(parts());
            it.unimi.dsi.fastutil.longs.LongOpenHashSet tips = sk.tips();
            float r = 0.075F * style.stem();
            for (long k : sk.order) {
                int x = PlantSkeleton.x(k), y = PlantSkeleton.y(k), z = PlantSkeleton.z(k);
                use(x, y, z, PlantPart.STEM);
                long p = sk.parent.get(k);
                Vector3f from = p == PlantSkeleton.FLOOR ? new Vector3f(x + 0.5F, Math.max(0, y), z + 0.5F) : c(p);
                tube(from, c(k), p == PlantSkeleton.FLOOR ? r * 1.07F : r);
                if (tips.contains(k)) {
                    // the growing tip: a little bud of young leaves, on in the direction the shoot grows
                    use(x, y, z, PlantPart.TOP);
                    Vector3f dir = new Vector3f(c(k)).sub(from);
                    if (dir.lengthSquared() < 1e-4F || p == PlantSkeleton.FLOOR) dir.set(0, 1, 0);
                    dir.normalize();
                    Vector3f tip = c(k).add(new Vector3f(dir).mul(0.3F));
                    tube(c(k), tip, r * 0.8F);
                    for (int i = 0; i < 3; i++) {
                        double a = i * Math.PI * 2 / 3 + (x * 7 + z * 13) * 0.3;
                        Vector3f d = new Vector3f((float) Math.cos(a), 0, (float) Math.sin(a)).add(new Vector3f(dir).mul(1.6F));
                        leaf(0, tip, d, new Vector3f((float) -Math.sin(a), 0, (float) Math.cos(a)), 0.5F * style.leafLong(), 0.36F * style.leafWide(), 0.08F);
                    }
                }
            }
            for (var e : sk.leafAt.long2LongEntrySet()) {
                long k = e.getLongKey(), n = e.getLongValue();
                int x = PlantSkeleton.x(k), y = PlantSkeleton.y(k), z = PlantSkeleton.z(k);
                PlantPart p = part(x, y, z);
                if (p == null) continue;
                boolean top = p == PlantPart.TOP;
                use(x, y, z, top ? PlantPart.TOP : PlantPart.LEAF);
                int dx = x - PlantSkeleton.x(n), dy = y - PlantSkeleton.y(n), dz = z - PlantSkeleton.z(n);
                double a = dx == 0 && dz == 0 ? (x * 5 + z * 11 + y * 3) * 0.7 : Math.atan2(dz, dx);
                // a pair of round leaves from the node of the stem, fanned out towards this block
                Vector3f node = c(n).add((float) Math.cos(a) * 0.06F, 0, (float) Math.sin(a) * 0.06F);
                for (int i = -1; i <= 1; i += 2) {
                    double b = a + i * 0.5;
                    float rise = (top ? 0.8F : 0.38F) + dy * 0.7F;
                    Vector3f d = new Vector3f((float) Math.cos(b), rise, (float) Math.sin(b));
                    leaf(0, node, d, new Vector3f((float) -Math.sin(b), 0, (float) Math.cos(b)), (top ? 0.78F : 1.0F) * style.leafLong(),
                            (top ? 0.55F : 0.68F) * style.leafWide(), 0.1F);
                }
            }
            m.swayPerBlock = 0.03;
            m.swayMax = 0.9;
        }

        // -------------------------------------------------------------- Vallisneria

        /**
         * M21.1: every ribbon ({@link PlantSkeleton#ribbons}) as one strip from its base on the floor (or the ribbon it
         * branches off) through its blocks up to the surface, round the bend and along the surface to a rounded tip; the
         * steps of the blocks smoothed into a curve, the strip turning from upright (slightly twisted) to flat on the
         * water without a gap.
         */
        void ribbons() {
            for (PlantSkeleton.Ribbon rb : PlantSkeleton.ribbons(parts())) {
                // a branch of one or two blocks lies against the ribbon it branches off (blocks of crossing ribbons the
                // walk could not tell apart): drawn, it would only be a little flap on that ribbon
                if (rb.from() != PlantSkeleton.FLOOR && rb.cells().size() <= 2) continue;
                List<Vector3f> pts = new ArrayList<>();
                List<int[]> cells = new ArrayList<>();
                long first = rb.cells().getLong(0);
                int fx = PlantSkeleton.x(first), fy = PlantSkeleton.y(first), fz = PlantSkeleton.z(first);
                pts.add(rb.from() == PlantSkeleton.FLOOR ? new Vector3f(fx + 0.5F, fy, fz + 0.5F) : c(rb.from()));
                cells.add(new int[] {fx, fy, fz});
                for (long k : rb.cells()) {
                    pts.add(c(k));
                    cells.add(new int[] {PlantSkeleton.x(k), PlantSkeleton.y(k), PlantSkeleton.z(k)});
                }
                // a rounded tip half a block further
                Vector3f last = pts.getLast(), prev = pts.get(pts.size() - 2);
                Vector3f ext = new Vector3f(last).sub(prev);
                if (ext.lengthSquared() < 1e-6F) ext.set(0, 1, 0);
                pts.add(new Vector3f(last).add(ext.normalize().mul(0.45F)));
                cells.add(cells.getLast());
                // smooth the steps of the blocks into a gentle curve (the ends stay)
                for (int it = 0; it < 3; it++) {
                    List<Vector3f> sm = new ArrayList<>(pts.size());
                    sm.add(pts.getFirst());
                    for (int k = 1; k < pts.size() - 1; k++) {
                        sm.add(new Vector3f(pts.get(k - 1)).add(pts.get(k)).add(pts.get(k)).add(pts.get(k + 1)).mul(0.25F));
                    }
                    sm.add(pts.getLast());
                    pts = sm;
                }
                float yaw = (float) Math.floorMod(fx * 73 + fz * 151, 360) * (float) Math.PI / 180F;
                strip(pts, cells, yaw, 0.34F * style.ribbon());
            }
            m.swayPerBlock = 0.05;
            m.swayMax = 1.6;
        }

        /**
         * A ribbon strip along the points: the half width across is worked out per point (upright parts turn slowly with
         * {@code yaw}, parts along the surface lie flat), so neighbouring pieces share their edges.
         */
        void strip(List<Vector3f> pts, List<int[]> cells, float yaw, float w) {
            int n = pts.size();
            Vector3f[] side = new Vector3f[n];
            for (int i = 0; i < n; i++) {
                Vector3f t = new Vector3f(pts.get(Math.min(n - 1, i + 1))).sub(pts.get(Math.max(0, i - 1)));
                if (t.lengthSquared() < 1e-8F) t.set(0, 1, 0);
                t.normalize();
                float ya = yaw + 0.08F * i;
                Vector3f sv = new Vector3f((float) Math.cos(ya), 0, (float) Math.sin(ya));
                Vector3f sh = new Vector3f(t).cross(0, 1, 0);
                Vector3f s;
                if (sh.lengthSquared() < 1e-4F) {
                    s = new Vector3f(sv);
                } else {
                    sh.normalize();
                    if (sh.dot(sv) < 0) sh.negate();
                    float up = Math.clamp((Math.abs(t.y) - 0.3F) / 0.5F, 0F, 1F);
                    s = new Vector3f(sv).mul(up).add(new Vector3f(sh).mul(1 - up));
                }
                s.sub(new Vector3f(t).mul(s.dot(t)));
                if (s.lengthSquared() < 1e-8F) s = new Vector3f(sh.lengthSquared() > 1e-4F ? sh : sv);
                s.normalize();
                if (i > 0 && s.dot(side[i - 1]) < 0) s.negate();
                side[i] = s;
            }
            float v = 0;
            for (int i = 1; i < n; i++) {
                Vector3f a = pts.get(i - 1), b = pts.get(i);
                Vector3f seg = new Vector3f(b).sub(a);
                float len = seg.length();
                if (len < 1e-4F) continue;
                int[] cell = cells.get(i);
                PlantPart p = part(cell[0], cell[1], cell[2]);
                use(cell[0], cell[1], cell[2], p == PlantPart.TOP ? PlantPart.TOP : PlantPart.RIBBON);
                // narrower towards the rounded tip
                Vector3f s0 = new Vector3f(side[i - 1]).mul(w), s1 = new Vector3f(side[i]).mul(i == n - 1 ? w * 0.55F : w);
                Vector3f nrm = new Vector3f(s0).cross(seg).normalize();
                if (nrm.y < 0) nrm.negate();
                float v1 = v + len * 0.5F;
                quad(2, new Vector3f(a).sub(s0), new Vector3f(a).add(s0), new Vector3f(b).add(s1), new Vector3f(b).sub(s1), 0, v1, 1, v, nrm);
                v = v1;
            }
        }

        // -------------------------------------------------------------- Anubias

        void epiphyte() {
            List<Long> leafBlocks = new ArrayList<>();
            List<Long> stems = new ArrayList<>();
            for (var e : blocks.long2ObjectEntrySet()) {
                long k = e.getLongKey();
                int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
                PlantPart p = e.getValue().getValue(AquaticPlantBlock.PART);
                switch (p) {
                    case ROOT, RHIZOME -> {
                        use(x, y, z, PlantPart.RHIZOME);
                        Vector3f a = new Vector3f(x + 0.5F, y + 0.32F, z + 0.5F);
                        boolean linked = false;
                        for (int[] d : new int[][] {{1, 0}, {0, 1}, {-1, 0}, {0, -1}}) {
                            PlantPart q = part(x + d[0], y, z + d[1]);
                            if (q == PlantPart.RHIZOME || q == PlantPart.ROOT) {
                                linked = true;
                                if (d[0] + d[1] > 0) tube(a, new Vector3f(x + d[0] + 0.5F, y + 0.32F, z + d[1] + 0.5F), 0.26F);
                            }
                        }
                        if (!linked) tube(new Vector3f(a).sub(0.3F, 0, 0), new Vector3f(a).add(0.3F, 0, 0), 0.26F);
                    }
                    case STEM -> stems.add(k);
                    case LEAF, TOP -> leafBlocks.add(k);
                    default -> { }
                }
            }
            // leaf bases: stalk blocks next to a leaf block; every leaf block belongs to the nearest base
            List<Long> leafBases = new ArrayList<>();
            for (long k : stems) {
                int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
                boolean touches = false;
                for (int dy = -1; dy <= 1 && !touches; dy++)
                    for (int dz = -1; dz <= 1 && !touches; dz++)
                        for (int dx = -1; dx <= 1 && !touches; dx++) {
                            PlantPart q = part(x + dx, y + dy, z + dz);
                            touches = q == PlantPart.LEAF || q == PlantPart.TOP;
                        }
                if (touches) leafBases.add(k);
            }
            // bases right next to each other are one leaf
            List<Long> merged = new ArrayList<>();
            for (long k : leafBases) {
                boolean near = false;
                for (long o : merged) near |= Math.abs(BlockPos.getX(o) - BlockPos.getX(k)) <= 1 && Math.abs(BlockPos.getY(o) - BlockPos.getY(k)) <= 1
                        && Math.abs(BlockPos.getZ(o) - BlockPos.getZ(k)) <= 1;
                if (!near) merged.add(k);
            }
            if (merged.isEmpty() && !leafBlocks.isEmpty()) merged.add(BlockPos.asLong(0, 0, 0));
            List<List<Long>> blades = new ArrayList<>();
            for (int n = 0; n < merged.size(); n++) blades.add(new ArrayList<>());
            for (long k : leafBlocks) {
                Vector3f p = c(BlockPos.getX(k), BlockPos.getY(k), BlockPos.getZ(k));
                int best = 0;
                double bd = Double.MAX_VALUE;
                for (int n = 0; n < merged.size(); n++) {
                    long b = merged.get(n);
                    double d = p.distanceSquared(c(BlockPos.getX(b), BlockPos.getY(b), BlockPos.getZ(b)));
                    if (d < bd) {
                        bd = d;
                        best = n;
                    }
                }
                blades.get(best).add(k);
            }
            for (int n = 0; n < merged.size(); n++) {
                if (blades.get(n).isEmpty()) continue;
                long b = merged.get(n);
                int bx = BlockPos.getX(b), by = BlockPos.getY(b), bz = BlockPos.getZ(b);
                Vector3f base = c(bx, by, bz);
                // the petiole: from the nearest piece of rhizome up to the base of the blade, a little arched
                Vector3f from = new Vector3f(0.5F, 0.4F, 0.5F);
                double fd = Double.MAX_VALUE;
                for (var e : blocks.long2ObjectEntrySet()) {
                    PlantPart q = e.getValue().getValue(AquaticPlantBlock.PART);
                    if (q != PlantPart.RHIZOME && q != PlantPart.ROOT) continue;
                    long k = e.getLongKey();
                    Vector3f r = new Vector3f(BlockPos.getX(k) + 0.5F, BlockPos.getY(k) + 0.4F, BlockPos.getZ(k) + 0.5F);
                    if (r.distanceSquared(base) < fd) {
                        fd = r.distanceSquared(base);
                        from = r;
                    }
                }
                use(bx, by, bz, PlantPart.STEM);
                Vector3f mid = new Vector3f(from).add(base).mul(0.5F).add(0, 0.25F, 0);
                tube(from, mid, 0.07F * style.stem());
                tube(mid, base, 0.065F * style.stem());
                blade(blades.get(n), base);
            }
            m.swayPerBlock = 0.006;
            m.swayMax = 0.08;
        }

        private void blade(List<Long> patch, Vector3f b) {
            Vector3f tip = new Vector3f(b);
            long tipKey = patch.getFirst();
            for (long k : patch) {
                Vector3f p = c(BlockPos.getX(k), BlockPos.getY(k), BlockPos.getZ(k));
                if (p.distanceSquared(b) > tip.distanceSquared(b)) {
                    tip = p;
                    tipKey = k;
                }
            }
            Vector3f dir = new Vector3f(tip).sub(b);
            if (dir.lengthSquared() < 0.01F) dir.set(0, 1, 0.3F);
            float len = dir.length() + 1.0F;
            dir.normalize();
            Vector3f across = Math.abs(dir.y) > 0.95F ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0).cross(dir).normalize();
            float spread = 0;
            for (long k : patch) {
                Vector3f p = c(BlockPos.getX(k), BlockPos.getY(k), BlockPos.getZ(k)).sub(b);
                spread = Math.max(spread, Math.abs(p.dot(across)));
            }
            float width = Math.max(1.2F, Math.min(3.4F, spread * 2 + 1.4F));
            PlantPart p = part(BlockPos.getX(tipKey), BlockPos.getY(tipKey), BlockPos.getZ(tipKey));
            use(BlockPos.getX(tipKey), BlockPos.getY(tipKey), BlockPos.getZ(tipKey), p == null ? PlantPart.LEAF : p);
            leaf(1, b, dir, across, len * style.leafLong(), width * Math.min(1.15F, style.leafWide()), 0.1F);
        }
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
        Builder b = new Builder(root, sp, blocks, style);
        switch (sp.form()) {
            case STEM -> b.stems();
            case RIBBON -> b.ribbons();
            default -> b.epiphyte();
        }
        Mesh m = b.m;
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

    /** RGB with the saturation scaled by {@code f} (1 = unchanged). */
    static int saturate(int rgb, float f) {
        if (f == 1F) return rgb;
        float r = (rgb >> 16 & 255), g = (rgb >> 8 & 255), b = (rgb & 255);
        float grey = r * 0.299F + g * 0.587F + b * 0.114F;
        int nr = Math.clamp(Math.round(grey + (r - grey) * f), 0, 255);
        int ng = Math.clamp(Math.round(grey + (g - grey) * f), 0, 255);
        int nb = Math.clamp(Math.round(grey + (b - grey) * f), 0, 255);
        return nr << 16 | ng << 8 | nb;
    }

    private static final ResourceLocation[] MINI_SPRITES = {BettaCraft.id("block/plant_mesh/leaf_round_mini"), BettaCraft.id("block/plant_mesh/leaf_ovate_mini"),
            BettaCraft.id("block/plant_mesh/ribbon_mini"), BettaCraft.id("block/plant_mesh/stem")};

    static net.minecraft.client.renderer.texture.TextureAtlasSprite[] miniSprites() {
        var atlas = Minecraft.getInstance().getModelManager().getAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS);
        var out = new net.minecraft.client.renderer.texture.TextureAtlasSprite[MINI_SPRITES.length];
        for (int i = 0; i < out.length; i++) out[i] = atlas.getSprite(MINI_SPRITES[i]);
        return out;
    }

    /**
     * Outside (the miniature): the block atlas with its mipmaps (the "mini" leaf textures are in it, being under
     * textures/block), drawn with the entity cut-out shader like the meshes inside (so shader packs treat both the same).
     */
    static final RenderType MINI = RenderType.create("bettacraft_plant_mesh_mini", com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY,
            com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, 1 << 18, true, false,
            RenderType.CompositeState.builder()
                    .setShaderState(net.minecraft.client.renderer.RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                    .setTextureState(new net.minecraft.client.renderer.RenderStateShard.TextureStateShard(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS, false, true))
                    .setTransparencyState(net.minecraft.client.renderer.RenderStateShard.NO_TRANSPARENCY)
                    .setCullState(net.minecraft.client.renderer.RenderStateShard.NO_CULL)
                    .setLightmapState(net.minecraft.client.renderer.RenderStateShard.LIGHTMAP)
                    .setOverlayState(net.minecraft.client.renderer.RenderStateShard.OVERLAY)
                    .createCompositeState(true));

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
        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        int budget = BUILDS_PER_FRAME;
        for (Long key : ROOTS.toArray(new Long[0])) {
            BlockPos pos = BlockPos.of(key);
            if (pos.distToCenterSqr(cam) > DRAW_DISTANCE * DRAW_DISTANCE) continue;
            BlockState s = level.getBlockState(pos);
            if (!prototype(s) || s.getValue(AquaticPlantBlock.PART) != PlantPart.ROOT) {
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
            draw(m, pos, cam, ps, buffers, time);
            lastDrawn++;
        }
        for (ResourceLocation t : TEXTURES) buffers.endBatch(RenderType.entityCutoutNoCull(t));
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
        Vec3 cam = event.getCamera().getPosition();
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
     */
    static void drawAt(Mesh m, PoseStack ps, MultiBufferSource buffers, float time, @javax.annotation.Nullable RenderType all) {
        PoseStack.Pose pose = ps.last();
        Matrix4f mat = pose.pose();
        double w = 0.045 * Math.PI * 2;
        for (int t = 0; t < TEXTURES.length; t++) {
            Layer l = m.layers[t];
            if (l.color.isEmpty()) continue;
            VertexConsumer vc = buffers.getBuffer(all != null ? all : RenderType.entityCutoutNoCull(TEXTURES[t]));
            float[] v = l.v.elements();
            int quads = l.color.size();
            for (int q = 0; q < quads; q++) {
                int color = l.color.getInt(q);
                int light = m.lights.length > l.light.getInt(q) ? m.lights[l.light.getInt(q)] : net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
                for (int k = 0; k < 4; k++) {
                    int o = (q * 4 + k) * 9;
                    float h = v[o + 8];
                    // like kelp: the higher, the further the plant swings in the current
                    double amp = Math.min(m.swayMax, m.swayPerBlock * Math.pow(h, 1.25));
                    float sx = (float) (amp * Math.sin(time * w + m.phase + h * 0.12));
                    float sz = (float) (amp * 0.6 * Math.cos(time * w * 0.8 + m.phase * 1.3 + h * 0.1));
                    vc.addVertex(mat, v[o] + sx, v[o + 1], v[o + 2] + sz).setColor(color).setUv(v[o + 3], v[o + 4])
                            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, v[o + 5], v[o + 6], v[o + 7]);
                }
            }
        }
    }
}
