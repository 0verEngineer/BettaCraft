package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
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
    private static final ResourceLocation DUCKWEED = BettaCraft.id(DIR + "duckweed.png");
    private static final ResourceLocation FROGBIT = BettaCraft.id(DIR + "frogbit_leaf.png");
    private static final ResourceLocation SALVINIA = BettaCraft.id(DIR + "salvinia.png");
    private static final ResourceLocation RED_ROOT = BettaCraft.id(DIR + "red_root_floater_leaf.png");
    private static final ResourceLocation ROOTS = BettaCraft.id(DIR + "roots.png");
    /** The vanilla lily pad texture (a game asset read at runtime, tinted like the lily pad block). */
    private static final ResourceLocation LILY_PAD = ResourceLocation.withDefaultNamespace("textures/block/lily_pad.png");
    private static final int LILY_TINT = 0xFF3A9A3E;
    private static final int FROGBIT_ROOTS = 0xFFE6E2D2, SALVINIA_ROOTS = 0xFF7A6142;
    /** Height of the leaves above the drawn water surface (no z-fighting with the water quad) and the bobbing. */
    private static final float ABOVE = 0.008F, BOB = 0.003F;

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
        for (FloatingPlant p : t.plants()) {
            double x = p.renderX(time, TankPlants.SYNC_TICKS), z = p.renderZ(time, TankPlants.SYNC_TICKS);
            col.set(Mth.floor(x), layer, Mth.floor(z));
            double y = surfaceY(level, col, dry, t);
            float bob = p.type.movable() ? (float) Math.sin(time * 0.07 + p.id * 1.7) * BOB : 0;
            int light = LevelRenderer.getLightColor(level, col);
            pose.pushPose();
            pose.translate(x - origin.getX(), y - origin.getY() + ABOVE + BOB + bob, z - origin.getZ());
            pose.mulPose(Axis.YP.rotation(p.rot));
            drawPlant(pose, buffers, p, light, dry);
            pose.popPose();
        }
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

    private static void drawPlant(PoseStack pose, MultiBufferSource buffers, FloatingPlant p, int light, boolean dry) {
        float r = (float) p.radius();
        float grow = 0.5F + 0.5F * p.size;
        switch (p.type) {
            case DUCKWEED -> {
                VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(DUCKWEED));
                flat(pose, vc, 0, 0, r, 0, 0xFFFFFFFF, light);
                // a second, smaller patch turned against the first: looks less like a square
                flat(pose, vc, r * 0.35F, -r * 0.2F, r * 0.7F, 0.0015F, 0xFFFFFFFF, light, 0.9F);
            }
            case FROGBIT -> {
                VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(FROGBIT));
                rosette(pose, vc, p, r, 3 + Math.floorMod(p.id, 3), 0.55F, 0.5F, 0xFFFFFFFF, light);
                if (!dry) roots(pose, buffers, (float) (p.type.rootLength() * grow), r * 0.55F, 3, FROGBIT_ROOTS, light);
            }
            case SALVINIA -> {
                VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(SALVINIA));
                flat(pose, vc, 0, 0, r, 0, 0xFFFFFFFF, light);
                flat(pose, vc, r * 0.25F, r * 0.3F, r * 0.75F, 0.0015F, 0xFFFFFFFF, light, 1.4F);
                if (!dry) roots(pose, buffers, (float) (p.type.rootLength() * grow), r * 0.6F, 2, SALVINIA_ROOTS, light);
            }
            case RED_ROOT_FLOATER -> {
                int tint = FloatingPlantSim.redRootLeafTint(p.red);
                VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(RED_ROOT));
                rosette(pose, vc, p, r, 4 + Math.floorMod(p.id, 3), 0.5F, 0.42F, tint, light);
                if (!dry) roots(pose, buffers, (float) (p.type.rootLength() * grow), r * 0.5F, 3, FloatingPlantSim.redRootRootTint(p.red), light);
            }
            case LILY_PAD -> {
                VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(LILY_PAD));
                flat(pose, vc, 0, 0, r, 0, LILY_TINT, light);
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
            flat(pose, vc, cx, cz, lr, i * 0.0008F, tint, light, a + (float) Math.PI / 2);
        }
        flat(pose, vc, 0, 0, lr * 0.8F, leaves * 0.0008F, tint, light, p.id * 0.9F);
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

    /** Crossed vertical strips of root texture hanging down {@code length} blocks, {@code width} wide. */
    private static void roots(PoseStack pose, MultiBufferSource buffers, float length, float width, int planes, int argb, int light) {
        if (length <= 0.01F) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(ROOTS));
        PoseStack.Pose last = pose.last();
        Matrix4f m = last.pose();
        float v1 = Math.min(1, length / 0.45F);
        for (int i = 0; i < planes; i++) {
            float a = (float) (i * Math.PI / planes);
            float dx = Mth.cos(a) * width, dz = Mth.sin(a) * width;
            float nx = -Mth.sin(a), nz = Mth.cos(a);
            float top = -0.004F;
            vc.addVertex(m, -dx, top, -dz).setColor(argb).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, nx, 0, nz);
            vc.addVertex(m, -dx, top - length, -dz).setColor(argb).setUv(0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, nx, 0, nz);
            vc.addVertex(m, dx, top - length, dz).setColor(argb).setUv(1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, nx, 0, nz);
            vc.addVertex(m, dx, top, dz).setColor(argb).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, nx, 0, nz);
        }
    }
}
