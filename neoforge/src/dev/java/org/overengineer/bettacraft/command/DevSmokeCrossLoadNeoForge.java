package org.overengineer.bettacraft.command;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.overengineer.bettacraft.BettaCraft;

/** The NeoForge tick of {@link DevSmokeCrossLoad} (the class itself is shared with the Fabric dev runs). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
final class DevSmokeCrossLoadNeoForge {
    private DevSmokeCrossLoadNeoForge() {}

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        DevSmokeCrossLoad.onTick(event.getServer());
    }
}
