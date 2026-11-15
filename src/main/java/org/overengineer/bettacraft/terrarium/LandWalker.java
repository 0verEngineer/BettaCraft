package org.overengineer.bettacraft.terrarium;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.critters.TankTerrain;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.interior.InteriorPaths;

/**
 * M31: the movement base of the terrarium animals in a tank (server, one per animal). <b>Kinematic</b> on purpose: the
 * animal has no gravity and no vanilla physics in its tank; every tick this moves it a step towards its goal and puts
 * its feet on the ground of the miniature ({@link TankTerrain}, the interior height map mapped into the tank), so it
 * walks on the land built in the interior, climbs slopes and steps (at most {@link Gait#maxStep}), walks or swims in the
 * water and climbs out of it onto the land; long ways go around the decoration on the walking grid of
 * {@link InteriorPaths#walk}. The goals: wander on the land, go into the water (by {@link Gait#waterTime}), bask on a basking
 * spot under a lit lamp ({@link TankClimate#baskingSpots}, by {@link Gait#baskTime} and more when the air is cold) and rest.
 * Simple and robust: no vanilla path finding in the tiny tank, nothing to fall through, works the same without an
 * interior (then the floor is the glass and there is no land).
 */
public final class LandWalker {
    /** What the animal does now. */
    public enum Activity { REST, LAND, WATER, BASK }

    /**
     * How a species moves.
     *
     * @param walk      blocks per tick on the land
     * @param swim      blocks per tick in the water (0 = it does not go into the water)
     * @param bottom    walks on the bottom of the water instead of swimming in the open water (musk turtles)
     * @param maxStep   highest step it climbs (tank blocks)
     * @param waterTime share of its goals in the water
     * @param baskTime  share of its goals on a basking spot (doubled while the air is below its range)
     * @param restTime  share of rests
     * @param height    height of its body (for swimming just under the surface)
     */
    public record Gait(double walk, double swim, boolean bottom, double maxStep, double waterTime, double baskTime, double restTime, double height) {}

    private Activity activity = Activity.REST;
    @Nullable
    private Vec3 goal;
    private int goalTicks;
    @Nullable
    private List<Vec3> path;
    private int pathIndex;
    private int stuck;
    @Nullable
    private TankTerrain terrain;
    private long terrainTime = Long.MIN_VALUE;
    private List<Vec3> spots = List.of();
    /** Ticks it spent on a basking spot (GameTests, care). */
    private int baskTicks;

    public Activity activity() {
        return activity;
    }

    public int baskTicks() {
        return baskTicks;
    }

    /** On its basking spot now. */
    public boolean basking() {
        return activity == Activity.BASK && atGoal;
    }

    private boolean atGoal;

    /** Forget the goal (a new one next tick). */
    public void reset() {
        goal = null;
        path = null;
        goalTicks = 0;
    }

    /** GameTests: go to this activity now. */
    public void forceActivity(Activity a) {
        activity = a;
        goal = null;
        path = null;
        goalTicks = 0;
        forced = a;
    }

    @Nullable
    private Activity forced;

    /** The terrain of the tank (rebuilt every second). */
    public TankTerrain terrain(ServerLevel level, TankData t) {
        long now = level.getGameTime();
        if (terrain == null || now - terrainTime >= 20 || now < terrainTime) {
            terrain = new TankTerrain(t, level.getServer(), org.overengineer.bettacraft.tank.interior.InteriorRegions.get(level.getServer()).get(t.regionId()),
                    level.isDay());
            terrainTime = now;
            spots = TankClimate.baskingSpots(level, t);
        }
        return terrain;
    }

