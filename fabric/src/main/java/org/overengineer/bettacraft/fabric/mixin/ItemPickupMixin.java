package org.overengineer.bettacraft.fabric.mixin;

import org.overengineer.bettacraft.fabric.FabricGameEvents;
import org.overengineer.bettacraft.platform.event.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52: fires the common game event NeoForge fires here (Fabric API has no callback for it). */
@Mixin(net.minecraft.world.entity.item.ItemEntity.class)
public abstract class ItemPickupMixin {

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void bettacraft$pickup(net.minecraft.world.entity.player.Player player, CallbackInfo ci) {
        if (FabricGameEvents.listens(ItemEntityPickupEvent.Pre.class)) FabricGameEvents.post(new ItemEntityPickupEvent.Pre(player, (net.minecraft.world.entity.item.ItemEntity) (Object) this));
    }
}
