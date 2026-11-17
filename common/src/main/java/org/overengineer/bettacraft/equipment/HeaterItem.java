package org.overengineer.bettacraft.equipment;

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

/**
 * Aquarium heater (PLAN 6.1) or cooler (M9, {@link #cooling()}). Placed inside the tank interior it stands as a rod of
 * {@link SegmentBlock}s (M3) and works while its lowest segment is under water; or into one of the two equipment slots
 * of an external filter (M4). Capacities add up; heaters only heat, coolers only cool.
 */
public class HeaterItem extends Item {
    private final int capacityBlocks;
    private final SegmentBlock.Size size;
    private final boolean cooling;

    public HeaterItem(int capacityBlocks, SegmentBlock.Size size, Properties properties) {
        this(capacityBlocks, size, false, properties);
    }

    public HeaterItem(int capacityBlocks, SegmentBlock.Size size, boolean cooling, Properties properties) {
        super(properties);
        this.capacityBlocks = capacityBlocks;
        this.size = size;
        this.cooling = cooling;
    }

    /** Number of water blocks this heater (or cooler) can keep at the target temperature. */
    public int capacityBlocks() {
        return capacityBlocks;
    }

    public SegmentBlock.Size size() {
        return size;
    }

    /** True for coolers (M9). */
    public boolean cooling() {
        return cooling;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return EquipmentPlacement.place(context, cooling ? SegmentBlock.Kind.COOLER : SegmentBlock.Kind.HEATER, size);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        List<Component> tooltip = new SinkList<>(tooltipAdder);
        String k = cooling ? "cooler" : "heater";
        tooltip.add(Component.translatable("tooltip.bettacraft." + k + ".capacity", capacityBlocks).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.bettacraft." + k + ".usage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
