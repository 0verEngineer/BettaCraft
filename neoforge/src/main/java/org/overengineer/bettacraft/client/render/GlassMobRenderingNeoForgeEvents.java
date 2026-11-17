package org.overengineer.bettacraft.client.render;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.overengineer.bettacraft.BettaCraft;

/** M52: the NeoForge event handlers of {@link GlassMobRendering} (moved here from the common class). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID, value = Dist.CLIENT)
final class GlassMobRenderingNeoForgeEvents {
    private GlassMobRenderingNeoForgeEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST) // after every listener that could cancel it (then Post never comes)
    public static void pre(RenderLivingEvent.Pre<?, ?> e) {
        GlassMobRendering.pre(e.getEntity(), e.getPartialTick(), e.getPoseStack());
    }

    @SubscribeEvent
    public static void post(RenderLivingEvent.Post<?, ?> e) {
        GlassMobRendering.post(e.getEntity(), e.getPoseStack());
    }
}
