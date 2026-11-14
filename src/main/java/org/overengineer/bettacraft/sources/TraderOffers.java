package org.overengineer.bettacraft.sources;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.world.TankmateTrades;

/**
 * M22.2: every offer Bettacraft gives the wandering trader, as one table. Each offer says in which slot it is (his usual
 * offers or the rare ones), what it can bring ({@link Product}: what, how many, the price, the share among the products
 * of the offer) and how it is made ({@code listing}). The trader gets the listings ({@link #onWandererTrades}); the
 * handbook prints the products ({@link Sources}); a GameTest checks that every listing only makes its products.
 * <p>
 * The offers are the ones of M12 (floating plants), M13 (aquarium plants), M14 (small animals), M19 (tankmates), M20
 * (reef) and M21.5 / M21.8, unchanged. Later milestones add theirs here.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class TraderOffers {
    private TraderOffers() {}

    public enum Slot { USUAL, RARE }

    /**
     * One possible result of an offer: the thing ({@link Target} key), how many, the price in emeralds (the lowest one if
     * it varies, {@code moreSometimes}: some offers of it cost more), its share among the results of the offer, and the
     * config switch it needs (null = always).
     */
    public record Product(String target, int count, int emeralds, double share, boolean moreSometimes, @Nullable String condition) {
        public Product(String target, int count, int emeralds) {
            this(target, count, emeralds, 1.0, false, null);
        }
    }

    public record Offer(String id, Slot slot, int uses, Supplier<List<Product>> products, VillagerTrades.ItemListing listing) {}

    /** Colour lines of the Neocaridina the trader brings (loci of {@link CritterSpecies#NEOCARIDINA}). */
    private static final int[] SHRIMP_LINES = {0, 1, 2, 3, 4, 5};

    private static List<Offer> offers;

    public static synchronized List<Offer> all() {
        if (offers == null) offers = build();
        return offers;
    }

    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        for (Offer o : all()) (o.slot() == Slot.USUAL ? event.getGenericTrades() : event.getRareTrades()).add(o.listing());
    }

    private static List<Offer> build() {
        List<Offer> out = new ArrayList<>();
        // ---------------------------------------------------------------- M14 small animals in fish bags
        out.add(new Offer("dwarf_crayfish", Slot.USUAL, 4, () -> List.of(new Product(Target.critter(CritterSpecies.DWARF_CRAYFISH), 2, 5)),
                offer(5, 4, (r, rnd) -> TankCritters.bagOf(CritterSpecies.DWARF_CRAYFISH, 2,
                        () -> CritterGenes.pure(0, 0.25F + rnd.nextFloat() * 0.25F), rnd, true))));
        out.add(new Offer("apple_snail", Slot.USUAL, 4, () -> List.of(new Product(Target.critter(CritterSpecies.APPLE_SNAIL), 1, 3)),
                offer(3, 4, (r, rnd) -> TankCritters.bagOf(CritterSpecies.APPLE_SNAIL, 1, () -> {
                    // M21.5: mostly gold (as in the shops), the other colours now and then
                    if (!CommonConfig.snailColors()) return CritterGenes.pure(rnd.nextBoolean() ? 0 : 1, 0.3F);
                    double x = rnd.nextDouble();
                    return CritterGenes.pure(x < 0.7 ? 0 : x < 0.8 ? 1 : x < 0.9 ? 2 : 3, 0.3F + rnd.nextFloat() * 0.2F);
                }, rnd, true))));
        out.add(new Offer("neocaridina", Slot.RARE, 3, () -> List.of(new Product(Target.critter(CritterSpecies.NEOCARIDINA), 2, 6)),
                offer(6, 3, (r, rnd) -> {
                    int line = SHRIMP_LINES[rnd.nextInt(SHRIMP_LINES.length)];
                    float grade = 0.15F + rnd.nextFloat() * 0.35F;
                    return TankCritters.bagOf(CritterSpecies.NEOCARIDINA, 2, () -> CritterGenes.pure(line, grade), rnd, true);
                })));
        out.add(new Offer("ramshorn", Slot.RARE, 3, () -> List.of(new Product(Target.critter(CritterSpecies.RAMSHORN), 2, 4)),
                offer(4, 3, (r, rnd) -> TankCritters.bagOf(CritterSpecies.RAMSHORN, 2,
                        () -> CritterGenes.pure(rnd.nextInt(CommonConfig.snailColors() ? 3 : 2), 0.3F), rnd, false))));
        // M21.5: racer nerites, red or yellow, every one its own shade
        out.add(new Offer("racer_nerites", Slot.RARE, 3, () -> List.of(
                new Product(Target.critter(CritterSpecies.RED_NERITE), 2, 4, 0.5, false, "features.snailColors"),
                new Product(Target.critter(CritterSpecies.YELLOW_NERITE), 2, 4, 0.5, false, "features.snailColors")),
                (trader, random) -> {
                    if (!CommonConfig.snailColors()) return null;
                    RandomGenerator rnd = random::nextLong;
                    CritterSpecies s = random.nextBoolean() ? CritterSpecies.RED_NERITE : CritterSpecies.YELLOW_NERITE;
                    return new MerchantOffer(new ItemCost(Items.EMERALD, 4), Optional.empty(), TankCritters.bagOf(s, 2, () -> CritterGenes.WILD, rnd, false), 3, 1, 0.05F);
                }));
        out.add(new Offer("assassin_snail", Slot.RARE, 2, () -> List.of(new Product(Target.critter(CritterSpecies.ASSASSIN_SNAIL), 1, 4)),
                offer(4, 2, (r, rnd) -> TankCritters.bagOf(CritterSpecies.ASSASSIN_SNAIL, 1, () -> CritterGenes.WILD, rnd, false))));
        out.add(new Offer("dwarf_clawed_frog", Slot.RARE, 2, () -> List.of(new Product(Target.FROG, 1, 6)),
                (trader, random) -> {
                    var frog = org.overengineer.bettacraft.registry.ModEntities.DWARF_FROG.get().create(trader.level());
                    if (frog == null) return null;
                    ItemStack bag = org.overengineer.bettacraft.entity.betta.FishBagItem.fillWith(frog);
                    frog.discard();
                    return new MerchantOffer(new ItemCost(Items.EMERALD, 6), Optional.empty(), bag, 2, 1, 0.05F);
                }));
        // ---------------------------------------------------------------- M19 tankmates: two usual offers, one rare
        out.add(new Offer("tankmates_1", Slot.USUAL, 3, () -> fishPool(TankmateSpecies.Trade.COMMON, false), TankmateTrades.listing(TankmateSpecies.Trade.COMMON)));
        out.add(new Offer("tankmates_2", Slot.USUAL, 3, () -> fishPool(TankmateSpecies.Trade.COMMON, false), TankmateTrades.listing(TankmateSpecies.Trade.COMMON)));
        out.add(new Offer("tankmates_rare", Slot.RARE, 3, () -> fishPool(TankmateSpecies.Trade.RARE, false), TankmateTrades.listing(TankmateSpecies.Trade.RARE)));
        // ---------------------------------------------------------------- M13 aquarium plants
        out.add(plant(Slot.USUAL, PlantSpecies.AMAZON_SWORD, 1, 1, 6));
        out.add(plant(Slot.USUAL, PlantSpecies.VALLISNERIA, 2, 1, 8));
        out.add(plant(Slot.USUAL, PlantSpecies.MONTE_CARLO, 2, 1, 8));
        out.add(plant(Slot.USUAL, PlantSpecies.ALTERNANTHERA, 1, 2, 6));
        out.add(plant(Slot.RARE, PlantSpecies.TIGER_LOTUS, 1, 4, 3));
        out.add(plant(Slot.RARE, PlantSpecies.CRYPT_FLAMINGO, 1, 5, 3));
        // ---------------------------------------------------------------- M12 floating plants
        out.add(floating(Slot.USUAL, FloatingPlantType.FROGBIT, 2, 1, 8));
        out.add(floating(Slot.USUAL, FloatingPlantType.SALVINIA, 3, 1, 8));
        out.add(floating(Slot.RARE, FloatingPlantType.RED_ROOT_FLOATER, 2, 3, 4));
        // ---------------------------------------------------------------- M20 reef
        out.add(new Offer("sea_salt", Slot.USUAL, 8, () -> List.of(new Product(Target.item("sea_salt"), 8, 2)),
                fixed(2, 8, () -> new ItemStack(ModItems.SEA_SALT.get(), 8))));
        out.add(new Offer("reef_fish", Slot.USUAL, 3, () -> fishPool(TankmateSpecies.Trade.COMMON, true), reefFish(TankmateSpecies.Trade.COMMON)));
        out.add(new Offer("coral_frags", Slot.USUAL, 3, () -> {
            List<Product> p = new ArrayList<>();
            for (PlantSpecies f : REEF_FRAGS) p.add(new Product(Target.plant(f), 2, 4, 1.0 / REEF_FRAGS.size(), false, null));
            return p;
        }, (trader, random) -> {
            RandomGenerator rnd = random::nextLong;
            ItemStack frag = InteriorPlants.item(REEF_FRAGS.get(rnd.nextInt(REEF_FRAGS.size())), 0);
            frag.setCount(2);
            return new MerchantOffer(new ItemCost(Items.EMERALD, 4), Optional.empty(), frag, 3, 1, 0.05F);
        }));
        out.add(new Offer("reef_fish_rare", Slot.RARE, 3, () -> fishPool(TankmateSpecies.Trade.RARE, true), reefFish(TankmateSpecies.Trade.RARE)));
        out.add(new Offer("reef_crew", Slot.RARE, 3, () -> List.of(
                new Product(Target.critter(CritterSpecies.CLEANER_SHRIMP), 1, 5, 0.5, false, null),
                new Product(Target.critter(CritterSpecies.HERMIT_CRAB), 3, 5, 0.5, false, null)),
                (trader, random) -> {
                    RandomGenerator rnd = random::nextLong;
                    ItemStack bag = rnd.nextBoolean() ? TankCritters.bagOf(CritterSpecies.CLEANER_SHRIMP, 1, () -> CritterGenes.WILD, rnd, false)
                            : TankCritters.bagOf(CritterSpecies.HERMIT_CRAB, 3, () -> CritterGenes.WILD, rnd, false);
                    return new MerchantOffer(new ItemCost(Items.EMERALD, 5), Optional.empty(), bag, 3, 1, 0.05F);
                }));
        out.add(new Offer("live_rock", Slot.RARE, 2, () -> List.of(new Product(Target.item("live_rock"), 8, 6)),
                fixed(6, 2, () -> new ItemStack(ModItems.LIVE_ROCK.get(), 8))));
        return List.copyOf(out);
    }

    /** M20: the soft coral frags and the anemone of the reef offer (all equally likely). */
    public static final List<PlantSpecies> REEF_FRAGS = List.of(PlantSpecies.ZOANTHUS, PlantSpecies.MUSHROOM_CORAL, PlantSpecies.BUBBLE_TIP_ANEMONE,
            PlantSpecies.GREEN_STAR_POLYPS);

    /** The species a tankmate offer picks from (one of them, all equally likely), with count and price. */
    public static List<Product> fishPool(TankmateSpecies.Trade kind, boolean marine) {
        List<TankmateSpecies> pool = new ArrayList<>();
        for (TankmateSpecies s : TankmateSpecies.values()) if (s.trade() == kind && s.marine() == marine && s.available()) pool.add(s);
        List<Product> out = new ArrayList<>();
        for (TankmateSpecies s : pool) {
            boolean strains = TankmateTrades.sellsStrains(s);
            out.add(new Product(Target.tankmate(s), TankmateTrades.count(s), TankmateTrades.basePrice(s), 1.0 / pool.size(), strains,
                    s.seahorse() ? "features.seahorses" : null));
        }
        return out;
    }

    private static Offer plant(Slot slot, PlantSpecies sp, int count, int emeralds, int uses) {
        return new Offer("plant_" + sp.id(), slot, uses, () -> List.of(new Product(Target.plant(sp), count, emeralds)),
                (trader, random) -> new BasicItemListing(emeralds, InteriorPlants.item(sp, 0).copyWithCount(count), uses, 1).getOffer(trader, random));
    }

    private static Offer floating(Slot slot, FloatingPlantType t, int count, int emeralds, int uses) {
        return new Offer("floating_" + t.id(), slot, uses, () -> List.of(new Product(Target.floating(t), count, emeralds)),
                (trader, random) -> new BasicItemListing(emeralds, TankPlants.item(t).copyWithCount(count), uses, 1).getOffer(trader, random));
    }

    /** A reef fish of this kind of offer (a pair, or a shoal for the chromis). */
    private static VillagerTrades.ItemListing reefFish(TankmateSpecies.Trade kind) {
        return (trader, random) -> {
            RandomGenerator rnd = random::nextLong;
            List<TankmateSpecies> pool = new ArrayList<>();
            for (TankmateSpecies s : TankmateSpecies.values()) if (s.trade() == kind && s.marine() && s.available()) pool.add(s); // M21.8: features.seahorses
            if (pool.isEmpty()) return null;
            return TankmateTrades.offer(pool.get(rnd.nextInt(pool.size())), rnd);
        };
    }

    private interface Maker {
        ItemStack make(net.minecraft.util.RandomSource random, RandomGenerator rnd);
    }

    private static VillagerTrades.ItemListing offer(int emeralds, int uses, Maker maker) {
        return (trader, random) -> {
            RandomGenerator rnd = random::nextLong;
            return new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), Optional.empty(), maker.make(random, rnd), uses, 1, 0.05F);
        };
    }

    private static VillagerTrades.ItemListing fixed(int emeralds, int uses, Supplier<ItemStack> item) {
        return (trader, random) -> new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), Optional.empty(), item.get(), uses, 1, 0.05F);
    }
}
