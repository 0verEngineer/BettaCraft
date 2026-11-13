package org.overengineer.bettacraft.reef;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;

/**
 * M20: salt shock of the animals that have no health timers of ours: the dwarf clawed frogs and (with
 * {@code saltwater.foreignAnimals = REALISTIC}) the animals of vanilla and other mods. The shock is kept in the
 * entity's persistent data and fades in the right water; at 1 the animal dies (tank log: wrong salinity). Bettas,
 * tankmates and the small animals have it in their own care ({@code BettaHealth}, {@code CritterLife}).
 */
public final class ReefCare {
    static final String KEY = "bettacraft_salt_shock";

    private ReefCare() {}

    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        if (days <= 0) return;
        TankData t = c.getTank();
        double speed = SaltTolerance.speed();
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            if (e instanceof BettaEntity || e instanceof TankmateEntity || !(e instanceof LivingEntity living)) continue;
            Salinity.Range range = SaltTolerance.of(e);
            CompoundTag data = e.getPersistentData();
            double shock = data.getDouble(KEY);
            if (range == null) {
                if (shock > 0) data.remove(KEY);
                continue;
            }
            double s = Salinity.shockPerDay(range, t.salinity(), speed);
            shock = s > 0 ? shock + s * days : Math.max(0, shock - days * 2);
            if (shock >= 1) {
                data.remove(KEY);
                TankCare.log(level, c, "log.bettacraft.died", TankCare.nameArg(e), "@cause.bettacraft.salinity");
                t.inhabitants().remove(e.getUUID());
                c.markTankChanged();
                living.discard(); // like a betta that dies of illness: it simply vanishes
                continue;
            }
            if (shock > 0) data.putDouble(KEY, shock);
            else data.remove(KEY);
        }
    }

    /** Whether a living cleaner shrimp is in the tank (the fish heal half again as fast). */
    public static boolean cleaners(TankData t) {
        for (var k : t.critters()) if (k.alive() && k.species == org.overengineer.bettacraft.critters.CritterSpecies.CLEANER_SHRIMP) return true;
        return false;
    }

    /** Salt shock of a frog or foreign animal (0..1, menu and tests). */
    public static double shock(Entity e) {
        return e.getPersistentData().getDouble(KEY);
    }
}
