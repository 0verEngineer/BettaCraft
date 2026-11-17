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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.ServerTickEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.progression.Progress;
import org.overengineer.bettacraft.registry.ModEntities;
import static org.overengineer.bettacraft.aquarist.AquaristArrival.*;

/** M52: the game event handlers of {@link AquaristArrival} (M52: on the common game events, fired by each loader). */
public final class AquaristArrivalHandlers {
    private AquaristArrivalHandlers() {}

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
            if (org.overengineer.bettacraft.platform.Platform.get().isFakePlayer(p instanceof net.minecraft.world.entity.player.Player fp ? fp : null)) continue;
            rec.player(p.getUUID()).name = p.getGameProfile().name();
            if (whyNot(p, false) == null) arrive(p);
        }
    }
}
