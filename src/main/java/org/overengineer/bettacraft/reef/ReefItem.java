package org.overengineer.bettacraft.reef;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** M20: sea salt and calcium additive: used on a tank ({@link ReefItems#useOnTank}), with a usage tooltip. */
public class ReefItem extends Item {
    private final String name;

    public ReefItem(Properties properties, String name) {
        super(properties);
        this.name = name;
    }

    /** Sneaking skips the block's own interaction: the tank is handled here then (sea salt: as much as it needs). */
    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext ctx) {
        if (ctx.getPlayer() == null || !(ctx.getLevel().getBlockState(ctx.getClickedPos()).getBlock() instanceof org.overengineer.bettacraft.tank.TankBlock)) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        var r = ReefItems.useOnTank(ctx.getItemInHand(), ctx.getLevel(), ctx.getClickedPos(), ctx.getPlayer(), ctx.getHand());
        return r.result();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bettacraft." + name).withStyle(ChatFormatting.GRAY));
    }
}
