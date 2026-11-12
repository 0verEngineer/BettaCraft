package org.overengineer.bettacraft.tank.interior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.overengineer.bettacraft.data.schema.Schemas;
import org.overengineer.bettacraft.registry.ModBlockEntities;

/**
 * Substrate of one interior floor block (PLAN 7.2): the block state whose look the floor takes, and whether a block
 * was paid for it (only paid substrate is given back when it is replaced, removed or the tank is broken).
 */
public class InteriorFloorBlockEntity extends BlockEntity {
    public static final ModelProperty<BlockState> SUBSTRATE_STATE = new ModelProperty<>();

    private BlockState substrate = Blocks.SAND.defaultBlockState();
    private boolean paid;

    public InteriorFloorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INTERIOR_FLOOR.get(), pos, state);
    }

    public BlockState substrate() {
        return substrate;
    }

    public boolean paid() {
        return paid;
    }

    public void set(BlockState substrate, boolean paid) {
        this.substrate = substrate;
        this.paid = paid;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag raw, HolderLookup.Provider registries) {
        super.loadAdditional(raw, registries);
        CompoundTag tag = Schemas.INTERIOR_FLOOR.migrate(raw);
        if (tag.contains("substrate")) {
            BlockState s = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("substrate"));
            substrate = s.isAir() ? Blocks.SAND.defaultBlockState() : s;
        }
        paid = tag.getBoolean("paid");
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        Schemas.INTERIOR_FLOOR.stamp(tag);
        tag.put("substrate", NbtUtils.writeBlockState(substrate));
        tag.putBoolean("paid", paid);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        super.onDataPacket(net, pkt, registries);
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(SUBSTRATE_STATE, substrate).build();
    }
}
