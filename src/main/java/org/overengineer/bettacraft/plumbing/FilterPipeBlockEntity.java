package org.overengineer.bettacraft.plumbing;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.overengineer.bettacraft.registry.ModBlockEntities;

/**
 * M40: the block entity of a filter pipe. Part a only keeps the place for what the later parts put on a pipe: the cover
 * (a facade: the block the pipe is drawn as, {@link #COVER}; set by part c) is saved, synced and handed to the model
 * ({@code client.model.FilterPipeModel} draws the cover instead of the pipe while one is set).
 * <p>
 * Part b: the block entity of the pipe bundle too ({@link FilterPipeBundleBlock}); it keeps the bundle's lines, each with
 * its sides and T-piece ({@link #lines()}, packed as in {@link FilterPipes}; 0 for a single pipe, whose line is in its
 * block state). Saved, synced and handed to the model ({@link #LINES}).
 * <p>
 * Part c: the cover is put on with the pipe cover or another mod's facade ({@link PipeCovers}); the block entity also keeps
 * the item it came from ({@link #coverItem()}: the block or the facade, given back when the cover comes off or the pipe is
 * broken) and the light the cover block gives (NeoForge's auxiliary light of the chunk, so a glowstone cover shines).
 */
public class FilterPipeBlockEntity extends BlockEntity {
    public static final ModelProperty<BlockState> COVER = new ModelProperty<>();
    /** Part b: the packed lines of a bundle. */
    public static final ModelProperty<Integer> LINES = new ModelProperty<>();
    @Nullable
    private BlockState cover;
    /** Part c: what the cover came from (one block or facade item; empty for a cover set without one). */
    private ItemStack coverItem = ItemStack.EMPTY;
    private int lines;

    public FilterPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FILTER_PIPE.get(), pos, state);
    }

    @Nullable
    public BlockState cover() {
        return cover;
    }

    /** Part c: the item the cover came from (given back when it comes off; empty if none). */
    public ItemStack coverItem() {
        return coverItem;
    }

    /** Sets or clears ({@code null}) the cover; synced to the clients. */
    public void setCover(@Nullable BlockState s) {
        setCover(s, ItemStack.EMPTY);
    }

    /** Part c: sets the cover and the item it came from (one of it is kept), or clears both ({@code null}); synced, light updated. */
    public void setCover(@Nullable BlockState s, ItemStack from) {
        cover = s != null && s.isAir() ? null : s;
        coverItem = cover == null || from.isEmpty() ? ItemStack.EMPTY : from.copyWithCount(1);
        changed();
        light();
    }

    /** The light of the cover in the chunk's auxiliary light (a lamp block as a cover shines), and the light engine told. */
    private void light() {
        if (level == null) return;
        int l = cover == null ? 0 : cover.getLightEmission();
        var aux = level.getAuxLightManager(worldPosition);
        if (aux != null && aux.getLightAt(worldPosition) != l) aux.setLightAt(worldPosition, l);
        level.getLightEngine().checkBlock(worldPosition);
    }

    /** Part b: the packed lines of a bundle (0 for a single pipe). */
    public int lines() {
        return lines;
    }

    /** Part b: sets the lines of a bundle; synced to the clients. */
    public void setLines(int l) {
        if (l == lines) return;
        lines = l;
        changed();
    }

    /** Sets cover and lines without telling anyone (a block that just turned into a bundle or back, before its neighbours hear of it). */
    void init(@Nullable BlockState c, ItemStack from, int l) {
        cover = c;
        coverItem = from;
        lines = l;
        setChanged();
        if (c != null) light();
    }

    private void changed() {
        setChanged();
        if (level != null) {
            if (!level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            else requestModelDataUpdate();
        }
    }

    private void write(CompoundTag t, HolderLookup.Provider registries) {
        if (cover != null) t.put("cover", NbtUtils.writeBlockState(cover));
        if (cover != null && !coverItem.isEmpty()) t.put("cover_item", coverItem.save(registries)); // part c
        if (lines != 0) t.putInt("lines", lines);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        write(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        cover = tag.contains("cover") ? NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("cover")) : null;
        if (cover != null && cover.isAir()) cover = null;
        coverItem = cover != null && tag.contains("cover_item") ? ItemStack.parseOptional(registries, tag.getCompound("cover_item")) : ItemStack.EMPTY;
        lines = tag.getInt("lines");
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            light();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
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
    public ModelData getModelData() {
        if (cover == null && lines == 0) return ModelData.EMPTY;
        ModelData.Builder b = ModelData.builder();
        if (cover != null) b.with(COVER, cover);
        if (lines != 0) b.with(LINES, lines);
        return b.build();
    }
}
