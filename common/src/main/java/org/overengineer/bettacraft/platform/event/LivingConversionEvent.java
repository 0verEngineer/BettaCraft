package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public final class LivingConversionEvent {
    private LivingConversionEvent() {}

    public static class Pre extends GameEvent {
        private final net.minecraft.world.entity.LivingEntity entity;
        private final net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.LivingEntity> outcome;

        public Pre(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.LivingEntity> outcome) {
            this.entity = entity;
            this.outcome = outcome;
        }

        public net.minecraft.world.entity.LivingEntity getEntity() {
            return entity;
        }

        public net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.LivingEntity> getOutcome() {
            return outcome;
        }

        @Override
        public boolean isCancelable() {
            return true;
        }
    }

    public static class Post extends GameEvent {
        private final net.minecraft.world.entity.LivingEntity entity;
        private final net.minecraft.world.entity.LivingEntity outcome;

        public Post(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.entity.LivingEntity outcome) {
            this.entity = entity;
            this.outcome = outcome;
        }

        public net.minecraft.world.entity.LivingEntity getEntity() {
            return entity;
        }

        public net.minecraft.world.entity.LivingEntity getOutcome() {
            return outcome;
        }
    }
}
