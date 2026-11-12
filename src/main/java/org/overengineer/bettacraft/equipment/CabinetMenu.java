package org.overengineer.bettacraft.equipment;

import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.registry.ModMenus;

/**
 * Cabinet menu: the filter slot, two heater slots that edit the heaters inside that filter item (only active while a
 * filter is in the slot), nine storage slots and the player inventory.
 */
public class CabinetMenu extends AbstractContainerMenu {
    public static final int FILTER_X = 26, FILTER_Y = 18, HEATER_X = 17, HEATER_Y = 44, STORAGE_X = 98, STORAGE_Y = 17;
    /** Menu slot indices. */
    public static final int FILTER = 0, HEATER0 = 1, STORAGE0 = 3, PLAYER0 = 3 + CabinetBlockEntity.STORAGE;
    @Nullable
    private final CabinetBlockEntity cabinet;
    private final IItemHandlerModifiable items;

    public CabinetMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, null, new ItemStackHandler(CabinetBlockEntity.SLOTS));
    }

    CabinetMenu(int id, Inventory inv, CabinetBlockEntity cabinet) {
        this(id, inv, cabinet, cabinet.items());
    }

    private CabinetMenu(int id, Inventory inv, @Nullable CabinetBlockEntity cabinet, IItemHandlerModifiable items) {
        super(ModMenus.CABINET.get(), id);
        this.cabinet = cabinet;
        this.items = items;
        addSlot(new SlotItemHandler(items, CabinetBlockEntity.FILTER_SLOT, FILTER_X, FILTER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.EXTERNAL_FILTER.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        FilterItemHeaters heaters = new FilterItemHeaters();
        for (int i = 0; i < 2; i++) addSlot(new HeaterSlot(heaters, i, HEATER_X + i * 18, HEATER_Y));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                addSlot(new SlotItemHandler(items, 1 + c + r * 3, STORAGE_X + c * 18, STORAGE_Y + r * 18));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
    }

    public boolean hasFilter() {
        return !items.getStackInSlot(CabinetBlockEntity.FILTER_SLOT).isEmpty();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        if (index < PLAYER0) {
            if (!moveItemStackTo(s, PLAYER0, slots.size(), true)) return ItemStack.EMPTY;
        } else if (s.is(ModItems.EXTERNAL_FILTER.get()) && !slots.get(FILTER).hasItem()) {
            if (!moveItemStackTo(s, FILTER, FILTER + 1, false)) return ItemStack.EMPTY;
        } else if (FilterHeaters.isHeater(s) && hasFilter() && (!slots.get(HEATER0).hasItem() || !slots.get(HEATER0 + 1).hasItem())) {
            if (!moveItemStackTo(s, HEATER0, HEATER0 + 2, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(s, STORAGE0, PLAYER0, false)) {
            return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return cabinet == null || (!cabinet.isRemoved() && player.distanceToSqr(cabinet.getBlockPos().getCenter()) <= 64.0);
    }

    /** The heaters inside the filter item in the filter slot, as a two-slot container (component in and out). */
    private final class FilterItemHeaters implements Container {
        private ItemStack filter() {
            return items.getStackInSlot(CabinetBlockEntity.FILTER_SLOT);
        }

        private void store(NonNullList<ItemStack> list) {
            ItemStack f = filter().copy();
            FilterHeaters.write(f, list);
            items.setStackInSlot(CabinetBlockEntity.FILTER_SLOT, f);
        }

        @Override
        public int getContainerSize() {
            return 2;
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack s : FilterHeaters.read(filter())) if (!s.isEmpty()) return false;
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            ItemStack f = filter();
            return f.isEmpty() ? ItemStack.EMPTY : FilterHeaters.read(f).get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack f = filter();
            if (f.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            NonNullList<ItemStack> list = FilterHeaters.read(f);
            ItemStack out = list.get(slot).split(amount);
            if (!out.isEmpty()) store(list);
            return out;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return removeItem(slot, 64);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (filter().isEmpty()) return;
            NonNullList<ItemStack> list = FilterHeaters.read(filter());
            list.set(slot, stack);
            store(list);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            if (cabinet != null) cabinet.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return !filter().isEmpty() && FilterHeaters.isHeater(stack);
        }

        @Override
        public void clearContent() {
            if (!filter().isEmpty()) store(NonNullList.withSize(2, ItemStack.EMPTY));
        }
    }

    private final class HeaterSlot extends Slot {
        HeaterSlot(Container c, int index, int x, int y) {
            super(c, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return hasFilter() && FilterHeaters.isHeater(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean isActive() {
            return hasFilter();
        }
    }
}
