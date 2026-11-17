package org.overengineer.bettacraft.command;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.overengineer.bettacraft.BettaCraft;

/** M52: the NeoForge tick of {@link DevSmokeUpgrade} (the class itself is shared with the Fabric dev runs). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
final class DevSmokeUpgradeNeoForge {
    private DevSmokeUpgradeNeoForge() {}

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        DevSmokeUpgrade.onTick(event.getServer());
    }
}
