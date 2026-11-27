package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
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
public class TankSignRenderer extends org.overengineer.bettacraft.client.render.LegacyBlockEntityRenderer<TankBlockEntity> {
    /** Height of the plate at {@code render.signScale} 1 (M15: was 0.25). */
    public static final float PLATE_HEIGHT = 0.12F;
    private static final float MARGIN = 0.06F;
    private final Font font;

    public TankSignRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.font();
    }


    /**
     * M17: the sign face was worked out from all tank members every frame; the client tank data is replaced on every
     * update, so the result is kept per data object (weakly) and front / top row.
     */
    private static final java.util.Map<TankData, Object[]> FACES = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    static TankShape.SignFace signFace(TankData t, Direction front, boolean top) {
        if (t.members().isEmpty()) return null;
        Object[] c = FACES.get(t);
        if (c != null && c[0] == front && (Boolean) c[1] == top) return (TankShape.SignFace) c[2];
        TankShape.SignFace face = TankShape.signFace(t.members(), front, top);
        FACES.put(t, new Object[]{front, top, face});
        return face;
    }

    @Override
    public void render(TankBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        TankData t = be.getTank();
        if (t == null || !be.isController()) return;
        if (BlockEntityCull.outside(getRenderBoundingBox(be))) return; // M66.5: Sodium draws the global list without a view test
        TravellingTankRenderer.render(be, t, partialTick, pose, buffers, light); // M32 part b: on a contraption or an airship
        if (t.nest() != null) NestRenderer.render(be, t.nest(), partialTick, pose, buffers); // M15: domed
        FloatingPlantRenderer.render(be, t, partialTick, pose, buffers); // M12
        CritterRenderer.render(be, t, partialTick, pose, buffers); // M14
        AlgaeRenderer.render(be, t, pose, buffers); // M14
        ClutchRenderer.render(be, t, partialTick, pose, buffers); // M19
        FrogBroodRenderer.render(be, t, partialTick, pose, buffers); // M47 part c
        if (!t.signVisible() || t.name().isEmpty()) return;
        Direction front = t.front();
        SignPosition sp = t.signPos();
        // M10: on an irregular front the sign sits on the widest flat run of the top (bottom) row
        TankShape.SignFace face = signFace(t, front, sp.top());
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
        int packed = be.getLevel() == null ? light : net.minecraft.util.LightCoordsUtil.getLightCoords(be.getLevel(), lightPos);

        pose.pushPose();
        pose.translate(x - origin.getX(), y - origin.getY(), z - origin.getZ());
        // local +Z = outward normal of the front face
        pose.mulPose(Axis.YP.rotationDegrees(-front.toYRot()));
        // black plate slightly behind the text (towards the glass), double-sided
        org.joml.Matrix4f m = pose.last().pose();
        com.mojang.blaze3d.vertex.VertexConsumer bg = buffers.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.textBackground());
        float hw = plateW / 2, hh = plateH / 2, bz = -0.004F;
        float[][] quad = {{-hw, -hh}, {hw, -hh}, {hw, hh}, {-hw, hh}};
        for (int i = 0; i < 4; i++) bg.addVertex(m, quad[i][0], quad[i][1], bz).setColor(0xFF101010).setLight(packed);
        for (int i = 3; i >= 0; i--) bg.addVertex(m, quad[i][0], quad[i][1], bz).setColor(0xFF101010).setLight(packed);
        pose.scale(scale, -scale, scale);
        LegacyBlockEntityRenderer.collector().submitText(pose, -textW / 2F, -4.0F, text, false, Font.DisplayMode.POLYGON_OFFSET, packed, 0xFFFFFFFF, 0, 0);
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

    /**
     * Only the controller draws (the whole tank). M66: every tank block used to say yes here, so the chunk mesher put every block
     * entity of every tank into the list drawn every frame whether its section is in view or not (Sodium: also in the Iris shadow
     * pass): thousands of calls per frame that only returned, most of them from the large tanks. Whether a block is the controller
     * is known when its section is built (the chunk packet carries it; a change rebuilds the section, TankBlockEntity).
     * Port 26.x: this method has no block entity here, so every tank block stays in the list; {@link #shouldRender} drops the
     * others at once.
     */
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    /** M66.5: the box of the tank blocks that draw nothing (everything but the controller): never in view. */
    private static final AABB NOWHERE = new AABB(0, -1.0E7, 0, 0, -1.0E7, 0);

    public AABB getRenderBoundingBox(TankBlockEntity be) {
        TankData t = be.getTank();
        // M66.5: NeoForge asks every tank block in view for its box every frame; only the controller draws (shouldRender), the
        // others are left out at the frustum test, without a new box each frame
        if (t == null || !be.isController()) return NOWHERE;
        // M32 part b: on an airship the block entity sits in Sable's plot far away and is drawn at the ship's pose: the box
        // in plot coordinates would never be in the view (the distance check still applies, with Sable's camera)
        if (TravellingTankRenderer.travelling(be)) return org.overengineer.bettacraft.platform.Platform.get().infiniteRenderBox();
        // M66.5: once a second (asked every frame, and again by the frustum test of render)
        return be.renderBoxCache().get(be.getLevel() == null ? 0 : be.getLevel().getGameTime(), () -> {
            BlockPos mn = t.min(), mx = t.max();
            return new AABB(mn.getX(), mn.getY(), mn.getZ(), mx.getX() + 1, mx.getY() + 1, mx.getZ() + 1).inflate(0.5);
        });
    }

    /**
     * M66: the distance is measured to the nearest point of the tank, not to the controller block (vanilla's default). The
     * controller sits at one corner: on a large tank (28 blocks long) the algae, floating plants, small animals and the sign of the
     * whole tank popped in and out while the player moved at about {@code render.tankDetailDistance} from that corner, also with
     * the near end of the tank only a few blocks away (the flicker of the algae when moving).
     */
    @Override
    public boolean shouldRender(TankBlockEntity be, net.minecraft.world.phys.Vec3 cam) {
        if (!be.isController()) return false; // only the controller draws (render returns at once for the others)
        TankData t = be.getTank();
        if (t == null || TravellingTankRenderer.travelling(be)) return super.shouldRender(be, cam);
        BlockPos mn = t.min(), mx = t.max();
        double dx = Math.max(0, Math.max(mn.getX() - cam.x, cam.x - (mx.getX() + 1)));
        double dy = Math.max(0, Math.max(mn.getY() - cam.y, cam.y - (mx.getY() + 1)));
        double dz = Math.max(0, Math.max(mn.getZ() - cam.z, cam.z - (mx.getZ() + 1)));
        double d = getViewDistance();
        return dx * dx + dy * dy + dz * dz < d * d;
    }

    @Override
    public int getViewDistance() {
        return org.overengineer.bettacraft.config.ClientConfig.TANK_DETAIL_DISTANCE.get(); // M17: configurable (was 48)
    }
}
