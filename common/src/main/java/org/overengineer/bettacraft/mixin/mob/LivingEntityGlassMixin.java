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
 * animals). Only on the server; the client still animates its legs from the movement (M44: scaled to the tank).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityGlassMixin {
    @org.spongepowered.asm.mixin.Shadow
    protected abstract void updateWalkAnimation(float distance);

    /**
     * M44 (client): a mob standing in a tank block (the mobs in glass, the terrarium pets, the land animals of other mods)
     * moves its legs by its walk divided by its scale times the factor of {@code walkAnimationBoost} ({@link
     * org.overengineer.bettacraft.terrarium.WalkAnimation}); climbers count the climb. Our own animals (their models scale
     * the swing themselves), fish and players stay as they are; {@code updateWalkAnimation} keeps the frog's own formula.
     */
    @Inject(method = "calculateEntityAnimation", at = @At("HEAD"), cancellable = true)
    private void bettacraft$tankWalk(boolean includeHeight, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide || self instanceof net.minecraft.world.entity.player.Player
                || self instanceof net.minecraft.world.entity.animal.WaterAnimal) return;
        float boost = org.overengineer.bettacraft.config.ClientConfig.walkAnimationBoost();
        if (boost == 0) return;
        if (!(self.level().getBlockState(net.minecraft.core.BlockPos.containing(self.getX(), self.getY() + 0.05, self.getZ())).getBlock()
                instanceof org.overengineer.bettacraft.tank.TankBlock)) return;
        if (net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(self.getType()).getNamespace().equals(org.overengineer.bettacraft.BettaCraft.MOD_ID)) return;
        double h = Math.hypot(self.getX() - self.xo, self.getZ() - self.zo), v = self.getY() - self.yo;
        boolean climb = includeHeight || self instanceof net.minecraft.world.entity.monster.Spider
                || org.overengineer.bettacraft.terrarium.WalkAnimation.mostlyVertical(h, v);
        float scale = self.getScale();
        updateWalkAnimation(org.overengineer.bettacraft.terrarium.WalkAnimation.distance(h, v, climb, scale,
                org.overengineer.bettacraft.terrarium.WalkAnimation.factor(boost, scale)));
        ci.cancel();
    }
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
