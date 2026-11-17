package org.overengineer.bettacraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * M52b: {@code BlockBehaviour#onRemove} of 1.21.1, which Minecraft 1.21.5 split into {@code affectNeighborsAfterRemoval}
 * (server, only when the block changes and neighbours are updated, after the block entity is gone) and
 * {@code BlockEntity#preRemoveSideEffects}. Our blocks rely on the old contract: called on the server for every state
 * change, whatever the update flags, while the block entity is still there (drop the contents, tell the tank its
 * equipment changed). {@code LevelChunkRemovalMixin} calls it at the same point of {@code LevelChunk#setBlockState} as
 * 1.21.1 did; vanilla removes the block entity afterwards, like the old default {@code onRemove}.
 */
public interface BlockRemoval {
    void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston);
}
