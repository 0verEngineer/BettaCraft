package org.overengineer.bettacraft.mixin.client;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.overengineer.bettacraft.platform.client.RenderFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * M52b: the culling frustum and projection of the frame (1.21.1 handed them to the render stage events; since 1.21.9 the
 * loaders' level render events have neither), kept for {@link RenderFrame}. M52c (26.1): the camera's render state carries
 * the frustum and the projection; taken when the level starts to render.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererFrustumMixin {
    @Inject(method = "render", at = @At("HEAD")) // M52d: 26.2 renamed renderLevel to render (without the sections)
    private void bettacraft$frustum(com.mojang.blaze3d.resource.GraphicsResourceAllocator allocator, net.minecraft.client.DeltaTracker deltaTracker,
            boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelView, com.mojang.blaze3d.buffers.GpuBufferSlice terrainFog,
            org.joml.Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        RenderFrame.capture(cameraState.cullFrustum, new Matrix4f(modelView), cameraState.projectionMatrix);
    }
}
