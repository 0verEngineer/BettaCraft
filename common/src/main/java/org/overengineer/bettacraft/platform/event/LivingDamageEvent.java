package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public final class LivingDamageEvent {
    private LivingDamageEvent() {}

    public static class Pre extends GameEvent {
        private final net.minecraft.world.entity.LivingEntity entity;
        private final net.minecraft.world.damagesource.DamageSource source;
        private float newDamage;

        public Pre(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float newDamage) {
            this.entity = entity;
            this.source = source;
            this.newDamage = newDamage;
        }

        public net.minecraft.world.entity.LivingEntity getEntity() {
            return entity;
        }

        public net.minecraft.world.damagesource.DamageSource getSource() {
            return source;
        }

        public float getNewDamage() {
            return newDamage;
        }

        public void setNewDamage(float damage) {
            this.newDamage = damage;
        }
    }
}
