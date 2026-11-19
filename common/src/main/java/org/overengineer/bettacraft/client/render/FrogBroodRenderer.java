package org.overengineer.bettacraft.client.render;

import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.entity.frog.FrogSpawn;
import org.overengineer.bettacraft.entity.frog.Tadpole;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;

/**
 * M47 part c: the frog spawn and the tadpoles of the dwarf clawed frogs, drawn by the tank's block entity renderer. The
 * spawn: dark eggs, each in a clear jelly ball, in a loose raft just under the surface. The tadpoles: a dark, round body
 * and a pale tail that beats; each hovers around its spot in small loops (the server moves the spot now and then); late
 * tadpoles grow hind legs, then front legs. Texture {@code textures/entity/critters/frog_brood.png} ({@code
 * tools/assetgen/critters.py}).
 */
public final class FrogBroodRenderer {
    private static final Identifier TEX = BettaCraft.id("textures/entity/critters/frog_brood.png");
    /** At most this many eggs of a clump are drawn. */
    private static final int MAX_DRAWN = 36;
    /** Cells of the texture (4 x 4 texels each, column in the first row): yolk, jelly, body, tail, legs, eye. */
    private static final int YOLK = 0, JELLY = 1, BODY = 2, TAIL = 3, LEGS = 4, EYE = 5;

    private FrogBroodRenderer() {}

    static void render(TankBlockEntity be, TankData t, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        if (t.frogSpawn().isEmpty() && t.tadpoles().isEmpty()) return;
        BlockPos origin = be.getBlockPos();
        VertexConsumer vc = buffers.getBuffer(RenderTypes.entityTranslucent(TEX));
        float time = be.getLevel() == null ? 0 : be.getLevel().getGameTime() + partialTick;
        for (FrogSpawn s : t.frogSpawn()) {
            int light = light(be, s.x, s.y - 0.05, s.z);
            pose.pushPose();
            pose.translate(s.x - origin.getX(), s.y - origin.getY() - 0.025, s.z - origin.getZ());
            spawn(pose, vc, s, light);
            pose.popPose();
        }
        for (Tadpole k : t.tadpoles()) {
            double a = time * 0.035 + k.id * 1.7, r = 0.06 + (k.id % 5) * 0.012;
            double x = k.x + Math.cos(a) * r, z = k.z + Math.sin(a) * r * 0.8, y = k.y + Math.sin(time * 0.05 + k.id) * 0.015;
            float yaw = (float) Math.toDegrees(Math.atan2(Math.cos(a), -Math.sin(a) * 0.8));
            int light = light(be, x, y, z);
            pose.pushPose();
            pose.translate(x - origin.getX(), y - origin.getY(), z - origin.getZ());
            pose.rotate(Axis.YP.rotationDegrees(yaw));
            tadpole(pose, vc, k, time, light);
            pose.popPose();
        }
    }

    private static int light(TankBlockEntity be, double x, double y, double z) {
        return be.getLevel() == null ? 0xF000F0 : net.minecraft.util.LightCoordsUtil.getLightCoords(be.getLevel(), BlockPos.containing(x, y, z));
    }

    /** A raft of eggs: about 9 cm across (a little bigger than in nature, so the eggs are visible). */
    private static void spawn(PoseStack pose, VertexConsumer vc, FrogSpawn s, int light) {
        PoseStack.Pose last = pose.last();
        int n = Math.min(MAX_DRAWN, s.count);
        float spread = 0.04F + 0.004F * n;
        long seed = s.id * 0x9E3779B97F4A7C15L;
        for (int i = 0; i < n; i++) {
            long h = seed + i * 0xBF58476D1CE4E5B9L;
            h ^= h >>> 31;
            float a = (float) ((h >>> 8 & 0xFFFF) / 65536.0 * Math.PI * 2);
            float r = (float) Math.sqrt((h >>> 24 & 0xFFFF) / 65536.0) * spread;
            float dx = Mth.cos(a) * r, dz = Mth.sin(a) * r, dy = ((h >>> 44) & 0xFF) / 255F * 0.012F;
            cube(last, vc, dx, dy, dz, 0.006F, 0.006F, 0.006F, YOLK, 255, light);
            cube(last, vc, dx, dy, dz, 0.012F, 0.012F, 0.012F, JELLY, 110, light);
        }
    }

