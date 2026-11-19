package org.overengineer.bettacraft.villager;

import org.jetbrains.annotations.Nullable;

/** M52c (26.1): a {@code villager_trade} entry (mixin {@code VillagerTradeOfferMixin}): the id of our offer it stands for, or null. */
public interface TradeEntry {
    @Nullable String bettacraft$offerId();
}
