package org.overengineer.bettacraft.fertilizer;

import org.overengineer.bettacraft.util.BlockRemoval;
import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.platform.fluid.FluidUtil;
import org.overengineer.bettacraft.registry.ModBlockEntities;

/**
 * M37: the fertilizer maker. A steel case with a mixing tank on top and a crank on the side. Right-click opens the window;
 * a water bucket (or any fluid container of other mods) fills its water tank; the crank winds its spring
 * ({@link FertilizerMakerBlockEntity}).
 */
public class FertilizerMakerBlock extends BaseEntityBlock implements BlockRemoval {
    public static final MapCodec<FertilizerMakerBlock> CODEC = simpleCodec(FertilizerMakerBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Position of the crank handle (quarter turns). */
    public static final IntegerProperty CRANK = IntegerProperty.create("crank", 0, 3);
    public static final BooleanProperty WORKING = BooleanProperty.create("working");
    private static final VoxelShape SHAPE = Shapes.or(box(2, 0, 2, 14, 10, 14), box(3, 10, 3, 13, 15, 13));

    public FertilizerMakerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CRANK, 0).setValue(WORKING, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, CRANK, WORKING);
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
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // a water bucket (or a tank item of another mod) fills the water tank
        if (!stack.isEmpty() && FluidUtil.getFluidHandler(stack).isPresent()) {
            if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FertilizerMakerBlockEntity be)) return InteractionResult.PASS;
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.bettacraft.crank.use_crank"));
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer sp) org.overengineer.bettacraft.platform.Platform.get().openMenu(sp, be, buf -> buf.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof FertilizerMakerBlockEntity be ? be.comparator() : 0;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!newState.is(this) && level.getBlockEntity(pos) instanceof FertilizerMakerBlockEntity be) {
            for (int i = 0; i < FertilizerMakerBlockEntity.SLOTS; i++) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.items().getStackInSlot(i));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!state.getValue(WORKING) || r.nextInt(3) != 0) return;
        double x = pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.4, y = pos.getY() + 0.97, z = pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.4;
        level.addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0, 0.01, 0);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FertilizerMakerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.FERTILIZER_MAKER.get(), FertilizerMakerBlockEntity::serverTick);
    }
}
