package org.overengineer.bettacraft.plumbing;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;

/**
 * M40 part c (C1): covers on the filter pipes and pipe bundles, a facade that draws the pipe as a full block while its
 * lines and connections stay as they are. A cover comes from
 * <ul>
 * <li>our pipe cover ({@link PipeCoverItem}): in the main hand, with a full block in the off hand; the block is used up and
 * comes back when the cover comes off (sneak-right-click with the pipe cover) or the pipe is broken;</li>
 * <li>the facade items of other mods that carry their block in their item data ({@link #FOREIGN}): Applied Energistics 2
 * ({@code ae2:facade}, component {@code ae2:facade_item}), EnderIO ({@code enderio:*conduit_facade}, {@code enderio:block_paint})
 * and Mekanism Covers ({@code mekanismcovers:cover}, {@code mekanismcovers:cover_block}). We only read the block from the
 * item's data (encoded by the component's own codec, no code of theirs); the facade item is kept and given back.</li>
 * </ul>
 * The Cable Facades mod puts its own facades on our pipes (the block tag {@code cable_facades:supports_facade} in our data,
 * {@code compat.CableFacadesCompat}); those live in its world data, not here.
 * <p>
 * While a cover is on, the pipe is a full block for its outline and collision, hides the faces of its neighbours like the
 * cover would, is shaded like it, shines like it (the auxiliary light of the chunk) and sounds like it. Light still passes
 * through (the light engine runs off the main thread and reads no block entities there; a cover only in the block entity
 * cannot make the block opaque without server and client disagreeing).
 */
public final class PipeCovers {
    /** The facade items of other mods and the data component that carries their block. */
    public static final Map<ResourceLocation, ResourceLocation> FOREIGN = Map.of(
            rl("ae2", "facade"), rl("ae2", "facade_item"),
            rl("enderio", "conduit_facade"), rl("enderio", "block_paint"),
            rl("enderio", "hardened_conduit_facade"), rl("enderio", "block_paint"),
            rl("enderio", "transparent_conduit_facade"), rl("enderio", "block_paint"),
            rl("enderio", "transparent_hardened_conduit_facade"), rl("enderio", "block_paint"),
            rl("mekanismcovers", "cover"), rl("mekanismcovers", "cover_block"));

    private PipeCovers() {}

    private static ResourceLocation rl(String ns, String path) {
        return ResourceLocation.fromNamespaceAndPath(ns, path);
    }

    // ------------------------------------------------------------------ what a pipe looks like

