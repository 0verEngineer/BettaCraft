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
 * Internal filter (PLAN 6.2 / 7.3): placed in the tank interior as a small box of {@link SegmentBlock}s with the outlet
 * facing the player. The miniature shows a water jet and bubbles at the outlet (M4). Filters have no effect on the
 * simulation (OFFEN-11).
 */
public class InternalFilterItem extends Item {
    public InternalFilterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return EquipmentPlacement.place(context, null);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bettacraft.internal_filter.usage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
