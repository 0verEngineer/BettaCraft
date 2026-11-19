package org.overengineer.bettacraft.fabric.mixin;

import org.overengineer.bettacraft.fabric.FabricGameEvents;
import org.overengineer.bettacraft.platform.event.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52: fires the common game event NeoForge fires here (Fabric API has no callback for it). */
@Mixin(net.minecraft.world.entity.LivingEntity.class)
public abstract class LivingDropsMixin {

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void bettacraft$drops(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, CallbackInfo ci) {
        if (FabricGameEvents.post(new LivingDropsEvent((net.minecraft.world.entity.LivingEntity) (Object) this)).isCanceled()) ci.cancel();
    }

    @Inject(method = "dropExperience", at = @At("HEAD"), cancellable = true)
    private void bettacraft$xp(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity attacker, CallbackInfo ci) {
        var e = FabricGameEvents.post(new LivingExperienceDropEvent((net.minecraft.world.entity.LivingEntity) (Object) this, 1));
        if (e.isCanceled() || e.getDroppedExperience() <= 0) ci.cancel();
    }

    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true)
    private float bettacraft$damage(float amount, net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float original) {
        if (!FabricGameEvents.listens(LivingDamageEvent.Pre.class)) return amount;
        return FabricGameEvents.post(new LivingDamageEvent.Pre((net.minecraft.world.entity.LivingEntity) (Object) this, source, amount)).getNewDamage();
    }
}
