package org.overengineer.bettacraft.world;

import java.util.Optional;
import java.util.random.RandomGenerator;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateItems;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M19: the wandering trader sells tankmates in fish bags: a shoal (six of a shoaling species, males and females) or a
 * pair, among his usual offers the common species, among the rare ones the others (L-numbers, glowlights, clown loaches).
 * Bred varieties (albino bristlenoses, coloured guppies and platys) come now and then. M21.6: guppies and endlers come
 * wild-caught or as one of the named strains (Red Cobra, Blue Moscow, Full Gold, Tuxedo Koi, Endler Tiger, ...).
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class TankmateTrades {
    private TankmateTrades() {}

    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        // two offers among the usual ones, one among the rare ones; each picks a species of its kind
        event.getGenericTrades().add(listing(TankmateSpecies.Trade.COMMON));
        event.getGenericTrades().add(listing(TankmateSpecies.Trade.COMMON));
        event.getRareTrades().add(listing(TankmateSpecies.Trade.RARE));
    }

    public static net.minecraft.world.entity.npc.VillagerTrades.ItemListing listing(TankmateSpecies.Trade kind) {
        return (trader, random) -> {
            RandomGenerator rnd = random::nextLong;
            java.util.List<TankmateSpecies> pool = new java.util.ArrayList<>();
            for (TankmateSpecies s : TankmateSpecies.values()) if (s.trade() == kind && !s.marine() && s.available()) pool.add(s); // M20: reef fish in ReefTrades
            if (pool.isEmpty()) return null;
            return offer(pool.get(rnd.nextInt(pool.size())), rnd);
        };
    }

    /** A shoal (six or the species' minimum) or a pair of one species, now and then of a bred variety. */
    public static MerchantOffer offer(TankmateSpecies s, RandomGenerator rnd) {
        int n = Math.min(TankmateItems.MAX_PER_BAG, s.shoaling() ? Math.max(6, s.shoal()) : 2);
        int price = Math.max(2, (int) Math.round(2 + s.stock() * n * 0.8 + (s.trade() == TankmateSpecies.Trade.RARE ? 3 : 0)));
        java.util.function.Supplier<TankmateGenes> genes;
        if (TankmateGenes.livebearer(s) && org.overengineer.bettacraft.config.CommonConfig.livebearerGenetics()) {
            // M21.6: guppies and endlers: wild-caught ones (each its own genes) or a pure-bred named strain
            var strains = traderStrains(s);
            if (rnd.nextInt(5) < 3 && !strains.isEmpty()) {
                var st = strains.get(rnd.nextInt(strains.size()));
                genes = () -> TankmateGenes.strain(s, st);
                price += 2;
            } else {
                genes = () -> TankmateGenes.wild(s, 0.08, rnd);
            }
        } else {
            String variety = s.varieties().size() > 1 && rnd.nextInt(3) == 0 ? s.variety(1 + rnd.nextInt(s.varieties().size() - 1)) : s.variety(0);
            genes = () -> TankmateGenes.pure(s, variety);
        }
        var bag = TankmateItems.groupBag(s, n, genes, rnd, true);
        return new MerchantOffer(new ItemCost(Items.EMERALD, Math.min(64, price)), Optional.empty(), bag, 3, 1, 0.05F);
    }

    /** M21.6: the named strains the trader sells of a guppy or endler species. */
    public static java.util.List<org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain> traderStrains(TankmateSpecies s) {
        java.util.List<org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain> out = new java.util.ArrayList<>();
        for (var st : org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain.of(s == TankmateSpecies.ENDLER_GUPPY)) {
            if (st.source == org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain.Source.TRADER) out.add(st);
        }
        return out;
    }
}
