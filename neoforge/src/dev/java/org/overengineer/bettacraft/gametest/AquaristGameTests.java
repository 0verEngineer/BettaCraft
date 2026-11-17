package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.genetics.BettaGenome;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tankmates.TankmateBucketItem;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateItems;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.villager.AquaristPrices;
import org.overengineer.bettacraft.villager.AquaristTrades;
import org.overengineer.bettacraft.villager.Aquarists;
import org.overengineer.bettacraft.villager.ModVillagers;

/**
 * M47 part a: the aquarist villager. The breeding basin is his workplace (an unemployed villager next to it takes the
 * job), each of the five levels has its two offers, the buying offers pay by the genes (a common betta little, a bred
 * long-finned betta with rare genes more, wild-caught tankmates nothing, a bred Black ram more than a bred wild-type), the
 * price table can be changed, and with {@code features.sources.aquaristVillager} off an aquarist loses his job and offers
 * and no offer is made.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AquaristGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private AquaristGameTests() {}

    private static void check(boolean ok, String what) {
        if (!ok) throw new GameTestAssertException(what);
    }

    private static Villager aquarist(GameTestHelper h, BlockPos at, int level) {
        Villager v = h.spawn(EntityType.VILLAGER, at);
        v.setVillagerData(v.getVillagerData().setProfession(ModVillagers.AQUARIST.get()).setLevel(level));
        v.setNoAi(true);
        return v;
    }

    /**
     * An unemployed villager next to a breeding basin becomes an aquarist (the basin is his point of interest). M52: in a
     * batch of its own, the hired aquarists of other tests nearby claim basins and beds (the order of the tests differs
     * by loader).
     */
    @GameTest(template = EMPTY, timeoutTicks = 1600, batch = "bettacraft_aquarist_basin")
    public static void basinMakesAnAquarist(GameTestHelper h) {
        BlockPos basin = new BlockPos(8, 1, 8);
        h.setBlock(basin.below(), net.minecraft.world.level.block.Blocks.STONE);
        h.setBlock(basin, ModBlocks.BREEDING_BASIN.get().defaultBlockState());
        var poi = PoiTypes.forState(ModBlocks.BREEDING_BASIN.get().defaultBlockState());
        check(poi.isPresent() && poi.get().is(ModVillagers.AQUARIST_POI_KEY), "the basin is the aquarist's point of interest: " + poi);
        check(ModVillagers.AQUARIST.get().acquirableJobSite().test(poi.get()), "the profession takes the basin");
        check(VillagerProfession.NONE.acquirableJobSite().test(poi.get()), "an unemployed villager looks for it");
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) h.setBlock(new BlockPos(x, 0, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
        Villager v = h.spawn(EntityType.VILLAGER, new BlockPos(10, 1, 8));
        h.succeedWhen(() -> check(Aquarists.isAquarist(v), "still " + v.getVillagerData().getProfession()));
    }

    /** Each level has its two offers, every listing gives an offer, the shown items are what the table says. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void tradesOnEveryLevel(GameTestHelper h) {
        var trades = VillagerTrades.TRADES.get(ModVillagers.AQUARIST.get());
        check(trades != null, "no trades for the aquarist");
        Villager v = aquarist(h, new BlockPos(3, 1, 3), 1);
        var rnd = h.getLevel().getRandom();
        for (int level = 1; level <= 5; level++) {
            VillagerTrades.ItemListing[] list = trades.get(level);
            check(list != null && list.length == 2, "level " + level + ": " + (list == null ? 0 : list.length) + " offers");
            for (int k = 0; k < 2; k++) {
                for (int i = 0; i < 20; i++) {
                    MerchantOffer o = list[k].getOffer(v, rnd);
                    check(o != null && !o.getResult().isEmpty(), "level " + level + " offer " + k + " made nothing");
                    boolean buys = AquaristTrades.isAppraisal(o.getResult());
                    if (level <= 2 && k == 1) {
                        check(buys, "level " + level + ": the second offer buys fish");
                        check(o.getItemCostA().item().value() == (level == 1 ? ModItems.BETTA_BUCKET.get() : ModItems.FISH_BAG.get()), "pays for " + o.getItemCostA());
                    } else {
                        check(!buys && o.getItemCostA().item().value() == Items.EMERALD, "level " + level + " sells for emeralds: " + o.getResult());
                    }
                    if (level == 5) {
                        var fish = TankmateItems.bagFish(o.getResult());
                        check(fish.size() == 2, "a pair: " + fish.size());
                        CompoundTag f = fish.getFirst().contains("tankmate") ? fish.getFirst().getCompoundOrEmpty("tankmate") : fish.getFirst();
                        TankmateSpecies s = TankmateSpecies.byId(f.getStringOr("species", ""));
                        TankmateGenes g = TankmateGenes.load(f.getCompoundOrEmpty("genes"), s);
                        check(!g.phenotype(s).equals(s.variety(0)) && g.pure(), "a pure bred variety: " + s.id() + " " + g.text());
                    }
                }
            }
        }
        // a new aquarist gets the two offers of his level
        check(v.getOffers().size() == 2, "novice offers " + v.getOffers().size());
        v.discard();
        h.succeed();
    }

    private static ItemStack betta(BettaGenome g, int generation, String stage) {
        BettaData d = new BettaData(BettaGenotype.homozygous(g), false, stage, 0, UUID.randomUUID(), Optional.empty(), Optional.empty(), generation, 0L,
                new BettaHealth().save(), new CompoundTag(), new CompoundTag());
        return BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d);
    }

    private static ItemStack tankmate(TankmateSpecies s, String a, String b, int generation) {
        ItemStack bag = TankmateBucketItem.withFish(new ItemStack(ModItems.FISH_BAG.get()), s, new TankmateGenes(a, b), false, LifeStage.ADULT);
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA, bag,
                t -> t.getCompoundOrEmpty("tankmate").putInt("gen", generation));
        bag.set(org.overengineer.bettacraft.registry.ModDataComponents.BAG_ENTITY.get(),
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(org.overengineer.bettacraft.registry.ModEntities.TANKMATES.get(s).get()));
        return bag;
    }

    /** What the trade slot gives for a fish (the buying offer of the level, through the merchant container). */
    private static ItemStack sell(Villager v, MerchantOffer buy, ItemStack fish) {
        v.setOffers(new MerchantOffers());
        v.getOffers().add(buy);
        MerchantContainer c = new MerchantContainer(v);
        c.setItem(0, fish.copy());
        return c.getItem(2);
    }

    /** The price by the genes: in the trade slot, by the table, changed by a datapack table. */
    @GameTest(template = EMPTY, batch = "bettacraft_m47_prices", timeoutTicks = 100)
    public static void pricesByGenes(GameTestHelper h) {
        Villager v = aquarist(h, new BlockPos(3, 1, 3), 2);
        var trades = VillagerTrades.TRADES.get(ModVillagers.AQUARIST.get());
        MerchantOffer bucketOffer = trades.get(1)[1].getOffer(v, h.getLevel().getRandom()), bagOffer = trades.get(2)[1].getOffer(v, h.getLevel().getRandom());
        try {
            ItemStack plain = betta(BettaGenome.DEFAULT, 0, "adult");
            BettaGenome fancy = new BettaGenome(BettaGenome.SPECIES_BETTA, "halfmoon", BettaGenome.LONG, "dumbo", "white", "red", "dalmatian",
                    "black", "high", "turquoise");
            ItemStack bred = betta(fancy, 4, "adult");
            ItemStack fry = betta(fancy, 4, LifeStage.JUVENILE.id());
            int cheap = AquaristPrices.price(plain), dear = AquaristPrices.price(bred);
            check(cheap >= 2 && cheap <= 5, "a wild-type adult: " + cheap);
            check(dear >= cheap + 4, "a bred long-finned dumbo dalmatian (generation 4): " + dear);
            check(AquaristPrices.price(fry) == 0, "a juvenile is not bought");
            ItemStack r = sell(v, bucketOffer, plain);
            check(r.is(Items.EMERALD) && r.getCount() == cheap && !AquaristTrades.isAppraisal(r), "the trade slot pays " + r);
            r = sell(v, bucketOffer, bred);
            check(r.getCount() == dear, "the trade slot pays for the bred betta " + r);
            check(sell(v, bucketOffer, fry).isEmpty(), "nothing for a juvenile");
            // tankmates: wild-caught refused, a bred wild-type little, a bred Black ram more; a bag of a betta works too
            TankmateSpecies ram = TankmateSpecies.RAM_CICHLID;
            check(AquaristPrices.price(tankmate(ram, "wild", "wild", 0)) == 0, "a wild-caught ram is refused");
            int wildBred = AquaristPrices.price(tankmate(ram, "wild", "black", 1)), black = AquaristPrices.price(tankmate(ram, "black", "black", 1));
            check(wildBred == 1 && black == 4, "bred rams: wild-type " + wildBred + ", black " + black);
            check(sell(v, bagOffer, tankmate(ram, "black", "black", 1)).getCount() == black, "the bag pays for the ram");
            check(sell(v, bagOffer, tankmate(ram, "wild", "wild", 0)).isEmpty(), "nothing for a wild-caught ram");
            check(sell(v, bagOffer, new ItemStack(ModItems.FISH_BAG.get())).isEmpty(), "nothing for an empty bag");
            // a modpack's table
            var t = new java.util.LinkedHashMap<>(AquaristPrices.DEFAULTS);
            t.put("betta.base", 5.0);
            t.put("tankmate.only_bred", 0.0);
            AquaristPrices.setForTest(t);
            check(AquaristPrices.price(plain) == cheap + 3, "the changed table: " + AquaristPrices.price(plain));
            check(AquaristPrices.price(tankmate(ram, "wild", "wild", 0)) == 1, "wild-caught rams bought with only_bred false");
            var parsed = AquaristPrices.parse(com.google.gson.JsonParser.parseString("{\"betta\":{\"max\":3},\"tankmate\":{\"only_bred\":false}}").getAsJsonObject(),
                    AquaristPrices.DEFAULTS);
            check(parsed.get("betta.max") == 3.0 && parsed.get("tankmate.only_bred") == 0.0 && parsed.get("betta.base") == 2.0, "the file parser");
        } finally {
            AquaristPrices.setForTest(null);
        }
        v.discard();
        h.succeed();
    }

    /** With the switch off: no offers, the profession takes no basin, an aquarist loses his job and his offers. */
    @GameTest(template = EMPTY, batch = "bettacraft_m47_aquarist_off", timeoutTicks = 300)
    public static void aquaristOffRemovesProfessionAndOffers(GameTestHelper h) {
        Villager v = aquarist(h, new BlockPos(3, 1, 3), 3);
        v.setNoAi(false);
        check(!v.getOffers().isEmpty(), "offers before");
        Features.set(Feature.AQUARIST_VILLAGER, false);
        var poi = PoiTypes.forState(ModBlocks.BREEDING_BASIN.get().defaultBlockState());
        List<String> problems = new ArrayList<>();
        var trades = VillagerTrades.TRADES.get(ModVillagers.AQUARIST.get());
        for (int level = 1; level <= 5; level++) for (var l : trades.get(level)) if (l.getOffer(v, h.getLevel().getRandom()) != null) problems.add("level " + level);
        if (poi.isPresent() && ModVillagers.AQUARIST.get().acquirableJobSite().test(poi.get())) problems.add("the basin is taken");
        h.runAfterDelay(210, () -> {
            try {
                check(problems.isEmpty(), "with the switch off: " + problems);
                check(v.getVillagerData().getProfession() == VillagerProfession.NONE && v.getOffers().isEmpty(),
                        "the aquarist keeps " + v.getVillagerData().getProfession() + " with " + v.getOffers().size() + " offers");
            } finally {
                Features.set(Feature.AQUARIST_VILLAGER, true);
            }
            v.discard();
            h.succeed();
        });
    }
}
