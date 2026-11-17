package org.overengineer.bettacraft.villager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.MerchantOffers;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.TradeWithVillagerEvent;
import org.overengineer.bettacraft.platform.event.EntityTickEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.progression.Progress;
import static org.overengineer.bettacraft.villager.Aquarists.*;

/** M52: the game event handlers of {@link Aquarists} (M52: on the common game events, fired by each loader). */
public final class AquaristsHandlers {
    private AquaristsHandlers() {}

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
