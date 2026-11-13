package org.overengineer.bettacraft.critters;

import java.util.random.RandomGenerator;

/**
 * How the small animals move (M14), one step per second on the server: they walk on the floor of the tank (substrate,
 * decoration and plants: the {@link Terrain} knows the height of every spot), climb up and down steps (snails slowly up
 * every wall, shrimp hop), crawl up the inside of the glass (snails and shrimp, grazing the algae there; nerites like the
 * dirtiest glass), shrimp now and then swim a short jump, and they rest in between. Clients glide them linearly from one
 * update to the next. Pure logic (unit-tested): the tank and interior lookups are behind {@link Terrain}.
 */
public final class CritterMovement {
    /** Distance of an animal on the glass from the outer face of the tank block (the glass is 1/16 thick). */
    public static final double GLASS_INSET = 1.0 / 16 + 0.004;
    /** Highest point an animal on the glass goes to, below the water surface. */
    public static final double BELOW_SURFACE = 0.03;
    /** Shrimp jump through the water at this speed (blocks per second). */
    public static final double SWIM_SPEED = 0.25;

    private CritterMovement() {}

    /** What the movement needs to know about the tank. */
    public interface Terrain {
        /** World y of the walkable top at (x, z): floor, substrate, decoration, plants. NaN where there is none. */
        double ground(double x, double z);

        /** World y of the water surface (NaN or the floor when the tank is dry). */
        double surface();

        /** Whether the block belongs to the tank. */
        boolean member(int x, int y, int z);

        /** Algae on the inside of the glass face of block (x, y, z) towards {@code dir} (2D data value), 0..1. */
        double algae(int x, int y, int z, int dir);

        /** Day time (trumpet snails dig in). */
        boolean day();

        /** Tank bounds (blocks, inclusive). */
        int minX();

        int minY();

        int minZ();

        int maxX();

        int maxY();

        int maxZ();

        /** Highest y above the water line an apple snail can reach on the glass (NaN: no air gap). */
        double airTop();
    }

    // ------------------------------------------------------------------ helpers

    /** 2D data value of the glass face ({@link Critter#GLASS_SOUTH} ...): 0 south, 1 west, 2 north, 3 east. */
    public static int dir(byte face) {
        return face - Critter.GLASS_SOUTH;
    }

    public static int stepX(int dir) {
        return dir == 1 ? -1 : dir == 3 ? 1 : 0;
    }

    public static int stepZ(int dir) {
        return dir == 0 ? 1 : dir == 2 ? -1 : 0;
    }

    /** Whether the glass face of block (x, y, z) towards {@code dir} is a wall of the tank (no tank block behind it). */
    public static boolean wall(Terrain t, int x, int y, int z, int dir) {
        return t.member(x, y, z) && !t.member(x + stepX(dir), y, z + stepZ(dir));
    }

    /** Coordinate (x for east/west, z for south/north) of the plane an animal on that glass sits on. */
    public static double plane(int bx, int bz, int dir) {
        return switch (dir) {
            case 0 -> bz + 1 - GLASS_INSET;
            case 2 -> bz + GLASS_INSET;
            case 1 -> bx + GLASS_INSET;
            default -> bx + 1 - GLASS_INSET;
        };
    }

    /** Distance an animal keeps from the glass while it walks on the floor. */
    public static double margin(CritterSpecies s) {
        return GLASS_INSET + s.length() * 0.3;
    }

    /** Whether an animal can stand at (x, y, z): inside the tank's blocks with the margin and under water. */
    public static boolean standable(Terrain t, double x, double y, double z, double m) {
        int by = (int) Math.floor(y + 0.01);
        for (int i = 0; i < 4; i++) {
            int bx = (int) Math.floor(x + ((i & 1) == 0 ? -m : m)), bz = (int) Math.floor(z + ((i & 2) == 0 ? -m : m));
            if (!t.member(bx, by, bz)) return false;
        }
        double s = t.surface();
        return Double.isFinite(s) && y < s - BELOW_SURFACE;
    }

    /** The walkable ground at (x, z) for this species, NaN when it cannot stand there. */
    public static double groundFor(Terrain t, CritterSpecies s, double x, double z) {
        double g = t.ground(x, z);
        if (!Double.isFinite(g)) return Double.NaN;
        return standable(t, x, g, z, margin(s)) ? g : Double.NaN;
    }

    private static boolean dry(Terrain t) {
        double s = t.surface();
        return !Double.isFinite(s) || s <= t.minY() + 0.07;
    }

    private static double climb(CritterSpecies s) {
        return switch (s.group()) {
            case SHRIMP -> 0.5;
            case CRAYFISH -> 0.3;
            case SNAIL -> s.speed();
        };
    }

