package org.overengineer.bettacraft.plumbing;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.overengineer.bettacraft.registry.ModBlockEntities;

/**
 * M24: the water devices around a tank: the reservoir (16 buckets of water for the top-off; buckets and pipes fill it),
 * the level sensor (tops off evaporated water) and the water change controller (changes a share of the water at an
 * interval). The sensor and the controller sit next to a tank (or the sensor at a multiblock filter); pipes join all three.
 */
public class WaterDeviceBlock extends BaseEntityBlock {
    public enum Kind { RESERVOIR, LEVEL_SENSOR, WATER_CHANGER }

    public static final MapCodec<WaterDeviceBlock> CODEC = simpleCodec(p -> new WaterDeviceBlock(Kind.RESERVOIR, p));
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Reservoir: how full it is (0..8). */
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 8);
    private static final VoxelShape SENSOR = box(4, 0, 4, 12, 10, 12), CHANGER = box(2, 0, 2, 14, 12, 14);
    private final Kind kind;

    public WaterDeviceBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FILL, 0));
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
        b.add(FACING, FILL);
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
            case WATER_CHANGER -> CHANGER;
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (kind == Kind.RESERVOIR && FluidUtil.getFluidHandler(stack).isPresent()) {
            return FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())
                    ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        switch (kind) {
            case WATER_CHANGER -> {
                if (level.isClientSide) org.overengineer.bettacraft.util.ClientHooks.openWaterChanger(pos);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            case LEVEL_SENSOR -> {
                if (!level.isClientSide && level.getBlockEntity(pos) instanceof LevelSensorBlockEntity s) s.cycleTarget(player);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            default -> {
                if (!level.isClientSide && level.getBlockEntity(pos) instanceof ReservoirBlockEntity r) r.tellFill(player);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        PipeNetwork.invalidate(level);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        PipeNetwork.invalidate(level);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return kind == Kind.RESERVOIR;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
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
        if (level.isClientSide) return null;
        return switch (kind) {
            case RESERVOIR -> null;
            case LEVEL_SENSOR -> createTickerHelper(type, ModBlockEntities.LEVEL_SENSOR.get(), LevelSensorBlockEntity::serverTick);
            case WATER_CHANGER -> createTickerHelper(type, ModBlockEntities.WATER_CHANGER.get(), WaterChangerBlockEntity::serverTick);
        };
    }
}
