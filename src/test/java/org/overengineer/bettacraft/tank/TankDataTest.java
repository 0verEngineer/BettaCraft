package org.overengineer.bettacraft.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Volume logic (PLAN 3.2) without Minecraft bootstrap. */
class TankDataTest {
    @Test
    void emptyLayerHasNoWater() {
        assertEquals(0, TankData.fillFor(0, 0, 4000));
        assertEquals(0, TankData.fillFor(3000, 4000, 4000), "water below this layer only");
    }

    @Test
    void fullLayer() {
        assertEquals(16, TankData.fillFor(4000, 0, 4000));
        assertEquals(16, TankData.fillFor(9000, 4000, 4000));
    }

    @Test
    void partialLayerRoundsUpSoAnyWaterIsVisible() {
        assertEquals(1, TankData.fillFor(100, 0, 16000));
        assertEquals(8, TankData.fillFor(2000, 0, 4000));
        assertEquals(4, TankData.fillFor(4000 + 1000, 4000, 4000));
    }

    @Test
    void zeroCapacityLayer() {
        assertEquals(0, TankData.fillFor(5000, 0, 0));
    }
}
