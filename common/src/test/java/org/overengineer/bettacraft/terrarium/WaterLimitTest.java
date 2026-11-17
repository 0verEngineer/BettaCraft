package org.overengineer.bettacraft.terrarium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.tank.TankShape;

/** M33.1: the water limit of a fixed paludarium ({@link WaterLimit#maxSurface}) and the inverse of the water surface. */
class WaterLimitTest {
    @Test
    void maxSurfaceKeepsTheLandShare() {
        // 20 columns: 17 flat at 2 interior blocks (32/16), 3 hills at 6 blocks (96/16): 15 % land needs the hills
        int[] tops = new int[20];
        java.util.Arrays.fill(tops, 32);
        tops[3] = tops[9] = tops[14] = 96;
        double h = WaterLimit.maxSurface(tops, 0.15);
        assertTrue(h < 6 - 0.05 && h > 5.8, "below the hills: " + h);
        // a land share of 20 % needs a fourth column above the water: only the flat ground is left
        double low = WaterLimit.maxSurface(tops, 0.2);
        assertTrue(low < 2 - 0.05 && low > 1.8, "below the flat ground: " + low);
        // the scan's rule (land = top above water + 0.05) still sees the share at the limit
        int land = 0;
        for (int t : tops) if (t / 16.0 > h + 0.05) land++;
        assertEquals(3, land);
        assertEquals(Double.POSITIVE_INFINITY, WaterLimit.maxSurface(tops, 0), "no share, no limit");
        assertEquals(0, WaterLimit.maxSurface(new int[0], 0.15), "no columns known: no water");
        assertTrue(WaterLimit.maxSurface(new int[]{0, 0, 0}, 0.15) < 0, "a bare floor: no water at all");
    }

    @Test
    void waterBelowIsTheInverseOfTheSurface() {
        int[] layers = {6, 6, 4}; // a free shape: 6 blocks, 6 blocks, 4 blocks on top
        for (long mb : new long[]{0, 1, 2999, 6000, 9500, 12000, 14000, 15999}) {
            double y = TankShape.surfaceY(layers, 10, mb, 2);
            long back = TankShape.waterBelow(layers, 10, y + 1e-9, 2);
            assertTrue(Math.abs(back - mb) <= 1, mb + " mB -> " + y + " -> " + back);
        }
        assertEquals(0, TankShape.waterBelow(layers, 10, 9.5, 2), "below the floor");
        assertEquals(16000, TankShape.waterBelow(layers, 10, 20, 2), "above the top: everything");
        assertEquals(9000, TankShape.waterBelow(layers, 10, 11.5, 2), "one and a half layers");
    }

    @Test
    void settingsByIdAndOrder() {
        assertEquals(TankModeSetting.AUTO, TankModeSetting.byId(""), "older worlds are automatic");
        assertEquals(TankModeSetting.AUTO, TankModeSetting.byId("nonsense"));
        for (TankModeSetting s : TankModeSetting.values()) assertEquals(s, TankModeSetting.byId(s.id()));
        assertEquals(TankModeSetting.AUTO, TankModeSetting.TERRARIUM.next());
        assertEquals(null, TankModeSetting.AUTO.fixed());
        assertEquals(TerrariumMode.PALUDARIUM, TankModeSetting.PALUDARIUM.fixed());
        assertEquals(TankModeSetting.AUTO, TankModeSetting.byOrdinal(99));
    }
}
