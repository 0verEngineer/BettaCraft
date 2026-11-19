package org.overengineer.bettacraft.plumbing;

import org.overengineer.bettacraft.util.BlockRemoval;
import org.overengineer.bettacraft.util.SinkList;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.util.RandomSource;
import java.util.function.Consumer;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.filter.FilterPartBlock;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M40: a filter pipe: the intake line (tanks to the filter's inlet) or the return line (its outlet back into the tanks),
 * {@link PipeLine}. A plain pipe joins at most two neighbours (a straight run or a bend, chosen when it is placed: the
 * block it was placed against first); a T-piece ({@link #JUNCTION}, put on with the pipe T-piece) joins every neighbour
 * that fits, so one filter feeds several tanks. A line joins pipes of the same line, its filter connector and tank blocks
 * on any side, the top or the bottom: there its connection appears in the interior ({@code TankAnchors}). Pipes hold
 * no water and pump nothing: the filter's pump head works through them ({@link FilterLines}).
 * <p>
 * Part b: also the fresh and the waste water line ({@link PipeLine#water()}: they join the fluid handlers of other blocks
 * instead of tanks and offer one themselves, {@link FilterLineFluid}). The pipe of another line used on a pipe makes a
 * bundle of both ({@link FilterPipeBundleBlock}). The joining rules are in {@link FilterPipes}, shared with the bundle.
 * <p>
 * Part c: a cover ({@link PipeCovers}) draws it as a full block; shape, light, sound and drops follow the cover.
 */
public class FilterPipeBlock extends BaseEntityBlock implements org.overengineer.bettacraft.util.BlockTooltip, BlockRemoval, org.overengineer.bettacraft.platform.PositionalBlock.Sound {
    public static final MapCodec<FilterPipeBlock> CODEC = simpleCodec(p -> new FilterPipeBlock(PipeLine.INTAKE, p));
    public static final Map<Direction, BooleanProperty> SIDES = PipeBlock.SIDES;
    /** A T-piece (joins up to all six sides). */
    public static final BooleanProperty JUNCTION = BooleanProperty.create("junction");
    private static final VoxelShape CORE = box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape JUNCTION_CORE = box(4, 4, 4, 12, 12, 12);
    private static final Map<Direction, VoxelShape> ARMS = Map.of(
            Direction.NORTH, box(5, 5, 0, 11, 11, 5), Direction.SOUTH, box(5, 5, 11, 11, 11, 16),
            Direction.WEST, box(0, 5, 5, 5, 11, 11), Direction.EAST, box(11, 5, 5, 16, 11, 11),
            Direction.DOWN, box(5, 0, 5, 11, 5, 11), Direction.UP, box(5, 11, 5, 11, 16, 11));
    /** Bumped on every change of a filter pipe (the pump heads look at their lines again). */
    private static volatile int version;
    private final PipeLine line;

    public FilterPipeBlock(PipeLine line, Properties properties) {
        super(properties);
        this.line = line;
        BlockState s = stateDefinition.any().setValue(JUNCTION, false);
        for (BooleanProperty p : SIDES.values()) s = s.setValue(p, false);
        registerDefaultState(s);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public PipeLine line() {
        return line;
    }

    public static int version() {
        return version;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        SIDES.values().forEach(b::add);
        b.add(JUNCTION);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FilterPipeBlockEntity(pos, state);
    }

    // ------------------------------------------------------------------ connections

    /** Sides one pipe joins at most. */
    public static int limit(BlockState s) {
        return s.getValue(JUNCTION) ? 6 : 2;
    }

    public static int count(BlockState s) {
        int n = 0;
        for (BooleanProperty p : SIDES.values()) if (s.getValue(p)) n++;
        return n;
    }

    /** Whether this pipe is joined on side {@code d}. */
    public static boolean joined(BlockState s, Direction d) {
        return s.getBlock() instanceof FilterPipeBlock && s.getValue(SIDES.get(d));
    }

    /** Part b: the line of this single pipe as a slot of {@link FilterPipes}. */
    public static int slot(BlockState s) {
        int slot = FilterPipes.PRESENT | (s.getValue(JUNCTION) ? FilterPipes.JUNCTION : 0);
        for (Direction d : Direction.values()) if (s.getValue(SIDES.get(d))) slot |= FilterPipes.bit(d);
        return slot;
    }

    /** The block state of a single pipe for a slot (its sides and its T-piece). */
    public BlockState stateOf(int slot) {
        BlockState s = defaultBlockState().setValue(JUNCTION, FilterPipes.junction(slot));
        for (Direction d : Direction.values()) s = s.setValue(SIDES.get(d), FilterPipes.joined(slot, d));
        return s;
    }

    /** Bumps the version (a filter part or something else a line reaches changed). */
    public static void touch() {
        version++;
    }

    /** Joins free sides (in this order) while the pipe has room. */
    BlockState connect(BlockState s, BlockGetter level, BlockPos pos, List<Direction> order) {
        return stateOf(FilterPipes.connect(line, slot(s), level, pos, order));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // the block it is placed against first, then the others
        List<Direction> order = new ArrayList<>();
        order.add(ctx.getClickedFace().getOpposite());
        for (Direction d : ctx.getNearestLookingDirections()) if (!order.contains(d)) order.add(d);
        for (Direction d : Direction.values()) if (!order.contains(d)) order.add(d);
        return connect(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos(), order);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos, Direction dir, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        version++;
        return state.setValue(SIDES.get(dir), FilterPipes.side(line, slot(state), level, pos, dir));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        version++;
        if (level.isClientSide() || old.is(this)) return;
        if (old.getBlock() instanceof FilterPipeBundleBlock) return; // a bundle down to its last line: its sides stay as they were
        // set directly (a structure, a command, the GameTests): join as if it had been placed by hand
        BlockState now = count(state) == 0 ? connect(state, level, pos, List.of(Direction.values())) : state;
        if (now != state) level.setBlock(pos, now, Block.UPDATE_ALL);
        FilterPipes.dock((ServerLevel) level, pos, line, slot(now));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        version++;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean moving) {
        super.neighborChanged(state, level, pos, block, orientation, moving);
        if (level.isClientSide()) return;
        version++;
        if (line.water()) {
            // part b: another mod's block next to it may get its fluid handler only after it was placed
            BlockState now = stateOf(FilterPipes.refresh(line, slot(state), level, pos));
            if (now != state) {
                level.setBlock(pos, now, Block.UPDATE_CLIENTS);
                state = now;
            }
        }
        FilterPipes.dock((ServerLevel) level, pos, line, slot(state));
    }

    // ------------------------------------------------------------------ the T-piece, a second line (part b)

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // part b: the pipe of another line goes into this block: it becomes a bundle that carries both
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof FilterPipeBlock other && other.line != line) {
            if (!level.isClientSide()) FilterPipeBundleBlock.addLine(level, pos, other.line, player, stack);
            return InteractionResult.SUCCESS;
        }
        InteractionResult facade = PipeCovers.useForeign(stack, level, pos, player, hand); // part c: another mod's facade
        if (facade != null) return facade;
        if (!(stack.getItem() instanceof PipeTeeItem)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (state.getValue(JUNCTION)) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.bettacraft.pipe_tee.already").withStyle(ChatFormatting.GOLD));
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            makeJunction(level, pos);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.COPPER_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.1F);
        }
        return InteractionResult.SUCCESS;
    }

    /** Turns the pipe at {@code pos} into a T-piece and joins every neighbour that fits (public for the GameTests; part b: a bundle's first line without one). */
    public static boolean makeJunction(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.getBlock() instanceof FilterPipeBundleBlock) {
            for (PipeLine l : FilterPipes.lines(level, pos)) if (!FilterPipes.junction(FilterPipes.at(level, pos, l))) return FilterPipeBundleBlock.makeJunction(level, pos, l);
            return false;
        }
        if (!(s.getBlock() instanceof FilterPipeBlock f) || s.getValue(JUNCTION)) return false;
        BlockState now = f.connect(s.setValue(JUNCTION, true), level, pos, List.of(Direction.values()));
        level.setBlock(pos, now, Block.UPDATE_ALL);
        if (level instanceof ServerLevel sl) FilterPipes.dock(sl, pos, f.line, slot(now));
        return true;
    }

    // ------------------------------------------------------------------ shape

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape s = state.getValue(JUNCTION) ? JUNCTION_CORE : CORE;
        for (var e : SIDES.entrySet()) if (state.getValue(e.getValue())) s = Shapes.or(s, ARMS.get(e.getKey()));
        return PipeCovers.shape(level, pos, s); // part c: a full block under a cover
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true; // part c: also under a cover (the light engine reads no block entities off the main thread)
    }

    /** Part c: the pipe (and its T-piece) from the loot table, and the block or facade of its cover. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        List<ItemStack> out = new ArrayList<>(super.getDrops(state, params));
        PipeCovers.addDrops(out, params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY));
        return out;
    }

    // ------------------------------------------------------------------ the cover (part c)

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

    public BlockState getAppearance(BlockState state, net.minecraft.client.renderer.block.BlockAndTintGetter level, BlockPos pos, Direction side, @Nullable BlockState queryState, @Nullable BlockPos queryPos) {
        BlockState c = PipeCovers.coverAt(level, pos);
        return c != null ? c : state;
    }

    @Override // BlockTooltip (M52b)
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext ctx, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        List<Component> tooltip = new SinkList<>(tooltipAdder);
        tooltip.add(Component.translatable(line.water() ? "tooltip.bettacraft.filter_water_pipe.usage" : "tooltip.bettacraft.filter_pipe.usage").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.bettacraft.filter_pipe.bundle").withStyle(ChatFormatting.DARK_GRAY)); // part b
    }
}
