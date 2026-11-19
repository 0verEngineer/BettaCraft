package org.overengineer.bettacraft.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.villager.BettaTrades;

/**
 * M52c (26.1): our trades as data ({@link BettaTrades}). One {@code villager_trade} entry per offer (its item modifier
 * {@code bettacraft:trade} makes the offer in code), the trade tags the entries belong to (the wandering trader's
 * {@code minecraft:wandering_trader/common|uncommon}, the aquarist's {@code bettacraft:aquarist/level_<n>}) and the
 * aquarist's trade sets (two offers of each level, every one once).
 */
final class TradeData implements DataProvider {
    private final PackOutput.PathProvider trades, tradeTags, tradeSets;

    TradeData(PackOutput out) {
        trades = out.createRegistryElementsPathProvider(net.minecraft.core.registries.Registries.VILLAGER_TRADE);
        tradeTags = out.createRegistryTagsPathProvider(net.minecraft.core.registries.Registries.VILLAGER_TRADE);
        tradeSets = out.createRegistryElementsPathProvider(net.minecraft.core.registries.Registries.TRADE_SET);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> all = new ArrayList<>();
        Map<String, JsonArray> tags = new LinkedHashMap<>();
        for (BettaTrades.Entry e : BettaTrades.entries()) {
            JsonObject t = new JsonObject();
            JsonObject wants = new JsonObject();
            wants.addProperty("id", "minecraft:emerald");
            t.add("wants", wants);
            JsonObject gives = new JsonObject();
            gives.addProperty("id", e.gives());
            t.add("gives", gives);
            // M52e (26.3): one item modifier (given_item_modifier) instead of a list
            JsonObject f = new JsonObject();
            f.addProperty("function", BettaCraft.id("trade").toString());
            f.addProperty("offer", e.id());
            t.add("given_item_modifier", f);
            all.add(DataProvider.saveStable(cache, t, trades.json(BettaCraft.id(e.id()))));
            tags.computeIfAbsent(e.tag(), k -> new JsonArray()).add(BettaCraft.id(e.id()).toString());
        }
        tags.forEach((tag, values) -> {
            JsonObject j = new JsonObject();
            j.add("values", values);
            all.add(DataProvider.saveStable(cache, j, tradeTags.json(Identifier.parse(tag))));
        });
        for (int lvl = 1; lvl <= 5; lvl++) {
            JsonObject s = new JsonObject();
            s.addProperty("amount", BettaTrades.AQUARIST_OFFERS_PER_LEVEL); // M52e: an int provider (26.3)
            s.addProperty("random_sequence", BettaCraft.id("trade_set/" + BettaTrades.aquaristTradeSet(lvl)).toString());
            s.addProperty("trades", "#" + BettaCraft.id(BettaTrades.aquaristTradeSet(lvl)));
            all.add(DataProvider.saveStable(cache, s, tradeSets.json(BettaCraft.id(BettaTrades.aquaristTradeSet(lvl)))));
        }
        return CompletableFuture.allOf(all.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Bettacraft trades";
    }
}
