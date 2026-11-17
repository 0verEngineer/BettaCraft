package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public class LivingDropsEvent extends GameEvent {
    private final net.minecraft.world.entity.LivingEntity entity;

    public LivingDropsEvent(net.minecraft.world.entity.LivingEntity entity) {
        this.entity = entity;
    }

    public net.minecraft.world.entity.LivingEntity getEntity() {
        return entity;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }
}
