package org.overengineer.bettacraft.tank.plants;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;

/**
 * Pure simulation of the floating plants of one tank (M12, unit-tested): drift, growth, offshoots, coverage, removing
 * and thinning, the nest spot and the compact sync format. One {@link #move} call is one care step (20 ticks).
 * <ul>
 *     <li><b>Drift</b>: a slow random walk (about a block a minute at most), pushed away from where a filter jet hits the
 *     water; plants push each other apart and stay inside the glass of the surface columns.</li>
 *     <li><b>Growth</b>: size grows with light x nutrients x the species speed; a full-grown plant puts out an offshoot
 *     next to itself while the coverage is below the cap. Very dense cover shades the plants (slower growth).</li>
 * </ul>
 */
public final class FloatingPlantSim {
    /** Largest drift per step (blocks per 20 ticks). */
    public static final double MAX_DRIFT = 0.025;
    /** Range of a filter jet on the surface at strength 1 (blocks). */
    public static final double JET_RANGE = 1.6;
    /** Push of a jet right at its impact point (blocks per step). */
    public static final double JET_PUSH = 0.06;
    /** Most a plant is pushed per step by jets and neighbours together. */
    public static final double MAX_PUSH = 0.07;
    /** Size of a new offshoot and what is left of the mother plant. */
    public static final float OFFSHOOT_SIZE = 0.2F, MOTHER_SIZE = 0.6F;
    /** Growth under dense cover (above {@link Params#shadeFrom}). */
    public static final double SHADED_GROWTH = 0.4;

    private FloatingPlantSim() {}

    /**
     * M21.1: a lily pad put down onto others is pushed a little away from them (at most about its radius), so pads lie
     * side by side with only a small overlap instead of on top of each other. Pure (unit tested).
     */
    public static double[] awayFromPads(List<FloatingPlant> plants, double x, double z, double radius) {
        double minD = radius * 1.5;
        double ox = x, oz = z;
        for (int it = 0; it < 6; it++) {
            boolean moved = false;
            for (FloatingPlant q : plants) {
                if (q.type != FloatingPlantType.LILY_PAD) continue;
                double dx = x - q.x, dz = z - q.z, d = Math.sqrt(dx * dx + dz * dz);
                if (d >= minD) continue;
                double push;
                if (d < 1e-4) {
                    // right on top: away in a direction of its own
                    double ang = (q.id * 2.399963) % (Math.PI * 2);
                    dx = Math.cos(ang);
                    dz = Math.sin(ang);
                    push = Math.min(minD, radius);
                } else {
                    dx /= d;
                    dz /= d;
                    push = Math.min(minD - d, radius);
                }
                x += dx * push;
                z += dz * push;
                moved = true;
            }
            if (!moved) break;
        }
        // never further than one radius from where the player aimed
        double mx = x - ox, mz = z - oz, m = Math.sqrt(mx * mx + mz * mz);
        if (m > radius) {
            x = ox + mx / m * radius;
            z = oz + mz / m * radius;
        }
        return new double[] {x, z};
    }

    /**
     * @param growthSpeed multiplier on the growth of the species (0 = no growth)
     * @param maxCoverage share of the surface above which no offshoots form
     * @param shadeFrom   coverage from which the cover counts as dense (warning, shading)
     * @param maxPlants   hard limit of plants per tank
     */
    public record Params(double growthSpeed, double maxCoverage, double shadeFrom, int maxPlants) {
        public static final Params DEFAULT = new Params(1.0, 0.6, 0.5, 256);
    }

    /**
     * @param light     growth factor of the light (0 = dark, 1 = strong light)
     * @param nutrients growth factor of the nutrients in the water (0..1)
     * @param redTarget how red a red root floater turns in this light (0..1)
     */
    public record Env(double light, double nutrients, double redTarget) {}

    /** Where a filter jet hits the water (x, z), its direction on the surface (unit) and its strength (0..1). */
    public record Jet(double x, double z, double dx, double dz, double strength) {}

    /** What a growth step did: offshoots formed and the leaf area taking up waste (blocks^2, weighted by species). */
    public record Growth(int offshoots, double activeArea) {}

    // ------------------------------------------------------------------ light / nutrients

