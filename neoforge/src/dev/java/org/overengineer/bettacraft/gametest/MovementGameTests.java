package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.critters.TankTerrain;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankInteractions;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.terrarium.Climate;
import org.overengineer.bettacraft.terrarium.Habits;
import org.overengineer.bettacraft.terrarium.HerpEntity;
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.overengineer.bettacraft.terrarium.LandWalker;
import org.overengineer.bettacraft.terrarium.TankClimate;
import org.overengineer.bettacraft.terrarium.TerrariumMode;

/**
 * M54: the movement of the animals made logical ({@link Habits}, switch {@code features.visuals.livelyMovement}): the tempo of
 * the snake with its fast phases, its actions (coiled at rest, the idle actions of a lizard), climbing onto the decoration (a
 * snake onto a rock, a dart frog onto leaves), hiding under it (a leopard gecko), the habits in the world (a basking turtle, a
 * gecko that hides by day, frogs that hop instead of gliding), a betta that waits keeps its height (the lift of its move
 * control, M52.5), and the switch off (the movement of 1.43). Every test seeds the random of its animals.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MovementGameTests {
    private static final String EMPTY = "empty_32x10x32";
    private static final String BATCH = "bettacraft_m54_movement";

    private MovementGameTests() {}

    // ------------------------------------------------------------------ helpers

    private static ServerLevel interior(GameTestHelper h) {
        ServerLevel in = InteriorManager.interiorLevel(h.getLevel().getServer());
        if (in == null) throw new GameTestAssertException("no interior dimension");
        return in;
    }

    private static InteriorRegion region(GameTestHelper h, TankBlockEntity c) {
        return InteriorManager.ensureRegion(h.getLevel(), c);
    }

    /** Fills interior blocks [x0, x1] x [y0, y1] x [z0, z1] (interior coordinates, y from the floor up). */
    private static void fill(GameTestHelper h, InteriorRegion r, int x0, int x1, int y0, int y1, int z0, int z1, BlockState s) {
        ServerLevel in = interior(h);
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) in.setBlock(r.toWorld(x, y, z), s, 2);
    }

    /** A dry 3x3x3 terrarium with a sand floor at a fixed climate. */
    private static TankBlockEntity terrarium(GameTestHelper h, int x, int z) {
        TankBlockEntity c = CritterGameTests.tank(h, x, 2, z, 3, 3, 3, 0);
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        for (int lx = 0; lx < r.sizeX(); lx++) for (int lz = 0; lz < r.sizeZ(); lz++) {
            Substrate.set(in, new BlockPos(r.originX() + lx, InteriorRegion.FLOOR_Y, r.originZ() + lz), Blocks.SAND.defaultBlockState(), false);
        }
        TankClimate.step(h.getLevel(), c, 0.01);
        if (c.getTank().terrariumMode() != TerrariumMode.TERRARIUM) throw new GameTestAssertException("no terrarium: " + c.getTank().terrariumMode());
        c.getTank().setClimate(new Climate.State(26, 26, 26, 60, 0, 0));
        return c;
    }

    /** A 3x3x3 paludarium: land of moss over x >= 16 up to 24 interior blocks, a third of water, humid. */
    private static TankBlockEntity paludarium(GameTestHelper h, int x, int z) {
        TankBlockEntity c = CritterGameTests.tank(h, x, 2, z, 3, 3, 3, 0);
        InteriorRegion r = region(h, c);
        ServerLevel in = interior(h);
        for (int lx = 0; lx < r.sizeX(); lx++) for (int lz = 0; lz < r.sizeZ(); lz++) {
            Substrate.set(in, new BlockPos(r.originX() + lx, InteriorRegion.FLOOR_Y, r.originZ() + lz), Blocks.SAND.defaultBlockState(), false);
        }
        fill(h, r, 16, r.sizeX() - 1, 0, 22, 0, r.sizeZ() - 1, Blocks.DIRT.defaultBlockState());
        fill(h, r, 16, r.sizeX() - 1, 23, 23, 0, r.sizeZ() - 1, Blocks.MOSS_BLOCK.defaultBlockState());
        TankInteractions.changeWater(h.getLevel(), c, 9000);
        TankClimate.step(h.getLevel(), c, 0.01);
        TankData t = c.getTank();
        if (t.terrariumMode() != TerrariumMode.PALUDARIUM) throw new GameTestAssertException("no paludarium: " + t.terrariumMode() + " land " + t.landShare());
        t.setClimate(new Climate.State(24, 24, 24, 90, 0, 0));
        t.setWaterTemp(24F);
        return c;
    }

    private static HerpEntity herp(GameTestHelper h, TankBlockEntity c, HerpSpecies s, long seed) {
        HerpEntity e = HerpGameTests.put(h, c, s, true, 0);
        e.getRandom().setSeed(seed);
        return e;
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new GameTestAssertException(what);
    }

    private static String f(double v) {
        return String.format(Locale.ROOT, "%.4f", v);
    }

    /** Average speed (blocks per tick) over the ticks it moved, and the fastest tick. */
    private static final class Speed {
        Vec3 last;
        double sum, max;
        int moving;

        void sample(Entity e) {
            Vec3 p = e.position();
            if (last != null) {
                double d = Math.hypot(p.x - last.x, p.z - last.z);
                if (d > 1e-4) {
                    sum += d;
                    moving++;
                    max = Math.max(max, d);
                }
            }
            last = p;
        }

        double mean() {
            return moving == 0 ? 0 : sum / moving;
        }
    }

    // ------------------------------------------------------------------ tempo (A1)

    /**
     * The corn snake crawls at its own pace (1.6 times the old gait: about 0.03 blocks a tick) and shoots forward in a fast
     * phase (three times that); with the switch off it crawls at the old 0.02.
     */
    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 400)
    public static void cornSnakeCrawlsAtItsPaceAndShootsForward(GameTestHelper h) {
        TankBlockEntity c = terrarium(h, 2, 2);
        HerpEntity snake = herp(h, c, HerpSpecies.CORN_SNAKE, 5401L);
        TankData t = c.getTank();
        ServerLevel level = h.getLevel();
        Vec3 a = new Vec3(t.min().getX() + 0.3, t.min().getY() + 0.05, t.min().getZ() + 0.3), b = new Vec3(t.min().getX() + 2.7, t.min().getY() + 0.05, t.min().getZ() + 2.7);
        snake.moveTo(a.x, a.y, a.z, 0, 0);
        h.runAfterDelay(1, () -> snake.walker().goTo(snake, level, t, b, 300));
        Speed calm = new Speed(), fast = new Speed();
        int[] tick = {0};
        h.onEachTick(() -> {
            tick[0]++;
            if (tick[0] == 150) {
                snake.walker().goTo(snake, level, t, a, 300);
                snake.walker().dashForTest(40);
            }
            if (tick[0] > 150 && snake.walker().dashing()) fast.sample(snake);
            else if (tick[0] < 150) calm.sample(snake);
            else fast.last = null;
        });
        h.runAfterDelay(260, () -> {
            com.mojang.logging.LogUtils.getLogger().info("[gametest] M54 corn snake: calm {} b/t over {} ticks (max {}), fast {} b/t over {} ticks",
                    f(calm.mean()), calm.moving, f(calm.max), f(fast.mean()), fast.moving);
            check(calm.moving > 40, "the snake crawls: " + calm.moving + " ticks moving");
            check(calm.mean() > 0.026 && calm.mean() < 0.045, "its calm pace about 0.03 blocks a tick: " + f(calm.mean()));
            check(fast.moving > 5 && fast.mean() > 2.2 * calm.mean(), "the fast phase: " + f(fast.mean()) + " against " + f(calm.mean()));
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ actions

    /** A resting snake lies coiled; a leopard gecko that sits still looks around, waves its tail or stretches. */
    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 900)
    public static void snakeCoilsAndGeckoLooksAround(GameTestHelper h) {
        TankBlockEntity c = terrarium(h, 2, 2);
        HerpEntity snake = herp(h, c, HerpSpecies.CORN_SNAKE, 5402L);
        HerpEntity gecko = herp(h, c, HerpSpecies.LEOPARD_GECKO, 5403L);
        snake.walker().forceActivity(LandWalker.Activity.REST);
        gecko.walker().forceActivity(LandWalker.Activity.REST);
        Set<Habits.Action> seen = EnumSet.noneOf(Habits.Action.class);
        boolean[] coiled = {false};
        h.onEachTick(() -> {
            if (snake.action() == Habits.Action.COIL) coiled[0] = true;
            seen.add(gecko.action());
        });
        h.succeedWhen(() -> {
            check(coiled[0], "the resting snake coils up: " + snake.action());
            check(seen.contains(Habits.Action.LOOK) || seen.contains(Habits.Action.TAIL) || seen.contains(Habits.Action.STRETCH),
                    "the gecko does something while it sits: " + seen);
            check(!seen.contains(Habits.Action.COIL) && !seen.contains(Habits.Action.HEAD_UP), "only a lizard's actions: " + seen);
        });
    }

    // ------------------------------------------------------------------ the furnishing (B1)

    /** A snake climbs onto a rock half a block high (higher than its step) and a leopard gecko hides under an overhang. */
    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 1500)
    public static void snakeClimbsTheRockAndGeckoHidesUnderIt(GameTestHelper h) {
        TankBlockEntity c = terrarium(h, 2, 2);
        InteriorRegion r = region(h, c);
        // a rock of 16 x 16 interior blocks, 8 high (half a tank block), and a ledge over the floor beside it (a cave)
        fill(h, r, 24, 39, 0, 7, 16, 31, Blocks.STONE.defaultBlockState());
        fill(h, r, 0, 23, 6, 7, 16, 31, Blocks.STONE.defaultBlockState());
        TankData t = c.getTank();
        HerpEntity snake = herp(h, c, HerpSpecies.CORN_SNAKE, 5404L);
        HerpEntity gecko = herp(h, c, HerpSpecies.LEOPARD_GECKO, 5405L);
        snake.moveTo(t.min().getX() + 0.4, t.min().getY() + 0.05, t.min().getZ() + 0.4, 0, 0);
        gecko.moveTo(t.min().getX() + 0.6, t.min().getY() + 0.05, t.min().getZ() + 2.5, 0, 0);
        h.runAfterDelay(2, () -> {
            snake.walker().forceActivity(LandWalker.Activity.PERCH);
            gecko.walker().forceActivity(LandWalker.Activity.HIDE);
        });
        double floor = t.min().getY();
        h.succeedWhen(() -> {
            check(snake.isAlive() && gecko.isAlive(), "alive");
            check(snake.walker().highestClimb() / 16.0 > 0.45, "the snake up on the rock: highest " + snake.walker().highestClimb() / 16.0);
            TankTerrain ter = gecko.walker().terrain(h.getLevel(), t);
            double top = ter.ground(gecko.getX(), gecko.getZ());
            check(top > gecko.getY() + 0.1 && gecko.getY() < floor + 0.2, "the gecko under the ledge: y " + f(gecko.getY() - floor) + ", above it " + f(top - floor));
        });
    }

    /** A dart frog hops up onto a leafy shrub on the land of a paludarium (higher than its step) and sits there. */
    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 1500)
    public static void dartFrogSitsOnTheLeaves(GameTestHelper h) {
        TankBlockEntity c = paludarium(h, 2, 2);
        InteriorRegion r = region(h, c);
        // a shrub of leaves 5 blocks over the land (24 + 5)
        fill(h, r, 26, 41, 24, 28, 14, 33, Blocks.OAK_LEAVES.defaultBlockState());
        TankData t = c.getTank();
        HerpEntity frog = herp(h, c, HerpSpecies.DART_FROG, 5406L);
        frog.moveTo(t.min().getX() + 2.6, t.min().getY() + 24 / 16.0 * (3 - 2 * TankTerrain.MARGIN) / 3 + 0.05, t.min().getZ() + 0.4, 0, 0);
        h.runAfterDelay(2, () -> frog.walker().forceActivity(LandWalker.Activity.PERCH));
        double land = t.min().getY() + TankTerrain.MARGIN + 24 / 16.0 * (3 - 2 * TankTerrain.MARGIN) / 3;
        h.succeedWhen(() -> {
            check(frog.isAlive(), "alive");
            check(frog.walker().highestClimb() / 16.0 + t.min().getY() > land + 0.2, "on the leaves: highest " + f(frog.walker().highestClimb() / 16.0)
                    + ", the land at " + f(land - t.min().getY()));
        });
    }

    // ------------------------------------------------------------------ in the world (C1)

    /** A floor of grass blocks under the open sky (the structure's floor is at y 0 of the test). */
    private static void meadow(GameTestHelper h) {
        for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK.defaultBlockState());
    }

    private static HerpEntity inWorld(GameTestHelper h, HerpSpecies s, BlockPos at, long seed) {
        HerpEntity e = ModEntities.herp(s).create(h.getLevel());
        if (e == null) throw new GameTestAssertException("no " + s.id());
        BlockPos p = h.absolutePos(at);
        e.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        e.getRandom().setSeed(seed);
        e.setPersistenceRequired();
        h.getLevel().addFreshEntity(e);
        return e;
    }

    /**
     * Outside a tank on a sunny day: a slider basks in the sun, a leopard gecko (active at night) rests under the roof nearby,
     * a dart frog hops on its walk (it used to glide with its legs still).
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m54_world", timeoutTicks = 4000)
    public static void herpsKeepTheirHabitsInTheWorld(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        long time = level.getDayTime();
        level.setDayTime(6000);
        meadow(h);
        // a roof of planks on posts beside the gecko (shade, no sky)
        for (int x = 20; x <= 24; x++) for (int z = 4; z <= 8; z++) h.setBlock(new BlockPos(x, 4, z), Blocks.OAK_PLANKS.defaultBlockState());
        HerpEntity slider = inWorld(h, HerpSpecies.SLIDER, new BlockPos(6, 2, 6), 5407L);
        HerpEntity gecko = inWorld(h, HerpSpecies.LEOPARD_GECKO, new BlockPos(18, 2, 6), 5408L);
        HerpEntity frog = inWorld(h, HerpSpecies.DART_FROG, new BlockPos(8, 2, 20), 5409L);
        boolean[] basked = {false}, hid = {false}, hopped = {false};
        double ground = h.absolutePos(new BlockPos(0, 2, 0)).getY();
        int[] tick = {0};
        h.onEachTick(() -> {
            tick[0]++;
            if (slider.isBasking()) basked[0] = true;
            if (gecko.restingInWorld() && !level.canSeeSky(gecko.blockPosition())) hid[0] = true;
            if (tick[0] % 40 == 1 && frog.getNavigation().isDone()) {
                BlockPos to = h.absolutePos(new BlockPos(tick[0] % 80 == 1 ? 26 : 6, 2, 20));
                frog.getNavigation().moveTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, 1.0);
            }
            if (!frog.onGround() && frog.getY() > ground + 0.08) hopped[0] = true;
        });
        h.succeedWhen(() -> {
            check(basked[0], "the slider basks in the sun");
            check(hid[0], "the gecko rests in the shade by day: " + gecko.position());
            check(hopped[0], "the dart frog hops");
            level.setDayTime(time);
        });
    }

    // ------------------------------------------------------------------ the betta's lift (M52.5)

    /**
     * A betta without goals hovers where it is: its move control lifts it only while the sink of the fish travel is in its
     * motion (before, a betta that waited rose by 0.005 a tick up to the surface).
     */
    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 300)
    public static void aWaitingBettaKeepsItsHeight(GameTestHelper h) {
        TankBlockEntity c = CritterGameTests.tank(h, 2, 2, 2, 3, 3, 3, 27_000);
        BettaData d = BettaData.create(BettaGenotype.randomDomestic(RandomSource.create(54)), true, 0);
        var err = TankInhabitants.insert(h.getLevel(), c, BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d), ModEntities.BETTA.get(), null, false);
        check(err == null, "the betta in: " + (err == null ? "" : err.getString()));
        BettaEntity betta = null;
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) if (e instanceof BettaEntity b) betta = b;
        check(betta != null, "a betta");
        BettaEntity b = betta;
        TankData t = c.getTank();
        h.runAfterDelay(2, () -> {
            b.stopGoalsForTest();
            b.moveTo(t.min().getX() + 1.5, t.min().getY() + 1.2, t.min().getZ() + 1.5, 0, 0);
            b.setDeltaMovement(Vec3.ZERO);
        });
        double[] start = {0};
        h.runAfterDelay(4, () -> start[0] = b.getY());
        h.runAfterDelay(124, () -> {
            double rise = b.getY() - start[0];
            com.mojang.logging.LogUtils.getLogger().info("[gametest] M54 betta drift over 120 ticks: {}", f(rise));
            check(Math.abs(rise) < 0.08, "the betta keeps its height: moved " + f(rise) + " in 6 s");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ the switch

    /**
     * {@code features.visuals.livelyMovement} off: no habits (no actions, no perches or hides, no fast phases), the snake
     * crawls at the old 0.02 blocks a tick and stays uncoiled at rest.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m54_lively_off", timeoutTicks = 400)
    public static void livelyMovementOff(GameTestHelper h) {
        boolean saved = Features.own(Feature.LIVELY_MOVEMENT);
        Features.set(Feature.LIVELY_MOVEMENT, false);
        try {
            TankBlockEntity c = terrarium(h, 2, 2);
            HerpEntity snake = herp(h, c, HerpSpecies.CORN_SNAKE, 5410L);
            HerpEntity resting = herp(h, c, HerpSpecies.CORN_SNAKE, 5411L);
            check(snake.habits() == null && !Habits.enabled(), "no habits with the switch off");
            TankData t = c.getTank();
            snake.moveTo(t.min().getX() + 0.3, t.min().getY() + 0.05, t.min().getZ() + 0.3, 0, 0);
            h.runAfterDelay(1, () -> snake.walker().goTo(snake, h.getLevel(), t, new Vec3(t.min().getX() + 2.7, t.min().getY() + 0.05, t.min().getZ() + 2.7), 300));
            resting.walker().forceActivity(LandWalker.Activity.REST);
            Speed calm = new Speed();
            List<Habits.Action> actions = new ArrayList<>();
            h.onEachTick(() -> {
                calm.sample(snake);
                if (resting.action() != Habits.Action.NONE) actions.add(resting.action());
            });
            h.runAfterDelay(200, () -> {
                try {
                    check(calm.moving > 40 && Math.abs(calm.mean() - HerpSpecies.CORN_SNAKE.gait().walk()) < 0.003,
                            "the old pace: " + f(calm.mean()) + " over " + calm.moving + " ticks");
                    check(calm.max < 0.03, "no fast phase: " + f(calm.max));
                    check(actions.isEmpty(), "no actions: " + actions);
                    h.succeed();
                } finally {
                    Features.set(Feature.LIVELY_MOVEMENT, saved);
                }
            });
        } catch (RuntimeException ex) {
            Features.set(Feature.LIVELY_MOVEMENT, saved);
            throw ex;
        }
    }

    // ------------------------------------------------------------------ census and bench

    /**
     * M54 analysis: the pace of the amphibians and reptiles of the dry terrarium in a tank (a walk across it, 200 ticks, the switch on
     * and off) for meilensteine/m54-analyse.md ({@code [gametest] M54 census ...} lines); the check only that each one moves.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m54_census", timeoutTicks = 600)
    public static void movementCensus(GameTestHelper h) {
        boolean saved = Features.own(Feature.LIVELY_MOVEMENT);
        List<HerpSpecies> land = List.of(HerpSpecies.HERMANN, HerpSpecies.HORNED_FROG, HerpSpecies.LEOPARD_GECKO, HerpSpecies.CRESTED_GECKO, HerpSpecies.CORN_SNAKE);
        List<HerpEntity> on = new ArrayList<>(), off = new ArrayList<>();
        List<Speed> son = new ArrayList<>(), soff = new ArrayList<>();
        int i = 0;
        for (HerpSpecies s : land) {
            if (s == HerpSpecies.HERMANN) continue; // needs a big terrarium (8 blocks): not in a 3x3x3 one
            for (int k = 0; k < 2; k++) {
                TankBlockEntity c = terrarium(h, 1 + i * 4 % 28, 1 + (i * 4 / 28) * 4);
                HerpEntity e = herp(h, c, s, 5420L + i);
                TankData t = c.getTank();
                e.moveTo(t.min().getX() + 0.3, t.min().getY() + 0.05, t.min().getZ() + 0.3, 0, 0);
                int delay = k == 0 ? 1 : 201;
                h.runAfterDelay(delay, () -> e.walker().goTo(e, h.getLevel(), t, new Vec3(t.min().getX() + 2.7, t.min().getY() + 0.05, t.min().getZ() + 2.7), 400));
                (k == 0 ? on : off).add(e);
                (k == 0 ? son : soff).add(new Speed());
                i++;
            }
        }
        int[] tick = {0};
        h.onEachTick(() -> {
            tick[0]++;
            if (tick[0] > 400) {
                Features.set(Feature.LIVELY_MOVEMENT, saved);
                return;
            }
            // the switch is global: the "on" animals walk in the first 200 ticks, the "off" ones in the next 200
            boolean first = tick[0] <= 200;
            Features.set(Feature.LIVELY_MOVEMENT, first);
            for (int j = 0; j < on.size(); j++) {
                if (first) son.get(j).sample(on.get(j));
                else soff.get(j).sample(off.get(j));
            }
        });
        h.runAfterDelay(405, () -> {
            Features.set(Feature.LIVELY_MOVEMENT, saved);
            for (int j = 0; j < on.size(); j++) {
                HerpEntity e = on.get(j);
                com.mojang.logging.LogUtils.getLogger().info("[gametest] M54 census {}: tank on {} b/t (moving {} ticks, max {}), off {} b/t (moving {} ticks)",
                        e.species().id(), f(son.get(j).mean()), son.get(j).moving, f(son.get(j).max), f(soff.get(j).mean()), soff.get(j).moving);
                check(son.get(j).moving > 0 && soff.get(j).moving > 0, e.species().id() + " moves");
            }
            h.succeed();
        });
    }

    /**
     * M54 bench (like M52): the server time of 24 amphibians and reptiles in tanks with the habits on and off (a tick of each,
     * 200 times); they must stay cheap with many animals.
     */
    @GameTest(template = EMPTY, batch = "bettacraft_m54_bench", timeoutTicks = 400)
    public static void benchmarkLivelyMovement(GameTestHelper h) {
        boolean saved = Features.own(Feature.LIVELY_MOVEMENT);
        try {
            List<HerpEntity> all = new ArrayList<>();
            HerpSpecies[] kinds = {HerpSpecies.CORN_SNAKE, HerpSpecies.LEOPARD_GECKO, HerpSpecies.CRESTED_GECKO, HerpSpecies.HORNED_FROG};
            for (int i = 0; i < 12; i++) {
                TankBlockEntity c = terrarium(h, 1 + (i % 4) * 7, 1 + (i / 4) * 9);
                InteriorRegion r = region(h, c);
                fill(h, r, 24, 39, 0, 7, 16, 31, Blocks.STONE.defaultBlockState());
                fill(h, r, 0, 23, 6, 7, 16, 31, Blocks.STONE.defaultBlockState());
                for (int k = 0; k < 2; k++) all.add(herp(h, c, kinds[(i + k) % kinds.length], 5440L + i * 2 + k));
            }
            double[] ms = new double[2];
            for (int pass = 0; pass < 2; pass++) {
                Features.set(Feature.LIVELY_MOVEMENT, pass == 0);
                for (int w = 0; w < 40; w++) for (HerpEntity e : all) e.tick(); // warm up
                long t0 = System.nanoTime();
                for (int k = 0; k < 200; k++) for (HerpEntity e : all) e.tick();
                ms[pass] = (System.nanoTime() - t0) / 1e6 / 200;
            }
            com.mojang.logging.LogUtils.getLogger().info("[gametest] benchmark: M54 24 amphibians and reptiles in 12 terraria: lively {} ms per tick, off {} ms per tick",
                    String.format(Locale.ROOT, "%.3f", ms[0]), String.format(Locale.ROOT, "%.3f", ms[1]));
            check(ms[0] < 5.0, "24 animals stay below 5 ms per tick: " + ms[0]);
            check(ms[0] < ms[1] * 3 + 0.5, "the habits cost little more than the old walk: " + ms[0] + " against " + ms[1]);
            h.succeed();
        } finally {
            Features.set(Feature.LIVELY_MOVEMENT, saved);
        }
    }
}
