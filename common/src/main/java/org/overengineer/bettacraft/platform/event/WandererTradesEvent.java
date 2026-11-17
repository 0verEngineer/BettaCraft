package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public class WandererTradesEvent extends GameEvent {
    private final java.util.List<net.minecraft.world.entity.npc.VillagerTrades.ItemListing> genericTrades;
    private final java.util.List<net.minecraft.world.entity.npc.VillagerTrades.ItemListing> rareTrades;

    public WandererTradesEvent(java.util.List<net.minecraft.world.entity.npc.VillagerTrades.ItemListing> genericTrades, java.util.List<net.minecraft.world.entity.npc.VillagerTrades.ItemListing> rareTrades) {
        this.genericTrades = genericTrades;
        this.rareTrades = rareTrades;
    }

    public java.util.List<net.minecraft.world.entity.npc.VillagerTrades.ItemListing> getGenericTrades() {
        return genericTrades;
    }

    public java.util.List<net.minecraft.world.entity.npc.VillagerTrades.ItemListing> getRareTrades() {
        return rareTrades;
    }
}
