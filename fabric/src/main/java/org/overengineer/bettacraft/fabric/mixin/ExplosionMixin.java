package org.overengineer.bettacraft.fabric.mixin;

import org.overengineer.bettacraft.fabric.FabricGameEvents;
import org.overengineer.bettacraft.platform.event.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52: fires the common game event NeoForge fires here (Fabric API has no callback for it). */
@Mixin(net.minecraft.world.level.Explosion.class)
public abstract class ExplosionMixin {

    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private net.minecraft.world.level.Level level;
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private it.unimi.dsi.fastutil.objects.ObjectArrayList<net.minecraft.core.BlockPos> toBlow;

    @Inject(method = "explode", at = @At("TAIL"))
    private void bettacraft$detonate(CallbackInfo ci) {
        FabricGameEvents.post(new ExplosionEvent.Detonate(level, toBlow));
    }
}
