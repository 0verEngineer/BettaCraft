package org.overengineer.bettacraft.fabric.mixin;

import org.overengineer.bettacraft.fabric.FabricGameEvents;
import org.overengineer.bettacraft.platform.event.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52: fires the common game event NeoForge fires here (Fabric API has no callback for it). */
@Mixin(net.minecraft.world.entity.projectile.FishingHook.class)
public abstract class FishingHookMixin {

    @Inject(method = "retrieve", at = @At("HEAD"), cancellable = true)
    private void bettacraft$fished(net.minecraft.world.item.ItemStack rod, CallbackInfoReturnable<Integer> cir) {
        var hook = (net.minecraft.world.entity.projectile.FishingHook) (Object) this;
        if (hook.getPlayerOwner() == null || !FabricGameEvents.listens(ItemFishedEvent.class)) return;
        if (FabricGameEvents.post(new ItemFishedEvent(hook.getPlayerOwner(), hook)).isCanceled()) {
            hook.discard();
            cir.setReturnValue(0);
        }
    }
}
