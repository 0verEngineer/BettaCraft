package org.overengineer.bettacraft.mixin.interior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Floating plants (PLAN 5.4 / 7.3): in the dry interior a lily pad floats wherever it is placed (it marks a spot of
 * the water surface for the bubble nest), so it survives without water below.
 */
@Mixin(WaterlilyBlock.class)
public abstract class WaterlilyBlockMixin {
    @Inject(method = "mayPlaceOn", at = @At("HEAD"), cancellable = true)
    private void bettacraft$floatInterior(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof Level l && InteriorManager.isInterior(l)) cir.setReturnValue(true);
    }
}
