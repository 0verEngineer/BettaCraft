package org.overengineer.bettacraft.mixin.mob;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.player.Player;
import org.overengineer.bettacraft.mobs.GlassMobs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** M31 part d: a small magma cube (or slime) in glass hurts nobody who touches it ({@link GlassMobs}). */
@Mixin(AbstractCubeMob.class) // M52d: 26.2 moved touching and pushing into the cube mobs' base (slime, magma cube, sulfur cube)
public abstract class SlimeGlassMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void bettacraft$glassTouch(Player player, CallbackInfo ci) {
        if (GlassMobs.held((AbstractCubeMob) (Object) this)) ci.cancel();
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void bettacraft$glassPush(Entity other, CallbackInfo ci) {
        if (GlassMobs.held((AbstractCubeMob) (Object) this)) ci.cancel();
    }
}
