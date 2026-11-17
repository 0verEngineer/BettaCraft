package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.entity.betta.BettaBucketItem;
import org.overengineer.bettacraft.entity.betta.BettaData;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.genetics.BettaGenotype;
import org.overengineer.bettacraft.genetics.BettaSpecies;
import org.overengineer.bettacraft.registry.ModEntities;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankBreeding;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankInhabitants;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tankmates.Clutch;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M29 part c: the chocolate gourami broods her eggs in her mouth and spits out free young (never into the breeding box);
 * honey gourami males live in peace, pearl gourami males fight; Betta imbellis males fight much less than Betta splendens
 * males, and a wild imbellis female with a splendens male gives hybrid young.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CommunityFishGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private CommunityFishGameTests() {}

    private static TankBlockEntity tank(GameTestHelper h, int x, int z, int sx, float temp) {
        TankBlockEntity c = CritterGameTests.tank(h, x, 2, z, sx, 2, 3, sx * 2 * 3 * 1000L);
        c.getTank().setWaterTemp(temp);
        c.getTank().setTargetTemp(temp);
        c.getTank().setWaterQuality(1F);
        return c;
    }

    private static TankmateEntity add(GameTestHelper h, TankBlockEntity c, TankmateSpecies s, boolean female) {
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        var b = w.box();
        Vec3 p = w.clamp(new Vec3((b.minX + b.maxX) / 2, b.minY + 0.5, (b.minZ + b.maxZ) / 2), 0.35, 0.1, 0.2);
        TankmateEntity e = TankmateBreeding.spawn(h.getLevel(), c, s, TankmateGenes.wild(s), female, LifeStage.ADULT, 0, p);
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

    static BettaEntity betta(GameTestHelper h, TankBlockEntity c, BettaGenotype g, boolean female) {
        BettaData d = BettaData.create(g, female, 0);
        ItemStack bucket = BettaBucketItem.withFish(new ItemStack(ModItems.BETTA_BUCKET.get()), d);
        Component err = TankInhabitants.insert(h.getLevel(), c, bucket, ModEntities.BETTA.get(), null, false);
        if (err != null) throw new GameTestAssertException("insert failed: " + err.getString());
        for (Entity e : TankInhabitants.liveInhabitants(h.getLevel(), c.getTank())) {
            if (e instanceof BettaEntity b && b.getBettaData().fishId().equals(d.fishId())) return b;
        }
        throw new GameTestAssertException("inserted betta not found");
    }

    /** A chocolate gourami pair spawns, the female carries the eggs in her mouth and spits out young; nothing goes into the box. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void chocolateGouramiBroodsInHerMouth(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 3, 28F);
        c.getTank().setBreedingBox(true);
        TankmateEntity m = add(h, c, TankmateSpecies.CHOCOLATE_GOURAMI, false);
        TankmateEntity f = add(h, c, TankmateSpecies.CHOCOLATE_GOURAMI, true);
        for (int i = 0; i < 60 && !f.isGravid(); i++) {
            m.health().feed();
            f.health().feed();
            TankmateBreeding.step(level, c, 0.25);
        }
        h.assertTrue(f.isGravid(), "the female took the eggs into her mouth");
        h.assertTrue(logged(c.getTank(), "log.bettacraft.tm.mouth_eggs"), "logged");
        f.setGravid(TankmateBreeding.gestationDays(TankmateSpecies.CHOCOLATE_GOURAMI) + 0.01, m.genes());
        TankmateBreeding.step(level, c, 0.01);
        h.assertTrue(!f.isGravid(), "she spat out the young");
        h.assertTrue(logged(c.getTank(), "log.bettacraft.tm.mouth_born"), "birth logged");
        h.assertTrue(c.getTank().clutches().stream().noneMatch(k -> k.kind == Clutch.Kind.BOX), "nothing in the breeding box");
        h.assertTrue(mates(h, c, TankmateSpecies.CHOCOLATE_GOURAMI).stream().anyMatch(x -> x.lifeStage() == LifeStage.FRY), "free young in the tank");
        h.succeed();
    }

    /** Two honey gourami males live in peace; two pearl gourami males fight. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void onlyTerritorialGouramisFight(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity honey = tank(h, 2, 2, 3, 27F);
        TankmateEntity a = add(h, honey, TankmateSpecies.HONEY_GOURAMI, false);
        TankmateEntity b = add(h, honey, TankmateSpecies.HONEY_GOURAMI, false);
        TankmateBreeding.step(level, honey, 2.0);
        h.assertTrue(a.getHealth() >= a.getMaxHealth() && b.getHealth() >= b.getMaxHealth(), "honey gouramis do not fight");
        h.assertTrue(!logged(honey.getTank(), "log.bettacraft.tm.gourami_fight"), "no fight logged");
        TankBlockEntity pearl = tank(h, 8, 2, 3, 27F);
        TankmateEntity p = add(h, pearl, TankmateSpecies.PEARL_GOURAMI, false);
        TankmateEntity q = add(h, pearl, TankmateSpecies.PEARL_GOURAMI, false);
        TankmateBreeding.step(level, pearl, 2.0); // 0.6 hit points a day: damage comes in whole points
        h.assertTrue(p.getHealth() < p.getMaxHealth() || q.getHealth() < q.getMaxHealth(), "pearl gourami males fight");
        h.assertTrue(logged(pearl.getTank(), "log.bettacraft.tm.gourami_fight"), "fight logged");
        h.succeed();
    }

    /** Two Betta imbellis males lose far fewer hit points in the same time than two Betta splendens males. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void imbellisMalesFightLess(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        RandomSource r = RandomSource.create(29);
        TankBlockEntity calm = tank(h, 2, 2, 3, 27F);
        BettaEntity i1 = betta(h, calm, BettaGenotype.randomWild(BettaSpecies.IMBELLIS, r), false);
        BettaEntity i2 = betta(h, calm, BettaGenotype.randomWild(BettaSpecies.IMBELLIS, r), false);
        TankBlockEntity wild = tank(h, 8, 2, 3, 27F);
        BettaEntity s1 = betta(h, wild, BettaGenotype.randomWild(BettaSpecies.SPLENDENS, r), false);
        BettaEntity s2 = betta(h, wild, BettaGenotype.randomWild(BettaSpecies.SPLENDENS, r), false);
        h.assertTrue("bettacraft:betta_imbellis".equals(i1.genome().species()), "an imbellis: " + i1.genome().species());
        // M32 part b: each tank draws the same fixed sequence from its own random (the shared level random made it flaky);
        // over 0.4 days the whole-point damage of the splendens males is a few points, far from deadly
        RandomSource rc = RandomSource.create(4242), rw = RandomSource.create(4242);
        for (int i = 0; i < 8; i++) {
            TankBreeding.step(level, calm, 0.05, rc);
            TankBreeding.step(level, wild, 0.05, rw);
        }
        float lostImbellis = i1.getMaxHealth() - i1.getHealth() + i2.getMaxHealth() - i2.getHealth();
        float lostSplendens = s1.getMaxHealth() - s1.getHealth() + s2.getMaxHealth() - s2.getHealth();
        h.assertTrue(lostSplendens > 0, "splendens males fight");
        h.assertTrue(lostImbellis < lostSplendens * 0.8, "imbellis males fight less: " + lostImbellis + " vs " + lostSplendens);
        h.succeed();
    }

    /** The offspring of an imbellis female and a splendens male are hybrids; the bucket tooltip names the shares. */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void imbellisAndSplendensGiveHybrids(GameTestHelper h) {
        RandomSource r = RandomSource.create(5);
        BettaGenotype mother = BettaGenotype.randomWild(BettaSpecies.IMBELLIS, r);
        BettaGenotype father = BettaGenotype.randomWild(BettaSpecies.SPLENDENS, r);
        BettaData young = BettaData.offspring(BettaGenotype.cross(mother, father, r, 0), true, null, null, 1, 0);
        h.assertTrue(BettaSpecies.HYBRID_ID.equals(young.genome().species()), "a hybrid: " + young.genome().species());
        List<Component> tip = new ArrayList<>();
        org.overengineer.bettacraft.entity.betta.BettaTooltip.append(young, tip);
        h.assertTrue(tip.stream().anyMatch(c -> c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                && tc.getKey().equals("tooltip.bettacraft.betta.hybrid")), "tooltip names the hybrid: " + tip);
        h.assertTrue(young.genotype().ancestry().imbellis() == 32, "half imbellis");
        h.succeed();
    }
}
