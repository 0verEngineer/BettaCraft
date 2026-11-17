package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public class LivingChangeTargetEvent extends GameEvent {
    private final net.minecraft.world.entity.LivingEntity entity;
    private final net.minecraft.world.entity.LivingEntity newAboutToBeSetTarget;

    public LivingChangeTargetEvent(net.minecraft.world.entity.LivingEntity entity, @Nullable net.minecraft.world.entity.LivingEntity newAboutToBeSetTarget) {
        this.entity = entity;
        this.newAboutToBeSetTarget = newAboutToBeSetTarget;
    }

    public net.minecraft.world.entity.LivingEntity getEntity() {
        return entity;
    }

    public @Nullable net.minecraft.world.entity.LivingEntity getNewAboutToBeSetTarget() {
        return newAboutToBeSetTarget;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }
}
