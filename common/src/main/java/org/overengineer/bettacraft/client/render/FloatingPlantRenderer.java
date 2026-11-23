package org.overengineer.bettacraft.client.render;

import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.tank.TankWaterGeometry;
import org.overengineer.bettacraft.network.TankPlantsPayload;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.plants.FloatingPlant;
import org.overengineer.bettacraft.tank.plants.FloatingPlantSim;
import org.overengineer.bettacraft.tank.plants.TankPlants;

/**
 * Floating plants on the water surface of a tank (M12), drawn by the controller's block entity renderer: small leaf
 * quads (cutout textures: the alpha is in the texture, so shader packs draw them like any leaf) just above the drawn
 * water surface of their column, bobbing a little, plus crossed root strips hanging into the water for frogbit, salvinia
 * and the red root floater. Positions glide between two server updates ({@link TankPlants#SYNC_TICKS}).
 */
public final class FloatingPlantRenderer {
    private static final String DIR = "textures/entity/plants/";
    private static final Identifier DUCKWEED = BettaCraft.id(DIR + "duckweed.png");
    private static final Identifier FROGBIT = BettaCraft.id(DIR + "frogbit_leaf.png");
    private static final Identifier SALVINIA = BettaCraft.id(DIR + "salvinia.png");
    private static final Identifier RED_ROOT = BettaCraft.id(DIR + "red_root_floater_leaf.png");
    private static final Identifier ROOTS = BettaCraft.id(DIR + "roots.png");
    // M28
    private static final Identifier WATER_LETTUCE = BettaCraft.id(DIR + "water_lettuce_leaf.png");
    private static final Identifier HORNWORT = BettaCraft.id(DIR + "hornwort.png");
    private static final Identifier HORNWORT_SHOOTS = BettaCraft.id(DIR + "hornwort_shoots.png");
    private static final Identifier RICCIA = BettaCraft.id(DIR + "riccia.png");
    private static final int LETTUCE_ROOTS = 0xFFD8DCCC, HORNWORT_TINT = 0xFF4E8A34;
    /** The vanilla lily pad texture (a game asset read at runtime, tinted like the lily pad block). */
    private static final Identifier LILY_PAD = Identifier.withDefaultNamespace("textures/block/lily_pad.png");
    private static final int LILY_TINT = 0xFF3A9A3E;
    private static final int FROGBIT_ROOTS = 0xFFE6E2D2, SALVINIA_ROOTS = 0xFF7A6142;
    /** Height of the leaves above the drawn water surface (no z-fighting with the water quad) and the bobbing. */
    private static final float ABOVE = 0.008F, BOB = 0.003F;
    /**
     * M21.1: every plant of a tank floats at a height of its own (steps of {@link #LAYER}, {@link #LAYERS} of them; the
     * leaves of one plant use {@link #LEAF_STEP} steps below that), so two overlapping plants never lie at the same
     * height and flicker (z-fighting). The order comes from the position for the fixed lily pads and from the id for the
     * drifting plants (their order must not change while they drift).
     */
    static final float LAYER = 0.003F, LEAF_STEP = 0.0004F;
    static final int LAYERS = 12;
    /** Off: every plant at the same height again (the smoke test's before/after picture). */
    public static boolean layered = true;

    private FloatingPlantRenderer() {}

    /** Plant update of a tank (network thread hands it over to the client thread). */
    public static void accept(TankPlantsPayload payload) {
        Level level = Minecraft.getInstance().level;
        if (level == null || !level.isLoaded(payload.controller())) return;
        if (!(level.getBlockEntity(payload.controller()) instanceof TankBlockEntity be) || !be.isController() || be.getTank() == null) return;
        TankData t = be.getTank();
        List<FloatingPlant> fresh = FloatingPlantSim.decode(payload.data(), t.min().getX(), t.min().getZ());
        FloatingPlantSim.adopt(t.plants(), fresh, level.getGameTime(), TankPlants.SYNC_TICKS);
        t.plants().clear();
        t.plants().addAll(fresh);
    }