    /** A tadpole about 1.2 cm long when it hatches, 2.4 cm before it turns into a frog. */
    private static void tadpole(PoseStack pose, VertexConsumer vc, Tadpole k, float time, int light) {
        float g = Mth.clamp(k.clientGrowth, 0, 1);
        float s = (0.5F + 0.5F * g) * 1.8F; // drawn a little bigger than in nature (visible through the glass)
        PoseStack.Pose last = pose.last();
        // body (z: forward), eyes
        cube(last, vc, 0, 0, 0.012F * s, 0.011F * s, 0.009F * s, 0.014F * s, BODY, 255, light);
        cube(last, vc, 0.009F * s, 0.003F * s, 0.022F * s, 0.002F * s, 0.002F * s, 0.002F * s, EYE, 255, light);
        cube(last, vc, -0.009F * s, 0.003F * s, 0.022F * s, 0.002F * s, 0.002F * s, 0.002F * s, EYE, 255, light);
        if (g > 0.6F) { // hind legs
            float l = (g - 0.6F) / 0.4F;
            cube(last, vc, 0.012F * s, -0.004F * s, -0.004F * s, 0.006F * s * l + 0.001F, 0.0015F * s, 0.002F * s, LEGS, 255, light);
            cube(last, vc, -0.012F * s, -0.004F * s, -0.004F * s, 0.006F * s * l + 0.001F, 0.0015F * s, 0.002F * s, LEGS, 255, light);
        }
        if (g > 0.85F) { // front legs
            cube(last, vc, 0.011F * s, -0.004F * s, 0.018F * s, 0.003F * s, 0.0015F * s, 0.0015F * s, LEGS, 255, light);
            cube(last, vc, -0.011F * s, -0.004F * s, 0.018F * s, 0.003F * s, 0.0015F * s, 0.0015F * s, LEGS, 255, light);
        }
        // the tail beats (shorter when the legs come)
        float tail = 0.03F * s * (1 - 0.6F * Math.max(0, g - 0.8F) / 0.2F);
        pose.pushPose();
        pose.translate(0, 0, -0.001F * s);
        pose.rotate(Axis.YP.rotation(Mth.sin(time * 0.6F + k.id) * 0.45F));
        PoseStack.Pose p = pose.last();
        cube(p, vc, 0, 0, -tail, 0.0012F * s, 0.006F * s, tail, TAIL, 200, light);
        pose.popPose();
    }

    private static void cube(PoseStack.Pose last, VertexConsumer vc, float cx, float cy, float cz, float hx, float hy, float hz, int cell, int alpha, int light) {
        Matrix4f m = last.pose();
        float u0 = (cell * 4 % 16 + 1) / 16F, u1 = (cell * 4 % 16 + 3) / 16F;
        float v0 = (cell / 4 * 4 + 1) / 16F, v1 = (cell / 4 * 4 + 3) / 16F;
        float x0 = cx - hx, x1 = cx + hx, y0 = cy - hy, y1 = cy + hy, z0 = cz - hz, z1 = cz + hz;
        float[][] faces = {
                {x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0},
                {x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0},
                {x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1},
                {x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1},
                {x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0},
                {x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0}};
        float[][] uv = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
        int color = alpha << 24 | 0xFFFFFF;
        for (float[] f : faces) {
            for (int k = 0; k < 4; k++) {
                vc.addVertex(m, f[k * 3], f[k * 3 + 1], f[k * 3 + 2]).setColor(color).setUv(uv[k][0], uv[k][1])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, f[12], f[13], f[14]);
            }
        }
    }
}
