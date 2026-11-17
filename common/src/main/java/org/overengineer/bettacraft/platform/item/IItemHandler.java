package org.overengineer.bettacraft.platform.item;

import net.minecraft.world.item.ItemStack;

/**
 * M52: an inventory other blocks and mods can move items in and out of (slots, simulate first). The loader-independent
 * counterpart of NeoForge's interface of the same name; the platform layer offers it to other mods (NeoForge: the item
 * handler capability; Fabric: the transfer API) and wraps theirs into it.
 */
public interface IItemHandler {
    int getSlots();

    ItemStack getStackInSlot(int slot);

    /** Inserts into a slot; returns what did not fit (empty when all went in). Must not change {@code stack}. */
    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    /** Takes up to {@code amount} out of a slot; returns what was (or would be) taken. */
    ItemStack extractItem(int slot, int amount, boolean simulate);

    int getSlotLimit(int slot);

    boolean isItemValid(int slot, ItemStack stack);
}
