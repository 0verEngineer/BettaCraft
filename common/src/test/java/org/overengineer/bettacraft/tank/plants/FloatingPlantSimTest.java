package org.overengineer.bettacraft.tank.plants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.tank.TankData;

/** M12: pure rules of the floating plants (surface, drift, growth, coverage, removing, nest spot, sync, save data). */
class FloatingPlantSimTest {
    /** 3x3 surface without the north-east corner (an L seen from above): columns x 0..2, z 0..2 minus (2, 0). */
    private static PlantSurface lShape() {
        PlantSurface s = new PlantSurface(64);
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) if (!(x == 2 && z == 0)) s.add(x, z);
        return s;
    }

    private static boolean insideWithMargin(PlantSurface s, FloatingPlant p, double eps) {
        double m = FloatingPlantSim.margin(p) - eps;
        // the leaf disc (radius ~ margin) must stay over surface columns: test 8 points on its rim
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            if (!s.contains(p.x + Math.cos(a) * m, p.z + Math.sin(a) * m)) return false;
        }
        return s.contains(p.x, p.z);
    }

    @Test
    void clampKeepsPointsOverTheWaterAndOutOfTheNotch() {
        PlantSurface s = lShape();
        assertEquals(8, s.area());
        double[] inNotch = s.clamp(2.5, 0.5, 0.2);
        assertTrue(s.contains(inNotch[0], inNotch[1]), "moved out of the notch");
        double[] outside = s.clamp(-3, 1.5, 0.2);
        assertEquals(0.2, outside[0], 1e-9, "margin from the glass");
        // inside corner of the L at (2, 1): a point right at the corner is pushed out of it
        for (double[] q : new double[][]{{2.05, 0.95}, {2.05, 1.05}, {1.95, 0.95}, {2.1, 1.02}}) {
            double[] corner = s.clamp(q[0], q[1], 0.2);
            double dx = Math.max(0, Math.max(2 - corner[0], corner[0] - 3)), dz = Math.max(0, Math.max(0 - corner[1], corner[1] - 1));
            assertTrue(Math.hypot(dx, dz) >= 0.2 - 1e-9 || (dx >= 0.2 - 1e-9) || (dz >= 0.2 - 1e-9),
                    "keeps the margin from the notch: " + corner[0] + "/" + corner[1]);
        }
        // towards a neighbouring column there is no margin
        double[] edge = s.clamp(0.99, 1.5, 0.2);
        assertEquals(0.99, edge[0], 1e-9);
    }

    @Test
    void driftIsSlowAndStaysOverTheSurface() {
        PlantSurface s = lShape();
        Random rnd = new Random(7);
        List<FloatingPlant> plants = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            double[] p = s.randomPoint(rnd::nextDouble, 0.3);
            plants.add(new FloatingPlant(i + 1, FloatingPlantType.values()[i % 4], p[0], p[1], 0.6F, 0));
        }
        for (int step = 0; step < 600; step++) {
            double[] bx = new double[plants.size()], bz = new double[plants.size()];
            for (int i = 0; i < plants.size(); i++) {
                bx[i] = plants.get(i).x;
                bz[i] = plants.get(i).z;
            }
            FloatingPlantSim.move(plants, s, List.of(), rnd);
            for (int i = 0; i < plants.size(); i++) {
                FloatingPlant p = plants.get(i);
                assertTrue(insideWithMargin(s, p, 1e-6), "step " + step + ": plant " + p.id + " at " + p.x + "/" + p.z + " left the water");
                assertTrue(Math.hypot(p.x - bx[i], p.z - bz[i]) < 0.2, "moves slowly");
            }
        }
    }

    @Test
    void plantsDriftAwayFromAFilterJet() {
        PlantSurface s = new PlantSurface(64);
        for (int x = 0; x < 4; x++) for (int z = 0; z < 4; z++) s.add(x, z);
        double withJet = 0, without = 0;
        for (int run = 0; run < 2; run++) {
            Random rnd = new Random(11);
            List<FloatingPlant> plants = new ArrayList<>();
            for (int i = 0; i < 8; i++) plants.add(new FloatingPlant(i, FloatingPlantType.SALVINIA, 0.5 + rnd.nextDouble() * 1.2, 0.5 + rnd.nextDouble() * 1.2, 0.5F, 0));
            List<FloatingPlantSim.Jet> jets = run == 0 ? List.of(new FloatingPlantSim.Jet(0.4, 1.0, 1, 0, 1.0)) : List.of();
            for (int step = 0; step < 120; step++) FloatingPlantSim.move(plants, s, jets, rnd);
            double d = 0;
            for (FloatingPlant p : plants) d += Math.hypot(p.x - 0.4, p.z - 1.0);
            if (run == 0) withJet = d / plants.size();
            else without = d / plants.size();
        }
        assertTrue(withJet > without + 0.5, "the jet pushes the plants away: " + withJet + " vs " + without);
        assertTrue(withJet > FloatingPlantSim.JET_RANGE * 0.8, "mostly out of the jet's range: " + withJet);
    }

    @Test
    void plantsPushEachOtherApartAndLilyPadsStay() {
        PlantSurface s = new PlantSurface(64);
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) s.add(x, z);
        FloatingPlant lily = new FloatingPlant(1, FloatingPlantType.LILY_PAD, 1.5, 1.5, 1F, 0);
        FloatingPlant a = new FloatingPlant(2, FloatingPlantType.FROGBIT, 1.5, 1.5, 1F, 0);
        FloatingPlant b = new FloatingPlant(3, FloatingPlantType.FROGBIT, 1.5, 1.5, 1F, 0);
        List<FloatingPlant> plants = new ArrayList<>(List.of(lily, a, b));
        Random rnd = new Random(3);
        for (int i = 0; i < 200; i++) FloatingPlantSim.move(plants, s, List.of(), rnd);
        assertEquals(1.5, lily.x, 1e-9, "the lily pad does not move");
        assertEquals(1.5, lily.z, 1e-9);
        assertTrue(Math.hypot(a.x - b.x, a.z - b.z) > (a.radius() + b.radius()) * 0.8, "pushed apart");
        assertTrue(Math.hypot(a.x - lily.x, a.z - lily.z) > (a.radius() + lily.radius()) * 0.8, "pushed off the lily pad");
    }

    @Test
    void growthNeedsLightAndStopsAtTheCoverageCap() {
        PlantSurface s = new PlantSurface(64);
        for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) s.add(x, z);
        AtomicInteger ids = new AtomicInteger(10);
        Random rnd = new Random(5);
        FloatingPlantSim.Params p = new FloatingPlantSim.Params(1.0, 0.6, 0.5, 256);
        List<FloatingPlant> plants = new ArrayList<>(List.of(new FloatingPlant(1, FloatingPlantType.DUCKWEED, 1, 1, 0.5F, 0)));
        // dark: nothing grows
        FloatingPlantSim.Growth dark = FloatingPlantSim.grow(plants, s, new FloatingPlantSim.Env(0, 1, 0), p, 5, rnd, ids::getAndIncrement);
        assertEquals(0, dark.offshoots());
        assertEquals(0.5F, plants.getFirst().size, 1e-6);
        assertEquals(0, dark.activeArea(), 1e-9, "no waste uptake in the dark");
        assertEquals(0, FloatingPlantSim.lightFactor(8), 1e-9);
        assertEquals(1, FloatingPlantSim.lightFactor(15), 1e-9);
        // light: offshoots until the cap
        int offshoots = 0;
        for (int day = 0; day < 200; day++) {
            FloatingPlantSim.Growth g = FloatingPlantSim.grow(plants, s, new FloatingPlantSim.Env(1, 1, 1), p, 0.05, rnd, ids::getAndIncrement);
            offshoots += g.offshoots();
            assertTrue(g.activeArea() > 0);
        }
        assertTrue(offshoots > 3, "offshoots: " + offshoots);
        for (int day = 0; day < 100; day++) FloatingPlantSim.grow(plants, s, new FloatingPlantSim.Env(1, 1, 1), p, 0.05, rnd, ids::getAndIncrement);
        double cov = FloatingPlantSim.coverage(plants, s.area());
        assertTrue(cov >= 0.6 - 0.1 && cov < 0.6 + 0.1, "coverage near the cap once all are grown: " + cov);
        int n = plants.size();
        for (int day = 0; day < 100; day++) FloatingPlantSim.grow(plants, s, new FloatingPlantSim.Env(1, 1, 1), p, 0.05, rnd, ids::getAndIncrement);
        assertEquals(n, plants.size(), "no new offshoots above the cap");
        for (FloatingPlant pl : plants) assertTrue(s.contains(pl.x, pl.z), "offshoots float on the water");
        // growth speed 0 = off
        List<FloatingPlant> one = new ArrayList<>(List.of(new FloatingPlant(1, FloatingPlantType.SALVINIA, 1, 1, 0.5F, 0)));
        FloatingPlantSim.grow(one, s, new FloatingPlantSim.Env(1, 1, 1), new FloatingPlantSim.Params(0, 0.6, 0.5, 256), 10, rnd, ids::getAndIncrement);
        assertEquals(1, one.size());
        assertEquals(0.5F, one.getFirst().size, 1e-6);
    }

    @Test
    void plantLimitAndLilyPadsDoNotGrow() {
        PlantSurface s = new PlantSurface(64);
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) s.add(x, z);
        AtomicInteger ids = new AtomicInteger(10);
        Random rnd = new Random(9);
        List<FloatingPlant> plants = new ArrayList<>(List.of(new FloatingPlant(1, FloatingPlantType.DUCKWEED, 4, 4, 0.9F, 0),
                new FloatingPlant(2, FloatingPlantType.LILY_PAD, 2, 2, 1F, 0)));
        for (int i = 0; i < 400; i++) FloatingPlantSim.grow(plants, s, new FloatingPlantSim.Env(1, 1, 0), new FloatingPlantSim.Params(1, 0.9, 0.9, 5), 0.05, rnd, ids::getAndIncrement);
        assertEquals(5, plants.size(), "hard limit per tank");
        assertEquals(1, plants.stream().filter(p -> p.type == FloatingPlantType.LILY_PAD).count(), "lily pads never multiply");
    }

    @Test
    void redRootFloaterFollowsTheLight() {
        PlantSurface s = new PlantSurface(64).add(0, 0);
        List<FloatingPlant> plants = new ArrayList<>(List.of(new FloatingPlant(1, FloatingPlantType.RED_ROOT_FLOATER, 0.5, 0.5, 0.3F, 0)));
        Random rnd = new Random(1);
        FloatingPlantSim.Params p = new FloatingPlantSim.Params(1, 0.99, 0.99, 1);
        for (int i = 0; i < 40; i++) FloatingPlantSim.grow(plants, s, new FloatingPlantSim.Env(1, 1, FloatingPlantSim.redTarget(15)), p, 0.05, rnd, () -> 2);
        assertTrue(plants.getFirst().red > 0.95F, "red under strong light: " + plants.getFirst().red);
        for (int i = 0; i < 40; i++) FloatingPlantSim.grow(plants, s, new FloatingPlantSim.Env(0.3, 1, FloatingPlantSim.redTarget(10)), p, 0.05, rnd, () -> 2);
        assertTrue(plants.getFirst().red < 0.05F, "green again under weak light: " + plants.getFirst().red);
        assertEquals(0, FloatingPlantSim.redTarget(10), 1e-9);
        assertEquals(1, FloatingPlantSim.redTarget(14), 1e-9);
    }

    @Test
    void nutrientsFollowTheWaste() {
        assertEquals(0.15, FloatingPlantSim.nutrientFactor(1.0F), 1e-6, "a little growth in clean water");
        assertEquals(0.5, FloatingPlantSim.nutrientFactor(0.9F), 1e-6);
        assertEquals(1.0, FloatingPlantSim.nutrientFactor(0.5F), 1e-6);
    }

    @Test
    void removeAndThin() {
        List<FloatingPlant> plants = new ArrayList<>();
        for (int i = 0; i < 9; i++) plants.add(new FloatingPlant(i, FloatingPlantType.SALVINIA, i, 0.5, 0.5F, 0));
        plants.add(new FloatingPlant(20, FloatingPlantType.LILY_PAD, 4.3, 3, 1F, 0));
        List<FloatingPlant> near = FloatingPlantSim.removeNear(plants, 3.05, 0.5, 0.2);
        assertEquals(1, near.size());
        assertEquals(3, near.getFirst().id);
        List<FloatingPlant> thinned = FloatingPlantSim.thin(plants, new Random(2));
        assertEquals(4, thinned.size(), "half of the 8 growing plants");
        assertEquals(5, plants.size(), "4 left plus the lily pad");
        assertTrue(plants.stream().anyMatch(p -> p.type == FloatingPlantType.LILY_PAD), "the lily pad stays");
        assertTrue(FloatingPlantSim.thin(new ArrayList<>(), new Random(1)).isEmpty());
    }

    @Test
    void nestGoesUnderTheDensestFrogbitCluster() {
        assertNull(FloatingPlantSim.nestCluster(List.of(), 0, 0));
        List<FloatingPlant> plants = new ArrayList<>();
        plants.add(new FloatingPlant(1, FloatingPlantType.DUCKWEED, 0.5, 0.5, 0.5F, 0));
        for (int i = 0; i < 4; i++) plants.add(new FloatingPlant(10 + i, FloatingPlantType.FROGBIT, 3 + 0.2 * i, 3, 1F, 0));
        double[] c = FloatingPlantSim.nestCluster(plants, 0.5, 0.5);
        assertNotNull(c);
        assertTrue(Math.abs(c[0] - 3.3) < 0.3 && Math.abs(c[1] - 3) < 0.1, "under the frogbit, not the lone duckweed near the male: " + c[0] + "/" + c[1]);
    }

    @Test
    void syncFormatRoundTripAndGlide() {
        List<FloatingPlant> plants = new ArrayList<>();
        FloatingPlant a = new FloatingPlant(5, FloatingPlantType.RED_ROOT_FLOATER, 103.25, -47.5, 0.7F, 2.0F);
        a.red = 0.6F;
        plants.add(a);
        plants.add(new FloatingPlant(9, FloatingPlantType.LILY_PAD, 100.01, -50, 1F, 0));
        List<FloatingPlant> back = FloatingPlantSim.decode(FloatingPlantSim.encode(plants, 100, -50), 100, -50);
        assertEquals(2, back.size());
        FloatingPlant b = back.getFirst();
        assertEquals(5, b.id);
        assertEquals(FloatingPlantType.RED_ROOT_FLOATER, b.type);
        assertEquals(103.25, b.x, 1.0 / 512);
        assertEquals(-47.5, b.z, 1.0 / 512);
        assertEquals(0.7F, b.size, 0.01);
        assertEquals(2.0F, b.rot, 0.03);
        assertEquals(0.6F, b.red, 0.01);
        // glide: the next update moves the plant from where it is drawn, over 20 ticks
        List<FloatingPlant> next = FloatingPlantSim.decode(FloatingPlantSim.encode(List.of(new FloatingPlant(5, FloatingPlantType.RED_ROOT_FLOATER, 104.25, -47.5, 0.7F, 2.0F)), 100, -50), 100, -50);
        FloatingPlantSim.adopt(back, next, 1000, 20);
        FloatingPlant n = next.getFirst();
        assertEquals(103.25, n.renderX(1000, 20), 0.01, "starts where it was");
        assertEquals(104.25, n.renderX(1020, 20), 0.01, "arrives after 20 ticks");
        double mid = n.renderX(1010, 20);
        assertTrue(mid > 103.4 && mid < 104.1, "in between: " + mid);
        assertTrue(FloatingPlantSim.decode(new byte[0], 0, 0).isEmpty());
    }

    @Test
    void tankDataSavesPlantsAndCopiesThemOnSplit() {
        // tank NBT as TankData#save writes it (save itself needs the item registry for the old heater slots)
        CompoundTag tag = new CompoundTag();
        tag.putLongArray("members", new long[]{BlockPos.asLong(0, 64, 0), BlockPos.asLong(1, 64, 0)});
        tag.putInt("scale", 16);
        net.minecraft.nbt.ListTag pl = new net.minecraft.nbt.ListTag();
        pl.add(new FloatingPlant(1, FloatingPlantType.FROGBIT, 0.5, 0.5, 0.8F, 1F).save());
        pl.add(new FloatingPlant(2, FloatingPlantType.LILY_PAD, 1.5, 0.5, 1F, 0F).save());
        tag.put("plants", pl);
        tag.putInt("next_plant_id", 3);
        TankData back = TankData.load(tag);
        assertEquals(2, back.plants().size());
        assertEquals(FloatingPlantType.LILY_PAD, back.plants().get(1).type);
        assertTrue(back.nextPlantId() >= 3, "ids keep counting");
        // unknown species (newer version) are skipped, not crashing
        CompoundTag bad = tag.copy();
        CompoundTag weird = new CompoundTag();
        weird.putString("type", "giant_kelp");
        weird.putDouble("x", 0.5);
        weird.putDouble("z", 0.5);
        bad.getListOrEmpty("plants").add(weird);
        assertEquals(2, TankData.load(bad).plants().size());
        // a piece of the tank keeps only the plants above its own blocks
        TankData left = TankData.create(List.of(new BlockPos(0, 64, 0)), 16);
        left.copySettingsFrom(back);
        assertEquals(1, left.plants().size());
        assertEquals(FloatingPlantType.FROGBIT, left.plants().getFirst().type);
        // client view carries the plants
        TankData view = TankData.loadClientView(back.saveClientView());
        assertEquals(2, view.plants().size());
        assertEquals(FloatingPlantType.FROGBIT, view.plants().getFirst().type);
        assertFalse(view.plants().get(1).type.movable(), "the lily pad is fixed");
    }

    @Test
    void redRootFloaterTintIsRedUnderStrongAndGreenUnderWeakLight() {
        int strong = FloatingPlantSim.redRootLeafTint((float) FloatingPlantSim.redTarget(15));
        int weak = FloatingPlantSim.redRootLeafTint((float) FloatingPlantSim.redTarget(9));
        int r = (strong >> 16) & 0xFF, g = (strong >> 8) & 0xFF;
        assertTrue(r > g + 80, "red leaves under a tank light: " + Integer.toHexString(strong));
        r = (weak >> 16) & 0xFF;
        g = (weak >> 8) & 0xFF;
        assertTrue(g > r + 40, "green leaves under weak light: " + Integer.toHexString(weak));
        assertEquals(FloatingPlantSim.RRF_GREEN, FloatingPlantSim.redRootLeafTint(0));
        assertEquals(FloatingPlantSim.RRF_RED, FloatingPlantSim.redRootLeafTint(1));
        int half = FloatingPlantSim.redRootLeafTint(0.5F);
        assertTrue(((half >> 16) & 0xFF) > ((FloatingPlantSim.RRF_GREEN >> 16) & 0xFF) && ((half >> 16) & 0xFF) < ((FloatingPlantSim.RRF_RED >> 16) & 0xFF), "in between");
        assertTrue(((FloatingPlantSim.redRootRootTint(1) >> 16) & 0xFF) > ((FloatingPlantSim.redRootRootTint(1) >> 8) & 0xFF) + 80, "red roots");
        assertEquals(0xFF, FloatingPlantSim.redRootLeafTint(2F) >>> 24, "clamped, opaque");
    }

    /**
     * The client glides between the updates of a drifting plant: drawn every frame (3 frames per tick) while the updates
     * come in every second with network jitter (18-22 ticks), the drawn position never jumps, never runs backwards and
     * follows the server within one update.
     */
    @Test
    void glidingIsSmoothWithJitteredUpdates() {
        double speed = 0.025 / 20; // blocks per tick, the fastest random drift
        double serverX = 100.0;
        List<FloatingPlant> client = new ArrayList<>(List.of(new FloatingPlant(3, FloatingPlantType.SALVINIA, serverX, 5, 0.5F, 0)));
        Random jitter = new Random(7);
        long tick = 0, nextUpdate = 20;
        double last = client.getFirst().renderX(0, 20), maxStep = 0, maxLag = 0;
        for (int frame = 0; frame < 3 * 20 * 60; frame++) { // one minute at 60 fps
            double time = frame / 3.0;
            long now = (long) Math.floor(time);
            serverX = 100.0 + speed * time;
            if (now >= nextUpdate && now > tick) {
                // a sync packet arrives: the server position of the last care step (once per second)
                double sent = 100.0 + speed * (Math.floor(now / 20.0) * 20);
                List<FloatingPlant> fresh = new ArrayList<>(List.of(new FloatingPlant(3, FloatingPlantType.SALVINIA, sent, 5, 0.5F, 0)));
                FloatingPlantSim.adopt(client, fresh, now, 20);
                client = fresh;
                nextUpdate = now + 18 + jitter.nextInt(5);
            }
            tick = now;
            double x = client.getFirst().renderX(time, 20);
            double step = x - last;
            assertTrue(step >= -1e-9, "never backwards: " + step + " at frame " + frame);
            maxStep = Math.max(maxStep, step);
            maxLag = Math.max(maxLag, serverX - x);
            last = x;
        }
        assertTrue(maxStep < speed * 2.5, "no jumps: biggest step per frame " + maxStep + " (drift per frame " + speed / 3 + ")");
        assertTrue(maxLag < speed * 70, "follows the server within about one update: lag " + maxLag);
        assertTrue(last > 100.0 + speed * 1100, "moved along: " + last);
    }

    @Test
    void netFindsThePlantAlongTheViewRay() {
        List<FloatingPlant> plants = new ArrayList<>();
        FloatingPlant near = new FloatingPlant(1, FloatingPlantType.FROGBIT, 2.5, 1.2, 1F, 0);
        FloatingPlant far = new FloatingPlant(2, FloatingPlantType.FROGBIT, 2.5, 3.5, 1F, 0);
        plants.add(far);
        plants.add(near);
        // from the side (north), eye under the surface (y 2.0), looking south through the glass
        assertEquals(near, FloatingPlantSim.firstAlongRay(plants, 2.5, 1.6, -1.5, 0, 0, 1, 1.0, 2.5, 12, 0.3), "the first plant along the ray");
        // beside the plants: nothing
        assertNull(FloatingPlantSim.firstAlongRay(plants, 4.5, 1.6, -1.5, 0, 0, 1, 1.0, 2.5, 12, 0.3));
        // high above the tank and looking level: the ray is never between the floor and just above the surface
        assertNull(FloatingPlantSim.firstAlongRay(plants, 2.5, 6.0, -1.5, 0, 0, 1, 1.0, 2.5, 12, 0.3));
        // slanted from above the far side: the far plant comes first
        assertEquals(far, FloatingPlantSim.firstAlongRay(plants, 2.5, 3.0, 5.5, 0, -0.5, -1, 1.0, 2.5, 12, 0.3));
        assertNull(FloatingPlantSim.firstAlongRay(List.of(), 0, 0, 0, 1, 0, 0, 0, 1, 12, 0.3));
    }
}
