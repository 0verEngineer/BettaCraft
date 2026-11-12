package org.overengineer.bettacraft.equipment;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Aquarium heater (PLAN 6.1). In M1 heaters go into the equipment slots of the tank menu; capacities add up.
 * M3 places them as rods into the tank interior.
 */
public class HeaterItem extends Item {
    private final int capacityBlocks;

    public HeaterItem(int capacityBlocks, Properties properties) {
        super(properties);
        this.capacityBlocks = capacityBlocks;
    }

    /** Number of water blocks this heater can keep at the target temperature. */
    public int capacityBlocks() {
        return capacityBlocks;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bettacraft.heater.capacity", capacityBlocks).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.bettacraft.heater.usage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
