package org.overengineer.bettacraft.equipment;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Aquarium light bar (PLAN 6.4). Segments connect only along their long axis into one continuous bar. Light level
 * 15; a redstone signal on any segment switches the whole bar off. Placed on a tank / glass edge, or hanging from a
 * ceiling (placed against the underside of a block).
 */
public class TankLightBlock extends Block {
    public static final MapCodec<TankLightBlock> CODEC = simpleCodec(TankLightBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    /** Connected to a segment in the negative / positive axis direction. */
    public static final BooleanProperty NEG = BooleanProperty.create("neg");
    public static final BooleanProperty POS = BooleanProperty.create("pos");
    public static final BooleanProperty HANGING = BlockStateProperties.HANGING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final int MAX_BAR = 64;

    public TankLightBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(NEG, false).setValue(POS, false)
                .setValue(HANGING, false).setValue(LIT, true).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(AXIS, NEG, POS, HANGING, LIT, POWERED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        double y0 = state.getValue(HANGING) ? 14 : 0;
        double a0 = state.getValue(NEG) ? 0 : 1, a1 = state.getValue(POS) ? 16 : 15;
        return state.getValue(AXIS) == Direction.Axis.X ? box(a0, y0, 6.5, a1, y0 + 2, 9.5) : box(6.5, y0, a0, 9.5, y0 + 2, a1);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        boolean hanging = ctx.getClickedFace() == Direction.DOWN;
        Direction.Axis axis = ctx.getHorizontalDirection().getClockWise().getAxis();
        // join an existing bar next to it
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockState n = level.getBlockState(pos.relative(d));
            if (n.getBlock() instanceof TankLightBlock && n.getValue(AXIS) == d.getAxis() && n.getValue(HANGING) == hanging) {
                axis = d.getAxis();
                break;
            }
        }
        BlockState s = defaultBlockState().setValue(AXIS, axis).setValue(HANGING, hanging)
                .setValue(POWERED, level.hasNeighborSignal(pos));
        return connect(s, level, pos);
    }

    private BlockState connect(BlockState s, LevelAccessor level, BlockPos pos) {
        Direction.Axis axis = s.getValue(AXIS);
        Direction neg = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE);
        return s.setValue(NEG, joins(s, level.getBlockState(pos.relative(neg))))
                .setValue(POS, joins(s, level.getBlockState(pos.relative(neg.getOpposite()))));
    }

    private static boolean joins(BlockState self, BlockState other) {
        return other.getBlock() instanceof TankLightBlock && other.getValue(AXIS) == self.getValue(AXIS)
                && other.getValue(HANGING) == self.getValue(HANGING);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (dir.getAxis() == state.getValue(AXIS)) return connect(state, level, pos);
        return state;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide && !old.is(this)) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if (!level.isClientSide && !newState.is(this)) {
            Direction neg = Direction.fromAxisAndDirection(state.getValue(AXIS), Direction.AxisDirection.NEGATIVE);
            for (Direction d : new Direction[]{neg, neg.getOpposite()}) {
                BlockPos n = pos.relative(d);
                if (level.getBlockState(n).getBlock() instanceof TankLightBlock) level.scheduleTick(n, this, 1);
            }
        }
    }

    /** Recomputes LIT for the whole bar this segment belongs to (lit unless any segment is powered). */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        List<BlockPos> bar = bar(level, pos, state);
        boolean anyPowered = false;
        for (BlockPos p : bar) anyPowered |= level.getBlockState(p).getValue(POWERED);
        for (BlockPos p : bar) {
            BlockState s = level.getBlockState(p);
            if (s.getValue(LIT) == anyPowered) level.setBlock(p, s.setValue(LIT, !anyPowered), Block.UPDATE_CLIENTS);
        }
    }

    public static List<BlockPos> bar(Level level, BlockPos pos, BlockState state) {
        List<BlockPos> out = new ArrayList<>();
        out.add(pos);
        Direction neg = Direction.fromAxisAndDirection(state.getValue(AXIS), Direction.AxisDirection.NEGATIVE);
        for (Direction d : new Direction[]{neg, neg.getOpposite()}) {
            BlockPos p = pos;
            for (int i = 0; i < MAX_BAR; i++) {
                p = p.relative(d);
                if (!joins(state, level.getBlockState(p))) break;
                out.add(p);
            }
        }
        return out;
    }
}
