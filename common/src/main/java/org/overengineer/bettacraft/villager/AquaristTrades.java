package org.overengineer.bettacraft.villager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.FeatureContent;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.sources.Target;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.TankPlants;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateItems;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M47 part a: the trades of the aquarist villager on his five levels (C1), as one table like {@link
 * org.overengineer.bettacraft.sources.TraderOffers}: two offers per level, so a villager always gets both (vanilla picks two
 * of each level). An offer that sells picks one of its {@link Product}s (the handbook prints them, {@code Sources}); the
 * two buying offers take a betta in a bucket (novice) or a betta or bred tankmates in a fish bag (apprentice) and pay by the
 * genes ({@link AquaristPrices}; the result of such an offer is computed when the fish lies in the trade slot,
 * {@code MerchantContainerMixin}).
 * <ul>
 *     <li>Novice: food or floating plants; buys bettas in a bucket.</li>
 *     <li>Apprentice: aquarium plants; buys bettas and bred tankmates in a fish bag.</li>
 *     <li>Journeyman: shrimp or snails; test kits, artemia cysts or substrate.</li>
 *     <li>Expert: fine food cultures; rare aquarium plants.</li>
 *     <li>Master: a pair of a rare bred variety of a tankmate (two offers).</li>
 * </ul>
 * Every offer is null while {@code features.sources.aquaristVillager} is off; a product whose features are off is not
 * picked.
 */
public final class AquaristTrades {
    private AquaristTrades() {}

    /** One possible result of a selling offer: target key, count, price, share among the products of the offer. */
    public record Product(String target, int count, int emeralds, double share, @Nullable String condition) {}

    /**
     * One offer of a level: {@code buys} = the villager buys a fish and pays by its genes (no products). The listing is
     * made on first use.
     */
    public record Offer(String id, int level, boolean buys, Supplier<List<Product>> products, Supplier<VillagerTrades.ItemListing> factory) {
        public VillagerTrades.ItemListing listing() {
            return LISTINGS.computeIfAbsent(id, k -> {
                VillagerTrades.ItemListing l = factory.get();
                return (traderLevel, trader, random) -> Features.enabled(Feature.AQUARIST_VILLAGER) ? l.getOffer(traderLevel, trader, random) : null;
            });
        }
    }

    private static final java.util.Map<String, VillagerTrades.ItemListing> LISTINGS = new java.util.concurrent.ConcurrentHashMap<>();
    private static List<Offer> offers;

    /** The custom data key that marks the result of a buying offer (an emerald that stands for "by the genes"). */
    public static final String APPRAISE = "bettacraft_appraise";
    /** The level names (translation keys {@code merchant.level.<n>} of vanilla). */
    public static final String[] LEVELS = {"", "Novice", "Apprentice", "Journeyman", "Expert", "Master"};

    public static synchronized List<Offer> all() {
        if (offers == null) offers = build();
        return offers;
    }


    private static final List<FloatingPlantType> FLOATING = List.of(FloatingPlantType.FROGBIT, FloatingPlantType.SALVINIA, FloatingPlantType.DUCKWEED,
            FloatingPlantType.WATER_LETTUCE);
    private static final List<PlantSpecies> PLANTS = List.of(PlantSpecies.JAVA_FERN, PlantSpecies.ANUBIAS_NANA, PlantSpecies.JAVA_MOSS,
            PlantSpecies.CRYPT_GREEN, PlantSpecies.VALLISNERIA, PlantSpecies.AMAZON_SWORD, PlantSpecies.ROTALA, PlantSpecies.SAGITTARIA);
    /** M56: the terrarium plants the apprentice sells. */
    private static final List<PlantSpecies> TERRA_PLANTS = List.of(PlantSpecies.NEOREGELIA, PlantSpecies.BOSTON_FERN, PlantSpecies.POTHOS,
            PlantSpecies.FITTONIA, PlantSpecies.CUSHION_MOSS, PlantSpecies.ECHEVERIA, PlantSpecies.ALOE, PlantSpecies.SWAMP_SEDGE);
    private static final List<PlantSpecies> RARE_PLANTS = List.of(PlantSpecies.TIGER_LOTUS, PlantSpecies.CRYPT_FLAMINGO, PlantSpecies.BUCE_BLUE,
            PlantSpecies.LUDWIGIA_SUPER_RED, PlantSpecies.ROTALA_HRA, PlantSpecies.MARIMO, PlantSpecies.ECHINODORUS_RED_DIAMOND);

