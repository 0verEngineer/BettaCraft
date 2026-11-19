package org.overengineer.bettacraft.co2;

import org.overengineer.bettacraft.util.BlockRemoval;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.registry.ModBlockEntities;

/**
 * M25: the bio CO2 bottle. It stands next to a tank (or on it) and leads the CO2 of fermenting sugar into the water:
 * right-click with a water bucket, up to four sugar and then yeast; it ferments for some days ({@code co2Dosing.bioCo2Days}
 * with four sugar), weak and uneven (it starts slowly, varies and fades), day and night. Then it is spent and is set up
 * again (a water bucket replaces the old brew). Needs no power. Empty hand: what it does; sneaking: pour it out.
 */
public class BioCo2Block extends BaseEntityBlock implements BlockRemoval, Co2SourceBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** 0 empty, 1 water (and sugar), 2 fermenting, 3 spent. */
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);
    public static final int EMPTY = 0, MIXED = 1, FERMENTING = 2, SPENT = 3;
    private static final VoxelShape SHAPE = box(4.5, 0, 4.5, 11.5, 13, 11.5);

    public BioCo2Block(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STAGE, EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, STAGE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BioCo2BlockEntity be)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!be.accepts(stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) be.use(player, hand, stack);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BioCo2BlockEntity be) {
            if (player.isShiftKeyDown()) be.pourOut(player);
            else be.tell(player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide() && !old.is(this)) Co2SourceBlock.invalidateTank(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!level.isClientSide() && !newState.is(this)) Co2SourceBlock.invalidateTank(level, pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BioCo2BlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? createTickerHelper(type, ModBlockEntities.BIO_CO2.get(), BioCo2BlockEntity::clientTick)
                : createTickerHelper(type, ModBlockEntities.BIO_CO2.get(), BioCo2BlockEntity::serverTick);
    }
}
