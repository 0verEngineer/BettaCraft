package org.overengineer.bettacraft.tank.plants;

import org.overengineer.bettacraft.util.SinkList;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.world.InteractionResult;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.overengineer.bettacraft.tank.TankBlock;

/**
 * Aquarium net (M12): right-click on a tank takes out the floating plants where the player looks at the water surface
 * (from above, from the side through the glass, any angle); sneaking thins the whole tank out by half. M14: aimed at a
 * shrimp, crayfish, snail, a clutch or a moult in a tank it catches that instead (handled in {@link FloatingPlantEvents}),
 * and in open water (rivers, swamps, mangroves, beaches, jungle, caves) it catches small animals into an empty fish bag
 * ({@link org.overengineer.bettacraft.critters.NetCatching}); that wears it a little.
 */
public class AquariumNetItem extends Item {
    public AquariumNetItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        BlockPos pos = hit.getBlockPos();
        if (level.getBlockState(pos).getBlock() instanceof TankBlock) return InteractionResult.PASS;
        // M28.1: a wild floating plant on the water is scooped up
        if (level.getBlockState(pos).getBlock() instanceof org.overengineer.bettacraft.plants.wild.WildFloatingPlantBlock) {
            if (level instanceof ServerLevel) org.overengineer.bettacraft.plants.wild.WildFloatingPlantBlock.scoop(level, pos, player);
            return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
        }
        if (!level.getFluidState(pos).is(FluidTags.WATER)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            boolean used = org.overengineer.bettacraft.critters.NetCatching.tryCatch(server, player, hand, pos);
            return used ? InteractionResult.CONSUME.heldItemTransformedTo(stack) : InteractionResult.PASS;
        }
        return player.getCooldowns().isOnCooldown(stack) ? InteractionResult.PASS : InteractionResult.SUCCESS.heldItemTransformedTo(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        List<Component> tooltip = new SinkList<>(tooltipAdder);
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
        tooltip.add(Component.translatable("tooltip.bettacraft.tank_net").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.bettacraft.tank_net.sneak").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.bettacraft.tank_net.water").withStyle(ChatFormatting.DARK_GRAY));
    }
}
