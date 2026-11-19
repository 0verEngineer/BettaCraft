package org.overengineer.bettacraft.mixin.mob;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.player.Player;
import org.overengineer.bettacraft.mobs.GlassMobs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** M31 part d: a small magma cube (or slime) in glass hurts nobody who touches it ({@link GlassMobs}). */
@Mixin(Slime.class)
public abstract class SlimeGlassMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void bettacraft$glassTouch(Player player, CallbackInfo ci) {
        if (GlassMobs.held((Slime) (Object) this)) ci.cancel();
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void bettacraft$glassPush(Entity other, CallbackInfo ci) {
        if (GlassMobs.held((Slime) (Object) this)) ci.cancel();
    }
}
