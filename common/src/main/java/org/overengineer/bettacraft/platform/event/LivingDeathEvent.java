package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public class LivingDeathEvent extends GameEvent {
    private final net.minecraft.world.entity.LivingEntity entity;
    private final net.minecraft.world.damagesource.DamageSource source;

    public LivingDeathEvent(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        this.entity = entity;
        this.source = source;
    }

    public net.minecraft.world.entity.LivingEntity getEntity() {
        return entity;
    }

    public net.minecraft.world.damagesource.DamageSource getSource() {
        return source;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }
}
