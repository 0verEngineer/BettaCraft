package org.overengineer.bettacraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.overengineer.bettacraft.util.BlockRemoval;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * M52b: calls {@link BlockRemoval#onRemove} of the replaced block on the server, right after the chunk section took the
 * new state and while the old block entity still exists (the place and contract of 1.21.1's {@code onRemove}).
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkRemovalMixin {
    @Shadow @Final Level level;

    @WrapOperation(method = "setBlockState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState bettacraft$legacyOnRemove(LevelChunkSection section, int x, int y, int z, BlockState state, Operation<BlockState> original,
            BlockPos pos, BlockState newState, int flags) {
        BlockState old = original.call(section, x, y, z, state);
        if (old != state && old.getBlock() instanceof BlockRemoval r && !level.isClientSide()) {
            r.onRemove(old, level, pos, state, (flags & 64) != 0);
        }
        return old;
    }
}
