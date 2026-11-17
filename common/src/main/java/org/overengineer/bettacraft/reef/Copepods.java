package org.overengineer.bettacraft.reef;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;

/**
 * M48 part b: the copepods of the live rock, the only food of the mandarin ({@link
 * org.overengineer.bettacraft.tankmates.TankmateSpecies#copepodEater()}). Live rock in salt water holds at most
 * {@code reef.copepodsPerLiveRock} portions per tank block of live rock; the copepods grow back by themselves (logistic, the
 * most at half the stock) and a few always come out of the rock. The other small reef fish pick some too (the mandarin needs a
 * quiet tank). A mandarin eats {@code reef.mandarinCopepodsPerDay}; each half-day ration it gets counts as a meal, so he stays
 * fed on half his need or more and starves (the bettas' hunger rules) below that. No rock, fresh water: they die off.
 */
public final class Copepods {
    /** Growth of the copepods per day at a low stock. */
    public static final double GROWTH = 0.8;
    /** Share of the capacity that comes out of the rock per day (they never die out while there is rock). */
    public static final double SEED = 0.03;
    /** Portions a small reef fish (not a mandarin) picks per day. */
    public static final double PICKED_BY_OTHERS = 0.1;
    /** Share of the capacity that stays hidden deep in the rock: the fish never get it, the stock always grows back from it. */
    public static final double REFUGE = 0.1;

    private Copepods() {}

    /** Most copepod portions the live rock of the tank holds (0 without live rock or in fresh water). */
    public static double capacity(ServerLevel level, TankData t) {
        return TankReef.liveRockTankBlocks(level, t) * ServerConfig.getOr(ServerConfig.REEF_COPEPODS_PER_LIVE_ROCK, 24.0);
    }

    public static double perDay() {
        return ServerConfig.getOr(ServerConfig.REEF_MANDARIN_COPEPODS, 1.0);
    }

    /** The copepod step of a tank: growth, the other fish picking, the mandarins eating. */
    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        if (days <= 0 || !Features.enabled(Feature.SALTWATER)) return;
        TankData t = c.getTank();
        double k = capacity(level, t);
        double p = t.copepods();
        if (k <= 0) {
            if (p > 0) t.setCopepods(p * Math.exp(-1.5 * days));
            return;
        }
        p = Math.max(0, Math.min(k, p + (GROWTH * p * (1 - p / k) + SEED * k) * days));
        if (Features.enabled(Feature.REEF_ANIMALS)) {
            List<TankmateEntity> mandarins = new ArrayList<>();
            double others = 0;
            for (TankmateEntity m : TankmateBreeding.tankmates(level, t)) {
                if (m.species().copepodEater()) mandarins.add(m);
                else if (m.species().marine() && !m.species().isLarge() && m.isAdult()) others += PICKED_BY_OTHERS;
            }
            if (!mandarins.isEmpty()) {
                double hidden = Math.min(p, REFUGE * k);
                double reach = p - hidden;
                reach = Math.max(0, reach - others * days);
                double demand = 0;
                for (TankmateEntity m : mandarins) demand += need(m) * days;
                double q = demand <= 0 ? 0 : Math.min(1, reach / demand);
                p = hidden + reach;
                for (TankmateEntity m : mandarins) {
                    double eat = need(m) * days * q;
                    p -= eat;
                    m.copepodMeal += eat;
                    double ration = need(m) * 0.5;
                    if (ration > 0 && m.copepodMeal >= ration) {
                        m.copepodMeal = Math.min(m.copepodMeal - ration, ration);
                        m.health().feed();
                        if (c.tankmateLogged.add("mandarin_fed")) {
                            org.overengineer.bettacraft.progression.Progress.awardNear(level, c,
                                    org.overengineer.bettacraft.progression.Progress.MANDARIN_COPEPODS, 0);
                        }
                    }
                }
                if (q < 0.5 && c.tankmateLogged.add("copepods_short")) TankCare.log(level, c, "log.bettacraft.reef.copepods_short");
                if (q >= 0.9) c.tankmateLogged.remove("copepods_short");
            }
        }
        t.setCopepods(Math.max(0, p));
        c.setChanged();
    }

    /** Portions a mandarin eats per day (a young one half). */
    public static double need(TankmateEntity m) {
        return perDay() * (m.lifeStage() == LifeStage.ADULT ? 1.0 : 0.5);
    }
}
