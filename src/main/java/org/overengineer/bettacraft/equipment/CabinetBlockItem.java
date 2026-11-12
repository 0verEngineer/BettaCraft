package org.overengineer.bettacraft.equipment;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.overengineer.bettacraft.registry.ModDataComponents;

/** Cabinet item: remembers its planks ({@code bettacraft:wood}) and is named after them ("Oak Cabinet"). */
public class CabinetBlockItem extends BlockItem {
    public CabinetBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static ResourceLocation getWood(ItemStack stack) {
        ResourceLocation w = stack.get(ModDataComponents.WOOD.get());
        return w == null ? CabinetBlockEntity.DEFAULT_WOOD : w;
    }

    public static ItemStack setWood(ItemStack stack, ResourceLocation wood) {
        stack.set(ModDataComponents.WOOD.get(), wood);
        return stack;
    }

    public static Block woodBlock(ItemStack stack) {
        Block b = BuiltInRegistries.BLOCK.get(getWood(stack));
        return b == Blocks.AIR ? Blocks.OAK_PLANKS : b;
    }

    static Component nameFor(ResourceLocation wood) {
        Block b = BuiltInRegistries.BLOCK.get(wood);
        return Component.translatable("block.bettacraft.cabinet.with_wood", (b == Blocks.AIR ? Blocks.OAK_PLANKS : b).getName());
    }

    @Override
    public Component getName(ItemStack stack) {
        return nameFor(getWood(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bettacraft.cabinet").withStyle(ChatFormatting.GRAY));
    }
}
