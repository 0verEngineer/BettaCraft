package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.sources.Target;
import org.overengineer.bettacraft.sources.TraderOffers;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tankmates.TankmateItems;
import org.overengineer.bettacraft.world.AquaticPlantFishing;

/**
 * M22.2: the data tables behind the handbook's "Obtaining" sections are the ones the game uses, and the refactoring into
 * tables changed nothing: the fishing tables pick exactly what the old roll chains picked, every offer of the trader table
 * reaches the wandering trader and only makes the products it declares (thing, count, price), and the chances the
 * handbook prints for the net ({@link NetCatching#odds}) match what {@link NetCatching#decide} rolls.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SourcesGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private SourcesGameTests() {}

    /** A generator whose nextInt(bound) returns the given roll (the fishing picks draw exactly one int). */
    private static RandomGenerator roll(int r) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                throw new IllegalStateException("only nextInt expected");
            }

            @Override
            public int nextInt(int bound) {
                return r;
            }
        };
    }

    /** The M13 roll chain of AquaticPlantFishing.pick before M22.2. */
    private static PlantSpecies oldAquatic(boolean swamp, int roll) {
        if (swamp) {
            if (roll < 35) return PlantSpecies.CRYPT_GREEN;
            if (roll < 60) return PlantSpecies.CRYPT_BROWN;
            if (roll < 80) return PlantSpecies.LUDWIGIA;
            return PlantSpecies.ROTALA;
        }
        if (roll < 30) return PlantSpecies.JAVA_FERN;
        if (roll < 60) return PlantSpecies.ANUBIAS_NANA;
        if (roll < 75) return PlantSpecies.BUCE_GREEN_WAVY;
        if (roll < 88) return PlantSpecies.BUCE_MINI_NEEDLE;
        if (roll < 94) return PlantSpecies.BUCE_BLUE;
        return PlantSpecies.BUCE_BROWNIE_RED;
    }

    /** The M12 roll chain of FloatingPlantFishingModifier.pick before M22.2. */
    private static FloatingPlantType oldFloating(boolean mangrove, int roll) {
        if (mangrove) {
            if (roll < 35) return FloatingPlantType.DUCKWEED;
            if (roll < 60) return FloatingPlantType.SALVINIA;
            if (roll < 85) return FloatingPlantType.FROGBIT;
            return FloatingPlantType.RED_ROOT_FLOATER;
        }
        if (roll < 45) return FloatingPlantType.DUCKWEED;
        if (roll < 75) return FloatingPlantType.SALVINIA;
        return FloatingPlantType.FROGBIT;
    }

    /**
     * The fishing tables pick like the old roll chains where they did not change (floating plants, the mangrove swamp and
     * the swamp water bottle); M22.4: the jungle, river and swamp tables start with the old ones and add the new plants
     * after them (rolls past the old weights).
     */
    @GameTest(template = EMPTY)
    public static void fishingTablesPickLikeBefore(GameTestHelper h) {
        for (int r = 0; r < 100; r++) {
            for (boolean swamp : new boolean[]{false, true}) {
                PlantSpecies before = oldAquatic(swamp, r);
                if (swamp) {
                    h.assertTrue(AquaticPlantFishing.pick(AquaticPlantFishing.Water.MANGROVE, roll(r)) == before, "mangrove roll " + r);
                    h.assertTrue(org.overengineer.bettacraft.sources.WildTables.BOTTLE_CUTTINGS.pick(roll(r)) == before, "bottle roll " + r);
                    h.assertTrue(AquaticPlantFishing.pick(AquaticPlantFishing.Water.SWAMP, roll(r)) == before, "swamp roll " + r);
                } else {
                    for (var w : new AquaticPlantFishing.Water[]{AquaticPlantFishing.Water.JUNGLE, AquaticPlantFishing.Water.RIVER}) {
                        PlantSpecies now = AquaticPlantFishing.pick(w, roll(r));
                        h.assertTrue(now == before, "aquatic " + w + " roll " + r + ": " + now + " instead of " + before);
                    }
                }
                FloatingPlantType f = org.overengineer.bettacraft.world.LootChanges.pickFloating(swamp, roll(r)), fb = oldFloating(swamp, r);
                h.assertTrue(f == fb, "floating mangrove=" + swamp + " roll " + r + ": " + f + " instead of " + fb);
            }
        }
        h.assertTrue(AquaticPlantFishing.table(AquaticPlantFishing.Water.MANGROVE).total() == 100, "mangrove weights sum to 100");
        h.assertTrue(AquaticPlantFishing.pick(AquaticPlantFishing.Water.JUNGLE, roll(100)) == PlantSpecies.AMAZON_SWORD, "jungle: sword after the epiphytes");
        h.assertTrue(AquaticPlantFishing.pick(AquaticPlantFishing.Water.JUNGLE, roll(112)) == PlantSpecies.TIGER_LOTUS, "jungle: tiger lotus");
        h.assertTrue(AquaticPlantFishing.pick(AquaticPlantFishing.Water.RIVER, roll(100)) == PlantSpecies.VALLISNERIA, "river: Vallisneria");
        h.assertTrue(AquaticPlantFishing.pick(AquaticPlantFishing.Water.RIVER, roll(111)) == PlantSpecies.MONTE_CARLO, "river: Monte Carlo");
        h.assertTrue(AquaticPlantFishing.pick(AquaticPlantFishing.Water.SWAMP, roll(100)) == PlantSpecies.ALTERNANTHERA, "swamp: Alternanthera");
        h.assertTrue(AquaticPlantFishing.pick(AquaticPlantFishing.Water.SWAMP, roll(106)) == PlantSpecies.TIGER_LOTUS, "swamp: tiger lotus");
        h.assertTrue(AquaticPlantFishing.pick(AquaticPlantFishing.Water.SWAMP, roll(111)) == PlantSpecies.CRYPT_FLAMINGO, "swamp: Flamingo");
        h.succeed();
    }

    /** What an offer result is, as a {@link Target} key, and how many. */
    private static String target(ItemStack r) {
        var bag = TankCritters.bagged(r);
        if (bag != null) return Target.critter(Critter.loadAnimal(bag.getFirst(), 0, 0, 0, 0).species);
        if (TankmateItems.bagSpecies(r) != null) return Target.tankmate(TankmateItems.bagSpecies(r));
        if (r.getItem() instanceof FishBagItem && FishBagItem.containedType(r) == ModEntities.DWARF_FROG.get()) return Target.FROG;
        String path = BuiltInRegistries.ITEM.getKey(r.getItem()).getPath();
        if (PlantSpecies.byIdOrNull(path) != null) return Target.plant(PlantSpecies.byIdOrNull(path));
        if (FloatingPlantType.byIdOrNull(path) != null) return Target.floating(FloatingPlantType.byIdOrNull(path));
        return Target.item(path);
    }

    private static int count(ItemStack r) {
        var bag = TankCritters.bagged(r);
        if (bag != null) return bag.size();
        if (TankmateItems.bagSpecies(r) != null) return TankmateItems.bagFish(r).size();
        if (r.getItem() instanceof FishBagItem) return 1;
        return r.getCount();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void traderOffersMakeOnlyTheirProducts(GameTestHelper h) {
        WanderingTrader trader = h.spawn(EntityType.WANDERING_TRADER, new BlockPos(2, 1, 2));
        var rnd = h.getLevel().getRandom();
        // every offer of the table reaches the trader (NeoForge builds his lists from the WandererTradesEvent)
        Set<VillagerTrades.ItemListing> registered = new HashSet<>();
        for (int level = 1; level <= 2; level++) registered.addAll(List.of(VillagerTrades.WANDERING_TRADER_TRADES.get(level)));
        for (TraderOffers.Offer o : TraderOffers.all()) {
            h.assertTrue(registered.contains(o.gatedListing()), "offer " + o.id() + " is not among the trader's listings");
            boolean usual = List.of(VillagerTrades.WANDERING_TRADER_TRADES.get(1)).contains(o.gatedListing());
            h.assertTrue(usual == (o.slot() == TraderOffers.Slot.USUAL), "offer " + o.id() + " in the wrong slot");
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < 60; i++) {
                MerchantOffer m = o.listing().getOffer(trader, rnd);
                if (m == null) continue;
                String t = target(m.getResult());
                int n = count(m.getResult()), price = m.getBaseCostA().getCount();
                seen.add(t);
                TraderOffers.Product p = null;
                for (TraderOffers.Product q : o.products().get()) if (q.target().equals(t)) p = q;
                h.assertTrue(p != null, "offer " + o.id() + " made " + t + ", not among its products");
                h.assertTrue(n == p.count(), "offer " + o.id() + ": " + n + " of " + t + " instead of " + p.count());
                h.assertTrue(price == p.emeralds() || (p.moreSometimes() && price == p.emeralds() + 2),
                        "offer " + o.id() + ": " + t + " for " + price + " emeralds instead of " + p.emeralds());
                h.assertTrue(o.uses() == m.getMaxUses(), "offer " + o.id() + ": " + m.getMaxUses() + " uses instead of " + o.uses());
            }
            // every declared product shows up (the tankmate pools have up to 16 species: 60 tries are plenty for the small ones)
            if (o.products().get().size() <= 4) {
                for (TraderOffers.Product q : o.products().get()) h.assertTrue(seen.contains(q.target()), "offer " + o.id() + " never made " + q.target());
            }
        }
        trader.discard();
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void netOddsMatchTheRolls(GameTestHelper h) {
        java.util.Random random = new java.util.Random(22);
        RandomGenerator rnd = random::nextLong;
        List<String> problems = new ArrayList<>();
        for (NetCatching.Water w : NetCatching.Water.values()) {
            for (boolean plants : new boolean[]{false, true}) {
                boolean hard = plants, lush = w == NetCatching.Water.CAVE && plants;
                var plantTable = org.overengineer.bettacraft.sources.WildTables.netPlants(w);
                Map<Object, Double> odds = NetCatching.odds(w, plants, hard, lush, 0.3, 0.4, 0.4, 0.2, plantTable);
                Map<Object, Integer> hits = new java.util.HashMap<>();
                int n = 120_000;
                for (int i = 0; i < n; i++) {
                    var o = NetCatching.decide(w, plants, hard, lush, 0.3, 0.4, 0.4, 0.2, plantTable, rnd);
                    Object k = o.frag() != null ? o.frag() : o.plant() != null ? o.plant() : o.fish() != null ? o.fish() : o.critter() != null
                            ? (o.critter().species() == null ? "frog" : o.critter().species()) : null;
                    if (k != null) hits.merge(k, 1, Integer::sum);
                }
                Set<Object> keys = new HashSet<>(odds.keySet());
                keys.addAll(hits.keySet());
                for (Object k : keys) {
                    double expected = odds.getOrDefault(k, 0.0), seen = hits.getOrDefault(k, 0) / (double) n;
                    double tol = 4 * Math.sqrt(Math.max(expected, 1e-4) * (1 - expected) / n) + 0.0005;
                    if (Math.abs(expected - seen) > tol) problems.add(w + "/" + plants + " " + k + ": book " + expected + ", rolled " + seen);
                }
            }
        }
        h.assertTrue(problems.isEmpty(), "net odds differ from the rolls: " + problems);
        h.succeed();
    }
}
