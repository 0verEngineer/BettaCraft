package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.big.BigAnimals;
import org.overengineer.bettacraft.big.BigCatching;
import org.overengineer.bettacraft.big.DolphinCare;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.equipment.FeederBlockEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModTags;
import org.overengineer.bettacraft.tank.BigTankRules;
import org.overengineer.bettacraft.tank.Salinity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tankmates.Clutch;
import org.overengineer.bettacraft.tankmates.LargeSpecies;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.world.Wildlife;

/**
 * M30 part c: the marine big animals (bamboo shark, blacktip reef shark, spotted eagle ray, cownose ray, honeycomb moray)
 * and the dolphin's food. Each goes into a sea water tank of exactly its size and is refused by one a block too small; the
 * bamboo shark lays egg cases only with room and its pups hatch only with room; the reef shark never rests and eats the
 * reef fish; the open-water rays stay off the ground; cownose rays keep together and spawn wild in small groups; the moray
 * lies in its lair most of the time; a hungry dolphin hunts a small fish and the feeder feeds it raw fish.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BigMarineGameTests {
    private static final String EMPTY = "empty_32x10x32";
    private static final float SEA = Salinity.SEA;

    private BigMarineGameTests() {}

    private static TankBlockEntity sea(GameTestHelper h, int x, int z, int sx, int sy, int sz) {
        return BigTankGameTests.tank(h, x, 1, z, sx, sy, sz, SEA);
    }

    /** Exactly its size (side, side, height) takes it, a block less on one side does not; the message names the size. */
    private static void admits(GameTestHelper h, TankmateSpecies s, int[] fits, int[] small) {
        BigTankRules.Need n = BigAnimals.need(s.large().lengthM());
        h.assertTrue(Math.max(fits[0], fits[2]) == n.longSide() && Math.min(fits[0], fits[2]) == n.shortSide() && fits[1] == n.height(),
                s.id() + ": the test tank is its minimum " + n);
        TankBlockEntity ok = sea(h, fits[3], fits[4], fits[0], fits[1], fits[2]);
        BigTankGameTests.ok(BigTankGameTests.insert(h, ok, BigFreshwaterGameTests.tub(h, s)));
        TankBlockEntity no = sea(h, small[3], small[4], small[0], small[1], small[2]);
        Component e = BigTankGameTests.insert(h, no, BigFreshwaterGameTests.tub(h, s));
        h.assertTrue(BigTankGameTests.says(e, "message.bettacraft.big.too_small"), s.id() + " refused by the smaller tank: " + (e == null ? "accepted" : e.getString()));
        h.assertTrue(e.getString().contains(String.valueOf(n.longSide())), "the message names the size: " + e.getString());
    }

    /** Bamboo shark and cownose ray 12 x 6 x 3, moray 15 x 8 x 4: in, one block less: out. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void smallerSeaGiantsNeedTheirSize(GameTestHelper h) {
        // {sx, sy, sz, x, z}
        admits(h, TankmateSpecies.BAMBOO_SHARK, new int[]{12, 3, 6, 0, 0}, new int[]{11, 3, 6, 13, 0});
        admits(h, TankmateSpecies.COWNOSE_RAY, new int[]{12, 3, 6, 0, 7}, new int[]{12, 2, 6, 13, 7});
        admits(h, TankmateSpecies.HONEYCOMB_MORAY, new int[]{15, 4, 8, 0, 14}, new int[]{15, 4, 7, 16, 14});
        h.succeed();
    }

    /** The reef shark 20 x 10 x 5 (410 water blocks), the eagle ray 18 x 9 x 5. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void reefSharkAndEagleRayNeedBigTanks(GameTestHelper h) {
        admits(h, TankmateSpecies.BLACKTIP_REEF_SHARK, new int[]{20, 5, 10, 0, 0}, new int[]{19, 5, 10, 0, 11});
        admits(h, TankmateSpecies.SPOTTED_EAGLE_RAY, new int[]{18, 5, 9, 0, 22}, new int[]{9, 4, 18, 21, 0});
        h.succeed();
    }

    private static TankmateEntity add(GameTestHelper h, TankBlockEntity c, TankmateSpecies s, boolean female, LifeStage stage) {
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        var b = w.box();
        Vec3 p = w.clamp(new Vec3((b.minX + b.maxX) / 2, b.minY + 0.3, (b.minZ + b.maxZ) / 2), 0.6, 0.1, 0.2);
        TankmateEntity e = TankmateBreeding.spawn(h.getLevel(), c, s, TankmateGenes.wild(s), female, stage, 0, p);
        if (e == null) throw new GameTestAssertException("could not spawn " + s.id());
        e.health().feed();
        return e;
    }

    private static List<TankmateEntity> mates(GameTestHelper h, TankBlockEntity c, TankmateSpecies s) {
        List<TankmateEntity> out = new ArrayList<>();
        for (TankmateEntity m : TankmateBreeding.tankmates(h.getLevel(), c.getTank())) if (s == null || m.species() == s) out.add(m);
        return out;
    }

    private static List<Clutch> cases(TankBlockEntity c) {
        return c.getTank().clutches().stream().filter(k -> k.kind == Clutch.Kind.EGG_CASE).toList();
    }

    /**
     * Bamboo sharks lay egg cases only with room by the size rule: a pair in the smallest tank (12 x 3 x 6, 216 water blocks)
     * lays none; in 16 x 4 x 8 (512: room for three more) they lay cases, one pup each, never more than there is room for;
     * a ripe case hatches into a big pup; a ripe case in a full tank waits and hatches once there is room.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void bambooSharksLayEggCasesOnlyWithRoom(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity small = sea(h, 0, 0, 12, 3, 6);
        TankmateEntity male = add(h, small, TankmateSpecies.BAMBOO_SHARK, false, LifeStage.ADULT);
        add(h, small, TankmateSpecies.BAMBOO_SHARK, true, LifeStage.ADULT);
        h.assertTrue(TankmateBreeding.bigRoom(level, small, TankmateSpecies.BAMBOO_SHARK) == 0, "no room for a third");
        for (int i = 0; i < 10; i++) {
            for (TankmateEntity m : mates(h, small, null)) m.health().feed();
            TankmateBreeding.step(level, small, 2.0);
        }
        h.assertTrue(cases(small).isEmpty(), "no egg cases without room");

        TankBlockEntity big = sea(h, 0, 8, 16, 4, 8);
        add(h, big, TankmateSpecies.BAMBOO_SHARK, false, LifeStage.ADULT);
        add(h, big, TankmateSpecies.BAMBOO_SHARK, true, LifeStage.ADULT);
        int room = TankmateBreeding.bigRoom(level, big, TankmateSpecies.BAMBOO_SHARK);
        h.assertTrue(room == 3, "room for three more: " + room);
        for (int i = 0; i < 40; i++) {
            for (TankmateEntity m : mates(h, big, null)) m.health().feed();
            TankmateBreeding.step(level, big, 1.0);
            if (!cases(big).isEmpty()) break;
        }
        List<Clutch> laid = cases(big);
        h.assertTrue(!laid.isEmpty(), "the female laid egg cases");
        h.assertTrue(laid.size() <= 3, "never more cases than room: " + laid.size());
        for (Clutch k : laid) h.assertTrue(k.count == 1 && k.species == TankmateSpecies.BAMBOO_SHARK, "one pup per case");
        h.assertTrue(Clutch.eggDays(TankmateSpecies.BAMBOO_SHARK) >= 5, "a long time in the case");
        // the adults do not eat the cases: ten days with hungry adults, no hatching yet
        for (Clutch k : laid) k.age = 0;
        TankmateBreeding.step(level, big, 0.5);
        h.assertTrue(cases(big).size() == laid.size(), "the cases are safe from the adults");
        // ripe: they hatch into big pups
        int before = mates(h, big, TankmateSpecies.BAMBOO_SHARK).size();
        for (Clutch k : cases(big)) k.age = k.totalDays();
        TankmateBreeding.step(level, big, 0.05);
        List<TankmateEntity> pups = mates(h, big, TankmateSpecies.BAMBOO_SHARK).stream().filter(m -> m.lifeStage() == LifeStage.FRY).toList();
        h.assertTrue(!pups.isEmpty() && pups.size() <= laid.size(), "pups hatched: " + pups.size());
        h.assertTrue(mates(h, big, TankmateSpecies.BAMBOO_SHARK).size() == before + pups.size(), "the pups count");
        h.assertTrue(BigAnimals.waterOfOthers(TankInhabitants.liveInhabitants(level, big.getTank()), null) <= BigAnimals.capacityBlocks(big.getTank()) + 1e-6,
                "the size rule holds");

        // a ripe case in the full small tank waits, then hatches once the male is gone
        Clutch k = new Clutch(small.getTank().nextClutchId(), TankmateSpecies.BAMBOO_SHARK, Clutch.Kind.EGG_CASE, male.getX(), male.getY(), male.getZ(), 1,
                TankmateGenes.wild(TankmateSpecies.BAMBOO_SHARK), TankmateGenes.wild(TankmateSpecies.BAMBOO_SHARK));
        k.age = k.totalDays() + 1;
        small.getTank().clutches().add(k);
        TankmateBreeding.step(level, small, 0.05);
        h.assertTrue(cases(small).size() == 1 && mates(h, small, TankmateSpecies.BAMBOO_SHARK).size() == 2, "the ripe case waits for room");
        male.discard();
        small.getTank().inhabitants().remove(male.getUUID());
        TankmateBreeding.step(level, small, 0.05);
        h.assertTrue(cases(small).isEmpty(), "with room it hatches");
        h.succeed();
    }

    /** The reef shark eats the reef fish of its tank and, cruising for 500 ticks, never rests. */
    @GameTest(template = EMPTY, timeoutTicks = 700)
    public static void reefSharkKeepsSwimmingAndHunts(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = sea(h, 0, 0, 20, 5, 10);
        BigTankGameTests.ok(BigTankGameTests.insert(h, c, BigFreshwaterGameTests.tub(h, TankmateSpecies.BLACKTIP_REEF_SHARK)));
        TankmateEntity shark = mates(h, c, TankmateSpecies.BLACKTIP_REEF_SHARK).getFirst();
        shark.getRandom().setSeed(30_3L);
        h.assertTrue(TankmateSpecies.BLACKTIP_REEF_SHARK.large().swimsConstantly(), "a constant swimmer");
        for (int i = 0; i < 3; i++) add(h, c, TankmateSpecies.GREEN_CHROMIS, i == 0, LifeStage.ADULT);
        add(h, c, TankmateSpecies.OCELLARIS_CLOWNFISH, true, LifeStage.ADULT);
        TankmateBreeding.step(level, c, 15.0);
        h.assertTrue(mates(h, c, TankmateSpecies.GREEN_CHROMIS).size() + mates(h, c, TankmateSpecies.OCELLARIS_CLOWNFISH).size() <= 1,
                "it ate the reef fish");
        int[] rest = {0};
        double[] path = {0};
        Vec3[] last = {null};
        h.onEachTick(() -> {
            if (shark.flag(TankmateEntity.FLAG_REST)) rest[0]++;
            if (last[0] != null) path[0] += last[0].distanceTo(shark.position());
            last[0] = shark.position();
        });
        h.runAfterDelay(500, () -> {
            h.assertTrue(rest[0] == 0, "it never rests: " + rest[0]);
            h.assertTrue(path[0] > 15, "it keeps swimming: " + path[0]);
            h.assertTrue(TankInhabitants.tankBox(c.getTank()).contains(shark.position()), "in the tank");
            h.succeed();
        });
    }

    /**
     * Open-water rays: an eagle ray (18 x 5 x 9) never lies on the ground and flies in the middle of the water; three cownose
     * rays (16 x 4 x 8) keep together (the followers near the leader on average).
     */
    @GameTest(template = EMPTY, timeoutTicks = 900)
    public static void openWaterRaysFlyAndCownoseRaysSchool(GameTestHelper h) {
        TankBlockEntity a = sea(h, 0, 0, 18, 5, 9);
        BigTankGameTests.ok(BigTankGameTests.insert(h, a, BigFreshwaterGameTests.tub(h, TankmateSpecies.SPOTTED_EAGLE_RAY)));
        TankmateEntity eagle = mates(h, a, TankmateSpecies.SPOTTED_EAGLE_RAY).getFirst();
        eagle.getRandom().setSeed(30_4L);
        h.assertTrue(TankmateSpecies.SPOTTED_EAGLE_RAY.large().swim() == LargeSpecies.Swim.RAY_OPEN, "an open-water ray");
        TankWaterVolume wa = new TankWaterVolume(a.getTank());
        double floor = wa.box().minY, height = wa.surface() - floor;
        TankBlockEntity b = sea(h, 0, 12, 16, 4, 8);
        for (int i = 0; i < 3; i++) BigTankGameTests.ok(BigTankGameTests.insert(h, b, BigFreshwaterGameTests.tub(h, TankmateSpecies.COWNOSE_RAY)));
        List<TankmateEntity> school = mates(h, b, TankmateSpecies.COWNOSE_RAY);
        h.assertTrue(school.size() == 3, "three cownose rays");
        // M39 part c (flaky before): the tubs put the rays at spots and headings of the level's random (shared by every test
        // running at the time), so the paths differed from run to run; now fixed spots, headings and seeds
        Vec3 ca = new TankWaterVolume(a.getTank()).box().getCenter();
        eagle.snapTo(ca.x, ca.y, ca.z, 0F, 0F);
        Vec3 cb = new TankWaterVolume(b.getTank()).box().getCenter();
        for (int i = 0; i < school.size(); i++) {
            school.get(i).snapTo(cb.x - 3 + 3 * i, cb.y, cb.z, 90F, 0F);
            school.get(i).setDeltaMovement(Vec3.ZERO);
            school.get(i).getRandom().setSeed(30_5L + i);
        }
        eagle.setDeltaMovement(Vec3.ZERO);
        eagle.getRandom().setSeed(30_4L);
        double[] eSum = {0}, apart = {0};
        int[] n = {0}, rest = {0};
        h.onEachTick(() -> {
            if (n[0]++ < 150) return; // after they settled from the tubs
            eSum[0] += eagle.getY() - floor;
            if (eagle.flag(TankmateEntity.FLAG_REST)) rest[0]++;
            TankmateEntity lead = school.stream().min((x, y) -> Integer.compare(x.getId(), y.getId())).orElseThrow();
            for (TankmateEntity m : school) if (m != lead) apart[0] += m.distanceTo(lead) / 2;
        });
        h.runAfterDelay(800, () -> {
            int k = Math.max(1, n[0] - 150);
            double avgE = eSum[0] / k, avgApart = apart[0] / k;
            org.slf4j.LoggerFactory.getLogger("bettacraft/m30").info("M30 eagle ray: {} blocks above the ground of {}, rest {}; cownose rays {} blocks from their leader",
                    String.format(java.util.Locale.ROOT, "%.2f", avgE), String.format(java.util.Locale.ROOT, "%.2f", height), rest[0],
                    String.format(java.util.Locale.ROOT, "%.2f", avgApart));
            h.assertTrue(rest[0] == 0, "the eagle ray never lies down");
            h.assertTrue(avgE > height * 0.2, "it flies off the ground: " + avgE);
            h.assertTrue(avgApart < 4.5, "the cownose rays keep together: " + avgApart);
            h.succeed();
        });
    }

    /** A moray in a bare 15 x 4 x 8 tank takes a lair (a corner on the ground) and lies there most of the time. */
    @GameTest(template = EMPTY, timeoutTicks = 1600)
    public static void morayLiesInItsLair(GameTestHelper h) {
        TankBlockEntity c = sea(h, 0, 0, 15, 4, 8);
        BigTankGameTests.ok(BigTankGameTests.insert(h, c, BigFreshwaterGameTests.tub(h, TankmateSpecies.HONEYCOMB_MORAY)));
        TankmateEntity moray = mates(h, c, TankmateSpecies.HONEYCOMB_MORAY).getFirst();
        moray.getRandom().setSeed(30_6L);
        int[] n = {0}, rest = {0};
        double[] path = {0};
        Vec3[] last = {null};
        h.onEachTick(() -> {
            if (n[0]++ < 300) return; // on its way to the lair
            if (moray.flag(TankmateEntity.FLAG_REST)) rest[0]++;
            if (last[0] != null) path[0] += last[0].distanceTo(moray.position());
            last[0] = moray.position();
        });
        h.runAfterDelay(1500, () -> {
            double share = rest[0] / (double) Math.max(1, n[0] - 300);
            org.slf4j.LoggerFactory.getLogger("bettacraft/m30").info("M30 moray: resting {} of the time, path {}",
                    String.format(java.util.Locale.ROOT, "%.2f", share), String.format(java.util.Locale.ROOT, "%.1f", path[0]));
            h.assertTrue(share > 0.5, "mostly in its lair: " + share);
            h.assertTrue(TankInhabitants.tankBox(c.getTank()).contains(moray.position()), "in the tank");
            h.succeed();
        });
    }

    /** Every sea giant has its wild waters (warm oceans, the cownose ray also lukewarm ones); only the cownose ray in groups. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void seaGiantsHaveWildSources(GameTestHelper h) {
        var warm = Wildlife.bigTable(NetCatching.Water.WARM_OCEAN);
        var lukewarm = Wildlife.bigTable(NetCatching.Water.LUKEWARM_OCEAN);
        var river = Wildlife.bigTable(NetCatching.Water.RIVER);
        for (TankmateSpecies s : new TankmateSpecies[]{TankmateSpecies.BAMBOO_SHARK, TankmateSpecies.BLACKTIP_REEF_SHARK, TankmateSpecies.SPOTTED_EAGLE_RAY,
                TankmateSpecies.COWNOSE_RAY, TankmateSpecies.HONEYCOMB_MORAY}) {
            h.assertTrue(warm.containsKey(s) && !river.containsKey(s), s.id() + " in warm oceans, not in rivers");
            h.assertTrue(s.marine(), s.id() + " is a sea animal");
            h.assertTrue(Wildlife.spawnsWild(s), s.id() + " spawns wild");
            h.assertTrue(ModEntities.TANKMATES.get(s).get().is(ModTags.WILD_ANIMALS), s.id() + " is a wild animal");
            h.assertTrue(BigCatching.caught(h.getLevel(), s, NetCatching.random(h.getLevel())) != null, s.id() + " goes into a transport tub");
            int[] g = Wildlife.group(s);
            if (s == TankmateSpecies.COWNOSE_RAY) h.assertTrue(g[0] == 2 && g[1] == 3, "cownose rays in small schools: " + g[0] + "-" + g[1]);
            else h.assertTrue(g[0] == 1 && g[1] == 1, s.id() + " alone");
        }
        h.assertTrue(lukewarm.containsKey(TankmateSpecies.COWNOSE_RAY) && lukewarm.containsKey(TankmateSpecies.BAMBOO_SHARK), "cownose ray and bamboo shark also lukewarm");
        h.succeed();
    }

    /**
     * A dolphin a day without fish hunts a small fish of its tank (a neon, not the big pangasius); a feeder on the tank with
     * raw cod feeds a hungry dolphin (the cod is gone, the dolphin fed).
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void dolphinHuntsSmallFishAndEatsFromTheFeeder(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = BigTankGameTests.tank(h, 0, 1, 0, 32, 8, 16, SEA);
        BigTankGameTests.ok(BigTankGameTests.insert(h, c, BigTankGameTests.dolphinTub(h)));
        Dolphin d = DolphinCare.dolphins(level, c.getTank()).getFirst();
        TankmateEntity neon = add(h, c, TankmateSpecies.GREEN_CHROMIS, true, LifeStage.ADULT);
        TankmateEntity big = add(h, c, TankmateSpecies.BAMBOO_SHARK, true, LifeStage.ADULT);
        org.overengineer.bettacraft.platform.Platform.get().persistentData(d).putLong(DolphinCare.FED, level.getGameTime());
        DolphinCare.step(level, c, 0.05);
        h.assertTrue(neon.isAlive() && !neon.isRemoved(), "a fed dolphin leaves the fish alone");
        org.overengineer.bettacraft.platform.Platform.get().persistentData(d).putLong(DolphinCare.FED, level.getGameTime() - 36000L);
        DolphinCare.step(level, c, 0.05);
        h.assertTrue(!neon.isAlive() || neon.isRemoved(), "the hungry dolphin caught the chromis");
        h.assertTrue(big.isAlive() && !big.isRemoved(), "but not the shark");
        h.assertTrue(DolphinCare.daysHungry(d, level.getGameTime()) < 0.01, "and is fed");

        BlockPos top = new BlockPos(3, 9, 3);
        h.setBlock(top, ModBlocks.FEEDER.get().defaultBlockState());
        FeederBlockEntity feeder = (FeederBlockEntity) level.getBlockEntity(h.absolutePos(top));
        h.assertTrue(feeder != null && feeder.tankBelow() == c, "a feeder on the tank");
        h.assertTrue(feeder.items().isItemValid(0, new ItemStack(Items.COD)), "raw fish goes into the feeder");
        feeder.items().insertItem(0, new ItemStack(Items.COD, 3), false);
        org.overengineer.bettacraft.platform.Platform.get().persistentData(d).putLong(DolphinCare.FED, level.getGameTime() - 24000L);
        feeder.feedNow(level, c);
        h.assertTrue(feeder.items().getStackInSlot(0).getCount() == 2, "one cod for the dolphin: " + feeder.items().getStackInSlot(0).getCount());
        h.assertTrue(DolphinCare.daysHungry(d, level.getGameTime()) < 0.01, "the dolphin ate from the feeder");
        feeder.feedNow(level, c);
        h.assertTrue(feeder.items().getStackInSlot(0).getCount() == 2, "a fed dolphin takes no more");
        h.succeed();
    }
}
