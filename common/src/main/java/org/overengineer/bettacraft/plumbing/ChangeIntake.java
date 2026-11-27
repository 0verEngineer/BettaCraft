package org.overengineer.bettacraft.plumbing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.plumbing.interior.TankAnchors;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;

/**
 * M68 part B: the waste water intake of the water change controller. Where the controller drains a tank itself (its waste water
 * line docks on the tank, or it touches the tank), that dock is a suction point like the intake of a multiblock filter: without an
 * intake guard (or the intake strainer) behind its connection in the interior a water change sucks in young animals (fish fry,
 * young shrimp, tadpoles; never snails, never adults), as many as the drained share of the water (a 25 % change takes about a
 * quarter of them). The fresh water line needs no guard.
 * <p>
 * The guard of a dock is looked up in {@link #guarded} only: today every face of a tank block has one connection in the interior
 * (its anchor, {@link TankAnchors}), shared by every line that docks there; part C gives every line of a bundle its own anchor and
 * switches this lookup to the anchor of the waste water line.
 */
public final class ChangeIntake {
    private ChangeIntake() {}

    /** Whether the dock {@code d} of tank {@code c} has an intake guard behind it (true when guards cannot be asked for at all). */
    public static boolean guarded(ServerLevel level, TankBlockEntity c, PipeNetwork.Dock d) {
        if (!TankAnchors.required(level)) return true;
        return TankAnchors.guardOf(level, c, d.member(), d.face(), true) == TankAnchors.Guard.OK;
    }

    /** The share (0..1) of the waste water docks on tank {@code c} without an intake guard. */
    public static double openShare(ServerLevel level, TankBlockEntity c, List<PipeNetwork.Dock> docks) {
        if (docks.isEmpty()) return 0;
        int open = 0;
        for (PipeNetwork.Dock d : docks) if (!guarded(level, c, d)) open++;
        return open / (double) docks.size();
    }

    /** Whether young animals can be sucked in at all ({@code features.breeding.fryIntake} and a care mode where animals die). */
    public static boolean active() {
        return Features.enabled(Feature.FRY_INTAKE) && org.overengineer.bettacraft.config.Care.canDie();
    }

    /** The young animals of a tank a suction point takes: fish fry, young shrimp and the tadpoles (as their count). */
    public static int young(ServerLevel level, TankData t) {
        int n = 0;
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) if (fry(e)) n++;
        for (var k : t.critters()) if (youngShrimp(k)) n++;
        return n + t.tadpoles().size();
    }

    private static boolean fry(Entity e) {
        return e instanceof BettaEntity b && b.lifeStage() == LifeStage.FRY || e instanceof TankmateEntity m && m.lifeStage() == LifeStage.FRY;
    }

    private static boolean youngShrimp(org.overengineer.bettacraft.critters.Critter k) {
        return k.alive() && !k.adult() && k.species.group() == org.overengineer.bettacraft.critters.CritterSpecies.Group.SHRIMP;
    }

    /**
     * A water change drained {@code drained} of the {@code before} mB of tank {@code c} through docks of which {@code open} (0..1) have
     * no guard: that share of the young is sucked in ({@code drained / before x open} of them; the fraction of one animal by chance).
     * Returns how many.
     */
    public static int suck(ServerLevel level, TankBlockEntity c, long drained, long before, double open) {
        if (!active() || open <= 0 || drained <= 0 || before <= 0) return 0;
        double p = Math.min(1, (double) drained / before) * Math.min(1, open);
        TankData t = c.getTank();
        List<Object> young = new ArrayList<>();
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) if (fry(e)) young.add(e);
        for (var k : t.critters()) if (youngShrimp(k)) young.add(k);
        young.addAll(t.tadpoles());
        if (young.isEmpty()) return 0;
        RandomSource rnd = level.getRandom();
        double exact = young.size() * p;
        int n = (int) Math.floor(exact);
        if (rnd.nextDouble() < exact - n) n++;
        n = Math.min(n, young.size());
        boolean critters = false;
        for (int i = 0; i < n; i++) {
            Object o = young.remove(rnd.nextInt(young.size()));
            if (o instanceof Entity e) {
                TankCare.log(level, c, "log.bettacraft.water_change_intake", TankCare.nameArg(e));
                org.overengineer.bettacraft.api.ApiEvents.died(level, c, e, "water_change_intake", null);
                t.inhabitants().remove(e.getUUID());
                e.discard();
            } else if (o instanceof org.overengineer.bettacraft.critters.Critter k) {
                TankCare.log(level, c, "log.bettacraft.water_change_intake", "@" + k.species.translationKey());
                t.critters().remove(k);
                critters = true;
            } else if (o instanceof org.overengineer.bettacraft.entity.frog.Tadpole k) {
                TankCare.log(level, c, "log.bettacraft.water_change_intake", "@log.bettacraft.fry_intake.tadpole");
                t.tadpoles().remove(k);
            }
        }
        if (n > 0) c.markTankChanged();
        if (critters) org.overengineer.bettacraft.critters.TankCritters.sync(level, c);
        return n;
    }
}
