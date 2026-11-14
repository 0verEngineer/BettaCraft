package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.CommonConfig;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateCare;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.genetics.livebearer.LbLocus;
import org.overengineer.bettacraft.genetics.livebearer.LivebearerGenome;
import org.overengineer.bettacraft.genetics.livebearer.LivebearerLegacy;
import org.overengineer.bettacraft.genetics.livebearer.LivebearerStrain;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateItems;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;
import org.overengineer.bettacraft.world.TankmateTrades;

/**
 * M21.6: the guppy and endler genetics in a tank: the young inherit at birth (the father's Y to every son, his X to
 * every daughter, recessive body colours carried hidden), the sons of a Red Cobra and a Full Gold are Red Cobras,
 * endlers and guppies make hybrids, old fixed varieties are converted on load and kept across save and load, the
 * trader sells strains, and with {@code features.livebearerGenetics} off nothing of it happens.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LivebearerGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private LivebearerGameTests() {}

    private static TankBlockEntity tank(GameTestHelper h, int x, int z) {
        TankBlockEntity c = CritterGameTests.tank(h, x, 2, z, 3, 2, 2, 12000);
        c.getTank().setWaterTemp(25F);
        c.getTank().setTargetTemp(25F);
        c.getTank().setWaterQuality(1F);
        return c;
    }

    private static TankmateEntity add(GameTestHelper h, TankBlockEntity c, TankmateSpecies s, TankmateGenes g, boolean female) {
        var w = new org.overengineer.bettacraft.tank.TankWaterVolume(c.getTank());
        Vec3 p = w.clamp(new Vec3((w.box().minX + w.box().maxX) / 2, w.box().minY + 0.3, (w.box().minZ + w.box().maxZ) / 2), 0.35, 0.1, 0.2);
        TankmateEntity e = TankmateBreeding.spawn(h.getLevel(), c, s, g, female, LifeStage.ADULT, 0, p);
        if (e == null) throw new GameTestAssertException("could not spawn " + s.id());
        e.health().feed();
        return e;
    }

    private static List<TankmateEntity> mates(GameTestHelper h, TankBlockEntity c) {
        return new ArrayList<>(TankmateBreeding.tankmates(h.getLevel(), c.getTank()));
    }

    private static void days(GameTestHelper h, TankBlockEntity c, double days, double step) {
        for (double d = 0; d < days - 1e-9; d += step) {
            for (TankmateEntity m : mates(h, c)) m.health().feed();
            c.getTank().setWaterTemp(25F);
            c.getTank().setWaterQuality(1F);
            for (TankmateEntity m : mates(h, c)) TankmateCare.advance(h.getLevel(), c, m, step);
            TankmateBreeding.step(h.getLevel(), c, step);
        }
    }

    private static boolean logged(TankData t, String key) {
        return t.log().stream().anyMatch(e -> e.key().equals(key));
    }

    /**
     * Up to {@code maxDays} of the breeding step until a female is pregnant: everyone fed, the water held, but no care
     * step. M22.5: the pregnancy is a chance (0.8 a day); with the care step a pair below the shoal size of three gets
     * shoal stress and falls sick after {@code health.daysUntilSick} (7 days), which ended the waiting early: about 1 in
     * 250 runs had no pregnancy (failed once on GitHub Actions). Without the care step nothing stops the waiting, and
     * 30 days leave a chance of e^-24 for no pregnancy.
     */
    static boolean waitForPregnancy(GameTestHelper h, TankBlockEntity c, float temp, double maxDays) {
        for (double d = 0; d < maxDays - 1e-9; d += 0.25) {
            List<TankmateEntity> all = new ArrayList<>(TankmateBreeding.tankmates(h.getLevel(), c.getTank()));
            if (all.stream().anyMatch(TankmateEntity::isGravid)) return true;
            for (TankmateEntity m : all) m.health().feed();
            c.getTank().setWaterTemp(temp);
            c.getTank().setWaterQuality(1F);
            TankmateBreeding.step(h.getLevel(), c, 0.25);
        }
        return TankmateBreeding.tankmates(h.getLevel(), c.getTank()).stream().anyMatch(TankmateEntity::isGravid);
    }

    /** Runs until the female is pregnant and has given birth; the young (generation 1). */
    private static List<TankmateEntity> breed(GameTestHelper h, TankBlockEntity c) {
        h.assertTrue(waitForPregnancy(h, c, 25F, 30), "a pregnant female");
        days(h, c, TankmateBreeding.gestationDays() + 0.25, 0.25);
        List<TankmateEntity> young = mates(h, c).stream().filter(m -> m.generation() == 1).toList();
        h.assertTrue(!young.isEmpty(), "young were born");
        return young;
    }

    /** Red Cobra father, Full Gold mother: Y to the sons, X to the daughters, gold carried hidden, the sons are Red Cobras. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void guppiesInheritAtBirth(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 2);
        add(h, c, TankmateSpecies.GUPPY, TankmateGenes.strain(TankmateSpecies.GUPPY, LivebearerStrain.FULL_GOLD), true);
        TankmateEntity father = add(h, c, TankmateSpecies.GUPPY, TankmateGenes.strain(TankmateSpecies.GUPPY, LivebearerStrain.RED_COBRA), false);
        h.assertTrue("cobra".equals(father.genes().lb().y), "the father has the cobra Y");
        List<TankmateEntity> young = breed(h, c);
        int sons = 0, daughters = 0, carriers = 0;
        for (TankmateEntity y : young) {
            LivebearerGenome g = y.genes().lb();
            h.assertTrue(g != null, "every young has a genome");
            h.assertTrue(g.endler == 0F, "pure guppies");
            if (y.isFemale()) {
                daughters++;
                h.assertTrue("none".equals(g.y), "no daughter has a Y: " + g.y);
                h.assertTrue("none".equals(g.express(true).y()), "no Y pattern on a daughter");
                h.assertTrue(g.xb.equals(father.genes().lb().xa), "the daughter has her father's X");
            } else {
                sons++;
                h.assertTrue("cobra".equals(g.y), "every son has his father's cobra Y: " + g.y);
            }
            if (g.a(LbLocus.GOLD).equals("gold") || g.b(LbLocus.GOLD).equals("gold")) carriers++;
        }
        h.assertTrue(carriers >= young.size() - 1, "the young carry the gold of their mother: " + carriers + "/" + young.size());
        h.assertTrue(sons + daughters == young.size(), "all young counted");
        // a son grows up: a Red Cobra like his father (red over yellow, the leopard tail, the cobra Y, gold hidden)
        TankmateEntity son = young.stream().filter(y -> !y.isFemale()).findFirst().orElse(null);
        if (son != null) {
            // the parents out (they would eat the fry)
            for (TankmateEntity m : mates(h, c)) {
                if (m.generation() == 0) {
                    c.getTank().inhabitants().remove(m.getUUID());
                    m.discard();
                }
            }
            days(h, c, (LifeStage.FRY.days() + LifeStage.JUVENILE.days() + LifeStage.SUBADULT.days()) * TankmateSpecies.GUPPY.growth() + 0.5, 0.5);
            h.assertTrue(son.isAdult(), "the son grew up");
            LivebearerStrain s = LivebearerStrain.of(son.livebearerPhenotype());
            h.assertTrue(s == LivebearerStrain.RED_COBRA || son.genes().lb().expressed(LbLocus.TAIL_COLOR).equals("yellow"),
                    "the son is a Red Cobra, got " + s + " " + son.genes().lb());
            h.assertTrue("wild".equals(son.livebearerPhenotype().body()), "gold stays hidden");
        }
        h.succeed();
    }

    /** An endler male with guppy females: hybrids of the mother's species, half endler, the sons with his Y. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void endlersAndGuppiesMakeHybrids(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 2);
        add(h, c, TankmateSpecies.GUPPY, TankmateGenes.strain(TankmateSpecies.GUPPY, LivebearerStrain.BLUE_GRASS), true);
        add(h, c, TankmateSpecies.ENDLER_GUPPY, TankmateGenes.strain(TankmateSpecies.ENDLER_GUPPY, LivebearerStrain.ENDLER_TIGER), false);
        List<TankmateEntity> young = breed(h, c);
        h.assertTrue(logged(c.getTank(), "log.bettacraft.tm.gravid_hybrid"), "the hybrid pregnancy is logged");
        for (TankmateEntity y : young) {
            h.assertTrue(y.species() == TankmateSpecies.GUPPY, "the young are of the mother's species");
            LivebearerGenome g = y.genes().lb();
            h.assertTrue(Math.abs(g.endler - 0.5F) < 1e-4, "half endler: " + g.endler);
            h.assertTrue(g.express(y.isFemale()).hybrid(), "a hybrid");
            h.assertTrue(LivebearerStrain.of(g.express(y.isFemale())) == null, "hybrids have no strain");
            if (!y.isFemale()) h.assertTrue("tiger".equals(g.y), "the sons have the endler's tiger Y");
        }
        h.succeed();
    }

    /** Fish of an older world (variety alleles, no genome) get the genome of their variety, kept across save and load. */
    @GameTest(template = EMPTY)
    public static void oldVarietiesConvertOnLoad(GameTestHelper h) {
        String[][] old = {{"guppy", "wild"}, {"guppy", "red"}, {"guppy", "blue_grass"}, {"guppy", "green_cobra"}, {"guppy", "tuxedo"},
                {"endler_guppy", "wild"}, {"endler_guppy", "tiger"}, {"endler_guppy", "black_bar"}};
        for (String[] o : old) {
            TankmateSpecies s = TankmateSpecies.byId(o[0]);
            for (boolean female : new boolean[] {false, true}) {
                CompoundTag fish = new CompoundTag();
                fish.putString("species", s.id());
                CompoundTag genes = new CompoundTag();
                genes.putString("a", o[1]);
                genes.putString("b", o[1]);
                fish.put("genes", genes);
                fish.putBoolean("female", female);
                fish.putString("stage", LifeStage.ADULT.id());
                TankmateEntity e = ModEntities.TANKMATES.get(s).get().create(h.getLevel());
                if (e == null) throw new GameTestAssertException("no entity");
                e.loadTankmate(fish);
                LivebearerGenome g = e.genes().lb();
                h.assertTrue(g != null, o[1] + ": converted");
                h.assertTrue(g.equals(LivebearerLegacy.convert(s == TankmateSpecies.ENDLER_GUPPY, o[1], o[1], o[1])), o[1] + ": the variety's genome");
                h.assertTrue(o[1].equals(e.variety()), o[1] + ": the variety stays " + e.variety());
                if (!female) {
                    var want = (s == TankmateSpecies.ENDLER_GUPPY ? LivebearerLegacy.ENDLER_STRAINS : LivebearerLegacy.GUPPY_STRAINS).get(o[1]);
                    h.assertTrue(LivebearerStrain.of(e.livebearerPhenotype()) == want, o[1] + ": strain " + want + ", got " + LivebearerStrain.of(e.livebearerPhenotype()));
                }
                // saved with its genome, loaded again: the same
                TankmateEntity again = ModEntities.TANKMATES.get(s).get().create(h.getLevel());
                again.loadTankmate(e.saveTankmate());
                h.assertTrue(g.equals(again.genes().lb()), o[1] + ": kept across save and load");
                e.discard();
                again.discard();
            }
        }
        h.succeed();
    }

    /** The trader: wild-caught guppies and endlers, and named strains. */
    @GameTest(template = EMPTY)
    public static void traderSellsStrains(GameTestHelper h) {
        Random r = new Random(21);
        int strains = 0, offers = 0;
        for (int i = 0; i < 60; i++) {
            TankmateSpecies s = i % 2 == 0 ? TankmateSpecies.GUPPY : TankmateSpecies.ENDLER_GUPPY;
            var offer = TankmateTrades.offer(s, r::nextLong);
            offers++;
            boolean named = false;
            for (CompoundTag f : TankmateItems.bagFish(offer.getResult())) {
                var t = f.getCompound(TankmateEntity.KEY);
                TankmateGenes g = TankmateGenes.load(t.getCompound("genes"), s);
                h.assertTrue(g.lb() != null, "every fish has genes");
                if (!t.getBoolean("female") && LivebearerStrain.of(g.lb().express(false)) != null && LivebearerStrain.of(g.lb().express(false)).source
                        == LivebearerStrain.Source.TRADER) named = true;
            }
            if (named) strains++;
        }
        h.assertTrue(strains > offers / 4 && strains < offers, "strains and wild fish: " + strains + "/" + offers);
        h.succeed();
    }

    /** features.livebearerGenetics off: no genome on the entity (old textures), no hybrids. */
    @GameTest(template = EMPTY, timeoutTicks = 300, batch = "bettacraft_livebearer_config")
    public static void geneticsOffKeepsTheOldVarieties(GameTestHelper h) {
        boolean before = CommonConfig.livebearerGenetics();
        try {
            CommonConfig.FEATURE_LIVEBEARER_GENETICS.set(false);
            TankBlockEntity c = tank(h, 2, 2);
            TankmateEntity f = add(h, c, TankmateSpecies.GUPPY, TankmateGenes.wild(TankmateSpecies.GUPPY, 0.0, new Random(3)::nextLong), true);
            add(h, c, TankmateSpecies.ENDLER_GUPPY, TankmateGenes.pure(TankmateSpecies.ENDLER_GUPPY, "tiger"), false);
            h.assertTrue(f.livebearerGenome() == null, "no genetics look");
            h.assertTrue(f.genes().lb().equals(LivebearerLegacy.convert(false, "wild", "wild", "wild")), "wild fish get the plain wild genome");
            h.assertTrue(TankmateBreeding.hybridPartner(TankmateSpecies.GUPPY) == null, "no crossing partner");
            days(h, c, 6.0, 0.5);
            h.assertTrue(!f.isGravid() && mates(h, c).size() == 2, "no hybrids without the genetics");
        } finally {
            CommonConfig.FEATURE_LIVEBEARER_GENETICS.set(before);
        }
        h.succeed();
    }
}