    public static void render(TankBlockEntity be, TankData t, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        Level level = be.getLevel();
        if (level == null || t.plants().isEmpty()) return;
        BlockPos origin = be.getBlockPos();
        double time = level.getGameTime() + partialTick;
        int layer = TankPlants.surfaceLayer(t);
        boolean dry = t.waterMb() <= 0;
        BlockPos.MutableBlockPos col = new BlockPos.MutableBlockPos();
        List<FloatingPlant> order = new java.util.ArrayList<>(t.plants());
        order.sort(java.util.Comparator.comparingLong(FloatingPlantRenderer::orderKey));
        java.util.IdentityHashMap<FloatingPlant, Integer> rank = new java.util.IdentityHashMap<>();
        for (int i = 0; i < order.size(); i++) rank.put(order.get(i), i);
        for (FloatingPlant p : t.plants()) {
            double x = p.renderX(time, TankPlants.SYNC_TICKS), z = p.renderZ(time, TankPlants.SYNC_TICKS);
            col.set(Mth.floor(x), layer, Mth.floor(z));
            double y = surfaceY(level, col, dry, t);
            float bob = p.type.movable() ? (float) Math.sin(time * 0.07 + p.id * 1.7) * BOB : 0;
            int light = LevelRenderer.getLightCoords(level, col);
            pose.pushPose();
            float above = layered ? layerOffset(rank.getOrDefault(p, 0)) : 0;
            pose.translate(x - origin.getX(), y - origin.getY() + ABOVE + BOB + bob + above, z - origin.getZ());
            pose.mulPose(Axis.YP.rotation(p.rot));
            drawPlant(pose, buffers, p, light, dry, (float) time);
            pose.popPose();
        }
    }

    /** Sort key of a plant for its height layer: the position for fixed plants, the id for drifting ones. */
    static long orderKey(FloatingPlant p) {
        if (p.type.movable()) return p.id;
        long gx = Math.round(p.x * 64), gz = Math.round(p.z * 64);
        return Long.MIN_VALUE / 2 + (gx * 0x9E3779B1L + gz * 0x85EBCA77L & 0xFFFFFFFFFFL);
    }

    /** Height of the plant with this rank above the others (a few thousandths of a block). */
    static float layerOffset(int rank) {
        return Math.floorMod(rank, LAYERS) * LAYER;
    }

    /** Height of the water as it is drawn in that column (fill of the block state), the floor when dry. */
    private static double surfaceY(Level level, BlockPos col, boolean dry, TankData t) {
        if (dry) return t.min().getY() + 1.0 / 16;
        BlockState s = level.getBlockState(col);
        if (s.getBlock() instanceof TankBlock) {
            int fill = s.getValue(TankBlock.FILL);
            if (fill > 0) return col.getY() + Math.min(fill / 16.0, 1 - TankWaterGeometry.INSET);
        }
        return t.waterSurfaceY();
    }

