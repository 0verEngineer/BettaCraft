package org.overengineer.bettacraft.util;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * M52b: the tooltip of a block's item. {@code Block#appendHoverText} is gone since 1.21.2; {@code BlockItemMixin} asks the
 * block of every block item for these lines.
 */
public interface BlockTooltip {
    void appendHoverText(ItemStack stack, Item.TooltipContext ctx, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag);
}
