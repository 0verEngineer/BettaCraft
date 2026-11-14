package org.overengineer.bettacraft.reef;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluids;

/**
 * M22.1: also pours into a water surface the player looks at (the sea from a boat or the shore, where vanilla buckets
 * only look for the bottom), into a cauldron and through the fluid capability into other mods' tanks and pipes (water).
 * <p>
 * M20: a bucket of sea water. In the world it pours plain water (Minecraft has one water); poured into a tank it brings
 * 35 ppt of salt and the calcium of sea water. Draining a salt water tank fills an empty bucket with it.
 */
public class SeaWaterBucketItem extends BucketItem {
    public SeaWaterBucketItem(Properties properties) {
        super(Fluids.WATER, properties);
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
                                                                    net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        var solid = getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.NONE);
        var fluid = getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.ANY);
        var eye = player.getEyePosition();
        if (fluid.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && level.getFluidState(fluid.getBlockPos()).is(net.minecraft.tags.FluidTags.WATER)
                && (solid.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK
                || solid.getLocation().distanceToSqr(eye) > fluid.getLocation().distanceToSqr(eye) + 0.01)) {
            // into the water the player looks at: no new block (a flowing water block becomes a source, as a bucket would)
            net.minecraft.core.BlockPos pos = fluid.getBlockPos();
            if (!level.mayInteract(player, pos)) return net.minecraft.world.InteractionResultHolder.fail(stack);
            if (!level.isClientSide && !level.getFluidState(pos).isSource() && level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 11);
            }
            playEmptySound(player, level, pos);
            level.gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.FLUID_PLACE, pos);
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(
                    net.minecraft.world.item.ItemUtils.createFilledResult(stack, player, getEmptySuccessItem(stack, player)), level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.bettacraft.sea_water_bucket").withStyle(ChatFormatting.GRAY));
    }
}
