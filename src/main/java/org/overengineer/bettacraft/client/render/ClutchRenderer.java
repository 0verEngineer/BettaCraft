package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.BubbleNest;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tankmates.Clutch;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;

/**
 * M19: the tankmates' clutches drawn by the tank's block entity renderer: corydoras eggs as cream beads glued to the
 * glass (or a leaf), scattered tetra eggs as tiny amber grains, pleco eggs as an orange clump in the cave, larvae as
 * small dark dots with a tail, the dwarf gourami's foam nest like the bettas' ({@link NestRenderer}), and the breeding
 * box hanging inside the front glass with the young in it.
 */
public final class ClutchRenderer {
    private static final ResourceLocation EGGS = BettaCraft.id("textures/entity/tankmates/eggs.png");
    private static final ResourceLocation BOX = BettaCraft.id("textures/entity/tankmates/breeding_box.png");
    /** At most this many eggs of a clutch are drawn. */
    private static final int MAX_DRAWN = 40;

    private ClutchRenderer() {}

    static void render(TankBlockEntity be, TankData t, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        if (t.clutches().isEmpty() && !t.breedingBox()) return;
        BlockPos origin = be.getBlockPos();
        VertexConsumer eggs = null;
        for (Clutch k : t.clutches()) {
            int light = be.getLevel() == null ? 0xF000F0 : LevelRenderer.getLightColor(be.getLevel(), BlockPos.containing(k.x, k.y, k.z));
            if (k.kind == Clutch.Kind.FOAM) {
                foam(be, k, partialTick, pose, buffers);
                continue;
            }
            if (k.kind == Clutch.Kind.BOX) continue;
            if (eggs == null) eggs = buffers.getBuffer(RenderType.entityCutoutNoCull(EGGS));
            pose.pushPose();
            pose.translate(k.x - origin.getX(), k.y - origin.getY(), k.z - origin.getZ());
            clutch(pose, eggs, k, light);
            pose.popPose();
        }
        if (t.breedingBox()) box(be, t, pose, buffers);
    }

    private static void clutch(PoseStack pose, VertexConsumer vc, Clutch k, int light) {
        PoseStack.Pose last = pose.last();
        int n = Math.min(MAX_DRAWN, k.count);
        boolean larvae = k.stage() >= 1;
        int col = switch (k.kind) {
            case GLASS -> 0;
            case CAVE, OPEN -> 2;
            default -> 1;
        };
        // a little bigger than in nature (2 mm corydoras eggs would be a quarter of a pixel)
        float e = switch (k.kind) {
            case GLASS -> 0.016F;
            case CAVE, OPEN -> 0.014F;
            default -> 0.009F;
        };
        float spread = switch (k.kind) {
            case GLASS -> 0.045F + 0.004F * n;
            case CAVE -> 0.035F;
            case OPEN -> 0.04F + 0.002F * n; // M29: a patch of eggs on the stone, leaf or glass
            default -> 0.07F;
        };
        long seed = k.id * 0x9E3779B97F4A7C15L;
        for (int i = 0; i < n; i++) {
            long h = seed + i * 0xBF58476D1CE4E5B9L;
            h ^= h >>> 31;
            float a = (float) ((h >>> 8 & 0xFFFF) / 65536.0 * Math.PI * 2);
            float r = (float) Math.sqrt((h >>> 24 & 0xFFFF) / 65536.0) * spread;
            float da = Mth.cos(a) * r, db = Mth.sin(a) * r;
            float dx, dy, dz;
            if ((k.kind == Clutch.Kind.GLASS || k.kind == Clutch.Kind.OPEN) && k.face >= 0) {
                // on the glass plane: spread along the wall and up and down
                dy = db;
                if (k.face <= 1) {
                    dx = da;
                    dz = 0;
                } else {
                    dx = 0;
                    dz = da;
                }
            } else if (k.kind == Clutch.Kind.CAVE || k.kind == Clutch.Kind.OPEN) {
                dx = da;
                dz = db;
                dy = ((h >>> 44) & 0xFF) / 255F * 0.02F;
            } else {
                dx = da;
                dz = db;
                dy = ((h >>> 44) & 0xFF) / 255F * 0.06F - 0.02F;
            }
            if (larvae) {
                cube(last, vc, dx, dy + 0.006F, dz, e * 0.6F, e * 0.6F, e * 0.6F, 3, light);
                cube(last, vc, dx, dy - 0.004F, dz, e * 0.25F, e * 0.6F, e * 0.25F, col, light);
            } else {
                cube(last, vc, dx, dy, dz, e, e, e, col, light);
            }
        }
    }

