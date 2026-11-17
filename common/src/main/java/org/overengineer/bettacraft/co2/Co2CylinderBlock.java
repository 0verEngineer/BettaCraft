package org.overengineer.bettacraft.co2;

import org.overengineer.bettacraft.util.BlockRemoval;
import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * M25: the CO2 gas cylinder with pressure regulator and bubble counter. It stands next to a tank (or on it) and doses
 * 0..10 bubbles per second (right-click: the regulator; sneak + mouse wheel) as long as it has gas. A solenoid valve
 * (right-click with it) closes it while the tank's lights are off (its daylight timer, or the night), so that the CO2
 * does not build up when the plants do not take it up. Empty cylinders are refilled at the CO2 filling station; broken,
 * the cylinder keeps its gas and its valve.
 */
public class Co2CylinderBlock extends BaseEntityBlock implements BlockRemoval, Co2SourceBlock {
    public static final MapCodec<Co2CylinderBlock> CODEC = simpleCodec(Co2CylinderBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty SOLENOID = BooleanProperty.create("solenoid");
    /** Gas flows right now (bubble counter). */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape SHAPE = box(4, 0, 4, 12, 16, 12);

    public Co2CylinderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SOLENOID, false).setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, SOLENOID, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Boolean valve = ctx.getItemInHand().get(org.overengineer.bettacraft.registry.ModDataComponents.CO2_SOLENOID.get());
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()).setValue(SOLENOID, valve != null && valve);
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
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.CO2_SOLENOID.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (state.getValue(SOLENOID)) {
            if (!level.isClientSide()) player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.bettacraft.co2.has_solenoid")
                    .withStyle(net.minecraft.ChatFormatting.GOLD), true);
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(SOLENOID, true), Block.UPDATE_ALL);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE, net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.6F);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.bettacraft.co2.solenoid_on")
                    .withStyle(net.minecraft.ChatFormatting.GREEN), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) org.overengineer.bettacraft.util.ClientHooks.openRegulator(pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide() && !old.is(this)) Co2SourceBlock.invalidateTank(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!level.isClientSide() && !newState.is(this)) Co2SourceBlock.invalidateTank(level, pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Co2CylinderBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? createTickerHelper(type, ModBlockEntities.CO2_CYLINDER.get(), Co2CylinderBlockEntity::clientTick) : null;
    }
}
