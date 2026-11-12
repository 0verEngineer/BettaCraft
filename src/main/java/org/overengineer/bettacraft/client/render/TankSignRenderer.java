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

/**
 * Name sign of a tank (PLAN 4.4): black plate with white text, 0.25 blocks high, on the front of the whole tank at
 * one of six positions. Drawn by the controller; shrinks the text when the front is too narrow.
 */
public class TankSignRenderer implements BlockEntityRenderer<TankBlockEntity> {
    private static final float PLATE_HEIGHT = 0.25F;
    private static final float MARGIN = 0.08F;
    private final Font font;

    public TankSignRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.getFont();
    }

    @Override
    public void render(TankBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        TankData t = be.getTank();
        if (t == null || !be.isController() || !t.signVisible() || t.name().isEmpty()) return;
        Direction front = t.front();
        BlockPos mn = t.min(), mx = t.max();
        BlockPos origin = be.getBlockPos();
        // front face plane and the "right" direction as seen by a viewer looking at the front
        Direction right = front.getCounterClockWise();
        double faceWidth = right.getAxis() == Direction.Axis.X ? t.sizeX() : t.sizeZ();
        double cx = (mn.getX() + mx.getX() + 1) / 2.0, cz = (mn.getZ() + mx.getZ() + 1) / 2.0;
        double fx = front == Direction.EAST ? mx.getX() + 1 : front == Direction.WEST ? mn.getX() : cx;
        double fz = front == Direction.SOUTH ? mx.getZ() + 1 : front == Direction.NORTH ? mn.getZ() : cz;
        double out = 0.01;

        FormattedCharSequence text = Component.literal(t.name()).getVisualOrderText();
        int textW = font.width(text);
        float scale = PLATE_HEIGHT / 10F;
        float maxW = (float) Math.max(0.2, faceWidth - 2 * MARGIN);
        if ((textW + 2) * scale > maxW) scale = maxW / (textW + 2);
        float plateW = (textW + 2) * scale;
        float plateH = 10 * scale;
        SignPosition sp = t.signPos();
        double along = sp.horizontal() * (faceWidth / 2 - MARGIN - plateW / 2);
        double y = sp.top() ? mx.getY() + 1 - MARGIN - plateH / 2 : mn.getY() + MARGIN + plateH / 2;

        double x = fx + front.getStepX() * out + right.getStepX() * along;
        double z = fz + front.getStepZ() * out + right.getStepZ() * along;
        BlockPos lightPos = BlockPos.containing(x + front.getStepX() * 0.5, y, z + front.getStepZ() * 0.5);
        int packed = be.getLevel() == null ? light : LevelRenderer.getLightColor(be.getLevel(), lightPos);

        pose.pushPose();
        pose.translate(x - origin.getX(), y - origin.getY(), z - origin.getZ());
        pose.mulPose(Axis.YP.rotationDegrees(-front.toYRot()));
        pose.scale(scale, -scale, scale);
        font.drawInBatch(text, -textW / 2F, -4.0F, 0xFFFFFFFF, false, pose.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET,
                0xFF000000, packed);
        pose.popPose();
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