    /** The gourami's foam nest: drawn like a betta nest (phase, size, eggs) from the clutch. */
    private static void foam(TankBlockEntity be, Clutch k, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        CompoundTag t = new CompoundTag();
        t.putString("phase", k.building ? "building" : k.count > 0 ? "eggs" : "built");
        t.putDouble("x", k.x);
        t.putDouble("y", k.y);
        t.putDouble("z", k.z);
        t.putDouble("progress", k.progress);
        t.putInt("egg_count", k.count);
        t.putString("stage", k.stage() >= 1 ? "larva" : "egg");
        BubbleNest n = BubbleNest.loadClientView(t);
        pose.pushPose();
        pose.scale(1, 1, 1);
        NestRenderer.render(be, n, partialTick, pose, buffers);
        pose.popPose();
    }

    /** A see-through box in the front glass near the surface, the young in it as small dots. */
    private static void box(TankBlockEntity be, TankData t, PoseStack pose, MultiBufferSource buffers) {
        BlockPos origin = be.getBlockPos();
        Vec3 p = TankmateBreeding.boxPos(t);
        int light = be.getLevel() == null ? 0xF000F0 : LevelRenderer.getLightColor(be.getLevel(), BlockPos.containing(p));
        pose.pushPose();
        pose.translate(p.x - origin.getX(), p.y - origin.getY(), p.z - origin.getZ());
        PoseStack.Pose last = pose.last();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(BOX));
        boolean x = t.front().getAxis() == net.minecraft.core.Direction.Axis.X;
        float hw = 0.16F, hh = 0.09F, hd = 0.08F;
        textured(last, vc, x ? hd : hw, hh, x ? hw : hd, light);
        int n = Math.min(MAX_DRAWN, TankmateBreeding.boxFry(t));
        if (n > 0) {
            VertexConsumer eggs = buffers.getBuffer(RenderType.entityCutoutNoCull(EGGS));
            for (int i = 0; i < n; i++) {
                long h = i * 0x9E3779B97F4A7C15L + 77;
                h ^= h >>> 29;
                float dx = ((h & 0xFF) / 255F - 0.5F) * (x ? hd : hw) * 1.6F;
                float dz = ((h >>> 8 & 0xFF) / 255F - 0.5F) * (x ? hw : hd) * 1.6F;
                float dy = ((h >>> 16 & 0xFF) / 255F - 0.5F) * hh * 1.4F;
                cube(last, eggs, dx, dy, dz, 0.006F, 0.004F, 0.006F, 3, light);
            }
        }
        pose.popPose();
    }

    private static void textured(PoseStack.Pose last, VertexConsumer vc, float hx, float hy, float hz, int light) {
        Matrix4f m = last.pose();
        float x0 = -hx, x1 = hx, y0 = -hy, y1 = hy, z0 = -hz, z1 = hz;
        float[][] faces = {
                {x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0},
                {x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0},
                {x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1},
                {x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1},
                {x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0},
                {x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0}};
        float[][] uv = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
        for (float[] f : faces) {
            for (int k = 0; k < 4; k++) {
                vc.addVertex(m, f[k * 3], f[k * 3 + 1], f[k * 3 + 2]).setColor(0xFFFFFFFF).setUv(uv[k][0], uv[k][1])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, f[12], f[13], f[14]);
            }
        }
    }

    /** Axis-aligned cube in the colour of egg kind {@code col} (0 cory, 1 tetra, 2 pleco, 3 dark larva). */
    private static void cube(PoseStack.Pose last, VertexConsumer vc, float cx, float cy, float cz, float hx, float hy, float hz, int col, int light) {
        Matrix4f m = last.pose();
        float u0, u1, v0 = 0.25F, v1 = 0.75F;
        if (col == 3) {
            u0 = 7F / 48;
            u1 = 8F / 48; // the dark embryo dot of the first egg
            v0 = 7F / 16;
            v1 = 8F / 16;
        } else {
            u0 = (col * 16 + 4) / 48F;
            u1 = (col * 16 + 12) / 48F;
        }
        float x0 = cx - hx, x1 = cx + hx, y0 = cy - hy, y1 = cy + hy, z0 = cz - hz, z1 = cz + hz;
        float[][] faces = {
                {x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0},
                {x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0},
                {x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1},
                {x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1},
                {x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0},
                {x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0}};
        float[][] uv = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
        for (float[] f : faces) {
            for (int k = 0; k < 4; k++) {
                vc.addVertex(m, f[k * 3], f[k * 3 + 1], f[k * 3 + 2]).setColor(0xFFFFFFFF).setUv(uv[k][0], uv[k][1])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, f[12], f[13], f[14]);
            }
        }
    }
}
