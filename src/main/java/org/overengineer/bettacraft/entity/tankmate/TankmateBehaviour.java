package org.overengineer.bettacraft.entity.tankmate;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.critters.TankTerrain;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorObstacles;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M19: how a tankmate moves in a tank (server). Shoaling fish follow a leader of their kind (the one with the lowest
 * entity id) that cruises like a betta, each at its own offset, in the height band of the species; bottom fish forage
 * along the substrate and the decoration (the interior height map, {@link TankTerrain}), rest there and - the corydoras -
 * dart up for a gulp of air now and then; plecos and otocinclus stick to the glass or lie on the bottom; a guarding male
 * stays at his cave or nest. Targets never lead through interior decoration. Outside a tank the fish just wanders.
 */
final class TankmateBehaviour {
    enum Mode { CRUISE, REST, GLASS, GULP, HOLD }

    private final TankmateEntity fish;
    Mode mode = Mode.CRUISE;
    @Nullable
    Vec3 target;
    int ticks;
    double speed = 0.8;
    /** Glass face the fish clings to (0..3 = N, S, W, E) or -1. */
    int glassFace = -1;
    @Nullable
    private TankTerrain terrain;
    private long terrainAt = -1000;
    @Nullable
    private TankmateEntity leader;
    private long leaderAt = -1000;
    private Vec3 offset = Vec3.ZERO;
    int behaviourFlags;
    /** M21.5: resting in a sheltered spot (under decoration) it reaches along a path. */
    boolean sheltered;

    TankmateBehaviour(TankmateEntity fish) {
        this.fish = fish;
    }

    private RandomSource r() {
        return fish.getRandom();
    }

    private TankmateMoveControl move() {
        return (TankmateMoveControl) fish.getMoveControl();
    }

    void tick() {
        TankWaterVolume w = fish.tankWater;
        if (w == null) {
            mode = Mode.CRUISE;
            behaviourFlags = 0;
            wild();
            apply();
            return;
        }
        if (!(fish.level() instanceof ServerLevel server)) return;
        long now = server.getGameTime();
        TankmateSpecies s = fish.species();
        // flee from an attacker
        if (fish.fleeFrom != null && fish.fleeFrom.isAlive() && now < fish.fleeUntil) {
            Vec3 away = fish.position().subtract(fish.fleeFrom.position());
            if (away.lengthSqr() < 1e-4) away = new Vec3(r().nextGaussian(), 0, r().nextGaussian());
            Vec3 t = clamp(w, fish.position().add(away.normalize().scale(1.2)));
            go(t, 1.4, Mode.CRUISE);
            apply();
            return;
        }
        // a guarding male or a spawning female stays at the cave or the nest
        Vec3 hold = fish.holdSpot();
        if (hold != null) {
            double d = fish.position().distanceTo(hold);
            if (d > 0.08) {
                // M21: into the cave through its mouth (not through the wall), around the decoration
                go(steer(server, w, hold), d > 0.6 ? 0.9 : 0.5, Mode.HOLD);
            } else {
                mode = Mode.HOLD;
                move().stop();
                fish.setDeltaMovement(fish.getDeltaMovement().scale(0.5));
                // in its cave a pleco looks out of the mouth
                Vec3 dir = fish.caveDir();
                if (fish.cave() != null && hold.distanceToSqr(fish.cave()) < 1e-4 && dir.lengthSqr() > 0) {
                    float yaw = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90F;
                    fish.setYRot(Mth.approachDegrees(fish.getYRot(), yaw, 10F));
                    fish.yBodyRot = fish.getYRot();
                    fish.yHeadRot = fish.getYRot();
                }
            }
            behaviourFlags = s.zone().bottom() ? TankmateEntity.FLAG_REST : 0;
            apply();
            return;
        }
        // hand-fed food: midwater fish swim to it, bottom fish wait under it on the ground
        if (fish.foodTarget != null && now < fish.foodUntil) {
            Vec3 f = fish.foodTarget;
            double delay = s.zone().bottom() ? 40 : 0;
            if (fish.foodUntil - now < 120 - delay) {
                Vec3 t = s.zone().bottom() ? onGround(server, w, f.x + (r().nextDouble() - 0.5) * 0.4, f.z + (r().nextDouble() - 0.5) * 0.4)
                        : clamp(w, new Vec3(f.x + (r().nextDouble() - 0.5) * 0.5, f.y - r().nextDouble() * 0.6, f.z + (r().nextDouble() - 0.5) * 0.5));
                if (mode != Mode.CRUISE || ticks <= 0 || target == null || target.distanceToSqr(t) > 0.5) {
                    go(t, 1.1, Mode.CRUISE);
                    ticks = 30;
                }
                behaviourFlags = 0;
                ticks--;
                apply();
                return;
            }
        }
        if (--ticks > 0 && target != null) {
            if (mode == Mode.CRUISE || mode == Mode.GULP) {
                if (fish.position().distanceToSqr(target) < 0.01) {
                    if (mode == Mode.GULP) {
                        // back down after the gulp
                        mode = Mode.CRUISE;
                        target = onGround(server, w, fish.getX(), fish.getZ());
                        ticks = 60;
                        speed = 1.0;
                    } else {
                        ticks = Math.min(ticks, 2);
                    }
                }
                if (target != null) move().swimTo(target.x, target.y, target.z, speed);
            } else if (mode == Mode.REST || mode == Mode.GLASS) {
                if (fish.position().distanceToSqr(target) > 0.004) {
                    // M21.5: a kuhli loach creeps into its shelter around the decoration
                    Vec3 to = sheltered ? steer(server, w, target) : target;
                    move().swimTo(to.x, to.y, to.z, sheltered ? 0.35 : 0.4);
                } else {
                    move().stop();
                    fish.setDeltaMovement(fish.getDeltaMovement().scale(0.3));
                    if (mode == Mode.GLASS && glassFace >= 0) {
                        faceGlass();
                        // M21.5: upright only once it is on the glass (it used to stand up on the way already)
                        behaviourFlags = TankmateEntity.FLAG_GLASS;
                    }
                }
            }
            apply();
            return;
        }
        decide(server, w, s);
        apply();
    }

