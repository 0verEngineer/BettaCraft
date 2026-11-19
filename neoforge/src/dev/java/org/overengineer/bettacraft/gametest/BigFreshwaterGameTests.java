package org.overengineer.bettacraft.gametest;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.util.ArrayList;
import java.util.List;
import org.overengineer.bettacraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.gametest.framework.GameTestHolder;
import org.overengineer.bettacraft.gametest.framework.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.big.BigAnimals;
import org.overengineer.bettacraft.big.BigCatching;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.BigTankRules;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tankmates.LargeSpecies;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.world.Wildlife;

/**
 * M30 part b: the freshwater giants (redtail catfish, wels, arapaima, motoro stingray). Each goes into a tank of exactly its
 * size and is refused by one a block too small; the redtail eats small fish and bettas, the stingray small fish but no
 * bettas; the stingray glides near the bottom; the stingray bears live young only with room for them; every giant has its
 * wild waters.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BigFreshwaterGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private BigFreshwaterGameTests() {}

    static ItemStack tub(GameTestHelper h, TankmateSpecies s) {
        ItemStack tub = BigCatching.caught(h.getLevel(), s, NetCatching.random(h.getLevel()));
        if (tub == null) throw new GameTestFailure("no " + s.id());
        return tub;
    }

    /** Exactly its size (side, side, height) takes it, a block less on one side does not; the message names the size. */
    private static void admits(GameTestHelper h, TankmateSpecies s, int[] fits, int[] small) {
        BigTankRules.Need n = BigAnimals.need(s.large().lengthM());
        h.assertTrue(Math.max(fits[0], fits[2]) == n.longSide() && Math.min(fits[0], fits[2]) == n.shortSide() && fits[1] == n.height(),
                s.id() + ": the test tank is its minimum " + n);
        TankBlockEntity ok = BigTankGameTests.tank(h, fits[3], 1, fits[4], fits[0], fits[1], fits[2], 0F);
        BigTankGameTests.ok(BigTankGameTests.insert(h, ok, tub(h, s)));
        TankBlockEntity no = BigTankGameTests.tank(h, small[3], 1, small[4], small[0], small[1], small[2], 0F);
        Component e = BigTankGameTests.insert(h, no, tub(h, s));
        h.assertTrue(BigTankGameTests.says(e, "message.bettacraft.big.too_small"), s.id() + " refused by the smaller tank: " + (e == null ? "accepted" : e.getString()));
        h.assertTrue(e.getString().contains(String.valueOf(n.longSide())), "the message names the size: " + e.getString());
    }

    /** Redtail 15 x 8 x 4, wels 18 x 9 x 5, stingray 8 x 4 x 2: in, one block less: out. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void freshwaterGiantsNeedTheirSize(GameTestHelper h) {
        // {sx, sy, sz, x, z}
        admits(h, TankmateSpecies.REDTAIL_CATFISH, new int[]{15, 4, 8, 0, 0}, new int[]{14, 4, 8, 16, 0});
        admits(h, TankmateSpecies.WELS_CATFISH, new int[]{18, 5, 9, 0, 9}, new int[]{18, 5, 8, 0, 19});
        admits(h, TankmateSpecies.MOTORO_STINGRAY, new int[]{8, 2, 4, 20, 9}, new int[]{8, 1, 4, 20, 14});
        h.succeed();
    }

    /** The arapaima, the biggest: 24 x 12 x 6 and 800 water blocks. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void arapaimaNeedsTheBiggestTank(GameTestHelper h) {
        admits(h, TankmateSpecies.ARAPAIMA, new int[]{24, 6, 12, 0, 0}, new int[]{24, 6, 11, 0, 13});
        h.assertTrue(TankmateSpecies.ARAPAIMA.labyrinth() && TankmateSpecies.ARAPAIMA.airGulper(), "an air breather");
        h.succeed();
    }

    private static TankmateEntity add(GameTestHelper h, TankBlockEntity c, TankmateSpecies s, boolean female, LifeStage stage) {
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        var b = w.box();
        Vec3 p = w.clamp(new Vec3((b.minX + b.maxX) / 2, b.minY + 0.3, (b.minZ + b.maxZ) / 2), 0.6, 0.1, 0.2);
        TankmateEntity e = TankmateBreeding.spawn(h.getLevel(), c, s, TankmateGenes.wild(s), female, stage, 0, p);
        if (e == null) throw new GameTestFailure("could not spawn " + s.id());
        e.health().feed();
        return e;
    }

    private static List<TankmateEntity> mates(GameTestHelper h, TankBlockEntity c, TankmateSpecies s) {
        List<TankmateEntity> out = new ArrayList<>();
        for (TankmateEntity m : TankmateBreeding.tankmates(h.getLevel(), c.getTank())) if (s == null || m.species() == s) out.add(m);
        return out;
    }

    /**
     * A redtail catfish eats a betta and small fish (ten days: three prey); a stingray eats the neons but never the adult
     * betta (its mouth is too small for one).
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void giantsEatSmallFish(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity a = BigTankGameTests.tank(h, 0, 1, 0, 15, 4, 8, 0F);
        add(h, a, TankmateSpecies.REDTAIL_CATFISH, false, LifeStage.ADULT);
        BettaEntity betta = LiveFoodGameTests.betta(h, a, false, 3001L);
        for (int i = 0; i < 2; i++) add(h, a, TankmateSpecies.NEON_TETRA, i == 0, LifeStage.ADULT);
        TankmateBreeding.step(level, a, 10.0); // 0.3 a day: three prey
        h.assertTrue(!betta.isAlive() || betta.isRemoved(), "the redtail ate the betta");
        h.assertTrue(mates(h, a, TankmateSpecies.NEON_TETRA).isEmpty(), "and the neons");
        TankBlockEntity b = BigTankGameTests.tank(h, 0, 1, 12, 8, 2, 4, 0F);
        add(h, b, TankmateSpecies.MOTORO_STINGRAY, true, LifeStage.ADULT);
        BettaEntity safe = LiveFoodGameTests.betta(h, b, false, 3002L);
        for (int i = 0; i < 6; i++) add(h, b, TankmateSpecies.NEON_TETRA, i % 2 == 0, LifeStage.ADULT);
        TankmateBreeding.step(level, b, 10.0);
        h.assertTrue(safe.isAlive() && !safe.isRemoved(), "the stingray leaves the adult betta alone");
        h.assertTrue(mates(h, b, TankmateSpecies.NEON_TETRA).size() <= 3, "but eats neons: " + mates(h, b, TankmateSpecies.NEON_TETRA).size() + " left");
        h.succeed();
    }

    /**
     * A stingray in a 12 x 4 x 6 tank (fixed seed) for 900 ticks: it stays in the lowest part of the water (on average less
     * than 0.4 blocks above the ground, never above the lower half) and still moves around.
     */
    @GameTest(template = EMPTY, timeoutTicks = 1100)
    public static void stingrayStaysNearTheBottom(GameTestHelper h) {
        TankBlockEntity c = BigTankGameTests.tank(h, 0, 1, 0, 12, 4, 6, 0F);
        c.getTank().setWaterTemp(27F);
        c.getTank().setTargetTemp(27F);
        BigTankGameTests.ok(BigTankGameTests.insert(h, c, tub(h, TankmateSpecies.MOTORO_STINGRAY)));
        TankmateEntity ray = mates(h, c, TankmateSpecies.MOTORO_STINGRAY).getFirst();
        ray.getRandom().setSeed(30_2L);
        h.assertTrue(TankmateSpecies.MOTORO_STINGRAY.large().swim() == LargeSpecies.Swim.RAY_BOTTOM, "a bottom ray");
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        double floor = w.box().minY, half = floor + (w.surface() - floor) * 0.5;
        double[] sum = {0}, max = {-1e9}, path = {0};
        int[] n = {0}, rest = {0}, counted = {0};
        boolean[] settled = {false};
        Vec3[] last = {null};
        h.onEachTick(() -> {
            if (!ray.isAlive()) return;
            double above = ray.getY() - floor;
            // M31.1: only once it settled from the tub: it comes in at the middle (the half mark itself) and the first glide
            // down can take longer than 100 ticks
            if (n[0] > 100 && above < 0.5) settled[0] = true;
            if (settled[0]) {
                counted[0]++;
                sum[0] += above;
                max[0] = Math.max(max[0], ray.getY());
                if (ray.flag(TankmateEntity.FLAG_REST)) rest[0]++;
            }
            n[0]++;
            if (last[0] != null) path[0] += last[0].distanceTo(ray.position());
            last[0] = ray.position();
        });
        h.runAfterDelay(1000, () -> {
            h.assertTrue(counted[0] > 500, "it reached the ground early: " + counted[0] + " ticks after");
            double avg = sum[0] / Math.max(1, counted[0]);
            org.slf4j.LoggerFactory.getLogger("bettacraft/m30").info("M30 stingray: average {} blocks above the ground, highest {} (half {}), resting {} ticks, path {}",
                    String.format(java.util.Locale.ROOT, "%.2f", avg), String.format(java.util.Locale.ROOT, "%.2f", max[0] - floor),
                    String.format(java.util.Locale.ROOT, "%.2f", half - floor), rest[0], String.format(java.util.Locale.ROOT, "%.1f", path[0]));
            h.assertTrue(TankInhabitants.tankBox(c.getTank()).contains(ray.position()), "in the tank");
            h.assertTrue(avg < 0.4, "near the ground: " + avg);
            h.assertTrue(max[0] < half, "never above the lower half: " + (max[0] - floor));
            h.assertTrue(path[0] > 2.0, "it glides around: " + path[0]);
            h.succeed();
        });
    }

    /**
     * Stingrays bear live young only with room by the size rule: a pair in the smallest tank (8 x 2 x 4, 64 water blocks,
     * room for two) never gets pregnant; in 10 x 2 x 5 (100 water blocks: room for two more) a pregnant female gives birth
     * to at most two pups, big ones (no breeding box).
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void stingraysBearYoungOnlyWithRoom(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity small = BigTankGameTests.tank(h, 0, 1, 0, 8, 2, 4, 0F);
        small.getTank().setWaterTemp(27F);
        small.getTank().setTargetTemp(27F);
        add(h, small, TankmateSpecies.MOTORO_STINGRAY, false, LifeStage.ADULT);
        TankmateEntity f1 = add(h, small, TankmateSpecies.MOTORO_STINGRAY, true, LifeStage.ADULT);
        h.assertTrue(TankmateBreeding.bigRoom(level, small, TankmateSpecies.MOTORO_STINGRAY) == 0, "no room for a third");
        for (int i = 0; i < 10; i++) {
            for (TankmateEntity m : mates(h, small, null)) m.health().feed();
            TankmateBreeding.step(level, small, 2.0);
        }
        h.assertTrue(!f1.isGravid(), "no young without room");
        TankBlockEntity big = BigTankGameTests.tank(h, 0, 1, 10, 10, 2, 5, 0F);
        big.getTank().setWaterTemp(27F);
        big.getTank().setTargetTemp(27F);
        TankmateEntity male = add(h, big, TankmateSpecies.MOTORO_STINGRAY, false, LifeStage.ADULT);
        TankmateEntity f = add(h, big, TankmateSpecies.MOTORO_STINGRAY, true, LifeStage.ADULT);
        int room = TankmateBreeding.bigRoom(level, big, TankmateSpecies.MOTORO_STINGRAY);
        h.assertTrue(room == 2, "room for two more: " + room);
        for (int i = 0; i < 20 && !f.isGravid(); i++) {
            for (TankmateEntity m : mates(h, big, null)) m.health().feed();
            TankmateBreeding.step(level, big, 1.0);
        }
        h.assertTrue(f.isGravid(), "the female carries young");
        h.assertTrue(TankmateBreeding.gestationDays(TankmateSpecies.MOTORO_STINGRAY) > TankmateBreeding.gestationDays() * 3, "a long pregnancy");
        f.setGravid(TankmateBreeding.gestationDays(TankmateSpecies.MOTORO_STINGRAY) + 0.1, male.genes());
        big.getTank().setBreedingBox(true);
        TankmateBreeding.step(level, big, 0.05);
        List<TankmateEntity> pups = mates(h, big, TankmateSpecies.MOTORO_STINGRAY).stream().filter(m -> m.lifeStage() == LifeStage.FRY).toList();
        h.assertTrue(!pups.isEmpty() && pups.size() <= 2, "one or two pups (room for two): " + pups.size());
        h.assertTrue(TankmateBreeding.boxFry(big.getTank()) == 0, "none in the breeding box");
        List<Entity> live = TankInhabitants.liveInhabitants(level, big.getTank());
        h.assertTrue(BigAnimals.waterOfOthers(live, null) <= BigAnimals.capacityBlocks(big.getTank()) + 1e-6, "the size rule holds");
        h.succeed();
    }

    /** Every giant has its wild waters: the tropical ones in jungle water, the wels (very rare) in rivers; all spawn wild. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void giantsHaveWildSources(GameTestHelper h) {
        var jungle = Wildlife.bigTable(NetCatching.Water.JUNGLE);
        var river = Wildlife.bigTable(NetCatching.Water.RIVER);
        for (TankmateSpecies s : new TankmateSpecies[]{TankmateSpecies.REDTAIL_CATFISH, TankmateSpecies.ARAPAIMA, TankmateSpecies.MOTORO_STINGRAY}) {
            h.assertTrue(jungle.containsKey(s) && !river.containsKey(s), s.id() + " in jungle water only");
        }
        h.assertTrue(river.containsKey(TankmateSpecies.WELS_CATFISH) && !jungle.containsKey(TankmateSpecies.WELS_CATFISH), "the wels in rivers");
        h.assertTrue(Wildlife.bigWeight(TankmateSpecies.WELS_CATFISH, NetCatching.Water.RIVER) < Wildlife.bigWeight(TankmateSpecies.PANGASIUS, NetCatching.Water.RIVER),
                "the wels is rarer than the pangasius");
        for (TankmateSpecies s : new TankmateSpecies[]{TankmateSpecies.REDTAIL_CATFISH, TankmateSpecies.WELS_CATFISH, TankmateSpecies.ARAPAIMA, TankmateSpecies.MOTORO_STINGRAY}) {
            h.assertTrue(Wildlife.spawnsWild(s), s.id() + " spawns wild");
            h.assertTrue(ModEntities.TANKMATES.get(s).get().builtInRegistryHolder().is(ModTags.WILD_ANIMALS), s.id() + " is a wild animal");
            h.assertTrue(BigCatching.caught(h.getLevel(), s, NetCatching.random(h.getLevel())) != null, s.id() + " goes into a transport tub");
        }
        h.succeed();
    }
}
