package org.overengineer.bettacraft.equipment;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import com.mojang.serialization.MapCodec;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * M23: high-power LED, the strongest light tier: a flat planted-tank light with a heat sink that joins with its
 * neighbours in all four directions into one fixture of any size (west/east = {@link #NEG}/{@link #POS}, north/south =
 * {@link #NORTH}/{@link #SOUTH}; {@link #AXIS} is unused and stays X). Gives light 15 plus {@code lights.highPowerBoost}
 * on the part of the tank it covers.
 */
public class HighPowerLedBlock extends TankLightBlock {
    public static final MapCodec<HighPowerLedBlock> CODEC = simpleCodec(HighPowerLedBlock::new);
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");

    public HighPowerLedBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(NORTH, false).setValue(SOUTH, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(NORTH, SOUTH);
    }

    @Override
    public double boost() {
        return Features.enabled(Feature.LED_TIERS)
                ? org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.LIGHT_HIGH_POWER_BOOST, 5.0) : 0;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        double y0 = state.getValue(HANGING) ? 13 : 0;
        return box(0, y0, 0, 16, y0 + 3, 16);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        boolean hanging = ctx.getClickedFace() == Direction.DOWN;
        BlockState s = defaultBlockState().setValue(AXIS, Direction.Axis.X).setValue(HANGING, hanging)
                .setValue(POWERED, ctx.getLevel().hasNeighborSignal(ctx.getClickedPos()));
        return connectAll(s, ctx.getLevel(), ctx.getClickedPos());
    }

    private BlockState connectAll(BlockState s, LevelAccessor level, BlockPos pos) {
        return s.setValue(NEG, joinsLed(s, level.getBlockState(pos.west()))).setValue(POS, joinsLed(s, level.getBlockState(pos.east())))
                .setValue(NORTH, joinsLed(s, level.getBlockState(pos.north()))).setValue(SOUTH, joinsLed(s, level.getBlockState(pos.south())));
    }

    private boolean joinsLed(BlockState self, BlockState other) {
        return other.getBlock() == this && other.getValue(HANGING) == self.getValue(HANGING);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (dir.getAxis().isHorizontal()) return connectAll(state, level, pos);
        return state;
    }

    @Override
    protected List<BlockPos> fixture(Level level, BlockPos pos, BlockState state) {
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        todo.add(pos);
        seen.add(pos);
        while (!todo.isEmpty() && out.size() < MAX_BAR * 4) {
            BlockPos p = todo.poll();
            BlockState s = level.getBlockState(p);
            if (s.getBlock() != this || s.getValue(HANGING) != state.getValue(HANGING)) continue;
            out.add(p);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (seen.add(n)) todo.add(n);
            }
        }
        return out;
    }
}