    /** One tick in the tank. {@code cold}: the air is below the animal's range (it seeks the basking spot). */
    public void tick(Mob mob, ServerLevel level, TankData t, Gait gait, boolean cold) {
        TankTerrain ter = terrain(level, t);
        double surface = ter.surface();
        boolean water = Double.isFinite(surface);
        Vec3 pos = mob.position();
        RandomSource r = mob.getRandom();
        if (goal == null || --goalTicks <= 0) pick(mob, level, t, ter, gait, cold, r);
        if (goal == null) return;
        Vec3 way = goal;
        if (path != null && pathIndex < path.size()) way = path.get(pathIndex);
        double dx = way.x - pos.x, dz = way.z - pos.z;
        double flat = Math.sqrt(dx * dx + dz * dz);
        boolean inWater = water && pos.y + gait.height() * 0.5 < surface;
        double speed = inWater && gait.swim() > 0 ? gait.swim() : gait.walk();
        if (activity == Activity.REST) speed = 0;
        atGoal = false;
        if (flat < 0.05) {
            if (path != null && pathIndex < path.size() - 1) {
                pathIndex++;
            } else {
                atGoal = true;
                if (activity == Activity.BASK) baskTicks++;
            }
            speed = 0;
        }
        double nx = pos.x, nz = pos.z;
        if (speed > 0 && flat > 1e-6) {
            double step = Math.min(speed, flat);
            nx += dx / flat * step;
            nz += dz / flat * step;
        }
        double ny = pos.y;
        double g = ter.groundAt(nx, pos.y + gait.maxStep() + 0.05, nz);
        if (!Double.isFinite(g) || !t.contains(BlockPos.containing(nx, Math.max(t.min().getY(), g + 0.05), nz))) {
            blocked();
            g = ter.groundAt(pos.x, pos.y + 0.1, pos.z);
            nx = pos.x;
            nz = pos.z;
            if (!Double.isFinite(g)) return;
        }
        boolean deep = water && g < surface - gait.height();
        boolean wantSurface = goal.y > surface - 0.05;
        if (deep && gait.swim() > 0 && !gait.bottom()) {
            // open water: towards the goal's height, just under the surface when the goal is on the land
            double want = wantSurface ? surface - gait.height() * 0.6 : Math.max(g, Math.min(goal.y, surface - gait.height()));
            ny = pos.y + Mth.clamp(want - pos.y, -0.04, 0.04);
            ny = Math.max(ny, g);
        } else if (deep && gait.swim() > 0 && wantSurface && path == null) {
            // a bottom walker without a way: it swims up and climbs out where the land meets the water
            ny = pos.y + Mth.clamp(surface - gait.height() * 0.6 - pos.y, -0.04, 0.04);
            ny = Math.max(ny, g);
        } else {
            double rise = g - pos.y;
            if (rise > gait.maxStep() && !(water && pos.y + gait.height() >= surface - 0.05 && g <= surface + gait.maxStep())) {
                blocked();
                return;
            }
            ny = pos.y + Mth.clamp(rise, -0.15, 0.08);
        }
        if (Math.abs(nx - pos.x) + Math.abs(ny - pos.y) + Math.abs(nz - pos.z) > 1e-5) {
            mob.setPos(nx, ny, nz);
            if (Math.abs(nx - pos.x) + Math.abs(nz - pos.z) > 1e-4) {
                float yaw = (float) (Mth.atan2(nz - pos.z, nx - pos.x) * 180.0 / Math.PI) - 90F;
                mob.setYRot(Mth.rotLerp(0.25F, mob.getYRot(), yaw));
                mob.yBodyRot = mob.getYRot();
                mob.yHeadRot = mob.getYRot();
            }
            stuck = 0;
        }
    }

    private void blocked() {
        if (++stuck > 10) {
            goal = null;
            path = null;
            stuck = 0;
        } else {
            path = null; // try straight / another way
        }
    }

    private void pick(Mob mob, ServerLevel level, TankData t, TankTerrain ter, Gait gait, boolean cold, RandomSource r) {
        double surface = ter.surface();
        boolean water = Double.isFinite(surface);
        Activity a;
        if (forced != null) {
            a = forced;
        } else {
            double bask = spots.isEmpty() ? 0 : gait.baskTime() * (cold ? 2 : 1);
            double wet = water && gait.swim() > 0 ? gait.waterTime() : 0;
            double rest = gait.restTime();
            double land = Math.max(0.05, 1 - bask - wet - rest);
            double x = r.nextDouble() * (bask + wet + rest + land);
            a = x < bask ? Activity.BASK : x < bask + wet ? Activity.WATER : x < bask + wet + rest ? Activity.REST : Activity.LAND;
        }
        Vec3 target = switch (a) {
            case BASK -> spots.isEmpty() ? null : spots.get(r.nextInt(spots.size())).add((r.nextDouble() - 0.5) * 0.2, 0, (r.nextDouble() - 0.5) * 0.2);
            case WATER -> spot(t, ter, r, false, gait);
            case LAND -> spot(t, ter, r, true, gait);
            case REST -> mob.position();
        };
        if (target == null && a != Activity.LAND) {
            a = Activity.LAND;
            target = spot(t, ter, r, true, gait);
        }
        if (target == null) {
            a = Activity.REST;
            target = mob.position();
        }
        activity = a;
        goal = target;
        goalTicks = switch (a) {
            case REST -> 60 + r.nextInt(120);
            case BASK -> 400 + r.nextInt(600);
            default -> 200 + r.nextInt(200);
        };
        path = null;
        pathIndex = 0;
        if (a != Activity.REST) {
            List<Vec3> way = InteriorPaths.walk(level.getServer(), t, mob.position(), target, Double.NaN);
            if (way != null && !way.isEmpty()) path = way;
        }
    }

    /** A random spot on the land ({@code land}) or in the water. */
    @Nullable
    private static Vec3 spot(TankData t, TankTerrain ter, RandomSource r, boolean land, Gait gait) {
        double surface = ter.surface();
        double m = 0.25;
        for (int i = 0; i < 24; i++) {
            double x = t.min().getX() + m + r.nextDouble() * Math.max(0.01, t.sizeX() - 2 * m);
            double z = t.min().getZ() + m + r.nextDouble() * Math.max(0.01, t.sizeZ() - 2 * m);
            double g = ter.ground(x, z);
            if (!Double.isFinite(g) || !t.contains(BlockPos.containing(x, g + 0.05, z))) continue;
            boolean dry = !Double.isFinite(surface) || g > surface + 0.01;
            if (dry != land) continue;
            if (!land && g > surface - gait.height()) continue; // too shallow to swim
            double y = land || gait.bottom() ? g : Math.min(surface - gait.height(), g + r.nextDouble() * Math.max(0, surface - gait.height() - g));
            return new Vec3(x, y, z);
        }
        return null;
    }
}
