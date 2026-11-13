package org.overengineer.bettacraft.reef;

import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.world.TankmateTrades;

/**
 * M20: the wandering trader brings reef things: sea salt, a reef fish (clownfish pair, goby, chromis shoal, damsel) and a
 * soft coral frag or an anemone among his usual offers; a yellow tang, a cleaner shrimp or live rock among the rare ones.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class ReefTrades {
    private ReefTrades() {}

    private static final List<PlantSpecies> FRAGS = List.of(PlantSpecies.ZOANTHUS, PlantSpecies.MUSHROOM_CORAL, PlantSpecies.BUBBLE_TIP_ANEMONE,
            PlantSpecies.GREEN_STAR_POLYPS);

    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        event.getGenericTrades().add(fixed(2, 8, () -> new ItemStack(ModItems.SEA_SALT.get(), 8)));
        event.getGenericTrades().add(fish(TankmateSpecies.Trade.COMMON));
        event.getGenericTrades().add((trader, random) -> {
            RandomGenerator rnd = random::nextLong;
            ItemStack frag = InteriorPlants.item(FRAGS.get(rnd.nextInt(FRAGS.size())), 0);
            frag.setCount(2);
            return new MerchantOffer(new ItemCost(Items.EMERALD, 4), Optional.empty(), frag, 3, 1, 0.05F);
        });
        event.getRareTrades().add(fish(TankmateSpecies.Trade.RARE));
        event.getRareTrades().add((trader, random) -> {
            RandomGenerator rnd = random::nextLong;
            ItemStack bag = rnd.nextBoolean() ? TankCritters.bagOf(CritterSpecies.CLEANER_SHRIMP, 1, () -> CritterGenes.WILD, rnd, false)
                    : TankCritters.bagOf(CritterSpecies.HERMIT_CRAB, 3, () -> CritterGenes.WILD, rnd, false);
            return new MerchantOffer(new ItemCost(Items.EMERALD, 5), Optional.empty(), bag, 3, 1, 0.05F);
        });
        event.getRareTrades().add(fixed(6, 2, () -> new ItemStack(ModItems.LIVE_ROCK.get(), 8)));
    }

    /** A reef fish of this kind of offer (a pair, or a shoal for the chromis). */
    private static VillagerTrades.ItemListing fish(TankmateSpecies.Trade kind) {
        return (trader, random) -> {
            RandomGenerator rnd = random::nextLong;
            List<TankmateSpecies> pool = new java.util.ArrayList<>();
            for (TankmateSpecies s : TankmateSpecies.values()) if (s.trade() == kind && s.marine()) pool.add(s);
            if (pool.isEmpty()) return null;
            return TankmateTrades.offer(pool.get(rnd.nextInt(pool.size())), rnd);
        };
    }

    private static VillagerTrades.ItemListing fixed(int emeralds, int uses, java.util.function.Supplier<ItemStack> item) {
        return (trader, random) -> new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), Optional.empty(), item.get(), uses, 1, 0.05F);
    }
}
