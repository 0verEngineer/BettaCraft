package org.overengineer.bettacraft.gametest.framework;

import net.minecraft.world.entity.npc.villager.VillagerTrades;

/**
 * M52b: the wandering trader's listings by the old levels (1.21.1: 1 = generic, 2 = rare). Since 1.21.5 his trades are a list
 * of pools: 0 = what he buys, 1 = the rare ("special") pool, 2 = the generic pool (vanilla and NeoForge's event alike).
 */
public final class DevTrades {
    private DevTrades() {}

    public static VillagerTrades.ItemListing[] wanderer(int oldLevel) {
        return VillagerTrades.WANDERING_TRADER_TRADES.get(oldLevel == 1 ? 2 : 1).getLeft();
    }
}
