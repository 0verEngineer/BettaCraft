package org.overengineer.bettacraft.tank.interior;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Floor of a tank interior (PLAN 7.2). Unbreakable. A shift-right-click with a block sets only that block's look as
 * substrate on this floor block ({@link #SUBSTRATE} = true, the state is kept in an {@link InteriorFloorBlockEntity});
 * plain floor blocks have no block entity.
 */
public class InteriorFloorBlock extends BaseEntityBlock implements org.overengineer.bettacraft.util.BlockRemoval {
    public static final MapCodec<InteriorFloorBlock> CODEC = simpleCodec(InteriorFloorBlock::new);
    public static final BooleanProperty SUBSTRATE = BooleanProperty.create("substrate");

    public InteriorFloorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SUBSTRATE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SUBSTRATE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * M17: turning the substrate off keeps the same block, so vanilla keeps the block entity (with its paid substrate).
     * A merge or split then copied that stale substrate into the new region and it could be taken out a second time.
     */
    @Override
    public void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (newState.is(this) && !newState.getValue(SUBSTRATE)) level.removeBlockEntity(pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(SUBSTRATE) ? new InteriorFloorBlockEntity(pos, state) : null;
    }
}
