package org.overengineer.bettacraft.monitor;

import org.overengineer.bettacraft.util.BlockRemoval;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.overengineer.bettacraft.registry.ModBlockEntities;

/**
 * M27: a monitor block. Its screen faces {@link #FACING} (towards the player who placed it, or the way of the monitor it was
 * placed against). Monitor blocks with the same facing next to each other in one plane join into screens of up to
 * {@link MonitorScreens#MAX_W} x {@link MonitorScreens#MAX_H} blocks ({@link MonitorScreens}); the four flags say on which
 * sides (as the viewer sees it) the block continues into the same screen, so the frame is only drawn around the screen.
 */
public class MonitorBlock extends BaseEntityBlock implements BlockRemoval {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");

    public MonitorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(UP, false).setValue(DOWN, false).setValue(LEFT, false).setValue(RIGHT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, UP, DOWN, LEFT, RIGHT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        // placed against the side, top or bottom of a monitor: continue its screen
        BlockState against = ctx.getLevel().getBlockState(ctx.getClickedPos().relative(ctx.getClickedFace().getOpposite()));
        if (against.getBlock() instanceof MonitorBlock && ctx.getClickedFace().getAxis() != against.getValue(FACING).getAxis()) {
            facing = against.getValue(FACING);
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (level instanceof ServerLevel server && !oldState.is(this)) MonitorScreens.reform(server, java.util.List.of(pos), state.getValue(FACING), null);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (newState.is(this) || !(level instanceof ServerLevel server)) {
            return;
        }
        MonitorScreens.Old old = level.getBlockEntity(pos) instanceof MonitorBlockEntity be && be.isController() ? MonitorScreens.Old.of(be) : null;
        Direction facing = state.getValue(FACING);
        Direction right = MonitorScreens.right(facing);
        java.util.List<BlockPos> seeds = java.util.List.of(pos.above(), pos.below(), pos.relative(right), pos.relative(right.getOpposite()));
        MonitorScreens.reform(server, seeds, facing, old);
    }

    // ------------------------------------------------------------------ part b: redstone from the alarms

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /** 15 while an alarm tile with redstone of the screen is triggered (every block of the screen, every side). */
    @Override
    protected int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction side) {
        return level.getBlockEntity(pos) instanceof MonitorBlockEntity be && be.alarms() > 0 ? 15 : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Comparator: the number of triggered alarm tiles with redstone (at most 15). */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof MonitorBlockEntity be ? Math.min(15, be.alarms()) : 0;
    }

    /** Right-click: the dashboard editor (needs build rights, the server checks again); sneaking: the info line. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof MonitorBlockEntity be)) return InteractionResult.PASS;
        if (!player.isShiftKeyDown() && player.mayBuild()) {
            if (level.isClientSide()) org.overengineer.bettacraft.util.ClientHooks.openMonitor(pos);
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            MonitorBlockEntity c = be.controller();
            if (c == null) c = be;
            Component status = Component.translatable("monitor.bettacraft.status." + switch (c.status()) {
                case MonitorBlockEntity.NO_POWER -> "no_power";
                case MonitorBlockEntity.OFF -> "off";
                default -> "on";
            });
            player.sendOverlayMessage(Component.translatable("message.bettacraft.monitor.info", c.width(), c.height(),
                    c.tanks().size(), MonitorSync.range(), status).withStyle(ChatFormatting.AQUA));
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MonitorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.MONITOR.get(), MonitorBlockEntity::serverTick);
    }
}
