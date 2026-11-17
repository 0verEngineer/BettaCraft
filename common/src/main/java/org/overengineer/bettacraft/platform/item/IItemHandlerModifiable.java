package org.overengineer.bettacraft.platform.item;

import net.minecraft.world.item.ItemStack;

/** M52: an {@link IItemHandler} whose slots can also be set directly (menus, loading). */
public interface IItemHandlerModifiable extends IItemHandler {
    void setStackInSlot(int slot, ItemStack stack);
}
