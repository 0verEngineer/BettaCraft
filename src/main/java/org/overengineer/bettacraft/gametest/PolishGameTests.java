package org.overengineer.bettacraft.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.entity.betta.LifeStage;
import org.overengineer.bettacraft.entity.tankmate.TankmateEntity;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankWaterVolume;
import org.overengineer.bettacraft.tankmates.TankmateBreeding;
import org.overengineer.bettacraft.tankmates.TankmateGenes;
import org.overengineer.bettacraft.tankmates.TankmateSpecies;

/**
 * M21.5 (polish): plecos and otocinclus stand upright only once they reach the glass.
 */
@GameTestHolder(BettaCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PolishGameTests {
    static final String EMPTY = "empty_32x10x32";

    private PolishGameTests() {}

    static TankBlockEntity tank(GameTestHelper h, int x, int y, int z, int sx, int sy, int sz) {
        TankBlockEntity c = CritterGameTests.tank(h, x, y, z, sx, sy, sz, (long) sx * sy * sz * 1000);
        c.getTank().setWaterQuality(1F);
        return c;
    }

    static TankmateEntity spawn(GameTestHelper h, TankBlockEntity c, TankmateSpecies s, Vec3 p) {
        TankmateEntity e = TankmateBreeding.spawn(h.getLevel(), c, s, TankmateGenes.wild(s), false, LifeStage.ADULT, 0, p);
        if (e == null) throw new GameTestAssertException("could not spawn " + s.id());
        e.health().feed();
        return e;
    }

    /** An otocinclus and an L134 in the middle of a big tank swim to the glass lying flat and stand up only there. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void glassCleanersStandUpOnlyAtTheGlass(GameTestHelper h) {
        TankBlockEntity c = tank(h, 2, 1, 2, 5, 3, 5);
        TankWaterVolume w = new TankWaterVolume(c.getTank());
        Vec3 mid = new Vec3((w.box().minX + w.box().maxX) / 2, w.box().minY + 1.2, (w.box().minZ + w.box().maxZ) / 2);
        TankmateEntity oto = spawn(h, c, TankmateSpecies.OTOCINCLUS, mid);
        TankmateEntity pleco = spawn(h, c, TankmateSpecies.LEOPARD_FROG_PLECO, mid.add(0.3, 0, 0.3));
        boolean[] started = new boolean[2], arrived = new boolean[2];
        TankmateEntity[] fish = {oto, pleco};
        h.onEachTick(() -> {
            for (int i = 0; i < 2; i++) {
                TankmateEntity f = fish[i];
                if (!started[i]) {
                    started[i] = f.swimToGlassForTest();
                    continue;
                }
                Vec3 t = f.behaviourTarget();
                if (t == null) continue;
                double d = f.position().distanceTo(t);
                boolean upright = f.flag(TankmateEntity.FLAG_GLASS);
                if (upright && d > 0.12) throw new GameTestAssertException(f.species().id() + " stands upright " + String.format("%.2f", d) + " blocks before the glass");
                if (upright) arrived[i] = true;
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(started[0] && started[1], "both found a spot on the glass");
            h.assertTrue(arrived[0], "the otocinclus reached the glass and stands upright");
            h.assertTrue(arrived[1], "the L134 reached the glass and stands upright");
        });
    }
}
