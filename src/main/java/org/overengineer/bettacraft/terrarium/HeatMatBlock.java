package org.overengineer.bettacraft.terrarium;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.tank.TankBlock;

/**
 * M31: heat mat, a thin heating foil stuck to the outside of a tank: under its floor ({@link #FACING} UP, against the
 * underside of a tank block) or on a wall (horizontal facing). {@link #FACING} points from the mat to the block it sticks
 * to. It warms the ground (and a part of the air) of the terrarium it touches while the air is below the tank's target
 * temperature ({@link TankClimate}); {@link #HEATING} shows that (a warm glow). Wall mats warm the floor half as much.
 * With {@code power.heaterRequiresEnergy} it uses the tank's FE like a heater.
 */
public class HeatMatBlock extends Block {
    public static final MapCodec<HeatMatBlock> CODEC = simpleCodec(HeatMatBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty HEATING = BooleanProperty.create("heating");
    private static final VoxelShape UP = box(1, 15, 1, 15, 16, 15), NORTH = box(1, 1, 0, 15, 15, 1), SOUTH = box(1, 1, 15, 15, 15, 16),
            WEST = box(0, 1, 1, 1, 15, 15), EAST = box(15, 1, 1, 16, 15, 15), DOWN = box(1, 0, 1, 15, 1, 15);

    public HeatMatBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(HEATING, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, HEATING);
    }

    /** The block the mat sticks to. */
    public static BlockPos attachedTo(BlockPos pos, BlockState state) {
        return pos.relative(state.getValue(FACING));
    }

    /** Whether the mat lies under the tank (warms the floor fully) rather than on a wall. */
    public static boolean underFloor(BlockState state) {
        return state.getValue(FACING) == Direction.UP;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // stuck onto the clicked face: under a block (clicked its underside) or on a wall; never lying on the floor
        Direction towards = ctx.getClickedFace().getOpposite();
        if (towards == Direction.DOWN) return null;
        BlockState s = defaultBlockState().setValue(FACING, towards);
        return canSurvive(s, ctx.getLevel(), ctx.getClickedPos()) ? s : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos on = attachedTo(pos, state);
        BlockState s = level.getBlockState(on);
        return s.getBlock() instanceof TankBlock || s.isFaceSturdy(level, on, state.getValue(FACING).getOpposite());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (dir == state.getValue(FACING) && !canSurvive(state, level, pos)) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return state;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide && !old.is(this)) TankClimate.equipmentChanged(level, attachedTo(pos, state));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if (!level.isClientSide && !newState.is(this)) TankClimate.equipmentChanged(level, attachedTo(pos, state));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case UP -> UP;
            case DOWN -> DOWN;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }
}
