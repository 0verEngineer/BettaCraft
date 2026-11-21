package org.overengineer.bettacraft.terrarium;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * <p>
 * M31 part c, the {@link Style}s of the other body plans:
 * <ul>
 *     <li>{@link Style#HOP} (frogs): on land it moves in hops (an arc, then a pause), in the water it swims;</li>
 *     <li>{@link Style#CLIMB} (the crested gecko): goes up the glass ({@link Activity#CLIMB}: walks to the foot of a wall,
 *     climbs it to a height, clings there, comes down again before it goes elsewhere), climbs straight up the faces of the
 *     decoration ({@link Gait#maxStep} high) and prefers the high spots of the land;</li>
 *     <li>{@link Style#SLITHER} (the snake): walks like {@link Style#WALK} (the model does the undulation);</li>
 *     <li>{@link Style#SWIM} (tadpoles): only in the water;</li>
 *     <li>M35 part b {@link Style#FLY} (dragonflies): flies straight through the air of the tank (over the land, the water
 *     and the decoration, never into them), hovers, skims the water surface ({@link Gait#waterTime}) and perches on the
 *     land to rest; {@link Gait#walk} is its flying speed, {@link Gait#height} its body height;</li>
 *     <li>{@link Gait#burrows} (the horned frog): a part ({@link Gait#special}) of its rests on the land it sits buried in
 *     the ground ({@link Pose#BURIED}).</li>
 * </ul>
 * {@link #pose()} tells the entity what to show (synced by {@link TerrariumAnimalEntity}).
 * <p>
 * M54 ({@link Habits}, switch {@code features.visuals.livelyMovement}): an animal of the mod with habits keeps its own hours
 * (more and longer rests while it is not active), goes at its own tempo with short fast phases ({@link #dashing}), climbs onto
 * the high spots of the decoration and the plants ({@link Activity#PERCH}: branches, rocks, leaves; up a face straight like the
 * climber) and hides under the decoration ({@link Activity#HIDE}: caves, overhangs, under leaves). Without habits (the animals
 * of other mods) nothing changes.
 */
public final class LandWalker {
    /** What the animal does now. */
    public enum Activity { REST, LAND, WATER, BASK, CLIMB,
        /** M54: up on a high spot of the decoration or the plants (a branch, a rock, a leaf) and sitting there */
        PERCH,
        /** M54: under the decoration (a cave, an overhang, leaves) and resting there */
        HIDE }

    /** How a body plan moves. */
    public enum Style { WALK, HOP, CLIMB, SLITHER, SWIM, FLY }

    /**
     * What the animal shows besides walking (synced as its ordinal): sitting buried in the ground, or clinging to a wall
     * whose glass lies to the north (-Z), east, south or west of it.
     */
    public enum Pose {
        NONE, BURIED, WALL_N, WALL_E, WALL_S, WALL_W;

        @Nullable
        public Direction wall() {
            return switch (this) {
                case WALL_N -> Direction.NORTH;
                case WALL_E -> Direction.EAST;
                case WALL_S -> Direction.SOUTH;
                case WALL_W -> Direction.WEST;
                default -> null;
            };
        }

        public static Pose wall(Direction d) {
            return switch (d) {
                case EAST -> WALL_E;
                case SOUTH -> WALL_S;
                case WEST -> WALL_W;
                default -> WALL_N;
            };
        }

        public static Pose of(int ordinal) {
            Pose[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
        }
    }

    /**
     * How a species moves.
     *
     * @param walk      blocks per tick on the land (a hopper: on average)
     * @param swim      blocks per tick in the water (0 = it does not go into the water)
     * @param bottom    walks on the bottom of the water instead of swimming in the open water (musk turtles)
     * @param maxStep   highest step it climbs (tank blocks)
     * @param waterTime share of its goals in the water
     * @param baskTime  share of its goals on a basking spot (doubled while the air is below its range)
     * @param restTime  share of rests
     * @param height    height of its body (for swimming just under the surface)
     * @param style     part c: the body plan's way of moving
     * @param special   part c: the share of climbing goals ({@link Style#CLIMB}) or of buried rests ({@code burrows})
     * @param burrows   part c: rests buried in the ground
     */
    public record Gait(double walk, double swim, boolean bottom, double maxStep, double waterTime, double baskTime, double restTime, double height,
                       Style style, double special, boolean burrows) {
        public Gait(double walk, double swim, boolean bottom, double maxStep, double waterTime, double baskTime, double restTime, double height) {
            this(walk, swim, bottom, maxStep, waterTime, baskTime, restTime, height, Style.WALK, 0, false);
        }

        /** The same gait with another style (tadpoles: {@link Style#SWIM}). */
        public Gait with(Style s) {
            return new Gait(walk, swim, bottom, maxStep, waterTime, baskTime, restTime, height, s, special, burrows);
        }
    }

    /** Ticks of one hop. */
    public static final int HOP_TICKS = 8;
    /** Distance of the glass the climber keeps (its belly on the glass). */
    private static final double CLING = TankTerrain.MARGIN + 0.01;

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
    private Pose pose = Pose.NONE;
    private boolean buried;
    /** Part c: the wall it clings to (null on the ground) and the foot it climbs from. */
    @Nullable
    private Direction wall;
    /** Part c: the hop: tick within the hop (-1 = between hops), its length, the pause, the lift added to the height now. */
    private int hopTick = -1, hopWait, hopPause = 10;
    private double hopLen, lift;
    private int highest;
    /** M54: the habits of this tick (null: none or the switch off) and the ticks of the fast phase left. */
    @Nullable
    private Habits habits;
    private int dash;

    public Activity activity() {
        return activity;
    }

    public int baskTicks() {
        return baskTicks;
    }

    /** Part c: what the animal shows now (buried, clinging to a wall). */
    public Pose pose() {
        return pose;
    }

    /** Part c: on a wall now (clinging to the glass or climbing a face of the decoration). */
    public boolean onWall() {
        return pose.wall() != null;
    }

    /** Part c (GameTests): the highest it got above the tank floor, in sixteenths of a block. */
    public int highestClimb() {
        return highest;
    }

    /** On its basking spot now. */
    public boolean basking() {
        return activity == Activity.BASK && atGoal;
    }

    /** M54: sits still where it wants to be: resting, or arrived at its basking spot, its perch or its hide. */
    public boolean settled() {
        return activity == Activity.REST || atGoal && (activity == Activity.BASK || activity == Activity.PERCH || activity == Activity.HIDE);
    }

    /** M54: in a fast phase now. */
    public boolean dashing() {
        return dash > 0;
    }

    /** M54 (GameTests): a fast phase of this many ticks now. */
    public void dashForTest(int ticks) {
        dash = ticks;
    }

    private boolean atGoal;

    /** Forget the goal (a new one next tick). */
    public void reset() {
        goal = null;
        path = null;
        goalTicks = 0;
    }

    /** GameTests: go to this activity now (and keep choosing it). */
    public void forceActivity(Activity a) {
        activity = a;
        goal = null;
        path = null;
        goalTicks = 0;
        forced = a;
    }

    @Nullable
    private Activity forced;

    /**
     * M31 part b: walk to this spot on the land now (a female to her nest), for at most {@code ticks}; afterwards the walker
     * picks its own goals again. {@link #arrivedAt} tells when it is there.
     */
    public void goTo(Mob mob, ServerLevel level, TankData t, Vec3 target, int ticks) {
        activity = Activity.LAND;
        goal = target;
        goalTicks = ticks;
        atGoal = false;
        buried = false;
        pathIndex = 0;
        List<Vec3> way = InteriorPaths.walk(level.getServer(), t, mob.position(), target, Double.NaN);
        path = way != null && !way.isEmpty() ? way : null;
    }

    /** M31 part b: whether the animal stands at this goal (within {@code reach} blocks, ignoring the height). */
    public static boolean arrivedAt(Mob mob, Vec3 target, double reach) {
        double dx = mob.getX() - target.x, dz = mob.getZ() - target.z;
        return dx * dx + dz * dz <= reach * reach;
    }

    /** M31 part b: stand still (brumation): no goal, no path, resting. */
    public void hold() {
        activity = Activity.REST;
        goal = null;
        path = null;
        goalTicks = 0;
        atGoal = false;
    }

    /** The terrain of the tank (rebuilt every second). */
    public TankTerrain terrain(ServerLevel level, TankData t) {
        long now = level.getGameTime();
        if (terrain == null || now - terrainTime >= 20 || now < terrainTime) {
            terrain = new TankTerrain(t, level.getServer(), org.overengineer.bettacraft.tank.interior.InteriorRegions.get(level.getServer()).get(t.regionId()),
                    level.isBrightOutside());
            terrainTime = now;
            spots = TankClimate.baskingSpots(level, t);
        }
        return terrain;
    }

    /** One tick in the tank. {@code cold}: the air is below the animal's range (it seeks the basking spot). */
    public void tick(Mob mob, ServerLevel level, TankData t, Gait gait, boolean cold) {
        tick(mob, level, t, gait, cold, null);
    }

    /** M54: one tick in the tank with the habits of the species (null: none). */
    public void tick(Mob mob, ServerLevel level, TankData t, Gait gait, boolean cold, @Nullable Habits h) {
        habits = h != null && Habits.enabled() ? h : null;
        if (habits == null) dash = 0;
        TankTerrain ter = terrain(level, t);
        double surface = ter.surface();
        boolean water = Double.isFinite(surface);
        Vec3 pos = mob.position().subtract(0, lift, 0); // the hop's lift is no part of where it stands
        RandomSource r = mob.getRandom();
        if (goal == null || --goalTicks <= 0) pick(mob, level, t, ter, gait, cold, r);
        if (goal == null) return;
        if (wall != null) {
            wallTick(mob, ter, pos, gait, t.min().getY());
            return;
        }
        if (gait.style() == Style.FLY) {
            flyTick(mob, t, ter, pos, gait);
            return;
        }
        Vec3 way = goal;
        if (path != null && pathIndex < path.size()) way = path.get(pathIndex);
        double dx = way.x - pos.x, dz = way.z - pos.z;
        double flat = Math.sqrt(dx * dx + dz * dz);
        boolean inWater = water && pos.y + gait.height() * 0.5 < surface;
        double speed = inWater && gait.swim() > 0 ? gait.swim() : gait.walk();
        if (habits != null) { // M54: its own tempo, faster in a fast phase
            speed *= habits.tempo();
            if (dash > 0) {
                dash--;
                if (gait.style() != Style.HOP) speed *= habits.dashFactor();
            }
        }
        if (activity == Activity.REST) speed = 0;
        double maxStep = climbing(gait) ? Math.max(gait.maxStep(), habits.climb()) : gait.maxStep();
        atGoal = false;
        if (flat < 0.05) {
            if (path != null && pathIndex < path.size() - 1) {
                pathIndex++;
            } else if (path != null && (goal.x - pos.x) * (goal.x - pos.x) + (goal.z - pos.z) * (goal.z - pos.z) > 0.05 * 0.05) {
                path = null; // part c: the end of the way is a grid cell: the last bit straight to the goal itself
            } else {
                atGoal = true;
                if (activity == Activity.BASK) baskTicks++;
                if (activity == Activity.CLIMB && gait.style() == Style.CLIMB && goal.y > pos.y + 0.05 && climbWall != null) {
                    wall = climbWall; // at the foot of the wall: up it goes
                    pose = Pose.wall(wall);
                    return;
                }
            }
            speed = 0;
        }
        // part c: hopping on land: a hop of HOP_TICKS ticks, then a pause
        double newLift = 0;
        if (gait.style() == Style.HOP && !inWater && speed > 0) {
            if (hopTick < 0) {
                speed = 0;
                if (++hopWait >= hopPause) {
                    hopTick = 0;
                    hopLen = Math.min(flat, gait.walk() * (habits != null ? habits.tempo() : 1) * HOP_TICKS * 3);
                }
            } else {
                speed = hopLen / HOP_TICKS;
                hopTick++;
                newLift = Math.sin(Math.PI * hopTick / HOP_TICKS) * Math.max(0.05, gait.height() * 0.9);
                if (hopTick >= HOP_TICKS) {
                    hopTick = -1;
                    hopWait = 0;
                    // M54: a fast phase of a hopper is a quick series of hops
                    hopPause = dash > 0 && habits != null ? 3 + r.nextInt(5) : 8 + r.nextInt(gait.burrows() ? 40 : 18);
                    newLift = 0;
                }
            }
        } else {
            hopTick = -1;
        }
        double nx = pos.x, nz = pos.z;
        if (speed > 0 && flat > 1e-6) {
            double step = Math.min(speed, flat);
            nx += dx / flat * step;
            nz += dz / flat * step;
        }
        double ny = pos.y;
        double g = ter.groundAt(nx, pos.y + maxStep + 0.05, nz);
        if (habits != null && Double.isFinite(g) && g - pos.y > maxStep) {
            // M54: a ledge lower than its step (a cave, a hide): the floor under it, when its body fits under the ledge
            double under = ter.groundAt(nx, pos.y + 0.05, nz);
            if (Double.isFinite(under) && under <= pos.y + 0.05 && Math.abs(ter.groundAt(nx, under + gait.height() + 0.02, nz) - under) < 1e-3) g = under;
        }
        if (!Double.isFinite(g) || !t.contains(BlockPos.containing(nx, Math.max(t.min().getY(), g + 0.05), nz))) {
            blocked();
            g = ter.groundAt(pos.x, pos.y + 0.1, pos.z);
            nx = pos.x;
            nz = pos.z;
            if (!Double.isFinite(g)) return;
        }
        boolean deep = water && g < surface - gait.height();
        boolean wantSurface = goal.y > surface - 0.05;
        Pose show = activity == Activity.REST && buried && !inWater ? Pose.BURIED : Pose.NONE;
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
            if (rise > maxStep && !(water && pos.y + gait.height() >= surface - 0.05 && g <= surface + maxStep)) {
                blocked();
                pose = show;
                return;
            }
            // M54: on the way to its perch a snake or a lizard climbs the face of the decoration like the climber (a hopper hops up)
            if ((gait.style() == Style.CLIMB || climbing(gait) && gait.style() != Style.HOP) && rise > 0.1) {
                // part c: a climber goes straight up the face of the decoration before it walks on
                nx = pos.x;
                nz = pos.z;
                ny = pos.y + Math.min(rise, Math.max(0.02, gait.walk()));
                show = Pose.wall(Direction.getApproximateNearest(dx, 0, dz));
            } else {
                ny = pos.y + Mth.clamp(rise, -0.15, 0.08);
            }
        }
        pose = show;
        highest = Math.max(highest, (int) Math.round((ny - t.min().getY()) * 16));
        ground(mob, newLift == 0 && show.wall() == null && ny <= g + GROUND_EPS); // M44: standing, not hopping, climbing or swimming
        move(mob, pos, nx, ny + newLift, nz, newLift);
    }

    /**
     * M35 part b: one tick of a flier: straight towards its goal in the air at its speed (slower when it is close, a
     * little bob while it hovers), never below the ground or the decoration under it (it rises over them), never out of
     * the tank (a blocked step: another goal), never under the water surface.
     */
    private void flyTick(Mob mob, TankData t, TankTerrain ter, Vec3 pos, Gait gait) {
        double surface = ter.surface();
        double dx = goal.x - pos.x, dy = goal.y - pos.y, dz = goal.z - pos.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        atGoal = dist < 0.05;
        pose = Pose.NONE;
        double nx = pos.x, ny = pos.y, nz = pos.z;
        if (!atGoal) {
            double step = Math.min(dist, Math.max(0.01, gait.walk() * Math.min(1.0, 0.35 + dist)));
            nx += dx / dist * step;
            ny += dy / dist * step;
            nz += dz / dist * step;
        } else if (activity != Activity.REST) {
            ny = goal.y + Math.sin(mob.tickCount * 0.3) * 0.02; // hovering
        }
        double g = ter.ground(nx, nz);
        if (!Double.isFinite(g)) {
            blocked();
            return;
        }
        double low = Math.max(g, Double.isFinite(surface) ? surface + 0.02 : g);
        ny = Math.max(ny, low);
        double top = flyTop(t, nx, nz, gait);
        if (ny > top) ny = Math.max(low, top);
        if (!t.contains(BlockPos.containing(nx, ny + 0.01, nz)) || !t.contains(BlockPos.containing(nx, ny + gait.height() * 0.5, nz))) {
            blocked();
            return;
        }
        highest = Math.max(highest, (int) Math.round((ny - t.min().getY()) * 16));
        ground(mob, ny <= g + GROUND_EPS); // M44: a flier only on its perch
        move(mob, pos, nx, ny, nz, 0);
    }

    /** M35 part b: the highest a flier goes at this column (under the lid, its body below it). */
    private static double flyTop(TankData t, double x, double z, Gait gait) {
        int bx = Mth.floor(x), bz = Mth.floor(z);
        int y = t.max().getY();
        while (y > t.min().getY() && !t.contains(new BlockPos(bx, y, bz))) y--;
        return y + 1 - TankTerrain.MARGIN - gait.height() - 0.05;
    }

    /**
     * M35 part b: a spot in the air of the tank for a flier: over a random column, between a little above the land or the
     * water and the lid; {@code skim}: just over the water surface (hunting, laying eggs).
     */
    @Nullable
    private static Vec3 airSpot(TankData t, TankTerrain ter, RandomSource r, Gait gait, boolean skim) {
        double surface = ter.surface();
        double m = 0.3;
        for (int i = 0; i < 24; i++) {
            double x = t.min().getX() + m + r.nextDouble() * Math.max(0.01, t.sizeX() - 2 * m);
            double z = t.min().getZ() + m + r.nextDouble() * Math.max(0.01, t.sizeZ() - 2 * m);
            double g = ter.ground(x, z);
            if (!Double.isFinite(g)) continue;
            boolean wet = Double.isFinite(surface) && g < surface - 0.01;
            if (skim && !wet) continue;
            double low = (wet ? surface : g) + (skim ? 0.08 : 0.35);
            double top = flyTop(t, x, z, gait);
            if (top < low) continue;
            double y = skim ? low : low + r.nextDouble() * (top - low);
            if (!t.contains(BlockPos.containing(x, y + 0.01, z))) continue;
            return new Vec3(x, y, z);
        }
        return null;
    }

    /** Part c: clinging to a wall: up to the goal while climbing, else down to its foot (then it walks on). */
    private void wallTick(Mob mob, TankTerrain ter, Vec3 pos, Gait gait, double floorY) {
        double g = ter.groundAt(pos.x, pos.y + 0.05, pos.z);
        if (!Double.isFinite(g)) g = pos.y;
        double climb = Math.max(0.01, gait.walk() * 0.7), ny;
        if (activity == Activity.CLIMB) {
            ny = pos.y + Mth.clamp(goal.y - pos.y, -climb, climb);
            atGoal = Math.abs(goal.y - ny) < 1e-3;
        } else {
            ny = Math.max(g, pos.y - climb);
            if (ny <= g + 1e-3) {
                wall = null;
                pose = Pose.NONE;
                ground(mob, true);
                move(mob, pos, pos.x, g, pos.z, 0);
                return;
            }
        }
        pose = Pose.wall(wall);
        highest = Math.max(highest, (int) Math.round((ny - floorY) * 16));
        // face the glass (the renderer lays the belly on it)
        float yaw = wall.toYRot();
        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        mob.yHeadRot = yaw;
        ground(mob, false);
        move(mob, pos, pos.x, ny, pos.z, 0);
    }

    /** M44: how close above the ground the animal counts as standing on it. */
    private static final double GROUND_EPS = 0.02;

    /**
     * M44: the walker places the animal with {@code setPos}, never with {@code move()}, so vanilla never sets {@code onGround}
     * (it stayed false: the chicken flapped its wings all the time). Set while it stands, not while it hops, climbs, swims
     * or flies; the client gets it with the position packets.
     */
    private static void ground(Mob mob, boolean on) {
        if (mob.onGround() != on) mob.setOnGround(on);
    }

    private void move(Mob mob, Vec3 pos, double nx, double ny, double nz, double newLift) {
        double oldY = pos.y + lift;
        lift = newLift;
        if (Math.abs(nx - pos.x) + Math.abs(ny - oldY) + Math.abs(nz - pos.z) > 1e-5) {
            mob.setPos(nx, ny, nz);
            if (wall == null && Math.abs(nx - pos.x) + Math.abs(nz - pos.z) > 1e-4) {
                float yaw = (float) (Mth.atan2(nz - pos.z, nx - pos.x) * 180.0 / Math.PI) - 90F;
                mob.setYRot(Mth.rotLerp(0.25F, mob.getYRot(), yaw));
                mob.yBodyRot = mob.getYRot();
                mob.yHeadRot = mob.getYRot();
            }
            stuck = 0;
        }
    }

    /** M54: on the way to a perch (it climbs higher faces than its step). */
    private boolean climbing(Gait gait) {
        return habits != null && activity == Activity.PERCH && habits.climb() > gait.maxStep();
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

    @Nullable
    private Direction climbWall;

    private void pick(Mob mob, ServerLevel level, TankData t, TankTerrain ter, Gait gait, boolean cold, RandomSource r) {
        double surface = ter.surface();
        boolean water = Double.isFinite(surface);
        Activity a;
        if (forced != null) {
            a = forced;
        } else if (gait.style() == Style.SWIM) {
            a = r.nextDouble() < gait.restTime() ? Activity.REST : Activity.WATER;
        } else if (gait.style() == Style.FLY) {
            double x = r.nextDouble();
            a = x < gait.restTime() ? Activity.REST : x < gait.restTime() + (water ? gait.waterTime() : 0) ? Activity.WATER : Activity.LAND;
        } else {
            double bask = spots.isEmpty() ? 0 : gait.baskTime() * (cold ? 2 : 1);
            double wet = water && gait.swim() > 0 ? gait.waterTime() : 0;
            double rest = gait.restTime();
            double climb = gait.style() == Style.CLIMB ? gait.special() : 0;
            double perch = 0, hide = 0;
            if (habits != null) { // M54: its hours: rests and hides while it is not active, climbs and hides by its habits
                long time = level.getDayTime();
                boolean day = level.isBrightOutside();
                rest *= habits.restFactor(time, day);
                perch = habits.perch() * (habits.active(time, day) ? 1.0 : 0.5);
                hide = habits.hide() * habits.hideFactor(time, day);
                if (!day) bask = 0;
            }
            double land = Math.max(0.05, 1 - bask - wet - rest - climb - perch - hide);
            double x = r.nextDouble() * (bask + wet + rest + climb + perch + hide + land);
            a = x < bask ? Activity.BASK : x < bask + wet ? Activity.WATER : x < bask + wet + rest ? Activity.REST
                    : x < bask + wet + rest + climb ? Activity.CLIMB : x < bask + wet + rest + climb + perch ? Activity.PERCH
                    : x < bask + wet + rest + climb + perch + hide ? Activity.HIDE : Activity.LAND;
        }
        climbWall = null;
        if (gait.style() == Style.FLY) { // M35 part b: a flier: a perch on the land, the air, just over the water
            Vec3 target = switch (a) {
                case REST -> spot1(t, ter, r, true, gait);
                case WATER -> airSpot(t, ter, r, gait, true);
                default -> airSpot(t, ter, r, gait, false);
            };
            if (target == null) {
                a = Activity.LAND;
                target = airSpot(t, ter, r, gait, false);
            }
            if (target == null) {
                a = Activity.REST;
                target = mob.position();
            }
            activity = a;
            goal = target;
            buried = false;
            goalTicks = a == Activity.REST ? 100 + r.nextInt(200) : a == Activity.WATER ? 40 + r.nextInt(60) : 60 + r.nextInt(100);
            path = null;
            pathIndex = 0;
            return;
        }
        Vec3 target = switch (a) {
            case BASK -> spots.isEmpty() ? null : spots.get(r.nextInt(spots.size())).add((r.nextDouble() - 0.5) * 0.2, 0, (r.nextDouble() - 0.5) * 0.2);
            case WATER -> spot(t, ter, r, false, gait);
            case LAND -> spot(t, ter, r, true, gait);
            case CLIMB -> wallSpot(t, ter, r, gait);
            case PERCH -> perchSpot(t, ter, r, gait);
            case HIDE -> hideSpot(mob, ter, r, gait);
            case REST -> mob.position().subtract(0, lift, 0);
        };
        if (target == null && a != Activity.LAND && !(gait.style() == Style.SWIM)) {
            a = Activity.LAND;
            target = spot(t, ter, r, true, gait);
        }
        if (target == null) {
            a = Activity.REST;
            target = mob.position().subtract(0, lift, 0);
        }
        // a climber on the wall that stays up rests there; one that goes elsewhere comes down first (wallTick)
        if (wall != null && a == Activity.REST) a = Activity.CLIMB;
        activity = a;
        goal = target;
        buried = a == Activity.REST && gait.burrows() && (forced == Activity.REST || r.nextDouble() < gait.special()); // GameTests: a forced rest is buried
        goalTicks = switch (a) {
            case REST -> buried ? 400 + r.nextInt(800) : habits != null ? 100 + r.nextInt(260) : 60 + r.nextInt(120);
            case BASK -> 400 + r.nextInt(600);
            case CLIMB -> 400 + r.nextInt(500);
            case PERCH -> 300 + r.nextInt(500);
            case HIDE -> 400 + r.nextInt(700);
            default -> 200 + r.nextInt(200);
        };
        // M54: a fast phase at the start of a walk now and then
        dash = habits != null && a != Activity.REST && a != Activity.BASK && a != Activity.HIDE && r.nextDouble() < habits.dashChance() ? habits.dashTicks() : 0;
        path = null;
        pathIndex = 0;
        if (a != Activity.REST && wall == null) {
            Vec3 to = a == Activity.CLIMB ? new Vec3(target.x, ter.ground(target.x, target.z), target.z) : target;
            List<Vec3> way = InteriorPaths.walk(level.getServer(), t, mob.position(), to, Double.NaN);
            if (way != null && !way.isEmpty()) path = way;
        }
    }

    /** A random spot on the land ({@code land}) or in the water; a climber takes the highest of three land spots. */
    @Nullable
    private static Vec3 spot(TankData t, TankTerrain ter, RandomSource r, boolean land, Gait gait) {
        if (land && gait.style() == Style.CLIMB) {
            Vec3 best = null;
            for (int i = 0; i < 3; i++) {
                Vec3 s = spot1(t, ter, r, true, gait);
                if (s != null && (best == null || s.y > best.y)) best = s;
            }
            return best;
        }
        return spot1(t, ter, r, land, gait);
    }

    @Nullable
    private static Vec3 spot1(TankData t, TankTerrain ter, RandomSource r, boolean land, Gait gait) {
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

    /**
     * M54: a high spot of the land (a branch, a rock, a leaf of a plant): the highest of eight random land spots, at least a
     * little above the lowest of them; null when the land is flat.
     */
    @Nullable
    private static Vec3 perchSpot(TankData t, TankTerrain ter, RandomSource r, Gait gait) {
        Vec3 best = null, low = null;
        for (int i = 0; i < 8; i++) {
            Vec3 s = spot1(t, ter, r, true, gait);
            if (s == null) continue;
            if (best == null || s.y > best.y) best = s;
            if (low == null || s.y < low.y) low = s;
        }
        return best != null && best.y > low.y + 0.12 ? best : null;
    }

    /**
     * M54: a spot under the decoration near the animal (a cave, an overhang, leaves above it; {@link TankTerrain#shelter}); a
     * land animal only on the land.
     */
    @Nullable
    private static Vec3 hideSpot(Mob mob, TankTerrain ter, RandomSource r, Gait gait) {
        // M56: snakes and geckos hide among the grown terrarium plants too (more often than under the decoration)
        if (org.overengineer.bettacraft.plants.TerrariumPlantLife.hides(mob) && r.nextInt(5) < 3) {
            double[] p = ter.plantShelter(mob.getX(), mob.getZ(), 2.5, r::nextLong);
            if (p != null) return new Vec3(p[0], p[1], p[2]);
        }
        double[] s = ter.shelter(mob.getX(), mob.getY(), mob.getZ(), 2.5, r::nextLong);
        if (s == null) return null;
        double surface = ter.surface();
        boolean wet = Double.isFinite(surface) && s[1] < surface - 0.01;
        if (wet && gait.swim() <= 0) return null;
        return new Vec3(s[0], s[1], s[2]);
    }

    /**
     * Part c: a spot high on a glass wall above the land: from a random land spot straight to the nearest glass in a
     * random direction (free tank shapes too); the height between half a block above the ground and just under the lid.
     */
    @Nullable
    private Vec3 wallSpot(TankData t, TankTerrain ter, RandomSource r, Gait gait) {
        double surface = ter.surface();
        for (int i = 0; i < 12; i++) {
            Vec3 from = spot1(t, ter, r, true, gait);
            if (from == null) return null;
            Direction d = Direction.from2DDataValue(r.nextInt(4));
            int bx = Mth.floor(from.x), bz = Mth.floor(from.z), by = Mth.floor(from.y + 0.05);
            int steps = 0;
            while (steps < 64 && t.contains(new BlockPos(bx + d.getStepX(), by, bz + d.getStepZ()))) {
                bx += d.getStepX();
                bz += d.getStepZ();
                steps++;
            }
            double x = from.x, z = from.z;
            if (d.getAxis() == Direction.Axis.X) x = d.getStepX() > 0 ? bx + 1 - CLING : bx + CLING;
            else z = d.getStepZ() > 0 ? bz + 1 - CLING : bz + CLING;
            double g = ter.ground(x, z);
            if (!Double.isFinite(g) || Double.isFinite(surface) && g <= surface + 0.01) continue;
            int topBlock = by;
            while (topBlock + 1 <= t.max().getY() && t.contains(new BlockPos(Mth.floor(x), topBlock + 1, Mth.floor(z)))) topBlock++;
            double top = topBlock + 1 - TankTerrain.MARGIN - gait.height() - 0.15;
            if (top < g + 0.5) continue;
            double y = g + 0.5 + r.nextDouble() * (top - g - 0.5);
            climbWall = d;
            return new Vec3(x, y, z);
        }
        return null;
    }
}
