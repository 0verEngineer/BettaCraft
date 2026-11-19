package org.overengineer.bettacraft.fabric.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.overengineer.bettacraft.fabric.client.BettaCraftFabricClient;
import org.overengineer.bettacraft.platform.client.RenderLevelStageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * M52b: the stage after the translucent terrain (Fabric 1.21.10+ has a world render event before it only; NeoForge:
 * AfterTranslucentBlocks). Fired in the main pass of the level renderer (its lambda, {@code method_62214}) where the
 * tripwire layer begins, i.e. right after the translucent chunk layer was drawn.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererTranslucentMixin {
    @Inject(method = "method_62214", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
            args = "ldc=string"))
    private void bettacraft$afterTranslucent(CallbackInfo ci) {
        BettaCraftFabricClient.fire(null, RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS);
    }
}
