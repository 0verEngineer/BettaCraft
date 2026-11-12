package org.overengineer.bettacraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.overengineer.bettacraft.tank.TankBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Suppresses vanilla water ambient particles, sounds and drips for water inside tank blocks (PLAN 3.3):
 * in {@code doAnimateTick} the fluid state of tank blocks is reported as empty. Nothing else is affected.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @WrapOperation(method = "doAnimateTick", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
    private FluidState bettacraft$hideTankWater(ClientLevel level, BlockPos pos, Operation<FluidState> original) {
        if (level.getBlockState(pos).getBlock() instanceof TankBlock) {
            return Fluids.EMPTY.defaultFluidState();
        }
        return original.call(level, pos);
    }
}
