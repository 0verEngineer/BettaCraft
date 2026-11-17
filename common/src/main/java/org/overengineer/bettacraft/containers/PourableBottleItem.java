package org.overengineer.bettacraft.containers;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * M22.1: a bottle of water or live food (salt water, swamp water, infusoria, artemia nauplii). In a tank, a culture tub
 * or a breeder it works as before (their blocks take it first). Anywhere else sneak + right-click, or right-click into
 * the air, pours it out: splashes and a splash sound, no water block (no mud on dirt either), an empty glass bottle
 * remains ({@link ContainerRules#pour}).
 */
public class PourableBottleItem extends Item {
    public PourableBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.bettacraft.pour").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (!player.isSecondaryUseActive() && hit.getType() != HitResult.Type.MISS) return InteractionResultHolder.pass(stack);
        ContainerRules.pour(level, player, ContainerRules.pourSpot(player, hit));
        return InteractionResultHolder.sidedSuccess(ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)), level.isClientSide());
    }
}
