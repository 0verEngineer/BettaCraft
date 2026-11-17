package org.overengineer.bettacraft.filter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.registry.ModBlockEntities;

/** M24: a filter chamber: one filter medium; its dirt grows while the filter runs (fractions kept here). */
public class FilterChamberBlockEntity extends BlockEntity {
    private ItemStack medium = ItemStack.EMPTY;
    private double carry;

    public FilterChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FILTER_CHAMBER.get(), pos, state);
    }

    public ItemStack medium() {
        return medium;
    }

    public FilterMedium kind() {
        return FilterMediaItem.mediumOf(medium);
    }

    public double dirt() {
        return FilterMediaItem.dirt(medium);
    }

    public void setMedium(ItemStack stack) {
        medium = stack;
        carry = 0;
        changed();
    }

    /** M42: whether the medium is used up (fully clogged or spent): only then may automation take it out. */
    public boolean spent() {
        return !medium.isEmpty() && dirt() >= 1.0;
    }

    /**
     * M42: the item capability of the chamber for hoppers, pipes and ME / RS buses (switch
     * {@code equipment.filterMediaAutomation}): one slot; a fresh filter medium goes into an empty chamber, a spent medium
     * (fully clogged or used up) comes out. A medium still working stays in, so an import bus or a pipe never pulls the
     * new medium right back out.
     */
    public org.overengineer.bettacraft.platform.item.IItemHandler automation() {
        return new org.overengineer.bettacraft.platform.item.IItemHandler() {
            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return slot == 0 ? medium : ItemStack.EMPTY;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (slot != 0 || stack.isEmpty() || !medium.isEmpty() || !isItemValid(slot, stack)) return stack;
                if (!simulate) setMedium(stack.copyWithCount(1));
                return stack.getCount() == 1 ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - 1);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot != 0 || amount <= 0 || !spent()) return ItemStack.EMPTY;
                ItemStack out = medium.copy();
                if (!simulate) setMedium(ItemStack.EMPTY);
                return out;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot == 0 && stack.getItem() instanceof FilterMediaItem;
            }
        };
    }

    /** Adds dirt (0..1 of a full clog). */
    public void dirty(double amount) {
        if (medium.isEmpty() || !medium.isDamageableItem() || amount <= 0) return;
        carry += amount * medium.getMaxDamage();
        int whole = (int) Math.floor(carry);
        if (whole <= 0) return;
        carry -= whole;
        int before = medium.getDamageValue();
        medium.setDamageValue(Math.min(medium.getMaxDamage(), before + whole));
        // sync now and then (every 2 %), not for every point
        if (medium.getDamageValue() / 20 != before / 20) changed();
        else setChanged();
    }

    private void changed() {
        setChanged();
        if (level == null || level.isClientSide) return;
        BlockState s = getBlockState();
        FilterMedium k = kind();
        if (s.hasProperty(FilterPartBlock.MEDIUM) && s.getValue(FilterPartBlock.MEDIUM) != k) level.setBlock(worldPosition, s.setValue(FilterPartBlock.MEDIUM, k), Block.UPDATE_ALL);
        else level.sendBlockUpdated(worldPosition, s, s, Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!medium.isEmpty()) tag.put("medium", medium.save(registries));
        tag.putDouble("carry", carry);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        medium = tag.contains("medium") ? ItemStack.parse(registries, tag.getCompound("medium")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        carry = tag.getDouble("carry");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        if (!medium.isEmpty()) t.put("medium", medium.save(registries));
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
