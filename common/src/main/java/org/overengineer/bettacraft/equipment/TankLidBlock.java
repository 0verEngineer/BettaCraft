package org.overengineer.bettacraft.equipment;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Glass lid (M9): a thin pane lying on a tank block. Every top block of a tank that carries a lid (or the feeder, a
 * light or any full block) does not lose water by evaporation. Purely a cover, no block entity.
 */
public class TankLidBlock extends Block {
    public static final MapCodec<TankLidBlock> CODEC = simpleCodec(TankLidBlock::new);
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 1, 16);

    public TankLidBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }
}
