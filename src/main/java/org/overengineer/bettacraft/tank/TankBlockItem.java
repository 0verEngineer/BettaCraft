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

    /** M11: textured (default, also for items made before M11) or clean. */
    public static TankStyle getStyle(ItemStack stack) {
        TankStyle s = stack.get(ModDataComponents.TANK_STYLE.get());
        return s == null ? TankStyle.TEXTURED : s;
    }

    public static ItemStack setStyle(ItemStack stack, TankStyle style) {
        stack.set(ModDataComponents.TANK_STYLE.get(), style);
        return stack;
    }

    /** A tank item of the given glass and style. */
    public static ItemStack of(net.minecraft.world.level.ItemLike item, ResourceLocation glass, TankStyle style, int count) {
        ItemStack s = new ItemStack(item, count);
        setGlass(s, glass);
        return setStyle(s, style);
    }

    public static Block glassBlock(ItemStack stack) {
        Block b = BuiltInRegistries.BLOCK.get(getGlass(stack));
        return b == Blocks.AIR ? Blocks.GLASS : b;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (getStyle(stack) == TankStyle.CLEAN) return Component.translatable("block.bettacraft.tank.clean", glassBlock(stack).getName());
        return Component.translatable("block.bettacraft.tank.with_glass", glassBlock(stack).getName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.bettacraft.tank").withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.bettacraft.tank.style", Component.translatable(getStyle(stack).key()))
                .withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
        if (flag.isAdvanced()) {
            tooltip.add(Component.literal(getGlass(stack).toString()).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
    }
}
