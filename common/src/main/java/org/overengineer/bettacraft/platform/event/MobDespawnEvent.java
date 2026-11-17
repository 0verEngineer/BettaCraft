package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public class MobDespawnEvent extends GameEvent {
    private final net.minecraft.world.entity.Mob entity;
    private DespawnResult result = DespawnResult.DEFAULT;

    public MobDespawnEvent(net.minecraft.world.entity.Mob entity) {
        this.entity = entity;
    }

    public net.minecraft.world.entity.Mob getEntity() {
        return entity;
    }

    /** NeoForge: MobDespawnEvent.Result (ALLOW: despawn now, DENY: never, DEFAULT: the mob decides). */
    public enum DespawnResult { ALLOW, DENY, DEFAULT }

    public DespawnResult getResult() {
        return result;
    }

    public void setResult(DespawnResult r) {
        this.result = r;
    }
}
