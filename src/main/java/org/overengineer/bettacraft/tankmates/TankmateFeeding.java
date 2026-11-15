package org.overengineer.bettacraft.tankmates;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.food.BettaFood;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;

/**
 * M19: the tankmates at feeding time (called from {@link TankCare#feed}): they eat the same food as the bettas by their
 * life stage (fry food for the fry), the hungriest first. Food sinks past the midwater fish: a bottom fish (corydoras,
 * pleco, otocinclus, loach) gets its share only with the chance {@code tankmates.bottomFoodShare} while faster fish are
 * there, unless the food is an algae wafer (#bettacraft:bottom_food), which only the bottom fish eat.
 */
public final class TankmateFeeding {
    private TankmateFeeding() {}

    public static boolean bottomFood(ItemStack stack) {
        return stack.is(ModTags.BOTTOM_FOOD);
    }

    public static boolean bottomFish(TankmateEntity m) {
        return m.species().zone().bottom() || m.species().model() == TankmateSpecies.Model.LOACH;
    }

    /** Tankmates that eat this kind of food (for an algae wafer only the bottom fish). */
    public static List<TankmateEntity> eaters(ServerLevel level, TankData t, @Nullable BettaFood.Kind kind, boolean bottomOnly) {
        List<TankmateEntity> out = new ArrayList<>();
        if (kind == null) return out;
        for (TankmateEntity m : TankmateBreeding.tankmates(level, t)) {
            if (bottomOnly && !bottomFish(m)) continue;
            // M21.8: seahorses take live food and artemia only (the fry artemia from birth)
            if (m.species().seahorse() ? Seahorses.canEat(m.lifeStage(), kind) : BettaFood.canEat(m.lifeStage(), m.stageProgress(), kind)) out.add(m);
        }
        return out;
    }

    /**
     * Feeds up to {@code portions} tankmates; {@code fastThere}: bettas or midwater fish compete (bottom fish only get
     * their share by chance). Returns the portions eaten.
     */
    public static int feed(ServerLevel level, TankBlockEntity c, List<TankmateEntity> eaters, BettaFood.Kind kind, int portions,
                           boolean fastThere, boolean wafer, @Nullable Set<UUID> round) {
        return feed(level, c, eaters, net.minecraft.world.item.ItemStack.EMPTY, kind, portions, fastThere, wafer, round);
    }

    /** As above with the food itself (M26: tubifex condition better, food of a poor culture stresses a little). */
    public static int feed(ServerLevel level, TankBlockEntity c, List<TankmateEntity> eaters, net.minecraft.world.item.ItemStack food, BettaFood.Kind kind,
                           int portions, boolean fastThere, boolean wafer, @Nullable Set<UUID> round) {
        if (portions <= 0 || eaters.isEmpty()) return 0;
        List<TankmateEntity> list = new ArrayList<>(eaters);
        if (round != null) list.removeIf(m -> round.contains(m.getUUID()));
        list.sort(Comparator.comparingDouble((TankmateEntity m) -> m.health().daysSinceFed()).reversed());
        boolean midwater = fastThere;
        for (TankmateEntity m : list) if (!bottomFish(m) && !m.species().seahorse()) midwater = true;
        double share = ServerConfig.getOr(ServerConfig.TANKMATES_BOTTOM_SHARE, 0.6);
        // M21.8: slow seahorses next to fast eaters (bettas, midwater fish)
        double seahorseShare = ServerConfig.getOr(ServerConfig.SEAHORSE_FAST_SHARE, 0.35);
        boolean sinks = !food.isEmpty() && org.overengineer.bettacraft.food.FoodDiet.sinks(food); // M26 part b: granules sink to them
        int used = 0, overfed = 0;
        for (TankmateEntity m : list) {
            if (used >= portions) break;
            if (!wafer && !sinks && bottomFish(m) && midwater && level.getRandom().nextDouble() >= share) continue;
            if (m.species().seahorse() && midwater && level.getRandom().nextDouble() >= seahorseShare) continue;
            // M29 part b: a puffer with overgrown teeth only takes hard food; hard food wears its teeth down
            boolean hard = Puffers.hardFood(food);
            if (Puffers.overgrown(m) && !hard) continue;
            if (hard && m.species().puffer()) m.wearTeeth();

            double overfedDays = m.species().seahorse() ? TankCare.OVERFED_DAYS / Math.max(1.0, ServerConfig.getOr(ServerConfig.SEAHORSE_FEEDINGS, 2.0))
                    : TankCare.OVERFED_DAYS; // M21.8: seahorses eat often
            if (m.health().daysSinceFed() < overfedDays) overfed++;

            m.health().feed();
            if (food.isEmpty()) {
                if (kind == BettaFood.Kind.LIVE && m.isAdult()) m.breeding().condition();
            } else {
                org.overengineer.bettacraft.food.LiveFood.eaten(food, m.health(), m.breeding(), m.isAdult(),
                        org.overengineer.bettacraft.food.FoodDiet.of(m.species())); // M26 part b: the diet of the species
            }
            if (round != null) round.add(m.getUUID());
            used++;
        }
        if (used > 0) TankCare.log(level, c, "log.bettacraft.tm.fed", String.valueOf(used));
        if (overfed > 0) org.overengineer.bettacraft.tank.TankWater.overfed(level, c, overfed);
        return used;
    }
}
