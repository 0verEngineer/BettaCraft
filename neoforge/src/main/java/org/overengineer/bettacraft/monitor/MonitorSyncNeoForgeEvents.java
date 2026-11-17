package org.overengineer.bettacraft.monitor;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.overengineer.bettacraft.platform.network.PacketDistributor;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.crucible.CrucibleBlockEntity;
import org.overengineer.bettacraft.network.MonitorDataPayload;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import static org.overengineer.bettacraft.monitor.MonitorSync.*;

/** M52: the NeoForge event handlers of {@link MonitorSync} (moved here from the common class). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
final class MonitorSyncNeoForgeEvents {
    private MonitorSyncNeoForgeEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        for (Map.Entry<ServerPlayer, List<MonitorDataPayload.Entry>> e : PENDING.entrySet()) {
            if (e.getKey().hasDisconnected()) continue;
            List<MonitorDataPayload.Entry> list = e.getValue();
            for (int i = 0; i < list.size(); i += MonitorDataPayload.MAX_ENTRIES) {
                PacketDistributor.sendToPlayer(e.getKey(), new MonitorDataPayload(List.copyOf(list.subList(i, Math.min(list.size(), i + MonitorDataPayload.MAX_ENTRIES)))));
                sentPayloads++;
            }
        }
        PENDING.clear();
    }
}
