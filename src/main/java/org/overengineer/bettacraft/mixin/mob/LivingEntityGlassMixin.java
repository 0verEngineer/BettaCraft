package org.overengineer.bettacraft.mixin.mob;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.mobs.GlassMobs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * M31 part d: a mob in glass ({@link GlassMobs}) has no vanilla physics in its tank (the walker places it: no gravity, no
 * pushing by the tank's water) and does not drown there (the air above the water is abstracted, like for the terrarium
 * animals). Only on the server; the client still animates its legs from the movement.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityGlassMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void bettacraft$glassTravel(Vec3 input, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (GlassMobs.held(self) || org.overengineer.bettacraft.foreign.ForeignTerrarium.held(self)) { // M35: the land animals of other mods
            self.setDeltaMovement(Vec3.ZERO);
            ci.cancel();
        }
    }

    @Inject(method = "decreaseAirSupply", at = @At("HEAD"), cancellable = true)
    private void bettacraft$glassAir(int air, CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (GlassMobs.held(self) || org.overengineer.bettacraft.foreign.ForeignTerrarium.held(self)) cir.setReturnValue(air);
    }
}