    /** Picks the next thing to do. */
    private void decide(ServerLevel server, TankWaterVolume w, TankmateSpecies s) {
        behaviourFlags = 0;
        glassFace = -1;
        sheltered = false;
        RandomSource r = r();
        boolean night = !server.isDay();
        double restChance = s.nocturnal() ? (night ? 0.15 : 0.75) : 0.3;
        if (eel(s)) {
            // M21.5: kuhli loaches: by day mostly lying still, often half hidden under the decoration; at night calm gliding
            if (r.nextDouble() < (night ? 0.2 : 0.85)) {
                if (!night && r.nextDouble() < 0.7 && shelter(server, w, 500 + r.nextInt(700))) return;
                rest(server, w, (night ? 100 : 300) + r.nextInt(night ? 200 : 500));
                return;
            }
            glide(server, w);
            return;
        }
        switch (s.zone()) {
            case BOTTOM -> {
                if (s.airGulper() && r.nextInt(30) == 0) {
                    gulp(w);
                    return;
                }
                if (r.nextDouble() < restChance) {
                    rest(server, w, 80 + r.nextInt(200));
                    return;
                }
                forage(server, w, s);
            }
            case GLASS -> {
                double x = r.nextDouble();
                if (x < (s.grazer() ? 0.5 : 0.35) && glass(server, w)) return;
                if (x < 0.75) {
                    rest(server, w, 120 + r.nextInt(300));
                    return;
                }
                forage(server, w, s);
            }
            default -> {
                if (s.airGulper() && r.nextInt(40) == 0) {
                    gulp(w);
                    return;
                }
                if (!s.shoaling() && r.nextDouble() < 0.25) {
                    // a solitary fish (gourami) hovers for a while
                    target = fish.position();
                    mode = Mode.REST;
                    ticks = 40 + r.nextInt(80);
                    move().stop();
                    return;
                }
                shoal(server, w, s);
            }
        }
    }

    // ------------------------------------------------------------------ modes

    private void shoal(ServerLevel server, TankWaterVolume w, TankmateSpecies s) {
        TankmateEntity lead = s.shoaling() ? leader(server, w) : null;
        RandomSource r = r();
        if (lead == null || lead == fish) {
            go(cruiseTarget(w, s, 1.2, 3.5), 0.75, Mode.CRUISE);
            ticks = 60 + r.nextInt(100);
            return;
        }
        double spread = 0.2 + s.length() * 1.6;
        if (offset == Vec3.ZERO || r.nextInt(6) == 0) {
            offset = new Vec3(r.nextGaussian() * spread, r.nextGaussian() * spread * 0.4, r.nextGaussian() * spread);
        }
        Vec3 t = band(w, s, clamp(w, lead.position().add(offset)));
        double d = fish.position().distanceTo(t);
        if (!pathFree(server, fish.position(), t)) t = band(w, s, clamp(w, lead.position()));
        go(t, d > 1.5 ? 1.3 : 0.85, Mode.CRUISE);
        ticks = 12 + r.nextInt(18);
    }

