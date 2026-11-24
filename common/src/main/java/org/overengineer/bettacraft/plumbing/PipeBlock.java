package org.overengineer.bettacraft.plumbing;

import org.overengineer.bettacraft.util.BlockRemoval;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.LevelReader;
import net.minecraft.util.RandomSource;
import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * M24: a pipe for water (glass: cheap, carries {@code pipes.glassPipeRate}; stainless steel: {@code pipes.steelPipeRate}).
 * Pipes hold no water themselves: whatever pushes water into one (our water pump, the pump head of the multiblock filter,
 * the pumps and pipes of other mods) reaches every fluid handler at the ends of the network ({@link PipeNetwork}), and
 * whatever pulls from one gets water from them. Joins other pipes, every block with a fluid handler on that face (our
 * tanks, the reservoir, cauldrons, other mods' tanks and pipes) and our pumps and filter connectors.
 */
public class PipeBlock extends Block implements BlockRemoval {
    public static final MapCodec<PipeBlock> CODEC = simpleCodec(p -> new PipeBlock(false, p));
    public static final Map<Direction, BooleanProperty> SIDES = new EnumMap<>(Map.of(
            Direction.NORTH, BlockStateProperties.NORTH, Direction.SOUTH, BlockStateProperties.SOUTH, Direction.EAST, BlockStateProperties.EAST,
            Direction.WEST, BlockStateProperties.WEST, Direction.UP, BlockStateProperties.UP, Direction.DOWN, BlockStateProperties.DOWN));
    // M65: the pipe 3 px thick (was 6); the T-piece collar 4.5 px (was 8)
    private static final VoxelShape CORE = box(6.5, 6.5, 6.5, 9.5, 9.5, 9.5);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Map.of(
            Direction.NORTH, box(6.5, 6.5, 0, 9.5, 9.5, 6.5), Direction.SOUTH, box(6.5, 6.5, 9.5, 9.5, 9.5, 16),
            Direction.WEST, box(0, 6.5, 6.5, 6.5, 9.5, 9.5), Direction.EAST, box(9.5, 6.5, 6.5, 16, 9.5, 9.5),
            Direction.DOWN, box(6.5, 0, 6.5, 9.5, 6.5, 9.5), Direction.UP, box(6.5, 9.5, 6.5, 9.5, 16, 9.5)));
    private final boolean steel;

    public PipeBlock(boolean steel, Properties properties) {
        super(properties);
        this.steel = steel;
        BlockState s = stateDefinition.any();
        for (BooleanProperty p : SIDES.values()) s = s.setValue(p, false);
        registerDefaultState(s);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public boolean steel() {
        return steel;
    }

    /** mB per tick a network with this pipe can carry. */
    public int rate() {
        return steel ? org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.PIPE_STEEL_RATE, 250)
                : org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.PIPE_GLASS_RATE, 50);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        SIDES.values().forEach(b::add);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return withConnections(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
    }

    BlockState withConnections(BlockState s, LevelAccessor level, BlockPos pos) {
        for (Direction d : Direction.values()) s = s.setValue(SIDES.get(d), connects(level, pos, d));
        return s;
    }

    /** Whether the pipe at {@code pos} joins whatever is on side {@code d}. */
    public static boolean connects(net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction d) {
        BlockPos n = pos.relative(d);
        BlockState ns = level.getBlockState(n);
        if (ns.getBlock() instanceof PipeBlock) return true;
        // M40 part b: a block of ours that pipes do not join on that face may still have a fluid handler there (the fresh and waste water ports)
        if (ns.getBlock() instanceof PipeConnectable c && c.pipeConnects(ns, d.getOpposite())) return true;
        if (ns.isAir() || !(level instanceof Level l)) return false;
        return org.overengineer.bettacraft.platform.Platform.get().fluidHandler(l, n, d.getOpposite()) != null;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos, Direction dir, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        if (level instanceof Level l) PipeNetwork.invalidate(l);
        return state.setValue(SIDES.get(dir), connects(level, pos, dir));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        PipeNetwork.invalidate(level);
        // tanks and other blocks with a fluid handler do not tell us when they appear: look once more after placing (also when
        // the state was set directly, e.g. by a structure or a command)
        if (!level.isClientSide()) {
            BlockState now = withConnections(state, level, pos);
            if (now != state) level.setBlock(pos, now, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        PipeNetwork.invalidate(level);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean moving) {
        super.neighborChanged(state, level, pos, block, orientation, moving);
        if (level.isClientSide()) return;
        PipeNetwork.invalidate(level);
        BlockState now = withConnections(state, level, pos);
        if (now != state) level.setBlock(pos, now, Block.UPDATE_CLIENTS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape s = CORE;
        for (var e : SIDES.entrySet()) if (state.getValue(e.getValue())) s = Shapes.or(s, ARMS.get(e.getKey()));
        return s;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }
}
