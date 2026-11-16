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
 */
public class FilterPipeBlockEntity extends BlockEntity {
    public static final ModelProperty<BlockState> COVER = new ModelProperty<>();
    /** Part b: the packed lines of a bundle. */
    public static final ModelProperty<Integer> LINES = new ModelProperty<>();
    @Nullable
    private BlockState cover;
    private int lines;

    public FilterPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FILTER_PIPE.get(), pos, state);
    }

    @Nullable
    public BlockState cover() {
        return cover;
    }

    /** Sets or clears ({@code null}) the cover; synced to the clients. */
    public void setCover(@Nullable BlockState s) {
        cover = s;
        changed();
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
    void init(@Nullable BlockState c, int l) {
        cover = c;
        lines = l;
        setChanged();
    }

    private void changed() {
        setChanged();
        if (level != null) {
            if (!level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            else requestModelDataUpdate();
        }
    }

    private void write(CompoundTag t) {
        if (cover != null) t.put("cover", NbtUtils.writeBlockState(cover));
        if (lines != 0) t.putInt("lines", lines);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        write(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        cover = tag.contains("cover") ? NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("cover")) : null;
        if (cover != null && cover.isAir()) cover = null;
        lines = tag.getInt("lines");
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        write(t);
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
