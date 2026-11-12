package org.overengineer.bettacraft.client.decor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.overengineer.bettacraft.decor.DecorItem;
import org.overengineer.bettacraft.decor.DecorTemplate;
import org.overengineer.bettacraft.network.DecorTemplatePayload;

/**
 * Placement hologram of decoration items (PLAN 7.3): while a decor item is held and a block is targeted, the
 * structure is drawn translucent where it would be placed (tinted red where a block is in the way).
 */
public final class DecorPreview {
    private static final Map<ResourceLocation, DecorTemplate> TEMPLATES = new HashMap<>();
    private static final Set<ResourceLocation> REQUESTED = new HashSet<>();

    private DecorPreview() {}

    public static void accept(DecorTemplatePayload p) {
        p.template().ifPresent(t -> TEMPLATES.put(p.id(), t));
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
        ResourceLocation id = item.type().structure();
        DecorTemplate t = TEMPLATES.get(id);
        if (t == null) {
            if (REQUESTED.add(id)) PacketDistributor.sendToServer(new DecorTemplatePayload(id, java.util.Optional.empty()));
            return;
        }
        BlockPos clicked = hit.getBlockPos();
        BlockPos base = mc.level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
        Rotation rot = DecorTemplate.rotationFor(mc.player.getDirection());
        List<DecorTemplate.Placed> blocks = t.layout(base, rot);
        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType rt = Sheets.translucentCullBlockSheet();
        float pulse = 0.45F + 0.1F * (float) Math.sin((mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false)) * 0.2);
        for (DecorTemplate.Placed b : blocks) {
            if (b.state().getRenderShape() != RenderShape.MODEL) continue;
            boolean free = mc.level.getBlockState(b.pos()).canBeReplaced();
            VertexConsumer vc = new Tinted(buffers.getBuffer(rt), free ? 1F : 1F, free ? 1F : 0.35F, free ? 1F : 0.35F, pulse);
            ps.pushPose();
            ps.translate(b.pos().getX() - cam.x, b.pos().getY() - cam.y, b.pos().getZ() - cam.z);
            ps.translate(0.5, 0.5, 0.5);
            ps.scale(1.002F, 1.002F, 1.002F);
            ps.translate(-0.5, -0.5, -0.5);
            mc.getBlockRenderer().renderSingleBlock(b.state(), ps, x -> vc, 0xF000F0, OverlayTexture.NO_OVERLAY,
                    net.neoforged.neoforge.client.model.data.ModelData.EMPTY, rt);
            ps.popPose();
        }
        buffers.endBatch(rt);
    }

    /** Multiplies the colour and sets the alpha of everything written through it. */
    private record Tinted(VertexConsumer d, float r, float g, float b, float a) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) { d.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            d.setColor((int) (red * r), (int) (green * g), (int) (blue * b), (int) (255 * a));
            return this;
        }
        @Override public VertexConsumer setUv(float u, float v) { d.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { d.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { d.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { d.setNormal(x, y, z); return this; }
    }
}