    private static double speed(Critter c) {
        double v = c.species.speed() * (c.adult() ? 1.0 : 0.7);
        if (c.hunger > 2) v *= 0.6;
        if (c.berried() && c.species.isCrayfish()) v *= 0.8;
        return v;
    }

    // ------------------------------------------------------------------ the step

    /** One second of movement. */
    public static void step(Critter c, Terrain t, RandomGenerator rnd) {
        if (!c.alive()) {
            // the body sinks to the floor
            c.face = Critter.FLOOR;
            double g = t.ground(c.x, c.z);
            if (Double.isFinite(g)) c.y = Math.max(g, c.y - 0.15);
            return;
        }
        if (dry(t)) return; // stranded on the floor
        if (c.species.burrows()) {
            if (t.day() && c.face == Critter.FLOOR && !c.buried && c.idle <= 0 && rnd.nextDouble() < 0.02) c.buried = true;
            if (c.buried) {
                if (!t.day()) {
                    c.buried = false;
                    c.idle = 2 + rnd.nextInt(5);
                }
                return;
            }
        }
        switch (c.face) {
            case Critter.SWIM -> swim(c, t, rnd);
            case Critter.GLASS_SOUTH, Critter.GLASS_WEST, Critter.GLASS_NORTH, Critter.GLASS_EAST -> glass(c, t, rnd);
            default -> floor(c, t, rnd);
        }
        // the water went down below an animal on the glass: it slides down
        double top = t.surface() - BELOW_SURFACE - 0.02;
        if (c.onGlass() && c.y > top && !laying(c, t)) c.y = Math.max(top, c.y - 0.2);
    }

    /** An apple snail on her way up to lay a clutch above the water line. */
    public static boolean laying(Critter c, Terrain t) {
        return c.species.breeding() == CritterSpecies.Breeding.PAIR_CLUTCH && c.female && c.eggs >= 0 && Double.isFinite(t.airTop());
    }

    private static void floor(Critter c, Terrain t, RandomGenerator rnd) {
        double g0 = t.ground(c.x, c.z);
        if (Double.isFinite(g0) && Math.abs(c.y - g0) > 1e-3 && c.idle > 0) c.y = g0 < c.y ? Math.max(g0, c.y - 0.2) : Math.min(g0, c.y + climb(c.species));
        if (c.idle > 0) {
            c.idle--;
            if (c.species.isShrimp() && rnd.nextDouble() < 0.4) c.yaw += (float) (rnd.nextGaussian() * 25);
            return;
        }
        double dx = c.tx - c.x, dz = c.tz - c.z, d = Math.sqrt(dx * dx + dz * dz);
        if (d < 0.02) {
            if (c.goalFace != 0) {
                startGlass(c, t, rnd);
                return;
            }
            rest(c, rnd);
            pick(c, t, rnd);
            return;
        }
        double v = speed(c), step = Math.min(d, v);
        double nx = c.x + dx / d * step, nz = c.z + dz / d * step;
        // where it stands now is too close to the glass (it came off it, or the water went down): it may walk out of there
        boolean stuck = !Double.isFinite(groundFor(t, c.species, c.x, c.z));
        double g = stuck ? t.ground(nx, nz) : groundFor(t, c.species, nx, nz);
        if (!Double.isFinite(g)) {
            c.idle = 1;
            c.goalFace = 0;
            pick(c, t, rnd);
            return;
        }
        double dy = g - c.y, climb = climb(c.species);
        if (dy > climb) {
            if (c.species.isSnail()) {
                c.y += climb; // crawls up the side of the stone
                c.yaw = yawOf(dx, dz);
                return;
            }
            c.idle = 1; // too high to hop: elsewhere
            c.goalFace = 0;
            pick(c, t, rnd);
            return;
        }
        c.x = nx;
        c.z = nz;
        c.y = dy < -0.2 ? Math.max(g, c.y - 0.2) : g; // down a step: drops (a little at a time)
        c.yaw = yawOf(dx, dz);
    }

    private static void swim(Critter c, Terrain t, RandomGenerator rnd) {
        double dx = c.tx - c.x, dz = c.tz - c.z, d = Math.sqrt(dx * dx + dz * dz);
        double total = Math.max(1e-3, Math.hypot(c.tx - c.sx, c.tz - c.sz));
        double step = Math.min(d, SWIM_SPEED);
        if (d < 0.02 || step >= d) {
            c.x = c.tx;
            c.z = c.tz;
            double g = t.ground(c.x, c.z);
            c.y = Double.isFinite(g) ? g : c.ty;
            c.face = Critter.FLOOR;
            rest(c, rnd);
            return;
        }
        c.x += dx / d * step;
        c.z += dz / d * step;
        double f = 1 - (d - step) / total;
        double base = c.sy + (c.ty - c.sy) * f;
        double arc = Math.sin(Math.PI * f) * Math.min(0.6, 0.25 + total * 0.3);
        c.y = Math.min(base + arc, t.surface() - 0.08);
        c.yaw = yawOf(dx, dz);
    }

