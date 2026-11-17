package org.overengineer.bettacraft.aquarist;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.progression.Progress;
import org.overengineer.bettacraft.registry.ModEntities;
import static org.overengineer.bettacraft.aquarist.AquaristArrival.*;

/** M52: the NeoForge event handlers of {@link AquaristArrival} (moved here from the common class). */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
final class AquaristArrivalNeoForgeEvents {
    private AquaristArrivalNeoForgeEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % CHECK_EVERY != 0 || !Features.enabled(Feature.AQUARIST_NPC)) return;
        AquaristRecords rec = AquaristRecords.get(server);
        long now = server.overworld().getGameTime();
        // offers that ran out while their aquarist was not loaded
        for (var en : rec.players().entrySet()) {
            AquaristRecords.PlayerRecord r = en.getValue();
            if (r.npc != null && !r.hired && now > r.offerUntil) {
                gone(server, r, false);
                r.refused++;
            }
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p instanceof net.neoforged.neoforge.common.util.FakePlayer) continue;
            rec.player(p.getUUID()).name = p.getGameProfile().getName();
            if (whyNot(p, false) == null) arrive(p);
        }
    }
}
