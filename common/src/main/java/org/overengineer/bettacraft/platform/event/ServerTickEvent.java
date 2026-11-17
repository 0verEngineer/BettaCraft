package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public final class ServerTickEvent {
    private ServerTickEvent() {}

    public static class Post extends GameEvent {
        private final net.minecraft.server.MinecraftServer server;

        public Post(net.minecraft.server.MinecraftServer server) {
            this.server = server;
        }

        public net.minecraft.server.MinecraftServer getServer() {
            return server;
        }
    }
}
