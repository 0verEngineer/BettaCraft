package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public final class PlayerTickEvent {
    private PlayerTickEvent() {}

    public static class Post extends GameEvent {
        private final net.minecraft.world.entity.player.Player entity;

        public Post(net.minecraft.world.entity.player.Player entity) {
            this.entity = entity;
        }

        public net.minecraft.world.entity.player.Player getEntity() {
            return entity;
        }
    }
}
