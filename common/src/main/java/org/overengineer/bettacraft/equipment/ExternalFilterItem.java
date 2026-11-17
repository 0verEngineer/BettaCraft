package org.overengineer.bettacraft.equipment;

import org.overengineer.bettacraft.util.SinkList;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
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
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        List<Component> tooltip = new SinkList<>(tooltipAdder);
        tooltip.add(Component.translatable("tooltip.bettacraft.external_filter.usage").withStyle(ChatFormatting.GRAY));
        int n = 0;
        for (ItemStack h : FilterHeaters.read(stack)) {
            if (h.isEmpty()) continue;
            n++;
            tooltip.add(Component.literal("  ").append(h.getHoverName()).withStyle(ChatFormatting.GOLD));
        }
        if (n == 0) tooltip.add(Component.translatable("tooltip.bettacraft.external_filter.no_heaters").withStyle(ChatFormatting.DARK_GRAY));
    }
}
