package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public class ItemFishedEvent extends GameEvent {
    private final net.minecraft.world.entity.player.Player entity;
    private final net.minecraft.world.entity.projectile.FishingHook hookEntity;

    public ItemFishedEvent(net.minecraft.world.entity.player.Player entity, net.minecraft.world.entity.projectile.FishingHook hookEntity) {
        this.entity = entity;
        this.hookEntity = hookEntity;
    }

    public net.minecraft.world.entity.player.Player getEntity() {
        return entity;
    }

    public net.minecraft.world.entity.projectile.FishingHook getHookEntity() {
        return hookEntity;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }
}
