package org.overengineer.bettacraft.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

/**
 * M52: a block whose picked item depends on where it was hit and on its block entity (NeoForge calls getCloneItemStack
 * with these arguments; Fabric's glue from the client's pick block).
 */
public interface PickableBlock {
    ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player);
}
