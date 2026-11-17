package org.overengineer.bettacraft.fabric.mixin;

import org.overengineer.bettacraft.fabric.FabricGameEvents;
import org.overengineer.bettacraft.platform.event.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52: fires the common game event NeoForge fires here (Fabric API has no callback for it). */
@Mixin(net.minecraft.world.entity.player.Player.class)
public abstract class PlayerTickMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void bettacraft$tick(CallbackInfo ci) {
        if (FabricGameEvents.listens(PlayerTickEvent.Post.class)) FabricGameEvents.post(new PlayerTickEvent.Post((net.minecraft.world.entity.player.Player) (Object) this));
    }
}
