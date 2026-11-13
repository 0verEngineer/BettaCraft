package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.tank.BubbleNest;
import org.overengineer.bettacraft.tank.TankBlockEntity;

/**
 * Bubble nest at the water surface (PLAN 5.4, M15 domed): a lumpy foam lens of several bubble layers (a main dome and
 * smaller lobes) that rises a little above the surface and hangs further below it, so it is easy to see from the side
 * through the glass. Eggs are small beads in the underside of the foam, larvae hang from it tail down; seen from above
 * and below the old textured egg / larva layer is still there. Grows while the male builds it.
 */
final class NestRenderer {
    private static final ResourceLocation NEST = BettaCraft.id("textures/entity/nest/bubble_nest.png");
    private static final ResourceLocation EGGS = BettaCraft.id("textures/entity/nest/nest_eggs.png");
    private static final ResourceLocation LARVAE = BettaCraft.id("textures/entity/nest/nest_larvae.png");
    private static final ResourceLocation FOAM = BettaCraft.id("textures/entity/nest/nest_foam.png");
    private static final ResourceLocation BEADS = BettaCraft.id("textures/entity/nest/nest_beads.png");
    /** Height of the dome above the surface and depth of the foam below it, relative to the nest size (0.7 when built). */
    static final float UP = 0.085F, DOWN = 0.22F;
    private static final int LON = 20, LAT = 4;
    /** {@code TankBreeding.nestSpot}: the nest position is this far below the surface. */
    private static final float SURFACE_GAP = 0.06F;

    private NestRenderer() {}

    /** Size of the nest (width of the foam texture square): grows from 0.25 to 0.7 while it is built. */
    static float size(BubbleNest nest) {
        return switch (nest.phase()) {
            case BUILDING -> 0.25F + 0.45F * (float) Math.min(1.0, nest.progress()
                    / Math.max(1e-6, ServerConfig.getOr(ServerConfig.BREEDING_NEST_BUILD_DAYS, 0.25)));
            default -> 0.7F;
        };
    }

