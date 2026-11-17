package org.overengineer.bettacraft.plumbing;

import org.overengineer.bettacraft.util.SinkList;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * M40 part c (C1): the pipe cover, a tool (not used up). In the main hand, with a full block in the off hand, a right-click
 * on a filter pipe or a pipe bundle puts one of that block on as its cover: the pipe is drawn as that block (turned as it
 * would be placed), its lines and connections stay. A cover already on comes back. Sneak-right-click with the pipe cover
 * takes the cover off again and gives its block (or another mod's facade it came from) back. Sneaking with an empty hand
 * on a bundle still takes a line out, so the cover only comes off with this tool.
 */
public class PipeCoverItem extends Item {
    public PipeCoverItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!FilterPipes.isPipe(level.getBlockState(pos))) {
            if (!level.isClientSide()) player.displayClientMessage(Component.translatable("message.bettacraft.pipe_cover.where").withStyle(ChatFormatting.GOLD), true);
            return InteractionResult.PASS;
        }
        if (player.isSecondaryUseActive()) return takeOff(level, pos, player);
        ItemStack block = player.getItemInHand(ctx.getHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!(block.getItem() instanceof BlockItem bi)) {
            if (!level.isClientSide()) player.displayClientMessage(Component.translatable("message.bettacraft.pipe_cover.need_block").withStyle(ChatFormatting.GOLD), true);
            return InteractionResult.SUCCESS;
        }
        BlockState cover = coverFor(bi, block, player, ctx);
        if (PipeCovers.apply(level, pos, cover, block, player)) {
            if (!level.isClientSide() && !player.getAbilities().instabuild) block.shrink(1);
        }
        // success either way: the block in the other hand is not placed instead
        return InteractionResult.SUCCESS;
    }

    /** The cover a block item gives: turned as it would be placed against the clicked face (logs, pillars), else its default. */
    static BlockState coverFor(BlockItem bi, ItemStack block, Player player, UseOnContext ctx) {
        BlockState s = null;
        try {
            InteractionHand other = ctx.getHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            s = bi.getBlock().getStateForPlacement(new BlockPlaceContext(player, other, block,
                    new BlockHitResult(ctx.getClickLocation(), ctx.getClickedFace(), ctx.getClickedPos(), ctx.isInside())));
        } catch (RuntimeException ignored) {
            // a block that looks at its neighbours in odd ways: its default state
        }
        if (s == null || !PipeCovers.fits(s)) s = bi.getBlock().defaultBlockState();
        return s;
    }

    private static InteractionResult takeOff(Level level, BlockPos pos, Player player) {
        if (PipeCovers.coverAt(level, pos) == null) {
            if (!level.isClientSide()) player.displayClientMessage(Component.translatable("message.bettacraft.pipe_cover.none").withStyle(ChatFormatting.GOLD), true);
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) PipeCovers.give(level, pos, player, PipeCovers.remove(level, pos));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        List<Component> tooltip = new SinkList<>(tooltipAdder);
        tooltip.add(Component.translatable("tooltip.bettacraft.pipe_cover.usage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
