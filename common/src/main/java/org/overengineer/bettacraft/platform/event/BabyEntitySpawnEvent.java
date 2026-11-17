package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public class BabyEntitySpawnEvent extends GameEvent {
    private final net.minecraft.world.entity.Mob parentA;
    private final net.minecraft.world.entity.Mob parentB;

    public BabyEntitySpawnEvent(net.minecraft.world.entity.Mob parentA, net.minecraft.world.entity.Mob parentB) {
        this.parentA = parentA;
        this.parentB = parentB;
    }

    public net.minecraft.world.entity.Mob getParentA() {
        return parentA;
    }

    public net.minecraft.world.entity.Mob getParentB() {
        return parentB;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }
}
