package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public final class BlockEvent {
    private BlockEvent() {}

    public static class BreakEvent extends GameEvent {
        private final net.minecraft.world.level.LevelAccessor level;
        private final net.minecraft.core.BlockPos pos;
        private final net.minecraft.world.level.block.state.BlockState state;
        private final net.minecraft.world.entity.player.Player player;

        public BreakEvent(net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.entity.player.Player player) {
            this.level = level;
            this.pos = pos;
            this.state = state;
            this.player = player;
        }

        public net.minecraft.world.level.LevelAccessor getLevel() {
            return level;
        }

        public net.minecraft.core.BlockPos getPos() {
            return pos;
        }

        public net.minecraft.world.level.block.state.BlockState getState() {
            return state;
        }

        public net.minecraft.world.entity.player.Player getPlayer() {
            return player;
        }

        @Override
        public boolean isCancelable() {
            return true;
        }
    }

    public static class EntityPlaceEvent extends GameEvent {
        private final net.minecraft.world.level.LevelAccessor level;
        private final net.minecraft.core.BlockPos pos;
        private final net.minecraft.world.level.block.state.BlockState placedBlock;
        private final net.minecraft.world.entity.Entity entity;

        public EntityPlaceEvent(net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState placedBlock, @Nullable net.minecraft.world.entity.Entity entity) {
            this.level = level;
            this.pos = pos;
            this.placedBlock = placedBlock;
            this.entity = entity;
        }

        public net.minecraft.world.level.LevelAccessor getLevel() {
            return level;
        }

        public net.minecraft.core.BlockPos getPos() {
            return pos;
        }

        public net.minecraft.world.level.block.state.BlockState getPlacedBlock() {
            return placedBlock;
        }

        public @Nullable net.minecraft.world.entity.Entity getEntity() {
            return entity;
        }

        @Override
        public boolean isCancelable() {
            return true;
        }
    }
}
