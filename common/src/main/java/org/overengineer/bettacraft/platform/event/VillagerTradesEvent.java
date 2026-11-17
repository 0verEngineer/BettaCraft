package org.overengineer.bettacraft.platform.event;

import org.jetbrains.annotations.Nullable;

/** M52: the game event of NeoForge's name and getters, fired by each loader (see {@link GameEvents}). */
public class VillagerTradesEvent extends GameEvent {
    private final net.minecraft.world.entity.npc.VillagerProfession type;
    private final it.unimi.dsi.fastutil.ints.Int2ObjectMap<java.util.List<net.minecraft.world.entity.npc.VillagerTrades.ItemListing>> trades;

    public VillagerTradesEvent(net.minecraft.world.entity.npc.VillagerProfession type, it.unimi.dsi.fastutil.ints.Int2ObjectMap<java.util.List<net.minecraft.world.entity.npc.VillagerTrades.ItemListing>> trades) {
        this.type = type;
        this.trades = trades;
    }

    public net.minecraft.world.entity.npc.VillagerProfession getType() {
        return type;
    }

    public it.unimi.dsi.fastutil.ints.Int2ObjectMap<java.util.List<net.minecraft.world.entity.npc.VillagerTrades.ItemListing>> getTrades() {
        return trades;
    }
}
