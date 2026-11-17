package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public class OnDatapackSyncEvent extends GameEvent {
    private final java.util.stream.Stream<net.minecraft.server.level.ServerPlayer> relevantPlayers;

    public OnDatapackSyncEvent(java.util.stream.Stream<net.minecraft.server.level.ServerPlayer> relevantPlayers) {
        this.relevantPlayers = relevantPlayers;
    }

    public java.util.stream.Stream<net.minecraft.server.level.ServerPlayer> getRelevantPlayers() {
        return relevantPlayers;
    }
}
