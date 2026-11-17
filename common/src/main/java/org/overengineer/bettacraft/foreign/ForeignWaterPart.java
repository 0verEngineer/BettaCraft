package org.overengineer.bettacraft.foreign;

import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.critters.TankTerrain;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.interior.InteriorRegions;
import org.overengineer.bettacraft.terrarium.PaludariumWater;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.TerrariumAnimals;

/**
 * M35 part b: the water animals of vanilla and of other mods (their own AI) stay in the <b>water part</b> of a paludarium.
 * The water of a tank is vanilla water in all its blocks below the surface, also under the land of the miniature, which the
 * AI of a mod cannot see; so every tick a fish that swam into the land (below the floor of the interior at its height) is put back where it last was in the open water, its way stopped and its swim
 * turned round (and into a spot of the water part if it never was in one). Our fish keep out of the land by the obstacles
 * of the interior themselves; the land animals go by our walker.
 */
public final class ForeignWaterPart {
    static final Map<Mob, State> STATES = new WeakHashMap<>();
    private static final Map<TankBlockEntity, Terrain> TERRAIN = new WeakHashMap<>();

    static final class State {
        @Nullable
        TankBlockEntity controller;
        int nextResolve;
        @Nullable
        Vec3 good;
        int putBack;
    }

    private record Terrain(TankTerrain terrain, long time) {}

    private ForeignWaterPart() {}


    /** Whether this rule moves the animal: a water animal that is not ours, not a terrarium animal, not a mob in glass. */
    public static boolean applies(Mob m) {
        if (BettaCraft.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getNamespace())) return false;
        if (TerrariumAnimals.isTerrariumAnimal(m.getType()) || ForeignTerrarium.held(m)) return false;
        if (org.overengineer.bettacraft.mobs.GlassMobs.isGlassMob(m)) return false;
        return TankInhabitants.isWaterAnimal(m);
    }

    /** Whether a spot is in the open water of the tank: above the floor at its height (not inside the land or the decoration). */
    public static boolean inWaterPart(TankTerrain ter, Vec3 p) {
        double surface = ter.surface();
        if (!Double.isFinite(surface)) return true;
        // the floor at its height: inside the land (a solid stack) that is the top of the land above it; under an overhang
        // or a floating bank the floor below (open water)
        double floor = ter.groundAt(p.x, p.y + 0.05, p.z);
        return !(Double.isFinite(floor) && floor > p.y + 0.05);
    }

    /** GameTests: how often this animal was put back into the water part. */
    public static int putBacks(Entity e) {
        State s = e instanceof Mob m ? STATES.get(m) : null;
        return s == null ? 0 : s.putBack;
    }

    static TankTerrain terrain(ServerLevel level, TankBlockEntity c) {
        long now = level.getGameTime();
        Terrain t = TERRAIN.get(c);
        if (t == null || now - t.time() >= 20 || now < t.time()) {
            TankData d = c.getTank();
            t = new Terrain(new TankTerrain(d, level.getServer(), InteriorRegions.get(level.getServer()).get(d.regionId()), level.isDay()), now);
            TERRAIN.put(c, t);
        }
        return t.terrain();
    }
}
