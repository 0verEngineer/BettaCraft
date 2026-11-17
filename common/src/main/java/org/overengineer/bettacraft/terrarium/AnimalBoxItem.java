package org.overengineer.bettacraft.terrarium;

import org.overengineer.bettacraft.util.SinkList;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.world.InteractionResult;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.overengineer.bettacraft.containers.ContainerRules;
import org.overengineer.bettacraft.registry.ModItems;

/**
 * M31 part b: a land animal in a terrarium box (Hermann's tortoise; the lizards and snakes of part c the same way): a
 * {@link MobBucketItem} without water. The empty {@link ModItems#TERRARIUM_BOX} catches it (right-click the animal); into a
 * tank the tank takes it as an inhabitant and adds no water; into the world it is let go on the block looked at (after
 * the same question as a bucket, {@link ContainerRules#release}), and the empty box comes back.
 */
public class AnimalBoxItem extends MobBucketItem {
    @SuppressWarnings("unchecked")
    public AnimalBoxItem(EntityType<?> type, Properties properties) {
        super((EntityType<? extends net.minecraft.world.entity.Mob>) type, Fluids.EMPTY, SoundEvents.ARMOR_EQUIP_LEATHER.value(), properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
        if (!level.mayInteract(player, hit.getBlockPos()) || !player.mayUseItemAt(pos, hit.getDirection(), stack)) return InteractionResult.FAIL;
        if (!ContainerRules.release(level, player, hand, stack)) return ContainerRules.waiting(stack);
        checkExtraContent(player, level, stack, pos);
        level.playSound(player, pos, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        ItemStack empty = player.hasInfiniteMaterials() ? stack : new ItemStack(ModItems.TERRARIUM_BOX.get());
        return InteractionResult.SUCCESS.heldItemTransformedTo(ItemUtils.createFilledResult(stack, player, empty));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        List<Component> lines = new SinkList<>(tooltipAdder);
        super.appendHoverText(stack, ctx, tooltipDisplay, tooltipAdder, flag);
        TurtleBucketItem.describe(stack, lines);
    }
}
