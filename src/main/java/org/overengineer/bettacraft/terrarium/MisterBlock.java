package org.overengineer.bettacraft.terrarium;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.registry.ModBlockEntities;

/**
 * M31: mister (fogger) for terraria and paludaria: a small water tank with a pump and a nozzle, on a top block of the tank
 * or beside it. A hygrostat: it sprays while the humidity is below its setting (right-click +5 %, sneak + right-click -5 %,
 * 40-95 %), and every second it sprays it uses {@code terrarium.misterMbPerSecond} of its water. Water comes in by
 * bucket, from pipes and other mods' fluid handlers, from a reservoir or water source next to it.
 */
public class MisterBlock extends BaseEntityBlock {
    public static final MapCodec<MisterBlock> CODEC = simpleCodec(MisterBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty MISTING = BooleanProperty.create("misting");
    private static final VoxelShape SHAPE = box(4, 0, 4, 12, 9, 12);

    public MisterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MISTING, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, MISTING);
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof MisterBlockEntity be)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return be.fillFrom(stack, player, hand) ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MisterBlockEntity be) be.adjust(player, player.isShiftKeyDown() ? -5 : 5);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide && !old.is(this)) {
            TankClimate.equipmentChanged(level, pos.below());
            org.overengineer.bettacraft.plumbing.PipeNetwork.invalidate(level);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if (!level.isClientSide && !newState.is(this)) {
            TankClimate.equipmentChanged(level, pos.below());
            org.overengineer.bettacraft.plumbing.PipeNetwork.invalidate(level);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Comparator: how full its water tank is. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MisterBlockEntity be ? be.comparator() : 0;
    }

    /** A fine mist sinks from the nozzle while it sprays. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!state.getValue(MISTING)) return;
        Direction f = state.getValue(FACING);
        for (int i = 0; i < 3; i++) {
            double x = pos.getX() + 0.5 + f.getStepX() * 0.3 + (r.nextDouble() - 0.5) * 0.3;
            double z = pos.getZ() + 0.5 + f.getStepZ() * 0.3 + (r.nextDouble() - 0.5) * 0.3;
            level.addParticle(ParticleTypes.CLOUD, x, pos.getY() + 0.1, z, f.getStepX() * 0.01, -0.03, f.getStepZ() * 0.01);
        }
        if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() - 0.2, pos.getZ() + 0.5, 0, 0, 0);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MisterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.MISTER.get(), MisterBlockEntity::serverTick);
    }
}
