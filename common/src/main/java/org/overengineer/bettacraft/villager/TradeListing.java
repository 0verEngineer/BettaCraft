package org.overengineer.bettacraft.villager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.Nullable;

/**
 * M52c (26.1): one offer of ours made in code (1.21.x {@code VillagerTrades.ItemListing}, gone with the data-driven trades
 * of 26.1). Each listing has a {@code villager_trade} entry in our data whose item modifier is {@link TradeFunction}; the
 * entry stands in the trader's trade set and {@code VillagerTradeOfferMixin} lets the listing make the offer. Null: no
 * offer (e.g. its feature is off).
 */
@FunctionalInterface
public interface TradeListing {
    @Nullable MerchantOffer getOffer(ServerLevel level, Entity trader, RandomSource random);
}
