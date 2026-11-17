package org.overengineer.bettacraft.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.overengineer.bettacraft.client.render.GlassMobRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** M52: NeoForge's RenderLivingEvent Pre / Post for {@link GlassMobRendering} (spiders on the glass of a terrarium). */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingRendererMixin {
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void bettacraft$pre(LivingEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        GlassMobRendering.pre(e, partialTick, pose);
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("RETURN"))
    private void bettacraft$post(LivingEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        GlassMobRendering.post(e, pose);
    }
}
