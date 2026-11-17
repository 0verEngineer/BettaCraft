package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public final class PistonEvent {
    private PistonEvent() {}

    public static class Pre extends GameEvent {
        private final net.minecraft.world.level.LevelAccessor level;
        private final net.minecraft.core.BlockPos pos;
        private final net.minecraft.core.Direction direction;
        private final boolean extending;

        public Pre(net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction direction, boolean extending) {
            this.level = level;
            this.pos = pos;
            this.direction = direction;
            this.extending = extending;
        }

        public net.minecraft.world.level.LevelAccessor getLevel() {
            return level;
        }

        public net.minecraft.core.BlockPos getPos() {
            return pos;
        }

        public net.minecraft.core.Direction getDirection() {
            return direction;
        }

        public boolean isExtend() {
            return extending;
        }

        @Override
        public boolean isCancelable() {
            return true;
        }

        /** The blocks the piston would move (vanilla structure resolver), or null when it cannot move. */
        public net.minecraft.world.level.block.piston.PistonStructureResolver getStructureHelper() {
            return new net.minecraft.world.level.block.piston.PistonStructureResolver((net.minecraft.world.level.Level) level, pos, direction, extending);
        }
    }
}
