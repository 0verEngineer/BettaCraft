package org.overengineer.bettacraft.reef;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.tank.TankBlock;

/**
 * M20: protein skimmer (hang-on): a tall reaction tube with a collection cup on top, hung next to a tank like the
 * external filter. {@link #FACING} points at the tank block it hangs on. In salt water it foams the waste out (an extra
 * filter for salt water, {@link TankReef#skimmerBlocks}); in fresh water there is no foam and it does nothing.
 */
public class ProteinSkimmerBlock extends Block {
    public static final MapCodec<ProteinSkimmerBlock> CODEC = simpleCodec(ProteinSkimmerBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape[] SHAPES = new VoxelShape[4];

    static {
        // model: tube 5..11 x 0..12 x 2..8 (north = towards the tank), cup 4..12 x 12..16 x 1..9, hook over the glass
        for (Direction d : Direction.Plane.HORIZONTAL) {
            SHAPES[d.get2DDataValue()] = rotate(Shapes.or(box(5, 0, 2, 11, 12, 8), box(4, 12, 1, 12, 16, 9), box(6, 13, 0, 10, 16, 1)), d);
        }
    }

    public ProteinSkimmerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Rotates a shape modelled for {@code NORTH} to face {@code d}. */
    private static VoxelShape rotate(VoxelShape north, Direction d) {
        VoxelShape[] out = {Shapes.empty()};
        int turns = switch (d) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        north.forAllBoxes((x0, y0, z0, x1, y1, z1) -> {
            double ax0 = x0, az0 = z0, ax1 = x1, az1 = z1;
            for (int i = 0; i < turns; i++) {
                double nx0 = 1 - az1, nz0 = ax0, nx1 = 1 - az0, nz1 = ax1;
                ax0 = nx0; az0 = nz0; ax1 = nx1; az1 = nz1;
            }
            out[0] = Shapes.or(out[0], Shapes.box(ax0, y0, az0, ax1, y1, az1));
        });
        return out[0];
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get2DDataValue()];
    }

    static boolean tankAt(BlockGetter level, BlockPos pos, Direction d) {
        return level.getBlockState(pos.relative(d)).getBlock() instanceof TankBlock;
    }

    @Nullable
    static Direction findTank(BlockGetter level, BlockPos pos, @Nullable Direction preferred) {
        if (preferred != null && preferred.getAxis().isHorizontal() && tankAt(level, pos, preferred)) return preferred;
        for (Direction d : Direction.Plane.HORIZONTAL) if (tankAt(level, pos, d)) return d;
        return null;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction preferred = ctx.getClickedFace().getAxis().isHorizontal() ? ctx.getClickedFace().getOpposite() : ctx.getHorizontalDirection();
        Direction d = findTank(ctx.getLevel(), ctx.getClickedPos(), preferred);
        return defaultBlockState().setValue(FACING, d != null ? d : ctx.getHorizontalDirection());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
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
}
