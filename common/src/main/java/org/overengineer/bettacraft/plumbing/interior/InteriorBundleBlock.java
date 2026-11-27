package org.overengineer.bettacraft.plumbing.interior;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.plumbing.PipeBlock;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;

/**
 * M68 part C: an interior pipe bundle: two to four separate lines ("lanes") of interior pipes in one block of a tank interior,
 * so the lines that leave the docks of a pipe bundle can run on together up to where they part. Made like a bundle outside:
 * sneak + right-click an interior pipe (or bundle) with a glass or steel pipe adds a lane (up to four, pipes of one kind);
 * sneak + empty hand takes the last lane out again (one left: a plain interior pipe).
 * <p>
 * Everything is in the block state, so the index ({@link InteriorPlumbing#search}) and the miniature outside (which has no block
 * entities) know it: the run's {@link #AXIS} and the number of {@link #LANES}. Lane 0 is the middle one: it runs along the axis
 * and goes on into a single piece (a pipe, a dock, a guard, an outlet) at either end, like an interior pipe. Lanes 1 to 3 sit
 * beside it, each towards its own side ({@link #laneSide}), and leave the run only there, into a single piece on that side.
 * Along the axis every lane goes on into the same lane of the next bundle of the same axis. So a line can join the run at one
 * block and part from it at another, and lines never meet inside it.
 */