    private static List<Offer> build() {
        List<Offer> out = new ArrayList<>();
        // ---------------------------------------------------------------- novice
        List<Product> food = new ArrayList<>();
        food.add(new Product(Target.item("betta_pellets"), 8, 1, 0.25, null));
        food.add(new Product(Target.item("fish_flakes"), 8, 1, 0.25, null));
        for (FloatingPlantType t : FLOATING) food.add(new Product(Target.floating(t), 2, 1, 0.5 / FLOATING.size(), null));
        out.add(sell("food_or_floating", 1, food, 2, 16));
        out.add(new Offer("buys_bettas", 1, true, List::of, () -> buy(ModItems.BETTA_BUCKET.get(), 2, 10)));
        // ---------------------------------------------------------------- apprentice
        List<Product> plants = new ArrayList<>();
        for (PlantSpecies p : PLANTS) plants.add(new Product(Target.plant(p), 2, 2, 1.0 / PLANTS.size(), null));
        out.add(sell("plants", 2, plants, 5, 12));
        // M56: terrarium plants
        List<Product> terra = new ArrayList<>();
        for (PlantSpecies p : TERRA_PLANTS) terra.add(new Product(Target.plant(p), 2, 2, 1.0 / TERRA_PLANTS.size(), null));
        out.add(sell("terrarium_plants", 2, terra, 5, 12));
        out.add(new Offer("buys_fish_bags", 2, true, List::of, () -> buy(ModItems.FISH_BAG.get(), 2, 15)));
        // ---------------------------------------------------------------- journeyman
        List<Product> animals = List.of(new Product(Target.critter(CritterSpecies.NEOCARIDINA), 2, 4, 0.4, null),
                new Product(Target.critter(CritterSpecies.RAMSHORN), 2, 3, 0.2, null),
                new Product(Target.critter(CritterSpecies.ZEBRA_NERITE), 1, 3, 0.2, null),
                new Product(Target.critter(CritterSpecies.APPLE_SNAIL), 1, 3, 0.2, null));
        out.add(sell("shrimp_or_snails", 3, animals, 10, 8));
        List<Product> kit = List.of(new Product(Target.item("ph_test_kit"), 1, 3, 0.25, null), new Product(Target.item("tds_meter"), 1, 4, 0.25, null),
                new Product(Target.item("artemia_cysts"), 4, 2, 0.25, null), new Product(Target.item("active_soil"), 8, 3, 0.25, null));
        out.add(sell("kits_or_substrate", 3, kit, 10, 8));
        // ---------------------------------------------------------------- expert
        List<Product> cultures = List.of(new Product(Target.item("infusoria_culture"), 1, 3, 0.3, null),
                new Product(Target.item("artemia_culture"), 1, 4, 0.25, null), new Product(Target.item("green_water_culture"), 1, 3, 0.25, null),
                new Product(Target.item("rotifer_culture"), 1, 5, 0.2, null)); // M48 part b
        out.add(sell("cultures", 4, cultures, 15, 6));
        List<Product> rare = new ArrayList<>();
        for (PlantSpecies p : RARE_PLANTS) rare.add(new Product(Target.plant(p), 1, 5, 1.0 / RARE_PLANTS.size(), null));
        out.add(sell("rare_plants", 4, rare, 15, 6));
        // ---------------------------------------------------------------- master: a pair of a rare bred variety
        out.add(new Offer("rare_varieties_1", 5, false, AquaristTrades::rareVarieties, AquaristTrades::rareVarietyListing));
        out.add(new Offer("rare_varieties_2", 5, false, AquaristTrades::rareVarieties, AquaristTrades::rareVarietyListing));
        return List.copyOf(out);
    }

    /** The species of the master offers: fresh water tankmates with bred varieties (not the guppies' genome strains). */
    public static List<TankmateSpecies> varietySpecies() {
        List<TankmateSpecies> out = new ArrayList<>();
        for (TankmateSpecies s : TankmateSpecies.values()) {
            if (s.varieties().size() > 1 && !s.marine() && !s.isLarge() && !TankmateGenes.livebearer(s)) out.add(s);
        }
        return out;
    }

    /** M47: the price of a pair of a rare variety. */
    public static final int RARE_PAIR_PRICE = 14;

    private static List<Product> rareVarieties() {
        List<Product> out = new ArrayList<>();
        List<TankmateSpecies> pool = varietySpecies();
        for (TankmateSpecies s : pool) out.add(new Product(Target.tankmate(s), 2, RARE_PAIR_PRICE, 1.0 / pool.size(), s.featureKey()));
        return out;
    }

