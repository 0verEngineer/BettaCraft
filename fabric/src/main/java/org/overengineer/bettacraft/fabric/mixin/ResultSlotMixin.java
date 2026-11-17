package org.overengineer.bettacraft.fabric.mixin;

import org.overengineer.bettacraft.fabric.FabricGameEvents;
import org.overengineer.bettacraft.platform.event.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52: fires the common game event NeoForge fires here (Fabric API has no callback for it). */
@Mixin(net.minecraft.world.inventory.ResultSlot.class)
public abstract class ResultSlotMixin {

    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private net.minecraft.world.entity.player.Player player;

    @Inject(method = "checkTakeAchievements", at = @At("HEAD"))
    private void bettacraft$crafted(net.minecraft.world.item.ItemStack stack, CallbackInfo ci) {
        FabricGameEvents.post(new PlayerEvent.ItemCraftedEvent(player, stack));
    }
}
