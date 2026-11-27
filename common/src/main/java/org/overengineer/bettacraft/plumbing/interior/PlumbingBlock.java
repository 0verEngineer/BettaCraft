package org.overengineer.bettacraft.plumbing.interior;

import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.LevelReader;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.plumbing.PipeBlock;

/**
 * M24: a plumbing piece in a tank interior (scale 16, so one pixel thin in the miniature outside): the bulkhead's
 * connection on the floor ({@link Plumb#PORT}), interior pipes and the four outlets. Every piece joins every piece next to
 * it. Outlets have a facing (the direction of their current) and {@link #ACTIVE} while water flows through them (set by
 * {@link InteriorPlumbing#syncStates}); then they show their stream.
 */
public class PlumbingBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape CORE = box(6, 6, 6, 10, 10, 10);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Map.of(
            Direction.NORTH, box(6, 6, 0, 10, 10, 6), Direction.SOUTH, box(6, 6, 10, 10, 10, 16),
            Direction.WEST, box(0, 6, 6, 6, 10, 10), Direction.EAST, box(10, 6, 6, 16, 10, 10),
            Direction.DOWN, box(6, 0, 6, 10, 6, 10), Direction.UP, box(6, 10, 6, 10, 16, 10)));
    private static final VoxelShape PORT_SHAPE = Shapes.or(box(3, 0, 3, 13, 2, 13), box(6, 2, 6, 10, 8, 10));
    private final Plumb kind;

    public PlumbingBlock(Plumb kind, Properties properties) {
        super(properties);
        this.kind = kind;
        BlockState s = stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false);
        for (BooleanProperty p : PipeBlock.SIDES.values()) s = s.setValue(p, false);
        registerDefaultState(s);
    }

    public Plumb kind() {
        return kind;
    }

    /** The code of a state in the equipment index (0 = no plumbing). */
    public static int code(BlockState s) {
        if (!(s.getBlock() instanceof PlumbingBlock b)) return 0;
        if (b instanceof PipeAnchorBlock) return Plumb.code(b.kind, s.getValue(PipeAnchorBlock.MOUNT).get3DDataValue()); // M40
        return Plumb.code(b.kind, b.kind.outlet() ? s.getValue(FACING).get2DDataValue() : 0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        // the kind is not known yet while the state definition is built: every piece gets every property, pipes use the sides
        PipeBlock.SIDES.values().forEach(b::add);
        b.add(FACING, ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = defaultBlockState();
        // M65: every piece knows the pieces it joins (the guards, outlets and docks draw a short arm towards them)
        for (Direction d : Direction.values()) s = s.setValue(PipeBlock.SIDES.get(d), joins(ctx.getLevel(), ctx.getClickedPos(), d));
        // outlets point away from the player (into the tank) or along the wall they are placed against
        if (kind.outlet()) s = s.setValue(FACING, placedFacing(s, ctx));
        return s;
    }

    private static boolean guard(Plumb kind) {
        return kind == Plumb.INTAKE_GUARD || kind == Plumb.OUTLET_GUARD;
    }

    /**
     * M65: a guard placed next to a piece it joins faces away from it (the mesh side is the free one); sneaking while placing
     * keeps the look direction. Several joined pieces: the one on the clicked face, else the first horizontal one; only a
     * vertical joint: the look direction.
     */
    private Direction placedFacing(BlockState s, BlockPlaceContext ctx) {
        Direction look = ctx.getHorizontalDirection();
        if (!guard(kind) || (ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown())) return look;
        return awayFrom(s, ctx.getClickedFace().getOpposite(), look);
    }

    /** Pure: the facing opposite to the joined piece (on {@code preferred} if joined there, else the first horizontal one), else {@code fallback}. */
    public static Direction awayFrom(BlockState s, Direction preferred, Direction fallback) {
        if (preferred.getAxis().isHorizontal() && s.getValue(PipeBlock.SIDES.get(preferred))) return preferred.getOpposite();
        for (Direction d : Direction.Plane.HORIZONTAL) if (s.getValue(PipeBlock.SIDES.get(d))) return d.getOpposite();
        return fallback;
    }

    /** Pure: the next horizontal facing (clockwise) that does not point at a joined piece, or the same one when there is none. */
    public static Direction nextFacing(BlockState s) {
        Direction f = s.getValue(FACING);
        for (int i = 0; i < 3; i++) {
            f = f.getClockWise();
            if (!s.getValue(PipeBlock.SIDES.get(f))) return f;
        }
        return s.getValue(FACING);
    }

    /** M65: sneak + empty hand turns a guard to its next facing (not towards a joined piece). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!guard(kind) || !player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        return rotate(level, pos, state) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    /** Turns the guard at {@code pos} to its next facing; false when no other facing is free. Public for the GameTests. */
    public static boolean rotate(Level level, BlockPos pos, BlockState state) {
        Direction next = nextFacing(state);
        if (next == state.getValue(FACING)) return false;
        level.setBlock(pos, state.setValue(FACING, next), Block.UPDATE_ALL);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.COPPER_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.6F);
        return true;
    }

    static boolean joins(LevelAccessor level, BlockPos pos, Direction d) {
        return level.getBlockState(pos.relative(d)).getBlock() instanceof PlumbingBlock;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos, Direction dir, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        return state.setValue(PipeBlock.SIDES.get(dir), neighbor.getBlock() instanceof PlumbingBlock);
    }

    /** M65: placed by the game (a dock, a guard put on from outside): it looks at its neighbours once (they look at it themselves). */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (level.isClientSide() || old.is(state.getBlock())) return;
        BlockState now = state;
        for (Direction d : Direction.values()) now = now.setValue(PipeBlock.SIDES.get(d), joins(level, pos, d));
        if (now != state) level.setBlock(pos, now, Block.UPDATE_CLIENTS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (kind) {
            case PORT -> PORT_SHAPE;
            case PIPE -> {
                VoxelShape s = CORE;
                for (var e : PipeBlock.SIDES.entrySet()) if (state.getValue(e.getValue())) s = Shapes.or(s, ARMS.get(e.getKey()));
                yield s;
            }
            case STRAINER -> box(4, 0, 4, 12, 12, 12);
            case INTAKE_GUARD -> box(3, 3, 3, 13, 13, 13); // M40
            case OUTLET_GUARD -> box(4, 4, 4, 12, 12, 12);
            case ANCHOR -> PipeAnchorBlock.shape(state);
            case LILY -> box(4, 0, 4, 12, 16, 12);
            case NOZZLE -> box(5, 4, 5, 11, 12, 11);
            case SPRAY_BAR -> state.getValue(FACING).getAxis() == Direction.Axis.Z ? box(0, 6, 6, 16, 10, 10) : box(6, 6, 0, 10, 10, 16);
        };
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    /** M68: an interior pipe is a glass or steel pipe placed in the interior: picking it gives that pipe. */
    @Override
    protected net.minecraft.world.item.ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        if (kind == Plumb.PIPE) return new net.minecraft.world.item.ItemStack(this == org.overengineer.bettacraft.registry.ModBlocks.INTERIOR_STEEL_PIPE.get()
                ? org.overengineer.bettacraft.registry.ModItems.STAINLESS_STEEL_PIPE.get() : org.overengineer.bettacraft.registry.ModItems.GLASS_PIPE.get());
        return super.getCloneItemStack(level, pos, state, includeData);
    }

    /** The stream of a working outlet (in the interior). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!kind.outlet() || !state.getValue(ACTIVE)) return;
        // M40: the guards are filter openings: few, slow bubbles (the filter keeps the water clear, not a jet)
        if ((kind == Plumb.INTAKE_GUARD || kind == Plumb.OUTLET_GUARD) && r.nextInt(3) != 0) return;
        Direction f = state.getValue(FACING);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        int n = kind == Plumb.SPRAY_BAR ? 6 : 3;
        for (int i = 0; i < n; i++) {
            if (kind.intake()) {
                // particles drawn into the strainer
                double ox = (r.nextDouble() - 0.5) * 3, oy = r.nextDouble() * 2, oz = (r.nextDouble() - 0.5) * 3;
                level.addParticle(new org.overengineer.bettacraft.registry.ModParticles.AirBubble((float) y + 3, 0.12F), x + ox, y + oy, z + oz, -ox * 0.12, 0.01, -oz * 0.12);
                continue;
            }
            double spread = kind == Plumb.SPRAY_BAR ? 0.9 : kind == Plumb.LILY ? 0.3 : 0.05;
            double sx = f.getAxis() == Direction.Axis.X ? 0 : (r.nextDouble() - 0.5) * 2 * spread;
            double sz = f.getAxis() == Direction.Axis.Z ? 0 : (r.nextDouble() - 0.5) * 2 * spread;
            double speed = kind == Plumb.NOZZLE ? 0.6 : 0.3;
            double py = kind == Plumb.LILY ? pos.getY() + 0.9 : y;
            level.addParticle(new org.overengineer.bettacraft.registry.ModParticles.AirBubble((float) py + 6, 0.12F), x + f.getStepX() * 0.5 + sx, py, z + f.getStepZ() * 0.5 + sz,
                    f.getStepX() * speed + (r.nextDouble() - 0.5) * 0.05, (r.nextDouble() - 0.5) * 0.05, f.getStepZ() * speed + (r.nextDouble() - 0.5) * 0.05);
        }
    }
}
