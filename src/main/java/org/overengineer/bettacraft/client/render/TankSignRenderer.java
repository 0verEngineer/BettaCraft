package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import org.overengineer.bettacraft.tank.SignPosition;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankShape;

/**
 * Name sign of a tank (PLAN 4.4): black plate with white text, 0.12 blocks high (M15, before 0.25; times the client config
 * {@code render.signScale}), on the front of the whole tank at
 * one of six positions (M10: on an irregular front, on the widest flat part of the top or bottom row). Drawn by the controller; shrinks the text when the front is too narrow. Also draws the bubble
 * nest with its eggs / larvae (M2) and the floating plants on the surface (M12, {@link FloatingPlantRenderer}).
 */
public class TankSignRenderer implements BlockEntityRenderer<TankBlockEntity> {
    /** Height of the plate at {@code render.signScale} 1 (M15: was 0.25). */
    public static final float PLATE_HEIGHT = 0.12F;
    private static final float MARGIN = 0.06F;
    private final Font font;

    public TankSignRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.getFont();
    }


    @Override
    public void render(TankBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        TankData t = be.getTank();
        if (t == null || !be.isController()) return;
        if (t.nest() != null) NestRenderer.render(be, t.nest(), partialTick, pose, buffers); // M15: domed
        FloatingPlantRenderer.render(be, t, partialTick, pose, buffers); // M12
        CritterRenderer.render(be, t, partialTick, pose, buffers); // M14
        AlgaeRenderer.render(be, t, pose, buffers); // M14
        if (!t.signVisible() || t.name().isEmpty()) return;
        Direction front = t.front();
        SignPosition sp = t.signPos();
        // M10: on an irregular front the sign sits on the widest flat run of the top (bottom) row
        TankShape.SignFace face = t.members().isEmpty() ? null : TankShape.signFace(t.members(), front, sp.top());
        BlockPos mn = t.min(), mx = t.max();
        BlockPos origin = be.getBlockPos();
        // front face plane and the "right" direction as seen by a viewer looking at the front
        Direction right = front.getCounterClockWise();
        double faceWidth, centre, plane, rowTop, rowBottom;
        if (face != null) {
            faceWidth = face.width();
            centre = face.centre();
            plane = face.plane();
            rowTop = face.y() + 1;
            rowBottom = face.y();
        } else {
            faceWidth = right.getAxis() == Direction.Axis.X ? t.sizeX() : t.sizeZ();
            centre = right.getAxis() == Direction.Axis.X ? (mn.getX() + mx.getX() + 1) / 2.0 : (mn.getZ() + mx.getZ() + 1) / 2.0;
            plane = front == Direction.EAST ? mx.getX() + 1 : front == Direction.WEST ? mn.getX()
                    : front == Direction.SOUTH ? mx.getZ() + 1 : mn.getZ();
            rowTop = mx.getY() + 1;
            rowBottom = mn.getY();
        }
        double fx = front.getAxis() == Direction.Axis.X ? plane : centre;
        double fz = front.getAxis() == Direction.Axis.Z ? plane : centre;
        double out = 0.01;

        FormattedCharSequence text = Component.literal(t.name()).getVisualOrderText();
        int textW = font.width(text);
        float scale = plateHeight() / 10F;
        float maxW = (float) Math.max(0.2, faceWidth - 2 * MARGIN);
        if ((textW + 2) * scale > maxW) scale = maxW / (textW + 2);
        float plateW = (textW + 2) * scale;
        float plateH = 10 * scale;
        double along = sp.horizontal() * (faceWidth / 2 - MARGIN - plateW / 2);
        double y = sp.top() ? rowTop - MARGIN - plateH / 2 : rowBottom + MARGIN + plateH / 2;

        double x = fx + front.getStepX() * out + right.getStepX() * along;
        double z = fz + front.getStepZ() * out + right.getStepZ() * along;
        BlockPos lightPos = BlockPos.containing(x + front.getStepX() * 0.5, y, z + front.getStepZ() * 0.5);
        int packed = be.getLevel() == null ? light : LevelRenderer.getLightColor(be.getLevel(), lightPos);

        pose.pushPose();
        pose.translate(x - origin.getX(), y - origin.getY(), z - origin.getZ());
        // local +Z = outward normal of the front face
        pose.mulPose(Axis.YP.rotationDegrees(-front.toYRot()));
        // black plate slightly behind the text (towards the glass), double-sided
        org.joml.Matrix4f m = pose.last().pose();
        com.mojang.blaze3d.vertex.VertexConsumer bg = buffers.getBuffer(net.minecraft.client.renderer.RenderType.textBackground());
        float hw = plateW / 2, hh = plateH / 2, bz = -0.004F;
        float[][] quad = {{-hw, -hh}, {hw, -hh}, {hw, hh}, {-hw, hh}};
        for (int i = 0; i < 4; i++) bg.addVertex(m, quad[i][0], quad[i][1], bz).setColor(0xFF101010).setLight(packed);
        for (int i = 3; i >= 0; i--) bg.addVertex(m, quad[i][0], quad[i][1], bz).setColor(0xFF101010).setLight(packed);
        pose.scale(scale, -scale, scale);
        font.drawInBatch(text, -textW / 2F, -4.0F, 0xFFFFFFFF, false, pose.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET,
                0, packed);
        pose.popPose();
    }

    /** Plate height in blocks with the client's {@code render.signScale}. */
    public static float plateHeight() {
        double f;
        try {
            f = org.overengineer.bettacraft.config.ClientConfig.SIGN_SCALE.get();
        } catch (IllegalStateException e) {
            f = 1.0;
        }
        return (float) (PLATE_HEIGHT * Math.max(0.5, Math.min(2.0, f)));
    }

    @Override
    public boolean shouldRenderOffScreen(TankBlockEntity be) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(TankBlockEntity be) {
        TankData t = be.getTank();
        if (t == null || !be.isController()) return BlockEntityRenderer.super.getRenderBoundingBox(be);
        BlockPos mn = t.min(), mx = t.max();
        return new AABB(mn.getX(), mn.getY(), mn.getZ(), mx.getX() + 1, mx.getY() + 1, mx.getZ() + 1).inflate(0.5);
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
