package org.overengineer.bettacraft.co2;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import org.overengineer.bettacraft.registry.ModBlockEntities;

/**
 * M25: the CO2 filling station. It sits on or next to a furnace, smoker, blast furnace or campfire
 * ({@code #bettacraft:flue_gas_sources}) and fills the cylinder in it with the CO2 of the flue gas while the fire burns
 * (more fires fill faster); with a power option on its compressor also needs FE. CO2 gas of other mods (fluid tag
 * {@code #bettacraft:co2_gas}) goes in through pipes. Right-click with a cylinder: put it in; empty hand: take it out;
 * sneaking: what it does. Hoppers take full cylinders out and put empty ones in.
 */
public class Co2StationBlock extends BaseEntityBlock {
    public static final MapCodec<Co2StationBlock> CODEC = simpleCodec(Co2StationBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** It is filling right now. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    /** A cylinder is in it. */
    public static final BooleanProperty LOADED = BooleanProperty.create("loaded");

    public Co2StationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false).setValue(LOADED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, ACTIVE, LOADED);
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof Co2CylinderItem)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof Co2StationBlockEntity be) be.insert(player, hand, stack);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof Co2StationBlockEntity be) {
            if (player.isShiftKeyDown() || !be.hasCylinder()) be.tell(player);
            else be.takeOut(player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof Co2StationBlockEntity be) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.items().getStackInSlot(0).copy());
            be.items().setStackInSlot(0, ItemStack.EMPTY);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof Co2StationBlockEntity be) || !be.hasCylinder()) return 0;
        return Math.max(1, Math.round(15 * Co2CylinderItem.share(be.items().getStackInSlot(0))));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Co2StationBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CO2_STATION.get(), Co2StationBlockEntity::serverTick);
    }
}