    private static void drawPlant(PoseStack pose, MultiBufferSource buffers, FloatingPlant p, int light, boolean dry, float time) {
        float r = (float) p.radius();
        float grow = 0.5F + 0.5F * p.size;
        // M64: the roots sway in the water (render.swayingPlants), every plant in its own rhythm
        Sway sway = org.overengineer.bettacraft.config.ClientConfig.swayingPlants() ? new Sway(time, p.id * 1.37F) : Sway.STILL;
        switch (p.type) {
            case DUCKWEED -> {
                VertexConsumer vc = buffers.getBuffer(RenderTypes.entityCutout(DUCKWEED));
                flat(pose, vc, 0, 0, r, 0, 0xFFFFFFFF, light);
                // a second, smaller patch turned against the first: looks less like a square
                flat(pose, vc, r * 0.35F, -r * 0.2F, r * 0.7F, LEAF_STEP * 3, 0xFFFFFFFF, light, 0.9F);
            }
            case FROGBIT -> {
                VertexConsumer vc = buffers.getBuffer(RenderTypes.entityCutout(FROGBIT));
                rosette(pose, vc, p, r, 3 + Math.floorMod(p.id, 3), 0.55F, 0.5F, 0xFFFFFFFF, light);
                if (!dry) roots(pose, buffers, (float) (p.type.rootLength() * grow), r * 0.55F, 3, FROGBIT_ROOTS, light, sway);
            }
            case SALVINIA -> {
                VertexConsumer vc = buffers.getBuffer(RenderTypes.entityCutout(SALVINIA));
                flat(pose, vc, 0, 0, r, 0, 0xFFFFFFFF, light);
                flat(pose, vc, r * 0.25F, r * 0.3F, r * 0.75F, LEAF_STEP * 3, 0xFFFFFFFF, light, 1.4F);
                if (!dry) roots(pose, buffers, (float) (p.type.rootLength() * grow), r * 0.6F, 2, SALVINIA_ROOTS, light, sway);
            }
            case RED_ROOT_FLOATER -> {
                int tint = FloatingPlantSim.redRootLeafTint(p.red);
                VertexConsumer vc = buffers.getBuffer(RenderTypes.entityCutout(RED_ROOT));
                rosette(pose, vc, p, r, 4 + Math.floorMod(p.id, 3), 0.5F, 0.42F, tint, light);
                if (!dry) roots(pose, buffers, (float) (p.type.rootLength() * grow), r * 0.5F, 3, FloatingPlantSim.redRootRootTint(p.red), light, sway);
            }
            case LILY_PAD -> {
                VertexConsumer vc = buffers.getBuffer(RenderTypes.entityCutout(LILY_PAD));
                flat(pose, vc, 0, 0, r, 0, LILY_TINT, light);
            }
            // M28: water lettuce: a cup of velvety leaves standing up a little, long feathery roots
            case WATER_LETTUCE -> {
                VertexConsumer vc = buffers.getBuffer(RenderTypes.entityCutout(WATER_LETTUCE));
                cup(pose, vc, p, r, 5 + Math.floorMod(p.id, 3), 0.62F, 0.45F, 0xFFFFFFFF, light);
                if (!dry) roots(pose, buffers, (float) (p.type.rootLength() * grow), r * 0.6F, 3, LETTUCE_ROOTS, light, sway);
            }
            // hornwort: a loose tuft at the surface, its shoots hanging deep into the water
            case HORNWORT -> {
                VertexConsumer vc = buffers.getBuffer(RenderTypes.entityCutout(HORNWORT));
                flat(pose, vc, 0, 0, r, 0, HORNWORT_TINT, light);
                flat(pose, vc, -r * 0.3F, r * 0.25F, r * 0.7F, LEAF_STEP * 3, HORNWORT_TINT, light, 1.1F);
                // M64: two loose planes of shoots (three looked like a curtain), see FloatingPlantType#rootLength
                strips(pose, buffers, HORNWORT_SHOOTS, (float) (p.type.rootLength() * (dry ? 0.0 : grow)), r * 0.65F, 2, HORNWORT_TINT, light, sway);
            }
            // floating Riccia: a bright cushion of forked thalli
            case RICCIA -> {
                VertexConsumer vc = buffers.getBuffer(RenderTypes.entityCutout(RICCIA));
                flat(pose, vc, 0, 0, r, 0, 0xFFFFFFFF, light);
                flat(pose, vc, r * 0.3F, -r * 0.25F, r * 0.75F, LEAF_STEP * 3, 0xFFFFFFFF, light, 0.7F);
            }
        }
    }

