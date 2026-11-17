package org.overengineer.bettacraft.terrarium;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.overengineer.bettacraft.config.ServerConfig;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;

/**
 * M31: the care step of the terrarium animals (every second, after the climate): every inhabitant with
 * {@link TerrariumNeeds} gets the assessment of the tank's conditions ({@link TankClimate#conditions}). An entity of ours
 * ({@link TerrariumAnimal}) decides itself what follows; any other registered animal loses one health point per
 * {@code terrarium.harmAfterDays} it bears a harmful problem (too cold, too hot, the wrong tank). New problems go into
 * the tank log once per animal kind and problem a day.
 */
public final class TerrariumCare {
    private TerrariumCare() {}

    public static void step(ServerLevel level, TankBlockEntity c, double days) {
        if (!TankClimate.enabled() || days <= 0) return;
        TankData t = c.getTank();
        if (t.inhabitants().isEmpty()) return;
        List<Entity> live = TankInhabitants.liveInhabitants(level, t);
        if (live.isEmpty()) return;
        TerrariumNeeds.Conditions cond = TankClimate.conditions(t);
        long now = level.getGameTime();
        for (Entity e : live) {
            TerrariumNeeds needs = TerrariumAnimals.needs(e);
            if (needs == null) continue;
            TerrariumNeeds.Assessment a = needs.assess(cond);
            if (e instanceof TerrariumAnimal ta) {
                ta.onTerrariumCare(level, c, a, days);
                a = ta.adjust(a); // M31 part b: brumation
            } else if (e instanceof LivingEntity le) {
                generic(le, a, days, c);
            }
            for (TerrariumNeeds.Problem p : a.problems()) {
                String kind = "terra:" + e.getType().getDescriptionId() + ":" + p.name();
                if (c.logDue(kind, now, 24000)) TankCare.log(level, c, "log.bettacraft.terrarium.problem", TankCare.nameArg(e), "@" + p.translationKey());
            }
        }
    }

    /** Harm of an animal without its own care: health loss after {@code terrarium.harmAfterDays} of a harmful problem. */
    private static void generic(LivingEntity e, TerrariumNeeds.Assessment a, double days, TankBlockEntity c) {
        UUID id = e.getUUID();
        double harm = c.terrariumHarm.getOrDefault(id, 0.0);
        harm = a.harmful() && org.overengineer.bettacraft.config.Care.needs() ? harm + days : Math.max(0, harm - days); // M33 part c: DECOR
        double after = ServerConfig.getOr(ServerConfig.TERRA_HARM_DAYS, 1.0);
        if (harm >= after && after >= 0) {
            harm -= Math.max(0.05, after);
            e.hurt(e.damageSources().generic(), 1.0F);
        }
        if (harm <= 0) c.terrariumHarm.remove(id);
        else c.terrariumHarm.put(id, harm);
    }
}
