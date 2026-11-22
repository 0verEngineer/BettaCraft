package org.overengineer.bettacraft.client.interior;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.BaseCoralPlantTypeBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.plants.AquaticPlantBlock;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankData;

/**
 * M22.1: Minecraft's and other mods' water plants (kelp, seagrass, corals, coral fans and whatever else
 * {@code #bettacraft:swaying_plants} holds, plus by default every other underwater plant) sway in the miniature outside
 * the tank like inside. At 1/16 their texture animation is invisible and shader packs do not know the miniature's
 * blocks, so they stood still before.
 * <p>
 * The miniature builder (worker thread) keeps the quads of these blocks as they are (their own models and textures,
 * nothing copied) in a buffer of their own ({@link Data}), each vertex with its height above the foot of its plant (the
 * lowest block of the column of swaying blocks it belongs to). Every frame they are drawn with the same sway function
 * as the plant meshes (the higher above the foot, the further), within the same LOD distance; further away, or switched
 * off, the ordinary static buffers of the same blocks are drawn instead. With an Iris shader pack they cast shadows.
 * Switches: {@code features.visuals.swayingForeignPlants} (pack), {@code render.swayingPlants} (player), the automatic detection
 * and an exclusion list ({@code swaying.*}).
 */
public final class MiniatureSway {
    /** Floats per vertex: x, y, z (interior), u, v (atlas), nx, ny, nz, height above the foot, phase of the plant. */
    static final int STRIDE = 10;
    private static final double SWAY_PER_BLOCK = 0.04, SWAY_MAX = 1.4;
    private static final Map<BlockState, Boolean> CACHE = new ConcurrentHashMap<>();
    private static volatile List<String> excludeIds = List.of();
    private static volatile List<TagKey<net.minecraft.world.level.block.Block>> excludeTags = List.of();
    private static volatile List<String> excludeMods = List.of();
    private static volatile boolean auto = true;
    private static final List<Shown> SHOWN = new ArrayList<>();
    /** Swaying quads drawn in the last frame (smoke tests, bench). */
    public static int lastQuads, lastTanks;

    private record Shown(ClientRegion region, double x, double y, double z, float fx, float fy, float fz) {}

    private MiniatureSway() {}

    /** The quads of the swaying blocks of one miniature cell. */
    static final class Data {
        final FloatArrayList v = new FloatArrayList();
        final IntArrayList color = new IntArrayList();
        final IntArrayList light = new IntArrayList();

        int quads() {
            return light.size() / 4;
        }
    }

    // ------------------------------------------------------------------ which blocks sway

    /** Config reloaded or tags changed: decide again (the miniatures are rebuilt by the caller). */
    public static void reset() {
        CACHE.clear();
        auto = CommonConfig.getOr(CommonConfig.SWAYING_AUTO, true);
        List<String> ids = new ArrayList<>(), mods = new ArrayList<>();
        List<TagKey<net.minecraft.world.level.block.Block>> tags = new ArrayList<>();
        for (String s : CommonConfig.getOr(CommonConfig.SWAYING_EXCLUDE, List.<String>of())) {
            String e = s.trim();
            if (e.isEmpty()) continue;
            if (e.startsWith("#")) {
                Identifier rl = Identifier.tryParse(e.substring(1));
                if (rl != null) tags.add(TagKey.create(net.minecraft.core.registries.Registries.BLOCK, rl));
            } else if (e.endsWith(":*")) {
                mods.add(e.substring(0, e.length() - 2));
            } else {
                ids.add(e);
            }
        }
        excludeIds = ids;
        excludeTags = tags;
        excludeMods = mods;
    }

    /** True if this block sways in the miniature (any thread; cached per state). */
    public static boolean candidate(BlockState s) {
        if (s.isAir() || s.getBlock() instanceof AquaticPlantBlock) return false;
        Boolean c = CACHE.get(s);
        if (c == null) {
            c = decide(s);
            CACHE.put(s, c);
        }
        return c;
    }

    private static boolean decide(BlockState s) {
        if (s.getRenderShape() != RenderShape.MODEL) return false;
        Identifier id = BuiltInRegistries.BLOCK.getKey(s.getBlock());
        if (excludeIds.contains(id.toString()) || excludeMods.contains(id.getNamespace())) return false;
        for (TagKey<net.minecraft.world.level.block.Block> t : excludeTags) if (s.is(t)) return false;
        if (s.is(ModTags.SWAYING_PLANTS)) return true;
        return auto && autoDetect(s);
    }

    /**
     * The automatic detection: a plant (a bush, a growing plant like kelp or a coral plant; no full cube) that stands in
     * water (a water plant or waterlogged) and lets light through (drawn as a cross or a plant model).
     */
    static boolean autoDetect(BlockState s) {
        var b = s.getBlock();
        boolean plant = b instanceof VegetationBlock || b instanceof GrowingPlantBlock || b instanceof BaseCoralPlantTypeBlock
                || s.is(BlockTags.CORALS) || s.is(BlockTags.WALL_CORALS) || s.is(BlockTags.REPLACEABLE_BY_TREES) && !s.canOcclude();
        if (!plant || s.canOcclude()) return false;
        // a water plant (its fluid is water), one that can stand in water (waterlogged), or one allowed in the dry interior
        return s.getFluidState().is(FluidTags.WATER) || s.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
                || s.is(ModTags.INTERIOR_WATER_PLANTS);
    }

    // ------------------------------------------------------------------ capturing (worker thread)

    /**
     * A vertex consumer that keeps the quads of one block model, with the height of each vertex above {@code footY}
     * (interior blocks) and the phase of the plant.
     */
    static final class Capture implements VertexConsumer {
        final Data d;
        float footY, phase;
        private int n;

