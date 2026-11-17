package org.overengineer.bettacraft.reef;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankMultiblock;

/**
 * M20: sea salt and calcium additive used on a tank. One sea salt turns a bucket (1000 mB) of the tank's water into sea
 * water (35 ppt; sneaking: as many as the tank needs to reach 35 ppt), one calcium additive raises the calcium.
 */
public final class ReefItems {
    private ReefItems() {}

    public static ItemInteractionResult useOnTank(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        boolean salt = stack.is(ModItems.SEA_SALT.get()), calcium = stack.is(ModItems.CALCIUM_ADDITIVE.get());
        if (!salt && !calcium) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        TankBlockEntity c = TankMultiblock.getController(level, pos);
        if (c == null) return ItemInteractionResult.CONSUME;
        TankData t = c.getTank();
        if (!TankInhabitants.canModify(player, t)) {
            player.displayClientMessage(Component.translatable("message.bettacraft.tank.locked"), true);
            return ItemInteractionResult.CONSUME;
        }
        if (t.waterMb() <= 0) {
            player.displayClientMessage(Component.translatable("message.bettacraft.reef.no_water").withStyle(ChatFormatting.GOLD), true);
            return ItemInteractionResult.CONSUME;
        }
        ServerLevel server = (ServerLevel) level;
        if (salt) {
            if (t.salinity() >= Salinity.SEA - 0.5F) {
                player.displayClientMessage(Component.translatable("message.bettacraft.reef.salty_enough", fmt(t.salinity())).withStyle(ChatFormatting.GOLD), true);
                return ItemInteractionResult.CONSUME;
            }
            int n = 1;
            if (player.isShiftKeyDown()) n = Math.max(1, itemsToSea(t.salinity(), t.waterMb()));
            if (!player.getAbilities().instabuild) n = Math.min(n, stack.getCount());
            float before = t.salinity();
            t.setSalinity(Math.min(Salinity.SEA, Salinity.addSalt(before, t.waterMb(), n)));
            t.setCalcium(t.calcium() + Salinity.calciumOf(t.salinity() - before));
            if (!player.getAbilities().instabuild) stack.shrink(n);
            level.playSound(null, pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.7F, 1.4F);
            player.displayClientMessage(Component.translatable("message.bettacraft.reef.salted", n, fmt(t.salinity())), true);
            if (Salinity.kind(before) != Salinity.kind(t.salinity())) {
                TankCare.log(server, c, "log.bettacraft.salinity_" + Salinity.kind(t.salinity()).name().toLowerCase(Locale.ROOT), fmt(t.salinity()));
            }
        } else {
            float dose = (float) (double) ServerConfig.getOr(ServerConfig.REEF_CALCIUM_DOSE, 200.0);
            t.setCalcium(Salinity.addCalcium(t.calcium(), t.waterMb(), 1, dose));
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.BLOCKS, 0.7F, 1.2F);
            player.displayClientMessage(Component.translatable("message.bettacraft.reef.calcium", Math.round(t.calcium())), true);
        }
        c.markTankChanged();
        return ItemInteractionResult.CONSUME;
    }

    /** Sea salt portions that bring {@code waterMb} of water from {@code ppt} to sea water. */
    public static int itemsToSea(float ppt, long waterMb) {
        if (ppt >= Salinity.SEA || waterMb <= 0) return 0;
        return (int) Math.ceil((Salinity.SEA - ppt) * waterMb / (Salinity.SEA * 1000.0) - 1e-6);
    }

    public static String fmt(float ppt) {
        return String.format(Locale.ROOT, "%.1f", ppt);
    }
}
