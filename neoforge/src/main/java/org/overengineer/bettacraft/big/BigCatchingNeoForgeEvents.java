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
import static org.overengineer.bettacraft.big.BigCatching.*;

/** M52: the NeoForge event handlers of {@link BigCatching} (moved here from the common class). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
final class BigCatchingNeoForgeEvents {
    private BigCatchingNeoForgeEvents() {}

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
