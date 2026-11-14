package org.overengineer.bettacraft.equipment;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * M23: an air stone in a tank interior (one interior block, a round stone on a small foot with the air hose). While the
 * air pump of its tank drives it ({@link DeviceSegmentBlock#ACTIVE}, set by the server's care step) a column of bubbles
 * rises from it to the water surface, inside the tank and in the miniature. {@link DeviceSegmentBlock#POWER} is its
 * regulator (0..100 %).
 */
public class AirStoneBlock extends BaseEntityBlock {
    public static final MapCodec<AirStoneBlock> CODEC = simpleCodec(AirStoneBlock::new);
    private static final VoxelShape SHAPE = box(3, 0, 3, 13, 6, 13);

    public AirStoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DeviceSegmentBlock.POWER, 10).setValue(DeviceSegmentBlock.ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(DeviceSegmentBlock.POWER, DeviceSegmentBlock.ACTIVE);
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
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.AIR_STONE.get());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!org.overengineer.bettacraft.config.CommonConfig.powerRegulator()) return InteractionResult.PASS;
        if (level.isClientSide) org.overengineer.bettacraft.util.ClientHooks.openRegulator(pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AirStoneBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, ModBlockEntities.AIR_STONE.get(), AirStoneBlockEntity::clientTick) : null;
    }
}
