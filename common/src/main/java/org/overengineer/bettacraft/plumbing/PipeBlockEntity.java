package org.overengineer.bettacraft.plumbing;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.model.ModelKey;
import org.overengineer.bettacraft.platform.model.ModelValues;
import org.overengineer.bettacraft.registry.ModBlockEntities;

/**
 * M68: the block entity of every pipe (glass and stainless steel, {@link PipeBlock}) and of the pipe bundle
 * ({@link PipeBundleBlock}); its type keeps the id {@code filter_pipe} of the M40 filter pipes, so their saved data (cover,
 * bundle lines) loads into it. It keeps
 * <ul>
 * <li>the sides closed with the pipe wrench ({@link #closed()}; a single pipe, whose joined sides and ring are in its block
 * state),</li>
 * <li>the pipes of a bundle ({@link #slots()}, {@link PipeSlots}: sides, closed sides, glass or steel, ring each),</li>
 * <li>the cover (M40 part c: a facade, {@link PipeCovers}) and the item it came from.</li>
 * </ul>
 * Saved, synced and handed to the models ({@link #CLOSED}, {@link #SLOTS}, {@link #COVER}).
 * <p>
 * Migration (saved without {@link #VERSION}): an M40 filter pipe (aliased to the stainless steel pipe, its sides in the block
 * state) closes every side it was not joined on, so it keeps exactly its old connections; an M40 bundle's packed lines become
 * steel slots the same way ({@link PipeSlots#fromLegacy}); a block without any side (an old water port of the filter, aliased
 * to a steel pipe) closes nothing and looks at its neighbours again. Each of them is then joined again and gets its ring
 * ({@link PipeUpdates}). A glass or steel pipe from before M68 has no block entity yet: it gets an empty one (nothing closed).
 */
public class PipeBlockEntity extends BlockEntity implements org.overengineer.bettacraft.platform.model.ModelDataSource {
    public static final ModelKey<BlockState> COVER = new ModelKey<>("cover");
    /** The sides of a single pipe closed with the wrench. */
    public static final ModelKey<Integer> CLOSED = new ModelKey<>("closed");
    /** The slots of a bundle. */
    public static final ModelKey<int[]> SLOTS = new ModelKey<>("slots");
    /** Saved data version (absent: M40 filter pipe or bundle, see the class comment). */
    public static final int VERSION = 2;

