package org.overengineer.bettacraft.villager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.progression.Progress;
import static org.overengineer.bettacraft.villager.Aquarists.*;

/** M52: the NeoForge event handlers of {@link Aquarists} (moved here from the common class). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
final class AquaristsNeoForgeEvents {
    private AquaristsNeoForgeEvents() {}

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Villager v) || !(v.level() instanceof ServerLevel level) || (v.tickCount + v.getId()) % 100 != 0) return;
        if (isAquarist(v) && !Features.enabled(Feature.AQUARIST_VILLAGER)) dismiss(level, v);
    }

    @SubscribeEvent
    static void onTrade(TradeWithVillagerEvent event) {
        if (event.getAbstractVillager() instanceof Villager v && isAquarist(v)) Progress.award(event.getEntity(), Progress.AQUARIST_TRADE);
    }
}
