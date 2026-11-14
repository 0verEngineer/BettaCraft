package org.overengineer.bettacraft.entity.tankmate;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.entity.betta.BettaCare;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.BettaHealth;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWater;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M19: the needs of a tankmate in a Bettacraft tank, every second from the game time like {@link BettaCare}: the health
 * timers (the bettas' rules: hunger, temperature outside the species' range, water below its quality, and stress -
 * overcrowding, a shoal that is too small, being chased), growth through the stages (fed, tolerable temperature, not
 * sick), the breeding timers and healing. Grazers (otocinclus, bristlenose) also live on the algae on the glass.
 */
public final class TankmateCare {
    private static final int INTERVAL = 20;
    /** Stress bits (menu). */
    public static final int STRESS_SHOAL = 1, STRESS_CHASED = 2, STRESS_TEMP = 4, STRESS_WATER = 8, STRESS_CROWDED = 16;
    /** M21.8 seahorses: the water moves too much, no hitching post. */
    public static final int STRESS_FLOW = 32, STRESS_NO_POSTS = 64;
    /** M23: too little oxygen in the water. */
    public static final int STRESS_OXYGEN = 128;
    /** M24: much nitrate (they breed less; only from the lethal level it is bad water). */
    public static final int STRESS_NITRATE = 256;
    /** M25: pH or TDS out of the species' range ({@link org.overengineer.bettacraft.chem.ChemTolerance}). */
    public static final int STRESS_CHEMISTRY = 512;
    /** Algae on the glass (mean share) from which grazers find enough to eat. */
    public static final double GRAZING_FROM = 0.04;

    private TankmateCare() {}

    static void serverTick(ServerLevel level, TankmateEntity fish) {
        if (fish.simStarted && (fish.tickCount + fish.getId()) % INTERVAL != 0) return;
        long now = level.getGameTime();
        TankBlockEntity controller = TankInhabitants.resolveController(level, fish);
        if (controller == null) {
            fish.tankWater = null;
            fish.lastSimTime = now;
            fish.simStarted = true;
            fish.careFlags = fish.health.isSick() ? TankmateEntity.FLAG_SICK : 0;
            return;
        }
        TankData tank = controller.getTank();
        fish.tankWater = new TankWaterVolume(tank);
        if (!fish.simStarted) {
            fish.simStarted = true;
            if (fish.lastSimTime < 0 || !ServerConfig.getOr(ServerConfig.SIMULATE_UNLOADED_TIME, false)) {
                fish.lastSimTime = now;
                return;
            }
        }
        if (fish.lastSimTime < 0) fish.lastSimTime = now;
        double days = Math.min(5.0, (now - fish.lastSimTime) / TankmateEntity.TICKS_PER_DAY);
        fish.lastSimTime = now;
        if (days > 0) advance(level, controller, fish, days);
    }

