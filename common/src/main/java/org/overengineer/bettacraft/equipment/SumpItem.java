package org.overengineer.bettacraft.equipment;

import org.overengineer.bettacraft.util.SinkList;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** M49 part a: the sump: goes into the sump slot of a cabinet under a tank; the tooltip lists what its chambers hold. */
public class SumpItem extends Item {
    public SumpItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        List<Component> tooltip = new SinkList<>(tooltipAdder);
        tooltip.add(Component.translatable("tooltip.bettacraft.sump.usage").withStyle(ChatFormatting.GRAY));
        int n = 0;
        for (ItemStack s : Sump.read(stack)) {
            if (s.isEmpty()) continue;
            n++;
            Component name = s.getCount() > 1 ? Component.literal(s.getCount() + " × ").append(s.getHoverName()) : s.getHoverName();
            tooltip.add(Component.literal("  ").append(name).withStyle(ChatFormatting.GOLD));
        }
        if (n == 0) tooltip.add(Component.translatable("tooltip.bettacraft.sump.empty").withStyle(ChatFormatting.DARK_GRAY));
    }
}
