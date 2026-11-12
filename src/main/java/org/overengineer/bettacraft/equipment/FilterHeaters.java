package org.overengineer.bettacraft.equipment;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Heaters and coolers (M9) inside an external filter (PLAN 6.1/6.2: two slots, any size and any mix). The placed filter keeps them in its block
 * entity; the filter item (dropped block, or the filter in a cabinet slot) keeps them in {@code minecraft:container}.
 */
public final class FilterHeaters {
    public static final int SLOTS = 2;

    private FilterHeaters() {}

    /** Fits into a filter slot: a heater or a cooler. */
    public static boolean isHeater(ItemStack stack) {
        return stack.getItem() instanceof HeaterItem;
    }

    public static boolean isCooler(ItemStack stack) {
        return stack.getItem() instanceof HeaterItem h && h.cooling();
    }

    /** Heating capacity (water blocks); 0 for coolers. */
    public static int capacity(ItemStack heater) {
        return heater.getItem() instanceof HeaterItem h && !h.cooling() ? h.capacityBlocks() : 0;
    }

    /** Cooling capacity (water blocks); 0 for heaters. */
    public static int coolingCapacity(ItemStack cooler) {
        return cooler.getItem() instanceof HeaterItem h && h.cooling() ? h.capacityBlocks() : 0;
    }

    /** The two heater slots of a filter item (empty stacks for free slots). */
    public static NonNullList<ItemStack> read(ItemStack filterItem) {
        NonNullList<ItemStack> list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ItemContainerContents c = filterItem.get(DataComponents.CONTAINER);
        if (c != null) c.copyInto(list);
        return list;
    }

    public static void write(ItemStack filterItem, List<ItemStack> heaters) {
        boolean any = false;
        for (ItemStack s : heaters) any |= !s.isEmpty();
        if (!any) filterItem.remove(DataComponents.CONTAINER);
        else filterItem.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(new ArrayList<>(heaters)));
    }

    /** Sum of the heating capacity (water blocks) of the heaters in a filter item. */
    public static int capacityOfItem(ItemStack filterItem) {
        int sum = 0;
        for (ItemStack s : read(filterItem)) sum += capacity(s);
        return sum;
    }

    /** Sum of the cooling capacity (water blocks) of the coolers in a filter item. */
    public static int coolingOfItem(ItemStack filterItem) {
        int sum = 0;
        for (ItemStack s : read(filterItem)) sum += coolingCapacity(s);
        return sum;
    }
}
