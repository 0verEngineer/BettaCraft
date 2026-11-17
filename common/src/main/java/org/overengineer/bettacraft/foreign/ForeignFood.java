package org.overengineer.bettacraft.foreign;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.food.BettaFood;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M35: the food of the foreign animals that is no aquarium food (the item tags and ids of their profile: greens for the
 * tortoise, raw meat and fish for the alligator). Aquarium food (pellets, frozen, live) goes the way of the fish
 * ({@code TankCare#feed}, M32). One item feeds one animal:
 * <ul>
 *     <li>by hand on the tank: the hungriest animal that eats it;</li>
 *     <li>from the feeder (and its schedule): every animal that ate {@link #HUNGRY_SHARE} of its {@code hunger_days} ago or
 *     longer gets one item it eats from the feeder's slots.</li>
 * </ul>
 */
public final class ForeignFood {
    /** From this share of its hunger days after its last meal an animal takes food from the feeder. */
    public static final double HUNGRY_SHARE = 0.5;

    private ForeignFood() {}

    /** Whether some profile eats this item as item food (the feeder takes it). */
    public static boolean isFood(ItemStack stack) {
        if (stack.isEmpty() || BettaFood.kind(stack) != null) return false;
        for (ForeignProfile p : ForeignProfiles.files().values()) {
            if (p.food().isEmpty()) continue;
            if (ForeignCare.eats(p, null, stack, null)) return true;
        }
        return false;
    }

    /** Item food by hand on a tank: the hungriest animal of another mod that eats it eats one. PASS if nobody does. */
    public static InteractionResult handFeed(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (stack.isEmpty() || BettaFood.kind(stack) != null || !(level instanceof ServerLevel server) || !Features.enabled(Feature.FOREIGN_CARE)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        if (c == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        List<LivingEntity> eaters = ForeignCare.eaters(server, c.getTank(), stack, null);
        if (eaters.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        LivingEntity pick = eaters.getFirst();
        ForeignCare.feed(List.of(pick), 1, null);
        ItemStack one = stack.copyWithCount(1);
        String id = stack.getItem().getDescriptionId();
        if (!player.getAbilities().instabuild) stack.shrink(1);
        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, one), pick.getX(), pick.getY() + pick.getBbHeight() * 0.5, pick.getZ(), 6, 0.1, 0.05, 0.1, 0.02);
        level.playSound(null, pick.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 0.5F, 1.2F);
        TankCare.log(server, c, "log.bettacraft.turtle.greens", TankCare.nameArg(pick), "@" + id);
        player.displayClientMessage(Component.translatable("message.bettacraft.terrarium.ate", pick.getDisplayName(), one.getHoverName()), true);
        return InteractionResult.CONSUME;
    }

    /** The feeder: one item each for every hungry animal of another mod that eats an item food of the slots. Returns the items used. */
    public static int feedFromFeeder(ServerLevel level, TankBlockEntity c, org.overengineer.bettacraft.platform.item.IItemHandler items) {
        if (!Features.enabled(Feature.FOREIGN_CARE) || ForeignProfiles.all().isEmpty()) return 0;
        int used = 0;
        for (var e : org.overengineer.bettacraft.tank.TankInhabitants.liveInhabitants(level, c.getTank())) {
            ForeignProfile p = ForeignProfiles.of(e.getType());
            if (p == null || !(e instanceof LivingEntity le)) continue;
            if (ForeignCare.health(le).daysSinceFed() < p.hungerDays() * HUNGRY_SHARE / speed()) continue;
            for (int i = 0; i < items.getSlots(); i++) {
                ItemStack s = items.getStackInSlot(i);
                if (s.isEmpty() || BettaFood.kind(s) != null || !ForeignCare.eats(p, le, s, null)) continue;
                ItemStack got = items.extractItem(i, 1, false);
                if (got.isEmpty()) continue;
                ForeignCare.feed(List.of(le), 1, null);
                TankCare.log(level, c, "log.bettacraft.turtle.greens", TankCare.nameArg(le), "@" + got.getItem().getDescriptionId());
                used++;
                break;
            }
        }
        return used;
    }

    private static double speed() {
        return org.overengineer.bettacraft.config.ServerConfig.getOr(org.overengineer.bettacraft.config.ServerConfig.FOREIGN_HEALTH_SPEED, 1.0);
    }
}
