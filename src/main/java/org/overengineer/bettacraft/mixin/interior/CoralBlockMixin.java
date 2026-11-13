package org.overengineer.bettacraft.mixin.interior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CoralBlock;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Coral blocks do not dry out in the interior dimension (PLAN 7.3). */
@Mixin(CoralBlock.class)
public abstract class CoralBlockMixin {
    @Inject(method = "scanForWater", at = @At("HEAD"), cancellable = true)
    private void bettacraft$wetInterior(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // M20: with saltwater.vanillaCoralsNeedSalt they only live in salt water tanks
        if (level instanceof Level l && InteriorManager.isInterior(l)) cir.setReturnValue(org.overengineer.bettacraft.reef.Corals.vanillaCoralLives(l, pos));
    }
}
