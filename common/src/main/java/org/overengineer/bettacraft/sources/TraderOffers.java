package org.overengineer.bettacraft.sources;

import org.overengineer.bettacraft.config.FeatureContent;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
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
import org.overengineer.bettacraft.villager.BasicItemListing;
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

    /**
     * One offer. The listing is made on first use ({@link #listing()}, the same object afterwards): the table itself can be
     * read without the item registry (handbook generator, unit tests).
     */
    public record Offer(String id, Slot slot, int uses, Supplier<List<Product>> products, Supplier<VillagerTrades.ItemListing> factory) {
        Offer(String id, Slot slot, int uses, Supplier<List<Product>> products, VillagerTrades.ItemListing listing) {
            this(id, slot, uses, products, () -> listing);
        }

        public VillagerTrades.ItemListing listing() {
            return LISTINGS.computeIfAbsent(id, k -> factory.get());
        }

        /** M33: the offer is made while at least one of its products has all its features on ({@link FeatureContent#ofTarget}). */
        public boolean allowed() {
            for (Product p : products.get()) if (Features.all(FeatureContent.ofTarget(p.target()))) return true;
            return false;
        }

        /** M33: the listing the trader gets: nothing while the offer is not {@link #allowed}, asked at every new trader. */
        public VillagerTrades.ItemListing gatedListing() {
            return GATED.computeIfAbsent(id, k -> (trader, random) -> allowed() ? listing().getOffer(trader, random) : null);
        }
    }

    private static final java.util.Map<String, VillagerTrades.ItemListing> LISTINGS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, VillagerTrades.ItemListing> GATED = new java.util.concurrent.ConcurrentHashMap<>();

    /** Colour lines of the Neocaridina the trader brings (loci of {@link CritterSpecies#NEOCARIDINA}). */
    private static final int[] SHRIMP_LINES = {0, 1, 2, 3, 4, 5};

    private static List<Offer> offers;

    public static synchronized List<Offer> all() {
        if (offers == null) offers = build();
        return offers;
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
                    if (!Features.enabled(Feature.SNAIL_COLORS)) return CritterGenes.pure(rnd.nextBoolean() ? 0 : 1, 0.3F);
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
                        () -> CritterGenes.pure(rnd.nextInt(Features.enabled(Feature.SNAIL_COLORS) ? 3 : 2), 0.3F), rnd, false))));
        // M21.5: racer nerites, red or yellow, every one its own shade
        out.add(new Offer("racer_nerites", Slot.RARE, 3, () -> List.of(
                new Product(Target.critter(CritterSpecies.RED_NERITE), 2, 4, 0.5, false, Feature.SNAIL_COLORS.key()),
                new Product(Target.critter(CritterSpecies.YELLOW_NERITE), 2, 4, 0.5, false, Feature.SNAIL_COLORS.key())),
                (trader, random) -> {
                    if (!Features.enabled(Feature.SNAIL_COLORS)) return null;
                    RandomGenerator rnd = random::nextLong;
                    CritterSpecies s = random.nextBoolean() ? CritterSpecies.RED_NERITE : CritterSpecies.YELLOW_NERITE;
                    return new MerchantOffer(new ItemCost(Items.EMERALD, 4), Optional.empty(), TankCritters.bagOf(s, 2, () -> CritterGenes.WILD, rnd, false), 3, 1, 0.05F);
                }));
        // M25 part c: crystal bee shrimp (A or S grade) and the Sulawesi animals, rare
        out.add(new Offer("caridina", Slot.RARE, 2, () -> List.of(new Product(Target.critter(CritterSpecies.CARIDINA), 2, 8, 1.0, false, Feature.CARIDINA.key())),
                (trader, random) -> {
                    if (!Features.enabled(Feature.CARIDINA)) return null;
                    RandomGenerator rnd = random::nextLong;
                    int line = random.nextBoolean() ? CritterGenes.CARIDINA_RED : CritterGenes.CARIDINA_BLACK;
                    float grade = 0.2F + random.nextFloat() * 0.3F;
                    return new MerchantOffer(new ItemCost(Items.EMERALD, 8), Optional.empty(),
                            TankCritters.bagOf(CritterSpecies.CARIDINA, 2, () -> CritterGenes.pure(line, grade), rnd, true), 2, 1, 0.05F);
                }));
        out.add(new Offer("sulawesi", Slot.RARE, 2, () -> List.of(
                new Product(Target.critter(CritterSpecies.SULAWESI_SHRIMP), 2, 7, 0.5, false, Feature.SULAWESI.key()),
                new Product(Target.critter(CritterSpecies.TYLOMELANIA), 2, 7, 0.5, false, Feature.SULAWESI.key())),
                (trader, random) -> {
                    if (!Features.enabled(Feature.SULAWESI)) return null;
                    RandomGenerator rnd = random::nextLong;
                    ItemStack bag = random.nextBoolean() ? TankCritters.bagOf(CritterSpecies.SULAWESI_SHRIMP, 2, () -> CritterGenes.WILD, rnd, true)
                            : TankCritters.bagOf(CritterSpecies.TYLOMELANIA, 2, () -> CritterGenes.WILD, rnd, true);
                    return new MerchantOffer(new ItemCost(Items.EMERALD, 7), Optional.empty(), bag, 2, 1, 0.05F);
                }));
        // M29 part d: the big crayfish, one per bag: a blue or an orange Florida crayfish, a marbled crayfish
        out.add(new Offer("florida_crayfish", Slot.RARE, 2, () -> List.of(
                new Product(Target.critter(CritterSpecies.FLORIDA_CRAYFISH), 1, 8, 1.0, false, Feature.BIG_CRAYFISH.key())),
                (trader, random) -> {
                    if (!Features.enabled(Feature.BIG_CRAYFISH)) return null;
                    RandomGenerator rnd = random::nextLong;
                    int line = random.nextFloat() < 0.65F ? 0 : 1;
                    return new MerchantOffer(new ItemCost(Items.EMERALD, 8), Optional.empty(),
                            TankCritters.bagOf(CritterSpecies.FLORIDA_CRAYFISH, 1, () -> CritterGenes.pure(line, 0.3F + random.nextFloat() * 0.3F), rnd, true),
                            2, 1, 0.05F);
                }));
        out.add(new Offer("marbled_crayfish", Slot.USUAL, 3, () -> List.of(
                new Product(Target.critter(CritterSpecies.MARBLED_CRAYFISH), 1, 4, 1.0, false, Feature.BIG_CRAYFISH.key())),
                (trader, random) -> {
                    if (!Features.enabled(Feature.BIG_CRAYFISH)) return null;
                    RandomGenerator rnd = random::nextLong;
                    return new MerchantOffer(new ItemCost(Items.EMERALD, 4), Optional.empty(),
                            TankCritters.bagOf(CritterSpecies.MARBLED_CRAYFISH, 1, () -> CritterGenes.WILD, rnd, true), 3, 1, 0.05F);
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
        // M31: the musk turtle in a bucket of water
        out.add(new Offer("musk_turtle", Slot.RARE, 2, () -> List.of(new Product(Target.item("musk_turtle_bucket"), 1, 7, 1.0, false, Feature.TURTLES.key())),
                (trader, random) -> {
                    if (!Features.enabled(Feature.TURTLES)) return null;
                    var turtle = org.overengineer.bettacraft.registry.ModEntities.MUSK_TURTLE.get().create(trader.level());
                    if (turtle == null) return null;
                    ItemStack bucket = turtle.getBucketItemStack();
                    turtle.saveToBucketTag(bucket);
                    turtle.discard();
                    return new MerchantOffer(new ItemCost(Items.EMERALD, 7), Optional.empty(), bucket, 2, 1, 0.05F);
                }));
        // M49 part b: the mudskipper in its terrarium box (features.brackish.mudskipper)
        out.add(new Offer("mudskipper", Slot.RARE, 2, () -> List.of(new Product(Target.item("mudskipper_box"), 1, 6, 1.0, false, Feature.MUDSKIPPER.key())),
                (trader, random) -> {
                    if (!Features.enabled(Feature.MUDSKIPPER)) return null;
                    var m = org.overengineer.bettacraft.registry.ModEntities.herp(org.overengineer.bettacraft.terrarium.HerpSpecies.MUDSKIPPER).create(trader.level());
                    if (m == null) return null;
                    ItemStack box = m.getBucketItemStack();
                    m.saveToBucketTag(box);
                    m.discard();
                    return new MerchantOffer(new ItemCost(Items.EMERALD, 6), Optional.empty(), box, 2, 1, 0.05F);
                }));
        // M49 part b: Amano shrimp in a fish bag (features.brackish.amanoShrimp)
        out.add(new Offer("amano_shrimp", Slot.USUAL, 3, () -> List.of(new Product(Target.critter(CritterSpecies.AMANO_SHRIMP), 3, 5, 1.0, false, null)),
                (trader, random) -> new MerchantOffer(new ItemCost(Items.EMERALD, 5), Optional.empty(),
                        TankCritters.bagOf(CritterSpecies.AMANO_SHRIMP, 3, () -> CritterGenes.WILD, random::nextLong, false), 3, 1, 0.05F)));
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
        // M48 part b: the fire shrimp (features.saltwater.moreReefAnimals), one at a time
        out.add(new Offer("fire_shrimp", Slot.RARE, 3, () -> List.of(new Product(Target.critter(CritterSpecies.FIRE_SHRIMP), 1, 8, 1.0, false, null)),
                (trader, random) -> new MerchantOffer(new ItemCost(Items.EMERALD, 8), Optional.empty(),
                        TankCritters.bagOf(CritterSpecies.FIRE_SHRIMP, 1, () -> CritterGenes.WILD, random::nextLong, false), 3, 1, 0.05F)));
        out.add(new Offer("live_rock", Slot.RARE, 2, () -> List.of(new Product(Target.item("live_rock"), 8, 6)),
                fixed(6, 2, () -> new ItemStack(ModItems.LIVE_ROCK.get(), 8))));
        // M49 part a: Chaetomorpha for the refugium of a sump (features.sump.refugium)
        out.add(new Offer("chaetomorpha", Slot.RARE, 3, () -> List.of(new Product(Target.item("chaetomorpha"), 4, 3)),
                fixed(3, 3, () -> new ItemStack(ModItems.CHAETOMORPHA.get(), 4))));
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
                    s.seahorse() ? Feature.SEAHORSES.key() : null));
        }
        return out;
    }

    private static Offer plant(Slot slot, PlantSpecies sp, int count, int emeralds, int uses) {
        return new Offer("plant_" + sp.id(), slot, uses, () -> List.of(new Product(Target.plant(sp), count, emeralds)),
                () -> new BasicItemListing(emeralds, InteriorPlants.item(sp, 0).copyWithCount(count), uses, 1));
    }

    private static Offer floating(Slot slot, FloatingPlantType t, int count, int emeralds, int uses) {
        return new Offer("floating_" + t.id(), slot, uses, () -> List.of(new Product(Target.floating(t), count, emeralds)),
                () -> new BasicItemListing(emeralds, TankPlants.item(t).copyWithCount(count), uses, 1));
    }

    /** A reef fish of this kind of offer (a pair, or a shoal for the chromis). */
    private static VillagerTrades.ItemListing reefFish(TankmateSpecies.Trade kind) {
        return (trader, random) -> {
            RandomGenerator rnd = random::nextLong;
            List<TankmateSpecies> pool = new ArrayList<>();
            for (TankmateSpecies s : TankmateSpecies.values()) if (s.trade() == kind && s.marine() && s.available()) pool.add(s); // M21.8: features.saltwater.seahorses
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
