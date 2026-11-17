package org.overengineer.bettacraft.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.overengineer.bettacraft.platform.client.ClientEvents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** M52: NeoForge's MouseScrollingEvent (in the world, no screen open): the scroll of the common client events. */
@Mixin(MouseHandler.class)
public abstract class MouseScrollMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"), cancellable = true)
    private void bettacraft$scroll(long window, double dx, double dy, CallbackInfo ci) {
        double sensitivity = minecraft.options.mouseWheelSensitivity().get();
        boolean discrete = minecraft.options.discreteMouseScroll().get();
        double x = (discrete ? Math.signum(dx) : dx) * sensitivity, y = (discrete ? Math.signum(dy) : dy) * sensitivity;
        if (ClientEvents.fireScroll(x, y)) ci.cancel();
    }
}
