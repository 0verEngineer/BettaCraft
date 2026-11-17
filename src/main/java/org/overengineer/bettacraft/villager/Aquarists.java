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

/**
 * M47 part a: the aquarists in the world. While {@code features.sources.aquaristVillager} is off an aquarist loses his
 * profession and his offers (checked every five seconds; he can take another job). A trade with an aquarist counts for
 * the advancement "An aquarist in the village".
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class Aquarists {
    private Aquarists() {}

    public static boolean isAquarist(Villager v) {
        return v.getVillagerData().getProfession() == ModVillagers.AQUARIST.get();
    }

    @SubscribeEvent
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Villager v) || !(v.level() instanceof ServerLevel level) || (v.tickCount + v.getId()) % 100 != 0) return;
        if (isAquarist(v) && !Features.enabled(Feature.AQUARIST_VILLAGER)) dismiss(level, v);
    }

    /** Takes the profession and the offers of an aquarist away (the switch is off). */
    public static void dismiss(ServerLevel level, Villager v) {
        v.setVillagerData(v.getVillagerData().setProfession(VillagerProfession.NONE).setLevel(1));
        v.setVillagerXp(0);
        v.setOffers(new MerchantOffers());
        v.releasePoi(MemoryModuleType.JOB_SITE);
        v.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
        v.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
        v.refreshBrain(level);
    }

    @SubscribeEvent
    static void onTrade(TradeWithVillagerEvent event) {
        if (event.getAbstractVillager() instanceof Villager v && isAquarist(v)) Progress.award(event.getEntity(), Progress.AQUARIST_TRADE);
    }
}
