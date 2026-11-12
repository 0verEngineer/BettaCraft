package org.overengineer.bettacraft.world;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.tank.plants.FloatingPlantType;
import org.overengineer.bettacraft.tank.plants.TankPlants;

/**
 * The wandering trader sells floating plants (M12): Amazon frogbit and salvinia among his usual offers, the red root
 * floater among the rare ones.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class FloatingPlantTrades {
    private FloatingPlantTrades() {}

    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        event.getGenericTrades().add(new BasicItemListing(1, TankPlants.item(FloatingPlantType.FROGBIT).copyWithCount(2), 8, 1));
        event.getGenericTrades().add(new BasicItemListing(1, TankPlants.item(FloatingPlantType.SALVINIA).copyWithCount(3), 8, 1));
        event.getRareTrades().add(new BasicItemListing(3, TankPlants.item(FloatingPlantType.RED_ROOT_FLOATER).copyWithCount(2), 4, 1));
    }
}
