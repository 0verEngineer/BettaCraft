package org.overengineer.bettacraft.mixin.client;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.overengineer.bettacraft.platform.client.RenderFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * M52b: the culling frustum and projection of the frame (1.21.1 handed them to the render stage events; since 1.21.9 the
 * loaders' level render events have neither), kept for {@link RenderFrame}.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererFrustumMixin {
    @Inject(method = "prepareCullFrustum", at = @At("RETURN"))
    private void bettacraft$frustum(Matrix4f modelView, Matrix4f projection, Vec3 camera, CallbackInfoReturnable<Frustum> cir) {
        RenderFrame.capture(cir.getReturnValue(), projection);
    }
}
