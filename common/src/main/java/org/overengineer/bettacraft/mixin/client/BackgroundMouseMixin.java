package org.overengineer.bettacraft.mixin.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Development only ({@code -Dbettacraft.background=true}): a background test client never captures the mouse, even
 * when the window gets the focus by accident (a second guard next to {@link BackgroundWindowMixin}).
 */
@Mixin(MouseHandler.class)
public abstract class BackgroundMouseMixin {
    private static final boolean BACKGROUND = Boolean.getBoolean("bettacraft.background");

    @Inject(method = "grabMouse", require = 0, at = @At("HEAD"), cancellable = true)
    private void bettacraft$noGrabInBackground(CallbackInfo ci) {
        if (BACKGROUND) ci.cancel();
    }
}
