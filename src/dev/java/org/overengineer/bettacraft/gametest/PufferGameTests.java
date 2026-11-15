package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.critters.Critter;
import org.overengineer.bettacraft.critters.CritterGenes;
import org.overengineer.bettacraft.critters.CritterSpecies;
import org.overengineer.bettacraft.critters.TankCritters;
import org.overengineer.bettacraft.entity.betta.BettaEntity;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateCare;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.food.BettaFood;
import org.overengineer.bettacraft.registry.ModItems;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankData;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tankmates.Puffers;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateFeeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M29 part b: the puffers eat snails (bladder and ramshorn snails first) and wear their teeth down on them; overgrown teeth
 * refuse soft food until shell food comes; fin nippers (tiger barbs) go for a betta and an angelfish but not for the
 * cherry barbs; the green spotted puffer only lives in brackish water. Snail counts of a multi-day step are deterministic
 * (expected value at least one per day).
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PufferGameTests {
    private static final String EMPTY = "empty_32x10x32";

    private PufferGameTests() {}

    private static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz, float temp) {
        TankBlockEntity c = CritterGameTests.tank(h, x, y, z, sx, sy, sz, sx * sy * sz * 1000L);
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
        for (TankmateEntity m : TankmateBreeding.tankmates(h.getLevel(), c.getTank())) if (s == null || m.species() == s) out.add(m);
        return out;
    }

    private static int count(TankData t, CritterSpecies s) {
        int n = 0;
        for (Critter k : t.critters()) if (k.alive() && k.species == s) n++;
        return n;
    }

    private static boolean logged(TankData t, String key) {
        return t.log().stream().anyMatch(e -> e.key().equals(key));
    }

    /** Two dwarf puffers eat snails (bladder snails sooner than the nerites) and the snails wear their teeth down. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void puffersEatSnailsAndWearTheirTeeth(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 2, 3, 2, 3, 26F);
        TankmateEntity a = add(h, c, TankmateSpecies.DWARF_PUFFER, false);
        TankmateEntity b = add(h, c, TankmateSpecies.DWARF_PUFFER, true);
        a.setTeethDaysForTest(5);
        b.setTeethDaysForTest(5);
        for (int i = 0; i < 8; i++) TankCritters.add(level, c, CritterSpecies.BLADDER_SNAIL, CritterGenes.WILD, i % 2 == 0, 10);
        for (int i = 0; i < 8; i++) TankCritters.add(level, c, CritterSpecies.ZEBRA_NERITE, CritterGenes.WILD, i % 2 == 0, 10);
        int bladder = count(c.getTank(), CritterSpecies.BLADDER_SNAIL), nerite = count(c.getTank(), CritterSpecies.ZEBRA_NERITE);
        TankmateBreeding.step(level, c, 3.0); // expected 6 snails
        int ateBladder = bladder - count(c.getTank(), CritterSpecies.BLADDER_SNAIL), ateNerite = nerite - count(c.getTank(), CritterSpecies.ZEBRA_NERITE);
        h.assertTrue(ateBladder + ateNerite >= 3, "snails eaten: " + ateBladder + " bladder, " + ateNerite + " nerites");
        h.assertTrue(ateBladder > ateNerite, "bladder snails first: " + ateBladder + " / " + ateNerite);
        h.assertTrue(a.teethDays() == 0 || b.teethDays() == 0, "a snail wore the teeth down");
        h.assertTrue(logged(c.getTank(), "log.bettacraft.tm.ate_snails"), "logged");
        h.succeed();
    }

    /** Overgrown teeth: the puffer is stressed and refuses flakes; shell food it eats and its teeth are fine again. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void overgrownTeethNeedHardFood(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 2, 2, 2, 2, 26F);
        TankmateEntity p = add(h, c, TankmateSpecies.RED_EYE_PUFFER, false);
        p.setTeethDaysForTest(Puffers.teethDays() - 0.05);
        TankmateCare.advance(level, c, p, 0.1);
        h.assertTrue(Puffers.overgrown(p), "teeth overgrown after " + p.teethDays() + " days");
        h.assertTrue((p.stressBits() & TankmateCare.STRESS_TEETH) != 0, "stressed");
        ItemStack flakes = new ItemStack(ModItems.FISH_FLAKES.get());
        int ate = TankmateFeeding.feed(level, c, List.of(p), flakes, BettaFood.kind(flakes), 4, false, false, null);
        h.assertTrue(ate == 0, "refuses flakes");
        ItemStack shell = new ItemStack(ModItems.SHELL_FOOD.get());
        h.assertTrue(Puffers.hardFood(shell) && BettaFood.kind(shell) != null, "shell food is hard fish food");
        ate = TankmateFeeding.feed(level, c, List.of(p), shell, BettaFood.kind(shell), 4, false, false, null);
        h.assertTrue(ate == 1, "eats shell food");
        h.assertTrue(p.teethDays() == 0 && !Puffers.overgrown(p), "teeth worn down");
        TankmateCare.advance(level, c, p, 0.1);
        h.assertTrue((p.stressBits() & TankmateCare.STRESS_TEETH) == 0, "no longer stressed");
        h.succeed();
    }

    /** Tiger barbs nip the fins of a betta and an angelfish; the betta loses hit points. Cherry barbs leave them alone. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void tigerBarbsNipFins(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity c = tank(h, 2, 2, 2, 3, 2, 3, 27F);
        for (int i = 0; i < 6; i++) add(h, c, TankmateSpecies.TIGER_BARB, i % 2 == 0);
        TankmateEntity angel = add(h, c, TankmateSpecies.ANGELFISH, false);
        BettaEntity betta = LiveFoodGameTests.betta(h, c, false, 29L);
        float hp = betta.getHealth();
        TankmateBreeding.step(level, c, 2.0);
        h.assertTrue((angel.stressBits() & TankmateCare.STRESS_NIPPED) != 0, "the angelfish is nipped");
        h.assertTrue(betta.getHealth() < hp, "the betta loses hit points: " + hp + " -> " + betta.getHealth());
        h.assertTrue(logged(c.getTank(), "log.bettacraft.tm.fin_nipping"), "logged");
        // peaceful cherry barbs
        TankBlockEntity calm = tank(h, 8, 2, 2, 3, 2, 3, 27F);
        for (int i = 0; i < 6; i++) add(h, calm, TankmateSpecies.CHERRY_BARB, i % 2 == 0);
        TankmateEntity angel2 = add(h, calm, TankmateSpecies.ANGELFISH, false);
        TankmateBreeding.step(level, calm, 2.0);
        h.assertTrue((angel2.stressBits() & TankmateCare.STRESS_NIPPED) == 0, "cherry barbs do not nip");
        h.succeed();
    }

    /** The green spotted puffer is stressed by fresh water (a salt shock) and lives in brackish water. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void greenSpottedPufferNeedsBrackishWater(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TankBlockEntity fresh = tank(h, 2, 2, 2, 2, 2, 2, 26F);
        TankmateEntity a = add(h, fresh, TankmateSpecies.GREEN_SPOTTED_PUFFER, false);
        TankmateCare.advance(level, fresh, a, 0.2);
        h.assertTrue((a.stressBits() & TankmateCare.STRESS_WATER) != 0, "fresh water is bad for it");
        TankBlockEntity brackish = tank(h, 8, 2, 2, 2, 2, 2, 26F);
        brackish.getTank().setSalinity(12F);
        TankmateEntity b = add(h, brackish, TankmateSpecies.GREEN_SPOTTED_PUFFER, false);
        TankmateCare.advance(level, brackish, b, 0.2);
        h.assertTrue((b.stressBits() & TankmateCare.STRESS_WATER) == 0, "brackish water suits it: " + b.stressBits());
        h.succeed();
    }
}
