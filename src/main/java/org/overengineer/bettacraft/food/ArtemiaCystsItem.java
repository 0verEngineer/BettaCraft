package org.overengineer.bettacraft.food;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Dried artemia cysts (brine shrimp eggs, PLAN 6.3): rare drop when mining sand. One item lasts {@link #USES} cultures;
 * crafting a culture uses one (the damaged cysts stay in the crafting grid).
 */
public class ArtemiaCystsItem extends Item {
    public static final int USES = 16;

    public ArtemiaCystsItem(Properties properties) {
        super(properties.durability(USES));
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack copy = stack.copyWithCount(1);
        int damage = copy.getDamageValue() + 1;
        if (damage >= copy.getMaxDamage()) return ItemStack.EMPTY;
        copy.setDamageValue(damage);
        return copy;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bettacraft.artemia_cysts", stack.getMaxDamage() - stack.getDamageValue())
                .withStyle(ChatFormatting.GRAY));
    }
}
