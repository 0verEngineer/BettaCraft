package org.overengineer.bettacraft.entity.tankmate;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
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
    /** M21.5: ticks on the way to the glass or the resting spot. */
    private int travel;

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
                if (largeGoal != null && mode == Mode.CRUISE) {
                    // M30: a big animal picks its next far target before it gets there (no braking), on its way around the decoration
                    if (fish.position().distanceTo(largeGoal) < Math.max(0.6, s.length() * fish.getAgeScale() * 0.7)) ticks = Math.min(ticks, 1);
                    else target = steer(server, w, largeGoal);
                } else if (fish.position().distanceToSqr(target) < 0.01) {
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
                // M21.5: on the glass it counts as arrived a little earlier (the buoyancy kept small fish hovering just off the
                // spot), then it is held there
                double arrive = mode == Mode.GLASS && Features.enabled(Feature.SMOOTH_SWIMMING) ? 0.0225
                        : hitchPost != null ? 0.04 : 0.004; // M21.8: a seahorse holds on a little beside its post (the path ends short of it)
                if (s.isLarge()) {
                    // M30 part b: a big animal cannot turn on the spot: it glides in over its spot, settles and sinks onto it
                    double len = Math.max(0.6, s.length() * fish.getAgeScale());
                    Vec3 d = target.subtract(fish.position());
                    // M30 part c: an eel slides right into its lair (around the decoration)
                    boolean lurker = s.large().swim() == org.overengineer.bettacraft.tankmates.LargeSpecies.Swim.LURK;
                    // (it cannot always get into the last corner of a crevice: after a while it settles where it is)
                    boolean over = d.horizontalDistanceSqr() < len * len * (lurker ? 0.05 : 0.16) || behaviourFlags == TankmateEntity.FLAG_REST
                            || lurker && travel > 300;
                    if (!over) {
                        Vec3 to = sheltered ? steer(server, w, target) : target;
                        move().swimTo(to.x, to.y, to.z, speed);
                        if (++travel < 600) ticks++;
                    } else {
                        move().stop();
                        behaviourFlags = TankmateEntity.FLAG_REST;
                        Vec3 v = fish.getDeltaMovement();
                        fish.setDeltaMovement(v.x * 0.85, Mth.clamp(d.y * 0.15, -0.04, 0.02), v.z * 0.85);
                    }
                    apply();
                    return;
                }
                // M52d: a spot on the glass just above the floor: the fish lies on the floor right under it (its body cannot get
                // lower), so it has arrived once it is there horizontally (it used to keep pushing into the floor, never upright)
                Vec3 off = target.subtract(fish.position());
                // (M52: or it is at the bottom of the water, where TankmateEntity#aiStep holds it without a collision)
                boolean atBottom = fish.verticalCollisionBelow || fish.getY() <= w.box().minY + 1.0 / 16 + 0.005 + 1e-3;
                boolean onFloorBelow = atBottom && off.y < 0 && off.horizontalDistanceSqr() <= arrive;
                if (fish.position().distanceToSqr(target) > arrive && !onFloorBelow) {
                    // M21.5: a kuhli loach creeps into its shelter around the decoration
                    Vec3 to = sheltered ? steer(server, w, target) : target;
                    // M21.5: a little faster while still far away (on the way to the glass it took minutes)
                    double far = fish.position().distanceTo(target);
                    double sp = Features.enabled(Feature.SMOOTH_SWIMMING) && far > 0.6 ? 0.8 : 0.4;
                    // M21.8: a seahorse on its way to a post swims at its (slow) full speed
                    move().swimTo(to.x, to.y, to.z, hitchPost != null ? 1.0 : sheltered ? Math.min(sp, 0.5) : sp);
                    // M21.5: the time on the glass (or resting) starts once it is there (the way does not use it up)
                    if (sp > 0.4 || arrive > 0.004) {
                        if (++travel < 900) ticks++;
                    }
                } else {
                    move().stop();
                    if (arrive > 0.004) fish.setDeltaMovement(target.subtract(fish.position()).scale(0.15));
                    else fish.setDeltaMovement(fish.getDeltaMovement().scale(0.3));
                    if (hitchPost != null) {
                        // M21.8: the seahorse holds on to its post: tail curled round it, facing it
                        hitched = true;
                        behaviourFlags = TankmateEntity.FLAG_REST;
                        fish.setDeltaMovement(target.subtract(fish.position()).scale(0.2));
                        Vec3 d = hitchPost.subtract(fish.position());
                        if (d.horizontalDistanceSqr() > 1e-6) {
                            float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90F;
                            fish.setYRot(Mth.approachDegrees(fish.getYRot(), yaw, 6F));
                            fish.yBodyRot = fish.getYRot();
                            fish.yHeadRot = fish.getYRot();
                        }
                    }
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
        travel = 0;
        hitchPost = null;
        hitched = false;
        RandomSource r = r();
        if (s.seahorse()) {
            seahorse(server, w, s);
            return;
        }
        if (s.isLarge()) {
            large(server, w, s);
            return;
        }
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

    /** M30: the far goal of a cruising big animal (null: none; {@link #target} is the next point on the way to it). */
    @Nullable
    Vec3 largeGoal;

    /**
     * M30: a big animal cruises far ahead: a goal 1.5 to 3.5 body lengths away, mostly within 60 degrees of its heading (so
     * it swims wide curves with its slow turns), in the height band of its zone, kept a body length from the glass; when
     * the glass is right ahead it looks to the sides and back. It gets there around the decoration along the M21 paths.
     */
    private void large(ServerLevel server, TankWaterVolume w, TankmateSpecies s) {
        RandomSource r = r();
        double len = Math.max(1.0, s.length() * fish.getAgeScale());
        double heading = fish.getYRot() * Mth.DEG_TO_RAD;
        Vec3 p = fish.position();
        var l = s.large();
        // M30 part c: a moray lies in its lair most of the time and comes out at night
        if (l.swim() == org.overengineer.bettacraft.tankmates.LargeSpecies.Swim.LURK && lurk(server, w)) return;
        // M30 part c: a schooling big animal (cownose rays) follows the one that leads its school, a little behind and beside it
        if (s.shoaling() && school(server, w, s, len)) return;
        // M30 part b: a stingray (and a night hunter by day) glides down to a spot ahead and lies on the ground for a while
        double rest = l.restShare();
        if (s.nocturnal()) rest *= server.isDay() ? 2.0 : 0.3;
        if (rest > 0 && r.nextDouble() < rest) {
            double ahead = len * (0.5 + r.nextDouble());
            double ang = heading + (r.nextDouble() - 0.5) * Math.PI / 3;
            Vec3 spot = onGround(server, w, p.x - Math.sin(ang) * ahead, p.z + Math.cos(ang) * ahead);
            go(spot, 0.7, Mode.REST);
            ticks = 240 + r.nextInt(l.ray() ? 600 : 360);
            return;
        }
        // M30 part b: an air breather (arapaima) rises to the surface ahead for a breath every few minutes
        if (s.airGulper() && r.nextInt(5) == 0) {
            double ahead = len * (1.5 + r.nextDouble());
            Vec3 at = clamp(w, new Vec3(p.x - Math.sin(heading) * ahead, p.y, p.z + Math.cos(heading) * ahead));
            Vec3 up = w.surfacePoint(at, 0.2, TankmateExtents.surfaceMargin(fish));
            go(steer(server, w, up), 0.9, Mode.CRUISE);
            largeGoal = up;
            ticks = 100 + r.nextInt(60);
            return;
        }
        Vec3 best = null;
        double bestLen = -1;
        for (int i = 0; i < 12; i++) {
            double spread = i < 8 ? Math.PI / 3 : Math.PI;
            double ang = heading + (r.nextDouble() - 0.5) * 2 * spread;
            double dist = len * (1.5 + r.nextDouble() * 2.0);
            Vec3 c = band(w, s, clamp(w, new Vec3(p.x - Math.sin(ang) * dist, p.y + (r.nextDouble() - 0.5) * len * 0.5, p.z + Math.cos(ang) * dist)));
            double got = Math.hypot(c.x - p.x, c.z - p.z);
            if (got < len * 0.8 || got <= bestLen) continue;
            best = c;
            bestLen = got;
            if (got >= len * 1.5) break;
        }
        if (best == null) best = band(w, s, clamp(w, w.box().getCenter()));
        go(steer(server, w, best), 0.8 + r.nextDouble() * 0.3, Mode.CRUISE);
        largeGoal = best;
        ticks = 80 + r.nextInt(80);
    }

    /** M30 part c: the moray's lair (null: not chosen yet). */
    @Nullable
    private Vec3 lair;

    /**
     * M30 part c: a moray keeps one lair: a sheltered spot under the decoration near it (else a spot on the ground along a
     * long wall), and lies there for minutes, its body waving slowly and its mouth breathing; by day it stays in nine of ten
     * decisions, at night in about half and goes cruising otherwise. False: it goes out now.
     */
    private boolean lurk(ServerLevel server, TankWaterVolume w) {
        RandomSource r = r();
        if (r.nextDouble() >= (server.isDay() ? 0.9 : 0.45)) return false;
        if (lair == null || !w.box().inflate(0.05).contains(lair)) {
            TankTerrain t = terrain(server);
            double[] spot = t == null ? null : t.shelter(fish.getX(), fish.getY(), fish.getZ(), 3.0, new java.util.Random(r.nextLong()));
            if (spot != null) {
                Vec3 c = clamp(w, new Vec3(spot[0], spot[1] + 0.01, spot[2]));
                lair = new Vec3(c.x, Math.max(w.box().minY, spot[1] + 0.01), c.z);
            } else {
                var b = w.box();
                double m = Math.max(0.6, fish.species().length() * 0.3);
                // a spot on the ground along a long wall (the clamp keeps it off the glass)
                boolean alongX = b.maxX - b.minX >= b.maxZ - b.minZ;
                double u = 0.25 + 0.5 * r.nextDouble();
                lair = alongX ? onGround(server, w, b.minX + (b.maxX - b.minX) * u, r.nextBoolean() ? b.minZ + m : b.maxZ - m)
                        : onGround(server, w, r.nextBoolean() ? b.minX + m : b.maxX - m, b.minZ + (b.maxZ - b.minZ) * u);
            }
        }
        target = lair;
        mode = Mode.REST;
        ticks = 600 + r.nextInt(900);
        sheltered = true;
        behaviourFlags = 0;
        largeGoal = null;
        return true;
    }

    /**
     * M30 part c: a school of big animals: the leader (lowest id of its kind) cruises as usual; the others swim to a place
     * a body length or so behind and beside it, a little ahead along its heading so they do not brake. False for the leader.
     */
    private boolean school(ServerLevel server, TankWaterVolume w, TankmateSpecies s, double len) {
        TankmateEntity lead = leader(server, w);
        if (lead == null || lead == fish) return false;
        RandomSource r = r();
        if (offset == Vec3.ZERO || r.nextInt(5) == 0) {
            double side = (r.nextBoolean() ? 1 : -1) * len * (0.4 + r.nextDouble() * 0.4);
            offset = new Vec3(side, (r.nextDouble() - 0.5) * len * 0.4, -len * (0.4 + r.nextDouble() * 0.6));
        }
        double h = lead.getYRot() * Mth.DEG_TO_RAD;
        double fx = -Math.sin(h), fz = Math.cos(h); // the leader's heading
        double ahead = len * 0.8 + offset.z;
        Vec3 t = band(w, s, clamp(w, lead.position().add(fx * ahead + fz * offset.x, offset.y, fz * ahead - fx * offset.x)));
        // M39 part c: a follower that fell behind hurries (from 1.5 body lengths) and looks again twice a second, so a turn of
        // the leader does not leave it a whole stale target behind (before: every 1-2 s, 1.1 only from 2 lengths)
        double d = fish.position().distanceTo(t);
        go(steer(server, w, t), d > len * 1.5 ? 1.2 : 0.85, Mode.CRUISE);
        largeGoal = null;
        ticks = 10 + r.nextInt(10);
        return true;
    }

    /** M21.8: the post a seahorse is on its way to or holds on to (null: none). */
    @Nullable
    Vec3 hitchPost;
    /** M21.8: the seahorse has reached its post and holds on to it. */
    boolean hitched;

    /**
     * M21.8: a seahorse mostly holds on to a hitching post (almost always in strong flow), now and then swims a slow,
     * upright tour or - without a post - lies on the bottom.
     */
    private void seahorse(ServerLevel server, TankWaterVolume w, TankmateSpecies s) {
        RandomSource r = r();
        boolean flow = (fish.stressBits() & TankmateCare.STRESS_FLOW) != 0;
        if (r.nextDouble() < (flow ? 0.95 : 0.72) && hitch(server, w, 400 + r.nextInt(800))) return;
        if ((fish.stressBits() & TankmateCare.STRESS_NO_POSTS) != 0 && r.nextDouble() < 0.6) {
            rest(server, w, 160 + r.nextInt(240));
            return;
        }
        go(cruiseTarget(w, s, 0.3, 1.2), 0.6, Mode.CRUISE);
        ticks = 140 + r.nextInt(160);
    }

    /** M21.8: swims to a hitching post nearby (around the decoration); false if the tank has none. */
    private boolean hitch(ServerLevel server, TankWaterVolume w, int duration) {
        var controller = TankInhabitants.resolveController(server, fish);
        if (controller == null) return false;
        List<org.overengineer.bettacraft.tankmates.Seahorses.Spot> spots = org.overengineer.bettacraft.tankmates.Seahorses.spots(server, controller.getTank());
        if (spots.isEmpty()) return false;
        RandomSource r = r();
        org.overengineer.bettacraft.tankmates.Seahorses.Spot best = null;
        double bestD = Double.MAX_VALUE;
        // the nearest of a few random posts (a seahorse does not cross the whole tank for every rest)
        for (int i = 0; i < 6; i++) {
            var sp = spots.get(r.nextInt(spots.size()));
            double d = sp.pos().distanceToSqr(fish.position());
            if (d < bestD) {
                best = sp;
                bestD = d;
            }
        }
        if (best == null) return false;
        target = clamp(w, best.pos());
        hitchPost = best.post();
        mode = Mode.REST;
        ticks = duration;
        sheltered = true; // the way leads around the decoration
        behaviourFlags = 0; // the tail curls once it is there
        return true;
    }

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
        return s.model() == TankmateSpecies.Model.KUHLI && Features.enabled(Feature.SMOOTH_SWIMMING);
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
        Vec3 up = w.surfacePoint(p, 0.2, TankmateExtents.surfaceMargin(fish));
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
            behaviourFlags = Features.enabled(Feature.SMOOTH_SWIMMING) ? 0 : TankmateEntity.FLAG_GLASS;
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
        this.largeGoal = null;
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
        // 1.33.3: never so high that the dorsal fin of a tall fish (angelfish, discus) leaves the water
        double cap = Math.max(floor + 1.0 / 16 + 0.02, w.surface() - TankmateExtents.surfaceMargin(fish) - 0.01);
        hi = Math.min(hi, cap);
        lo = Math.min(lo, cap);
        if (hi < lo) hi = lo;
        return new Vec3(v.x, Mth.clamp(v.y, lo, hi), v.z);
    }

    private Vec3 clamp(TankWaterVolume w, Vec3 v) {
        return w.clamp(v, fish.sideMargin() + 0.015, 1.0 / 16 + 0.02, TankmateExtents.surfaceMargin(fish) + 0.01);
    }

    /** A point just above the ground (substrate, decoration, leaves) at x/z. */
    private Vec3 onGround(ServerLevel server, TankWaterVolume w, double x, double z) {
        Vec3 c = clamp(w, new Vec3(x, w.box().minY, z));
        TankTerrain t = terrain(server);
        double g = t == null ? Double.NaN : t.ground(c.x, c.z);
        if (!Double.isFinite(g)) g = w.box().minY;
        double y = Math.min(g + 0.01, w.surface() - TankmateExtents.surfaceMargin(fish) - 0.01);
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

    /** M52 GameTests: swim to this spot on the glass (wall {@code face} as in {@link #glass}) and cling to it (false before the fish knows its tank). */
    boolean glassAt(Vec3 spot, int face) {
        if (fish.tankWater == null) return false;
        target = spot;
        mode = Mode.GLASS;
        glassFace = face;
        ticks = 600;
        travel = 0;
        sheltered = false;
        hitchPost = null;
        behaviourFlags = Features.enabled(Feature.SMOOTH_SWIMMING) ? 0 : TankmateEntity.FLAG_GLASS;
        return true;
    }

    /** M21.8 GameTests and smoke scenes: hitch to the nearest post now (false if there is none). */
    boolean hitchNow() {
        if (!(fish.level() instanceof ServerLevel server) || fish.tankWater == null) return false;
        travel = 0;
        return hitch(server, fish.tankWater, 2000);
    }

    void freeze() {

        target = fish.position();
        mode = Mode.REST;
        ticks = Integer.MAX_VALUE / 2;
        move().stop();
    }
}
