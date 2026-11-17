package org.overengineer.bettacraft.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * M52: a block that removes itself when a player breaks it and may refuse (NeoForge calls onDestroyedByPlayer by this
 * name; Fabric's glue from the block breaking of the player's game mode, server and client).
 */
public interface PlayerDestroyable {
    /** Removes the block (calls playerWillDestroy itself); false: it stays. */
    boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid);
}
