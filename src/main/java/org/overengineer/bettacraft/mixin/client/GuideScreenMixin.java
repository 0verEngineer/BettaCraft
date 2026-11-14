package org.overengineer.bettacraft.mixin.client;

import net.minecraft.resources.ResourceLocation;
import org.overengineer.bettacraft.compat.guideme.DeepNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * M22.2: after the handbook loaded a page, the navigation folds open the chapters above it and scrolls to its row
 * ({@link DeepNavigation#reveal}). Skipped without GuideME ({@link Pseudo}, {@code require = 0}).
 */
@Pseudo
@Mixin(targets = "guideme.internal.screen.GuideScreen", remap = false)
public abstract class GuideScreenMixin {
    @Inject(method = "loadPage(Lnet/minecraft/resources/ResourceLocation;)V", at = @At("TAIL"), require = 0, remap = false)
    private void bettacraft$reveal(ResourceLocation pageId, CallbackInfo ci) {
        DeepNavigation.reveal(this);
    }
}
