package org.overengineer.bettacraft.platform.item;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** M52: inserting into an {@link IItemHandler} slot by slot. */
public final class ItemHandlerHelper {
    private ItemHandlerHelper() {}

    /** Inserts into the slots in order; returns what did not fit. */
    public static ItemStack insertItem(@Nullable IItemHandler dest, ItemStack stack, boolean simulate) {
        if (dest == null || stack.isEmpty()) return stack;
        for (int i = 0; i < dest.getSlots(); i++) {
            stack = dest.insertItem(i, stack, simulate);
            if (stack.isEmpty()) return ItemStack.EMPTY;
        }
        return stack;
    }

    /** Fills the slots that hold the same item first, then the empty ones; returns what did not fit. */
    public static ItemStack insertItemStacked(@Nullable IItemHandler inventory, ItemStack stack, boolean simulate) {
        if (inventory == null || stack.isEmpty()) return stack;
        if (!stack.isStackable()) return insertItem(inventory, stack, simulate);
        int size = inventory.getSlots();
        for (int i = 0; i < size; i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(slot, stack)) {
                stack = inventory.insertItem(i, stack, simulate);
                if (stack.isEmpty()) break;
            }
        }
        if (!stack.isEmpty()) {
            for (int i = 0; i < size; i++) {
                if (inventory.getStackInSlot(i).isEmpty()) {
                    stack = inventory.insertItem(i, stack, simulate);
                    if (stack.isEmpty()) break;
                }
            }
        }
        return stack;
    }
}
