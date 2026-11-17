package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public class TradeWithVillagerEvent extends GameEvent {
    private final net.minecraft.world.entity.player.Player entity;
    private final net.minecraft.world.entity.npc.AbstractVillager abstractVillager;

    public TradeWithVillagerEvent(net.minecraft.world.entity.player.Player entity, net.minecraft.world.entity.npc.AbstractVillager abstractVillager) {
        this.entity = entity;
        this.abstractVillager = abstractVillager;
    }

    public net.minecraft.world.entity.player.Player getEntity() {
        return entity;
    }

    public net.minecraft.world.entity.npc.AbstractVillager getAbstractVillager() {
        return abstractVillager;
    }
}
