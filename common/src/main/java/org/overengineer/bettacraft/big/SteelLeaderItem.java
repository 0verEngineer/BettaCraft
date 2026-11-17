package org.overengineer.bettacraft.big;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * M30: a steel leader (a short piece of stainless steel wire with a swivel) held in the other hand while fishing: big fish
 * of the water bite now and then and go into an empty transport tub ({@link BigCatching#onFished}). An item rather than a
 * rod recipe, so any rod (also of other mods) works with it.
 */
public class SteelLeaderItem extends Item {
    public SteelLeaderItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.bettacraft.steel_leader").withStyle(ChatFormatting.GRAY));
    }
}
