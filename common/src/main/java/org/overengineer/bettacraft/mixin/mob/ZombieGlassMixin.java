package org.overengineer.bettacraft.mixin.mob;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.overengineer.bettacraft.mobs.GlassMobs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * M31 part d: in a tank every block with water is vanilla water, also under the land of a paludarium, so the vanilla
 * conversion of a zombie in glass is off; {@link GlassMobs} converts it when it is really under the tank's water line.
 */
@Mixin({Zombie.class, Husk.class})
public abstract class ZombieGlassMixin {
    @Inject(method = "convertsInWater", at = @At("HEAD"), cancellable = true)
    private void bettacraft$glassConversion(CallbackInfoReturnable<Boolean> cir) {
        if (GlassMobs.held((Entity) (Object) this)) cir.setReturnValue(false);
    }
}
