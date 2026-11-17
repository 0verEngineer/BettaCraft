package org.overengineer.bettacraft.mixin.interior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The interior is dry (PLAN 7.3): seagrass and kelp always report a water source as their fluid. In the interior
 * dimension that implicit water must not spread out of the plant. Real water blocks (buckets) still flow normally.
 */
@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {
    @Inject(method = "spread", at = @At("HEAD"), cancellable = true)
    private void bettacraft$dryInterior(net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState blockState, FluidState state, CallbackInfo ci) {
        if (InteriorManager.isInterior(level) && !(level.getBlockState(pos).getBlock() instanceof LiquidBlock)) ci.cancel();
    }
}
