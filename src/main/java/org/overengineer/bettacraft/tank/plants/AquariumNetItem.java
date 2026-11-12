package org.overengineer.bettacraft.tank.plants;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Aquarium net (M12): right-click on a tank takes out the floating plants where the player looks at the water surface
 * (from above, from the side through the glass, any angle); sneaking thins the whole tank out by half. The handling is
 * in {@link FloatingPlantEvents}; the net has no other use (it does not catch fish, the bucket and bags do that).
 */
public class AquariumNetItem extends Item {
    public AquariumNetItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.bettacraft.tank_net").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.bettacraft.tank_net.sneak").withStyle(ChatFormatting.DARK_GRAY));
    }
}
