package org.overengineer.bettacraft.fabric.mixin;

import org.overengineer.bettacraft.fabric.FabricGameEvents;
import org.overengineer.bettacraft.platform.event.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52: fires the common game event NeoForge fires here (Fabric API has no callback for it). */
@Mixin(net.minecraft.world.entity.animal.Animal.class)
public abstract class AnimalBreedMixin {

    @Inject(method = "spawnChildFromBreeding", at = @At("HEAD"), cancellable = true)
    private void bettacraft$breed(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.animal.Animal mate, CallbackInfo ci) {
        var self = (net.minecraft.world.entity.animal.Animal) (Object) this;
        if (FabricGameEvents.post(new BabyEntitySpawnEvent(self, mate)).isCanceled()) {
            // like NeoForge: the parents are no longer in love and wait as after breeding
            self.setAge(6000);
            mate.setAge(6000);
            self.resetLove();
            mate.resetLove();
            ci.cancel();
        }
    }
}
