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
 * the blocks of the plant, builds a mesh in the plant's own coordinates (again every few seconds, so growth and the
 * shears show) and draws it every frame with the sway. Roots are noted by the chunk renderer while it asks the model for
 * its data, so plants in existing worlds work without a block entity. Never in the Iris shadow pass.
 */
public final class PlantMeshRenderer {
    /** Set by {@link PlantMeshModel} for the miniature builder: there the plant keeps its block model. */
    public static final ModelProperty<Boolean> MINIATURE = new ModelProperty<>();
    private static final ResourceLocation TEX_ROUND = BettaCraft.id("textures/block/plant_mesh/leaf_round.png");
    private static final ResourceLocation TEX_OVATE = BettaCraft.id("textures/block/plant_mesh/leaf_ovate.png");
    private static final ResourceLocation TEX_RIBBON = BettaCraft.id("textures/block/plant_mesh/ribbon.png");
    private static final ResourceLocation TEX_STEM = BettaCraft.id("textures/block/plant_mesh/stem.png");
    private static final ResourceLocation[] TEXTURES = {TEX_ROUND, TEX_OVATE, TEX_RIBBON, TEX_STEM};
    private static final int REBUILD_TICKS = 100, RELIGHT_TICKS = 20, MAX_BLOCKS = 2000, REACH = 10;
    private static final double DRAW_DISTANCE = 80;

    private static final Set<Long> ROOTS = ConcurrentHashMap.newKeySet();
    private static final Long2ObjectOpenHashMap<Mesh> MESHES = new Long2ObjectOpenHashMap<>();
    private static ClientLevel meshLevel;
    /** Plants drawn in the last frame (smoke tests). */
    public static int lastDrawn;

    private PlantMeshRenderer() {}

    public static boolean active() {
        return CommonConfig.plantMeshes();
    }

    static void noteRoot(BlockPos pos) {
        ROOTS.add(pos.asLong());
    }

    /** Roots known right now (smoke tests). */
    public static int knownRoots() {
        return ROOTS.size();
    }

    public static void clear() {
        ROOTS.clear();
        MESHES.clear();
        meshLevel = null;
    }

    // ------------------------------------------------------------------ mesh

    /** Quads of one texture: 4 vertices of 9 floats (x, y, z, u, v, nx, ny, nz, height for the sway) per quad. */
    private static final class Layer {
        final FloatArrayList v = new FloatArrayList();
        final IntArrayList color = new IntArrayList();
        final IntArrayList light = new IntArrayList();
    }

    private static final class Mesh {
        final Layer[] layers = new Layer[TEXTURES.length];
        final LongArrayList lightPos = new LongArrayList();
        final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap lightIndex = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
        int[] lights = new int[0];
        AABB box;
        long built, lit;
        double swayPerBlock, swayMax;
        float phase;

        Mesh() {
            for (int i = 0; i < layers.length; i++) layers[i] = new Layer();
        }
    }

    /** Builds the quads of a plant; positions relative to the corner of the root block. */
    private static final class Builder {
        final Mesh m = new Mesh();
        final BlockPos root;
        final PlantSpecies sp;
        final Long2ObjectOpenHashMap<BlockState> blocks;
        int curLight;
        int curColor;

