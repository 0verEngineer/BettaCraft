package org.overengineer.bettacraft.plumbing;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * M24: a pipe for water (glass: cheap, carries {@code pipes.glassPipeRate}; stainless steel: {@code pipes.steelPipeRate}).
 * Pipes hold no water themselves: whatever pushes water into one (our water pump, the pumps and pipes of other mods) reaches
 * every fluid handler at the ends of the network ({@link PipeNetwork}), and whatever pulls from one gets water from them.
 * <p>
 * M68: the only two pipes there are (the filter pipes, their T-piece and the interior pipe are gone). A pipe joins every
 * neighbour that fits on every side ({@link Pipes#canJoin}): other pipes, tanks, the ports of the filter, our devices and every
 * block with a fluid handler; the pipe wrench closes a side ({@link PipeWrenchItem}, kept in the block entity). The ports a
 * network touches give it its role ({@link PipeRole}: intake or return line of a filter, plain water line without one) and
 * its pipes the ring of that role at their block borders ({@link #RING}, set by {@link PipeUpdates}). Sneaking with a pipe on a
 * pipe puts both into one block (a bundle, {@link PipeBundleBlock}); placed in a tank interior a pipe is an interior pipe
 * ({@link PipeItem}). A cover (M40 part c, {@link PipeCovers}) draws it as a full block.
 */
public class PipeBlock extends BaseEntityBlock implements org.overengineer.bettacraft.platform.PositionalBlock.Sound {
    public static final MapCodec<PipeBlock> CODEC = simpleCodec(p -> new PipeBlock(false, p));
    public static final Map<Direction, BooleanProperty> SIDES = new EnumMap<>(Map.of(
            Direction.NORTH, BlockStateProperties.NORTH, Direction.SOUTH, BlockStateProperties.SOUTH, Direction.EAST, BlockStateProperties.EAST,
            Direction.WEST, BlockStateProperties.WEST, Direction.UP, BlockStateProperties.UP, Direction.DOWN, BlockStateProperties.DOWN));
    /** M68: the ring of the pipe's network (its role). */
    public static final EnumProperty<PipeRing> RING = EnumProperty.create("ring", PipeRing.class);
    // M65: the pipe 3 px thick (was 6)
    private static final VoxelShape CORE = box(6.5, 6.5, 6.5, 9.5, 9.5, 9.5);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Map.of(
            Direction.NORTH, box(6.5, 6.5, 0, 9.5, 9.5, 6.5), Direction.SOUTH, box(6.5, 6.5, 9.5, 9.5, 9.5, 16),
            Direction.WEST, box(0, 6.5, 6.5, 6.5, 9.5, 9.5), Direction.EAST, box(9.5, 6.5, 6.5, 16, 9.5, 9.5),
            Direction.DOWN, box(6.5, 0, 6.5, 9.5, 6.5, 9.5), Direction.UP, box(6.5, 9.5, 6.5, 9.5, 16, 9.5)));
    private static final VoxelShape[] SHAPES = new VoxelShape[64];
    private final boolean steel;

    static {
        for (int m = 0; m < 64; m++) {
            VoxelShape s = CORE;
            for (Direction d : Direction.values()) if ((m & PipeSlots.bit(d)) != 0) s = Shapes.or(s, ARMS.get(d));
            SHAPES[m] = s.optimize();
        }
    }

    public PipeBlock(boolean steel, Properties properties) {
        super(properties);
        this.steel = steel;
        BlockState s = stateDefinition.any().setValue(RING, PipeRing.NONE);
        for (BooleanProperty p : SIDES.values()) s = s.setValue(p, false);
        registerDefaultState(s);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public boolean steel() {
        return steel;
    }

    /** mB per tick a network with this pipe can carry. */
    public int rate() {
        return rate(steel);
    }

    /** mB per tick a glass or a steel pipe carries. */
    public static int rate(boolean steel) {
        return steel ? org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.PIPE_STEEL_RATE, 250)
                : org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.PIPE_GLASS_RATE, 50);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        SIDES.values().forEach(b::add);
        b.add(RING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBlockEntity(pos, state);
    }

    /** The joined sides of a pipe's state (bit = {@code 1 << direction.get3DDataValue()}). */
    public static int sides(BlockState s) {
        int m = 0;
        if (!(s.getBlock() instanceof PipeBlock)) return 0;
        for (Direction d : Direction.values()) if (s.getValue(SIDES.get(d))) m |= PipeSlots.bit(d);
        return m;
    }

    /** The state with these joined sides. */
    public static BlockState withSides(BlockState s, int sides) {
        for (Direction d : Direction.values()) s = s.setValue(SIDES.get(d), (sides & PipeSlots.bit(d)) != 0);
        return s;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return withConnections(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
    }

    /** The state with every side joined that fits now ({@link Pipes#canJoin}; sides towards unloaded chunks stay). */
    public static BlockState withConnections(BlockState s, LevelAccessor level, BlockPos pos) {
        for (Direction d : Direction.values()) s = s.setValue(SIDES.get(d), Pipes.canJoin(level, pos, -1, d, s.getValue(SIDES.get(d))));
        return s;
    }

    /** Whether the pipe at {@code pos} joins whatever is on side {@code d}. */
    public static boolean connects(LevelAccessor level, BlockPos pos, Direction d) {
        BlockState s = level.getBlockState(pos);
        return Pipes.canJoin(level, pos, -1, d, s.getBlock() instanceof PipeBlock && s.getValue(SIDES.get(d)));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (level instanceof Level l && !l.isClientSide) {
            PipeNetwork.invalidate(l, pos);
            PipeUpdates.schedule(l, pos, false);
        }
        return state.setValue(SIDES.get(dir), Pipes.canJoin(level, pos, -1, dir, state.getValue(SIDES.get(dir))));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        // only a new ring: nothing changed for the networks
        if (old.is(this) && sides(old) == sides(state)) return;
        PipeNetwork.invalidate(level, pos);
        if (level.isClientSide) return;
        PipeUpdates.schedule(level, pos, false);
        // set directly without any side (a structure, a command, a GameTest): join as if it had been placed by hand. A state that
        // comes with its sides keeps them (a bundle down to its last pipe; a pipe of an old world, whose block entity closes the
        // sides it was not joined on, PipeBlockEntity)
        if (!old.is(this) && sides(state) == 0) {
            BlockState now = withConnections(state, level, pos);
            if (now != state) level.setBlock(pos, now, Block.UPDATE_ALL);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if (newState.is(this)) return;
        PipeNetwork.invalidate(level, pos);
        if (!level.isClientSide) PipeUpdates.schedule(level, pos, false);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        if (level.isClientSide) return;
        PipeNetwork.invalidate(level, pos);
        PipeUpdates.schedule(level, pos, false);
        BlockState now = withConnections(state, level, pos);
        if (now != state) level.setBlock(pos, now, Block.UPDATE_ALL);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemInteractionResult facade = PipeCovers.useForeign(stack, level, pos, player, hand); // M40 part c: another mod's facade
        return facade != null ? facade : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return PipeCovers.shape(level, pos, SHAPES[sides(state)]);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true; // also under a cover (the light engine reads no block entities off the main thread)
    }

    /** The pipe from the loot table, and the block or facade of its cover. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        List<ItemStack> out = new ArrayList<>(super.getDrops(state, params));
        PipeCovers.addDrops(out, params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY));
        return out;
    }

    // ------------------------------------------------------------------ the cover (M40 part c)

    public boolean hidesNeighborFace(BlockGetter level, BlockPos pos, BlockState state, BlockState neighborState, Direction dir) {
        return PipeCovers.hidesNeighbour(level, pos);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return PipeCovers.shade(level, pos);
    }

    public boolean hasDynamicLightEmission(BlockState state) {
        return true;
    }

    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return PipeCovers.light(level, pos);
    }

    public net.minecraft.world.level.block.SoundType getSoundType(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos, @Nullable net.minecraft.world.entity.Entity entity) {
        var s = PipeCovers.sound(level, pos, entity);
        return s != null ? s : state.getSoundType();
    }

    public BlockState getAppearance(BlockState state, net.minecraft.world.level.BlockAndTintGetter level, BlockPos pos, Direction side, @Nullable BlockState queryState, @Nullable BlockPos queryPos) {
        BlockState c = PipeCovers.coverAt(level, pos);
        return c != null ? c : state;
    }
}
