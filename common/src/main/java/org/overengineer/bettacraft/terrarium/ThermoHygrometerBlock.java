package org.overengineer.bettacraft.terrarium;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M31: digital thermo-hygrometer: a small display stuck to the outside of a tank wall ({@link #FACING} = the side it looks
 * out of). Its screen shows the air temperature and the humidity of the terrarium behind it (the water temperature on an
 * aquarium); right-click tells all values; a comparator reads the humidity (0-15).
 */
public class ThermoHygrometerBlock extends BaseEntityBlock {
    public static final MapCodec<ThermoHygrometerBlock> CODEC = simpleCodec(ThermoHygrometerBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape NORTH = box(4, 4, 15, 12, 12, 16), SOUTH = box(4, 4, 0, 12, 12, 1), WEST = box(15, 4, 4, 16, 12, 12),
            EAST = box(0, 4, 4, 1, 12, 12);

    public ThermoHygrometerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction face = ctx.getClickedFace();
        if (!face.getAxis().isHorizontal()) return null;
        BlockState s = defaultBlockState().setValue(FACING, face);
        return canSurvive(s, ctx.getLevel(), ctx.getClickedPos()) ? s : null;
    }

    /** The block it sticks to. */
    public static BlockPos attachedTo(BlockPos pos, BlockState state) {
        return pos.relative(state.getValue(FACING).getOpposite());
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos on = attachedTo(pos, state);
        BlockState s = level.getBlockState(on);
        return s.getBlock() instanceof TankBlock || s.isFaceSturdy(level, on, state.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (dir == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return state;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> NORTH;
        };
    }

    /** The tank it is on (also on the client). */
    @Nullable
    public static TankBlockEntity tank(Level level, BlockPos pos, BlockState state) {
        BlockPos on = attachedTo(pos, state);
        return level.isLoaded(on) ? TankMultiblock.getController(level, on) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        TankBlockEntity c = tank(level, pos, state);
        player.displayClientMessage(readout(c == null ? null : c.getTank()), true);
        return InteractionResult.CONSUME;
    }

    /** The readout for the action bar. */
    public static Component readout(@Nullable TankData t) {
        if (t == null) return Component.translatable("message.bettacraft.thermometer.no_tank").withStyle(ChatFormatting.GRAY);
        if (!t.hasClimate()) {
            return Component.translatable("message.bettacraft.thermometer.water", TankClimate.f1(t.waterTemp())).withStyle(ChatFormatting.AQUA);
        }
        return Component.translatable("message.bettacraft.thermometer.climate", Component.translatable(t.terrariumMode().translationKey()),
                TankClimate.f1(t.airTemp()), Math.round(t.humidity()), TankClimate.f1(t.baskTemp()), TankClimate.f1(t.uv())).withStyle(ChatFormatting.GREEN);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        TankBlockEntity c = tank(level, pos, state);
        if (c == null || !c.getTank().hasClimate()) return 0;
        return Math.max(0, Math.min(15, Math.round(c.getTank().humidity() * 15 / 100F)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ThermoHygrometerBlockEntity(pos, state);
    }
}