    private void forage(ServerLevel server, TankWaterVolume w, TankmateSpecies s) {
        RandomSource r = r();
        TankmateEntity lead = s.shoaling() ? leader(server, w) : null;
        Vec3 from = lead != null && lead != fish && r.nextInt(3) > 0 ? lead.position() : fish.position();
        for (int i = 0; i < 8; i++) {
            double ang = r.nextDouble() * Math.PI * 2, dist = 0.3 + r.nextDouble() * 1.2;
            Vec3 t = onGround(server, w, from.x + Math.cos(ang) * dist, from.z + Math.sin(ang) * dist);
            if (!pathFree(server, fish.position(), t)) continue;
            go(t, 0.5, Mode.CRUISE);
            ticks = 40 + r.nextInt(60);
            return;
        }
        rest(server, w, 60);
    }

    /** M21.5: the eel-like kuhli loach (smooth swimming on). */
    private static boolean eel(TankmateSpecies s) {
        return s.model() == TankmateSpecies.Model.KUHLI && org.overengineer.bettacraft.config.CommonConfig.smoothSwimming();
    }

    /** M21.5: a calm, long glide along the bottom, mostly ahead (few changes of direction). */
    private void glide(ServerLevel server, TankWaterVolume w) {
        RandomSource r = r();
        double heading = fish.getYRot() * Mth.DEG_TO_RAD;
        for (int i = 0; i < 10; i++) {
            double ang = i < 7 ? heading + (r.nextDouble() - 0.5) * (Math.PI / 2) : r.nextDouble() * Math.PI * 2;
            double dist = 0.8 + r.nextDouble() * 1.4;
            Vec3 t = onGround(server, w, fish.getX() - Math.sin(ang) * dist, fish.getZ() + Math.cos(ang) * dist);
            if (t.distanceToSqr(fish.position()) < 0.16 || !pathFree(server, fish.position(), t)) continue;
            go(t, 0.4, Mode.CRUISE);
            ticks = 90 + r.nextInt(90);
            return;
        }
        rest(server, w, 120);
    }

    /** M21.5: rests in a sheltered spot under decoration nearby (reached along a path); false if there is none. */
    private boolean shelter(ServerLevel server, TankWaterVolume w, int duration) {
        TankTerrain t = terrain(server);
        if (t == null) return false;
        double[] spot = t.shelter(fish.getX(), fish.getY(), fish.getZ(), 2.0, new java.util.Random(r().nextLong()));
        if (spot == null) return false;
        Vec3 c = clamp(w, new Vec3(spot[0], spot[1] + 0.01, spot[2]));
        target = new Vec3(c.x, Math.max(w.box().minY, spot[1] + 0.01), c.z);
        mode = Mode.REST;
        ticks = duration;
        sheltered = true;
        behaviourFlags = TankmateEntity.FLAG_REST;
        return true;
    }

    private void rest(ServerLevel server, TankWaterVolume w, int duration) {
        target = onGround(server, w, fish.getX(), fish.getZ());
        mode = Mode.REST;
        ticks = duration;
        behaviourFlags = TankmateEntity.FLAG_REST;
    }

    private void gulp(TankWaterVolume w) {
        Vec3 p = fish.position();
        Vec3 up = w.surfacePoint(p, 0.2, Math.max(0.05, fish.getBbHeight()));
        go(up, 1.4, Mode.GULP);
        ticks = 80;
    }

