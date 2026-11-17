package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterMovement;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.critters.TankTerrain;
import org.overengineer.bettacraft.decor.DecorItem;
import org.overengineer.bettacraft.decor.DecorType;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.frog.DwarfFrogEntity;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.entity.betta.FishBagItem;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorObstacles;
import org.overengineer.bettacraft.tank.interior.InteriorPaths;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tank.interior.Substrate;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.tankmates.TankmateSpots;

/**
 * M21: path finding in the tank interior: the dwarf frog walks round a wall of decoration through its gap (never
 * through the stone), small animals walk under an overhang and back out, a pleco male swims into his spawning cave
 * through the mouth; with {@code features.interior.pathfinding} off the old straight lines come back.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PathfindingGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private PathfindingGameTests() {}

    private static InteriorRegion interior(GameTestHelper h, TankBlockEntity c) {
        if (InteriorManager.interiorLevel(h.getLevel().getServer()) == null) throw new GameTestAssertException("no interior dimension");
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
        ServerLevel in = InteriorManager.interiorLevel(h.getLevel().getServer());
        for (int x = 0; x < r.sizeX(); x++) for (int z = 0; z < r.sizeZ(); z++) {
            Substrate.set(in, new BlockPos(r.originX() + x, InteriorRegion.FLOOR_Y, r.originZ() + z), Blocks.SAND.defaultBlockState(), false);
        }
        return r;
    }

    private static void fill(GameTestHelper h, InteriorRegion r, int x0, int y0, int z0, int x1, int y1, int z1) {
        ServerLevel in = InteriorManager.interiorLevel(h.getLevel().getServer());
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) {
            in.setBlock(r.toWorld(x, y, z), Blocks.STONE.defaultBlockState(), 3);
        }
    }

    private static boolean inStone(GameTestHelper h, TankBlockEntity c, Vec3 p) {
        return InteriorObstacles.blocked(h.getLevel().getServer(), c.getTank(), p);
    }

    /** Whether the interior block at the tank-world position is not air (exact, not the coarse obstacle grid). */
    private static boolean solidAt(GameTestHelper h, TankBlockEntity c, InteriorRegion r, Vec3 p) {
        var t = c.getTank();
        // M21.1: the same mapping as the miniature and TankTerrain (0.5 px gap to the glass, interior squeezed into the
        // rest); scaling by r.scale() alone put spots right beside a pillar into it (the flaky crayfish test)
        double m = TankTerrain.MARGIN;
        double fx = (t.sizeX() - 2 * m) / r.sizeX(), fy = (t.sizeY() - 2 * m) / r.sizeY(), fz = (t.sizeZ() - 2 * m) / r.sizeZ();
        int lx = (int) Math.floor((p.x - t.min().getX() - m) / fx), ly = (int) Math.floor((p.y - t.min().getY() - m) / fy),
                lz = (int) Math.floor((p.z - t.min().getZ() - m) / fz);
        if (lx < 0 || ly < 0 || lz < 0 || lx >= r.sizeX() || ly >= r.sizeY() || lz >= r.sizeZ()) return false;
        ServerLevel in = InteriorManager.interiorLevel(h.getLevel().getServer());
        BlockPos bp = r.toWorld(lx, ly, lz);
        return !in.getBlockState(bp).getCollisionShape(in, bp).isEmpty();
    }

    private static DwarfFrogEntity frogIn(GameTestHelper h, TankBlockEntity c) {
        ServerLevel level = h.getLevel();
        DwarfFrogEntity f = ModEntities.DWARF_FROG.get().create(level);
        ItemStack bag = FishBagItem.fillWith(f);
        f.discard();
        var err = TankInhabitants.insert(level, c, bag, ModEntities.DWARF_FROG.get(), null, 0L);
        if (err != null) throw new GameTestAssertException("frog in: " + err.getString());
        for (var e : TankInhabitants.liveInhabitants(level, c.getTank())) if (e instanceof DwarfFrogEntity d) return d;
        throw new GameTestAssertException("no frog in the tank");
    }

    /**
     * A wall of stone across a 3x2x2 tank with a gap at the back: the path leads through the gap, and the frog walks it
     * from the west end to the east end without ever standing in the stone.
     */
    @GameTest(template = EMPTY, timeoutTicks = 1400)
    public static void frogWalksRoundAWallThroughItsGap(GameTestHelper h) {
        TankBlockEntity c = CritterGameTests.tank(h, 2, 2, 2, 3, 2, 2, 12000);
        InteriorRegion r = interior(h, c);
        // the wall: interior x 22..25 over the whole height, open for z >= 24 (the back quarter)
        fill(h, r, 22, 0, 0, 25, r.sizeY() - 1, 23);
        var t = c.getTank();
        TankWaterVolume w = new TankWaterVolume(t);
        double floor = w.box().minY + 0.04;
        Vec3 from = new Vec3(t.min().getX() + 0.5, floor, t.min().getZ() + 0.6);
        Vec3 to = new Vec3(t.min().getX() + 2.5, floor, t.min().getZ() + 0.6);
        h.assertTrue(inStone(h, c, new Vec3(t.min().getX() + 1.5, floor + 0.1, t.min().getZ() + 0.6)), "the wall is an obstacle");
        h.assertFalse(InteriorPaths.straight(h.getLevel().getServer(), t, from, to), "no straight way through the wall");
        List<Vec3> way = InteriorPaths.swim(h.getLevel().getServer(), t, from, to, w.surface());
        h.assertTrue(way != null && way.size() >= 2, "a way round the wall: " + way);
        for (Vec3 p : way) h.assertFalse(solidAt(h, c, r, p), "waypoint in the stone: " + p);
        h.assertTrue(way.stream().anyMatch(p -> p.z > t.min().getZ() + 1.45), "through the gap at the back");

        DwarfFrogEntity frog = frogIn(h, c);
        frog.snapTo(from.x, from.y, from.z, 0, 0);
        frog.setBreathForTest(DwarfFrogEntity.maxBreath());
        AtomicInteger inside = new AtomicInteger();
        frog.goToForTest(to);
        AtomicInteger ticks = new AtomicInteger();
        h.onEachTick(() -> {
            frog.setBreathForTest(DwarfFrogEntity.maxBreath());
            if (ticks.incrementAndGet() % 200 == 0 && frog.position().x < to.x - 0.2) frog.goToForTest(to);
            if (solidAt(h, c, r, frog.position().add(0, 0.05, 0))) {
                if (inside.incrementAndGet() % 5 == 1) org.slf4j.LoggerFactory.getLogger("bettacraft/gametest").info("[pathtest] frog in stone at {} rel {}",
                        frog.position(), frog.position().subtract(t.min().getX(), t.min().getY(), t.min().getZ()));
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(frog.position().x > t.min().getX() + 2.15, "the frog is not past the wall yet: " + frog.position());
            h.assertTrue(inside.get() <= 6, "the frog walked through the stone for " + inside.get() + " ticks");
        });
    }

    /** A stone table over the floor: crayfish find the spot under it, walk there and stand under the roof. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void smallAnimalsWalkUnderAnOverhang(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = CritterGameTests.tank(h, 2, 2, 2, 2, 2, 2, 8000);
        InteriorRegion r = interior(h, c);
        // a table: a roof at interior y 6..7 over x 6..25, z 6..25 on a pillar in the middle
        fill(h, r, 6, 6, 6, 25, 7, 25);
        fill(h, r, 14, 0, 14, 17, 5, 17);
        var t = c.getTank();
        TankTerrain terrain = new TankTerrain(t, level.getServer(), r, true);
        double x = t.min().getX() + 1.0, z = t.min().getZ() + 0.65;
        double top = terrain.ground(x, z), floor = terrain.groundAt(x, terrain.ground(t.min().getX() + 0.15, t.min().getZ() + 0.15) + 0.1, z);
        h.assertTrue(top > floor + 0.3, "under the roof the floor is lower than the top: " + floor + " / " + top);
        SplittableRandom rnd = new SplittableRandom(21);
        double[] spot = terrain.shelter(t.min().getX() + 0.15, terrain.ground(t.min().getX() + 0.15, t.min().getZ() + 0.15), t.min().getZ() + 0.15, 1.5, rnd);
        h.assertTrue(spot != null, "a sheltered spot under the table");
        int under = 0;
        for (int n = 0; n < 6; n++) {
            Critter k = TankCritters.add(level, c, CritterSpecies.DWARF_CRAYFISH, CritterGenes.wild(1, 0, rnd), n % 2 == 0, 30);
            if (k == null) throw new GameTestAssertException("tank full");
            k.x = t.min().getX() + 0.12 + n * 0.02;
            k.z = t.min().getZ() + 0.12;
            k.y = terrain.ground(k.x, k.z);
        }
        for (int sec = 0; sec < 400; sec++) {
            for (Critter k : t.critters()) {
                k.idle = Math.min(k.idle, 1);
                CritterMovement.step(k, terrain, rnd);
            }
        }
        for (Critter k : t.critters()) {
            h.assertFalse(solidAt(h, c, r, new Vec3(k.x, k.y + 0.03, k.z)), "a crayfish stands in the stone: " + k.debug());
        }
        // over a while some of them are under the roof
        for (int sec = 0; sec < 600 && under == 0; sec++) {
            for (Critter k : t.critters()) {
                k.idle = Math.min(k.idle, 1);
                CritterMovement.step(k, terrain, rnd);
                if (k.face == Critter.FLOOR && CritterMovement.covered(terrain, k) && k.y < floor + 0.1) under++;
            }
        }
        h.assertTrue(under > 0, "no crayfish walked under the table");
        // with the feature off they stay on the top of the columns (the old height map)
        boolean saved = Features.own(Feature.PATHFINDING);
        Features.set(Feature.PATHFINDING, false);
        try {
            h.assertTrue(Math.abs(terrain.groundAt(x, floor + 0.05, z) - top) < 1e-6, "off: the top again");
            h.assertTrue(terrain.shelter(x, floor, z, 1.5, rnd) == null, "off: no shelters");
        } finally {
            Features.set(Feature.PATHFINDING, saved);
        }
        h.succeed();
    }

    /** A pleco male behind his cave swims round it and in through the mouth to the spot inside. */
    @GameTest(template = EMPTY, timeoutTicks = 1200)
    public static void plecoSwimsIntoHisCaveThroughTheMouth(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = CritterGameTests.tank(h, 2, 2, 2, 2, 2, 2, 8000);
        c.getTank().setWaterQuality(1F);
        InteriorRegion r = interior(h, c);
        ServerLevel in = InteriorManager.interiorLevel(level.getServer());
        var err = DecorItem.place(in, DecorType.SPAWNING_CAVE, new BlockPos(r.originX() + 17, InteriorRegion.BASE_Y, r.originZ() + 12), Rotation.NONE, null);
        if (err != null) throw new GameTestAssertException("cave: " + err.getString());
        var caves = TankmateSpots.caves(level.getServer(), c.getTank());
        h.assertTrue(caves.size() == 1, "one cave");
        var spot = caves.getFirst();
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        // behind the closed end of the cave
        Vec3 behind = w.clamp(spot.pos().subtract(spot.dir().scale(0.8)).add(0, 0.15, 0), 0.3, 0.1, 0.2);
        TankmateEntity m = TankmateBreeding.spawn(level, c, TankmateSpecies.BRISTLENOSE_PLECO, TankmateGenes.wild(TankmateSpecies.BRISTLENOSE_PLECO),
                false, LifeStage.ADULT, 0, behind);
        if (m == null) throw new GameTestAssertException("no pleco");
        m.health().feed();
        m.setCave(spot.pos(), spot.dir());
        List<Vec3> way = InteriorPaths.swim(level.getServer(), c.getTank(), behind, spot.pos(), w.surface());
        h.assertTrue(way != null, "a way into the cave");
        AtomicInteger inside = new AtomicInteger();
        h.onEachTick(() -> {
            m.holdAt(spot.pos(), level.getGameTime() + 100);
            m.health().feed();
            // (at the spot itself the body may touch the floor of the tube)
            if (m.position().distanceTo(spot.pos()) > 0.2 && solidAt(h, c, r, m.position().add(0, 0.05, 0)) && inside.incrementAndGet() % 4 == 1) {
                var t = c.getTank();
                org.slf4j.LoggerFactory.getLogger("bettacraft/gametest").info("[pathtest] pleco in the wall at rel {} spot rel {}",
                        m.position().subtract(t.min().getX(), t.min().getY(), t.min().getZ()), spot.pos().subtract(t.min().getX(), t.min().getY(), t.min().getZ()));
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(m.position().distanceTo(spot.pos()) < 0.15, "the pleco is not in his cave yet: " + m.position() + " -> " + spot.pos()
                    + " way " + way + " dir " + spot.dir() + " behind " + behind);
            h.assertTrue(inside.get() <= 10, "the pleco swam through the cave wall for " + inside.get() + " ticks");
        });
    }
}
