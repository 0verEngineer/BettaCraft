package org.overengineer.bettacraft.genetics;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import org.overengineer.bettacraft.platform.network.PacketDistributor;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.network.GeneticsSyncPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.overengineer.bettacraft.genetics.GeneticsReloadListener.*;

/** M52: the NeoForge event handlers of {@link GeneticsReloadListener} (moved here from the common class). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
final class GeneticsReloadListenerNeoForgeEvents {
    private GeneticsReloadListenerNeoForgeEvents() {}

    @SubscribeEvent
    static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new GeneticsReloadListener());
    }

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        GeneticsSyncPayload p = new GeneticsSyncPayload(GeneticsData.get().toNbt());
        event.getRelevantPlayers().forEach((ServerPlayer sp) -> {
            // fake and GameTest mock players have no channel
            if (sp.connection != null && sp.connection.hasChannel(GeneticsSyncPayload.TYPE)) PacketDistributor.sendToPlayer(sp, p);
        });
    }
}
