package org.overengineer.bettacraft.world;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;

/**
 * The wandering trader sells aquarium plants (M13): Amazon sword, Vallisneria, Monte Carlo and Alternanthera among his
 * usual offers, the red tiger lotus and Cryptocoryne "Flamingo" among the rare ones.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class AquaticPlantTrades {
    private AquaticPlantTrades() {}

    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        event.getGenericTrades().add(new BasicItemListing(1, InteriorPlants.item(PlantSpecies.AMAZON_SWORD, 0), 6, 1));
        event.getGenericTrades().add(new BasicItemListing(1, InteriorPlants.item(PlantSpecies.VALLISNERIA, 0).copyWithCount(2), 8, 1));
        event.getGenericTrades().add(new BasicItemListing(1, InteriorPlants.item(PlantSpecies.MONTE_CARLO, 0).copyWithCount(2), 8, 1));
        event.getGenericTrades().add(new BasicItemListing(2, InteriorPlants.item(PlantSpecies.ALTERNANTHERA, 0), 6, 1));
        event.getRareTrades().add(new BasicItemListing(4, InteriorPlants.item(PlantSpecies.TIGER_LOTUS, 0), 3, 1));
        event.getRareTrades().add(new BasicItemListing(5, InteriorPlants.item(PlantSpecies.CRYPT_FLAMINGO, 0), 3, 1));
    }
}
