package org.overengineer.bettacraft.winding;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * M37: the crank. Right-click a device with a spring ({@link Winding}) to wind it a step; holding the button winds on
 * (about five clicks a second, one charge in five seconds). It works before the block's own click, so the food maker's
 * window does not open while you wind it.
 */
public class CrankItem extends Item implements org.overengineer.bettacraft.platform.UseFirstItem {
    public CrankItem(Properties props) {
        super(props);
    }

    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (Winding.find(level, ctx.getClickedPos()) == null) return InteractionResult.PASS;
        if (!level.isClientSide) Winding.wind(level, ctx.getClickedPos(), ctx.getPlayer());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        // a block without a spring: say so (server), so a player knows the crank is not broken
        if (!ctx.getLevel().isClientSide) Winding.wind(ctx.getLevel(), ctx.getClickedPos(), ctx.getPlayer());
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bettacraft.crank").withStyle(ChatFormatting.GRAY));
    }
}
