package org.overengineer.bettacraft.food;

import org.overengineer.bettacraft.util.SinkList;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** M26: tubifex worms; a stack from a foul dish says so (they stress the fish a little). */
public class LiveFoodItem extends Item {
    public LiveFoodItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        List<Component> tooltip = new SinkList<>(tooltipAdder);
        if (LiveFood.poor(stack)) tooltip.add(Component.translatable("tooltip.bettacraft.live_food.poor").withStyle(ChatFormatting.GOLD));
    }
}
