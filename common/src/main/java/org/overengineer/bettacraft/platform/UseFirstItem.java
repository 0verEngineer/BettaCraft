package org.overengineer.bettacraft.platform;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * M52: an item that acts on a block before the block's own use (NeoForge calls onItemUseFirst by this name; Fabric's
 * glue from its block use callback).
 */
public interface UseFirstItem {
    InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx);
}
