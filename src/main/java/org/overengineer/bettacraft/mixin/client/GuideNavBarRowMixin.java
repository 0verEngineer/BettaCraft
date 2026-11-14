package org.overengineer.bettacraft.mixin.client;

import guideme.document.LytRect;
import org.overengineer.bettacraft.compat.guideme.DeepNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * M22.2: a row of the handbook navigation is visible when all its ancestors are open (not only its parent), and its
 * bounds (hover highlight, fold triangle) move in with its depth. See {@link DeepNavigation}; skipped without GuideME.
 */
@Pseudo
@Mixin(targets = "guideme.internal.screen.GuideNavBar$Row", remap = false)
public abstract class GuideNavBarRowMixin {
    @Inject(method = "isVisible()Z", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void bettacraft$visible(CallbackInfoReturnable<Boolean> cir) {
        Boolean v = DeepNavigation.visible(this);
        if (v != null) cir.setReturnValue(v);
    }

    @Inject(method = "getBounds()Lguideme/document/LytRect;", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private void bettacraft$bounds(CallbackInfoReturnable<LytRect> cir) {
        int x = DeepNavigation.depth(this) * DeepNavigation.STEP;
        if (x == 0) return;
        LytRect r = cir.getReturnValue();
        cir.setReturnValue(new LytRect(r.x() + x, r.y(), Math.max(1, r.width() - x), r.height()));
    }
}
