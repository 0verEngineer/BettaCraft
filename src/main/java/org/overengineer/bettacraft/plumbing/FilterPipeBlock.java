package org.overengineer.bettacraft.plumbing;

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
import net.minecraft.world.ItemInteractionResult;
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
 */
public class FilterPipeBlock extends BaseEntityBlock {
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

    /** What is on side {@code d} of a pipe of {@code line}: a pipe of the same line, a tank, its connector, or nothing it joins. */
    enum Target { NONE, PIPE, TANK, CONNECTOR }

    static Target target(PipeLine line, BlockState n, Direction d) {
        if (n.getBlock() instanceof FilterPipeBlock f) return f.line == line ? Target.PIPE : Target.NONE;
        if (n.getBlock() instanceof TankBlock) return Target.TANK;
        if (n.getBlock() instanceof FilterPartBlock p && p.part() == line.connector() && n.getValue(FilterPartBlock.FACING) == d.getOpposite()) return Target.CONNECTOR;
        return Target.NONE;
    }

    /** Whether a new connection on side {@code d} can be made (the neighbour pipe has room or is already joined to us). */
    private boolean canJoin(BlockGetter level, BlockPos pos, Direction d) {
        BlockState n = level.getBlockState(pos.relative(d));
        Target t = target(line, n, d);
        if (t == Target.NONE) return false;
        if (t == Target.PIPE) return n.getValue(SIDES.get(d.getOpposite())) || count(n) < limit(n);
        return true;
    }

    /** Joins free sides (in this order) while the pipe has room. */
    BlockState connect(BlockState s, BlockGetter level, BlockPos pos, List<Direction> order) {
        for (Direction d : order) {
            if (count(s) >= limit(s)) break;
            if (!s.getValue(SIDES.get(d)) && canJoin(level, pos, d)) s = s.setValue(SIDES.get(d), true);
        }
        return s;
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
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        version++;
        BooleanProperty p = SIDES.get(dir);
        Target t = target(line, neighbor, dir);
        boolean on;
        if (t == Target.PIPE) on = neighbor.getValue(SIDES.get(dir.getOpposite())); // the neighbour pipe decided when it was placed
        else if (t == Target.NONE) on = false;
        else on = state.getValue(p) || count(state) < limit(state); // a tank or the connector appeared: joined while there is room
        return state.setValue(p, on);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        version++;
        if (level.isClientSide || old.is(this)) return;
        // set directly (a structure, a command, the GameTests): join as if it had been placed by hand
        BlockState now = count(state) == 0 ? connect(state, level, pos, List.of(Direction.values())) : state;
        if (now != state) level.setBlock(pos, now, Block.UPDATE_ALL);
        dock((ServerLevel) level, pos, now);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        version++;
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        if (!level.isClientSide) dock((ServerLevel) level, pos, state);
    }

    /** E2: where the pipe docks on a tank, its connection appears in the interior right away. */
    private static void dock(ServerLevel level, BlockPos pos, BlockState s) {
        if (!Features.enabled(Feature.FILTER_LINES)) return;
        for (Direction d : Direction.values()) {
            if (!s.getValue(SIDES.get(d))) continue;
            BlockPos n = pos.relative(d);
            if (!(level.getBlockState(n).getBlock() instanceof TankBlock)) continue;
            TankBlockEntity c = TankMultiblock.getController(level, n);
            if (c == null) continue;
            if (org.overengineer.bettacraft.plumbing.interior.TankAnchors.existing(level, c, n, d.getOpposite()) == Long.MIN_VALUE)
                org.overengineer.bettacraft.plumbing.interior.TankAnchors.ensure(level, c, n, d.getOpposite());
        }
    }

    // ------------------------------------------------------------------ the T-piece

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof PipeTeeItem)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (state.getValue(JUNCTION)) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.bettacraft.pipe_tee.already").withStyle(ChatFormatting.GOLD), true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            makeJunction(level, pos);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.COPPER_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.1F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Turns the pipe at {@code pos} into a T-piece and joins every neighbour that fits (public for the GameTests). */
    public static boolean makeJunction(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof FilterPipeBlock f) || s.getValue(JUNCTION)) return false;
        BlockState now = f.connect(s.setValue(JUNCTION, true), level, pos, List.of(Direction.values()));
        level.setBlock(pos, now, Block.UPDATE_ALL);
        if (level instanceof ServerLevel sl) dock(sl, pos, now);
        return true;
    }

    // ------------------------------------------------------------------ shape

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape s = state.getValue(JUNCTION) ? JUNCTION_CORE : CORE;
        for (var e : SIDES.entrySet()) if (state.getValue(e.getValue())) s = Shapes.or(s, ARMS.get(e.getKey()));
        return s;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bettacraft.filter_pipe.usage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
