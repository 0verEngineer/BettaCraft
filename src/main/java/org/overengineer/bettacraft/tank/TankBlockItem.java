package org.overengineer.bettacraft.tank;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.overengineer.bettacraft.registry.ModDataComponents;

/** Tank item; the glass it is made of is the {@code bettacraft:glass} component. */
public class TankBlockItem extends BlockItem {
    public TankBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static ResourceLocation getGlass(ItemStack stack) {
        ResourceLocation g = stack.get(ModDataComponents.GLASS.get());
        return g == null ? TankBlockEntity.DEFAULT_GLASS : g;
    }

    public static ItemStack setGlass(ItemStack stack, ResourceLocation glass) {
        stack.set(ModDataComponents.GLASS.get(), glass);
        return stack;
    }

    public static Block glassBlock(ItemStack stack) {
        Block b = BuiltInRegistries.BLOCK.get(getGlass(stack));
        return b == Blocks.AIR ? Blocks.GLASS : b;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("block.bettacraft.tank.with_glass", glassBlock(stack).getName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.bettacraft.tank").withStyle(net.minecraft.ChatFormatting.GRAY));
        if (flag.isAdvanced()) {
            tooltip.add(Component.literal(getGlass(stack).toString()).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
    }
}
