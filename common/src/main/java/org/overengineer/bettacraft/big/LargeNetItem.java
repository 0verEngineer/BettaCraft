package org.overengineer.bettacraft.big;

import org.overengineer.bettacraft.util.SinkList;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankInhabitants;

/**
 * M30: the large net for big animals: used on a wild big animal it catches it into an empty transport tub of the
 * inventory; swung in open water it now and then brings up a big animal of that water ({@link BigCatching#swing}).
 * Small animals go into the aquarium net.
 */
public class LargeNetItem extends Item {
    public LargeNetItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!target.isAlive() || target instanceof Player) return InteractionResult.PASS;
        if (!BigAnimals.isBig(target)) {
            if (!player.level().isClientSide() && TankInhabitants.isWaterAnimal(target)) {
                player.displayClientMessage(Component.translatable("message.bettacraft.big.net_small", target.getDisplayName()), true);
            }
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        int slot = BigCatching.emptyTub(player);
        if (slot < 0) {
            player.displayClientMessage(Component.translatable("message.bettacraft.big.need_tub", target.getDisplayName()), true);
            return InteractionResult.CONSUME;
        }
        org.overengineer.bettacraft.tank.TankBlockEntity from = null;
        if (TankInhabitants.hasTankRef(target) && player.level() instanceof ServerLevel sl) {
            var c = TankInhabitants.resolveController(sl, target);
            if (c != null && !TankInhabitants.canModify(player, c.getTank())) {
                player.displayClientMessage(Component.translatable("message.bettacraft.tank.locked"), true);
                return InteractionResult.CONSUME;
            }
            from = c;
        }
        if (!player.getAbilities().instabuild) stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
        Component name = target.getDisplayName();
        TankInhabitants.release(target); // M35 part b: first (a mother takes her clutch along), then the tub saves her
        ItemStack tub = TransportTubItem.fillWith(target);
        target.discard();
        if (from != null) { // M39 part b: out of its tank's list at once (not only by the next reconciliation)
            from.getTank().inhabitants().remove(target.getUUID());
            from.markTankChanged();
        }
        BigCatching.give(player, slot, tub, name);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        BlockPos pos = hit.getBlockPos();
        if (level.getBlockState(pos).getBlock() instanceof TankBlock || !level.getFluidState(pos).is(FluidTags.WATER)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            return BigCatching.swing(server, player, hand, pos) ? InteractionResult.CONSUME.heldItemTransformedTo(stack) : InteractionResult.PASS;
        }
        return player.getCooldowns().isOnCooldown(stack) ? InteractionResult.PASS : InteractionResult.SUCCESS.heldItemTransformedTo(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        List<Component> tooltip = new SinkList<>(tooltipAdder);
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
        tooltip.add(Component.translatable("tooltip.bettacraft.large_net").withStyle(ChatFormatting.GRAY));
    }
}
