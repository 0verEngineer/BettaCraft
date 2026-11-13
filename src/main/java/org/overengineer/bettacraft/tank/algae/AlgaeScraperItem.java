package org.overengineer.bettacraft.tank.algae;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Algae scraper (M14): right-click on the glass of a tank cleans that glass face of algae; sneaking cleans the whole
 * side. Wears a little with every face it cleans. The handling is in {@link org.overengineer.bettacraft.critters.CritterEvents}.
 */
public class AlgaeScraperItem extends Item {
    public AlgaeScraperItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.bettacraft.algae_scraper").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.bettacraft.algae_scraper.sneak").withStyle(ChatFormatting.DARK_GRAY));
    }
}