    private static VillagerTrades.ItemListing rareVarietyListing() {
        return (traderLevel, trader, random) -> {
            RandomGenerator rnd = random::nextLong;
            List<TankmateSpecies> pool = new ArrayList<>();
            for (TankmateSpecies s : varietySpecies()) if (s.available() && s.randomBred(rnd) != null) pool.add(s); // M48: forms switched off
            if (pool.isEmpty()) return null;
            TankmateSpecies s = pool.get(rnd.nextInt(pool.size()));
            String bred = s.randomBred(rnd); // M48: rare forms less often
            String variety = bred != null ? bred : s.variety(1 + rnd.nextInt(s.varieties().size() - 1));
            ItemStack bag = TankmateItems.groupBag(s, 2, () -> TankmateGenes.pure(s, variety), rnd, true);
            return new MerchantOffer(new ItemCost(Items.EMERALD, RARE_PAIR_PRICE), Optional.empty(), bag, 3, 30, 0.05F);
        };
    }

    /** A selling offer: one of the products whose features are on (by their shares). */
    private static Offer sell(String id, int level, List<Product> products, int xp, int uses) {
        return new Offer(id, level, false, () -> products, () -> (traderLevel, trader, random) -> {
            List<Product> on = new ArrayList<>();
            double total = 0;
            for (Product p : products) {
                if (Features.all(FeatureContent.ofTarget(p.target()))) {
                    on.add(p);
                    total += p.share();
                }
            }
            if (on.isEmpty()) return null;
            double x = random.nextDouble() * total;
            Product pick = on.getLast();
            for (Product p : on) {
                x -= p.share();
                if (x < 0) {
                    pick = p;
                    break;
                }
            }
            ItemStack stack = stack(pick, random::nextLong);
            if (stack.isEmpty()) return null;
            return new MerchantOffer(new ItemCost(Items.EMERALD, pick.emeralds()), Optional.empty(), stack, uses, xp, 0.05F);
        });
    }

    /** The thing a product stands for. */
    static ItemStack stack(Product p, RandomGenerator rnd) {
        String t = p.target();
        int c = t.indexOf(':');
        String kind = t.substring(0, c), id = t.substring(c + 1);
        return switch (kind) {
            case "plant" -> InteriorPlants.item(PlantSpecies.byId(id), 0).copyWithCount(p.count());
            case "floating" -> TankPlants.item(FloatingPlantType.byId(id)).copyWithCount(p.count());
            case "critter" -> {
                CritterSpecies s = CritterSpecies.byId(id);
                if (s == CritterSpecies.NEOCARIDINA) {
                    int line = rnd.nextInt(6);
                    float grade = 0.2F + rnd.nextFloat() * 0.35F;
                    yield TankCritters.bagOf(s, p.count(), () -> CritterGenes.pure(line, grade), rnd, true);
                }
                if (s == CritterSpecies.APPLE_SNAIL) yield TankCritters.bagOf(s, p.count(), () -> CritterGenes.pure(rnd.nextInt(4), 0.35F), rnd, true);
                if (s == CritterSpecies.RAMSHORN) yield TankCritters.bagOf(s, p.count(), () -> CritterGenes.pure(rnd.nextInt(2), 0.3F), rnd, false);
                yield TankCritters.bagOf(s, p.count(), () -> CritterGenes.WILD, rnd, false);
            }
            case "item" -> {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(BettaCraft.id(id));
                yield item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, p.count());
            }
            default -> ItemStack.EMPTY;
        };
    }

    // ------------------------------------------------------------------ buying by the genes

    /** The result shown for a buying offer: {@code shown} emeralds marked "by the genes" (replaced when a fish lies there). */
    public static ItemStack appraisalResult(int shown) {
        ItemStack s = new ItemStack(Items.EMERALD, shown);
        CompoundTag t = new CompoundTag();
        t.putBoolean(APPRAISE, true);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        s.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("trade.bettacraft.aquarist.by_genes")
                .setStyle(Style.EMPTY.withItalic(false).withColor(0xA0E0D0)))));
        return s;
    }

    /** Whether a stack is the marked result of a buying offer. */
    public static boolean isAppraisal(ItemStack s) {
        if (!s.is(Items.EMERALD)) return false;
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d != null && d.copyTag().contains(APPRAISE);
    }

    private static VillagerTrades.ItemListing buy(net.minecraft.world.item.Item pays, int shown, int xp) {
        return (traderLevel, trader, random) -> new MerchantOffer(new ItemCost(pays), Optional.empty(), appraisalResult(shown), 6, xp, 0F);
    }

    /** The emeralds a buying offer gives for the fish in the trade slot (0: the aquarist does not take it). */
    public static ItemStack payFor(ItemStack fish) {
        int price = AquaristPrices.price(fish);
        return price <= 0 ? ItemStack.EMPTY : new ItemStack(Items.EMERALD, price);
    }
}
