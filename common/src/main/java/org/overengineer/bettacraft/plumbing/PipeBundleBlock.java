package org.overengineer.bettacraft.plumbing;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * M40 part b (B2), M68: a pipe bundle, two to four pipes in one block like the conduits of EnderIO. It appears when a pipe is
 * put into a pipe (sneak + right-click it with a glass or steel pipe, {@link PipeItem}); more go in the same way, up to four.
 * Each pipe is a slot ({@link PipeSlots}): glass or steel, its own joined and closed sides and the ring of its own network;
 * two pipes of a bundle never mix, also of the same role (two intake lines side by side). Sneaking with an empty hand takes the
 * pipe you look at out again; with one left the block is a single pipe again. Sneaking with the pipe wrench moves the pipe you
 * look at to the next spot ({@link #reslot}), so it lines up with the next bundle.
 * <p>
 * The block id stays {@code filter_pipe_bundle} (M40 saves). An M40 bundle's four filter lines are its slots in the old order
 * (intake, return, fresh, waste), as steel pipes with exactly their old connections ({@link PipeBlockEntity}).
 * <p>
 * Drawn as a steel manifold ({@link #HUB_MIN}..{@link #HUB_MAX}) with one arm per joined side and slot; the arms of a slot sit
 * at the same spot of a face in every bundle (so bundles line up slot to slot), {@link #arm}. M65: where one slot alone joins a
 * side whose neighbour is no bundle (a single pipe, a port, a tank) its arm leaves the manifold in the middle of the face
 * ({@link #centredSides}). A cover ({@link PipeCovers}) draws it as a full block.
 */
public class PipeBundleBlock extends BaseEntityBlock implements org.overengineer.bettacraft.util.BlockRemoval, org.overengineer.bettacraft.platform.PickableBlock, org.overengineer.bettacraft.platform.PositionalBlock.Sound {
    /** The manifold in the middle (in pixels). */
    public static final float HUB_MIN = 4.625F, HUB_MAX = 11.375F;
    /** Half the width of an arm and of a cap, the distance of an arm from the middle of its face. */
    public static final float ARM = 1.5F, COLLAR = 1.8F, OFFSET = 1.875F;
    private static final VoxelShape HUB = box(HUB_MIN, HUB_MIN, HUB_MIN, HUB_MAX, HUB_MAX, HUB_MAX);
    private static final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<VoxelShape> SHAPES = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();

    public PipeBundleBlock(Properties properties) {
        super(properties);
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

    // ------------------------------------------------------------------ geometry (shared with the model)

    /** The corner of the middle each slot keeps to (x, y, z: -1 or +1; any two slots differ in two of them). */
    public static int[] signs(int slot) {
        return switch (slot) {
            case 0 -> new int[]{-1, 1, -1};
            case 1 -> new int[]{1, 1, 1};
            case 2 -> new int[]{-1, -1, 1};
            default -> new int[]{1, -1, -1};
        };
    }

    /**
     * The box (pixels: minX, minY, minZ, maxX, maxY, maxZ) of the arm of slot {@code slot} towards {@code d}: from the manifold
     * to the face, {@code half} wide around its spot on that face, {@code depth} long from the manifold (0 = to the face);
     * {@code centred}: in the middle of the face (towards a single pipe, {@link #centredSides}).
     */
    public static float[] arm(int slot, Direction d, float half, float depth, boolean centred) {
        int[] sg = centred ? new int[3] : signs(slot);
        float[] min = new float[3], max = new float[3];
        int axis = d.getAxis().ordinal();
        for (int i = 0; i < 3; i++) {
            if (i == axis) continue;
            float c = 8 + sg[i] * OFFSET;
            min[i] = Math.max(HUB_MIN, c - half);
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

    public static float[] arm(int slot, Direction d, float half, float depth) {
        return arm(slot, d, half, depth, false);
    }

    private static VoxelShape box(float[] b) {
        return box(b[0], b[1], b[2], b[3], b[4], b[5]);
    }

    /** The shape for these slots with the arms on the {@link #centredSides} in the middle (cached). */
    public static VoxelShape shapeOf(int[] slots, int centred) {
        long key = centred;
        for (int i = 0; i < PipeSlots.COUNT; i++) key = key << 7 | (PipeSlots.present(slots[i]) ? 64 | PipeSlots.sides(slots[i]) : 0);
        synchronized (SHAPES) {
            VoxelShape s = SHAPES.get(key);
            if (s != null) return s;
            s = HUB;
            for (int i = 0; i < PipeSlots.COUNT; i++) {
                if (!PipeSlots.present(slots[i])) continue;
                for (Direction d : Direction.values()) if (PipeSlots.joined(slots[i], d)) s = Shapes.or(s, box(arm(i, d, ARM, 0, (centred & 1 << d.ordinal()) != 0)));
            }
            s = s.optimize();
            SHAPES.put(key, s);
            return s;
        }
    }

    /**
     * M65: the sides (bit = direction ordinal) where exactly one slot joins and the neighbour is no bundle (a single pipe, a port,
     * a tank): there the arm is drawn in the middle of the face, in line with the single pipe. Between two bundles every slot
     * keeps its corner, so they line up.
     */
    public static int centredSides(BlockGetter level, BlockPos pos, int[] slots) {
        int out = 0;
        for (Direction d : Direction.values()) {
            int n = 0;
            for (int s : slots) if (PipeSlots.present(s) && PipeSlots.joined(s, d)) n++;
            if (n == 1 && !(level.getBlockState(pos.relative(d)).getBlock() instanceof PipeBundleBlock)) out |= 1 << d.ordinal();
        }
        return out;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int[] slots = Pipes.slotsAt(level, pos);
        return PipeCovers.shape(level, pos, shapeOf(slots, centredSides(level, pos, slots)));
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    /** The slot closest to where it was hit (an arm, else the slot's corner of the manifold); -1 without any. */
    public static int slotAt(int[] slots, int centred, Vec3 local) {
        int best = -1;
        double bd = Double.MAX_VALUE;
        for (int i = 0; i < slots.length; i++) {
            if (!PipeSlots.present(slots[i])) continue;
            int[] sg = signs(i);
            double d = new AABB((8 + sg[0] * OFFSET - ARM) / 16, (8 + sg[1] * OFFSET - ARM) / 16, (8 + sg[2] * OFFSET - ARM) / 16,
                    (8 + sg[0] * OFFSET + ARM) / 16, (8 + sg[1] * OFFSET + ARM) / 16, (8 + sg[2] * OFFSET + ARM) / 16).distanceToSqr(local) + 1e-4;
            for (Direction dir : Direction.values()) {
                if (!PipeSlots.joined(slots[i], dir)) continue;
                float[] b = arm(i, dir, ARM, 0, (centred & 1 << dir.ordinal()) != 0);
                d = Math.min(d, new AABB(b[0] / 16, b[1] / 16, b[2] / 16, b[3] / 16, b[4] / 16, b[5] / 16).distanceToSqr(local));
            }
            if (d < bd) {
                bd = d;
                best = i;
            }
        }
        return best;
    }

    /** The slot of the bundle at {@code pos} that {@code hit} (world coordinates) points at (-1: none). */
    public static int slotHit(BlockGetter level, BlockPos pos, Vec3 hit) {
        int[] slots = Pipes.slotsAt(level, pos);
        return slotAt(slots, centredSides(level, pos, slots), hit.subtract(Vec3.atLowerCornerOf(pos)));
    }

    // ------------------------------------------------------------------ joining

    /** The slots with the sides in {@code dirs} looked at again ({@link Pipes#canJoin}). */
    static int[] rejoin(LevelAccessor level, BlockPos pos, int[] slots, Direction... dirs) {
        int[] now = slots.clone();
        for (Direction d : dirs) {
            boolean[] on = new boolean[slots.length];
            // every slot decides on the slots as they were before this side (the facing slot must not move while we look)
            for (int i = 0; i < slots.length; i++) on[i] = PipeSlots.present(slots[i]) && Pipes.canJoin(level, pos, i, d, PipeSlots.joined(slots[i], d));
            for (int i = 0; i < slots.length; i++) {
                if (!PipeSlots.present(slots[i])) continue;
                int sides = on[i] ? PipeSlots.sides(now[i]) | PipeSlots.bit(d) : PipeSlots.sides(now[i]) & ~PipeSlots.bit(d);
                now[i] = PipeSlots.withSides(now[i], sides);
            }
        }
        return now;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, net.minecraft.world.level.ScheduledTickAccess tickAccess, BlockPos pos, Direction dir,
                                     BlockPos neighborPos, BlockState neighbor, net.minecraft.util.RandomSource random) {
        if (!(level instanceof Level l) || l.isClientSide() || !(level.getBlockEntity(pos) instanceof PipeBlockEntity be)) return state;
        PipeNetwork.invalidate(l, pos);
        PipeUpdates.schedule(l, pos, false);
        be.setSlots(rejoin(l, pos, be.slots(), dir));
        return state;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean moving) {
        super.neighborChanged(state, level, pos, block, orientation, moving);
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof PipeBlockEntity be)) return;
        PipeNetwork.invalidate(level, pos);
        PipeUpdates.schedule(level, pos, false);
        int[] now = rejoin(level, pos, be.slots(), Direction.values());
        if (!java.util.Arrays.equals(now, be.slots())) {
            be.setSlots(now);
            after(level, pos);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        PipeNetwork.invalidate(level, pos);
        if (!level.isClientSide()) PipeUpdates.schedule(level, pos, false);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        PipeNetwork.invalidate(level, pos);
        if (!level.isClientSide()) PipeUpdates.schedule(level, pos, false);
    }

    /** After the pipes of the block changed: its neighbours look at it again, other mods see its fluid handler change. */
    static void after(Level level, BlockPos pos) {
        PipeNetwork.invalidate(level, pos);
        org.overengineer.bettacraft.platform.Platform.get().invalidateCapabilities(level, pos);
        BlockState s = level.getBlockState(pos);
        s.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
        level.updateNeighborsAt(pos, s.getBlock());
        if (!level.isClientSide()) PipeUpdates.schedule(level, pos, false);
    }

    /** The item of a glass or a steel pipe. */
    public static ItemStack pipeItem(boolean steel) {
        return new ItemStack(steel ? ModItems.STAINLESS_STEEL_PIPE.get() : ModItems.GLASS_PIPE.get());
    }

    /**
     * Puts a glass or steel pipe into the pipe or bundle at {@code pos} (public for the GameTests; {@code player} and
     * {@code stack} may be null): into the first free slot; the new pipe joins what fits ({@link Pipes#canJoin}: the same slot
     * of a neighbouring bundle, tanks, and the sides where it is the slot that faces the neighbour). Returns the slot, -1 when
     * the block is full or no pipe.
     */
    public static int addPipe(Level level, BlockPos pos, boolean steel, @Nullable Player player, @Nullable ItemStack stack) {
        if (!Features.enabled(Feature.PIPES)) {
            if (player != null) player.sendOverlayMessage(Component.translatable("message.bettacraft.feature_off").withStyle(ChatFormatting.GOLD));
            return -1;
        }
        BlockState s = level.getBlockState(pos);
        if (!Pipes.isPipe(s) || !(level.getBlockEntity(pos) instanceof PipeBlockEntity old)) return -1;
        int[] slots;
        if (s.getBlock() instanceof PipeBlock p) {
            slots = new int[PipeSlots.COUNT];
            slots[0] = PipeSlots.of(p.steel(), PipeBlock.sides(s), old.closed(), s.getValue(PipeBlock.RING));
            BlockState cover = old.cover();
            ItemStack from = old.coverItem();
            // without shape updates first: the neighbours look at the bundle once it carries its pipes
            level.setBlock(pos, ModBlocks.FILTER_PIPE_BUNDLE.get().defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            if (!(level.getBlockEntity(pos) instanceof PipeBlockEntity be)) return -1;
            be.init(cover, from, 0, slots);
        } else {
            slots = old.slots();
        }
        int free = -1;
        for (int i = 0; i < slots.length && free < 0; i++) if (!PipeSlots.present(slots[i])) free = i;
        if (free < 0) {
            if (player != null) player.sendOverlayMessage(Component.translatable("message.bettacraft.pipe_bundle.full").withStyle(ChatFormatting.GOLD));
            return -1;
        }
        if (!(level.getBlockEntity(pos) instanceof PipeBlockEntity be)) return -1;
        slots[free] = PipeSlots.of(steel, 0, 0, PipeRing.NONE);
        be.setSlots(slots);
        be.setSlots(rejoin(level, pos, slots, Direction.values()));
        level.sendBlockUpdated(pos, s, level.getBlockState(pos), Block.UPDATE_CLIENTS);
        after(level, pos);
        if (player != null && stack != null && !player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, pos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 0.8F, 0.9F);
        return free;
    }

    /** Takes slot {@code slot} out of the bundle at {@code pos}; returns its pipe. Public for the GameTests. */
    public static List<ItemStack> removePipe(Level level, BlockPos pos, int slot) {
        List<ItemStack> out = new ArrayList<>();
        if (!(level.getBlockState(pos).getBlock() instanceof PipeBundleBlock) || !(level.getBlockEntity(pos) instanceof PipeBlockEntity be)) return out;
        int[] slots = be.slots();
        if (slot < 0 || slot >= slots.length || !PipeSlots.present(slots[slot])) return out;
        out.add(pipeItem(PipeSlots.steel(slots[slot])));
        slots[slot] = 0;
        if (PipeSlots.count(slots) == 1) {
            int left = 0;
            for (int v : slots) if (PipeSlots.present(v)) left = v;
            BlockState cover = be.cover();
            ItemStack from = be.coverItem();
            PipeBlock p = PipeSlots.steel(left) ? ModBlocks.STAINLESS_STEEL_PIPE.get() : ModBlocks.GLASS_PIPE.get();
            BlockState single = PipeBlock.withSides(p.defaultBlockState(), PipeSlots.sides(left)).setValue(PipeBlock.RING, PipeSlots.ring(left));
            level.setBlock(pos, single, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            if (level.getBlockEntity(pos) instanceof PipeBlockEntity n) n.init(cover, from, PipeSlots.closed(left), new int[PipeSlots.COUNT]);
        } else {
            be.setSlots(slots);
        }
        after(level, pos);
        level.playSound(null, pos, SoundEvents.COPPER_BREAK, SoundSource.BLOCKS, 0.8F, 1.0F);
        return out;
    }

    /**
     * Moves slot {@code slot} of the bundle at {@code pos} to the next spot (swapping with the pipe there, if any), so it lines up
     * with the slot of a neighbouring bundle. Returns the new slot (-1: nothing moved). Public for the GameTests.
     */
    public static int reslot(Level level, BlockPos pos, int slot) {
        if (!(level.getBlockEntity(pos) instanceof PipeBlockEntity be) || !(level.getBlockState(pos).getBlock() instanceof PipeBundleBlock)) return -1;
        int[] slots = be.slots();
        if (slot < 0 || slot >= slots.length || !PipeSlots.present(slots[slot])) return -1;
        int to = (slot + 1) % slots.length;
        int a = slots[slot];
        slots[slot] = slots[to];
        slots[to] = a;
        be.setSlots(slots);
        be.setSlots(rejoin(level, pos, slots, Direction.values()));
        after(level, pos);
        return to;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult facade = PipeCovers.useForeign(stack, level, pos, player, hand); // part c: another mod's facade
        return facade != null ? facade : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            int slot = slotHit(level, pos, hit.getLocation());
            if (slot < 0) return InteractionResult.FAIL;
            for (ItemStack it : removePipe(level, pos, slot)) {
                if (player.getAbilities().instabuild) continue;
                if (!player.addItem(it)) player.drop(it, false, net.minecraft.util.Prediction.SERVER_ONLY);
            }
        }
        return InteractionResult.SUCCESS;
    }

    // ------------------------------------------------------------------ drops, pick block, cover

    /** What breaking a bundle gives: the pipe of every slot. */
    public static List<ItemStack> drops(int[] slots) {
        List<ItemStack> out = new ArrayList<>();
        for (int s : slots) if (PipeSlots.present(s)) out.add(pipeItem(PipeSlots.steel(s)));
        return out;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(be instanceof PipeBlockEntity f)) return List.of();
        List<ItemStack> out = drops(f.slots());
        PipeCovers.addDrops(out, f);
        return out;
    }

    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        int[] slots = Pipes.slotsAt(level, pos);
        int slot = target instanceof BlockHitResult b ? slotHit(level, pos, b.getLocation()) : -1;
        return slot < 0 ? pipeItem(true) : pipeItem(PipeSlots.steel(slots[slot]));
    }

    /** M52b: NeoForge 21.11 asks without the hit; it is the player's view ray, as for the pick-block key. */
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        return getCloneItemStack(state, player.pick(player.blockInteractionRange(), 1.0F, false), level, pos, player);
    }

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
}