    static void render(TankBlockEntity be, BubbleNest nest, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        BlockPos origin = be.getBlockPos();
        Vec3 p = nest.pos();
        float size = size(nest);
        int light = be.getLevel() == null ? 0xF000F0 : LevelRenderer.getLightColor(be.getLevel(), BlockPos.containing(p));
        long time = be.getLevel() == null ? 0 : be.getLevel().getGameTime();
        // a stable lumpy shape per nest
        long seed = Mth.getSeed(Mth.floor(p.x * 16), Mth.floor(p.y * 16), Mth.floor(p.z * 16));
        pose.pushPose();
        // the nest position lies SURFACE_GAP under the water surface: the dome is built on the surface itself
        pose.translate(p.x - origin.getX(), p.y + SURFACE_GAP - origin.getY(), p.z - origin.getZ());
        pose.mulPose(Axis.YP.rotationDegrees(((time + partialTick) * 0.15F) % 360F));
        VertexConsumer foam = buffers.getBuffer(RenderType.entityTranslucent(NEST));
        float up = size * UP, down = size * DOWN;
        // main dome, an inner layer, and three smaller lobes around it (layers of bubbles of different heights)
        lens(pose, foam, size, 1.0F, up, down, 0, 0, 0F, light, 0xFFFFFFFF);
        VertexConsumer dense = buffers.getBuffer(RenderType.entityTranslucent(FOAM));
        lens(pose, dense, size, 0.72F, up * 0.8F, down * 1.05F, 0, 0, 1.7F, light, 0xFFFFFFFF);
        for (int i = 0; i < 3; i++) {
            float a = (float) (i * 2.1 + ((seed >> (i * 8)) & 0xFF) / 255.0 * 0.9);
            float d = lobeOffset(size, i);
            float k = lobeScale(i);
            lens(pose, dense, size, k, up * (0.9F - 0.1F * i), down * (0.8F - 0.1F * i), Mth.cos(a) * d, Mth.sin(a) * d, a * 3, light, 0xFFFFFFFF);
        }
        if (nest.hasEggs()) {
            boolean larvae = nest.visibleStage() == LifeStage.LARVA;
            int n = Math.min(40, nest.visibleEggCount());
            // the textured layer just under the deepest part of the foam (seen from below and from above through it)
            float layer = Math.min(size * 0.5F, size * Math.min(1.0F, 0.55F + n / 40F) * 0.62F);
            VertexConsumer ev = buffers.getBuffer(RenderType.entityTranslucent(larvae ? LARVAE : EGGS));
            float ly = -down * 0.93F;
            flat(pose, ev, layer, ly, light, false);
            flat(pose, ev, layer, ly + 0.002F, light, true);
            beads(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(BEADS)), size, down, n, larvae, seed, light);
        }
        pose.popPose();
    }

    static float lobeOffset(float size, int i) {
        return size * (0.14F + 0.04F * i);
    }

    static float lobeScale(int i) {
        return 0.5F - 0.06F * i;
    }

    /** Farthest the foam reaches sideways from the nest position (main lens and lobes, every angle). */
    static float reach(float size) {
        float maxRag = 0;
        for (int a = 0; a < 3600; a++) maxRag = Math.max(maxRag, rag((float) (a * Math.PI / 1800)));
        float r = size * 0.97F * maxRag;
        for (int i = 0; i < 3; i++) r = Math.max(r, lobeOffset(size, i) + size * lobeScale(i) * 0.97F * maxRag);
        return r;
    }

    /** Radius of the ragged foam outline at angle {@code a} (as in the texture, assetgen breeding.bubble_nest). */
    static float rag(float a) {
        return 0.40F + 0.05F * Mth.sin(a * 5 + 1.3F) + 0.03F * Mth.sin(a * 11 + 0.4F);
    }

    /**
     * One foam lens: an upper cap {@code up} high and a lower cap {@code down} deep over the ragged outline of the
     * texture, scaled by {@code k} and moved by (ox, oz); the texture is projected from above (turned by {@code rot}).
     */
    private static void lens(PoseStack pose, VertexConsumer vc, float size, float k, float up, float down, float ox, float oz, float rot,
                             int light, int color) {
        PoseStack.Pose last = pose.last();
        Matrix4f m = last.pose();
        float[][][] pts = new float[2][LAT + 1][];
        for (int cap = 0; cap < 2; cap++) {
            float h = cap == 0 ? up : -down;
            for (int i = 0; i < LON; i++) {
                float a0 = (float) (2 * Math.PI * i / LON), a1 = (float) (2 * Math.PI * (i + 1) / LON);
                for (int j = 0; j < LAT; j++) {
                    float p0 = (float) (Math.PI / 2 * j / LAT), p1 = (float) (Math.PI / 2 * (j + 1) / LAT);
                    float[] v00 = point(size, k, h, a0, p0, ox, oz, rot), v10 = point(size, k, h, a1, p0, ox, oz, rot);
                    float[] v11 = point(size, k, h, a1, p1, ox, oz, rot), v01 = point(size, k, h, a0, p1, ox, oz, rot);
                    float[][] quad = cap == 0 ? new float[][]{v00, v01, v11, v10} : new float[][]{v00, v10, v11, v01};
                    for (float[] v : quad) {
                        vc.addVertex(m, v[0], v[1], v[2]).setColor(color).setUv(v[3], v[4]).setOverlay(OverlayTexture.NO_OVERLAY)
                                .setLight(light).setNormal(last, v[5], v[6], v[7]);
                    }
                }
            }
        }
    }

    /** Point of an ellipsoid cap: angle {@code a} around, latitude {@code p} (0 = rim, pi/2 = top / bottom); x y z u v nx ny nz. */
    private static float[] point(float size, float k, float h, float a, float p, float ox, float oz, float rot) {
        float r = size * k * rag(a + rot) * 0.97F;
        float c = Mth.cos(p), s = Mth.sin(p);
        float x = ox + r * c * Mth.cos(a), z = oz + r * c * Mth.sin(a), y = h * k * s;
        // texture projected from above: centre of the lobe = centre of the texture, the lobe covers the ragged disc
        float tu = (x - ox) / (size * k), tv = (z - oz) / (size * k);
        float cr = Mth.cos(rot), sr = Mth.sin(rot);
        float u = 0.5F + tu * cr - tv * sr, v = 0.5F + tu * sr + tv * cr;
        float nx = c * Mth.cos(a) / Math.max(1e-3F, r), ny = s / Math.max(1e-3F, Math.abs(h * k)) * Math.signum(h), nz = c * Mth.sin(a) / Math.max(1e-3F, r);
        float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        return new float[]{x, y, z, u, v, nx / len, ny / len, nz / len};
    }

    /** Eggs as small beads in the underside of the foam, larvae as dark heads with a pale tail hanging below it. */
    private static void beads(PoseStack pose, VertexConsumer vc, float size, float down, int n, boolean larvae, long seed, int light) {
        PoseStack.Pose last = pose.last();
        float maxR = size * 0.27F;
        for (int i = 0; i < n; i++) {
            long h = seed * 31 + i * 0x9E3779B97F4A7C15L;
            float a = (float) ((h >>> 8 & 0xFFFF) / 65536.0 * Math.PI * 2);
            float r = (float) Math.sqrt((h >>> 24 & 0xFFFF) / 65536.0) * maxR;
            float x = r * Mth.cos(a), z = r * Mth.sin(a);
            // bottom of the main lens at this radius (the lens is deeper than the inner layer's surface there)
            float rimR = size * rag(a) * 0.97F;
            float q = Math.min(1F, r / rimR);
            float bottom = -down * Mth.sqrt(Math.max(0F, 1 - q * q));
            float jitter = (h >>> 40 & 0xFF) / 255F;
            if (larvae) {
                float y = bottom + 0.004F;
                box(last, vc, x, y - 0.012F, z, 0.007F, 0.006F, 0.007F, 2, light); // head at the foam
                box(last, vc, x, y - 0.012F - 0.014F - jitter * 0.006F, z, 0.0035F, 0.011F, 0.0035F, 3, light); // tail down
            } else {
                float e = 0.0075F + jitter * 0.002F;
                box(last, vc, x, bottom + e * 0.6F, z, e, e, e, (i & 3) == 0 ? 1 : 0, light);
            }
        }
    }

    /** Axis-aligned cube (half sizes hx hy hz) in the colour of texel {@code col} of the bead texture. */
    private static void box(PoseStack.Pose last, VertexConsumer vc, float cx, float cy, float cz, float hx, float hy, float hz, int col, int light) {
        Matrix4f m = last.pose();
        float u = (col + 0.5F) / 4F, v = 0.5F / 4F;
        float x0 = cx - hx, x1 = cx + hx, y0 = cy - hy, y1 = cy + hy, z0 = cz - hz, z1 = cz + hz;
        float[][] faces = {
                {x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0},
                {x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0},
                {x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1},
                {x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1},
                {x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0},
                {x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0}};
        for (float[] f : faces) {
            for (int k = 0; k < 4; k++) {
                vc.addVertex(m, f[k * 3], f[k * 3 + 1], f[k * 3 + 2]).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light).setNormal(last, f[12], f[13], f[14]);
            }
        }
    }

    /** Horizontal textured square centred at the origin; up = facing up, else facing down. */
    private static void flat(PoseStack pose, VertexConsumer vc, float size, float y, int light, boolean up) {
        PoseStack.Pose last = pose.last();
        Matrix4f m = last.pose();
        float h = size / 2;
        float[][] c = up ? new float[][]{{-h, -h, 0, 0}, {-h, h, 0, 1}, {h, h, 1, 1}, {h, -h, 1, 0}}
                : new float[][]{{-h, -h, 0, 0}, {h, -h, 1, 0}, {h, h, 1, 1}, {-h, h, 0, 1}};
        for (float[] v : c) {
            vc.addVertex(m, v[0], y, v[1]).setColor(0xFFFFFFFF).setUv(v[2], v[3])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, up ? 1 : -1, 0);
        }
    }
}
