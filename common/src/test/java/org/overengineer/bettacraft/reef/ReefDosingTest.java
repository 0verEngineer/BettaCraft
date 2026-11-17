package org.overengineer.bettacraft.reef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.chem.WaterChemistry;
import org.overengineer.bettacraft.tank.TankData;

/** M25 part c: alkalinity, magnesium and trace elements of salt water ("Balling light") and what the dosing pump adds. */
class ReefDosingTest {
    private static TankData tank(long mb) {
        int n = (int) Math.max(1, mb / 1000);
        java.util.List<BlockPos> blocks = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) blocks.add(new BlockPos(i % 8, i / 64, (i / 8) % 8));
        TankData t = TankData.create(blocks, 16);
        t.setWaterMb(mb);
        return t;
    }

    @Test
    void valuesFollowTheSalt() {
        TankData t = tank(1000);
        assertEquals(WaterChemistry.TAP_KH, t.kh(), 1e-4, "a new tank holds tap water");
        assertEquals(0F, t.trace(), 1e-6);
        t.setSalinity(35F);
        assertEquals(WaterChemistry.SEA_KH, t.kh(), 1e-3, "salt brings the alkalinity of sea water");
        assertEquals(WaterChemistry.SEA_MG, t.magnesium(), 1e-2);
        assertEquals(WaterChemistry.SEA_TRACE, t.trace(), 1e-4);
        // a depleted tank gets saltier by evaporation: only the difference comes in
        t.setKh(5F);
        t.setSalinity(36F);
        assertEquals(5F + WaterChemistry.khOf(36) - WaterChemistry.khOf(35), t.kh(), 1e-4);
    }

    @Test
    void waterChangeWithSeaWaterRestores() {
        TankData t = tank(4000);
        t.setSalinity(35F);
        t.setKh(4F);
        t.setMagnesium(1000F);
        ReefDosing.Snapshot s = ReefDosing.Snapshot.of(t);
        s.mixIn(t, 4000, 35F, 1000); // a fifth of sea water
        assertEquals((4F * 4 + 8F) / 5, t.kh(), 1e-3);
        assertEquals((1000F * 4 + 1300F) / 5, t.magnesium(), 1e-2);
        // evaporation leaves them behind
        ReefDosing.Snapshot e = ReefDosing.Snapshot.of(t);
        e.concentrate(t, 5000, 1000);
        assertEquals((4F * 4 + 8F) / 5 * 5 / 4, t.kh(), 1e-3);
    }

    @Test
    void stonyCoralsUseAlkalinityAndMagnesium() {
        TankData t = tank(8000); // 8 water blocks
        t.setSalinity(35F);
        ReefDosing.useWithCalcium(t, 40);
        assertEquals(8F - 40 / 7.14F * 0.25F / 8, t.kh(), 1e-3);
        assertEquals(1300F - 40 * 0.15F / 8, t.magnesium(), 1e-2);
        assertEquals(1.0, ReefDosing.stonyFactor(t), 1e-9);
        t.setKh(4F);
        assertEquals(0.5, ReefDosing.stonyFactor(t), 1e-9, "low alkalinity halves the growth");
        t.setMagnesium(1100F);
        assertEquals(0.3, ReefDosing.stonyFactor(t), 1e-9);
        // fresh water: no reef chemistry
        TankData fresh = tank(8000);
        fresh.setKh(1F);
        assertEquals(1.0, ReefDosing.stonyFactor(fresh), 1e-9);
    }

    @Test
    void dosingRaisesTheValuesSharedByTheVolume() {
        TankData t = tank(40_000); // 40 water blocks
        t.setSalinity(35F);
        float ca = t.calcium();
        ReefDosing.dose(t, ReefDosing.Additive.CALCIUM, 10);
        assertEquals(ca + 10 * 2.0 / 40, t.calcium(), 1e-3);
        ReefDosing.dose(t, ReefDosing.Additive.ALKALINITY, 10);
        assertEquals(8F + 10 * 0.07 / 40, t.kh(), 1e-4);
        assertFalse(ReefDosing.traceBonus(t), "fresh sea water: no colour bonus yet");
        for (int i = 0; i < 40; i++) ReefDosing.dose(t, ReefDosing.Additive.TRACE, 10);
        assertTrue(ReefDosing.traceBonus(t));
        // the care step: magnesium falls by a mg/L a day, the trace elements by 4 %
        float mg = t.magnesium(), tr = t.trace();
        ReefDosing.step(t, 2);
        assertEquals(mg - 2, t.magnesium(), 1e-3);
        assertEquals(tr - 0.08F, t.trace(), 1e-4);
        assertTrue(ReefDosing.dosable(t));
        assertFalse(ReefDosing.dosable(tank(8000)), "the pump leaves fresh water alone");
    }
}
