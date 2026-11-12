package org.overengineer.bettacraft.equipment;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Aquarium heater (PLAN 6.1). Placed inside the tank interior it stands as a rod of {@link SegmentBlock}s (M3) and
 * heats while its lowest segment is under water; or into one of the two heater slots of an external filter (M4).
 * Capacities add up.
 */
public class HeaterItem extends Item {
    private final int capacityBlocks;
    private final SegmentBlock.Size size;

    public HeaterItem(int capacityBlocks, SegmentBlock.Size size, Properties properties) {
        super(properties);
        this.capacityBlocks = capacityBlocks;
        this.size = size;
    }

    /** Number of water blocks this heater can keep at the target temperature. */
    public int capacityBlocks() {
        return capacityBlocks;
    }

    public SegmentBlock.Size size() {
        return size;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return EquipmentPlacement.place(context, size);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bettacraft.heater.capacity", capacityBlocks).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.bettacraft.heater.usage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
