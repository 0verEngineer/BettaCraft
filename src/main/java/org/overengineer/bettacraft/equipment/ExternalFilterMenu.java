package org.overengineer.bettacraft.equipment;

import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.overengineer.bettacraft.registry.ModMenus;

/** Two heater slots of the external filter plus the player inventory (vanilla 3-row container layout). */
public class ExternalFilterMenu extends AbstractContainerMenu {
    public static final int SLOT_X = 71, SLOT_Y = 35;
    @Nullable
    private final ExternalFilterBlockEntity filter;

    public ExternalFilterMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, null, new ItemStackHandler(FilterHeaters.SLOTS));
    }

    ExternalFilterMenu(int id, Inventory inv, ExternalFilterBlockEntity filter) {
        this(id, inv, filter, filter.heaters());
    }

    private ExternalFilterMenu(int id, Inventory inv, @Nullable ExternalFilterBlockEntity filter, IItemHandler heaters) {
        super(ModMenus.EXTERNAL_FILTER.get(), id);
        this.filter = filter;
        for (int i = 0; i < FilterHeaters.SLOTS; i++) {
            addSlot(new SlotItemHandler(heaters, i, SLOT_X + i * 18, SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return FilterHeaters.isHeater(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack s = slot.getItem();
        ItemStack copy = s.copy();
        int end = FilterHeaters.SLOTS;
        if (index < end) {
            if (!moveItemStackTo(s, end, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!FilterHeaters.isHeater(s) || !moveItemStackTo(s, 0, end, false)) return ItemStack.EMPTY;
        }
        if (s.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return filter == null || (!filter.isRemoved() && player.distanceToSqr(filter.getBlockPos().getCenter()) <= 64.0);
    }
}
