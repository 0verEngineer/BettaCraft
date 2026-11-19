package org.overengineer.bettacraft.mixin.client;

import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.overengineer.bettacraft.client.render.LevelDraws;
import org.overengineer.bettacraft.platform.client.RenderLevelStageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * M52e: the main pass of 26.3's level rendering (see {@link LevelDraws}): our stages of it run right before it is opened,
 * and the pass is kept for the draws of those stages. With improved transparency (OIT) the classic translucent part does
 * not run in that pass: our translucent draws come at the end of the solid part then.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererPassMixin {
    @Inject(method = "prepareTranslucents", at = @At("HEAD"), require = 1)
    private void bettacraft$prepare(CallbackInfo ci) {
        LevelDraws.prepare();
    }

    @Inject(method = "executeSolid", at = @At("HEAD"), require = 1)
    private void bettacraft$solid(ChunkSectionsToRender sections, FeatureRenderDispatcher.PreparedFrame frame, RenderPass pass, CallbackInfo ci) {
        LevelDraws.mainPass(pass);
    }

    @Inject(method = "executeSolid", at = @At("TAIL"), require = 1)
    private void bettacraft$solidEnd(ChunkSectionsToRender sections, FeatureRenderDispatcher.PreparedFrame frame, RenderPass pass, CallbackInfo ci) {
        if (net.minecraft.client.Minecraft.getInstance().gameRenderer.useImprovedTransparency()) {
            LevelDraws.replay(RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS, pass);
        }
    }
}
