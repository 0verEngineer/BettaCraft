package org.overengineer.bettacraft.mixin.mob;

import net.minecraft.world.entity.Mob;
import org.overengineer.bettacraft.mobs.GlassMobs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * M31 part d: a mob in glass ({@link GlassMobs}): in its tank the walker moves it instead of the vanilla AI (no goals, no
 * targets, no navigation), and the undead do not burn in daylight there (unless {@code terrarium.mobUndeadBurn} under an
 * open top).
 */
@Mixin(Mob.class)
public abstract class MobGlassMixin {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void bettacraft$glassAi(CallbackInfo ci) {
        if (GlassMobs.aiStep((Mob) (Object) this)) ci.cancel();
    }

    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void bettacraft$glassSun(CallbackInfoReturnable<Boolean> cir) {
        Mob m = (Mob) (Object) this;
        if (GlassMobs.inTank(m) && !GlassMobs.mayBurn(m)) cir.setReturnValue(false);
    }
}
