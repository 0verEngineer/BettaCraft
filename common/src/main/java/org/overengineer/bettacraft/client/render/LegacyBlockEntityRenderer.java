package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * M52b: a block entity renderer of the mod on the 1.21.9 render states. The state keeps the block entity of this frame;
 * at submit time the 1.21.1 {@link #render} code draws into {@link RecordingBuffers}, which goes to the submit queue.
 * {@link #collector()} is the queue of the current submit (for items and blocks, which have their own submit calls now).
 */
public abstract class LegacyBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, LegacyBlockEntityRenderer.State<T>> {
    private static final ThreadLocal<SubmitNodeCollector> CURRENT = new ThreadLocal<>();

    public static class State<T extends BlockEntity> extends BlockEntityRenderState {
        public T blockEntity;
        public float partialTick;
        public Vec3 cameraPos = Vec3.ZERO;
    }

    @Override
    public State<T> createRenderState() {
        return new State<>();
    }

    @Override
    public void extractRenderState(T be, State<T> state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, crumbling);
        state.blockEntity = be;
        state.partialTick = partialTick;
        state.cameraPos = cameraPos;
    }

    @Override
    public void submit(State<T> state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.blockEntity == null) return;
        RecordingBuffers buffers = new RecordingBuffers();
        SubmitNodeCollector before = CURRENT.get();
        CURRENT.set(collector);
        try {
            render(state.blockEntity, state.partialTick, pose, buffers, state.lightCoords, OverlayTexture.NO_OVERLAY);
        } finally {
            if (before == null) CURRENT.remove();
            else CURRENT.set(before);
        }
        buffers.submitTo(collector);
    }

    /** The submit queue of the renderer being drawn right now (only inside {@link #render}). */
    public static SubmitNodeCollector collector() {
        SubmitNodeCollector c = CURRENT.get();
        if (c == null) throw new IllegalStateException("not inside a block entity renderer's submit");
        return c;
    }

    /** The 1.21.1 drawing code: everything drawn into {@code buffers} goes to Minecraft's submit queue. */
    public abstract void render(T be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay);
}
