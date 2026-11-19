package org.overengineer.bettacraft.osmosis;

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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.registry.ModBlockEntities;

/**
 * M25: the reverse osmosis unit. Water comes in through pipes, buckets or a water source block next to it; the
 * prefilter and the membrane make reverse osmosis water ({@code bettacraft:ro_water}, nothing dissolved) out of it, one
 * part for {@code osmosis.roWastePerPart} parts of waste water (it leaves through a pipe or a tank of another mod below,
 * otherwise down the drain). It simply runs without power (slowly); with a power option on and FE its pressure pump
 * makes it faster. Membrane and prefilter wear out and are replaced (right-click with a new one). Pipes and buckets take
 * the reverse osmosis water out. Empty hand: what it does; sneaking: take the worn parts out.
 */
public class RoUnitBlock extends BaseEntityBlock implements BlockRemoval {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape SHAPE = box(1, 0, 3, 15, 14, 13), SHAPE_X = box(3, 0, 1, 13, 14, 15);

    public RoUnitBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, ACTIVE);
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
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE : SHAPE_X;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RoUnitBlockEntity be) || !be.accepts(stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) be.use(player, hand, stack);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RoUnitBlockEntity be) {
            if (player.isShiftKeyDown()) be.takeParts(player);
            else be.tell(player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        org.overengineer.bettacraft.plumbing.PipeNetwork.invalidate(level);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RoUnitBlockEntity be) {
            for (int i = 0; i < be.parts().getSlots(); i++) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.parts().getStackInSlot(i).copy());
                be.parts().setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        org.overengineer.bettacraft.plumbing.PipeNetwork.invalidate(level);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RoUnitBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.RO_UNIT.get(), RoUnitBlockEntity::serverTick);
    }
}
