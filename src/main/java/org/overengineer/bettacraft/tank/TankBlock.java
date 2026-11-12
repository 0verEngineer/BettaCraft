package org.overengineer.bettacraft.tank;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.overengineer.bettacraft.registry.ModBlockEntities;
import org.overengineer.bettacraft.tank.menu.TankMenu;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.tank.interior.InteriorManager;

/**
 * One block of an aquarium multiblock. The glass type lives in the {@link TankBlockEntity}; the block state only
 * carries what rendering and collision need without a block entity lookup:
 * <ul>
 *     <li>six "connected" flags (neighbour in that direction belongs to the same tank),</li>
 *     <li>{@link #FILL}: water height in this block in 1/16 (0 = dry). Any value &gt; 0 means the block is waterlogged
 *     with <b>vanilla</b> water ({@link #getFluidState}).</li>
 * </ul>
 * Water never spreads out: we never schedule fluid ticks and the outer walls have a full collision face.
 */
public class TankBlock extends BaseEntityBlock {
    public static final MapCodec<TankBlock> CODEC = simpleCodec(TankBlock::new);

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 16);

    public static final Map<Direction, BooleanProperty> CONNECTION = new EnumMap<>(Map.of(
            Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH,
            Direction.WEST, WEST, Direction.UP, UP, Direction.DOWN, DOWN));

    /** Wall thickness of the collision shape (1 px). */
    private static final double WALL = 1.0;
    private static final VoxelShape[] COLLISION = new VoxelShape[64];
    /** Same walls plus a closed top ("lid") for inhabitants (PLAN 4.2). */
    private static final VoxelShape[] COLLISION_LID = new VoxelShape[64];

    static {
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = Shapes.empty();
            for (Direction d : Direction.values()) {
                if (d == Direction.UP || (mask & (1 << d.ordinal())) != 0) {
                    continue; // open top, and no wall towards connected neighbours
                }
                shape = Shapes.or(shape, switch (d) {
                    case DOWN -> box(0, 0, 0, 16, WALL, 16);
                    case NORTH -> box(0, 0, 0, 16, 16, WALL);
                    case SOUTH -> box(0, 0, 16 - WALL, 16, 16, 16);
                    case WEST -> box(0, 0, 0, WALL, 16, 16);
                    case EAST -> box(16 - WALL, 0, 0, 16, 16, 16);
                    default -> Shapes.empty();
                });
            }
            COLLISION[mask] = shape.optimize();
            boolean up = (mask & (1 << Direction.UP.ordinal())) != 0;
            COLLISION_LID[mask] = up ? COLLISION[mask] : Shapes.or(shape, box(0, 16 - WALL, 0, 16, 16, 16)).optimize();
        }
    }

    public TankBlock(Properties properties) {
        super(properties);
        BlockState def = this.stateDefinition.any().setValue(FILL, 0);
        for (BooleanProperty p : CONNECTION.values()) {
            def = def.setValue(p, false);
        }
        registerDefaultState(def);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, FILL);
    }

    public static boolean isConnected(BlockState state, Direction dir) {
        return state.getValue(CONNECTION.get(dir));
    }

    public static int connectionMask(BlockState state) {
        int mask = 0;
        for (Direction d : Direction.values()) {
            if (isConnected(state, d)) {
                mask |= 1 << d.ordinal();
            }
        }
        return mask;
    }

    /**
     * Concave creases of all outer faces (M10, for the edge lines of free shapes): bit {@code side * 6 + e} is set when the
     * face towards {@code side} is outer, the neighbour towards {@code e} belongs to the tank and the block beyond it
     * towards {@code side} does too (the inner corner of an L). Only block states are read.
     */
    public static long concaveMask(BlockGetter level, BlockPos pos, BlockState state) {
        long out = 0;
        for (Direction side : Direction.values()) {
            if (isConnected(state, side)) continue;
            for (Direction e : Direction.values()) {
                if (e.getAxis() == side.getAxis() || !isConnected(state, e)) continue;
                BlockState n = level.getBlockState(pos.relative(e));
                if (n.getBlock() instanceof TankBlock && isConnected(n, side)) out |= 1L << (side.ordinal() * 6 + e.ordinal());
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ block entity

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.TANK.get(), TankCare::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // ------------------------------------------------------------------ fluid / shape / light

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(FILL) > 0 ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext ec && ec.getEntity() != null && !(ec.getEntity() instanceof Player)
                && TankInhabitants.hasTankRef(ec.getEntity())) {
            return COLLISION_LID[connectionMask(state)];
        }
        return COLLISION[connectionMask(state)];
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        return false;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(this)) {
            // Fallback for placements without setPlacedBy (commands, structures): form the tank next tick.
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel server) {
            TankMultiblock.onTankBlockAdded(server, pos, placer instanceof ServerPlayer sp ? sp : null);
            TankBlockEntity c = TankMultiblock.getController(level, pos);
            if (c != null) org.overengineer.bettacraft.progression.Progress.award(placer instanceof ServerPlayer sp ? sp : null, org.overengineer.bettacraft.progression.Progress.TANK_BUILT, c.getTank().memberCount());
            if (c != null && c.getTank().memberCount() == 1 && placer != null) {
                // a new tank faces the player who placed it
                c.getTank().setFront(placer.getDirection().getOpposite());
                c.markTankChanged();
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof TankBlockEntity be && be.getControllerPos() == null) {
            TankMultiblock.onTankBlockAdded(level, pos, null);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof TankBlockEntity be) {
            TankMultiblock.onTankBlockRemoved(server, pos, be);
            if (!newState.getFluidState().isEmpty() && newState.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
                // Level#destroyBlock leaves the fluid's legacy block (a water source) behind: remove it before it flows.
                TankEvents.dryLater(server, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        Optional<Component> blocked = TankInhabitants.breakBlockedReason(level, pos);
        if (blocked.isPresent()) {
            if (!level.isClientSide) player.displayClientMessage(blocked.get(), true);
            return false;
        }
        playerWillDestroy(level, pos, state, player);
        // The water simply vanishes: the block becomes air, never a water source (PLAN 4.1).
        return level.setBlock(pos, Blocks.AIR.defaultBlockState(), level.isClientSide ? 11 : 3);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);
        if (level.getBlockEntity(pos) instanceof TankBlockEntity be) {
            TankBlockItem.setGlass(stack, be.getGlassId());
        }
        return stack;
    }

    // ------------------------------------------------------------------ interaction

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        return TankInteractions.useItemOn(stack, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            InteriorManager.enter(sp, pos);
        } else {
            TankBlockEntity controller = TankMultiblock.getController(level, pos);
            if (controller == null) {
                TankInteractions.showInfo(sp, pos);
            } else {
                sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new TankMenu(id, inv, controller),
                        Component.translatable("gui.bettacraft.tank.title")), buf -> buf.writeBlockPos(controller.getBlockPos()));
                org.overengineer.bettacraft.progression.Progress.onMenuOpened(sp, controller);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Intentionally empty: vanilla water particles/drips for tank water are suppressed by ClientLevelMixin.
    }
}
