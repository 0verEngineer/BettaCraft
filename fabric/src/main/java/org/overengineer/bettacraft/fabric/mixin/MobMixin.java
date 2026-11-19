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

    // M52b (1.21.4+): the conversion with its parameters and spawn reason (the short overload delegates to it)
    @Inject(method = "convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;",
            at = @At("HEAD"), cancellable = true)
    private <T extends net.minecraft.world.entity.Mob> void bettacraft$convert(net.minecraft.world.entity.EntityType<T> type, net.minecraft.world.entity.ConversionParams params,
            net.minecraft.world.entity.EntitySpawnReason reason, net.minecraft.world.entity.ConversionParams.AfterConversion<T> after, CallbackInfoReturnable<T> cir) {
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
