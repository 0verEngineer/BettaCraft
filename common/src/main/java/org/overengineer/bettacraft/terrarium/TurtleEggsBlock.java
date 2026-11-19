package org.overengineer.bettacraft.terrarium;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * M31 part b: a clutch of turtle eggs half buried in sand or soil in the interior of a tank (the miniature shows it in
 * the tank). Only the turtles lay it ({@link HerpBreeding}); the incubation is the {@link HerpClutch} of the tank. It
 * has no item and drops nothing: a player who breaks it in the interior destroys the eggs. No collision, so the animals
 * and the height map of the interior walk over it. The model is larger than the block (an egg is about a fifth of a
 * grown musk turtle).
 */
public class TurtleEggsBlock extends Block {
    public static final IntegerProperty EGGS = IntegerProperty.create("eggs", 1, 4);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 8, 15);

    public TurtleEggsBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(EGGS, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(EGGS);
    }

    /** The state that shows this many eggs (at most four). */
    public BlockState withEggs(int eggs) {
        return defaultBlockState().setValue(EGGS, Math.max(1, Math.min(4, eggs)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }
}
