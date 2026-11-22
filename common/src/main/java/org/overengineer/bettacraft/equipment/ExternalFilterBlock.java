package org.overengineer.bettacraft.equipment;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.tank.TankBlock;

/**
 * External filter (PLAN 6.2): a canister next to a tank. {@link #FACING} points at the tank block it is attached to;
 * a hose and an outlet pipe with a water jet go over the glass into the tank (drawn by the client renderer).
 * Two heater slots; right-click with a heater puts it in, right-click with an empty hand opens the slots.
 */
public class ExternalFilterBlock extends BaseEntityBlock {
    public static final MapCodec<ExternalFilterBlock> CODEC = simpleCodec(ExternalFilterBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Shapes.or(box(3, 0, 3, 13, 12, 13), box(4, 12, 4, 12, 14, 12));

    public ExternalFilterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** True if the block in direction {@code d} is a tank block. */
    static boolean tankAt(BlockGetter level, BlockPos pos, Direction d) {
        return level.getBlockState(pos.relative(d)).getBlock() instanceof TankBlock;
    }

    /** Direction to an adjacent tank block: the preferred one if it is a tank, else the first horizontal one that is. */
    @Nullable
    static Direction findTank(BlockGetter level, BlockPos pos, @Nullable Direction preferred) {
        if (preferred != null && preferred.getAxis().isHorizontal() && tankAt(level, pos, preferred)) return preferred;
        for (Direction d : Direction.Plane.HORIZONTAL) if (tankAt(level, pos, d)) return d;
        return null;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // clicked on the side of a tank: the tank is behind the clicked face
        Direction preferred = ctx.getClickedFace().getAxis().isHorizontal() ? ctx.getClickedFace().getOpposite() : ctx.getHorizontalDirection();
        Direction d = findTank(ctx.getLevel(), ctx.getClickedPos(), preferred);
        return defaultBlockState().setValue(FACING, d != null ? d : ctx.getHorizontalDirection());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // follow the tank if the one it pointed at is gone but another one is next to it (M62: not a filter whose tank was chosen)
        if (level.getBlockEntity(pos) instanceof ExternalFilterBlockEntity be && be.link() != null) return state;
        Direction f = state.getValue(FACING);
        if (!tankAt(level, pos, f)) {
            Direction d = findTank(level, pos, null);
            if (d != null) return state.setValue(FACING, d);
        }
        return state;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExternalFilterBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!FilterHeaters.isHeater(stack)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof ExternalFilterBlockEntity be) {
            if (be.insertHeater(stack)) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.METAL_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.3F);
            } else {
                player.displayClientMessage(Component.translatable("message.bettacraft.filter.full"), true);
            }
        }
        return ItemInteractionResult.CONSUME;
    }

    /** M23: a fitted intake sponge comes back when the filter goes (the heaters stay in the item). */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ExternalFilterBlockEntity be && be.intakeSponge()) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(org.overengineer.bettacraft.registry.ModItems.INTAKE_SPONGE.get()));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** M62: a filter that finds no tank by itself says how to choose one. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player p && level.getBlockEntity(pos) instanceof ExternalFilterBlockEntity be && FilterLinks.unlinked(level, be)) {
            p.displayClientMessage(Component.translatable("message.bettacraft.filter_link.hint").withStyle(net.minecraft.ChatFormatting.GOLD), true);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // M62: sneak + right-click with an empty hand: choose the tank
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide && player instanceof ServerPlayer sp) FilterLinks.open(sp, pos);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof ExternalFilterBlockEntity be) {
            org.overengineer.bettacraft.platform.Platform.get().openMenu(sp, be, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
