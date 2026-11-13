package org.overengineer.bettacraft.tankmates;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** M19: one block of wool strands of a spawning mop (crossed planes like a plant, no collision). */
public class SpawningMopBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public SpawningMopBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
