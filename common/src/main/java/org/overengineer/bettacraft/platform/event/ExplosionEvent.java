package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public final class ExplosionEvent {
    private ExplosionEvent() {}

    public static class Detonate extends GameEvent {
        private final net.minecraft.world.level.Level level;
        private final java.util.List<net.minecraft.core.BlockPos> affectedBlocks;

        public Detonate(net.minecraft.world.level.Level level, java.util.List<net.minecraft.core.BlockPos> affectedBlocks) {
            this.level = level;
            this.affectedBlocks = affectedBlocks;
        }

        public net.minecraft.world.level.Level getLevel() {
            return level;
        }

        public java.util.List<net.minecraft.core.BlockPos> getAffectedBlocks() {
            return affectedBlocks;
        }
    }
}
