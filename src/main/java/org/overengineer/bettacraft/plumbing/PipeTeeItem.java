package org.overengineer.bettacraft.plumbing;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

/**
 * M40: the pipe T-piece. Used on a filter pipe it makes that pipe a branch: it then joins every neighbour that fits (up to
 * all six sides), so one filter feeds several tanks. Breaking the branch gives the pipe and the T-piece back.
 */
public class PipeTeeItem extends Item {
    public PipeTeeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        // a filter pipe takes it in its own use handler; anywhere else it only says where it goes
        if (!ctx.getLevel().isClientSide && ctx.getPlayer() != null && !(ctx.getLevel().getBlockState(ctx.getClickedPos()).getBlock() instanceof FilterPipeBlock))
            ctx.getPlayer().displayClientMessage(Component.translatable("message.bettacraft.pipe_tee.where").withStyle(ChatFormatting.GOLD), true);
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bettacraft.pipe_tee.usage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
