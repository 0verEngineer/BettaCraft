package org.overengineer.bettacraft.equipment;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import org.overengineer.bettacraft.tank.interior.InteriorManager;

/** M23: the air stone; it can only be placed in a tank interior (and only with features.aeration). */
public class AirStoneItem extends BlockItem {
    public AirStoneItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        if (!InteriorManager.isInterior(ctx.getLevel())) {
            if (!ctx.getLevel().isClientSide && ctx.getPlayer() != null) ctx.getPlayer().displayClientMessage(
                    Component.translatable("message.bettacraft.equipment.interior_only").withStyle(ChatFormatting.GOLD), true);
            return InteractionResult.FAIL;
        }
        if (!org.overengineer.bettacraft.config.CommonConfig.aeration()) {
            if (!ctx.getLevel().isClientSide && ctx.getPlayer() != null) ctx.getPlayer().displayClientMessage(
                    Component.translatable("message.bettacraft.feature_off").withStyle(ChatFormatting.GOLD), true);
            return InteractionResult.FAIL;
        }
        if (!ctx.getLevel().isClientSide && ctx.getPlayer() != null && !InteriorManager.canBuild(ctx.getPlayer(), ctx.getClickedPos())) return InteractionResult.FAIL;
        return super.place(ctx);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bettacraft.air_stone.usage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
