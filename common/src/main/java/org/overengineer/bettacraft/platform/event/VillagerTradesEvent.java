package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public class VillagerTradesEvent extends GameEvent {
    private final net.minecraft.world.entity.npc.villager.VillagerProfession type;
    private final it.unimi.dsi.fastutil.ints.Int2ObjectMap<java.util.List<net.minecraft.world.entity.npc.villager.VillagerTrades.ItemListing>> trades;

    public VillagerTradesEvent(net.minecraft.world.entity.npc.villager.VillagerProfession type, it.unimi.dsi.fastutil.ints.Int2ObjectMap<java.util.List<net.minecraft.world.entity.npc.villager.VillagerTrades.ItemListing>> trades) {
        this.type = type;
        this.trades = trades;
    }

    public net.minecraft.world.entity.npc.villager.VillagerProfession getType() {
        return type;
    }

    public it.unimi.dsi.fastutil.ints.Int2ObjectMap<java.util.List<net.minecraft.world.entity.npc.villager.VillagerTrades.ItemListing>> getTrades() {
        return trades;
    }
}