    /** Swims to a point on the inside of a side wall below the surface and clings to it. */
    private boolean glass(ServerLevel server, TankWaterVolume w) {
        var controller = TankInhabitants.resolveController(server, fish);
        if (controller == null) return false;
        TankData t = controller.getTank();
        RandomSource r = r();
        List<Long> walls = new java.util.ArrayList<>();
        for (long l : t.members()) {
            int y = BlockPos.getY(l);
            if (y + 0.2 > w.surface()) continue;
            walls.add(l);
        }
        if (walls.isEmpty()) return false;
        for (int attempt = 0; attempt < 12; attempt++) {
            long l = walls.get(r.nextInt(walls.size()));
            int dir = r.nextInt(4);
            int x = BlockPos.getX(l), y = BlockPos.getY(l), z = BlockPos.getZ(l);
            int nx = x + (dir == 3 ? 1 : dir == 2 ? -1 : 0), nz = z + (dir == 1 ? 1 : dir == 0 ? -1 : 0);
            if (t.contains(BlockPos.asLong(nx, y, nz))) continue; // not a wall
            double half = fish.getBbWidth() * 0.5;
            double inset = fish.sideMargin() + 0.005;
            double top = Math.min(y + 1.0, w.surface()) - Math.max(0.1, fish.species().length() * fish.getAgeScale() * 0.6);
            double bottom = y + (y == t.min().getY() ? 0.15 : 0.0);
            if (top <= bottom) continue;
            double py = bottom + r.nextDouble() * (top - bottom);
            double along = 0.2 + half + r.nextDouble() * Math.max(0, 0.6 - 2 * half);
            double px = switch (dir) {
                case 2 -> x + inset;
                case 3 -> x + 1 - inset;
                default -> x + along;
            };
            double pz = switch (dir) {
                case 0 -> z + inset;
                case 1 -> z + 1 - inset;
                default -> z + along;
            };
            Vec3 p = new Vec3(px, py, pz);
            if (!pathFree(server, fish.position(), p)) continue;
            target = p;
            mode = Mode.GLASS;
            glassFace = dir;
            ticks = 200 + r.nextInt(400);
            // M21.5: FLAG_GLASS (upright) only on arrival (see tick); legacy: at once
            behaviourFlags = org.overengineer.bettacraft.config.CommonConfig.smoothSwimming() ? 0 : TankmateEntity.FLAG_GLASS;
            return true;
        }
        return false;
    }

    /** Head up, belly to the glass: the body turned towards the wall, the renderer pitches it upright. */
    private void faceGlass() {
        float yaw = switch (glassFace) {
            case 0 -> 180F; // north wall (-z)
            case 1 -> 0F;
            case 2 -> 90F; // west wall (-x)
            default -> -90F;
        };
        fish.setYRot(Mth.approachDegrees(fish.getYRot(), yaw, 12F));
        fish.yBodyRot = fish.getYRot();
        fish.yHeadRot = fish.getYRot();
    }

    private void wild() {
        if (!fish.isInWater()) return;
        if (--ticks > 0 && target != null) {
            move().swimTo(target.x, target.y, target.z, speed);
            return;
        }
        RandomSource r = r();
        Vec3 p = fish.position().add(r.nextGaussian() * 2, r.nextGaussian() * 0.5, r.nextGaussian() * 2);
        BlockPos bp = BlockPos.containing(p);
        if (fish.level().getFluidState(bp).is(net.minecraft.tags.FluidTags.WATER)) {
            target = p;
            speed = 0.6;
        }
        ticks = 80 + r.nextInt(80);
    }

    private void apply() {
        int care = fish.careFlags;
        fish.setFlags(care | behaviourFlags);
    }

    // ------------------------------------------------------------------ helpers

    private void go(Vec3 t, double speed, Mode m) {
        this.target = t;
        this.speed = speed;
        this.mode = m;
        if (ticks <= 0) ticks = 40;
        move().precise = m == Mode.HOLD; // M21: on the way into the cave it holds its height (no drifting over the roof)
        move().swimTo(t.x, t.y, t.z, speed);
    }

    /** The shoal leader: the fish of this species in the tank with the lowest entity id (refreshed every 2 s). */
    @Nullable
    private TankmateEntity leader(ServerLevel server, TankWaterVolume w) {
        long now = server.getGameTime();
        if (now - leaderAt < 40 && leader != null && leader.isAlive()) return leader;
        leaderAt = now;
        TankmateEntity best = null;
        for (TankmateEntity e : server.getEntitiesOfClass(TankmateEntity.class, w.box().inflate(0.5),
                e -> e.isAlive() && e.species() == fish.species() && e.isInTank() && e.holdSpot() == null)) {
            if (best == null || e.getId() < best.getId()) best = e;
        }
        leader = best;
        return best;
    }

