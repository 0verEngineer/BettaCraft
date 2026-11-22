package org.overengineer.bettacraft.client.decor;

import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import org.overengineer.bettacraft.client.render.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.platform.client.RenderLevelStageEvent;
import org.overengineer.bettacraft.platform.network.PacketDistributor;
import org.overengineer.bettacraft.decor.DecorItem;
import org.overengineer.bettacraft.decor.DecorTemplate;
import org.overengineer.bettacraft.network.DecorTemplatePayload;

/**
 * Placement hologram of decoration items (PLAN 7.3): while a decor item is held and a block is targeted, the
 * structure is drawn translucent where it would be placed (tinted red where a block is in the way).
 */
public final class DecorPreview {
    private static final Map<Identifier, DecorTemplate> TEMPLATES = new HashMap<>();
    private static final Set<Identifier> REQUESTED = new HashSet<>();

    private DecorPreview() {}

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("bettacraft/decor");
    private static boolean drawnLogged;

    public static void accept(DecorTemplatePayload p) {
        p.template().ifPresent(t -> TEMPLATES.put(p.id(), t));
        if (org.overengineer.bettacraft.debug.LogArea.NETWORK.debug) org.overengineer.bettacraft.debug.LogArea.NETWORK.debug("Received decor template {} ({} blocks)", p.id(), p.template().map(t -> t.blocks().size()).orElse(0));
    }

    public static void clear() {
        TEMPLATES.clear();
        REQUESTED.clear();
    }

    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof DecorItem item)) return;
        Identifier id = item.type().structure();
        DecorTemplate t = TEMPLATES.get(id);
        if (t == null) {
            if (REQUESTED.add(id)) {
                if (org.overengineer.bettacraft.debug.LogArea.NETWORK.debug) org.overengineer.bettacraft.debug.LogArea.NETWORK.debug("Requesting decor template {}", id);
                PacketDistributor.sendToServer(new DecorTemplatePayload(id, java.util.Optional.empty()));
            }
            return;
        }
        BlockPos clicked = hit.getBlockPos();
        BlockPos base = mc.level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
        Rotation rot = DecorTemplate.rotationFor(mc.player.getDirection());
        List<DecorTemplate.Placed> blocks = t.layout(base, rot);
        Vec3 cam = event.getCamera().position();
        PoseStack ps = event.getPoseStack();
        org.overengineer.bettacraft.client.render.ImmediateBuffers buffers = org.overengineer.bettacraft.client.render.ImmediateBuffers.shared();
        RenderType rt = Sheets.translucentBlockItemSheet();
        // M5: more visible - stronger alpha, a slight cyan glow and an outline of the whole footprint
        float pulse = 0.68F + 0.12F * (float) Math.sin((mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false)) * 0.2);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        boolean blocked = false;
        for (DecorTemplate.Placed b : blocks) {
            BlockPos q = b.pos();
            minX = Math.min(minX, q.getX()); minY = Math.min(minY, q.getY()); minZ = Math.min(minZ, q.getZ());
            maxX = Math.max(maxX, q.getX()); maxY = Math.max(maxY, q.getY()); maxZ = Math.max(maxZ, q.getZ());
            if (b.state().getRenderShape() != RenderShape.MODEL) continue;
            boolean free = mc.level.getBlockState(b.pos()).canBeReplaced();
            blocked |= !free;
            VertexConsumer vc = new Tinted(buffers.getBuffer(rt), free ? 0.85F : 1F, free ? 1F : 0.3F, free ? 1.1F : 0.3F, pulse);
            ps.pushPose();
            ps.translate(b.pos().getX() - cam.x, b.pos().getY() - cam.y, b.pos().getZ() - cam.z);
            ps.translate(0.5, 0.5, 0.5);
            ps.scale(1.002F, 1.002F, 1.002F);
            ps.translate(-0.5, -0.5, -0.5);
            // M52b: tesselated without face culling like 1.21.1's renderSingleBlock; Tinted makes it full bright
            var cp = org.overengineer.bettacraft.platform.client.ClientPlatform.get();
            var model = mc.getModelManager().getBlockStateModelSet().get(b.state());
            cp.tesselate(b.state(), b.pos(), mc.level, ps, x -> vc, false, net.minecraft.util.RandomSource.create(42L),
                    cp.modelData(model, mc.level, b.pos(), b.state()), null);
            ps.popPose();
        }
        buffers.endBatch(rt);
        if (!blocks.isEmpty()) {
            ps.pushPose();
            ps.translate(-cam.x, -cam.y, -cam.z);
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1).inflate(0.01);
            outline(ps, buffers.getBuffer(RenderTypes.lines()), net.minecraft.world.phys.shapes.Shapes.create(box),
                    net.minecraft.util.ARGB.colorFromFloat(0.9F, blocked ? 1F : 0.35F, blocked ? 0.25F : 0.95F, blocked ? 0.25F : 1F),
                    mc.getWindow().getAppropriateLineWidth());
            ps.popPose();
            buffers.endBatch(RenderTypes.lines());
        }
        if (!drawnLogged) {
            drawnLogged = true;
            if (org.overengineer.bettacraft.debug.LogArea.NETWORK.debug) org.overengineer.bettacraft.debug.LogArea.NETWORK.debug("Decor hologram {}: {} blocks at {}", id, blocks.size(), base);
        }
    }

    /** Multiplies the colour and sets the alpha of everything written through it. */
    private record Tinted(VertexConsumer d, float r, float g, float b, float a) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) { d.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            d.setColor(Math.min(255, (int) (red * r)), Math.min(255, (int) (green * g)), Math.min(255, (int) (blue * b)), (int) (255 * a));
            return this;
        }
        @Override public VertexConsumer setUv(float u, float v) { d.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { d.setUv1(u, v); return this; }
        @Override public VertexConsumer setColor(int argb) {
            return setColor(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24);
        }
        @Override public VertexConsumer setUv2(int u, int v) { d.setUv2(240, 240); return this; } // full bright
        @Override public VertexConsumer setUv3(float u, float v) { d.setUv3(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { d.setNormal(x, y, z); return this; }
        @Override public VertexConsumer setLineWidth(float width) { d.setLineWidth(width); return this; }
    }

    /** M52d: the edges of a shape as lines (26.2 has no ShapeRenderer any more). */
    private static void outline(PoseStack ps, com.mojang.blaze3d.vertex.VertexConsumer vc, net.minecraft.world.phys.shapes.VoxelShape shape, int color, float width) {
        PoseStack.Pose pose = ps.last();
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            org.joml.Vector3f n = new org.joml.Vector3f((float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1)).normalize();
            vc.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(color).setNormal(pose, n).setLineWidth(width);
            vc.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(color).setNormal(pose, n).setLineWidth(width);
        });
    }
}
