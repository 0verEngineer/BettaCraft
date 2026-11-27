package org.overengineer.bettacraft.plumbing;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.plumbing.interior.PlumbingItem;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.interior.InteriorManager;

/**
 * M68: the glass and the stainless steel pipe. Placed like a block it joins every neighbour that fits ({@link PipeBlock}).
 * Sneak + right-click on a pipe or a bundle puts it into that block (a bundle of up to four pipes, {@link PipeBundleBlock}).
 * Placed inside a tank interior it is an interior pipe there (the M24 interior pipe, glass or steel to look at; same function:
 * it leads the water from a bulkhead or a pipe dock to the outlets and guards; no pipe through the tank).
 */
public class PipeItem extends BlockItem {
    private final boolean steel;

    public PipeItem(PipeBlock block, boolean steel, Properties properties) {
        super(block, properties);
        this.steel = steel;
    }

    public boolean steel() {
        return steel;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        if (ctx.isSecondaryUseActive() && Pipes.isPipe(level.getBlockState(pos))) {
            if (!level.isClientSide) PipeBundleBlock.addPipe(level, pos, steel, ctx.getPlayer(), ctx.getItemInHand());
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useOn(ctx);
    }

    /** The interior pipe block this pipe is inside a tank interior. */
    public BlockState interiorState() {
        return (steel ? ModBlocks.INTERIOR_STEEL_PIPE.get() : ModBlocks.INTERIOR_PIPE.get()).defaultBlockState();
    }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        if (!InteriorManager.isInterior(level)) return super.place(ctx);
        if (!Features.enabled(Feature.INTERIOR_PIPES)) {
            say(ctx, "message.bettacraft.feature_off");
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide && ctx.getPlayer() != null && !InteriorManager.canBuild(ctx.getPlayer(), ctx.getClickedPos())) return InteractionResult.FAIL;
        // M40: no pipe that leads into the tank and out again
        if (!level.isClientSide && PlumbingItem.joinsTwoPorts((ServerLevel) level, ctx.getClickedPos())) {
            say(ctx, "message.bettacraft.plumbing.pass_through");
            return InteractionResult.FAIL;
        }
        return super.place(ctx);
    }

    @Nullable
    @Override
    protected BlockState getPlacementState(BlockPlaceContext ctx) {
        if (!InteriorManager.isInterior(ctx.getLevel())) return super.getPlacementState(ctx);
        BlockState s = interiorState().getBlock().getStateForPlacement(ctx);
        return s != null && canPlace(ctx, s) ? s : null;
    }

    private static void say(BlockPlaceContext ctx, String key) {
        if (!ctx.getLevel().isClientSide && ctx.getPlayer() != null)
            ctx.getPlayer().displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.bettacraft.pipe.rate", PipeBlock.rate(steel) * 20).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.bettacraft.pipe.usage").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.bettacraft.pipe.bundle").withStyle(ChatFormatting.DARK_GRAY));
    }
}
