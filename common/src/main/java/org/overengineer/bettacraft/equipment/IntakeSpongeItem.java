package org.overengineer.bettacraft.equipment;

import org.overengineer.bettacraft.util.SinkList;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * M23: intake sponge (pre-filter sponge) for a motor filter: used on an internal filter or corner filter in the interior,
 * on an external filter or on a cabinet with a filter it is fitted over the intake, and the filter no longer sucks in
 * fry. It comes back when the filter is broken (or the cabinet's filter is taken out).
 */
public class IntakeSpongeItem extends Item {
    public IntakeSpongeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (!Features.enabled(Feature.MORE_INTERNAL_FILTERS)) return InteractionResult.PASS;
        Regulator.Device d = Regulator.find(level, pos);
        if (d == null || !d.takesSponge()) return InteractionResult.PASS;
        if (d.hasSponge()) {
            if (!level.isClientSide() && player != null) player.displayClientMessage(Component.translatable("message.bettacraft.intake_sponge.already"), true);
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player != null && !player.mayUseItemAt(pos, ctx.getClickedFace(), ctx.getItemInHand())) return InteractionResult.FAIL;
        d.setSponge(true);
        if (player == null || !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
        if (player != null) player.displayClientMessage(Component.translatable("message.bettacraft.intake_sponge.fitted"), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        List<Component> tooltip = new SinkList<>(tooltipAdder);
        tooltip.add(Component.translatable("tooltip.bettacraft.intake_sponge.usage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