        Builder(BlockPos root, PlantSpecies sp, Long2ObjectOpenHashMap<BlockState> blocks) {
            this.root = root;
            this.sp = sp;
            this.blocks = blocks;
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
            curColor = 0xFF000000 | PlantColors.color(sp, part, tint);
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
            vert(l, a, u0, v1, n);
            vert(l, b, u1, v1, n);
            vert(l, c, u1, v0, n);
            vert(l, d, u0, v0, n);
            l.color.add(curColor);
            l.light.add(curLight);
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

        void stems() {
            // stem-like: stems, the root and the tips that continue a stem (bottom up)
            Set<Long> stemLike = new java.util.HashSet<>();
            List<long[]> sorted = new ArrayList<>();
            for (var e : blocks.long2ObjectEntrySet()) sorted.add(new long[] {e.getLongKey(), BlockPos.getY(e.getLongKey())});
            sorted.sort((p, q) -> Long.compare(p[1], q[1]));
            for (long[] k : sorted) {
                int x = BlockPos.getX(k[0]), y = BlockPos.getY(k[0]), z = BlockPos.getZ(k[0]);
                PlantPart p = part(x, y, z);
                if (p == PlantPart.STEM || p == PlantPart.ROOT) stemLike.add(k[0]);
                else if (p == PlantPart.TOP && below(stemLike, x, y, z) != null) stemLike.add(k[0]);
            }
            for (long k : stemLike) {
                int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
                use(x, y, z, PlantPart.STEM);
                int[] up = above(stemLike, x, y, z);
                if (below(stemLike, x, y, z) == null) tube(new Vector3f(x + 0.5F, Math.max(0, y), z + 0.5F), c(x, y, z), 0.08F);
                if (up != null) tube(c(x, y, z), c(up[0], up[1], up[2]), 0.075F);
                else {
                    // the growing tip: a little bud of young leaves
                    use(x, y, z, PlantPart.TOP);
                    Vector3f tip = c(x, y, z).add(0, 0.3F, 0);
                    tube(c(x, y, z), tip, 0.06F);
                    for (int i = 0; i < 3; i++) {
                        double a = i * Math.PI * 2 / 3 + (x * 7 + z * 13) * 0.3;
                        Vector3f d = new Vector3f((float) Math.cos(a), 1.6F, (float) Math.sin(a));
                        leaf(0, tip, d, new Vector3f((float) -Math.sin(a), 0, (float) Math.cos(a)), 0.5F, 0.36F, 0.08F);
                    }
                }
            }
            for (var e : blocks.long2ObjectEntrySet()) {
                long k = e.getLongKey();
                if (stemLike.contains(k)) continue;
                int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
                PlantPart p = e.getValue().getValue(AquaticPlantBlock.PART);
                if (p != PlantPart.LEAF && p != PlantPart.TOP) continue;
                use(x, y, z, p);
                boolean top = p == PlantPart.TOP;
                int[] stem = null;
                for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    if (stemLike.contains(BlockPos.asLong(x - d[0], y, z - d[1]))) {
                        stem = d;
                        break;
                    }
                }
                if (stem != null) {
                    // a pair of round leaves from the node of the stem, fanned out towards this block
                    Vector3f node = c(x - stem[0], y, z - stem[1]).add(stem[0] * 0.06F, 0, stem[1] * 0.06F);
                    for (int i = -1; i <= 1; i += 2) {
                        double a = Math.atan2(stem[1], stem[0]) + i * 0.5;
                        float rise = top ? 0.8F : 0.38F;
                        Vector3f d = new Vector3f((float) Math.cos(a), rise, (float) Math.sin(a));
                        leaf(0, node, d, new Vector3f((float) -Math.sin(a), 0, (float) Math.cos(a)), top ? 0.78F : 1.0F, top ? 0.55F : 0.68F, 0.1F);
                    }
                } else {
                    // a tip running along the surface: a loose rosette
                    for (int i = 0; i < 3; i++) {
                        double a = i * Math.PI * 2 / 3 + (x * 5 + z * 11) * 0.4;
                        leaf(0, c(x, y, z).sub(0, 0.15F, 0), new Vector3f((float) Math.cos(a), 0.3F, (float) Math.sin(a)),
                                new Vector3f((float) -Math.sin(a), 0, (float) Math.cos(a)), 0.7F, 0.5F, 0.08F);
                    }
                }
            }
            m.swayPerBlock = 0.03;
            m.swayMax = 0.9;
        }

        private int[] below(Set<Long> set, int x, int y, int z) {
            if (set.contains(BlockPos.asLong(x, y - 1, z))) return new int[] {x, y - 1, z};
            for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                if (set.contains(BlockPos.asLong(x + d[0], y - 1, z + d[1]))) return new int[] {x + d[0], y - 1, z + d[1]};
            }
            return null;
        }

