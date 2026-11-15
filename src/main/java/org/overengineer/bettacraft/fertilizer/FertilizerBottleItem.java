package org.overengineer.bettacraft.fertilizer;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * M37: a bottle of liquid fertilizer with {@value Fertilizer#BOTTLE_PORTIONS} portions (shown as wear). Right-click a
 * tank: one portion into its water ({@link Fertilizer#useOnTank}); the last one leaves the empty glass bottle.
 */
public class FertilizerBottleItem extends Item {
    public FertilizerBottleItem(Properties props) {
        super(props.durability(Fertilizer.BOTTLE_PORTIONS).craftRemainder(net.minecraft.world.item.Items.GLASS_BOTTLE));
    }

    public static int portionsLeft(ItemStack s) {
        return Math.max(0, s.getMaxDamage() - s.getDamageValue());
    }

    /** The bottle after one portion: one less, or the empty glass bottle. */
    public static ItemStack usePortion(ItemStack s) {
        if (portionsLeft(s) <= 1) return s.getCount() > 1 ? s.copyWithCount(s.getCount() - 1) : Fertilizer.emptyBottle();
        ItemStack n = s.copy();
        n.setDamageValue(s.getDamageValue() + 1);
        return n;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bettacraft.liquid_fertilizer", portionsLeft(stack), Fertilizer.BOTTLE_PORTIONS).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.bettacraft.liquid_fertilizer.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