    /** Advances the needs of a tankmate by {@code days} (also used by GameTests). */
    public static void advance(ServerLevel level, TankBlockEntity controller, TankmateEntity fish, double days) {
        TankData tank = controller.getTank();
        TankmateSpecies s = fish.species();
        BettaHealth.Params p = BettaCare.params();
        if (s.seahorse()) p = org.overengineer.bettacraft.tankmates.Seahorses.params(p); // M21.8: they need food more often
        boolean tempOk = s.tolerates(tank.waterTemp());
        double salt = org.overengineer.bettacraft.reef.SaltTolerance.shock(s.salinity(), tank); // M20
        boolean waterBad = TankWater.qualityEnabled() && tank.waterQuality() < s.minQuality() || salt >= org.overengineer.bettacraft.tank.Salinity.STRESS_FROM;
        // M25: pH and TDS: a little out of the range only stresses, far out the water is bad for the species
        var chem = org.overengineer.bettacraft.chem.TankChemistry.fit(tank, org.overengineer.bettacraft.chem.ChemTolerance.tankmate(s));
        int stress = fish.stress & (STRESS_SHOAL | STRESS_CHASED);
        boolean crowded = controller.isCrowded() && fish.isAdult();
        if (crowded) stress |= STRESS_CROWDED;
        if (!tempOk) stress |= STRESS_TEMP;
        if (waterBad) stress |= STRESS_WATER;
        if (chem != org.overengineer.bettacraft.chem.ChemTolerance.Fit.OK) stress |= STRESS_CHEMISTRY;
        // M23: low oxygen (the dwarf gourami breathes air like the bettas)
        boolean lowO2 = org.overengineer.bettacraft.tank.TankAeration.lowFor(tank, org.overengineer.bettacraft.tank.TankAeration.labyrinth(fish));
        if (lowO2) stress |= STRESS_OXYGEN;
        if (org.overengineer.bettacraft.tank.TankNitrate.high(tank)) stress |= STRESS_NITRATE;
        if (s.seahorse()) {
            // M21.8: strong flow, nothing to hold on to
            if (org.overengineer.bettacraft.tankmates.Seahorses.flowTooStrong(org.overengineer.bettacraft.tankmates.Seahorses.flowAt(level, controller, fish.getX(), fish.getZ()))) stress |= STRESS_FLOW;
            if (ServerConfig.getOr(ServerConfig.SEAHORSE_NEED_POSTS, true) && !org.overengineer.bettacraft.tankmates.Seahorses.hasPosts(level, tank)) stress |= STRESS_NO_POSTS;
        }
        fish.stress = stress;
        boolean social = ServerConfig.getOr(ServerConfig.TANKMATES_SHOAL_STRESS, true) && (stress & STRESS_SHOAL) != 0;
        BettaHealth.Conditions c = new BettaHealth.Conditions(!tempOk, crowded || social || (stress & (STRESS_CHASED | STRESS_FLOW | STRESS_NO_POSTS)) != 0, waterBad || lowO2 || chem == org.overengineer.bettacraft.chem.ChemTolerance.Fit.BAD, salt);
        // grazers find their food on the glass
        if (s.grazer() && fish.health.daysSinceFed() > 0.3 && org.overengineer.bettacraft.tank.algae.TankAlgae.mean(tank) >= GRAZING_FROM) {
            fish.health.feed();
        }
        boolean wasSick = fish.health.isSick();
        BettaHealth.Event event = fish.health.step(days, c, p);
        switch (event) {
            case GOT_SICK -> TankCare.log(level, controller, "log.bettacraft.sick", TankCare.nameArg(fish));
            case RECOVERED -> TankCare.log(level, controller, "log.bettacraft.recovered", TankCare.nameArg(fish));
            case DIED -> {
                dieOfIllness(level, controller, fish, c, p);
                return;
            }
            default -> {}
        }
        fish.breeding.tick(days);
        if (fish.gravidDays >= 0) fish.gravidDays += days;
        boolean fed = !fish.health.isHungry(p);
        if (fish.stage != LifeStage.ADULT && fed && tempOk && !fish.health.isSick()) grow(level, controller, fish, days);
        double heal = ServerConfig.getOr(ServerConfig.HEALTH_HEAL_PER_DAY, 2.0);
        if (org.overengineer.bettacraft.reef.ReefCare.cleaners(controller.getTank())) heal *= 1.5; // M20: a cleaner shrimp picks the wounds clean
        if (fed && !fish.health.isSick() && fish.getHealth() < fish.getMaxHealth() && heal > 0) fish.heal((float) (heal * days));
        int flags = 0;
        if (fish.health.isSick()) flags |= TankmateEntity.FLAG_SICK;
        if (fish.gravidDays >= 0) flags |= TankmateEntity.FLAG_GRAVID;
        if (fish.guarding) flags |= TankmateEntity.FLAG_GUARD;
        if (stress != 0) flags |= TankmateEntity.FLAG_STRESS;
        fish.careFlags = flags;
        if (wasSick != fish.health.isSick()) controller.setChanged();
    }

    private static void grow(ServerLevel level, TankBlockEntity controller, TankmateEntity fish, double days) {
        fish.stageDays += days;
        double need = fish.stageDuration(fish.stage);
        while (fish.stage != LifeStage.ADULT && fish.stageDays >= need) {
            double rest = fish.stageDays - need;
            fish.setStage(fish.stage.next());
            fish.stageDays = rest;
            need = fish.stageDuration(fish.stage);
            if (fish.stage == LifeStage.ADULT) {
                TankCare.log(level, controller, "log.bettacraft.grown_up", TankCare.nameArg(fish));
                controller.markTankChanged();
                org.overengineer.bettacraft.progression.Progress.onTankmateGrownUp(level, controller, fish);
            }
        }
    }

    private static void dieOfIllness(ServerLevel level, TankBlockEntity controller, TankmateEntity fish, BettaHealth.Conditions c, BettaHealth.Params p) {
        String key = c.saltBad() ? "salinity" : fish.health.isHungry(p) ? "hunger" : c.tempBad() ? "temperature"
                : c.waterBad() && (fish.stress & STRESS_WATER) == 0 && (fish.stress & STRESS_OXYGEN) != 0 ? "oxygen"
                : c.waterBad() && (fish.stress & STRESS_WATER) == 0 && (fish.stress & STRESS_CHEMISTRY) != 0 ? "water_chemistry" : c.waterBad() ? "water_quality"
                : (fish.stress & STRESS_SHOAL) != 0 ? "lonely" : (fish.stress & STRESS_FLOW) != 0 ? "flow"
                : (fish.stress & STRESS_NO_POSTS) != 0 ? "no_posts" : c.crowdBad() ? "overcrowding" : "illness";

        TankCare.log(level, controller, "log.bettacraft.died", TankCare.nameArg(fish), "@cause.bettacraft." + key);
        controller.getTank().inhabitants().remove(fish.getUUID());
        controller.markTankChanged();
        fish.discard();
    }

    /** Death by damage (a betta, a rival, a predator): the tank log. */
    static void onDeath(ServerLevel level, TankmateEntity fish, DamageSource source) {
        TankBlockEntity controller = TankInhabitants.resolveController(level, fish);
        if (controller == null) return;
        Entity attacker = source.getEntity();
        if (attacker != null) {
            TankCare.log(level, controller, attacker instanceof BettaEntity ? "log.bettacraft.killed_by_betta" : "log.bettacraft.killed_predator",
                    TankCare.nameArg(fish), TankCare.nameArg(attacker));
        } else {
            TankCare.log(level, controller, "log.bettacraft.died", TankCare.nameArg(fish), "@cause.bettacraft.injuries");
        }
        controller.getTank().inhabitants().remove(fish.getUUID());
        controller.markTankChanged();
    }
}
