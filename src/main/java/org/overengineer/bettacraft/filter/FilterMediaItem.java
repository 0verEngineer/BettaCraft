package org.overengineer.bettacraft.filter;

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

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState s = level.getBlockState(pos);
        ItemStack stack = ctx.getItemInHand();
        if (!s.is(Blocks.WATER_CAULDRON)) return InteractionResult.PASS;
        if (!medium.cleanable()) {
            if (!level.isClientSide && ctx.getPlayer() != null) ctx.getPlayer().displayClientMessage(
                    Component.translatable("message.bettacraft.media.not_cleanable").withStyle(ChatFormatting.GOLD), true);
            return InteractionResult.FAIL;
        }
        if (stack.getDamageValue() <= 0) return InteractionResult.PASS;
        if (!level.isClientSide) {
            stack.setDamageValue(0);
            LayeredCauldronBlock.lowerFillLevel(s, level, pos);
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.6F, 1.2F);
            if (ctx.getPlayer() != null) ctx.getPlayer().displayClientMessage(Component.translatable("message.bettacraft.media.cleaned", stack.getHoverName())
                    .withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int pct = (int) Math.round(dirt(stack) * 100);
        String key = medium.cleanable() ? "tooltip.bettacraft.media.dirt" : "tooltip.bettacraft.media.used";
        tooltip.add(Component.translatable(key, pct).withStyle(pct >= 80 ? ChatFormatting.RED : pct >= 40 ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.bettacraft.media." + medium.getSerializedName()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
