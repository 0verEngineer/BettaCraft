package org.overengineer.bettacraft.reef;

import org.overengineer.bettacraft.util.SinkList;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import org.overengineer.bettacraft.equipment.EquipmentPlacement;
import org.overengineer.bettacraft.equipment.SegmentBlock;

/**
 * M20: flow pump. Placed in a tank interior like the internal filter (a small box, the outlet facing away from the
 * player); under water it moves the water for the corals ({@link TankReef#flow}).
 */
public class FlowPumpItem extends Item {
    public FlowPumpItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return EquipmentPlacement.place(context, SegmentBlock.Kind.PUMP, SegmentBlock.Size.SMALL);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        List<Component> tooltip = new SinkList<>(tooltipAdder);
        tooltip.add(Component.translatable("tooltip.bettacraft.flow_pump.usage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
