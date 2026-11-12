package org.overengineer.bettacraft.tank.plants;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A floating plant for the water surface of a tank (M12). Used on a tank it floats where the player looks at the
 * surface ({@link TankPlants#place}); it is no block and cannot be placed in the world.
 */
public class FloatingPlantItem extends Item {
    private final FloatingPlantType type;

    public FloatingPlantItem(FloatingPlantType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public FloatingPlantType type() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.bettacraft." + type.id()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.bettacraft.floating_plant").withStyle(ChatFormatting.DARK_GRAY));
    }
}
