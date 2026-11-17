package org.overengineer.bettacraft.fabric.mixin;

import org.overengineer.bettacraft.fabric.FabricGameEvents;
import org.overengineer.bettacraft.platform.event.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52: fires the common game event NeoForge fires here (Fabric API has no callback for it). */
@Mixin(net.minecraft.world.level.Level.class)
public abstract class EntityTickMixin {

    @Inject(method = "guardEntityTick", at = @At("TAIL"))
    private <T extends net.minecraft.world.entity.Entity> void bettacraft$tick(java.util.function.Consumer<T> tick, T entity, CallbackInfo ci) {
        if (FabricGameEvents.listens(EntityTickEvent.Post.class)) FabricGameEvents.post(new EntityTickEvent.Post(entity));
    }
}
