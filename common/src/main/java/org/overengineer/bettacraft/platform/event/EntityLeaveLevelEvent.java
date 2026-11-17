package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public class EntityLeaveLevelEvent extends GameEvent {
    private final net.minecraft.world.entity.Entity entity;
    private final net.minecraft.world.level.Level level;

    public EntityLeaveLevelEvent(net.minecraft.world.entity.Entity entity, net.minecraft.world.level.Level level) {
        this.entity = entity;
        this.level = level;
    }

    public net.minecraft.world.entity.Entity getEntity() {
        return entity;
    }

    public net.minecraft.world.level.Level getLevel() {
        return level;
    }
}
