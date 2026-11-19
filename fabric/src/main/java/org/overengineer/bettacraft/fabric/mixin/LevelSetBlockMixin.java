package org.overengineer.bettacraft.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.overengineer.bettacraft.tank.interior.InteriorSync;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** M52: the interior sync learns of every block change (NeoForge: in its markAndNotifyBlock, see LevelNotifyMixin). */
@Mixin(Level.class)
public abstract class LevelSetBlockMixin {
    @WrapOperation(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState bettacraft$interiorChanged(LevelChunk chunk, BlockPos pos, BlockState state, int flags, Operation<BlockState> op) {
        BlockState old = op.call(chunk, pos, state, flags);
        if (old != null && (Object) this instanceof ServerLevel server) InteriorSync.onBlockUpdated(server, pos, old, chunk.getBlockState(pos));
        return old;
    }
}
