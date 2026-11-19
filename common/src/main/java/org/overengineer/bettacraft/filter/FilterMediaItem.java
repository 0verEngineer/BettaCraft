package org.overengineer.bettacraft.filter;

import org.overengineer.bettacraft.util.SinkList;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * M24: a filter medium (filter sponge, ceramic rings, activated carbon, filter peat). Its dirt is its damage (0..
 * {@link #MAX_DIRT}); right-click a water cauldron with a dirty sponge or ceramic rings to wash them (one level of water).
 */
public class FilterMediaItem extends Item {
    public static final int MAX_DIRT = 1000;
    private final FilterMedium medium;

    public FilterMediaItem(FilterMedium medium, Properties properties) {
        super(properties.durability(MAX_DIRT));
        this.medium = medium;
    }

    public FilterMedium medium() {
        return medium;
    }

    /** 0..1 how dirty (used up) a stack is. */
    public static double dirt(ItemStack stack) {
        return stack.isDamageableItem() ? stack.getDamageValue() / (double) stack.getMaxDamage() : 0;
    }

    public static FilterMedium mediumOf(ItemStack stack) {
        return stack.getItem() instanceof FilterMediaItem m ? m.medium : FilterMedium.NONE;
    }

    /** M26: mulm a medium of this dirt gives when it is washed (see {@link FilterMedium#mulmFrom}). */
    public static int mulmFrom(double dirt) {
        return FilterMedium.mulmFrom(dirt);
    }

    // M52b: not repairable (no repairable component; isValidRepairItem is gone since 1.21.2)

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState s = level.getBlockState(pos);
        ItemStack stack = ctx.getItemInHand();
        if (!s.is(Blocks.WATER_CAULDRON)) return InteractionResult.PASS;
        if (!medium.cleanable()) {
            if (!level.isClientSide() && ctx.getPlayer() != null) ctx.getPlayer().sendOverlayMessage(Component.translatable("message.bettacraft.media.not_cleanable").withStyle(ChatFormatting.GOLD));
            return InteractionResult.FAIL;
        }
        if (stack.getDamageValue() <= 0) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            // M26: the mulm washed out of a dirty medium (food for the tubifex dish)
            int mulm = mulmFrom(dirt(stack));
            stack.setDamageValue(0);
            if (mulm > 0 && ctx.getPlayer() != null) {
                ItemStack m = new ItemStack(org.overengineer.bettacraft.registry.ModItems.MULM.get(), mulm);
                if (!ctx.getPlayer().getInventory().add(m)) ctx.getPlayer().drop(m, false);
            }
            LayeredCauldronBlock.lowerFillLevel(s, level, pos);
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.6F, 1.2F);
            if (ctx.getPlayer() != null) ctx.getPlayer().sendOverlayMessage(Component.translatable("message.bettacraft.media.cleaned", stack.getHoverName())
                    .withStyle(ChatFormatting.AQUA));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        List<Component> tooltip = new SinkList<>(tooltipAdder);
        int pct = (int) Math.round(dirt(stack) * 100);
        String key = medium.cleanable() ? "tooltip.bettacraft.media.dirt" : "tooltip.bettacraft.media.used";
        tooltip.add(Component.translatable(key, pct).withStyle(pct >= 80 ? ChatFormatting.RED : pct >= 40 ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.bettacraft.media." + medium.getSerializedName()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
