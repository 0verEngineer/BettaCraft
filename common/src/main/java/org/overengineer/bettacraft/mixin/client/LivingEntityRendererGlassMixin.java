package org.overengineer.bettacraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.overengineer.bettacraft.client.render.GlassMobRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * M52b: spiders in glass lying on the glass ({@link GlassMobRendering}) on both loaders; 1.21.1 used NeoForge's
 * RenderLivingEvent (Fabric: a mixin on render), which since 1.21.9 no longer carries the entity.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererGlassMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("RETURN"))
    private void bettacraft$extract(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        GlassMobRendering.extract(entity, state, partialTick);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD"))
    private void bettacraft$pre(LivingEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        GlassMobRendering.pre(state, pose);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("RETURN"))
    private void bettacraft$post(LivingEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        GlassMobRendering.post(state, pose);
    }
}
