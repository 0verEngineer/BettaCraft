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
import net.neoforged.neoforge.network.PacketDistributor;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.network.GeneticsSyncPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads {@code data/<ns>/bettacraft/{colors,genes,varieties}/*.json} into {@link GeneticsData} on (re)load and sends the
 * result to the clients (join and {@code /reload}).
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class GeneticsReloadListener extends SimplePreparableReloadListener<GeneticsData> {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/genetics");
    private static final FileToIdConverter COLORS = FileToIdConverter.json("bettacraft/colors");
    private static final FileToIdConverter GENES = FileToIdConverter.json("bettacraft/genes");
    private static final FileToIdConverter VARIETIES = FileToIdConverter.json("bettacraft/varieties");

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

    @Override
    protected GeneticsData prepare(ResourceManager rm, ProfilerFiller profiler) {
        return GeneticsData.fromJson(read(rm, COLORS), read(rm, GENES), read(rm, VARIETIES), w -> LOGGER.warn("Ignoring {}", w));
    }

    @Override
    protected void apply(GeneticsData data, ResourceManager rm, ProfilerFiller profiler) {
        GeneticsData.set(data);
        LOGGER.info("Loaded {} colours, {} varieties", data.colors().size(), data.varieties().size());
    }

    private static Map<String, JsonElement> read(ResourceManager rm, FileToIdConverter conv) {
        Map<String, JsonElement> out = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Resource> e : conv.listMatchingResources(rm).entrySet()) {
            ResourceLocation id = conv.fileToId(e.getKey());
            String key = BettaCraft.MOD_ID.equals(id.getNamespace()) ? id.getPath() : id.toString();
            try (Reader r = e.getValue().openAsReader()) {
                out.put(key, JsonParser.parseReader(r));
            } catch (Exception ex) {
                LOGGER.warn("Could not read {}: {}", e.getKey(), ex.toString());
            }
        }
        return out;
    }
}
