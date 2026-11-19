package org.overengineer.bettacraft.food;

import org.overengineer.bettacraft.util.BlockRemoval;
import net.minecraft.core.Direction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.registry.ModBlockEntities;

/**
 * M26: live food cultures. The tubifex dish keeps tubifex worms in mud and stale water fed with mulm (or the waste water
 * of a tank through a pipe); the daphnia barrel keeps water fleas in water with yeast or green water, in the light. The
 * net harvests them, hoppers and the live food pump take them out ({@link LiveCultureBlockEntity}).
 */
public class LiveCultureBlock extends BaseEntityBlock implements BlockRemoval {
    public enum Kind { TUBIFEX, DAPHNIA }

    /** How full the culture is: 0 none, 1 few, 2 many, 3 crowded. */
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);
    public static final BooleanProperty WATER = BooleanProperty.create("water");
    /** Tubifex dish: mud is in it. */
    public static final BooleanProperty MUD = BooleanProperty.create("mud");
    private static final VoxelShape DISH = box(1, 0, 1, 15, 5, 15);
    private static final VoxelShape BARREL = box(1, 0, 1, 15, 15, 15);

    private final Kind kind;

    public LiveCultureBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0).setValue(WATER, false).setValue(MUD, false));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(STAGE, WATER, MUD);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind == Kind.TUBIFEX ? DISH : BARREL;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LiveCultureBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.LIVE_CULTURE.get(), LiveCultureBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof LiveCultureBlockEntity be) || stack.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return be.accepts(stack) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
        return be.use((ServerLevel) level, player, hand, stack) ? InteractionResult.CONSUME : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LiveCultureBlockEntity be) be.showStatus(level, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LiveCultureBlockEntity be) be.dropContents(level, pos);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Comparator: how full the culture is (15 = full room). */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof LiveCultureBlockEntity be ? be.comparatorSignal() : 0;
    }
}
