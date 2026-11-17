package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public class LivingExperienceDropEvent extends GameEvent {
    private final net.minecraft.world.entity.LivingEntity entity;
    private int droppedExperience;

    public LivingExperienceDropEvent(net.minecraft.world.entity.LivingEntity entity, int droppedExperience) {
        this.entity = entity;
        this.droppedExperience = droppedExperience;
    }

    public net.minecraft.world.entity.LivingEntity getEntity() {
        return entity;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }

    public int getDroppedExperience() {
        return droppedExperience;
    }

    public void setDroppedExperience(int xp) {
        this.droppedExperience = xp;
    }
}
