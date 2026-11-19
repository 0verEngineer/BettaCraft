package org.overengineer.bettacraft.tank.interior;

import org.overengineer.bettacraft.util.NbtCompat;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
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
import org.overengineer.bettacraft.data.schema.Schemas;
import org.overengineer.bettacraft.registry.ModBlockEntities;

/**
 * Substrate of one interior floor block (PLAN 7.2): the block state whose look the floor takes, and whether a block
 * was paid for it (only paid substrate is given back when it is replaced, removed or the tank is broken).
 */
public class InteriorFloorBlockEntity extends BlockEntity implements org.overengineer.bettacraft.platform.model.ModelDataSource, org.overengineer.bettacraft.platform.ClientSyncedBlockEntity {
    public static final org.overengineer.bettacraft.platform.model.ModelKey<BlockState> SUBSTRATE_STATE = new org.overengineer.bettacraft.platform.model.ModelKey<>("substrate_state");

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
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        CompoundTag raw = NbtCompat.read(input);
        HolderLookup.Provider registries = input.lookup();
        super.loadAdditional(input);
        CompoundTag tag = Schemas.INTERIOR_FLOOR.migrate(raw);
        if (tag.contains("substrate")) {
            BlockState s = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompoundOrEmpty("substrate"));
            substrate = s.isAir() ? Blocks.SAND.defaultBlockState() : s;
        }
        paid = tag.getBooleanOr("paid", false);
        if (level != null && level.isClientSide()) {
            org.overengineer.bettacraft.platform.Platform.get().requestModelDataUpdate(this);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        CompoundTag tag = new CompoundTag();
        HolderLookup.Provider registries = NbtCompat.registries(this);
        super.saveAdditional(output);
        Schemas.INTERIOR_FLOOR.stamp(tag);
        tag.put("substrate", NbtUtils.writeBlockState(substrate));
        tag.putBoolean("paid", paid);
        NbtCompat.write(output, tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries); // M52b: saveAdditional takes a value output now
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        if (!pkt.getTag().isEmpty()) loadWithComponents(NbtCompat.input(registries, pkt.getTag())); // NeoForge's default
    }

    @Override
    public void handleUpdateTag(net.minecraft.nbt.CompoundTag tag, HolderLookup.Provider registries) {
        loadWithComponents(NbtCompat.input(registries, tag)); // NeoForge's default
    }

    /** M52b: NeoForge 21.11 hands the client update over as a value input (the same handling as the compound). */
    public void handleUpdateTag(net.minecraft.world.level.storage.ValueInput input) {
        handleUpdateTag(org.overengineer.bettacraft.util.NbtCompat.read(input), org.overengineer.bettacraft.util.NbtCompat.registries(this));
    }

    /** M52b: NeoForge 21.11 hands the update packet's data over as a value input. */
    public void onDataPacket(Connection net, net.minecraft.world.level.storage.ValueInput input) {
        net.minecraft.nbt.CompoundTag tag = org.overengineer.bettacraft.util.NbtCompat.read(input);
        if (!tag.isEmpty()) handleUpdateTag(tag, org.overengineer.bettacraft.util.NbtCompat.registries(this));
    }

    @Override
    public org.overengineer.bettacraft.platform.model.ModelValues modelValues() {
        return org.overengineer.bettacraft.platform.model.ModelValues.builder().with(SUBSTRATE_STATE, substrate).build();
    }
}