public class InteriorBundleBlock extends PlumbingBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final IntegerProperty LANES = IntegerProperty.create("lanes", 2, 4);
    /** Pieces of a lane ({@link InteriorLanes#box}). */
    public static final int ROD = InteriorLanes.ROD, END_NEG = InteriorLanes.END_NEG, END_POS = InteriorLanes.END_POS, ARM = InteriorLanes.ARM;
    private static final Map<BlockState, VoxelShape> SHAPES = new ConcurrentHashMap<>();
    private final boolean steel;

    public InteriorBundleBlock(boolean steel, Properties properties) {
        super(Plumb.BUNDLE, properties);
        this.steel = steel;
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.X).setValue(LANES, 2));
    }

    public boolean steel() {
        return steel;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        PipeBlock.SIDES.values().forEach(b::add);
        b.add(AXIS, LANES);
    }

    // ------------------------------------------------------------------ the rule ({@link InteriorLanes}, the same as InteriorPlumbing#search)

    public static Direction laneSide(Direction.Axis axis, int lane) {
        return InteriorLanes.laneSide(axis, lane);
    }

    public static int laneOn(Direction.Axis axis, int lanes, Direction d) {
        return InteriorLanes.laneOn(axis, lanes, d);
    }

    /** The lane of the bundle state {@code s} a single piece on its side {@code d} joins, or -1. */
    public static int laneOn(BlockState s, Direction d) {
        return InteriorLanes.laneOn(s.getValue(AXIS), s.getValue(LANES), d);
    }

    public static int laneOn(int code, Direction d) {
        return InteriorLanes.laneOn(code, d);
    }

    public static Direction.Axis axisOf(int code) {
        return InteriorLanes.axisOf(code);
    }

    public static Direction.Axis axisFor(int sides, Direction.Axis clicked) {
        return InteriorLanes.axisFor(sides, clicked);
    }

    public static float[] box(Direction.Axis axis, int lane, int piece) {
        return InteriorLanes.box(axis, lane, piece);
    }

    /** Pure: whether the bundle {@code self} joins the piece {@code n} on its side {@code d}. */
    static boolean bundleJoins(BlockState self, BlockState n, Direction d) {
        Direction.Axis a = self.getValue(AXIS);
        boolean bundle = n.getBlock() instanceof InteriorBundleBlock;
        if (d.getAxis() == a) return !bundle || n.getValue(AXIS) == a; // lane 0 into a single piece, every lane into the next bundle of the run
        return !bundle && laneOn(self, d) >= 1;
    }

    static VoxelShape shape(BlockState s) {
        return SHAPES.computeIfAbsent(s, st -> {
            Direction.Axis a = st.getValue(AXIS);
            Direction neg = Direction.fromAxisAndDirection(a, Direction.AxisDirection.NEGATIVE), pos = neg.getOpposite();
            VoxelShape out = Shapes.empty();
            for (int lane = 0; lane < st.getValue(LANES); lane++) {
                out = Shapes.or(out, box(box(a, lane, ROD)));
                if (st.getValue(PipeBlock.SIDES.get(neg))) out = Shapes.or(out, box(box(a, lane, END_NEG)));
                if (st.getValue(PipeBlock.SIDES.get(pos))) out = Shapes.or(out, box(box(a, lane, END_POS)));
                if (lane > 0 && st.getValue(PipeBlock.SIDES.get(laneSide(a, lane)))) out = Shapes.or(out, box(box(a, lane, ARM)));
            }
            return out.optimize();
        });
    }

    private static VoxelShape box(float[] b) {
        return Block.box(b[0], b[1], b[2], b[3], b[4], b[5]);
    }

    // ------------------------------------------------------------------ making and taking apart

    private static void say(@Nullable Player p, String key) {
        if (p != null) p.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD));
    }

    /** The bundle block of a pipe kind. */
    public static InteriorBundleBlock of(boolean steel) {
        return steel ? ModBlocks.INTERIOR_STEEL_PIPE_BUNDLE.get() : ModBlocks.INTERIOR_PIPE_BUNDLE.get();
    }

    /** Whether the interior piece {@code s} is of steel (pipes and bundles), null for other pieces. */
    @Nullable
    public static Boolean steelOf(BlockState s) {
        if (s.getBlock() instanceof InteriorBundleBlock b) return b.steel;
        if (s.is(ModBlocks.INTERIOR_STEEL_PIPE.get())) return true;
        if (s.is(ModBlocks.INTERIOR_PIPE.get())) return false;
        return null;
    }

    /** {@code s} with its joined sides towards the pieces around {@code pos}. */
    static BlockState withSides(Level level, BlockPos pos, BlockState s) {
        for (Direction d : Direction.values()) s = s.setValue(PipeBlock.SIDES.get(d), PlumbingBlock.joins(s, level.getBlockState(pos.relative(d)), d));
        return s;
    }

    /**
     * Puts a glass or steel pipe into the interior pipe or bundle at {@code pos} (sneak + right-click with the pipe; public for the
     * GameTests, {@code player} and {@code stack} may be null): a pipe becomes a bundle of two lanes (its line goes on in lane 0),
     * a bundle gets one more lane. Returns the new lane count, or -1 (full, another kind of pipe, a lane that would lead into the
     * tank and out again, no interior piece).
     */
    public static int addPipe(Level level, BlockPos pos, boolean steel, @Nullable Player player, @Nullable ItemStack stack, Direction clicked) {
        if (!Features.enabled(Feature.INTERIOR_PIPES)) {
            say(player, "message.bettacraft.feature_off");
            return -1;
        }
        BlockState s = level.getBlockState(pos);
        Boolean kind = steelOf(s);
        if (kind == null) return -1;
        if (kind != steel) {
            say(player, "message.bettacraft.interior_bundle.kind");
            return -1;
        }
        if (player != null && !InteriorManager.canBuild(player, pos)) return -1;
        BlockState next;
        if (s.getBlock() instanceof InteriorBundleBlock) {
            if (s.getValue(LANES) >= 4) {
                say(player, "message.bettacraft.pipe_bundle.full");
                return -1;
            }
            next = s.setValue(LANES, s.getValue(LANES) + 1);
        } else {
            int sides = 0;
            for (Direction d : Direction.values()) if (s.getValue(PipeBlock.SIDES.get(d))) sides |= 1 << d.get3DDataValue();
            next = of(steel).defaultBlockState().setValue(AXIS, axisFor(sides, clicked.getAxis())).setValue(LANES, 2);
        }
        next = withSides(level, pos, next);
        if (level instanceof ServerLevel sl && passThrough(sl, pos, next)) {
            say(player, "message.bettacraft.plumbing.pass_through");
            return -1;
        }
        level.setBlock(pos, next, Block.UPDATE_ALL);
        if (player != null && stack != null && !player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, pos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 0.6F, 1.3F);
        return next.getValue(LANES);
    }

    /** Takes the last lane out of the bundle at {@code pos} (two lanes: a plain interior pipe stays); returns its pipe (empty if none). Public for the GameTests. */
    public static ItemStack removeLane(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof InteriorBundleBlock b)) return ItemStack.EMPTY;
        int lanes = s.getValue(LANES);
        BlockState next = lanes > 2 ? s.setValue(LANES, lanes - 1)
                : (b.steel ? ModBlocks.INTERIOR_STEEL_PIPE.get() : ModBlocks.INTERIOR_PIPE.get()).defaultBlockState();
        level.setBlock(pos, withSides(level, pos, next), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.COPPER_BREAK, SoundSource.BLOCKS, 0.6F, 1.2F);
        return new ItemStack(b.steel ? ModItems.STAINLESS_STEEL_PIPE.get() : ModItems.GLASS_PIPE.get());
    }

    /** Whether the bundle state {@code next} at {@code pos} would join two connections to the outside in one of its lanes. */
    static boolean passThrough(ServerLevel interior, BlockPos pos, BlockState next) {
        InteriorRegion r = InteriorManager.regionAt(interior.getServer(), pos);
        if (r == null || !r.built()) return false;
        Long2IntOpenHashMap m = new Long2IntOpenHashMap(r.equipment().plumbing());
        long k = BlockPos.asLong(r.localX(pos.getX()), r.localY(pos.getY()), r.localZ(pos.getZ()));
        m.put(k, PlumbingBlock.code(next));
        for (int lane = 0; lane < next.getValue(LANES); lane++) if (InteriorPlumbing.search(m, k, lane).ports().size() >= 2) return true;
        return false;
    }

    /** Sneak + empty hand takes the last lane out (into the hand). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!InteriorManager.canBuild(player, pos)) return InteractionResult.FAIL;
        ItemStack out = removeLane(level, pos);
        if (!out.isEmpty() && !player.getAbilities().instabuild && !player.getInventory().add(out)) player.drop(out, false);
        return InteractionResult.CONSUME;
    }
}
