package org.overengineer.bettacraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.overengineer.bettacraft.client.interior.PlantMeshRenderer;
import org.overengineer.bettacraft.tank.TankBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses vanilla water ambient particles, sounds and drips for water inside tank blocks (PLAN 3.3):
 * in {@code doAnimateTick} the fluid state of tank blocks is reported as empty. Nothing else is affected.
 * <p>
 * M21 follow-up: block changes and light packets of the client level tell {@link PlantMeshRenderer} which plant meshes
 * to rebuild or relight (instead of rebuilding every mesh every few seconds).
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

    @Inject(method = "sendBlockUpdated", at = @At("HEAD"))
    private void bettacraft$plantMeshBlockChanged(BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        PlantMeshRenderer.onBlockChanged((ClientLevel) (Object) this, pos, oldState, newState);
    }

    /** Light packets and chunks whose light arrived (the light engine of the client itself: {@code ClientChunkCacheMixin}). */
    @Inject(method = "setSectionDirtyWithNeighbors", at = @At("HEAD"), require = 0)
    private void bettacraft$plantMeshLight(int sx, int sy, int sz, CallbackInfo ci) {
        PlantMeshRenderer.onSectionLight(sx, sy, sz);
    }
}
