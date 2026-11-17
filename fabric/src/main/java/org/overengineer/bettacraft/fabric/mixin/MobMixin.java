package org.overengineer.bettacraft.fabric.mixin;

import org.overengineer.bettacraft.fabric.FabricGameEvents;
import org.overengineer.bettacraft.platform.event.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52: fires the common game event NeoForge fires here (Fabric API has no callback for it). */
@Mixin(net.minecraft.world.entity.Mob.class)
public abstract class MobMixin {

    @Inject(method = "convertTo", at = @At("HEAD"), cancellable = true)
    private <T extends net.minecraft.world.entity.Mob> void bettacraft$convert(net.minecraft.world.entity.EntityType<T> type, boolean keep, CallbackInfoReturnable<T> cir) {
        if (FabricGameEvents.post(new LivingConversionEvent.Pre((net.minecraft.world.entity.LivingEntity) (Object) this, type)).isCanceled()) cir.setReturnValue(null);
    }

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void bettacraft$target(net.minecraft.world.entity.LivingEntity target, CallbackInfo ci) {
        if (FabricGameEvents.listens(LivingChangeTargetEvent.class)
                && FabricGameEvents.post(new LivingChangeTargetEvent((net.minecraft.world.entity.LivingEntity) (Object) this, target)).isCanceled()) ci.cancel();
    }

    @Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true)
    private void bettacraft$despawn(CallbackInfo ci) {
        var mob = (net.minecraft.world.entity.Mob) (Object) this;
        var e = FabricGameEvents.post(new MobDespawnEvent(mob));
        if (e.getResult() == MobDespawnEvent.DespawnResult.DENY) {
            mob.setNoActionTime(0);
            ci.cancel();
        } else if (e.getResult() == MobDespawnEvent.DespawnResult.ALLOW) {
            mob.discard();
            ci.cancel();
        }
    }
}
