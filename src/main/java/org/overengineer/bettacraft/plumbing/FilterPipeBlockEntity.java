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
 */
public class FilterPipeBlockEntity extends BlockEntity {
    public static final ModelProperty<BlockState> COVER = new ModelProperty<>();
    @Nullable
    private BlockState cover;

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
        setChanged();
        if (level != null) {
            if (!level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            else requestModelDataUpdate();
        }
    }

    private void write(CompoundTag t) {
        if (cover != null) t.put("cover", NbtUtils.writeBlockState(cover));
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
        if (level != null && level.isClientSide) requestModelDataUpdate();
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
        return cover == null ? ModelData.EMPTY : ModelData.builder().with(COVER, cover).build();
    }
}
