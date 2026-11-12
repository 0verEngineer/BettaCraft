package org.overengineer.bettacraft.mixin.interior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseCoralPlantTypeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Coral plants and fans do not dry out in the interior dimension (PLAN 7.3). */
@Mixin(BaseCoralPlantTypeBlock.class)
public abstract class CoralMixin {
    @Inject(method = "scanForWater", at = @At("HEAD"), cancellable = true)
    private static void bettacraft$wetInterior(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof Level l && InteriorManager.isInterior(l)) cir.setReturnValue(true);
    }
}
