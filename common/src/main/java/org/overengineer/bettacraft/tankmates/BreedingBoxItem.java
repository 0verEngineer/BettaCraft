package org.overengineer.bettacraft.tankmates;

import org.overengineer.bettacraft.util.SinkList;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * M19: a breeding box (right-click on a tank hangs it inside the glass, the tank menu takes it out again). Young guppies,
 * endlers and platys born while it hangs in the tank fall into it and are safe from the adults until they are juveniles.
 */
public class BreedingBoxItem extends Item {
    public BreedingBoxItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        List<Component> tooltip = new SinkList<>(tooltipAdder);
        tooltip.add(Component.translatable("tooltip.bettacraft.breeding_box").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.bettacraft.breeding_box.usage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