    /** The cover of the filter pipe or bundle at {@code pos}, or null. */
    @Nullable
    public static BlockState coverAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FilterPipeBlockEntity be ? be.cover() : null;
    }

    /** The shape of a pipe: a full block with a cover, else its own. */
    public static VoxelShape shape(BlockGetter level, BlockPos pos, VoxelShape own) {
        return coverAt(level, pos) != null ? Shapes.block() : own;
    }

    /** Whether the pipe at {@code pos} hides the face of its neighbour (a cover that is a solid full block). */
    public static boolean hidesNeighbour(BlockGetter level, BlockPos pos) {
        BlockState c = coverAt(level, pos);
        return c != null && c.isSolidRender(level, pos);
    }

    public static float shade(BlockGetter level, BlockPos pos) {
        BlockState c = coverAt(level, pos);
        return c == null ? 1.0F : c.getShadeBrightness(level, pos);
    }

    /** The light of the cover, from the auxiliary light the block entity keeps (thread safe, also during chunk loading). */
    public static int light(BlockGetter level, BlockPos pos) {
        var aux = level.getAuxLightManager(pos);
        return aux == null ? 0 : aux.getLightAt(pos);
    }

    @Nullable
    public static SoundType sound(LevelReader level, BlockPos pos, @Nullable Entity entity) {
        BlockState c = coverAt(level, pos);
        return c == null ? null : c.getSoundType(level, pos, entity);
    }

    // ------------------------------------------------------------------ which blocks may cover a pipe

    /**
     * Whether {@code s} may be a cover: a full cube drawn by its model, without a block entity (its look would need one),
     * no pipe or tank of ours.
     */
    public static boolean fits(BlockState s) {
        if (s.isAir() || s.getBlock() instanceof EntityBlock || s.getRenderShape() != RenderShape.MODEL || FilterPipes.isPipe(s)) return false;
        if (!s.getFluidState().isEmpty()) return false;
        return Block.isShapeFullBlock(s.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
    }

    /** The block a facade item of another mod carries (read from its item data), or null for any other item. */
    @Nullable
    public static BlockState foreign(ItemStack stack, @Nullable Level level) {
        if (stack.isEmpty()) return null;
        ResourceLocation comp = FOREIGN.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (comp == null) return null;
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(comp);
        if (type == null) return null;
        Block b = blockIn(type, stack.get(type), level);
        return b == null || b.defaultBlockState().isAir() ? null : b.defaultBlockState();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <T> Block blockIn(DataComponentType<T> type, @Nullable Object value, @Nullable Level level) {
        if (value == null || type.codec() == null) return null;
        try {
            var ops = level != null ? level.registryAccess().createSerializationContext(NbtOps.INSTANCE) : NbtOps.INSTANCE;
            Tag t = type.codec().encodeStart(ops, (T) value).result().orElse(null);
            return t == null ? null : blockIn(t, 0);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The first block or block item id in the encoded data (a plain id, {@code Name}, {@code id}, {@code paint}, nested). */
    @Nullable
    static Block blockIn(Tag t, int depth) {
        if (depth > 4) return null;
        if (t instanceof StringTag s) return blockOf(s.getAsString());
        if (t instanceof CompoundTag c) {
            for (String k : new String[]{"Name", "id", "block", "paint", "item", "facade_block"}) {
                if (c.contains(k)) {
                    Block b = blockIn(c.get(k), depth + 1);
                    if (b != null) return b;
                }
            }
            for (String k : c.getAllKeys()) {
                Block b = blockIn(c.get(k), depth + 1);
                if (b != null) return b;
            }
        }
        if (t instanceof ListTag l) {
            for (Tag e : l) {
                Block b = blockIn(e, depth + 1);
                if (b != null) return b;
            }
        }
        return null;
    }

    @Nullable
    private static Block blockOf(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || "air".equals(rl.getPath())) return null;
        if (BuiltInRegistries.BLOCK.containsKey(rl)) return BuiltInRegistries.BLOCK.get(rl);
        if (BuiltInRegistries.ITEM.containsKey(rl) && BuiltInRegistries.ITEM.get(rl) instanceof BlockItem bi) return bi.getBlock();
        return null;
    }

    // ------------------------------------------------------------------ putting covers on and off

    /**
     * Puts {@code cover} on the pipe at {@code pos}, keeping one {@code from} (the block or facade it came from; given back
     * later). A cover already on comes off first (to the player, or dropped). Public for the GameTests.
     */
    public static boolean apply(Level level, BlockPos pos, BlockState cover, ItemStack from, @Nullable Player player) {
        if (!Features.enabled(Feature.PIPE_COVERS)) {
            if (player != null) player.displayClientMessage(Component.translatable("message.bettacraft.feature_off").withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        if (!(level.getBlockEntity(pos) instanceof FilterPipeBlockEntity be) || !FilterPipes.isPipe(level.getBlockState(pos))) return false;
        if (!fits(cover)) {
            if (player != null) player.displayClientMessage(Component.translatable("message.bettacraft.pipe_cover.not_full", cover.getBlock().getName()).withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        if (level.isClientSide) return true;
        ItemStack old = be.cover() != null ? be.coverItem() : ItemStack.EMPTY;
        be.setCover(cover, from);
        give(level, pos, player, old);
        SoundType st = cover.getSoundType(level, pos, player);
        level.playSound(null, pos, st.getPlaceSound(), SoundSource.BLOCKS, (st.getVolume() + 1) / 2, st.getPitch() * 0.8F);
        return true;
    }

    /** Takes the cover off the pipe at {@code pos}; returns what it came from (empty without a cover). Public for the GameTests. */
    public static ItemStack remove(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof FilterPipeBlockEntity be) || be.cover() == null) return ItemStack.EMPTY;
        BlockState c = be.cover();
        ItemStack out = be.coverItem().copy();
        be.setCover(null);
        SoundType st = c.getSoundType(level, pos, null);
        level.playSound(null, pos, st.getBreakSound(), SoundSource.BLOCKS, (st.getVolume() + 1) / 2, st.getPitch() * 0.8F);
        return out;
    }

    static void give(Level level, BlockPos pos, @Nullable Player player, ItemStack it) {
        if (it.isEmpty()) return;
        if (player != null) {
            if (player.getAbilities().instabuild) return;
            if (!player.addItem(it)) player.drop(it, false);
        } else {
            Block.popResource(level, pos, it);
        }
    }

    /**
     * The use of an item on a pipe: another mod's facade puts its block on as the cover (one facade is kept). Null if the
     * item is no facade (the pipe goes on as usual).
     */
    @Nullable
    public static ItemInteractionResult useForeign(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        BlockState c = foreign(stack, level);
        if (c == null) return null;
        if (apply(level, pos, c, stack, player) && !level.isClientSide && !player.getAbilities().instabuild) stack.shrink(1);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** What breaking the pipe gives besides its pipes: the item its cover came from. */
    public static void addDrops(java.util.List<ItemStack> drops, @Nullable net.minecraft.world.level.block.entity.BlockEntity be) {
        if (be instanceof FilterPipeBlockEntity f && f.cover() != null && !f.coverItem().isEmpty()) drops.add(f.coverItem().copy());
    }
}
