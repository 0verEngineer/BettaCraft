package org.overengineer.bettacraft.plumbing;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * M40 part b (B2): a pipe bundle, two to four filter lines in one block like the conduits of EnderIO: the intake and the
 * return line, the fresh and the waste water line. It appears when the pipe of a second line is put into a filter pipe
 * (right-click with it); more lines go in the same way, a T-piece goes on the line it is used on. Sneaking with an empty
 * hand takes the line you look at out again (its pipe and its T-piece back); with one line left the block is a single
 * pipe again. Every line keeps its own sides and T-piece in the block entity ({@link FilterPipes}) and joins only pipes,
 * bundles and connectors of its line, exactly like a single pipe; the lines never mix.
 * <p>
 * Drawn as a steel manifold ({@link #HUB}) with one arm per joined side and line; the arms of a line sit at the same spot
 * of a face in every bundle (so bundles line up), {@link #arm}.
 * <p>
 * Part c: a cover ({@link PipeCovers}) draws it as a full block; shape, light, sound and drops follow the cover.
 */
public class FilterPipeBundleBlock extends BaseEntityBlock {
    public static final MapCodec<FilterPipeBundleBlock> CODEC = simpleCodec(FilterPipeBundleBlock::new);
    /** The manifold in the middle (in pixels). */
    public static final float HUB_MIN = 3.5F, HUB_MAX = 12.5F;
    /** Half the width of an arm and of a T-piece collar, the distance of an arm from the middle of its face. */
    public static final float ARM = 2F, COLLAR = 2.4F, OFFSET = 2.5F;
    private static final VoxelShape HUB = box(HUB_MIN, HUB_MIN, HUB_MIN, HUB_MAX, HUB_MAX, HUB_MAX);
    private static final Int2ObjectOpenHashMap<VoxelShape> SHAPES = new Int2ObjectOpenHashMap<>();

    public FilterPipeBundleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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

    // ------------------------------------------------------------------ geometry (shared with the model)

    /** The corner of the middle each line keeps to (x, y, z: -1 or +1; any two lines differ in two of them). */
    private static int[] signs(PipeLine l) {
        return switch (l) {
            case INTAKE -> new int[]{-1, 1, -1};
            case RETURN -> new int[]{1, 1, 1};
            case FRESH -> new int[]{-1, -1, 1};
            case WASTE -> new int[]{1, -1, -1};
        };
    }

    /**
     * The box (pixels: minX, minY, minZ, maxX, maxY, maxZ) of the arm of line {@code l} towards {@code d}: from the
     * manifold to the face, {@code half} wide around its spot on that face ({@link #ARM}; {@link #COLLAR} for the collar of
     * a T-piece, {@code depth} long from the manifold, 0 = to the face).
     */
    public static float[] arm(PipeLine l, Direction d, float half, float depth) {
        int[] sg = signs(l);
        float[] min = new float[3], max = new float[3];
        int axis = d.getAxis().ordinal();
        for (int i = 0; i < 3; i++) {
            if (i == axis) continue;
            float c = 8 + sg[i] * OFFSET;
            min[i] = Math.max(HUB_MIN, c - half); // never wider than the manifold (the collars of two sides would meet at its corner)
            max[i] = Math.min(HUB_MAX, c + half);
        }
        boolean pos = d.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        if (pos) {
            min[axis] = HUB_MAX;
            max[axis] = depth > 0 ? HUB_MAX + depth : 16;
        } else {
            min[axis] = depth > 0 ? HUB_MIN - depth : 0;
            max[axis] = HUB_MIN;
        }
        return new float[]{min[0], min[1], min[2], max[0], max[1], max[2]};
    }

    private static VoxelShape box(float[] b) {
        return box(b[0], b[1], b[2], b[3], b[4], b[5]);
    }

    /** The shape for packed lines (cached). */
    public static VoxelShape shapeOf(int lines) {
        synchronized (SHAPES) {
            VoxelShape s = SHAPES.get(lines);
            if (s != null) return s;
            s = HUB;
            for (PipeLine l : PipeLine.values()) {
                int slot = FilterPipes.slot(lines, l);
                for (Direction d : Direction.values()) if (FilterPipes.joined(slot, d)) s = Shapes.or(s, box(arm(l, d, ARM, 0)));
            }
            s = s.optimize();
            SHAPES.put(lines, s);
            return s;
        }
    }

    private static int linesAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FilterPipeBlockEntity be ? be.lines() : 0;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return PipeCovers.shape(level, pos, shapeOf(linesAt(level, pos))); // part c: a full block under a cover
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true; // part c: also under a cover (the light engine reads no block entities off the main thread)
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

    public BlockState getAppearance(BlockState state, net.minecraft.world.level.BlockAndTintGetter level, BlockPos pos, Direction side, @Nullable BlockState queryState, @Nullable BlockPos queryPos) {
        BlockState c = PipeCovers.coverAt(level, pos);
        return c != null ? c : state;
    }

    /** The line of the bundle closest to where it was hit (an arm, else the spot of the line next to the manifold). */
    @Nullable
    public static PipeLine lineAt(int lines, Vec3 local) {
        PipeLine best = null;
        double bd = Double.MAX_VALUE;
        for (PipeLine l : FilterPipes.lines(lines)) {
            int slot = FilterPipes.slot(lines, l);
            int[] sg = signs(l);
            // the corner of the manifold this line keeps to
            double d = new AABB((8 + sg[0] * OFFSET - ARM) / 16, (8 + sg[1] * OFFSET - ARM) / 16, (8 + sg[2] * OFFSET - ARM) / 16,
                    (8 + sg[0] * OFFSET + ARM) / 16, (8 + sg[1] * OFFSET + ARM) / 16, (8 + sg[2] * OFFSET + ARM) / 16).distanceToSqr(local) + 1e-4;
            for (Direction dir : Direction.values()) {
                if (!FilterPipes.joined(slot, dir)) continue;
                float[] b = arm(l, dir, ARM, 0);
                d = Math.min(d, new AABB(b[0] / 16, b[1] / 16, b[2] / 16, b[3] / 16, b[4] / 16, b[5] / 16).distanceToSqr(local));
            }
            if (d < bd) {
                bd = d;
                best = l;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ joining

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        FilterPipeBlock.touch();
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof FilterPipeBlockEntity be)) return state;
        int lines = be.lines(), now = lines;
        for (PipeLine l : PipeLine.values()) {
            int slot = FilterPipes.slot(lines, l);
            if (!FilterPipes.present(slot)) continue;
            boolean on = FilterPipes.side(l, slot, level, pos, dir);
            now = FilterPipes.with(now, l, on ? slot | FilterPipes.bit(dir) : slot & ~FilterPipes.bit(dir));
        }
        if (now != lines) be.setLines(now);
        return state;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof FilterPipeBlockEntity be)) return;
        FilterPipeBlock.touch();
        int lines = be.lines(), now = lines;
        for (PipeLine l : PipeLine.values()) {
            int slot = FilterPipes.slot(lines, l);
            if (FilterPipes.present(slot) && l.water()) now = FilterPipes.with(now, l, FilterPipes.refresh(l, slot, level, pos));
        }
        if (now != lines) be.setLines(now);
        for (PipeLine l : FilterPipes.lines(now)) FilterPipes.dock((ServerLevel) level, pos, l, FilterPipes.slot(now, l));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        FilterPipeBlock.touch();
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        FilterPipeBlock.touch();
        super.onRemove(state, level, pos, newState, moving);
    }

    /** After the lines of the block changed: its neighbours look at it again, other mods see its fluid handler change. */
    private static void after(Level level, BlockPos pos) {
        FilterPipeBlock.touch();
        org.overengineer.bettacraft.platform.Platform.get().invalidateCapabilities(level, pos);
        BlockState s = level.getBlockState(pos);
        s.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
        level.updateNeighborsAt(pos, s.getBlock());
        if (level instanceof ServerLevel sl) {
            int lines = FilterPipes.packed(level, pos, s);
            for (PipeLine l : FilterPipes.lines(lines)) FilterPipes.dock(sl, pos, l, FilterPipes.slot(lines, l));
        }
    }

    /** The single pipe block of a line. */
    public static FilterPipeBlock pipeOf(PipeLine l) {
        return switch (l) {
            case INTAKE -> ModBlocks.FILTER_INLET_PIPE.get();
            case RETURN -> ModBlocks.FILTER_OUTLET_PIPE.get();
            case FRESH -> ModBlocks.FILTER_FRESH_PIPE.get();
            case WASTE -> ModBlocks.FILTER_WASTE_PIPE.get();
        };
    }

    /**
     * Puts the pipe of {@code line} into the filter pipe or bundle at {@code pos} (public for the GameTests; {@code player}
     * and {@code stack} may be null). The new line joins its own pipes, its connector, tanks or other mods' blocks next to it.
     */
    public static boolean addLine(Level level, BlockPos pos, PipeLine line, @Nullable Player player, @Nullable ItemStack stack) {
        if (!Features.enabled(Feature.FILTER_LINES)) {
            if (player != null) player.displayClientMessage(Component.translatable("message.bettacraft.feature_off").withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        BlockState s = level.getBlockState(pos);
        if (!FilterPipes.isPipe(s)) return false;
        int lines = FilterPipes.packed(level, pos, s);
        if (FilterPipes.present(FilterPipes.slot(lines, line))) {
            if (player != null) player.displayClientMessage(Component.translatable("message.bettacraft.pipe_bundle.has_line").withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        if (s.getBlock() instanceof FilterPipeBlock) {
            BlockState cover = level.getBlockEntity(pos) instanceof FilterPipeBlockEntity be ? be.cover() : null;
            ItemStack from = level.getBlockEntity(pos) instanceof FilterPipeBlockEntity be ? be.coverItem() : ItemStack.EMPTY;
            // without shape updates first: the neighbours look at the bundle once it carries its lines
            level.setBlock(pos, ModBlocks.FILTER_PIPE_BUNDLE.get().defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            if (!(level.getBlockEntity(pos) instanceof FilterPipeBlockEntity be)) return false;
            be.init(cover, from, lines);
        }
        if (!(level.getBlockEntity(pos) instanceof FilterPipeBlockEntity be)) return false;
        int slot = FilterPipes.connectNew(line, FilterPipes.PRESENT, level, pos);
        be.setLines(FilterPipes.with(be.lines(), line, slot));
        level.sendBlockUpdated(pos, s, level.getBlockState(pos), Block.UPDATE_CLIENTS);
        after(level, pos);
        if (player != null && stack != null && !player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, pos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 0.8F, 0.9F);
        return true;
    }

    /** Takes {@code line} out of the bundle at {@code pos}; returns what it gives back (its pipe, its T-piece). Public for the GameTests. */
    public static List<ItemStack> removeLine(Level level, BlockPos pos, PipeLine line) {
        List<ItemStack> out = new ArrayList<>();
        if (!(level.getBlockState(pos).getBlock() instanceof FilterPipeBundleBlock) || !(level.getBlockEntity(pos) instanceof FilterPipeBlockEntity be)) return out;
        int lines = be.lines(), slot = FilterPipes.slot(lines, line);
        if (!FilterPipes.present(slot)) return out;
        out.add(new ItemStack(pipeOf(line)));
        if (FilterPipes.junction(slot)) out.add(new ItemStack(ModItems.PIPE_TEE.get()));
        int now = FilterPipes.with(lines, line, 0);
        var left = FilterPipes.lines(now);
        if (left.size() == 1) {
            PipeLine l = left.iterator().next();
            BlockState cover = be.cover();
            ItemStack from = be.coverItem();
            level.setBlock(pos, pipeOf(l).stateOf(FilterPipes.slot(now, l)), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            if (level.getBlockEntity(pos) instanceof FilterPipeBlockEntity single) single.init(cover, from, 0);
        } else {
            be.setLines(now);
        }
        after(level, pos);
        level.playSound(null, pos, SoundEvents.COPPER_BREAK, SoundSource.BLOCKS, 0.8F, 1.0F);
        return out;
    }

    /** Puts a T-piece on {@code line} of the bundle at {@code pos}: it joins every neighbour of that line that fits. */
    public static boolean makeJunction(Level level, BlockPos pos, PipeLine line) {
        if (!(level.getBlockEntity(pos) instanceof FilterPipeBlockEntity be)) return false;
        int slot = FilterPipes.slot(be.lines(), line);
        if (!FilterPipes.present(slot) || FilterPipes.junction(slot)) return false;
        slot = FilterPipes.connect(line, slot | FilterPipes.JUNCTION, level, pos, List.of(Direction.values()));
        be.setLines(FilterPipes.with(be.lines(), line, slot));
        after(level, pos);
        return true;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof FilterPipeBlock f) {
            if (!level.isClientSide) addLine(level, pos, f.line(), player, stack);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        ItemInteractionResult facade = PipeCovers.useForeign(stack, level, pos, player, hand); // part c: another mod's facade
        if (facade != null) return facade;
        if (stack.getItem() instanceof PipeTeeItem) {
            if (!level.isClientSide) {
                PipeLine l = lineAt(linesAt(level, pos), hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)));
                if (l == null) return ItemInteractionResult.FAIL;
                if (FilterPipes.junction(FilterPipes.at(level, pos, l))) {
                    player.displayClientMessage(Component.translatable("message.bettacraft.pipe_tee.already").withStyle(ChatFormatting.GOLD), true);
                } else if (makeJunction(level, pos, l)) {
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    level.playSound(null, pos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 0.8F, 1.1F);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            PipeLine l = lineAt(linesAt(level, pos), hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)));
            if (l == null) return InteractionResult.FAIL;
            for (ItemStack it : removeLine(level, pos, l)) {
                if (player.getAbilities().instabuild) continue;
                if (!player.addItem(it)) player.drop(it, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ------------------------------------------------------------------ drops, pick block

    /** What breaking a bundle gives: the pipe of every line and the T-pieces on them. */
    public static List<ItemStack> drops(int lines) {
        List<ItemStack> out = new ArrayList<>();
        for (PipeLine l : FilterPipes.lines(lines)) {
            out.add(new ItemStack(pipeOf(l)));
            if (FilterPipes.junction(FilterPipes.slot(lines, l))) out.add(new ItemStack(ModItems.PIPE_TEE.get()));
        }
        return out;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(be instanceof FilterPipeBlockEntity f)) return List.of();
        List<ItemStack> out = drops(f.lines());
        PipeCovers.addDrops(out, f); // part c: the block or facade of its cover
        return out;
    }

    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        int lines = linesAt(level, pos);
        PipeLine l = target instanceof BlockHitResult b ? lineAt(lines, b.getLocation().subtract(Vec3.atLowerCornerOf(pos))) : null;
        return l == null ? new ItemStack(ModItems.FILTER_INLET_PIPE.get()) : new ItemStack(pipeOf(l));
    }

    /** The id of the item of a line's pipe (for texts). */
    public static ResourceLocation pipeId(PipeLine l) {
        return BettaCraft.id(l.blockId());
    }

    static Item item(PipeLine l) {
        return BuiltInRegistries.ITEM.get(pipeId(l));
    }
}
