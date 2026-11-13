package org.overengineer.bettacraft.equipment;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Aquarium cabinet (PLAN 6.5): one block, made of any planks (the planks are kept in the block entity and the model
 * is retextured with them on the client). Neighbouring cabinets facing the same way join into one counter
 * ({@link #LEFT} / {@link #RIGHT} as seen from the front). One slot for an external filter (with two heaters), nine
 * storage slots; the feeder of the tank above takes food from it.
 * <p>
 * M21.7: a cabinet of stone ({@link CabinetStones#isStoneMaterial}) is mined like its stone: pickaxe, hardness, blast
 * resistance, sound and map colour come from the stone block; without the right tool it drops nothing (its contents
 * still drop). Wooden cabinets keep the properties of this block.
 */
public class CabinetBlock extends BaseEntityBlock {
    public static final MapCodec<CabinetBlock> CODEC = simpleCodec(CabinetBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");

    public CabinetBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LEFT, false).setValue(RIGHT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEFT, RIGHT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Left as seen by someone standing in front of the cabinet. */
    public static Direction left(Direction facing) {
        return facing.getClockWise();
    }

    private static boolean joins(BlockState self, BlockState other) {
        return other.getBlock() instanceof CabinetBlock && other.getValue(FACING) == self.getValue(FACING);
    }

    static BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        Direction f = state.getValue(FACING);
        return state.setValue(LEFT, joins(state, level.getBlockState(pos.relative(left(f)))))
                .setValue(RIGHT, joins(state, level.getBlockState(pos.relative(left(f).getOpposite()))));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        return connect(s, ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return dir.getAxis().isHorizontal() ? connect(state, level, pos) : state;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState s = state.rotate(mirror.getRotation(state.getValue(FACING)));
        return mirror == Mirror.NONE ? s : s.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CabinetBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof CabinetBlockEntity be) {
            sp.openMenu(be, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CabinetBlockEntity be) {
            for (int i = 0; i < be.items().getSlots(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.items().getStackInSlot(i));
            }
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // ------------------------------------------------------------------ M21.7: stone cabinets behave like their stone

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockState stone = CabinetStones.stoneOf(level, pos);
        return stone != null ? stone.getDestroyProgress(player, level, pos) : super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        BlockState stone = CabinetStones.stoneOf(level, pos);
        return stone != null ? stone.canHarvestBlock(level, pos, player) : super.canHarvestBlock(state, level, pos, player);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.level.Explosion explosion) {
        BlockState stone = CabinetStones.stoneOf(level, pos);
        return stone != null ? stone.getExplosionResistance(level, pos, explosion) : super.getExplosionResistance(state, level, pos, explosion);
    }

    @Override
    public net.minecraft.world.level.block.SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable net.minecraft.world.entity.Entity entity) {
        BlockState stone = CabinetStones.stoneOf(level, pos);
        return stone != null ? stone.getSoundType(level, pos, entity) : super.getSoundType(state, level, pos, entity);
    }

    @Override
    public net.minecraft.world.level.material.MapColor getMapColor(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.level.material.MapColor defaultColor) {
        BlockState stone = CabinetStones.stoneOf(level, pos);
        return stone != null ? stone.getMapColor(level, pos) : super.getMapColor(state, level, pos, defaultColor);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        ItemStack s = super.getCloneItemStack(state, target, level, pos, player);
        if (level.getBlockEntity(pos) instanceof CabinetBlockEntity be) CabinetBlockItem.setWood(s, be.woodId());
        return s;
    }
}
