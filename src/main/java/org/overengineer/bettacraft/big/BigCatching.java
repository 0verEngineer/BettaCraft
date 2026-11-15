package org.overengineer.bettacraft.big;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.Map;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.world.Wildlife;

/**
 * M30: catching big animals in the wild. Both ways need an empty transport tub in the inventory (the catch goes into it;
 * without one it gets away):
 * <ul>
 *     <li><b>Large net</b> ({@link LargeNetItem}) swung in open water: with {@code bigAnimalRules.largeNetChance} a big animal of
 *     that water ({@link Wildlife#bigTable}); used on a wild big animal it catches that one.</li>
 *     <li><b>Steel leader</b> ({@link SteelLeaderItem}) in the other hand while fishing: with {@code bigAnimalRules.steelLeaderChance}
 *     a big animal of the water bites instead of the normal catch (a thin line would break, the leader holds; it wears a
 *     little with every big catch).</li>
 * </ul>
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class BigCatching {
    private BigCatching() {}

    /** Slot of an empty transport tub in the inventory, or -1. */
    public static int emptyTub(Player p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof TransportTubItem && !TransportTubItem.isFilled(s)) return i;
        }
        return -1;
    }

    /** A tub with a freshly caught adult of the species (wild genes; not wild any more: it was caught). */
    @Nullable
    public static ItemStack caught(ServerLevel level, TankmateSpecies s, RandomGenerator r) {
        var type = ModEntities.TANKMATES.get(s);
        TankmateEntity e = type == null ? null : type.get().create(level);
        if (e == null) return null;
        e.setup(Wildlife.tankmate(s, r), r.nextBoolean(), LifeStage.ADULT, 0);
        ItemStack tub = TransportTubItem.fillWith(e);
        e.discard();
        return tub;
    }

    /** Rolls a big animal of the water at {@code pos} (null: none lives there). */
    @Nullable
    public static TankmateSpecies roll(ServerLevel level, BlockPos pos, RandomGenerator r) {
        Map<TankmateSpecies, Double> table = Wildlife.bigTable(NetCatching.water(level, pos));
        return NetCatching.rollFish(table, r);
    }

    /** Puts the tub with the catch where the empty tub was. */
    static void give(Player p, int slot, ItemStack tub, Component name) {
        org.overengineer.bettacraft.containers.AnimalContainers.give(p, slot, tub);
        p.displayClientMessage(Component.translatable("message.bettacraft.big.caught", name).withStyle(ChatFormatting.GREEN), true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.BUCKET_FILL_FISH, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    /** One swing of the large net in open water. True when the net was used. */
    public static boolean swing(ServerLevel level, Player player, InteractionHand hand, BlockPos water) {
        ItemStack tool = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(tool.getItem())) return false;
        player.getCooldowns().addCooldown(tool.getItem(), ServerConfig.getOr(ServerConfig.CRITTERS_NET_COOLDOWN, 40));
        level.playSound(null, water, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.8F, 0.9F);
        if (!Features.enabled(Feature.BIG_ANIMALS)) return true;
        int slot = emptyTub(player);
        if (slot < 0) {
            player.displayClientMessage(Component.translatable("message.bettacraft.big.need_tub_net"), true);
            return true;
        }
        if (!player.getAbilities().instabuild) tool.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
        RandomGenerator r = NetCatching.random(level);
        TankmateSpecies s = roll(level, water, r);
        if (s == null) {
            player.displayClientMessage(Component.translatable("message.bettacraft.big.nothing_here"), true);
            return true;
        }
        if (r.nextDouble() >= ServerConfig.getOr(ServerConfig.BIG_NET_CHANCE, 0.04)) {
            player.displayClientMessage(Component.translatable("message.bettacraft.net.empty"), true);
            return true;
        }
        ItemStack tub = caught(level, s, r);
        if (tub != null) give(player, slot, tub, Component.translatable(s.translationKey()));
        return true;
    }

    /** The steel leader in the hand opposite the rod (or either hand), or empty. */
    static ItemStack leader(Player p) {
        for (InteractionHand h : InteractionHand.values()) {
            ItemStack s = p.getItemInHand(h);
            if (s.getItem() instanceof SteelLeaderItem) return s;
        }
        return ItemStack.EMPTY;
    }

    /** Fishing with a steel leader: now and then a big animal of the water instead of the catch. */
    @SubscribeEvent
    public static void onFished(ItemFishedEvent event) {
        Player p = event.getEntity();
        if (!(p instanceof ServerPlayer sp) || !(p.level() instanceof ServerLevel level) || !Features.enabled(Feature.BIG_ANIMALS)) return;
        ItemStack lead = leader(p);
        if (lead.isEmpty()) return;
        int slot = emptyTub(p);
        if (slot < 0) return;
        RandomGenerator r = NetCatching.random(level);
        BlockPos at = event.getHookEntity().blockPosition();
        if (r.nextDouble() >= ServerConfig.getOr(ServerConfig.BIG_LEADER_CHANCE, 0.05)) return;
        TankmateSpecies s = roll(level, at, r);
        if (s == null) return;
        ItemStack tub = caught(level, s, r);
        if (tub == null) return;
        event.setCanceled(true);
        lead.hurtAndBreak(1, sp, p.getMainHandItem() == lead ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        give(p, slot, tub, Component.translatable(s.translationKey()));
    }
}
