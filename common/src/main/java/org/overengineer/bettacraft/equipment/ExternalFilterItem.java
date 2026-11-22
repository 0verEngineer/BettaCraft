package org.overengineer.bettacraft.equipment;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** External filter item: lists the heaters inside (they come along when the filter is broken or moved into a cabinet). */
public class ExternalFilterItem extends BlockItem {
    public ExternalFilterItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bettacraft.external_filter.usage").withStyle(ChatFormatting.GRAY));
        if (org.overengineer.bettacraft.config.Features.enabled(org.overengineer.bettacraft.config.Feature.FILTER_BELOW_TANK)) {
            tooltip.add(Component.translatable("tooltip.bettacraft.external_filter.link").withStyle(ChatFormatting.GRAY)); // M62
        }
        int n = 0;
        for (ItemStack h : FilterHeaters.read(stack)) {
            if (h.isEmpty()) continue;
            n++;
            tooltip.add(Component.literal("  ").append(h.getHoverName()).withStyle(ChatFormatting.GOLD));
        }
        if (n == 0) tooltip.add(Component.translatable("tooltip.bettacraft.external_filter.no_heaters").withStyle(ChatFormatting.DARK_GRAY));
    }
}
