package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.entity.critter.WildCritterEntity;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.terrarium.HerpEntity;
import org.overengineer.bettacraft.terrarium.HerpSpecies;
import org.overengineer.bettacraft.terrarium.LandWalker;
import org.overengineer.bettacraft.terrarium.WorldClimbing;

/**
 * M62 part 1: the animals in the world. Wild shoaling fish swim as a school (one leader, the others close by; switch
 * {@code tankmates.wildSchools}); reptiles climb one-block steps without a jump, the crested gecko climbs whole walls (over the top,
 * or part of the way to hold on and back down), a shrimp climbs a wall under water (switch {@code visuals.worldClimbing}); with the
 * switches off the old movement. Every test seeds the random of its animals; the switch tests run in their own batches.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorldAnimalsGameTests {
    private static final String EMPTY = "empty_32x10x32";
    private static final String BATCH = "bettacraft_m62_world";

    private WorldAnimalsGameTests() {}

    private static void check(boolean ok, String what) {
        if (!ok) throw new org.overengineer.bettacraft.gametest.framework.GameTestFailure(what);
    }

    /** A pool of water x/z 2..29, y 2..6 (stone floor at y 1, stone rim). */
    private static void pool(GameTestHelper h) {
        for (int x = 1; x <= 30; x++) for (int z = 1; z <= 30; z++) {
            h.setBlock(new BlockPos(x, 1, z), Blocks.STONE.defaultBlockState());
            boolean rim = x == 1 || x == 30 || z == 1 || z == 30;
            for (int y = 2; y <= 7; y++) {
                h.setBlock(new BlockPos(x, y, z), rim ? Blocks.STONE.defaultBlockState() : y <= 6 ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
        }
    }

    /** A floor of grass blocks (y 1) and a stone platform one block higher for x >= 12. */
    private static void step(GameTestHelper h) {
        for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) {
            h.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK.defaultBlockState());
            if (x >= 12) h.setBlock(new BlockPos(x, 2, z), Blocks.STONE.defaultBlockState());
        }
    }

    private static TankmateEntity fish(GameTestHelper h, TankmateSpecies s, double x, double y, double z, long seed) {
        TankmateEntity e = ModEntities.TANKMATES.get(s).get().create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.LOAD);
        if (e == null) throw new org.overengineer.bettacraft.gametest.framework.GameTestFailure("no " + s);
        BlockPos p = h.absolutePos(BlockPos.ZERO);
        e.snapTo(p.getX() + x, p.getY() + y, p.getZ() + z, 0, 0);
        e.getRandom().setSeed(seed);
        e.setPersistenceRequired();
        h.getLevel().addFreshEntity(e);
        return e;
    }

    private static HerpEntity herp(GameTestHelper h, HerpSpecies s, double x, double y, double z, long seed) {
        HerpEntity e = ModEntities.herp(s).create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.LOAD);
        if (e == null) throw new org.overengineer.bettacraft.gametest.framework.GameTestFailure("no " + s.id());
        BlockPos p = h.absolutePos(BlockPos.ZERO);
        e.snapTo(p.getX() + x, p.getY() + y, p.getZ() + z, 0, 0);
        e.getRandom().setSeed(seed);
        e.setPersistenceRequired();
        h.getLevel().addFreshEntity(e);
        e.stopGoalsForTest();
        return e;
    }

    // ------------------------------------------------------------------ schools

    /** Six neon tetras let go apart in a pool come together as one school: the same leader, all close to it, all in the water. */
    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 900)
    public static void wildShoalSwimsAsASchool(GameTestHelper h) {
        pool(h);
        double[][] at = {{8.5, 4, 8.5}, {16.5, 4, 8.5}, {8.5, 4, 16.5}, {16.5, 3, 16.5}, {12.5, 5, 12.5}, {12.5, 3, 8.5}};
        List<TankmateEntity> fish = new ArrayList<>();
        for (int i = 0; i < at.length; i++) fish.add(fish(h, TankmateSpecies.NEON_TETRA, at[i][0], at[i][1], at[i][2], 6200L + i));
        TankmateEntity first = fish.get(0);
        double[] spreadStart = {spread(fish)};
        h.runAfterDelay(20, () -> spreadStart[0] = spread(fish));
        int[] tick = {0};
        h.onEachTick(() -> tick[0]++);
        h.succeedWhen(() -> {
            check(tick[0] > 300, "wait");
            TankmateEntity lead = first.wildSchoolLeader();
            check(lead == first, "the first fish (lowest id) leads: " + (lead == null ? null : lead.getId()) + " / " + first.getId());
            for (TankmateEntity f : fish) {
                check(f.wildSchoolLeader() == lead, "every fish follows the same leader");
                check(!f.isInTank() && f.isInWater(), "in the water of the pool: " + f.position());
                check(f.level().getFluidState(f.blockPosition()).is(FluidTags.WATER), "in a water block");
                check(f.distanceTo(lead) < 3.5, "close to the leader: " + String.format("%.2f", f.distanceTo(lead)));
            }
            check(spread(fish) < spreadStart[0] * 0.6, "the school is closer than at the start: " + spread(fish) + " vs " + spreadStart[0]);
        });
    }

    /** Mean distance of the fish from their centre. */
    private static double spread(List<TankmateEntity> fish) {
        Vec3 c = Vec3.ZERO;
        for (TankmateEntity f : fish) c = c.add(f.position());
        c = c.scale(1.0 / fish.size());
        double d = 0;
        for (TankmateEntity f : fish) d += f.position().distanceTo(c);
        return d / fish.size();
    }

    /** With {@code tankmates.wildSchools} off every wild fish swims for itself (no leader). */
    @GameTest(template = EMPTY, batch = "bettacraft_m62_world_schools_off", timeoutTicks = 300)
    public static void wildSchoolsOffSwimAlone(GameTestHelper h) {
        boolean saved = Features.own(Feature.WILD_SCHOOLS);
        Features.set(Feature.WILD_SCHOOLS, false);
        try {
            pool(h);
            List<TankmateEntity> fish = new ArrayList<>();
            for (int i = 0; i < 4; i++) fish.add(fish(h, TankmateSpecies.NEON_TETRA, 10.5 + i, 4, 10.5, 6210L + i));
            h.runAtTickTime(80, () -> {
                try {
                    for (TankmateEntity f : fish) check(f.wildSchoolLeader() == null, "no school with the switch off");
                } finally {
                    Features.set(Feature.WILD_SCHOOLS, saved);
                }
                h.succeed();
            });
        } catch (RuntimeException ex) {
            Features.set(Feature.WILD_SCHOOLS, saved);
            throw ex;
        }
    }

    // ------------------------------------------------------------------ climbing

    /** What a climber did during a test. */
    private static final class Seen {
        boolean climbed, wallPose, jumped, heldOn;
        double maxY = -1e9;

        void look(HerpEntity e, double ground) {
            if (e.climbingInWorld()) climbed = true;
            if (e.herpPose().wall() != null) wallPose = true;
            if (e.clingingInWorld()) heldOn = true;
            // a jump: off the ground, rising, while not on a face
            if (!e.climbingInWorld() && !e.onGround() && e.getDeltaMovement().y > 0.2) jumped = true;
            maxY = Math.max(maxY, e.getY() - ground);
        }
    }

    /**
     * A leopard gecko, a slider and a corn snake walk to the top of a one-block platform: each climbs the step (wall pose on the
     * way, no jump) and stands on top.
     */
    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 600)
    public static void reptilesClimbAStepWithoutAJump(GameTestHelper h) {
        step(h);
        double ground = h.absolutePos(new BlockPos(0, 2, 0)).getY();
        List<HerpEntity> herps = List.of(herp(h, HerpSpecies.LEOPARD_GECKO, 9.5, 2, 4.5, 6220L),
                herp(h, HerpSpecies.SLIDER, 9.5, 2, 10.5, 6221L), herp(h, HerpSpecies.CORN_SNAKE, 9.5, 2, 16.5, 6222L));
        List<Seen> seen = new ArrayList<>();
        for (HerpEntity e : herps) {
            check(e.worldClimb() == WorldClimbing.Style.STEP, e.species().id() + " climbs steps");
            seen.add(new Seen());
        }
        int[] tick = {0};
        h.onEachTick(() -> {
            tick[0]++;
            for (int i = 0; i < herps.size(); i++) {
                HerpEntity e = herps.get(i);
                seen.get(i).look(e, ground);
                if (!e.climbingInWorld() && e.getNavigation().isDone() && e.getY() < ground + 0.9) {
                    BlockPos to = h.absolutePos(new BlockPos(15, 3, (int) (e.getZ() - h.absolutePos(BlockPos.ZERO).getZ())));
                    e.getNavigation().snapTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, 1.0);
                }
            }
        });
        h.succeedWhen(() -> {
            for (int i = 0; i < herps.size(); i++) {
                HerpEntity e = herps.get(i);
                Seen s = seen.get(i);
                String n = e.species().id();
                check(s.climbed && s.wallPose, n + " climbs the step on the face");
                check(!s.jumped, n + " does not jump");
                check(e.getY() > ground + 0.95 && e.onGround() && !e.climbingInWorld(), n + " stands on the platform: " + e.position());
            }
        });
    }

    /** A crested gecko climbs a four-block wall to its top and over; another climbs two blocks up, holds on and comes down. */
    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 1200)
    public static void crestedGeckoClimbsAWholeWall(GameTestHelper h) {
        for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK.defaultBlockState());
        for (int y = 2; y <= 5; y++) for (int z = 0; z < 32; z++) h.setBlock(new BlockPos(12, y, z), Blocks.STONE.defaultBlockState());
        double ground = h.absolutePos(new BlockPos(0, 2, 0)).getY();
        HerpEntity over = herp(h, HerpSpecies.CRESTED_GECKO, 10.5, 2, 6.5, 6230L);
        HerpEntity part = herp(h, HerpSpecies.CRESTED_GECKO, 10.5, 2, 20.5, 6231L);
        check(over.worldClimb() == WorldClimbing.Style.WALL, "the crested gecko climbs walls");
        part.climbPart(2, 60);
        Seen so = new Seen(), sp = new Seen();
        boolean[] down = {false};
        int[] tick = {0};
        BlockPos base = h.absolutePos(BlockPos.ZERO);
        h.onEachTick(() -> {
            tick[0]++;
            so.look(over, ground);
            sp.look(part, ground);
            for (HerpEntity e : List.of(over, part)) {
                if (!e.climbingInWorld() && e.getY() < ground + 0.5 && !(e == part && sp.heldOn)) {
                    e.getMoveControl().setWantedPosition(base.getX() + 12.5, e.getY(), e.getZ(), 1.0);
                }
            }
            if (sp.heldOn && !part.climbingInWorld() && part.onGround() && part.getY() < ground + 0.1) down[0] = true;
        });
        h.succeedWhen(() -> {
            check(so.climbed && so.wallPose && over.herpPose() != LandWalker.Pose.BURIED, "the gecko climbs the wall on its face");
            check(over.getY() > ground + 3.95 && !over.climbingInWorld() && over.onGround(), "over the top of the wall: " + over.position());
            check(sp.heldOn && sp.maxY > 1.9 && sp.maxY < 2.6, "the other holds on two blocks up: " + sp.maxY);
            check(down[0], "and comes down again: " + part.position());
        });
    }

    /** A Neocaridina crawls into a stone wall under water and climbs it (wall pose, synced), staying under water. */
    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 900)
    public static void shrimpClimbsAWallUnderWater(GameTestHelper h) {
        pool(h);
        for (int y = 2; y <= 7; y++) for (int z = 2; z <= 29; z++) h.setBlock(new BlockPos(12, y, z), Blocks.STONE.defaultBlockState());
        ServerLevel level = h.getLevel();
        BlockPos base = h.absolutePos(BlockPos.ZERO);
        Critter k = new Critter(0, CritterSpecies.NEOCARIDINA, true, CritterGenes.WILD, 0, 0, 0);
        k.age = CritterSpecies.NEOCARIDINA.juvenileDays() + 2;
        WildCritterEntity e = WildCritterEntity.of(level, k.saveAnimal());
        check(e != null, "a wild shrimp");
        e.snapTo(base.getX() + 11.6, base.getY() + 2, base.getZ() + 10.5, 0, 0);
        e.getRandom().setSeed(6240L);
        e.setPersistenceRequired();
        level.addFreshEntity(e);
        double ground = base.getY() + 2;
        check(e.climbsWalls(), "a Neocaridina climbs walls");
        double[] maxY = {0};
        boolean[] pose = {false};
        h.onEachTick(() -> {
            if (!e.onWall()) e.crawlTo(new Vec3(base.getX() + 12.5, e.getY(), base.getZ() + 10.5));
            if (e.climbWall() == Direction.EAST) pose[0] = true;
            maxY[0] = Math.max(maxY[0], e.getY() - ground);
            check(e.getY() < base.getY() + 7, "stays under the water: " + e.position());
        });
        h.succeedWhen(() -> {
            check(pose[0], "on the wall (east)");
            check(maxY[0] > 0.6, "up the wall: " + maxY[0]);
        });
    }

    /** With {@code visuals.worldClimbing} off a leopard gecko does not climb (the vanilla jump takes it up the step). */
    @GameTest(template = EMPTY, batch = "bettacraft_m62_world_climbing_off", timeoutTicks = 400)
    public static void climbingOffJumpsAsBefore(GameTestHelper h) {
        boolean saved = Features.own(Feature.WORLD_CLIMBING);
        Features.set(Feature.WORLD_CLIMBING, false);
        try {
            step(h);
            double ground = h.absolutePos(new BlockPos(0, 2, 0)).getY();
            HerpEntity e = herp(h, HerpSpecies.LEOPARD_GECKO, 9.5, 2, 6.5, 6250L);
            check(e.worldClimb() == WorldClimbing.Style.NONE, "no climbing with the switch off");
            Seen s = new Seen();
            BlockPos base = h.absolutePos(BlockPos.ZERO);
            h.onEachTick(() -> {
                s.look(e, ground);
                if (e.getNavigation().isDone() && e.getY() < ground + 0.9) e.getNavigation().snapTo(base.getX() + 15.5, base.getY() + 3, base.getZ() + 6.5, 1.0);
            });
            h.runAtTickTime(300, () -> {
                try {
                    check(!s.climbed && !s.wallPose, "no climb with the switch off");
                    check(e.getY() > ground + 0.95, "up the step by the vanilla jump: " + e.position());
                } finally {
                    Features.set(Feature.WORLD_CLIMBING, saved);
                }
                h.succeed();
            });
        } catch (RuntimeException ex) {
            Features.set(Feature.WORLD_CLIMBING, saved);
            throw ex;
        }
    }
}
