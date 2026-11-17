package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public final class LevelTickEvent {
    private LevelTickEvent() {}

    public static class Post extends GameEvent {
        private final net.minecraft.world.level.Level level;

        public Post(net.minecraft.world.level.Level level) {
            this.level = level;
        }

        public net.minecraft.world.level.Level getLevel() {
            return level;
        }
    }
}