    @Nullable
    private BlockState cover;
    private ItemStack coverItem = ItemStack.EMPTY;
    private int closed;
    private int[] slots = new int[PipeSlots.COUNT];
    /** Loaded from before M68: joined again and its ring set once it is in a level. */
    private boolean legacy;

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PIPE.get(), pos, state);
    }

    // ------------------------------------------------------------------ cover

    @Nullable
    public BlockState cover() {
        return cover;
    }

    public ItemStack coverItem() {
        return coverItem;
    }

    public void setCover(@Nullable BlockState s) {
        setCover(s, ItemStack.EMPTY);
    }

    /** Sets the cover and the item it came from (one of it is kept), or clears both ({@code null}); synced, light updated. */
    public void setCover(@Nullable BlockState s, ItemStack from) {
        cover = s != null && s.isAir() ? null : s;
        coverItem = cover == null || from.isEmpty() ? ItemStack.EMPTY : from.copyWithCount(1);
        changed();
        light();
    }

    private void light() {
        if (level == null) return;
        int l = cover == null ? 0 : cover.getLightEmission();
        org.overengineer.bettacraft.platform.Platform.get().setAuxLight(level, worldPosition, l);
        level.getLightEngine().checkBlock(worldPosition);
    }

    // ------------------------------------------------------------------ closed sides, slots

    /** The sides of a single pipe closed with the wrench (bit = {@code 1 << direction.get3DDataValue()}). */
    public int closed() {
        return closed;
    }

    public boolean closed(Direction d) {
        return (closed & PipeSlots.bit(d)) != 0;
    }

    public void setClosed(int c) {
        c &= PipeSlots.SIDES;
        if (c == closed) return;
        closed = c;
        changed();
    }

    /** The slots of a bundle (a copy). */
    public int[] slots() {
        return slots.clone();
    }

    public int slot(int i) {
        return i >= 0 && i < slots.length ? slots[i] : 0;
    }

    public void setSlots(int[] s) {
        if (java.util.Arrays.equals(s, slots)) return;
        slots = java.util.Arrays.copyOf(s, PipeSlots.COUNT);
        changed();
    }

    public void setSlot(int i, int slot) {
        if (slots[i] == slot) return;
        slots[i] = slot;
        changed();
    }

    /** Sets everything without telling anyone (a block that just turned into a bundle or back, before its neighbours hear of it). */
    void init(@Nullable BlockState c, ItemStack from, int closedSides, int[] s) {
        cover = c;
        coverItem = from;
        closed = closedSides & PipeSlots.SIDES;
        slots = java.util.Arrays.copyOf(s, PipeSlots.COUNT);
        setChanged();
        if (c != null) light();
    }

    /** Loaded from before M68 and not yet joined again ({@link PipeUpdates}). */
    public boolean legacy() {
        return legacy;
    }

    void migrated() {
        if (!legacy) return;
        legacy = false;
        setChanged();
    }

    private void changed() {
        setChanged();
        if (level != null) {
            if (!level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            else org.overengineer.bettacraft.platform.Platform.get().requestModelDataUpdate(this);
        }
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (legacy && !level.isClientSide()) PipeUpdates.schedule(level, worldPosition, true);
    }

    // ------------------------------------------------------------------ save / sync

    private void write(CompoundTag t, HolderLookup.Provider registries) {
        t.putInt("pipe", VERSION);
        if (cover != null) t.put("cover", NbtUtils.writeBlockState(cover));
        if (cover != null && !coverItem.isEmpty()) t.put("cover_item", org.overengineer.bettacraft.util.NbtCompat.saveStack(registries, coverItem));
        if (closed != 0) t.putInt("closed", closed);
        if (PipeSlots.count(slots) > 0) t.putIntArray("slots", slots);
        if (legacy) t.putBoolean("legacy", true);
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = org.overengineer.bettacraft.util.NbtCompat.registries(this);
        super.saveAdditional(output);
        write(tag, registries);
        org.overengineer.bettacraft.util.NbtCompat.write(output, tag);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        CompoundTag tag = org.overengineer.bettacraft.util.NbtCompat.read(input);
        HolderLookup.Provider registries = input.lookup();
        super.loadAdditional(input);
        cover = tag.contains("cover") ? NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompoundOrEmpty("cover")) : null;
        if (cover != null && cover.isAir()) cover = null;
        coverItem = cover != null && tag.contains("cover_item") ? org.overengineer.bettacraft.util.NbtCompat.parseOptionalStack(registries, tag.getCompoundOrEmpty("cover_item")) : ItemStack.EMPTY;
        if (tag.getIntOr("pipe", 0) >= VERSION || (level != null && level.isClientSide())) {
            closed = tag.getIntOr("closed", 0) & PipeSlots.SIDES;
            int[] s = tag.getIntArray("slots").orElse(new int[0]);
            slots = java.util.Arrays.copyOf(s, PipeSlots.COUNT);
            legacy = tag.getBooleanOr("legacy", false);
        } else {
            migrate(tag);
        }
        if (level != null && level.isClientSide()) {
            org.overengineer.bettacraft.platform.Platform.get().requestModelDataUpdate(this);
            light();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
    }

    /** Data of an M40 filter pipe, bundle or water port (see the class comment). */
    private void migrate(CompoundTag tag) {
        legacy = true;
        slots = new int[PipeSlots.COUNT];
        closed = 0;
        if (tag.contains("lines")) {
            slots = PipeSlots.fromLegacy(tag.getIntOr("lines", 0));
            return;
        }
        BlockState s = getBlockState();
        if (!(s.getBlock() instanceof PipeBlock)) return;
        int sides = PipeBlock.sides(s);
        closed = sides == 0 ? 0 : ~sides & PipeSlots.SIDES;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        write(t, registries);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public ModelValues modelValues() {
        boolean bundle = PipeSlots.count(slots) > 0;
        if (cover == null && closed == 0 && !bundle) return ModelValues.EMPTY;
        ModelValues.Builder b = ModelValues.builder();
        if (cover != null) b.with(COVER, cover);
        if (closed != 0) b.with(CLOSED, closed);
        if (bundle) b.with(SLOTS, slots.clone());
        return b.build();
    }
}
