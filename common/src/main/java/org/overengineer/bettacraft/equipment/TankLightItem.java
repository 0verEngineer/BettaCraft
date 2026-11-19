package org.overengineer.bettacraft.equipment;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;

/**
 * M18: the aquarium light item. The colour lives in the vanilla block state component ({@code minecraft:block_state},
 * {@code color=red}), so the placed block gets it without any code of ours; white lights have no component (the
 * lights of earlier versions, they stack with new white ones).
 */
public class TankLightItem extends BlockItem {
    public TankLightItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static DyeColor colorOf(ItemStack stack) {
        BlockItemStateProperties p = stack.get(DataComponents.BLOCK_STATE);
        if (p == null) return DyeColor.WHITE;
        DyeColor c = p.get(TankLightBlock.COLOR);
        return c == null ? DyeColor.WHITE : c;
    }

    /** One light of the given colour (white: the plain item). */
    /** M52c (26.1): the stack template of a light of a colour (recipes, made before the item components are bound). */
    public static net.minecraft.world.item.ItemStackTemplate template(net.minecraft.world.level.ItemLike item, DyeColor color, int count) {
        var b = net.minecraft.core.component.DataComponentPatch.builder();
        if (color != DyeColor.WHITE) b.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(TankLightBlock.COLOR, color));
        return new net.minecraft.world.item.ItemStackTemplate(item.asItem().builtInRegistryHolder(), count, b.build());
    }

    public static ItemStack of(net.minecraft.world.level.ItemLike item, DyeColor color, int count) {
        ItemStack s = new ItemStack(item, count);
        if (color != DyeColor.WHITE) s.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(TankLightBlock.COLOR, color));
        return s;
    }

    @Override
    public Component getName(ItemStack stack) {
        DyeColor c = colorOf(stack);
        if (c == DyeColor.WHITE) return super.getName(stack);
        return Component.translatable(getDescriptionId() + ".colored", Component.translatable("color.minecraft." + c.getName()));
    }
}