    /**
     * M28: leaves around the centre that rise outwards like a cup (water lettuce): each a quad from the centre, its outer
     * end {@code rise} times its length above the water.
     */
    private static void cup(PoseStack pose, VertexConsumer vc, FloatingPlant p, float r, int leaves, float leafLen, float rise, int tint, int light) {
        PoseStack.Pose last = pose.last();
        Matrix4f m = last.pose();
        float len = r * leafLen * 1.3F, half = r * leafLen * 0.5F; // inside the radius of the plant (never over the glass)
        for (int i = 0; i < leaves; i++) {
            float a = (float) (i * Math.PI * 2 / leaves + Math.sin(p.id * 2.3 + i) * 0.3);
            float ca = Mth.cos(a), sa = Mth.sin(a);
            float in = r * 0.08F, y0 = i * LEAF_STEP, y1 = y0 + len * rise;
            float nx = 0, ny = 1, nz = 0;
            // inner edge (stem end) at v = 1, outer edge (leaf tip) at v = 0
            float[][] v = {
                    {ca * in - sa * half * 0.4F, y0, sa * in + ca * half * 0.4F, 0, 1},
                    {ca * in + sa * half * 0.4F, y0, sa * in - ca * half * 0.4F, 1, 1},
                    {ca * (in + len) + sa * half, y1, sa * (in + len) - ca * half, 1, 0},
                    {ca * (in + len) - sa * half, y1, sa * (in + len) + ca * half, 0, 0}};
            for (float[] q : v) {
                vc.addVertex(m, q[0], q[1], q[2]).setColor(tint).setUv(q[3], q[4]).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light).setNormal(last, nx, ny, nz);
            }
        }
    }

    /** Leaves around the centre (and one in the middle), each a round leaf quad whose stem points to the centre. */
    private static void rosette(PoseStack pose, VertexConsumer vc, FloatingPlant p, float r, int leaves, float leafSize, float spread, int tint,
                                int light) {
        float lr = r * leafSize;
        for (int i = 0; i < leaves; i++) {
            float a = (float) (i * Math.PI * 2 / leaves + Math.sin(p.id * 3.1 + i) * 0.35);
            float d = r * spread * (0.85F + 0.15F * (float) Math.sin(p.id + i * 2.3));
            float cx = Mth.cos(a) * d, cz = Mth.sin(a) * d;
            // overlapping leaves at slightly different heights (no z-fighting)
            flat(pose, vc, cx, cz, lr, i * LEAF_STEP, tint, light, a + (float) Math.PI / 2);
        }
        flat(pose, vc, 0, 0, lr * 0.8F, leaves * LEAF_STEP, tint, light, p.id * 0.9F);
    }

    private static void flat(PoseStack pose, VertexConsumer vc, float cx, float cz, float h, float y, int argb, int light) {
        flat(pose, vc, cx, cz, h, y, argb, light, 0);
    }

    /** Horizontal textured square of half size {@code h} at (cx, y, cz), turned by {@code angle}, seen from both sides. */
    private static void flat(PoseStack pose, VertexConsumer vc, float cx, float cz, float h, float y, int argb, int light, float angle) {
        PoseStack.Pose last = pose.last();
        Matrix4f m = last.pose();
        float c = Mth.cos(angle) * h, s = Mth.sin(angle) * h;
        float[][] v = {{-c + s, -s - c, 0, 0}, {-c - s, -s + c, 0, 1}, {c - s, s + c, 1, 1}, {c + s, s - c, 1, 0}};
        for (float[] q : v) {
            vc.addVertex(m, cx + q[0], y, cz + q[1]).setColor(argb).setUv(q[2], q[3]).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light).setNormal(last, 0, 1, 0);
        }
    }

    /**
     * M64: how the roots of one plant sway: the bottom end swings by up to a fifth of the root length on a slow ellipse, the
     * top stays at the plant ({@link #STILL}: not at all).
     */
    record Sway(float time, float phase) {
        static final Sway STILL = new Sway(Float.NaN, 0F);

        /** Sideways offset (x, z in the plant's frame) of the point {@code f} (0 = top, 1 = bottom) of roots {@code length} long. */
        float[] at(float f, float length) {
            if (Float.isNaN(time) || f <= 0) return new float[] {0F, 0F};
            float a = 0.2F * length * f * f;
            return new float[] {a * Mth.sin(time * 0.055F + phase + f * 0.9F), a * 0.7F * Mth.cos(time * 0.041F + phase * 1.7F + f * 0.6F)};
        }
    }

    /** Crossed vertical strips of root texture hanging down {@code length} blocks, {@code width} wide. */
    private static void roots(PoseStack pose, MultiBufferSource buffers, float length, float width, int planes, int argb, int light, Sway sway) {
        strips(pose, buffers, ROOTS, length, width, planes, argb, light, sway);
    }

    /**
     * Crossed vertical strips of a texture hanging down {@code length} blocks (M28: also the hornwort shoots). M64: each
     * strip in four pieces that bend with the sway.
     */
    private static void strips(PoseStack pose, MultiBufferSource buffers, Identifier tex, float length, float width, int planes, int argb, int light,
                               Sway sway) {
        if (length <= 0.01F) return;
        VertexConsumer vc = buffers.getBuffer(RenderTypes.entityCutout(tex));
        PoseStack.Pose last = pose.last();
        Matrix4f m = last.pose();
        float v1 = Math.min(1, length / 0.45F);
        final int pieces = 4;
        float top = -0.004F;
        for (int i = 0; i < planes; i++) {
            float a = (float) (i * Math.PI / planes);
            float dx = Mth.cos(a) * width, dz = Mth.sin(a) * width;
            float nx = -Mth.sin(a), nz = Mth.cos(a);
            for (int k = 0; k < pieces; k++) {
                float f0 = k / (float) pieces, f1 = (k + 1) / (float) pieces;
                float[] o0 = sway.at(f0, length), o1 = sway.at(f1, length);
                float y0 = top - length * f0, y1 = top - length * f1;
                vc.addVertex(m, -dx + o0[0], y0, -dz + o0[1]).setColor(argb).setUv(0, v1 * f0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, nx, 0, nz);
                vc.addVertex(m, -dx + o1[0], y1, -dz + o1[1]).setColor(argb).setUv(0, v1 * f1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, nx, 0, nz);
                vc.addVertex(m, dx + o1[0], y1, dz + o1[1]).setColor(argb).setUv(1, v1 * f1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, nx, 0, nz);
                vc.addVertex(m, dx + o0[0], y0, dz + o0[1]).setColor(argb).setUv(1, v1 * f0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, nx, 0, nz);
            }
        }
    }
}
