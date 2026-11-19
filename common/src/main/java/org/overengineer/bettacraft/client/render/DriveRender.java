package org.overengineer.bettacraft.client.render;

import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.CabinetBlockEntity;

/**
 * M41: the drawing of Create's drive on our blocks (no Create classes): the coupling flange on the drive side of a cabinet
 * and the turning shaft stub of the drive adapter ({@code compat.create.DriveAdapterRenderer}), as boxes with our own
 * procedural textures.
 */
public final class DriveRender {
    private DriveRender() {}

    public static TextureAtlasSprite sprite(String name) {
        return org.overengineer.bettacraft.client.ClientCompat.blockSprites().apply(BettaCraft.id("block/" + name));
    }

    /** Turns the pose so that model north (-z) points to {@code facing}, about the block centre. */
    public static void face(PoseStack ps, Direction facing) {
        ps.translate(0.5, 0.5, 0.5);
        switch (facing) {
            case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> ps.mulPose(Axis.YP.rotationDegrees(-90));
            case WEST -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case UP -> ps.mulPose(Axis.XP.rotationDegrees(90));
            case DOWN -> ps.mulPose(Axis.XP.rotationDegrees(-90));
            default -> {
            }
        }
        ps.translate(-0.5, -0.5, -0.5);
    }

    /** The flange where the shaft goes into a cabinet (on its drive side), a ring around the shaft hole. */
    public static void cabinetCoupling(CabinetBlockEntity cab, PoseStack ps, MultiBufferSource buffers, int light) {
        Direction face = cab.driveFace();
        if (face == null || cab.getLevel() == null) return;
        // the light in front of the face (the cabinet's own block is dark inside)
        light = net.minecraft.client.renderer.LevelRenderer.getLightCoords(cab.getLevel(), cab.getBlockPos().relative(face));
        VertexConsumer vc = buffers.getBuffer(RenderTypes.entityCutoutCull(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS));
        TextureAtlasSprite s = sprite("drive_coupling");
        ps.pushPose();
        face(ps, face);
        // a square flange 10 x 10 px standing 1 px out of the face (north = -z), the hub 6 x 6 px one more
        box(ps, vc, s, 3, 3, -1, 13, 13, 0.01F, light);
        box(ps, vc, s, 5, 5, -2, 11, 11, -1, light);
        ps.popPose();
    }

    /** A box in pixels of the block (0..16), textured with {@code s} (each face maps its own size on the sprite). */
    public static void box(PoseStack ps, VertexConsumer vc, TextureAtlasSprite s, float x0, float y0, float z0, float x1, float y1, float z1, int light) {
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        float a = x0 / 16F, b = y0 / 16F, c = z0 / 16F, d = x1 / 16F, e = y1 / 16F, f = z1 / 16F;
        float w = Math.min(16, x1 - x0), h = Math.min(16, y1 - y0), l = Math.min(16, z1 - z0);
        quad(m, pose, vc, s, light, 0, 0, -1, a, e, c, d, e, c, d, b, c, a, b, c, w, h);
        quad(m, pose, vc, s, light, 0, 0, 1, d, e, f, a, e, f, a, b, f, d, b, f, w, h);
        quad(m, pose, vc, s, light, -1, 0, 0, a, e, f, a, e, c, a, b, c, a, b, f, l, h);
        quad(m, pose, vc, s, light, 1, 0, 0, d, e, c, d, e, f, d, b, f, d, b, c, l, h);
        quad(m, pose, vc, s, light, 0, 1, 0, a, e, f, d, e, f, d, e, c, a, e, c, w, l);
        quad(m, pose, vc, s, light, 0, -1, 0, a, b, c, d, b, c, d, b, f, a, b, f, w, l);
    }

    private static void quad(Matrix4f m, PoseStack.Pose pose, VertexConsumer vc, TextureAtlasSprite s, int light, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
                             float wu, float hv) {
        float u0 = s.getU(0), v0 = s.getV(0), u1 = s.getU(wu / 16F), v1 = s.getV(hv / 16F);
        float[][] p = {{ax, ay, az}, {bx, by, bz}, {cx, cy, cz}, {dx, dy, dz}};
        float[][] uv = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
        for (int i = 0; i < 4; i++) {
            vc.addVertex(m, p[i][0], p[i][1], p[i][2]).setColor(255, 255, 255, 255).setUv(uv[i][0], uv[i][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        }
    }
}
