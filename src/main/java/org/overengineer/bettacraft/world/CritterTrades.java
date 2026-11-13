package org.overengineer.bettacraft.world;

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

/**
 * The wandering trader sells small animals in fish bags (M14): orange dwarf crayfish (a pair) and gold or blue apple
 * snails among his usual offers; a pair of Neocaridina of one colour line, pink or blue ramshorns, an assassin snail and
 * a dwarf clawed frog among the rare ones.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class CritterTrades {
    private CritterTrades() {}

    /** Colour lines of the Neocaridina the trader brings (loci of {@link CritterSpecies#NEOCARIDINA}). */
    private static final int[] SHRIMP_LINES = {0, 1, 2, 3, 4, 5};

    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        event.getGenericTrades().add(offer(5, 4, (r, rnd) -> TankCritters.bagOf(CritterSpecies.DWARF_CRAYFISH, 2,
                () -> CritterGenes.pure(0, 0.25F + rnd.nextFloat() * 0.25F), rnd, true)));
        event.getGenericTrades().add(offer(3, 4, (r, rnd) -> TankCritters.bagOf(CritterSpecies.APPLE_SNAIL, 1, () -> {
            // M21.5: mostly gold (as in the shops), the other colours now and then
            if (!org.overengineer.bettacraft.config.CommonConfig.snailColors()) return CritterGenes.pure(rnd.nextBoolean() ? 0 : 1, 0.3F);
            double x = rnd.nextDouble();
            return CritterGenes.pure(x < 0.7 ? 0 : x < 0.8 ? 1 : x < 0.9 ? 2 : 3, 0.3F + rnd.nextFloat() * 0.2F);
        }, rnd, true)));
        event.getRareTrades().add(offer(6, 3, (r, rnd) -> {
            int line = SHRIMP_LINES[rnd.nextInt(SHRIMP_LINES.length)];
            float grade = 0.15F + rnd.nextFloat() * 0.35F;
            return TankCritters.bagOf(CritterSpecies.NEOCARIDINA, 2, () -> CritterGenes.pure(line, grade), rnd, true);
        }));
        event.getRareTrades().add(offer(4, 3, (r, rnd) -> TankCritters.bagOf(CritterSpecies.RAMSHORN, 2,
                () -> CritterGenes.pure(rnd.nextInt(org.overengineer.bettacraft.config.CommonConfig.snailColors() ? 3 : 2), 0.3F), rnd, false)));
        // M21.5: racer nerites, red or yellow, every one its own shade
        event.getRareTrades().add((trader, random) -> {
            if (!org.overengineer.bettacraft.config.CommonConfig.snailColors()) return null;
            RandomGenerator rnd = random::nextLong;
            CritterSpecies s = random.nextBoolean() ? CritterSpecies.RED_NERITE : CritterSpecies.YELLOW_NERITE;
            return new MerchantOffer(new ItemCost(Items.EMERALD, 4), Optional.empty(), TankCritters.bagOf(s, 2, () -> CritterGenes.WILD, rnd, false), 3, 1, 0.05F);
        });
        event.getRareTrades().add(offer(4, 2, (r, rnd) -> TankCritters.bagOf(CritterSpecies.ASSASSIN_SNAIL, 1, () -> CritterGenes.WILD, rnd, false)));
        event.getRareTrades().add((trader, random) -> {
            var frog = org.overengineer.bettacraft.registry.ModEntities.DWARF_FROG.get().create(trader.level());
            if (frog == null) return null;
            ItemStack bag = org.overengineer.bettacraft.entity.betta.FishBagItem.fillWith(frog);
            frog.discard();
            return new MerchantOffer(new ItemCost(Items.EMERALD, 6), Optional.empty(), bag, 2, 1, 0.05F);
        });
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
}