    private static void startGlass(Critter c, Terrain t, RandomGenerator rnd) {
        int dir = c.goalFace - Critter.GLASS_SOUTH;
        c.face = c.goalFace;
        c.goalFace = 0;
        if (dir == 0 || dir == 2) c.z = plane(c.goalBx, c.goalBz, dir);
        else c.x = plane(c.goalBx, c.goalBz, dir);
        glassTarget(c, t, rnd);
    }

    /** A new spot higher up on the same glass column (or above the water for a laying apple snail). */
    private static void glassTarget(Critter c, Terrain t, RandomGenerator rnd) {
        int dir = Critter.GLASS_SOUTH <= c.face ? c.face - Critter.GLASS_SOUTH : 0;
        int bx = (int) Math.floor(c.x - stepX(dir) * 0.01), bz = (int) Math.floor(c.z - stepZ(dir) * 0.01);
        double g = t.ground(c.x, c.z);
        double low = Double.isFinite(g) ? g : t.minY() + 0.05;
        double top = columnTop(t, bx, (int) Math.floor(c.y + 0.01), bz, dir);
        double high = Math.min(top - c.species.length() * 0.5, t.surface() - BELOW_SURFACE - c.species.length() * 0.4);
        if (laying(c, t)) high = Math.min(top - 0.05, t.airTop());
        if (high <= low + 0.05) {
            c.ty = low;
            c.tx = c.x;
            c.tz = c.z;
            return;
        }
        double m = c.species.length() * 0.5 + 0.05;
        double u = rnd.nextDouble() * Math.max(0, 1 - 2 * m) + m;
        if (dir == 0 || dir == 2) {
            c.tx = bx + u;
            c.tz = c.z;
        } else {
            c.tz = bz + u;
            c.tx = c.x;
        }
        c.ty = laying(c, t) ? high : low + 0.05 + rnd.nextDouble() * (high - low - 0.05);
    }

    /** Top of the contiguous glass wall above block (bx, by, bz) in this direction. */
    static double columnTop(Terrain t, int bx, int by, int bz, int dir) {
        int y = by;
        while (y + 1 <= t.maxY() && wall(t, bx, y + 1, bz, dir)) y++;
        return y + 1;
    }