    /** Growth factor of a light level 0..15: nothing below 9, full from 14 (a lit tank light, sunlight). */
    public static double lightFactor(int light) {
        return Math.max(0, Math.min(1, (light - 8) / 6.0));
    }

    /** Red of the red root floater: green at 10 and below, fully red at 14 and above. */
    public static double redTarget(int light) {
        return Math.max(0, Math.min(1, (light - 10) / 4.0));
    }

    /** Nutrient factor of the waste in the water (1 - quality): a little growth even in clean water, full from 20 % waste. */
    public static double nutrientFactor(float quality) {
        double waste = 1 - Math.max(0, Math.min(1, quality));
        return Math.max(0.15, Math.min(1, waste / 0.2));
    }

    // ------------------------------------------------------------------ coverage

    /** Leaf area / surface area (can exceed 1 a little where leaves overlap). */
    public static double coverage(List<FloatingPlant> plants, int area) {
        if (area <= 0) return plants.isEmpty() ? 0 : 1;
        double sum = 0;
        for (FloatingPlant p : plants) sum += p.area();
        return sum / area;
    }

    /** Cover once every plant is full-grown (used for the cap on offshoots). */
    public static double fullCoverage(List<FloatingPlant> plants, int area) {
        if (area <= 0) return plants.isEmpty() ? 0 : 1;
        double sum = 0;
        for (FloatingPlant p : plants) sum += Math.PI * p.type.maxRadius() * p.type.maxRadius();
        return sum / area;
    }

    // ------------------------------------------------------------------ movement

    /** One drift step: random walk, filter jets, pushing apart, glass. */
    public static void move(List<FloatingPlant> plants, PlantSurface surface, List<Jet> jets, RandomGenerator rnd) {
        if (plants.isEmpty() || surface.isEmpty()) return;
        int n = plants.size();
        double[] px = new double[n], pz = new double[n];
        for (int i = 0; i < n; i++) {
            FloatingPlant p = plants.get(i);
            if (!p.type.movable()) continue;
            p.vx = p.vx * 0.85 + rnd.nextGaussian() * 0.006;
            p.vz = p.vz * 0.85 + rnd.nextGaussian() * 0.006;
            double s = Math.hypot(p.vx, p.vz);
            if (s > MAX_DRIFT) {
                p.vx *= MAX_DRIFT / s;
                p.vz *= MAX_DRIFT / s;
            }
            for (Jet j : jets) {
                double range = JET_RANGE * Math.max(0.2, j.strength());
                double dx = p.x - j.x(), dz = p.z - j.z();
                double d = Math.hypot(dx, dz);
                if (d >= range) continue;
                double f = JET_PUSH * j.strength() * (1 - d / range);
                // away from the impact, and along with the current of the jet
                double ax = (d > 1e-6 ? dx / d : j.dx()) + 0.6 * j.dx(), az = (d > 1e-6 ? dz / d : j.dz()) + 0.6 * j.dz();
                double al = Math.hypot(ax, az);
                if (al < 1e-6) continue;
                px[i] += ax / al * f;
                pz[i] += az / al * f;
            }
        }
        // pushing apart (grid of 0.75 blocks: plants are at most 0.6 across)
        Long2ObjectOpenHashMap<List<Integer>> grid = new Long2ObjectOpenHashMap<>();
        for (int i = 0; i < n; i++) {
            FloatingPlant p = plants.get(i);
            grid.computeIfAbsent(cell(p.x, p.z), k -> new ArrayList<>()).add(i);
        }
        for (int i = 0; i < n; i++) {
            FloatingPlant a = plants.get(i);
            int cx = (int) Math.floor(a.x / 0.75), cz = (int) Math.floor(a.z / 0.75);
            for (int gx = cx - 1; gx <= cx + 1; gx++) {
                for (int gz = cz - 1; gz <= cz + 1; gz++) {
                    List<Integer> c = grid.get(((long) gx << 32) | (gz & 0xFFFFFFFFL));
                    if (c == null) continue;
                    for (int j : c) {
                        if (j <= i) continue;
                        FloatingPlant b = plants.get(j);
                        double dx = b.x - a.x, dz = b.z - a.z;
                        double d = Math.hypot(dx, dz);
                        double overlap = a.radius() + b.radius() - d;
                        if (overlap <= 0) continue;
                        if (d < 1e-6) {
                            double ang = (a.id * 0.7548776662 + b.id * 0.5698402910) % 1.0 * Math.PI * 2;
                            dx = Math.cos(ang);
                            dz = Math.sin(ang);
                            d = 1;
                        }
                        double ux = dx / d, uz = dz / d;
                        double push = Math.min(0.05, overlap * 0.25);
                        boolean ma = a.type.movable(), mb = b.type.movable();
                        if (ma && mb) {
                            px[i] -= ux * push;
                            pz[i] -= uz * push;
                            px[j] += ux * push;
                            pz[j] += uz * push;
                        } else if (ma) {
                            px[i] -= ux * push * 2;
                            pz[i] -= uz * push * 2;
                        } else if (mb) {
                            px[j] += ux * push * 2;
                            pz[j] += uz * push * 2;
                        }
                    }
                }
            }
        }
        for (int i = 0; i < n; i++) {
            FloatingPlant p = plants.get(i);
            if (!p.type.movable()) {
                double[] c = surface.clamp(p.x, p.z, margin(p));
                p.x = c[0];
                p.z = c[1];
                continue;
            }
            // pushes (jets, neighbours) add up to at most MAX_PUSH per step
            double push = Math.hypot(px[i], pz[i]);
            if (push > MAX_PUSH) {
                px[i] *= MAX_PUSH / push;
                pz[i] *= MAX_PUSH / push;
            }
            double nx = p.x + p.vx + px[i], nz = p.z + p.vz + pz[i];
            double[] c = surface.clamp(nx, nz, margin(p));
            // bounce softly off the glass
            if (Math.abs(c[0] - nx) > 1e-9) p.vx *= -0.3;
            if (Math.abs(c[1] - nz) > 1e-9) p.vz *= -0.3;
            p.x = c[0];
            p.z = c[1];
        }
    }

