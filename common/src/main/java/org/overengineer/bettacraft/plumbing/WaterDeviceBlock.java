package org.overengineer.bettacraft.plumbing;

import org.overengineer.bettacraft.util.BlockRemoval;
import com.mojang.serialization.MapCodec;
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
import org.overengineer.bettacraft.platform.fluid.FluidUtil;
import org.overengineer.bettacraft.registry.ModBlockEntities;

/**
 * M24: the water devices around a tank: the reservoir (16 buckets of water; buckets and pipes fill it), the level sensor and the
 * water change controller.
 * <p>
 * M68 part B: the level sensor only measures (it sits at a tank, reports when the water is below its target and gives a redstone
 * signal then, {@link #POWERED}); no pipe joins it. The water change controller has four pipe ports ({@link PipePort}, see
 * {@link #changerPort}): fresh water in (left, seen from the front), fresh water out to the tanks (back), waste water in from the
 * tanks (top) and waste water out (right). Placed against a tank or a multiblock filter the touch stands for both tank-side ports.
 */
public class WaterDeviceBlock extends BaseEntityBlock implements BlockRemoval, PipePort {
    public enum Kind { RESERVOIR, LEVEL_SENSOR, WATER_CHANGER }

    public static final MapCodec<WaterDeviceBlock> CODEC = simpleCodec(p -> new WaterDeviceBlock(Kind.RESERVOIR, p));
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Reservoir: how full it is (0..8). */
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 8);
    /** M68 part B: level sensor: the water is below its target (redstone signal 15). */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape SENSOR = box(4, 0, 4, 12, 10, 12);
    /** M68 part B: the body and the four port stubs of the controller (fresh in east, waste out west, fresh out south, waste in up, facing north). */
    private static final java.util.Map<Direction, VoxelShape> CHANGER = new java.util.EnumMap<>(Direction.class);

    static {
        for (Direction f : Direction.Plane.HORIZONTAL) {
            VoxelShape s = box(2, 0, 2, 14, 13, 14);
            s = net.minecraft.world.phys.shapes.Shapes.or(s, box(5, 13, 5, 11, 16, 11)); // waste in, on top
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (changerPort(f, side) == null) continue;
                s = net.minecraft.world.phys.shapes.Shapes.or(s, switch (side) {
                    case NORTH -> box(5, 5, 0, 11, 11, 2);
                    case SOUTH -> box(5, 5, 14, 11, 11, 16);
                    case WEST -> box(0, 5, 5, 2, 11, 11);
                    default -> box(14, 5, 5, 16, 11, 11);
                });
            }
            CHANGER.put(f, s);
        }
    }
    private final Kind kind;

    public WaterDeviceBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FILL, 0).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, FILL, POWERED);
    }

    /**
     * M68 part B: the port of the water change controller on {@code face} when it faces {@code facing} (pure): fresh water in on the
     * left (seen from the front), out to the tanks at the back, waste water in from the tanks on top, out on the right; none at the
     * front (its panel) and below.
     */
    @Nullable
    public static PipeRole changerPort(Direction facing, Direction face) {
        if (face == Direction.UP) return PipeRole.WASTE_IN;
        if (face == facing.getOpposite()) return PipeRole.FRESH_OUT;
        if (face == facing.getClockWise()) return PipeRole.FRESH_IN;
        if (face == facing.getCounterClockWise()) return PipeRole.WASTE_OUT;
        return null;
    }

    /** M68 part B: the face of the controller's port {@code role} when it faces {@code facing} (pure). */
    public static Direction changerFace(Direction facing, PipeRole role) {
        return switch (role) {
            case WASTE_IN -> Direction.UP;
            case FRESH_OUT -> facing.getOpposite();
            case FRESH_IN -> facing.getClockWise();
            case WASTE_OUT -> facing.getCounterClockWise();
            default -> throw new IllegalArgumentException("no controller port: " + role);
        };
    }

    @Nullable
    @Override
    public PipeRole pipeRole(BlockGetter level, BlockPos pos, BlockState state, Direction face) {
        return kind == Kind.WATER_CHANGER && state.hasProperty(FACING) ? changerPort(state.getValue(FACING), face) : null;
    }

    /** M68 part B: the fresh water inlet takes what is pushed into its line, the waste water outlet gives what is pulled. */
    @Override
    public boolean pipeEnd(BlockState state, Direction face) {
        PipeRole r = pipeRole(null, null, state, face);
        return r == PipeRole.FRESH_IN || r == PipeRole.WASTE_OUT;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return kind == Kind.LEVEL_SENSOR;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return kind == Kind.LEVEL_SENSOR && state.getValue(POWERED) ? 15 : 0;
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (kind) {
            case RESERVOIR -> super.getShape(state, level, pos, ctx);
            case LEVEL_SENSOR -> SENSOR;
            case WATER_CHANGER -> CHANGER.getOrDefault(state.getValue(FACING), SENSOR);
        };
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (kind == Kind.RESERVOIR && FluidUtil.getFluidHandler(stack).isPresent()) {
            return FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())
                    ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        switch (kind) {
            case WATER_CHANGER -> {
                if (level.isClientSide()) org.overengineer.bettacraft.util.ClientHooks.openWaterChanger(pos);
                return InteractionResult.SUCCESS;
            }
            case LEVEL_SENSOR -> {
                if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LevelSensorBlockEntity s) s.cycleTarget(player);
                return InteractionResult.SUCCESS;
            }
            default -> {
                if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ReservoirBlockEntity r) r.tellFill(player);
                return InteractionResult.SUCCESS;
            }
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (old.is(this) && old.getValue(FACING) == state.getValue(FACING)) return; // the fill or the sensor's signal changed
        PipeNetwork.invalidate(level, pos); // M68 part B: only the lines here (the controller's ports give their roles)
        PipeUpdates.schedule(level, pos, false);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (newState.is(this) && newState.getValue(FACING) == state.getValue(FACING)) return;
        PipeNetwork.invalidate(level, pos);
        PipeUpdates.schedule(level, pos, false);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return kind == Kind.RESERVOIR;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof ReservoirBlockEntity r ? (int) Math.ceil(15.0 * r.water().amount() / r.water().capacity()) : 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case RESERVOIR -> new ReservoirBlockEntity(pos, state);
            case LEVEL_SENSOR -> new LevelSensorBlockEntity(pos, state);
            case WATER_CHANGER -> new WaterChangerBlockEntity(pos, state);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return switch (kind) {
            case RESERVOIR -> null;
            case LEVEL_SENSOR -> createTickerHelper(type, ModBlockEntities.LEVEL_SENSOR.get(), LevelSensorBlockEntity::serverTick);
            case WATER_CHANGER -> createTickerHelper(type, ModBlockEntities.WATER_CHANGER.get(), WaterChangerBlockEntity::serverTick);
        };
    }
}
