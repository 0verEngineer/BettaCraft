package org.overengineer.bettacraft.platform.fluid;

import net.minecraft.world.item.ItemStack;

/** M52: the fluid of an item (a bucket, a tank item); {@link #getContainer()} is the item after filling or draining. */
public interface IFluidHandlerItem extends IFluidHandler {
    ItemStack getContainer();
}
