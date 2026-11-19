package org.overengineer.bettacraft.food;

import org.overengineer.bettacraft.util.BlockRemoval;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.plumbing.PipeConnectable;
import org.overengineer.bettacraft.registry.ModBlockEntities;

/**
 * M26: the live food pump. It sits next to a live food breeder (daphnia barrel, artemia hatchery, larva tub, tubifex
 * dish) and pumps the food as a suspension through the pipes into the tanks, on a schedule set in its window
 * ({@link LiveFoodPumpBlockEntity}). Pipes join it on every side; a tank right next to it is fed directly.
 */
public class LiveFoodPumpBlock extends BaseEntityBlock implements BlockRemoval, PipeConnectable {
    private static final VoxelShape SHAPE = box(3, 0, 3, 13, 9, 13);

    public LiveFoodPumpBlock(Properties properties) {
        super(properties);
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
    public boolean pipeConnects(BlockState state, Direction face) {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof LiveFoodPumpBlockEntity be) org.overengineer.bettacraft.platform.Platform.get().openMenu(sp, be, buf -> buf.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        org.overengineer.bettacraft.plumbing.PipeNetwork.invalidate(level);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LiveFoodPumpBlockEntity be && be.bottles() > 0) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.GLASS_BOTTLE, be.bottles()));
        }
        org.overengineer.bettacraft.plumbing.PipeNetwork.invalidate(level);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LiveFoodPumpBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.LIVE_FOOD_PUMP.get(), LiveFoodPumpBlockEntity::serverTick);
    }
}
