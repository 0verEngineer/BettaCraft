package org.overengineer.bettacraft.fabric.mixin;

import org.overengineer.bettacraft.fabric.FabricGameEvents;
import org.overengineer.bettacraft.platform.event.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52: fires the common game event NeoForge fires here (Fabric API has no callback for it). */
@Mixin(net.minecraft.world.entity.npc.AbstractVillager.class)
public abstract class VillagerTradeMixin {

    @Inject(method = "notifyTrade", at = @At("HEAD"))
    private void bettacraft$trade(net.minecraft.world.item.trading.MerchantOffer offer, CallbackInfo ci) {
        var self = (net.minecraft.world.entity.npc.AbstractVillager) (Object) this;
        if (self.getTradingPlayer() != null) FabricGameEvents.post(new TradeWithVillagerEvent(self.getTradingPlayer(), self));
    }
}
