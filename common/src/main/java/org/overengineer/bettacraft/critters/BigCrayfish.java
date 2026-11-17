package org.overengineer.bettacraft.critters;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.plants.InteriorPlant;
import org.overengineer.bettacraft.plants.InteriorPlants;
import org.overengineer.bettacraft.plants.PlantSpecies;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M29 part d: what the big crayfish (Florida and marbled crayfish, {@link CritterSpecies#big()}) do to a tank besides
 * catching small animals ({@link CritterLife}): they eat plants back and dig young rooted plants out of the substrate
 * (epiphytes and moss on wood and stone are safe), eat fry, young fish and small bottom fish, and pinch bettas and other
 * slow fish near the floor. Plants and shelters hide the fish. Rates per adult big crayfish and day from the config
 * ({@code invertebrates.bigCrayfishFishPerDay}, {@code bigCrayfishPlantsPerDay}).
 */
public final class BigCrayfish {
    /** Damage of one pinch of the claws. */
    public static final double PINCH = 3.0;
    /** Ticks between two log lines of the same kind. */
    private static final long LOG_TICKS = 6000;

    private BigCrayfish() {}

    /** Adult big crayfish of a tank. */
    public static int adults(TankData t) {
        int n = 0;
        for (Critter k : t.critters()) if (k.alive() && k.adult() && k.species.big()) n++;
        return n;
    }

    public static void step(ServerLevel level, TankBlockEntity c, double days, RandomGenerator rnd) {
        if (days <= 0) return;
        int n = adults(c.getTank());
        if (n == 0) return;
        plants(level, c, n, days, rnd);
        fish(level, c, n, days, rnd);
    }

    static int events(double expected, RandomGenerator rnd) {
        if (expected <= 0) return 0;
        return (int) Math.floor(expected) + (rnd.nextDouble() < expected - Math.floor(expected) ? 1 : 0);
    }

    /** Whether a big crayfish can damage this plant (no epiphytes, moss or corals). */
    public static boolean edible(PlantSpecies s) {
        return !s.marine() && s.form().rooted();
    }

    // ------------------------------------------------------------------ plants

    /** Plants damaged in this step (eaten back by a stage or dug out). */
    public static int plants(ServerLevel level, TankBlockEntity c, int crayfish, double days, RandomGenerator rnd) {
        double rate = ServerConfig.getOr(ServerConfig.BIG_CRAYFISH_PLANTS, 0.3);
        int k = events(rate * crayfish * days, rnd);
        if (k == 0) return 0;
        InteriorRegion r = InteriorPlants.region(level.getServer(), c.getTank());
        if (r == null || r.plants().isEmpty()) return 0;
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        if (interior == null) return 0;
        int done = 0;
        long now = level.getGameTime();
        for (int i = 0; i < k; i++) {
            List<InteriorPlant> food = new ArrayList<>();
            for (InteriorPlant p : r.plants().all()) if (edible(p.species)) food.add(p);
            if (food.isEmpty()) break;
            InteriorPlant p = food.get(rnd.nextInt(food.size()));
            String name = "@" + InteriorPlants.item(p.species, 0).getDescriptionId();
            if (p.stage <= 1) {
                // a young plant does not hold in the substrate yet: dug out and gone
                InteriorPlants.remove(interior, r, p, false);
                if (c.logDue("big_crayfish_uprooted", now, LOG_TICKS)) TankCare.log(level, c, "log.bettacraft.big_crayfish_uprooted", name);
            } else {
                p.target = p.stage - 1;
                p.progress = 0;
                InteriorPlants.apply(interior, r, p);
                if (c.logDue("big_crayfish_plant", now, LOG_TICKS)) TankCare.log(level, c, "log.bettacraft.big_crayfish_plant", name);
            }
            done++;
        }
        if (done > 0) org.overengineer.bettacraft.tank.interior.InteriorRegions.get(level.getServer()).setDirty();
        return done;
    }

    // ------------------------------------------------------------------ fish

    /** How likely a big crayfish goes for this fish (0 = never); fry, young and small bottom fish are eaten, the rest pinched. */
    public static double fishWeight(Entity e) {
        if (e instanceof BettaEntity b) {
            return switch (b.lifeStage()) {
                case EGG, LARVA -> 0;
                case FRY -> 1.0;
                case JUVENILE -> 0.8;
                default -> 0.4; // long fins, rests near the floor at night
            };
        }
        if (e instanceof TankmateEntity m) {
            TankmateSpecies s = m.species();
            if (m.lifeStage().ordinal() < LifeStage.SUBADULT.ordinal()) return s.length() <= 0.6 ? 1.0 : 0.3;
            if (s.zone() == TankmateSpecies.Zone.BOTTOM || s.zone() == TankmateSpecies.Zone.GLASS) return s.length() <= 0.55 ? 0.5 : 0.1;
            return s.length() <= 0.3 ? 0.1 : 0.03;
        }
        return 0;
    }

    /** Whether a catch eats the fish (else the claws only pinch it). */
    public static boolean eaten(Entity e) {
        if (e instanceof BettaEntity b) return b.lifeStage().ordinal() < LifeStage.SUBADULT.ordinal();
        if (e instanceof TankmateEntity m) {
            if (m.lifeStage().ordinal() < LifeStage.SUBADULT.ordinal()) return m.species().length() <= 0.6;
            return m.species().length() <= 0.3;
        }
        return false;
    }

    /** Fish caught in this step (eaten or pinched). */
    public static int fish(ServerLevel level, TankBlockEntity c, int crayfish, double days, RandomGenerator rnd) {
        double rate = ServerConfig.getOr(ServerConfig.BIG_CRAYFISH_FISH, 0.15);
        if (rate <= 0) return 0;
        TankData t = c.getTank();
        double hide = TankCritters.hiding(level, t);
        int k = events(rate * crayfish * days * (1 - 0.6 * hide), rnd);
        if (k == 0) return 0;
        List<Entity> prey = new ArrayList<>(TankCare.bettas(level, t));
        prey.addAll(TankmateBreeding.tankmates(level, t));
        prey.removeIf(e -> fishWeight(e) <= 0);
        int done = 0;
        long now = level.getGameTime();
        for (int i = 0; i < k && !prey.isEmpty(); i++) {
            double total = 0;
            for (Entity e : prey) total += fishWeight(e);
            double x = rnd.nextDouble() * total;
            Entity victim = prey.getLast();
            for (Entity e : prey) {
                x -= fishWeight(e);
                if (x < 0) {
                    victim = e;
                    break;
                }
            }
            if (eaten(victim) && org.overengineer.bettacraft.config.Care.canDie()) { // M33 part c: NO_DEATH / DECOR only pinch
                prey.remove(victim);
                if (c.logDue("big_crayfish_ate", now, LOG_TICKS)) TankCare.log(level, c, "log.bettacraft.big_crayfish_ate", TankCare.nameArg(victim));
                level.sendParticles(ParticleTypes.BUBBLE_POP, victim.getX(), victim.getY(), victim.getZ(), 3, 0.05, 0.05, 0.05, 0.01);
                org.overengineer.bettacraft.api.ApiEvents.died(level, c, victim, "eaten", null); // M32 part d
                t.inhabitants().remove(victim.getUUID());
                victim.discard();
                c.markTankChanged();
            } else {
                if (victim instanceof BettaEntity b) b.addDamage(PINCH, null);
                else if (victim instanceof TankmateEntity m) m.addDamage(PINCH, null);
                if (c.logDue("big_crayfish_pinched", now, LOG_TICKS)) TankCare.log(level, c, "log.bettacraft.big_crayfish_pinched", TankCare.nameArg(victim));
            }
            done++;
        }
        return done;
    }
}
