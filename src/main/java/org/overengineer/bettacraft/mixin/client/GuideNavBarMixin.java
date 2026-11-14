package org.overengineer.bettacraft.mixin.client;

import org.overengineer.bettacraft.compat.guideme.DeepNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * M22.2: the handbook navigation shows pages at any depth (chapters, sub-chapters, pages), see {@link DeepNavigation}.
 * GuideME is optional: without it this mixin is skipped ({@link Pseudo}, {@code require = 0}); if its navigation bar
 * changes, {@link DeepNavigation} gives up and GuideME's own two levels stay.
 */
@Pseudo
@Mixin(targets = "guideme.internal.screen.GuideNavBar", remap = false)
public abstract class GuideNavBarMixin {
    @Inject(method = "recreateRows()V", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void bettacraft$deepRows(CallbackInfo ci) {
        if (DeepNavigation.recreate(this)) ci.cancel();
    }

    @Inject(method = "updateLayout()V", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void bettacraft$deepLayout(CallbackInfo ci) {
        if (DeepNavigation.layout(this)) ci.cancel();
    }
}
