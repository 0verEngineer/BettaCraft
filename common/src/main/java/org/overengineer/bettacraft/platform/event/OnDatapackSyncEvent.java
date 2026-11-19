package org.overengineer.bettacraft.platform.event;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.server.level.ServerPlayer;

/**
 * M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). M52b: the players are
 * kept as a list, every handler gets a fresh stream (NeoForge 21.11 hands over a stream that can be used only once).
 */
public class OnDatapackSyncEvent extends GameEvent {
    private final List<ServerPlayer> relevantPlayers;

    public OnDatapackSyncEvent(Stream<ServerPlayer> relevantPlayers) {
        this.relevantPlayers = relevantPlayers.toList();
    }

    public Stream<ServerPlayer> getRelevantPlayers() {
        return relevantPlayers.stream();
    }
}
