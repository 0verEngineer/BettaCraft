package org.overengineer.bettacraft.tankmates;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;

/**
 * M29 part b: the puffers and the fin nippers.
 * <ul>
 *     <li><b>Teeth</b>: the beak-like teeth of a puffer never stop growing; cracking snails ({@link TankmateBreeding} lets them
 *     eat the M14 snails) or hard food ({@code #bettacraft:hard_food}, the shell food) wears them down. After
 *     {@code tankmates.behaviour.pufferTeethDays} days without either they have overgrown: the puffer is stressed and refuses
 *     soft food, so it starves unless it gets a snail or hard food again.</li>
 *     <li><b>Fin nipping</b>: puffers and tiger barbs nip the long fins of bettas, angelfish, gouramis and male guppies
 *     ({@code tankmates.behaviour.finNipping}); tiger barbs in a too small shoal nip twice as hard.</li>
 * </ul>
 */
public final class Puffers {
    private Puffers() {}

    /** Days without snails or hard food until the teeth have overgrown (0 = off). */
    public static double teethDays() {
        return ServerConfig.getOr(ServerConfig.TANKMATES_PUFFER_TEETH_DAYS, 8.0);
    }

    public static boolean overgrown(TankmateEntity m) {
        double limit = teethDays();
        return m.species().puffer() && limit > 0 && m.teethDays() >= limit;
    }

    public static boolean hardFood(ItemStack food) {
        return !food.isEmpty() && food.is(ModTags.HARD_FOOD);
    }

    /**
     * Fin nippers go for every long-finned fish in the tank (the bettas too): it flees, counts as harassed and loses hit
     * points ({@code tankmates.behaviour.finNipping}, more with more nippers, at most twice).
     */
    static void finNipping(ServerLevel level, TankBlockEntity c, List<BettaEntity> bettas, List<TankmateEntity> mates, double days, long now) {
        for (TankmateEntity m : mates) m.setNipped(false);
        double rate = ServerConfig.getOr(ServerConfig.TANKMATES_FIN_NIPPING, 0.6);
        List<TankmateEntity> nippers = new ArrayList<>();
        double strength = 0;
        for (TankmateEntity m : mates) {
            if (!m.species().finNipper() || m.lifeStage().ordinal() < LifeStage.SUBADULT.ordinal()) continue;
            nippers.add(m);
            // puffers nip hard; a tiger barb a little, much more in a shoal that is too small
            double w = m.species().puffer() ? 1.0 : 0.3;
            if ((m.stressBits() & org.overengineer.bettacraft.entity.tankmate.TankmateCare.STRESS_SHOAL) != 0) w *= 2;
            strength += w;
        }
        String key = "fin_nipping";
        if (nippers.isEmpty() || rate <= 0) {
            c.tankmateLogged.remove(key);
            return;
        }
        double dmg = rate * Math.min(2.0, strength) * days;
        Entity victim = null;
        TankmateEntity by = null;
        for (TankmateEntity m : mates) {
            if (m.species().finNipper() || !m.species().longFins(m.isFemale()) || m.lifeStage() == LifeStage.FRY) continue;
            TankmateEntity n = nearest(m, nippers);
            if (n == null) continue;
            m.setNipped(true);
            m.flee(n, now + TankmateBreeding.HOLD);
            m.addDamage(dmg, n);
            if (victim == null) {
                victim = m;
                by = n;
            }
        }
        for (BettaEntity b : bettas) {
            if (b.lifeStage() == LifeStage.FRY) continue;
            TankmateEntity n = nearest(b, nippers);
            if (n == null) continue;
            b.flee(n, now + TankmateBreeding.HOLD);
            b.addDamage(dmg, n);
            if (victim == null) {
                victim = b;
                by = n;
            }
        }
        if (victim == null) {
            c.tankmateLogged.remove(key);
        } else if (c.tankmateLogged.add(key)) {
            TankCare.log(level, c, "log.bettacraft.tm.fin_nipping", TankCare.nameArg(by), TankCare.nameArg(victim));
        }
    }

    @Nullable
    private static TankmateEntity nearest(Entity from, List<TankmateEntity> list) {
        TankmateEntity best = null;
        double bd = Double.MAX_VALUE;
        for (TankmateEntity e : list) {
            if (e == from || !e.isAlive()) continue;
            double d = e.distanceToSqr(from);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }
}
