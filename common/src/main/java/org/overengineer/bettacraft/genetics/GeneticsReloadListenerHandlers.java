package org.overengineer.bettacraft.genetics;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.AddReloadListenerEvent;
import org.overengineer.bettacraft.platform.event.OnDatapackSyncEvent;
import org.overengineer.bettacraft.platform.network.PacketDistributor;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.network.GeneticsSyncPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.overengineer.bettacraft.genetics.GeneticsReloadListener.*;

/** M52: the game event handlers of {@link GeneticsReloadListener} (M52: on the common game events, fired by each loader). */
public final class GeneticsReloadListenerHandlers {
    private GeneticsReloadListenerHandlers() {}

    @SubscribeEvent
    static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new GeneticsReloadListener());
    }

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        GeneticsSyncPayload p = new GeneticsSyncPayload(GeneticsData.get().toNbt());
        event.getRelevantPlayers().forEach((ServerPlayer sp) -> {
            // fake and GameTest mock players have no channel
            if (org.overengineer.bettacraft.platform.Platform.get().hasChannel(sp, GeneticsSyncPayload.TYPE)) PacketDistributor.sendToPlayer(sp, p);
        });
    }
}