    private static void glass(Critter c, Terrain t, RandomGenerator rnd) {
        if (c.idle > 0) {
            c.idle--;
            return;
        }
        double dx = c.tx - c.x, dy = c.ty - c.y, dz = c.tz - c.z, d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d < 0.02) {
            double g = t.ground(c.x, c.z);
            if (Double.isFinite(g) && c.y <= g + 0.02) {
                // back down on the floor: a step away from the glass (to where it can walk)
                int dir = c.face - Critter.GLASS_SOUTH;
                double away = margin(c.species) - GLASS_INSET + 0.01;
                c.x -= stepX(dir) * away;
                c.z -= stepZ(dir) * away;
                c.face = Critter.FLOOR;
                double g2 = t.ground(c.x, c.z);
                c.y = Double.isFinite(g2) ? g2 : g;
                rest(c, rnd);
                pick(c, t, rnd);
                return;
            }
            c.idle = laying(c, t) ? 4 : (c.species.isSnail() ? 6 + rnd.nextInt(20) : 2 + rnd.nextInt(6));
            if (!laying(c, t) && rnd.nextDouble() < 0.4) {
                // down again
                c.tx = c.x;
                c.tz = c.z;
                c.ty = Double.isFinite(g) ? g : c.y;
            } else {
                glassTarget(c, t, rnd);
            }
            return;
        }
        double v = speed(c), step = Math.min(d, v);
        c.x += dx / d * step;
        c.y += dy / d * step;
        c.z += dz / d * step;
        // on the glass the yaw is the crawling direction in the plane: 0 = up
        int dir = c.face - Critter.GLASS_SOUTH;
        double along = (dir == 0 || dir == 2) ? dx : dz;
        c.yaw = (float) Math.toDegrees(Math.atan2(along, dy));
    }

    private static void rest(Critter c, RandomGenerator rnd) {
        c.idle = switch (c.species.group()) {
            case SHRIMP -> 1 + rnd.nextInt(6);
            case CRAYFISH -> 2 + rnd.nextInt(10);
            case SNAIL -> 4 + rnd.nextInt(25);
        };
    }

    /** Chance that an animal heads for the glass when it picks a new spot. */
    public static double glassChance(CritterSpecies s) {
        if (!s.climbsGlass()) return 0;
        if (s.nerite()) return 0.55;
        return switch (s.group()) {
            case SHRIMP -> 0.12;
            case CRAYFISH -> 0;
            case SNAIL -> 0.4;
        };
    }

    /** Picks the next spot: the glass, a jump (shrimp) or a spot on the floor nearby. */
    public static void pick(Critter c, Terrain t, RandomGenerator rnd) {
        boolean lay = laying(c, t);
        if (lay || rnd.nextDouble() < glassChance(c.species)) {
            if (glassGoal(c, t, rnd)) return;
        }
        if (c.species.isShrimp() && rnd.nextDouble() < 0.15 && jump(c, t, rnd)) return;
        double reach = switch (c.species.group()) {
            case SHRIMP -> 1.0;
            case CRAYFISH -> 1.2;
            case SNAIL -> 0.4;
        };
        for (int i = 0; i < 10; i++) {
            double x = c.x + (rnd.nextDouble() * 2 - 1) * reach, z = c.z + (rnd.nextDouble() * 2 - 1) * reach;
            if (Double.isFinite(groundFor(t, c.species, x, z))) {
                c.tx = x;
                c.tz = z;
                c.goalFace = 0;
                return;
            }
        }
        c.tx = c.x;
        c.tz = c.z;
    }

    private static boolean jump(Critter c, Terrain t, RandomGenerator rnd) {
        for (int i = 0; i < 6; i++) {
            double x = c.x + (rnd.nextDouble() * 2 - 1) * 1.5, z = c.z + (rnd.nextDouble() * 2 - 1) * 1.5;
            double g = groundFor(t, c.species, x, z);
            if (!Double.isFinite(g)) continue;
            c.face = Critter.SWIM;
            c.sx = c.x;
            c.sy = c.y;
            c.sz = c.z;
            c.tx = x;
            c.ty = g;
            c.tz = z;
            return true;
        }
        return false;
    }

    /**
     * Heads for a glass wall below the water: a random wall face (grazers prefer algae), walking first to its foot on the
     * floor. False when the tank has no wet glass the animal can reach.
     */
    private static boolean glassGoal(Critter c, Terrain t, RandomGenerator rnd) {
        int surfaceLayer = (int) Math.floor(t.surface() - 1e-4);
        double total = 0;
        // weighted reservoir over the wall faces under water
        int bestX = 0, bestY = 0, bestZ = 0, bestDir = -1;
        boolean grazer = c.species.algaePerDay() >= 0.5;
        for (int y = t.minY(); y <= Math.min(t.maxY(), surfaceLayer); y++)
            for (int z = t.minZ(); z <= t.maxZ(); z++)
                for (int x = t.minX(); x <= t.maxX(); x++)
                    for (int dir = 0; dir < 4; dir++) {
                        if (!wall(t, x, y, z, dir)) continue;
                        double w = grazer ? 0.15 + 3 * t.algae(x, y, z, dir) : 1.0;
                        // nearby walls are much more likely (an animal does not cross the whole tank for any glass)
                        double cx = x + 0.5, cz = z + 0.5;
                        double dist = Math.hypot(cx - c.x, cz - c.z);
                        w /= 1 + dist * dist;
                        total += w;
                        if (rnd.nextDouble() * total < w) {
                            bestX = x;
                            bestY = y;
                            bestZ = z;
                            bestDir = dir;
                        }
                    }
        if (bestDir < 0) return false;
        // the foot: the lowest block of this wall column
        int fy = bestY;
        while (fy - 1 >= t.minY() && wall(t, bestX, fy - 1, bestZ, bestDir)) fy--;
        double m = margin(c.species);
        double u = 0.15 + rnd.nextDouble() * 0.7;
        double fx, fz;
        if (bestDir == 0 || bestDir == 2) {
            fx = bestX + u;
            fz = bestDir == 0 ? bestZ + 1 - m - 0.001 : bestZ + m + 0.001;
        } else {
            fz = bestZ + u;
            fx = bestDir == 3 ? bestX + 1 - m - 0.001 : bestX + m + 0.001;
        }
        double g = groundFor(t, c.species, fx, fz);
        if (!Double.isFinite(g) || g > fy + 1) return false;
        c.tx = fx;
        c.tz = fz;
        c.goalFace = (byte) (Critter.GLASS_SOUTH + bestDir);
        c.goalBx = bestX;
        c.goalBy = fy;
        c.goalBz = bestZ;
        return true;
    }

    public static float yawOf(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    /** A random spot on the floor of the tank where this species can stand (for new animals), or null. */
    public static double[] randomSpot(Terrain t, CritterSpecies s, RandomGenerator rnd) {
        for (int i = 0; i < 64; i++) {
            double x = t.minX() + rnd.nextDouble() * (t.maxX() - t.minX() + 1);
            double z = t.minZ() + rnd.nextDouble() * (t.maxZ() - t.minZ() + 1);
            double g = groundFor(t, s, x, z);
            if (Double.isFinite(g)) return new double[]{x, g, z};
        }
        return null;
    }
}
