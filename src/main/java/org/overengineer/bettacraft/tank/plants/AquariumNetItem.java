package org.overengineer.bettacraft.tank.plants;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        BlockPos pos = hit.getBlockPos();
        if (level.getBlockState(pos).getBlock() instanceof TankBlock) return InteractionResultHolder.pass(stack);
        if (!level.getFluidState(pos).is(FluidTags.WATER)) return InteractionResultHolder.pass(stack);
        if (level instanceof ServerLevel server) {
            boolean used = org.overengineer.bettacraft.critters.NetCatching.tryCatch(server, player, hand, pos);
            return used ? InteractionResultHolder.consume(stack) : InteractionResultHolder.pass(stack);
        }
        return player.getCooldowns().isOnCooldown(this) ? InteractionResultHolder.pass(stack) : InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.bettacraft.tank_net").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.bettacraft.tank_net.sneak").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.bettacraft.tank_net.water").withStyle(ChatFormatting.DARK_GRAY));
    }
}