        Capture(Data d) {
            this.d = d;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            d.v.add(x);
            d.v.add(y);
            d.v.add(z);
            d.v.add(0);
            d.v.add(0);
            d.v.add(0);
            d.v.add(1);
            d.v.add(0);
            d.v.add(Math.max(0, y - footY));
            d.v.add(phase);
            d.color.add(-1);
            d.light.add(0);
            n = d.light.size() - 1;
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            d.color.set(n, (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255));
            return this;
        }

        @Override
        public VertexConsumer setColor(int argb) {
            d.color.set(n, argb);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            d.v.set(n * STRIDE + 3, u);
            d.v.set(n * STRIDE + 4, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            d.light.set(n, (v & 0xFFFF) << 16 | (u & 0xFFFF));
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            d.v.set(n * STRIDE + 5, x);
            d.v.set(n * STRIDE + 6, y);
            d.v.set(n * STRIDE + 7, z);
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this; // M52b: quads only
        }
    }

    /** The foot of the plant at (x, y, z): the lowest of the swaying blocks straight below it (with itself). */
    static int footOf(RegionBlockGetter getter, int x, int y, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y, z);
        int foot = y;
        for (int yy = y - 1; yy >= Math.max(0, y - 64); yy--) {
            p.setY(yy);
            if (!candidate(getter.getBlockState(p))) break;
            foot = yy;
        }
        return foot;
    }

    // ------------------------------------------------------------------ drawing (render thread)

    /** Swaying is on and the tank is close enough (the same LOD distance as the plant meshes). */
    static boolean wanted(double distSq) {
        if (!Features.enabled(Feature.SWAYING_PLANTS) || !ClientConfig.swayingPlants()) return false;
        int lod = ClientConfig.MINIATURE_PLANT_MESH_DISTANCE.get();
        return lod > 0 && distSq <= (double) lod * lod;
    }

    static void beginFrame() {
        SHOWN.clear();
        lastQuads = 0;
        lastTanks = 0;
    }

    /** Draws the swaying blocks of a tank (main pass). */
    static void draw(PoseStack ps, Vec3 cam, ClientRegion region, TankData t, MultiBufferSource buffers, float time) {
        boolean any = false;
        for (MiniatureMesh m : region.meshes.values()) any |= m.sway != null;
        if (!any) return;
        BlockPos min = t.min();
        float mg = MiniatureRenderer.MARGIN;
        Shown sh = new Shown(region, min.getX() + mg, min.getY() + mg, min.getZ() + mg, (t.sizeX() - 2 * mg) / region.sizeX,
                (t.sizeY() - 2 * mg) / region.sizeY, (t.sizeZ() - 2 * mg) / region.sizeZ);
        SHOWN.add(sh);
        lastTanks++;
        lastQuads += drawAll(ps, cam, sh, buffers, time);
    }

    private static int drawAll(PoseStack ps, Vec3 cam, Shown sh, MultiBufferSource buffers, float time) {
        ps.pushPose();
        ps.translate(sh.x - cam.x, sh.y - cam.y, sh.z - cam.z);
        ps.scale(sh.fx, sh.fy, sh.fz);
        PoseStack.Pose pose = ps.last();
        Matrix4f mat = pose.pose();
        VertexConsumer vc = buffers.getBuffer(PlantMeshRenderer.Mini.TYPE);
        double w = 0.045 * Math.PI * 2;
        int quads = 0;
        for (MiniatureMesh m : sh.region.meshes.values()) {
            Data d = m.sway;
            if (d == null) continue;
            float[] v = d.v.elements();
            int[] col = d.color.elements(), light = d.light.elements();
            int verts = d.light.size();
            for (int i = 0; i < verts; i++) {
                int o = i * STRIDE;
                float h = v[o + 8], ph = v[o + 9];
                double amp = Math.min(SWAY_MAX, SWAY_PER_BLOCK * Math.pow(h, 1.25));
                float x = v[o] + (float) (amp * Math.sin(time * w + ph + h * 0.12));
                float z = v[o + 2] + (float) (amp * 0.6 * Math.cos(time * w * 0.8 + ph * 1.3 + h * 0.1));
                vc.addVertex(mat, x, v[o + 1], z).setColor(col[i]).setUv(v[o + 3], v[o + 4]).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light[i]).setNormal(pose, v[o + 5], v[o + 6], v[o + 7]);
            }
            quads += d.quads();
        }
        ps.popPose();
        return quads;
    }

    /** The Iris shadow pass: the swaying blocks of the miniatures shown in the last main frame. */
    static int renderShadow(PoseStack ps, Vec3 cam, MultiBufferSource buffers, float time, double maxDist) {
        int n = 0;
        for (Shown sh : SHOWN) {
            double dx = sh.x - cam.x, dy = sh.y - cam.y, dz = sh.z - cam.z;
            if (dx * dx + dy * dy + dz * dz > maxDist * maxDist) continue;
            n += drawAll(ps, cam, sh, buffers, time) > 0 ? 1 : 0;
        }
        return n;
    }

    /** Quads of swaying blocks captured for a region (smoke tests), or -1 without a miniature. */
    public static int quads(int regionId) {
        ClientRegion r = ClientInteriorCache.get(regionId);
        if (r == null) return -1;
        int q = 0;
        for (MiniatureMesh m : r.meshes.values()) if (m.sway != null) q += m.sway.quads();
        return q;
    }

    /** Rebuilds every miniature (the blocks that sway changed: config, tags). */
    public static void rebuildAll() {
        reset();
        Minecraft.getInstance().execute(ClientInteriorCache::markAllDirty);
    }
}
