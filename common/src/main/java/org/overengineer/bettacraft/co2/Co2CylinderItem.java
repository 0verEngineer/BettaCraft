package org.overengineer.bettacraft.co2;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.overengineer.bettacraft.registry.ModDataComponents;

/** M25: the CO2 cylinder as an item: its gas (bar and tooltip) and whether a solenoid valve sits on it. */
public class Co2CylinderItem extends BlockItem {
    public Co2CylinderItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static float gas(ItemStack s) {
        Float g = s.get(ModDataComponents.CO2_GAS.get());
        return g == null ? 0 : Math.max(0, g);
    }

    public static void setGas(ItemStack s, float g) {
        g = Math.max(0, Math.min(Co2CylinderBlockEntity.capacity(), g));
        if (g <= 0) s.remove(ModDataComponents.CO2_GAS.get());
        else s.set(ModDataComponents.CO2_GAS.get(), g);
    }

    public static boolean full(ItemStack s) {
        return gas(s) >= Co2CylinderBlockEntity.capacity() - 1e-4F;
    }

    /** 0..1 how full. */
    public static float share(ItemStack s) {
        return Math.min(1F, gas(s) / Math.max(0.001F, Co2CylinderBlockEntity.capacity()));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13F * share(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x8FB8D8;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        float g = gas(stack);
        tooltip.add(Component.translatable("tooltip.bettacraft.co2_cylinder.gas", Math.round(100 * share(stack)),
                String.format(java.util.Locale.ROOT, "%.0f", g)).withStyle(g > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        if (Boolean.TRUE.equals(stack.get(ModDataComponents.CO2_SOLENOID.get())))
            tooltip.add(Component.translatable("tooltip.bettacraft.co2_cylinder.solenoid").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
