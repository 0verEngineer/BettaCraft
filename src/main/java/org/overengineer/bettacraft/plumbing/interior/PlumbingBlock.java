package org.overengineer.bettacraft.plumbing.interior;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.plumbing.PipeBlock;

/**
 * M24: a plumbing piece in a tank interior (scale 16, so one pixel thin in the miniature outside): the bulkhead's
 * connection on the floor ({@link Plumb#PORT}), interior pipes and the four outlets. Every piece joins every piece next to
 * it. Outlets have a facing (the direction of their current) and {@link #ACTIVE} while water flows through them (set by
 * {@link InteriorPlumbing#syncStates}); then they show their stream.
 */
public class PlumbingBlock extends Block {
    public static final MapCodec<PlumbingBlock> CODEC = simpleCodec(p -> new PlumbingBlock(Plumb.PIPE, p));
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape CORE = box(6, 6, 6, 10, 10, 10);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Map.of(
            Direction.NORTH, box(6, 6, 0, 10, 10, 6), Direction.SOUTH, box(6, 6, 10, 10, 10, 16),
            Direction.WEST, box(0, 6, 6, 6, 10, 10), Direction.EAST, box(10, 6, 6, 16, 10, 10),
            Direction.DOWN, box(6, 0, 6, 10, 6, 10), Direction.UP, box(6, 10, 6, 10, 16, 10)));
    private static final VoxelShape PORT_SHAPE = Shapes.or(box(3, 0, 3, 13, 2, 13), box(6, 2, 6, 10, 8, 10));
    private final Plumb kind;

    public PlumbingBlock(Plumb kind, Properties properties) {
        super(properties);
        this.kind = kind;
        BlockState s = stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false);
        for (BooleanProperty p : PipeBlock.SIDES.values()) s = s.setValue(p, false);
        registerDefaultState(s);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public Plumb kind() {
        return kind;
    }

    /** The code of a state in the equipment index (0 = no plumbing). */
    public static int code(BlockState s) {
        if (!(s.getBlock() instanceof PlumbingBlock b)) return 0;
        return Plumb.code(b.kind, b.kind.outlet() ? s.getValue(FACING).get2DDataValue() : 0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        // the kind is not known yet while the state definition is built: every piece gets every property, pipes use the sides
        PipeBlock.SIDES.values().forEach(b::add);
        b.add(FACING, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = defaultBlockState();
        if (kind == Plumb.PIPE) {
            for (Direction d : Direction.values()) s = s.setValue(PipeBlock.SIDES.get(d), joins(ctx.getLevel(), ctx.getClickedPos(), d));
        }
        // outlets point away from the player (into the tank) or along the wall they are placed against
        if (kind.outlet()) s = s.setValue(FACING, ctx.getHorizontalDirection());
        return s;
    }

    static boolean joins(LevelAccessor level, BlockPos pos, Direction d) {
        return level.getBlockState(pos.relative(d)).getBlock() instanceof PlumbingBlock;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (kind == Plumb.PIPE) return state.setValue(PipeBlock.SIDES.get(dir), neighbor.getBlock() instanceof PlumbingBlock);
        return state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (kind) {
            case PORT -> PORT_SHAPE;
            case PIPE -> {
                VoxelShape s = CORE;
                for (var e : PipeBlock.SIDES.entrySet()) if (state.getValue(e.getValue())) s = Shapes.or(s, ARMS.get(e.getKey()));
                yield s;
            }
            case STRAINER -> box(4, 0, 4, 12, 12, 12);
            case LILY -> box(4, 0, 4, 12, 16, 12);
            case NOZZLE -> box(5, 4, 5, 11, 12, 11);
            case SPRAY_BAR -> state.getValue(FACING).getAxis() == Direction.Axis.Z ? box(0, 6, 6, 16, 10, 10) : box(6, 6, 0, 10, 10, 16);
        };
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    /** The stream of a working outlet (in the interior). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!kind.outlet() || !state.getValue(ACTIVE)) return;
        Direction f = state.getValue(FACING);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        int n = kind == Plumb.SPRAY_BAR ? 6 : 3;
        for (int i = 0; i < n; i++) {
            if (kind.intake()) {
                // particles drawn into the strainer
                double ox = (r.nextDouble() - 0.5) * 3, oy = r.nextDouble() * 2, oz = (r.nextDouble() - 0.5) * 3;
                level.addParticle(new org.overengineer.bettacraft.registry.ModParticles.AirBubble((float) y + 3, 0.12F), x + ox, y + oy, z + oz, -ox * 0.12, 0.01, -oz * 0.12);
                continue;
            }
            double spread = kind == Plumb.SPRAY_BAR ? 0.9 : kind == Plumb.LILY ? 0.3 : 0.05;
            double sx = f.getAxis() == Direction.Axis.X ? 0 : (r.nextDouble() - 0.5) * 2 * spread;
            double sz = f.getAxis() == Direction.Axis.Z ? 0 : (r.nextDouble() - 0.5) * 2 * spread;
            double speed = kind == Plumb.NOZZLE ? 0.6 : 0.3;
            double py = kind == Plumb.LILY ? pos.getY() + 0.9 : y;
            level.addParticle(new org.overengineer.bettacraft.registry.ModParticles.AirBubble((float) py + 6, 0.12F), x + f.getStepX() * 0.5 + sx, py, z + f.getStepZ() * 0.5 + sz,
                    f.getStepX() * speed + (r.nextDouble() - 0.5) * 0.05, (r.nextDouble() - 0.5) * 0.05, f.getStepZ() * speed + (r.nextDouble() - 0.5) * 0.05);
        }
    }
}
