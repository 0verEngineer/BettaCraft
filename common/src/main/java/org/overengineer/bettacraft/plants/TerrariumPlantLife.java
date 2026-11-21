package org.overengineer.bettacraft.plants;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tankmates.TankmateSpots;
import org.overengineer.bettacraft.terrarium.HerpEntity;
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.overengineer.bettacraft.terrarium.TerrariumNeeds;

/**
 * M56: the terrarium plants and the animals of the tank (the switches below {@code features.terrariumPlants}):
 * <ul>
 *     <li><b>Grazing</b> ({@code tortoiseGrazing}): a tortoise that has not eaten for {@link #GRAZE_AFTER_DAYS} eats from an
 *     edible plant of its tank ({@link PlantSpecies#edible()}: the prickly pear, stonecrop, the grasses) that has at least two
 *     stages: the plant loses a stage, the tortoise is fed. So the plants replace a part of its food, as fast as they grow.</li>
 *     <li><b>Tadpoles in bromeliads</b> ({@code bromeliadTadpoles}): the grown bromeliads of a tank ({@link #cups}) take one dart frog
 *     tadpole each into their water cup ({@code HerpBreeding#hatch}); there it needs no open water and no food (the mother feeds
 *     it) and leaves as a froglet.</li>
 *     <li><b>Hiding</b> ({@code hiding}): snakes and geckos hide among the grown shelter plants ({@link #shelter}).</li>
 * </ul>
 */
public final class TerrariumPlantLife {
    /** Days without food after which a tortoise grazes (half a day: a plant stage instead of a meal). */
    public static final double GRAZE_AFTER_DAYS = 0.5;

    private TerrariumPlantLife() {}

    /** A plant is grown enough for a tadpole or a hiding animal from half its stages on. */
    public static boolean grown(InteriorPlant p) {
        return p.stage >= Math.max(2, (p.species.stages() + 1) / 2);
    }

    // ------------------------------------------------------------------ grazing

    /** A land tortoise (it grazes): a turtle of the dry terrarium that eats greens. */
    public static boolean grazer(Entity e) {
        return e instanceof HerpEntity h && h.species().turtle() && h.species().eats(org.overengineer.bettacraft.terrarium.TerrariumFood.Diet.GREENS)
                && h.species().needs().habitat() == TerrariumNeeds.Habitat.DRY;
    }

    /** One care step: every hungry tortoise grazes one edible plant (if any). Returns the plants grazed. */
    public static int graze(ServerLevel level, TankBlockEntity c) {
        if (!Features.enabled(Feature.TORTOISE_GRAZING) || !LandPlants.enabled()) return 0;
        TankData t = c.getTank();
        if (t.inhabitants().isEmpty()) return 0;
        InteriorRegion r = InteriorPlants.region(level.getServer(), t);
        ServerLevel interior = InteriorManager.interiorLevel(level.getServer());
        if (r == null || interior == null || r.plants().isEmpty()) return 0;
        int n = 0;
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            if (!grazer(e) || !(e instanceof HerpEntity h) || h.isBrumating() || h.daysSinceFed() < GRAZE_AFTER_DAYS) continue;
            InteriorPlant food = edible(r);
            if (food == null) break;
            food.target = food.stage - 1;
            food.progress = 0;
            InteriorPlants.apply(interior, r, food);
            h.feed();
            n++;
            if (c.logDue("graze:" + food.species.id(), level.getGameTime(), 24000)) {
                TankCare.log(level, c, "log.bettacraft.terrarium_plant.grazed", TankCare.nameArg(h), "@" + InteriorPlants.item(food.species, 0).getDescriptionId());
            }
        }
        if (n > 0) org.overengineer.bettacraft.tank.interior.InteriorRegions.get(level.getServer()).setDirty();
        return n;
    }

    /** The edible plant with the most stages (at least two), or null. */
    @Nullable
    static InteriorPlant edible(InteriorRegion r) {
        InteriorPlant best = null;
        for (InteriorPlant p : r.plants().all()) {
            if (!p.species.edible() || p.stage < 2 || p.target < p.stage) continue;
            if (best == null || p.stage > best.stage) best = p;
        }
        return best;
    }

    // ------------------------------------------------------------------ bromeliads

    /** The water cups of the grown bromeliads of a tank (tank world positions, the top of the cup), with or without a tadpole. */
    public static List<Vec3> cups(MinecraftServer server, TankData t) {
        List<Vec3> out = new ArrayList<>();
        InteriorRegion r = InteriorPlants.region(server, t);
        if (r == null) return out;
        for (InteriorPlant p : r.plants().all()) {
            if (!p.species.bromeliad() || !grown(p)) continue;
            out.add(TankmateSpots.toWorld(t, r, p.x + 0.5, p.y + 1.0, p.z + 0.5));
        }
        return out;
    }

    /** The cups without a tadpole in them now ({@code features.terrariumPlants.bromeliadTadpoles}; empty while it is off). */
    public static List<Vec3> freeCups(ServerLevel level, TankData t) {
        if (!Features.enabled(Feature.BROMELIAD_TADPOLES) || !LandPlants.enabled()) return List.of();
        List<Vec3> cups = cups(level.getServer(), t);
        if (cups.isEmpty()) return cups;
        for (Entity e : TankInhabitants.liveInhabitants(level, t)) {
            if (!(e instanceof HerpEntity h) || h.nursery() == null) continue;
            cups.removeIf(v -> v.distanceToSqr(h.nursery()) < 1e-4);
        }
        return cups;
    }

    // ------------------------------------------------------------------ hiding

    /** Snakes and geckos hide in the plants (with {@code features.terrariumPlants.hiding}). */
    public static boolean hides(Entity e) {
        return e instanceof HerpEntity h && (h.species().body() == HerpSpecies.Body.SNAKE || h.species().body() == HerpSpecies.Body.GECKO)
                && Features.enabled(Feature.PLANT_HIDING) && LandPlants.enabled();
    }

    /**
     * A spot at the foot of a grown shelter plant within {@code reach} (tank blocks, horizontally) of (x, z): x, ground y, z in
     * the tank, or null. Random among them (the generator of the caller).
     */
    @Nullable
    public static double[] shelter(MinecraftServer server, TankData t, double x, double z, double reach, java.util.random.RandomGenerator rnd) {
        InteriorRegion r = InteriorPlants.region(server, t);
        if (r == null) return null;
        List<double[]> spots = new ArrayList<>();
        for (InteriorPlant p : r.plants().all()) {
            if (!p.species.shelter() || !grown(p)) continue;
            Vec3 w = TankmateSpots.toWorld(t, r, p.x + 0.5, p.y, p.z + 0.5);
            if (Math.abs(w.x - x) > reach || Math.abs(w.z - z) > reach) continue;
            spots.add(new double[]{w.x, w.y, w.z});
        }
        if (spots.isEmpty()) return null;
        double[] s = spots.get(rnd.nextInt(spots.size()));
        // a little beside the stem, under the leaves
        double a = rnd.nextDouble() * Math.PI * 2, d = 0.04 + rnd.nextDouble() * 0.06;
        return new double[]{s[0] + Math.cos(a) * d, s[1], s[2] + Math.sin(a) * d};
    }
}