    private static long cell(double x, double z) {
        int cx = (int) Math.floor(x / 0.75), cz = (int) Math.floor(z / 0.75);
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    /** Distance a plant keeps from the glass: its leaves stay inside (plus 1/16 for the glass itself). */
    public static double margin(FloatingPlant p) {
        return Math.min(0.45, p.radius() * 0.9 + 1.0 / 16);
    }

    // ------------------------------------------------------------------ growth

    /** Grows the plants by {@code days}; offshoots are added to {@code plants} (ids from {@code nextId}). */
    public static Growth grow(List<FloatingPlant> plants, PlantSurface surface, Env env, Params p, double days, RandomGenerator rnd,
                              IntSupplier nextId) {
        if (plants.isEmpty() || days <= 0) return new Growth(0, 0);
        int area = surface.area();
        double coverage = coverage(plants, area);
        // the cap counts every plant as full-grown: young offshoots grow into the room they will need
        double potential = fullCoverage(plants, area);
        double speed = Math.max(0, p.growthSpeed()) * Math.max(0, env.light()) * Math.max(0, env.nutrients());
        int offshoots = 0;
        double active = 0;
        int count = plants.size();
        for (int i = 0; i < count; i++) {
            FloatingPlant pl = plants.get(i);
            if (pl.type == FloatingPlantType.RED_ROOT_FLOATER) {
                pl.red += (float) ((env.redTarget() - pl.red) * Math.min(1, days / 0.3));
                pl.red = Math.max(0, Math.min(1, pl.red));
            }
            if (!pl.type.grows()) continue;
            active += pl.area() * pl.type.uptake();
            if (speed <= 0) continue;
            double shade = coverage >= p.shadeFrom() ? SHADED_GROWTH : 1.0;
            float before = pl.size;
            pl.size = (float) Math.min(1.0, pl.size + pl.type.growthPerDay() * speed * shade * days);
            coverage += (pl.type.radius(pl.size) * pl.type.radius(pl.size) - pl.type.radius(before) * pl.type.radius(before)) * Math.PI
                    / Math.max(1, area);
            if (pl.size < 1.0F) continue;
            if (potential >= p.maxCoverage() || plants.size() >= p.maxPlants() || surface.isEmpty()) continue;
            double old = pl.area();
            pl.size = MOTHER_SIZE;
            double ang = rnd.nextDouble() * Math.PI * 2;
            FloatingPlant child = new FloatingPlant(nextId.getAsInt(), pl.type, 0, 0, OFFSHOOT_SIZE, (float) (rnd.nextDouble() * Math.PI * 2));
            double dist = (pl.radius() + child.radius()) * 1.05;
            double[] c = surface.clamp(pl.x + Math.cos(ang) * dist, pl.z + Math.sin(ang) * dist, margin(child));
            child.x = c[0];
            child.z = c[1];
            child.red = pl.red;
            plants.add(child);
            coverage += (pl.area() - old + child.area()) / Math.max(1, area);
            potential += Math.PI * child.type.maxRadius() * child.type.maxRadius() / Math.max(1, area);
            offshoots++;
        }
        return new Growth(offshoots, active * Math.max(0, env.light()));
    }

    // ------------------------------------------------------------------ removing

    /** Removes the plants whose leaves reach within {@code reach} of (x, z); returns them. */
    public static List<FloatingPlant> removeNear(List<FloatingPlant> plants, double x, double z, double reach) {
        List<FloatingPlant> out = new ArrayList<>();
        plants.removeIf(p -> {
            boolean hit = Math.hypot(p.x - x, p.z - z) <= reach + p.radius();
            if (hit) out.add(p);
            return hit;
        });
        return out;
    }

    /**
     * The first plant a view ray passes over or under (aquarium net from any angle, e.g. from the side through the glass):
     * walks the ray from the eye in small steps while it is between {@code minY} and {@code maxY} (the water of the tank
     * up to a little above the surface) and returns the first plant whose leaves are within {@code reach} of the ray's
     * x/z there, or null.
     */
    @Nullable
    public static FloatingPlant firstAlongRay(List<FloatingPlant> plants, double ex, double ey, double ez, double lx, double ly, double lz,
                                              double minY, double maxY, double maxDist, double reach) {
        double len = Math.sqrt(lx * lx + ly * ly + lz * lz);
        if (len < 1e-9 || plants.isEmpty()) return null;
        lx /= len;
        ly /= len;
        lz /= len;
        for (double k = 0; k <= maxDist; k += 0.05) {
            double y = ey + ly * k;
            if (y < minY || y > maxY) continue;
            double x = ex + lx * k, z = ez + lz * k;
            FloatingPlant best = null;
            double bestD = Double.MAX_VALUE;
            for (FloatingPlant p : plants) {
                double d = Math.hypot(p.x - x, p.z - z) - p.radius();
                if (d <= reach && d < bestD) {
                    best = p;
                    bestD = d;
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    /** Takes out half of the growing plants (rounded up, picked at random; lily pads stay); returns them. */
    public static List<FloatingPlant> thin(List<FloatingPlant> plants, RandomGenerator rnd) {
        List<FloatingPlant> growing = new ArrayList<>();
        for (FloatingPlant p : plants) if (p.type.grows()) growing.add(p);
        growing.sort(Comparator.comparingInt(p -> p.id));
        for (int i = growing.size() - 1; i > 0; i--) {
            int k = rnd.nextInt(i + 1);
            FloatingPlant t = growing.get(i);
            growing.set(i, growing.get(k));
            growing.set(k, t);
        }
        List<FloatingPlant> out = new ArrayList<>(growing.subList(0, (growing.size() + 1) / 2));
        plants.removeAll(out);
        return out;
    }

    // ------------------------------------------------------------------ nest

    /**
     * Centre (x, z) of the densest cluster of plants the male likes (leaf area x nest favour within 0.75 blocks), the
     * one nearest to {@code nearX/nearZ} on a tie; null without plants.
     */
    @Nullable
    public static double[] nestCluster(List<FloatingPlant> plants, double nearX, double nearZ) {
        double bestScore = -1, bestD = Double.MAX_VALUE;
        double[] best = null;
        for (FloatingPlant a : plants) {
            double score = 0, sx = 0, sz = 0;
            for (FloatingPlant b : plants) {
                if (Math.hypot(a.x - b.x, a.z - b.z) > 0.75) continue;
                double v = b.area() * b.type.nestFavour();
                score += v;
                sx += b.x * v;
                sz += b.z * v;
            }
            if (score <= 0) continue;
            double d = Math.hypot(a.x - nearX, a.z - nearZ);
            if (score > bestScore + 1e-9 || (Math.abs(score - bestScore) <= 1e-9 && d < bestD)) {
                bestScore = score;
                bestD = d;
                best = new double[]{sx / score, sz / score};
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ sync format

    /**
     * Compact client copy: per plant id, species, x/z relative to {@code (ox, oz)} in 1/512 block, size, rotation and
     * red in a byte each (12 bytes per plant).
     */
    public static byte[] encode(List<FloatingPlant> plants, int ox, int oz) {
        int n = Math.min(plants.size(), 65535);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(2 + n * 12);
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeShort(n);
            for (int k = 0; k < n; k++) {
                FloatingPlant p = plants.get(k);
                out.writeInt(p.id);
                out.writeByte(p.type.ordinal());
                out.writeShort(fixed(p.x - ox));
                out.writeShort(fixed(p.z - oz));
                out.writeByte(Math.round(Math.max(0, Math.min(1, p.size)) * 255));
                out.writeByte((int) Math.floorMod(Math.round(p.rot / (Math.PI * 2) * 256), 256L));
                out.writeByte(Math.round(Math.max(0, Math.min(1, p.red)) * 255));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return bytes.toByteArray();
    }

    public static List<FloatingPlant> decode(byte[] data, int ox, int oz) {
        List<FloatingPlant> out = new ArrayList<>();
        if (data == null || data.length < 2) return out;
        FloatingPlantType[] types = FloatingPlantType.values();
        try (DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(data))) {
            int n = in.readUnsignedShort();
            for (int i = 0; i < n; i++) {
                int id = in.readInt();
                int t = in.readUnsignedByte();
                double x = ox + in.readUnsignedShort() / 512.0, z = oz + in.readUnsignedShort() / 512.0;
                float size = in.readUnsignedByte() / 255F;
                float rot = (float) (in.readUnsignedByte() / 256.0 * Math.PI * 2);
                float red = in.readUnsignedByte() / 255F;
                if (t >= types.length) continue;
                FloatingPlant p = new FloatingPlant(id, types[t], x, z, size, rot);
                p.red = red;
                out.add(p);
            }
        } catch (IOException e) {
            // truncated data: keep what was read
        }
        return out;
    }

    /** Red root floater leaves: green in weak light, red in strong light (tint of the greyscale texture). */
    public static final int RRF_GREEN = 0xFF86C25E, RRF_RED = 0xFFC8483A;
    /** Red root floater roots: pale in weak light, red in strong light. */
    public static final int RRF_ROOTS_PALE = 0xFFD8CBB8, RRF_ROOTS_RED = 0xFFB04436;

    /** Client: ARGB tint of the leaves of a red root floater that is {@code red} (0 = green .. 1 = red). */
    public static int redRootLeafTint(float red) {
        return lerpColor(RRF_GREEN, RRF_RED, red);
    }

    /** Client: ARGB tint of the roots of a red root floater that is {@code red}. */
    public static int redRootRootTint(float red) {
        return lerpColor(RRF_ROOTS_PALE, RRF_ROOTS_RED, red);
    }

    static int lerpColor(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /**
     * Client: a new update {@code fresh} replaces {@code old}; plants that were there already glide from where they are
     * drawn now ({@code time}) to their new place over {@code duration} ticks, new ones appear at once.
     */
    public static void adopt(List<FloatingPlant> old, List<FloatingPlant> fresh, long time, double duration) {
        it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<FloatingPlant> byId = new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>();
        for (FloatingPlant p : old) byId.put(p.id, p);
        for (FloatingPlant p : fresh) {
            FloatingPlant o = byId.get(p.id);
            if (o == null || o.type != p.type) continue;
            p.prevX = o.renderX(time, duration);
            p.prevZ = o.renderZ(time, duration);
            p.lerpStart = time;
        }
    }

    private static int fixed(double v) {
        return (int) Math.max(0, Math.min(65535, Math.round(v * 512)));
    }
}
