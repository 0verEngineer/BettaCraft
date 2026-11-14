package org.overengineer.bettacraft.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.config.ServerConfig.WaterChangeDifficulty;
import org.overengineer.bettacraft.filter.FilterMedium;
import org.overengineer.bettacraft.plumbing.WaterStore;
import org.overengineer.bettacraft.plumbing.interior.InteriorPlumbing;
import org.overengineer.bettacraft.plumbing.interior.Plumb;

/** M24: the nitrate model (difficulty, saw tooth of the water changes, plants), the interior pipe search, water mixing, media. */
class TankNitrateTest {
    /** Peak of the saw tooth with 25 % every {@code days} days, after many changes. */
    private static double peak(WaterChangeDifficulty d, int days, double load) {
        double rate = TankNitrate.ratePerDay(d, load, 0, 1.2);
        float n = 0;
        double max = 0;
        for (int change = 0; change < 60; change++) {
            for (int day = 0; day < days; day++) n = TankNitrate.step(n, 1, rate);
            max = n;
            // a quarter out (the concentration stays), a quarter of fresh water in
            n = TankNitrate.mix(n, 750, 0F, 250);
        }
        return max;
    }

    @Test
    void scheduleOfEachDifficultyKeepsANormalTankAtForty() {
        assertEquals(40, peak(WaterChangeDifficulty.EASY, 20, 1), 1.0);
        assertEquals(40, peak(WaterChangeDifficulty.NORMAL, 14, 1), 1.0);
        assertEquals(40, peak(WaterChangeDifficulty.REALISTIC, 7, 1), 1.0);
        // twice the fish: twice the nitrate
        assertEquals(80, peak(WaterChangeDifficulty.NORMAL, 14, 2), 2.0);
        assertEquals(0, TankNitrate.baseRate(WaterChangeDifficulty.OFF));
    }

    @Test
    void withoutChangesItGetsBadOnlyAfterLongTimes() {
        double r = TankNitrate.ratePerDay(WaterChangeDifficulty.NORMAL, 1, 0, 1.2);
        assertEquals(70, 50 / r, 0.5); // stress after 70 days on NORMAL
        assertEquals(168, 120 / r, 0.5); // lethal after 168 days
        double rr = TankNitrate.ratePerDay(WaterChangeDifficulty.REALISTIC, 1, 0, 1.2);
        assertEquals(35, 50 / rr, 0.5);
    }

    @Test
    void densePlantsTakeItAllUp() {
        assertTrue(TankNitrate.ratePerDay(WaterChangeDifficulty.NORMAL, 1, 1, 1.2) < 0);
        double half = TankNitrate.ratePerDay(WaterChangeDifficulty.NORMAL, 1, 0.5, 1.2);
        assertEquals(TankNitrate.baseRate(WaterChangeDifficulty.NORMAL) * 0.4, half, 1e-9);
        assertEquals(0, TankNitrate.step(3, 10, -1), 1e-6); // never below 0
    }

    @Test
    void evaporationConcentratesAndTopOffDilutesBack() {
        float n = TankNitrate.concentrate(30, 10000, 1000);
        assertEquals(33.33, n, 0.01);
        assertEquals(30, TankNitrate.mix(n, 9000, 0, 1000), 1e-3);
        assertEquals(TankNitrate.State.HIGH, TankNitrate.state(55, 50, 120));
        assertEquals(TankNitrate.State.RISING, TankNitrate.state(40, 50, 120));
        assertEquals(TankNitrate.State.LETHAL, TankNitrate.state(130, 50, 120));
        assertEquals(1.0, TankNitrate.algaeFactor(40, 50));
        assertEquals(2.0, TankNitrate.algaeFactor(150, 50), 1e-9);
    }

    @Test
    void interiorPipesLeadFromTheBulkheadToTheOutlets() {
        Long2IntOpenHashMap m = new Long2IntOpenHashMap();
        long port = BlockPos.asLong(8, 0, 8);
        m.put(port, Plumb.code(Plumb.PORT, 0));
        for (int y = 1; y <= 5; y++) m.put(BlockPos.asLong(8, y, 8), Plumb.code(Plumb.PIPE, 0));
        m.put(BlockPos.asLong(9, 5, 8), Plumb.code(Plumb.NOZZLE, 3));
        m.put(BlockPos.asLong(7, 2, 8), Plumb.code(Plumb.STRAINER, 0));
        // a lily pipe that is not joined
        m.put(BlockPos.asLong(20, 3, 20), Plumb.code(Plumb.LILY, 0));
        InteriorPlumbing.Net n = InteriorPlumbing.search(m, port);
        assertEquals(1, n.outflows().size());
        assertEquals(1, n.intakes().size());
        assertEquals(Plumb.NOZZLE, Plumb.kind(m.get(n.outflows().getLong(0))));
        assertEquals(3, Plumb.facing(m.get(n.outflows().getLong(0))));
        assertEquals(port, InteriorPlumbing.portInCell(m, 0, 0, 16));
        assertEquals(Long.MIN_VALUE, InteriorPlumbing.portInCell(m, 1, 0, 16));
    }

    @Test
    void storedWaterMixesItsValues() {
        assertNull(WaterStore.mix(null, 1000, null, 1000));
        WaterValues sea = new WaterValues(26, 35, 420, 0.8F, 20);
        WaterValues m = WaterStore.mix(sea, 1000, null, 1000);
        assertEquals(17.5, m.salinity(), 1e-4);
        assertEquals(10, m.nitrate(), 1e-4);
        assertEquals(26, m.temp(), 1e-4); // tap water takes the other's temperature here
        assertTrue(new WaterValues(25, 1, 0, 1, 0).fresh());
    }

    @Test
    void mediaLoseEfficiencyWithDirt() {
        assertEquals(1.0, FilterMedium.SPONGE.efficiency(0), 1e-9);
        assertEquals(0.2, FilterMedium.SPONGE.efficiency(1), 1e-9);
        assertEquals(0.0, FilterMedium.CARBON.efficiency(1), 1e-9);
        assertEquals(0.0, FilterMedium.NONE.efficiency(0), 1e-9);
    }
}
