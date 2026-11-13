package org.overengineer.bettacraft.equipment;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModTags;

/**
 * M21.5: clip-on feeder, hung on a side face of a top tank block (FACING points away from the glass). Right-click with
 * food: fills its one slot; with an empty hand: 1 -> 2 -> 3 feedings a day; sneaking with an empty hand: takes the food out.
 */
public class ClipFeederBlock extends BaseEntityBlock {
    public static final MapCodec<ClipFeederBlock> CODEC = simpleCodec(ClipFeederBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** A small box at the top edge, against the glass behind it. */
    private static final VoxelShape NORTH = box(5, 7, 11, 11, 16, 16), SOUTH = box(5, 7, 0, 11, 16, 5),
            WEST = box(11, 7, 5, 16, 16, 11), EAST = box(0, 7, 5, 5, 16, 11);

    public ClipFeederBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> NORTH;
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** It hangs on a side face of a tank block of the top layer. */
    public static boolean canHangOn(LevelReader level, BlockPos tankPos) {
        return level.getBlockState(tankPos).is(ModBlocks.TANK.get()) && !level.getBlockState(tankPos.above()).is(ModBlocks.TANK.get());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction face = ctx.getClickedFace();
        Player player = ctx.getPlayer();
        if (!org.overengineer.bettacraft.config.CommonConfig.clipFeeder()) {
            if (player != null && !ctx.getLevel().isClientSide) {
                player.displayClientMessage(Component.translatable("message.bettacraft.clip_feeder.off").withStyle(ChatFormatting.RED), true);
            }
            return null;
        }
        if (face.getAxis().isHorizontal() && canHangOn(ctx.getLevel(), ctx.getClickedPos().relative(face.getOpposite()))) {
            return defaultBlockState().setValue(FACING, face);
        }
        if (player != null && !ctx.getLevel().isClientSide) {
            player.displayClientMessage(Component.translatable("message.bettacraft.clip_feeder.where").withStyle(ChatFormatting.RED), true);
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canHangOn(level, pos.relative(state.getValue(FACING).getOpposite()));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (!canSurvive(state, level, pos)) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return super.updateShape(state, dir, neighbour, level, pos, neighbourPos);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ClipFeederBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CLIP_FEEDER.get(), FeederBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModTags.BETTA_FOOD)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ClipFeederBlockEntity be) {
            ItemStack rest = be.items().insertItem(0, stack.copy(), false);
            int moved = stack.getCount() - rest.getCount();
            if (moved > 0 && !player.getAbilities().instabuild) stack.shrink(moved);
            player.displayClientMessage(moved > 0 ? status(be) : Component.translatable("message.bettacraft.clip_feeder.full").withStyle(ChatFormatting.RED), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ClipFeederBlockEntity be) {
            if (player.isShiftKeyDown()) {
                ItemStack out = be.items().extractItem(0, 64, false);
                if (!out.isEmpty() && !player.getInventory().add(out)) player.drop(out, false);
            } else {
                be.cycleFeedings();
            }
            player.displayClientMessage(status(be), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static Component status(ClipFeederBlockEntity be) {
        return Component.translatable("message.bettacraft.clip_feeder.status", be.foodCount(), be.feedingsPerDay()).withStyle(ChatFormatting.GREEN);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ClipFeederBlockEntity be) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.items().getStackInSlot(0));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
