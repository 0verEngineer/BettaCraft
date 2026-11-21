package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.NetCatching;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateCare;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.reef.ClownLarvae;
import org.overengineer.bettacraft.reef.Copepods;
import org.overengineer.bettacraft.reef.ReefCare;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankCare;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tank.interior.InteriorManager;
import org.overengineer.bettacraft.tank.interior.InteriorRegion;
import org.overengineer.bettacraft.tankmates.Clutch;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M48 part b: the new reef animals and the clownfish larvae: the mandarin lives on the copepods of the live rock and starves
 * without them (fed food does not help), the Banggai male broods the eggs in his mouth and releases juveniles, the fire shrimp
 * comes from the net and the trader and cleans; the larvae of the clownfish survive with rotifers (phytoplankton keeps them
 * alive) and only rarely without, an old clutch loads; the switch {@code features.saltwater.larvaRearing} off brings back the
 * old one-day larvae.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ReefBreedingGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private ReefBreedingGameTests() {}

    private static void check(boolean ok, String what) {
        if (!ok) throw new GameTestAssertException(what);
    }

    private static TankBlockEntity reef(GameTestHelper h, int x, int z) {
        TankBlockEntity c = SeahorseGameTests.tank(h, x, 2, z, 2, 2, 2, 8000, 35F, 26F);
        c.getTank().setWaterQuality(1F);
        check(c.getTank().marine(), "salt water");
        return c;
    }

    private static TankmateEntity add(GameTestHelper h, TankBlockEntity c, TankmateSpecies s, boolean female, LifeStage stage) {
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        var r = h.getLevel().getRandom();
        Vec3 p = w.clamp(new Vec3(w.box().minX + 0.4 + r.nextDouble() * 1.2, w.box().minY + 0.4, w.box().minZ + 0.4 + r.nextDouble() * 1.2), 0.3, 0.1, 0.2);
        TankmateEntity e = TankmateBreeding.spawn(h.getLevel(), c, s, TankmateGenes.wild(s), female, stage, 0, p);
        if (e == null) throw new GameTestAssertException("could not spawn " + s.id());
        e.health().feed();
        return e;
    }

    private static List<TankmateEntity> mates(GameTestHelper h, TankBlockEntity c, TankmateSpecies s) {
        List<TankmateEntity> out = new ArrayList<>();
        for (TankmateEntity m : TankmateBreeding.tankmates(h.getLevel(), c.getTank())) if (m.species() == s) out.add(m);
        return out;
    }

    private static boolean logged(TankData t, String key) {
        return t.log().stream().anyMatch(e -> e.key().equals(key));
    }

    /** {@code n} blocks of live rock on the floor of the interior. */
    private static void liveRock(GameTestHelper h, TankBlockEntity c, int n) {
        if (InteriorManager.interiorLevel(h.getLevel().getServer()) == null) throw new GameTestAssertException("no interior dimension");
        InteriorRegion r = InteriorManager.ensureRegion(h.getLevel(), c);
        ServerLevel in = InteriorManager.interiorLevel(h.getLevel().getServer());
        int placed = 0;
        for (int y = 0; y < 8 && placed < n; y++)
            for (int x = 0; x < r.sizeX() && placed < n; x++)
                for (int z = 0; z < r.sizeZ() && placed < n; z++, placed++) in.setBlock(r.toWorld(x, y, z), ModBlocks.LIVE_ROCK.get().defaultBlockState(), 3);
    }

    // ------------------------------------------------------------------ the mandarin

    /**
     * Two reef tanks, one with a pile of live rock: the copepods grow there (and not in the other); a mandarin in each, offered
     * pellets and artemia every day: he takes none of it; on the rock he stays fed, in the bare tank he goes hungry and dies.
     */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void mandarinLivesOnCopepodsAndStarvesWithout(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity rock = reef(h, 2, 2), bare = reef(h, 8, 2);
        liveRock(h, rock, 1024);
        InteriorManager.ensureRegion(level, bare);
        double cap = Copepods.capacity(level, rock.getTank());
        check(cap > 4 && Copepods.capacity(level, bare.getTank()) == 0, "the rock holds copepods: " + cap);
        // the rock matures for a week
        for (int i = 0; i < 70; i++) Copepods.step(level, rock, 0.1);
        check(rock.getTank().copepods() > cap * 0.5, "the copepods grew: " + rock.getTank().copepods() + " of " + cap);
        TankmateEntity fed = add(h, rock, TankmateSpecies.MANDARINFISH, false, LifeStage.ADULT);
        TankmateEntity starved = add(h, bare, TankmateSpecies.MANDARINFISH, true, LifeStage.ADULT);
        check(TankCare.anyoneEats(level, bare.getTank(), new ItemStack(ModItems.BETTA_PELLETS.get())) == false, "nobody takes pellets");
        check(TankCare.feed(level, bare, new ItemStack(ModItems.ARTEMIA_NAUPLII.get()), 1) == 0 || starved.health().daysSinceFed() >= 0, "no artemia");
        double rockDays = 0;
        boolean died = false;
        for (int i = 0; i < 160 && !died; i++) {
            for (TankBlockEntity c : List.of(rock, bare)) {
                TankCare.feed(level, c, new ItemStack(ModItems.BETTA_PELLETS.get()), 2);
                Copepods.step(level, c, 0.25);
            }
            if (fed.isAlive()) {
                TankmateCare.advance(level, rock, fed, 0.25);
                rockDays += 0.25;
            }
            if (starved.isAlive() && !starved.isRemoved()) TankmateCare.advance(level, bare, starved, 0.25);
            died = starved.isRemoved() || !starved.isAlive();
            if (i == 12) {
                check(fed.health().daysSinceFed() < 0.9, "the mandarin on the rock eats copepods: " + fed.health().daysSinceFed());
                check(starved.health().daysSinceFed() > 2.5, "the mandarin without rock goes hungry: " + starved.health().daysSinceFed());
            }
        }
        check(died, "the mandarin without copepods starved");
        check(logged(bare.getTank(), "log.bettacraft.died"), "logged");
        check(fed.isAlive() && !fed.isRemoved() && fed.health().daysSinceFed() < 1.0, "the mandarin on the rock lives after " + rockDays + " days");
        check(rock.getTank().copepods() > 0, "the rock keeps copepods");
        // the copepods are saved with the tank
        TankData loaded = TankData.load(rock.getTank().save());
        check(Math.abs(loaded.copepods() - rock.getTank().copepods()) < 1e-6, "copepods saved");
        h.succeed();
    }

    // ------------------------------------------------------------------ the Banggai cardinalfish

    /** A Banggai pair: the female passes her eggs to the male, he carries them in his mouth and releases juveniles. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void banggaiMaleBroodsInHisMouth(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = reef(h, 2, 2);
        TankmateEntity male = add(h, c, TankmateSpecies.BANGGAI_CARDINAL, false, LifeStage.ADULT);
        TankmateEntity female = add(h, c, TankmateSpecies.BANGGAI_CARDINAL, true, LifeStage.ADULT);
        TankData t = c.getTank();
        // M54: the courtship and the birth are draws of the level's random (shared by every running test): 15 days at half a
        // courtship a day leave a chance of 1 in 1800 that the pair never paired ("the male carries the eggs", seen once in CI on
        // 26.2); a fixed seed before the synchronous steps makes them the same every run
        level.getRandom().setSeed(54_048L);
        for (int i = 0; i < 150 && !male.isGravid(); i++) {
            male.health().feed();
            female.health().feed();
            t.setWaterTemp(26F);
            TankmateBreeding.step(level, c, 0.1);
        }
        check(male.isGravid() && !female.isGravid(), "the male carries the eggs");
        check(logged(t, "log.bettacraft.tm.banggai_eggs"), "logged");
        double gest = TankmateBreeding.gestationDays(TankmateSpecies.BANGGAI_CARDINAL);
        check(gest > TankmateBreeding.gestationDays(TankmateSpecies.PLATY), "he carries them longer than a livebearer: " + gest);
        for (int i = 0; i < 80 && mates(h, c, TankmateSpecies.BANGGAI_CARDINAL).size() <= 2; i++) {
            male.health().feed();
            female.health().feed();
            t.setWaterTemp(26F);
            TankmateCare.advance(level, c, male, 0.1);
            TankmateCare.advance(level, c, female, 0.1);
            TankmateBreeding.step(level, c, 0.1);
        }
        List<TankmateEntity> young = new ArrayList<>();
        for (TankmateEntity m : mates(h, c, TankmateSpecies.BANGGAI_CARDINAL)) if (m != male && m != female) young.add(m);
        check(!young.isEmpty(), "young came out of his mouth");
        for (TankmateEntity y : young) check(y.lifeStage() == LifeStage.JUVENILE && y.generation() == 1, "fully formed: " + y.lifeStage());
        check(logged(t, "log.bettacraft.tm.banggai_born"), "logged");
        h.succeed();
    }

    // ------------------------------------------------------------------ the fire shrimp and the anthias

    /** The fire shrimp: warm ocean net, trader, marine, a cleaner; the anthias is hungry again after a third of a day. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void fireShrimpCleansAnthiasEatOften(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var warm = NetCatching.table(NetCatching.Water.WARM_OCEAN, false, false, false);
        check(warm.stream().anyMatch(k -> k.species() == CritterSpecies.FIRE_SHRIMP), "the fire shrimp in the net");
        check(org.overengineer.bettacraft.sources.TraderOffers.all().stream().anyMatch(o -> o.id().equals("fire_shrimp")), "a trader offer");
        check(CritterSpecies.FIRE_SHRIMP.marine() && CritterSpecies.FIRE_SHRIMP.available(), "a marine animal");
        TankBlockEntity c = reef(h, 2, 2);
        check(!ReefCare.cleaners(c.getTank()), "no cleaner yet");
        check(TankCritters.add(level, c, CritterSpecies.FIRE_SHRIMP, CritterGenes.WILD, true, 10) != null, "fire shrimp in");
        check(ReefCare.cleaners(c.getTank()), "the fire shrimp cleans");
        TankmateEntity a = add(h, c, TankmateSpecies.LYRETAIL_ANTHIAS, true, LifeStage.ADULT);
        TankmateEntity d = add(h, c, TankmateSpecies.GREEN_CHROMIS, true, LifeStage.ADULT);
        check(TankmateSpecies.LYRETAIL_ANTHIAS.feedingsPerDay() >= 3, "feeds often");
        TankmateCare.advance(level, c, a, 0.5);
        TankmateCare.advance(level, c, d, 0.5);
        check(a.health().isHungry(TankmateCare.params(TankmateSpecies.LYRETAIL_ANTHIAS)), "the anthias is hungry after half a day");
        check(!d.health().isHungry(TankmateCare.params(TankmateSpecies.GREEN_CHROMIS)), "a chromis is not");
        h.succeed();
    }

    // ------------------------------------------------------------------ the clownfish larvae

    /** A guarded clownfish clutch at the start of its larva stage in a reef tank. */
    private static Clutch clownClutch(GameTestHelper h, TankBlockEntity c, int count) {
        TankmateSpecies s = TankmateSpecies.OCELLARIS_CLOWNFISH;
        TankmateEntity male = add(h, c, s, false, LifeStage.ADULT);
        add(h, c, s, true, LifeStage.ADULT);
        TankData t = c.getTank();
        TankWaterVolume w = new TankWaterVolume(t);
        Vec3 p = w.clamp(w.box().getCenter(), 0.2, 0.1, 0.2);
        Clutch k = new Clutch(t.nextClutchId(), s, Clutch.Kind.CAVE, p.x, w.box().minY + 0.1, p.z, count, TankmateGenes.wild(s), TankmateGenes.wild(s));
        k.guardian = male.getUUID();
        k.generation = 1;
        k.age = Clutch.eggDays(s) + 1e-3;
        t.clutches().add(k);
        return k;
    }

    private static int clownYoung(GameTestHelper h, TankBlockEntity c) {
        int n = 0;
        for (TankmateEntity m : mates(h, c, TankmateSpecies.OCELLARIS_CLOWNFISH)) if (!m.isAdult()) n++;
        return n;
    }

    /** {@code days} of breeding steps, the adults fed, the tank held at 26 °C; {@code daily} runs once a day. */
    private static void larvaDays(GameTestHelper h, TankBlockEntity c, double days, Runnable daily) {
        for (double d = 0; d < days; d += 0.1) {
            if (Math.abs(d - Math.round(d)) < 0.05) daily.run();
            for (TankmateEntity m : TankmateBreeding.tankmates(h.getLevel(), c.getTank())) m.health().feed();
            c.getTank().setWaterTemp(26F);
            c.getTank().setWaterQuality(1F);
            ClownLarvae.step(h.getLevel(), c, 0.1);
            TankmateBreeding.step(h.getLevel(), c, 0.1);
        }
    }

    /**
     * The food chain: phytoplankton ripens from its culture in light, rotifers from theirs; dosed into the tank the rotifers live
     * on the phytoplankton and die without it. Twenty larvae with rotifers (later artemia nauplii): most settle (advancement log);
     * twenty without: only a few (about 5 %).
     */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void clownLarvaeSurviveWithRotifers(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        check(ClownLarvae.active(), "larva rearing on");
        check(ModItems.PHYTOPLANKTON_CULTURE.get().result() == ModItems.PHYTOPLANKTON.get() && ModItems.PHYTOPLANKTON_CULTURE.get().needsLight(),
                "phytoplankton ripens in light");
        check(ModItems.ROTIFER_CULTURE.get().result() == ModItems.ROTIFERS.get(), "rotifers ripen");
        var rm = FeatureSwitchGameTests.recipes(level);
        check(rm.byKey(BettaCraft.id("phytoplankton_culture_seagrass")).isPresent() && rm.byKey(BettaCraft.id("rotifer_culture_start")).isPresent()
                && rm.byKey(BettaCraft.id("rotifer_culture")).isPresent(), "the culture recipes");
        // the rotifers live on the phytoplankton
        TankBlockEntity plain = reef(h, 2, 14);
        TankBlockEntity green = reef(h, 8, 14);
        for (TankBlockEntity c : List.of(plain, green)) check(TankCare.feed(level, c, new ItemStack(ModItems.ROTIFERS.get()), 1) > 0, "rotifers into the water");
        check(ClownLarvae.dosePhytoplankton(green.getTank()), "phytoplankton dosed");
        for (int i = 0; i < 30; i++) {
            ClownLarvae.step(level, plain, 0.1);
            ClownLarvae.step(level, green, 0.1);
        }
        check(green.getTank().rotifers() > plain.getTank().rotifers() * 2, "rotifers live on phytoplankton: " + green.getTank().rotifers()
                + " vs " + plain.getTank().rotifers());
        // larvae with and without rotifers
        TankBlockEntity fed = reef(h, 2, 2), hungry = reef(h, 8, 2);
        Clutch kf = clownClutch(h, fed, 20), kh = clownClutch(h, hungry, 20);
        check(kf.stage() == 1 && kh.stage() == 1, "larvae");
        double days = Clutch.larvaDays(TankmateSpecies.OCELLARIS_CLOWNFISH) + 0.3;
        check(days > 2.5, "the larvae drift for days: " + days);
        // M52: the larva days run within this tick: a fixed seed of the level's random makes them the same in every run (the shared
        // random of the suite let the adults' fry hunting take 11 of 20 young now and then)
        h.getLevel().getRandom().setSeed(4811L);
        larvaDays(h, fed, days, () -> {
            TankCare.feed(level, fed, new ItemStack(ModItems.ROTIFERS.get()), 1);
            TankCare.feed(level, fed, new ItemStack(ModItems.ARTEMIA_NAUPLII.get()), 1);
            ClownLarvae.dosePhytoplankton(fed.getTank());
        });
        larvaDays(h, hungry, days, () -> {});
        int raised = clownYoung(h, fed), few = clownYoung(h, hungry);
        check(fed.getTank().clutches().isEmpty(), "the fed larvae settled: " + fed.getTank().clutches().size());
        check(raised >= 10, "most larvae with rotifers settle: " + raised + " of 20");
        check(few <= 5, "few without: " + few + " of 20");
        check(logged(fed.getTank(), "log.bettacraft.tm.clown_larvae_raised"), "raised on rotifers (advancement)");
        check(!logged(hungry.getTank(), "log.bettacraft.tm.clown_larvae_raised"), "not raised on rotifers");
        h.succeed();
    }

    /** An old clownfish clutch (no rotifer flag) loads; with the switch off the larvae settle after one day as before, all of them. */
    @GameTest(template = EMPTY, batch = "bettacraft_m48_larvae_off", timeoutTicks = 300)
    public static void larvaRearingOffAsBefore(GameTestHelper h) {
        CompoundTag old;
        TankmateSpecies s = TankmateSpecies.OCELLARIS_CLOWNFISH;
        Clutch tmp = new Clutch(7, s, Clutch.Kind.CAVE, 1, 2, 3, 12, TankmateGenes.wild(s), TankmateGenes.wild(s));
        old = tmp.save();
        old.remove("rotifers");
        Clutch loaded = Clutch.load(old);
        check(loaded != null && loaded.count == 12 && !loaded.rotifers, "an old clutch loads");
        TankBlockEntity c = reef(h, 2, 2);
        Features.set(Feature.LARVA_REARING, false);
        try {
            check(Clutch.larvaDays(s) <= 1.0 + 1e-9, "one larva day again");
            Clutch k = clownClutch(h, c, 12);
            check(!ClownLarvae.wantFood(c.getTank(), new ItemStack(ModItems.ROTIFERS.get())), "no plankton pool");
            h.getLevel().getRandom().setSeed(4812L); // M52: the same larva days in every run (see above)
            larvaDays(h, c, 1.5, () -> {});
            // (the adults may still catch a free young now and then: their fry hunting)
            check(c.getTank().clutches().isEmpty() && clownYoung(h, c) >= 8 && !logged(c.getTank(), "log.bettacraft.reef.larvae_died"),
                    "the larvae settled without food as before: " + clownYoung(h, c));
        } finally {
            Features.set(Feature.LARVA_REARING, true);
        }
        h.succeed();
    }
}
