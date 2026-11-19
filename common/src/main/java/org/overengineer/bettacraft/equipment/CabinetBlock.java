package org.overengineer.bettacraft.equipment;

import org.overengineer.bettacraft.util.BlockRemoval;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
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
public class CabinetBlock extends BaseEntityBlock implements BlockRemoval, org.overengineer.bettacraft.platform.PickableBlock, org.overengineer.bettacraft.platform.PositionalBlock.Sound, org.overengineer.bettacraft.platform.PositionalBlock.Blast, org.overengineer.bettacraft.platform.PositionalBlock.Harvest {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    /**
     * M44: the front this block shows ({@link CabinetFront}); computed from the row and the menu setting of the block entity
     * ({@link #withFront}). The default (also of cabinets saved before M44 until they are loaded) is the old two-door front.
     */
    public static final EnumProperty<CabinetFront> FRONT = EnumProperty.create("front", CabinetFront.class);
    /** M44: thin dark joints between the leaves and drawers (menu setting; off with the feature). */
    public static final BooleanProperty JOINTS = BooleanProperty.create("joints");

    public CabinetBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LEFT, false).setValue(RIGHT, false)
                .setValue(FRONT, CabinetFront.TWO).setValue(JOINTS, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEFT, RIGHT, FRONT, JOINTS);
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
        return withFront(state.setValue(LEFT, joins(state, level.getBlockState(pos.relative(left(f)))))
                .setValue(RIGHT, joins(state, level.getBlockState(pos.relative(left(f).getOpposite())))), level, pos);
    }

    // ------------------------------------------------------------------ M44 fronts

    /**
     * M44: the state with the front and the joints it should show: the old two-door front without joints while {@code
     * features.equipment.cabinetFronts} is off (the menu setting is kept), else the setting of the block entity, with {@link
     * CabinetFrontSetting#AUTO} the front of its place in the row ({@link #autoFront}).
     */
    public static BlockState withFront(BlockState state, BlockGetter level, BlockPos pos) {
        if (!Features.enabled(Feature.CABINET_FRONTS)) return state.setValue(FRONT, CabinetFront.TWO).setValue(JOINTS, false);
        CabinetFrontSetting setting = CabinetFrontSetting.AUTO;
        boolean joints = true;
        if (level.getBlockEntity(pos) instanceof CabinetBlockEntity be) {
            setting = be.frontSetting();
            joints = be.joints();
        }
        CabinetFront front = setting.fixed() != null ? setting.fixed() : autoFront(state, level, pos);
        return state.setValue(FRONT, front).setValue(JOINTS, joints);
    }

    /** Cabinets joined to this one in direction {@code d}, at most {@link CabinetFront#MAX_ROW}. */
    private static int count(BlockState state, BlockGetter level, BlockPos pos, Direction d) {
        int n = 0;
        BlockPos.MutableBlockPos p = pos.mutable();
        while (n < CabinetFront.MAX_ROW && joins(state, level.getBlockState(p.move(d)))) n++;
        return n;
    }

    /** M44: the front of this block from the row it stands in (cabinets side by side facing the same way). */
    public static CabinetFront autoFront(BlockState state, BlockGetter level, BlockPos pos) {
        Direction left = left(state.getValue(FACING)), right = left.getOpposite();
        int l = count(state, level, pos, left), r = count(state, level, pos, right);
        if (l + r + 1 > CabinetFront.MAX_ROW) return CabinetFront.longRow(pos.getX() * right.getStepX() + pos.getZ() * right.getStepZ());
        return CabinetFront.layout(l + r + 1, l);
    }

    /** M44: sets the right front on this cabinet (no shape updates around it). */
    public static void refresh(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof CabinetBlock)) return;
        BlockState want = connect(s, level, pos);
        if (!want.equals(s)) level.setBlock(pos, want, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    /**
     * M44: the fronts of the whole row through {@code pos} (as far as a change at {@code pos} reaches). Shape updates alone
     * stop at the first block whose state stays the same, but a row of 4 that grows to 5 changes its middle as well.
     */
    public static void refreshRow(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof CabinetBlock)) return;
        Direction left = left(s.getValue(FACING));
        BlockPos start = pos;
        for (int i = 0; i <= CabinetFront.MAX_ROW && joins(s, level.getBlockState(start.relative(left))); i++) start = start.relative(left);
        BlockPos p = start;
        for (int i = 0; i <= 2 * CabinetFront.MAX_ROW + 2; i++) {
            refresh(level, p);
            BlockPos next = p.relative(left.getOpposite());
            if (!joins(s, level.getBlockState(next))) break;
            p = next;
        }
    }

    /** M44: the rows of this block and of its four neighbours (placed or turned). */
    private static void refreshAround(Level level, BlockPos pos) {
        refreshRow(level, pos);
        for (Direction d : Direction.Plane.HORIZONTAL) refreshRow(level, pos.relative(d));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && (!oldState.is(this) || oldState.getValue(FACING) != state.getValue(FACING))) refreshAround(level, pos);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) refresh(level, pos); // the menu setting came with the item
    }

    /** M44: scheduled when the block entity loads (cabinets of older versions, a feature switch changed meanwhile). */
    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        refresh(level, pos);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        return connect(s, ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos, Direction dir, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        return dir.getAxis().isHorizontal() ? connect(state, level, pos) : state;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState s = state.rotate(mirror.getRotation(state.getValue(FACING)));
        return mirror == Mirror.NONE ? s : s.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT))
                .setValue(FRONT, state.getValue(FRONT).mirrored());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CabinetBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof CabinetBlockEntity be) {
            org.overengineer.bettacraft.platform.Platform.get().openMenu(sp, be, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CabinetBlockEntity be) {
            for (int i = 0; i < be.items().getSlots(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.items().getStackInSlot(i));
            }
            if (be.intakeSponge()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(org.overengineer.bettacraft.registry.ModItems.INTAKE_SPONGE.get())); // M23
            level.updateNeighbourForOutputSignal(pos, this);
        }
        // M44: the rows on both sides get their fronts again
        if (!level.isClientSide() && (!newState.is(this) || newState.getValue(FACING) != state.getValue(FACING))) {
            for (Direction d : Direction.Plane.HORIZONTAL) refreshRow(level, pos.relative(d));
        }
    }

    // ------------------------------------------------------------------ M21.7: stone cabinets behave like their stone

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockState stone = CabinetStones.stoneOf(level, pos);
        return stone != null ? stone.getDestroyProgress(player, level, pos) : super.getDestroyProgress(state, player, level, pos);
    }

    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        BlockState stone = CabinetStones.stoneOf(level, pos);
        return stone != null ? org.overengineer.bettacraft.platform.Platform.get().canHarvestBlock(stone, level, pos, player) : org.overengineer.bettacraft.platform.Platform.get().defaultHarvestCheck(state, level, pos, player);
    }

    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.level.Explosion explosion) {
        BlockState stone = CabinetStones.stoneOf(level, pos);
        return stone != null ? org.overengineer.bettacraft.platform.Platform.get().explosionResistance(stone, level, pos, explosion) : getExplosionResistance();
    }

    public net.minecraft.world.level.block.SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable net.minecraft.world.entity.Entity entity) {
        BlockState stone = CabinetStones.stoneOf(level, pos);
        return stone != null ? org.overengineer.bettacraft.platform.Platform.get().soundType(stone, level, pos, entity) : state.getSoundType();
    }

    public net.minecraft.world.level.material.MapColor getMapColor(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.level.material.MapColor defaultColor) {
        BlockState stone = CabinetStones.stoneOf(level, pos);
        return stone != null ? stone.getMapColor(level, pos) : defaultColor;
    }

    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        ItemStack s = super.getCloneItemStack(level, pos, state, false);
        if (level.getBlockEntity(pos) instanceof CabinetBlockEntity be) {
            CabinetBlockItem.setWood(s, be.woodId());
            if (be.outletSetting() != OutletSide.BACK) s.set(org.overengineer.bettacraft.registry.ModDataComponents.OUTLET_SIDE.get(), be.outletSetting().id());
            // M44: the front and the joints set in the menu
            if (be.frontSetting() != CabinetFrontSetting.AUTO) s.set(org.overengineer.bettacraft.registry.ModDataComponents.CABINET_FRONT.get(), be.frontSetting().id());
            if (!be.joints()) s.set(org.overengineer.bettacraft.registry.ModDataComponents.CABINET_JOINTS.get(), false);
        }
        return s;
    }

    /** M52b: NeoForge 21.11 asks without the hit; it is the player's view ray, as for the pick-block key. */
    public net.minecraft.world.item.ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos, BlockState state, boolean includeData,
            net.minecraft.world.entity.player.Player player) {
        return getCloneItemStack(state, player.pick(player.blockInteractionRange(), 1.0F, false), level, pos, player);
    }
}