        private int[] above(Set<Long> set, int x, int y, int z) {
            if (set.contains(BlockPos.asLong(x, y + 1, z))) return new int[] {x, y + 1, z};
            for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                if (set.contains(BlockPos.asLong(x + d[0], y + 1, z + d[1]))) return new int[] {x + d[0], y + 1, z + d[1]};
            }
            return null;
        }

        // -------------------------------------------------------------- Vallisneria

        void ribbons() {
            Set<Long> done = new java.util.HashSet<>();
            List<Long> bases = new ArrayList<>();
            for (var e : blocks.long2ObjectEntrySet()) {
                long k = e.getLongKey();
                int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
                boolean under = has(x, y - 1, z) || has(x + 1, y - 1, z) || has(x - 1, y - 1, z) || has(x, y - 1, z + 1) || has(x, y - 1, z - 1);
                if (!under) bases.add(k);
            }
            bases.sort(Long::compare);
            for (long b : bases) {
                if (!done.add(b)) continue;
                int cx = BlockPos.getX(b), cy = BlockPos.getY(b), cz = BlockPos.getZ(b);
                List<Vector3f> pts = new ArrayList<>();
                List<int[]> cells = new ArrayList<>();
                pts.add(new Vector3f(cx + 0.5F, cy, cz + 0.5F));
                cells.add(new int[] {cx, cy, cz});
                for (int guard = 0; guard < 200; guard++) {
                    pts.add(c(cx, cy, cz));
                    cells.add(new int[] {cx, cy, cz});
                    int[] next = null;
                    for (int[] d : new int[][] {{0, 1, 0}, {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}}) {
                        long k = BlockPos.asLong(cx + d[0], cy + d[1], cz + d[2]);
                        if (blocks.containsKey(k) && !done.contains(k)) {
                            next = new int[] {cx + d[0], cy + d[1], cz + d[2]};
                            break;
                        }
                    }
                    if (next == null) break;
                    done.add(BlockPos.asLong(next[0], next[1], next[2]));
                    cx = next[0];
                    cy = next[1];
                    cz = next[2];
                }
                // a rounded tip half a block further
                Vector3f last = pts.getLast(), prev = pts.get(pts.size() - 2);
                pts.add(new Vector3f(last).add(new Vector3f(last).sub(prev).normalize().mul(0.45F)));
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
                float yaw = (float) Math.floorMod(cells.getFirst()[0] * 73 + cells.getFirst()[2] * 151, 360) * (float) Math.PI / 180F;
                float v = 0;
                for (int k = 1; k < pts.size(); k++) {
                    int[] cell = cells.get(k);
                    PlantPart p = part(cell[0], cell[1], cell[2]);
                    use(cell[0], cell[1], cell[2], p == PlantPart.TOP ? PlantPart.TOP : PlantPart.RIBBON);
                    v = ribbon(pts.get(k - 1), pts.get(k), yaw, yaw + 0.08F, v);
                    yaw += 0.08F;
                }
            }
            m.swayPerBlock = 0.05;
            m.swayMax = 1.6;
        }

        /** One piece of ribbon from a to b; returns the texture v at b. */
        float ribbon(Vector3f a, Vector3f b, float yaw0, float yaw1, float v0) {
            Vector3f seg = new Vector3f(b).sub(a);
            float len = seg.length();
            if (len < 1e-4F) return v0;
            float w = 0.34F;
            Vector3f s0, s1;
            if (Math.abs(seg.y) / len > 0.5F) {
                s0 = new Vector3f((float) Math.cos(yaw0), 0, (float) Math.sin(yaw0)).mul(w);
                s1 = new Vector3f((float) Math.cos(yaw1), 0, (float) Math.sin(yaw1)).mul(w);
            } else {
                s0 = new Vector3f(seg).cross(0, 1, 0).normalize().mul(w);
                s1 = new Vector3f(s0);
            }
            Vector3f n = new Vector3f(s0).cross(seg).normalize();
            if (n.y < 0) n.negate();
            float v1 = v0 + len * 0.5F;
            quad(2, new Vector3f(a).sub(s0), new Vector3f(a).add(s0), new Vector3f(b).add(s1), new Vector3f(b).sub(s1), 0, v1, 1, v0, n);
            return v1;
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
                tube(from, mid, 0.07F);
                tube(mid, base, 0.065F);
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
            leaf(1, b, dir, across, len, width, 0.1F);
        }
    }

    private static Mesh build(ClientLevel level, BlockPos root, PlantSpecies sp, long now) {
        // the plant: every block of this species connected to the root, other roots stop the search
        Long2ObjectOpenHashMap<BlockState> blocks = new Long2ObjectOpenHashMap<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(root);
        java.util.HashSet<Long> seen = new java.util.HashSet<>();
        seen.add(root.asLong());
        BlockState rs = level.getBlockState(root);
        while (!q.isEmpty() && blocks.size() < MAX_BLOCKS) {
            BlockPos p = q.poll();
            BlockState s = level.getBlockState(p);
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
        Builder b = new Builder(root, sp, blocks);
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
        m.built = now;
        m.lit = Long.MIN_VALUE;
        m.phase = (float) ((root.getX() * 0.37 + root.getZ() * 0.61) % (Math.PI * 2));
        return m;
    }

    // ------------------------------------------------------------------ drawing

    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || IrisCompat.renderingShadowPass()) return;
        if (level != meshLevel) {
            MESHES.clear();
            if (meshLevel != null) ROOTS.clear();
            meshLevel = level;
        }
        lastDrawn = 0;
        if (!active() || ROOTS.isEmpty()) return;
        long now = level.getGameTime();
        float time = now + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        int budget = 3;
        for (Long key : ROOTS.toArray(new Long[0])) {
            BlockPos pos = BlockPos.of(key);
            if (pos.distToCenterSqr(cam) > DRAW_DISTANCE * DRAW_DISTANCE) continue;
            BlockState s = level.getBlockState(pos);
            if (!(s.getBlock() instanceof AquaticPlantBlock pb) || !pb.species().meshPrototype() || s.getValue(AquaticPlantBlock.PART) != PlantPart.ROOT) {
                if (level.isLoaded(pos)) {
                    ROOTS.remove(key);
                    MESHES.remove((long) key);
                }
                continue;
            }
            Mesh m = MESHES.get((long) key);
            if (m == null || now - m.built > REBUILD_TICKS && budget > 0) {
                if (budget-- <= 0 && m == null) continue;
                m = build(level, pos, pb.species(), now);
                MESHES.put((long) key, m);
            }
            if (!event.getFrustum().isVisible(m.box)) continue;
            if (now - m.lit > RELIGHT_TICKS || m.lights.length != m.lightPos.size()) {
                int[] l = new int[m.lightPos.size()];
                for (int i = 0; i < l.length; i++) l[i] = LevelRenderer.getLightColor(level, BlockPos.of(m.lightPos.getLong(i)));
                m.lights = l;
                m.lit = now;
            }
            draw(m, pos, cam, ps, buffers, time);
            lastDrawn++;
        }
        for (ResourceLocation t : TEXTURES) buffers.endBatch(RenderType.entityCutoutNoCull(t));
    }

    private static void draw(Mesh m, BlockPos root, Vec3 cam, PoseStack ps, MultiBufferSource buffers, float time) {
        ps.pushPose();
        ps.translate(root.getX() - cam.x, root.getY() - cam.y, root.getZ() - cam.z);
        PoseStack.Pose pose = ps.last();
        Matrix4f mat = pose.pose();
        double w = 0.045 * Math.PI * 2;
        for (int t = 0; t < TEXTURES.length; t++) {
            Layer l = m.layers[t];
            if (l.color.isEmpty()) continue;
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURES[t]));
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
        ps.popPose();
    }
}
