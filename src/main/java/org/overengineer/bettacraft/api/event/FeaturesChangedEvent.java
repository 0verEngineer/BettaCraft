package org.overengineer.bettacraft.api.event;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

/**
 * M45: the feature switches or the server config of Bettacraft were reloaded while the server runs (the files changed). On
 * {@code NeoForge.EVENT_BUS}, server side, main thread; like the other events it is turned off by
 * {@code features.compat.modpackEvents}. The example script {@code bettacraft_quests.js} shows and hides the quests of
 * the switches again ({@code BettacraftApi.questTagsOn}).
 */
public class FeaturesChangedEvent extends Event {
    private final MinecraftServer server;

    public FeaturesChangedEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
