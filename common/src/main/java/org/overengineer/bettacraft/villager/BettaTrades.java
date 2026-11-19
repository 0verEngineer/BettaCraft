package org.overengineer.bettacraft.villager;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.sources.Target;
import org.overengineer.bettacraft.sources.TraderOffers;

/**
 * M52c (26.1): our offers in the data-driven trades of 26.1. Every offer of {@link TraderOffers} (wandering trader) and
 * {@link AquaristTrades} (aquarist villager) is a {@code villager_trade} entry {@code bettacraft:<id>} (made by the data
 * generator from {@link #entries}) in the trade tag of its pool: the wandering trader's {@code minecraft:wandering_trader/common}
 * (usual offers) and {@code /uncommon} (rare ones), the aquarist's {@code bettacraft:aquarist/level_<n>} (trade sets
 * {@code bettacraft:aquarist/level_<n>}, two offers of each level). The trader picks among vanilla's and our entries as
 * before; the offer itself is made by our listing ({@link TradeFunction}).
 */
public final class BettaTrades {
    private BettaTrades() {}

    public static final String WANDERER = "wandering_trader/";
    public static final String AQUARIST = "aquarist/";
    /** Offers per level of the aquarist (both of a level, M47). */
    public static final int AQUARIST_OFFERS_PER_LEVEL = 2;

    /** One entry of the data: its id (below {@code bettacraft:}), the trade tag it belongs to and the item it stands for. */
    public record Entry(String id, String tag, String gives) {}

    /** The listing of an entry ({@code wandering_trader/<offer>}, {@code aquarist/<offer>}), or null. */
    public static @Nullable TradeListing listing(String id) {
        if (id.startsWith(WANDERER)) {
            String o = id.substring(WANDERER.length());
            for (TraderOffers.Offer x : TraderOffers.all()) if (x.id().equals(o)) return x.gatedListing();
        } else if (id.startsWith(AQUARIST)) {
            String o = id.substring(AQUARIST.length());
            for (AquaristTrades.Offer x : AquaristTrades.all()) if (x.id().equals(o)) return x.listing();
        }
        return null;
    }

    /** Every entry of our trades (data generator, tests). */
    public static List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        for (TraderOffers.Offer o : TraderOffers.all()) {
            String first = o.products().get().isEmpty() ? null : o.products().get().getFirst().target();
            out.add(new Entry(WANDERER + o.id(), o.slot() == TraderOffers.Slot.USUAL ? "minecraft:wandering_trader/common" : "minecraft:wandering_trader/uncommon",
                    gives(first)));
        }
        for (AquaristTrades.Offer o : AquaristTrades.all()) {
            String first = o.buys() || o.products().get().isEmpty() ? null : o.products().get().getFirst().target();
            out.add(new Entry(AQUARIST + o.id(), "bettacraft:" + aquaristTradeSet(o.level()), o.buys() ? "minecraft:emerald" : gives(first)));
        }
        return out;
    }

    /** The trade set (and trade tag) of an aquarist level, {@code aquarist/level_<n>}. */
    public static String aquaristTradeSet(int level) {
        return "aquarist/level_" + level;
    }

    /** The item an entry stands for (what another mod reading the trade shows): the first product's item, else a fish bag. */
    private static String gives(@Nullable String target) {
        String path = target == null ? null : Target.itemPath(target);
        return "bettacraft:" + (path != null ? path : target != null && target.equals(Target.item("musk_turtle_bucket")) ? "musk_turtle_bucket" : "fish_bag");
    }
}
