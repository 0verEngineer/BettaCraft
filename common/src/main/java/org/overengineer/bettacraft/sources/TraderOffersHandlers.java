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
import org.overengineer.bettacraft.platform.event.SubscribeEvent;
import org.overengineer.bettacraft.platform.event.WandererTradesEvent;
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
import static org.overengineer.bettacraft.sources.TraderOffers.*;

/** M52: the game event handlers of {@link TraderOffers} (M52: on the common game events, fired by each loader). */
public final class TraderOffersHandlers {
    private TraderOffersHandlers() {}

    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        for (Offer o : all()) (o.slot() == Slot.USUAL ? event.getGenericTrades() : event.getRareTrades()).add(o.gatedListing());
    }
}
