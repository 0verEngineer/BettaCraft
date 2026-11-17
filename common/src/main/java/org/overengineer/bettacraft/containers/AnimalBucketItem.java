package org.overengineer.bettacraft.containers;

import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * M22.1: a bucket of water with an animal (betta, tankmates, seahorses, frog). Into a tank the tank takes it as an
 * inhabitant (before this item is asked). Into the world like a vanilla fish bucket, also into a water surface out of
 * reach of the bottom (the sea from a boat or the shore) - but only after the player confirmed it
 * ({@link ContainerRules#release}).
 */
public class AnimalBucketItem extends MobBucketItem {
    @SuppressWarnings("unchecked")
    public AnimalBucketItem(EntityType<?> type, Fluid fluid, SoundEvent emptySound, Properties properties) {
        super((EntityType<? extends net.minecraft.world.entity.Mob>) type, fluid, emptySound, properties);
    }

    /** M22.3: a bucket with several fish of one species lets all of them go. */
    @Override
    public void checkExtraContent(@javax.annotation.Nullable net.minecraft.world.entity.LivingEntity player, Level level, ItemStack stack, BlockPos pos) {
        if (level instanceof net.minecraft.server.level.ServerLevel server && stack.has(org.overengineer.bettacraft.registry.ModDataComponents.TANKMATE_GROUP.get())) {
            org.overengineer.bettacraft.tankmates.TankmateItems.release(server, stack, pos, player instanceof Player p ? p : null);
            level.gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.ENTITY_PLACE, pos);
            return;
        }
        super.checkExtraContent(player, level, stack, pos);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult solid = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        BlockHitResult fluid = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        boolean water = fluid.getType() == HitResult.Type.BLOCK && level.getFluidState(fluid.getBlockPos()).is(FluidTags.WATER);
        // nothing in reach: nothing would happen (no question either)
        if (!water && solid.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        if (!ContainerRules.release(level, player, hand, stack)) return ContainerRules.waiting(stack);
        if (water && (solid.getType() != HitResult.Type.BLOCK
                || solid.getLocation().distanceToSqr(player.getEyePosition()) > fluid.getLocation().distanceToSqr(player.getEyePosition()) + 0.01)) {
            // the animal goes into the water the player looks at (no new water block)
            BlockPos pos = fluid.getBlockPos();
            if (!level.mayInteract(player, pos)) return InteractionResult.FAIL;
            checkExtraContent(player, level, stack, pos);
            playEmptySound(player, level, pos);
            return InteractionResult.SUCCESS.heldItemTransformedTo(ItemUtils.createFilledResult(stack, player, getEmptySuccessItem(stack, player)));
        }
        return super.use(level, player, hand);
    }
}
