package org.overengineer.bettacraft.food;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A jar of dried artemia cysts (brine shrimp eggs, PLAN 6.3): a drop when mining sand. M35.1: one jar is one culture
 * (crafting an artemia culture or one batch of the hatchery uses the jar up) and jars always stack to 64.
 * <p>
 * Old jars (M2..M35) kept their spent uses in the vanilla {@code minecraft:damage} component (without {@code max_damage}).
 * {@link #verifyComponentsAfterLoad} drops that component whenever a stack of jars is made from saved or sent data
 * (every {@code ItemStack} codec, the network codec, commands and {@code copy()} go through it), so an old stack keeps
 * its count, loses its remaining uses and stacks with new jars, in inventories, containers, machine slots, item
 * entities and pipes alike, without a world scan.
 */
public class ArtemiaCystsItem extends Item {
    public ArtemiaCystsItem(Properties properties) {
        super(properties.stacksTo(64));
    }

    /** M35.1: the spent uses of an old jar are gone; the jar is a whole jar again (stacks with new ones). */
    @Override
    public void verifyComponentsAfterLoad(ItemStack stack) {
        stripOldUses(stack);
    }

    /** Removes the old {@code minecraft:damage} of a jar of cysts (M35.1); true if there was one. */
    public static boolean stripOldUses(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArtemiaCystsItem) || !stack.has(DataComponents.DAMAGE)) return false;
        stack.remove(DataComponents.DAMAGE);
        return true;
    }

    /** Uses one jar of {@code stack} (one culture); false if it was empty. */
    public static boolean useOne(ItemStack stack) {
        if (stack.isEmpty()) return false;
        stack.shrink(1);
        return true;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bettacraft.artemia_cysts").withStyle(ChatFormatting.GRAY));
    }
}
