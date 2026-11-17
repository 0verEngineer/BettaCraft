package org.overengineer.bettacraft.containers;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.overengineer.bettacraft.critters.CritterTooltip;
import org.overengineer.bettacraft.registry.ModDataComponents;

/**
 * M22.3: shrimp, crayfish, snails or hermit crabs of one species in a plain bucket of water (no fish bag needed): taken
 * out of a tank with an empty bucket (1000 mB of the tank's water) or a bucket of water that suits them, or caught in the
 * world. Into a tank they go in with the water; into the world (after the confirmation of {@link ContainerRules}) the
 * water is poured out and the animals crawl off as wild animals ({@code features.invertebrates.wildSpawns}).
 */
public class CritterBucketItem extends BucketItem {
    public CritterBucketItem(Properties properties) {
        super(Fluids.WATER, properties);
    }

    @Nullable
    public static List<CompoundTag> animals(ItemStack stack) {
        return AnimalContainers.critters(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (animals(stack) == null) return InteractionResultHolder.pass(stack);
        BlockHitResult solid = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        BlockHitResult fluid = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        boolean water = fluid.getType() == HitResult.Type.BLOCK && level.getFluidState(fluid.getBlockPos()).is(FluidTags.WATER);
        if (!water && solid.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        if (!ContainerRules.release(level, player, hand, stack)) return ContainerRules.waiting(stack);
        if (water && (solid.getType() != HitResult.Type.BLOCK
                || solid.getLocation().distanceToSqr(player.getEyePosition()) > fluid.getLocation().distanceToSqr(player.getEyePosition()) + 0.01)) {
            BlockPos pos = fluid.getBlockPos();
            if (!level.mayInteract(player, pos)) return InteractionResultHolder.fail(stack);
            checkExtraContent(player, level, stack, pos);
            playEmptySound(player, level, pos);
            return InteractionResultHolder.sidedSuccess(ItemUtils.createFilledResult(stack, player, getEmptySuccessItem(stack, player)), level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    /** The animals crawl off where the water was poured. */
    @Override
    public void checkExtraContent(@Nullable Player player, Level level, ItemStack stack, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return;
        List<CompoundTag> a = animals(stack);
        if (a == null) return;
        org.overengineer.bettacraft.entity.critter.WildCritterEntity.release(server, a, pos, player);
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_FISH, SoundSource.NEUTRAL, 0.8F, 1.3F);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
    }

    @Override
    public Component getName(ItemStack stack) {
        List<CompoundTag> a = animals(stack);
        if (a == null) return super.getName(stack);
        return Component.translatable("item.bettacraft.critter_bucket.filled", a.size(), CritterTooltip.bagName(a));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<CompoundTag> a = animals(stack);
        if (a != null) CritterTooltip.append(a, tooltip);
    }
}
