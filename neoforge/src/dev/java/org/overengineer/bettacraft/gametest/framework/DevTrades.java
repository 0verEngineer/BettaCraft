package org.overengineer.bettacraft.gametest.framework;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.trading.VillagerTrade;
import org.apache.commons.lang3.tuple.Pair;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.villager.BettaTrades;
import org.overengineer.bettacraft.villager.TradeEntry;
import org.overengineer.bettacraft.villager.TradeListing;

/**
 * M52b: the wandering trader's listings by the old levels (1.21.1: 1 = generic, 2 = rare). M52c (26.1): trades are data
 * (villager trades in trade tags and trade sets): our listings of a pool are the entries of its tag that stand for one of
 * our offers ({@link TradeEntry}), in the order of the tag.
 */
public final class DevTrades {
    private DevTrades() {}

    /** Our listings of the trader's usual (1) or rare (2) pool. */
    public static TradeListing[] wanderer(ServerLevel level, int oldLevel) {
        return listings(level.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE)
                .get(TagKey.create(Registries.VILLAGER_TRADE, Identifier.withDefaultNamespace(oldLevel == 1 ? "wandering_trader/common" : "wandering_trader/uncommon")))
                .map(s -> (Iterable<Holder<VillagerTrade>>) s).orElse(List.of()));
    }

    /** Both pools of the trader (usual, rare), like 1.21.x's {@code VillagerTrades.WANDERING_TRADER_TRADES}. */
    public static List<Pair<TradeListing[], Integer>> wandererPools(ServerLevel level) {
        return List.of(Pair.of(wanderer(level, 1), 5), Pair.of(wanderer(level, 2), 2));
    }

    /** The aquarist's listings per level (its trade sets), like 1.21.x's {@code VillagerTrades.TRADES} of the profession. */
    public static Int2ObjectMap<TradeListing[]> aquarist(ServerLevel level) {
        Int2ObjectMap<TradeListing[]> out = new Int2ObjectOpenHashMap<>();
        var sets = level.registryAccess().lookupOrThrow(Registries.TRADE_SET);
        for (int lvl = 1; lvl <= 5; lvl++) {
            var set = sets.getValue(ResourceKey.create(Registries.TRADE_SET, BettaCraft.id(BettaTrades.aquaristTradeSet(lvl))));
            if (set != null) out.put(lvl, listings(set.trades()));
        }
        return out;
    }

    private static TradeListing[] listings(Iterable<Holder<VillagerTrade>> trades) {
        List<TradeListing> out = new ArrayList<>();
        for (Holder<VillagerTrade> h : trades) {
            String id = ((TradeEntry) h.value()).bettacraft$offerId();
            TradeListing l = id == null ? null : BettaTrades.listing(id);
            if (l != null) out.add(l);
        }
        return out.toArray(TradeListing[]::new);
    }
}