    private Vec3 cruiseTarget(TankWaterVolume w, TankmateSpecies s, double min, double max) {
        Vec3 p = fish.position();
        RandomSource r = r();
        double heading = fish.getYRot() * Mth.DEG_TO_RAD;
        Vec3 best = null;
        double bestLen = -1;
        ServerLevel server = (ServerLevel) fish.level();
        for (int attempt = 0; attempt < 10; attempt++) {
            double dist = min + r.nextDouble() * (max - min);
            double ang = r.nextFloat() < 0.7 ? heading + (r.nextDouble() - 0.5) * (2 * Math.PI / 3) : r.nextDouble() * 2 * Math.PI;
            Vec3 c = band(w, s, clamp(w, new Vec3(p.x - Math.sin(ang) * dist, p.y + (r.nextDouble() - 0.5) * 0.5, p.z + Math.cos(ang) * dist)));
            if (!pathFree(server, p, c)) continue;
            double len = Math.hypot(c.x - p.x, c.z - p.z);
            if (len >= 0.8) return c;
            if (len > bestLen) {
                best = c;
                bestLen = len;
            }
        }
        return best != null ? best : band(w, s, p);
    }

    /** Keeps a target in the height band of the species' zone. */
    private Vec3 band(TankWaterVolume w, TankmateSpecies s, Vec3 v) {
        double floor = w.box().minY, h = Math.max(0.1, w.surface() - floor);
        double lo = floor + h * s.zone().lo + fish.getBbHeight() * 0.5, hi = floor + h * s.zone().hi;
        if (hi < lo) hi = lo;
        return new Vec3(v.x, Mth.clamp(v.y, lo, hi), v.z);
    }

    private Vec3 clamp(TankWaterVolume w, Vec3 v) {
        return w.clamp(v, fish.sideMargin() + 0.015, 1.0 / 16 + 0.02, Math.max(0.05, fish.getBbHeight()));
    }

    /** A point just above the ground (substrate, decoration, leaves) at x/z. */
    private Vec3 onGround(ServerLevel server, TankWaterVolume w, double x, double z) {
        Vec3 c = clamp(w, new Vec3(x, w.box().minY, z));
        TankTerrain t = terrain(server);
        double g = t == null ? Double.NaN : t.ground(c.x, c.z);
        if (!Double.isFinite(g)) g = w.box().minY;
        double y = Math.min(g + 0.01, w.surface() - fish.getBbHeight() - 0.02);
        return new Vec3(c.x, Math.max(w.box().minY, y), c.z);
    }

    @Nullable
    private TankTerrain terrain(ServerLevel server) {
        long now = server.getGameTime();
        if (terrain != null && now - terrainAt < 200) return terrain;
        TankBlockEntity c = TankInhabitants.resolveController(server, fish);
        if (c == null) return null;
        TankData t = c.getTank();
        var region = org.overengineer.bettacraft.tank.interior.InteriorRegions.get(server.getServer()).get(t.regionId());
        terrain = new TankTerrain(t, server.getServer(), region, server.isDay());
        terrainAt = now;
        return terrain;
    }

    /** The straight line does not cross interior decoration (samples every 1/8 block). */
    private boolean pathFree(ServerLevel server, Vec3 a, Vec3 b) {
        TankBlockEntity c = TankInhabitants.resolveController(server, fish);
        if (c == null || c.getTank().regionId() < 0) return true;
        double len = a.distanceTo(b);
        int steps = Math.max(1, (int) Math.ceil(len * 8));
        for (int i = 1; i <= steps; i++) {
            if (InteriorObstacles.blocked(server.getServer(), c.getTank(), a.lerp(b, i / (double) steps))) return false;
        }
        return true;
    }

    private final org.overengineer.bettacraft.tank.interior.PathFollower path = new org.overengineer.bettacraft.tank.interior.PathFollower();

    /** M21: the next point on the way to {@code goal} around the interior decoration (the goal itself without one). */
    private Vec3 steer(ServerLevel server, TankWaterVolume w, Vec3 goal) {
        TankBlockEntity c = TankInhabitants.resolveController(server, fish);
        if (c == null) return goal;
        return path.next(server.getServer(), c.getTank(), fish.position(), goal, w.surface(), 0.08, server.getGameTime(), fish.sideMargin(), true);
    }

    /** GameTests: waypoints left on the way to the cave. */
    int pathLeft() {
        return path.remaining();
    }

    /** Smoke tests and GameTests: rest or cling right where the fish is. */
    /** GameTests: start the swim to the glass now. */
    boolean glassNow() {
        if (!(fish.level() instanceof ServerLevel server) || fish.tankWater == null) return false;
        return glass(server, fish.tankWater);
    }

    void freeze() {
        target = fish.position();
        mode = Mode.REST;
        ticks = Integer.MAX_VALUE / 2;
        move().stop();
    }
}
