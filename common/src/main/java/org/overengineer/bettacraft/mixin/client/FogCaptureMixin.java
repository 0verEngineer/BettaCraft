package org.overengineer.bettacraft.mixin.client;

import java.nio.ByteBuffer;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.overengineer.bettacraft.client.interior.MiniatureFog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** M67.5: the fog values of the frame, for the miniature's own (scaled) fog ({@link MiniatureFog}). */
@Mixin(FogRenderer.class)
public abstract class FogCaptureMixin {
    @Inject(method = "updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V", at = @At("HEAD"), require = 0)
    private void bettacraft$capture(ByteBuffer buffer, int offset, Vector4f color, float envStart, float envEnd, float renderStart, float renderEnd,
                                    float skyEnd, float cloudsEnd, CallbackInfo ci) {
        MiniatureFog.capture(color, envStart, envEnd, renderStart, renderEnd, skyEnd, cloudsEnd);
    }
}
