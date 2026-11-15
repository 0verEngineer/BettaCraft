package org.overengineer.bettacraft.fertilizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.winding.WindSpring;
import org.overengineer.bettacraft.winding.Winding;

/** M37: the pure rules of liquid fertilizer and of the crank's spring. */
class FertilizerTest {
    @Test
    void growthRisesUpToTheRightLevel() {
        assertEquals(1.0, Fertilizer.growth(0, 0.5), 1e-9);
        assertEquals(1.25, Fertilizer.growth(0.5, 0.5), 1e-9);
        assertEquals(1.5, Fertilizer.growth(1.0, 0.5), 1e-9);
        assertEquals(1.5, Fertilizer.growth(4.0, 0.5), 1e-9, "more brings no more");
        assertEquals(1.0, Fertilizer.growth(1.0, 0), 1e-9);
    }

    @Test
    void algaeOnlyFromTheOverdose() {
        assertEquals(1.0, Fertilizer.algae(1.0, 1.5, 3), 1e-9);
        assertEquals(1.0, Fertilizer.algae(1.5, 1.5, 3), 1e-9);
        assertEquals(2.0, Fertilizer.algae(2.25, 1.5, 3), 1e-9);
        assertEquals(3.0, Fertilizer.algae(3.0, 1.5, 3), 1e-9);
        assertEquals(3.0, Fertilizer.algae(20, 1.5, 3), 1e-9, "capped");
    }

    @Test
    void states() {
        assertEquals(Fertilizer.State.NONE, Fertilizer.state(0, 1.5));
        assertEquals(Fertilizer.State.LOW, Fertilizer.state(0.3, 1.5));
        assertEquals(Fertilizer.State.GOOD, Fertilizer.state(1.0, 1.5));
        assertEquals(Fertilizer.State.GOOD, Fertilizer.state(1.5, 1.5));
        assertEquals(Fertilizer.State.OVERDOSED, Fertilizer.state(1.6, 1.5));
    }

    @Test
    void springWindsInStepsAndRunsDown() {
        int[] changes = {0};
        WindSpring s = new WindSpring(() -> 24000, () -> 25, () -> changes[0]++);
        assertTrue(s.empty());
        assertEquals(960, s.step());
        int clicks = 0;
        while (s.windStep()) clicks++;
        assertEquals(25, clicks);
        assertTrue(s.full());
        assertEquals(100, s.percent());
        assertTrue(s.use(1000));
        assertEquals(23000, s.ticks());
        assertFalse(s.use(30000), "all or nothing");
        assertEquals(23000, s.drain(30000));
        assertTrue(s.empty());
        assertTrue(changes[0] > 0);
        CompoundTag t = new CompoundTag();
        s.set(500);
        s.save(t, "spring");
        WindSpring back = new WindSpring(() -> 24000, () -> 25, () -> {});
        back.load(t, "spring");
        assertEquals(500, back.ticks());
        // the food maker of an older version stored an int
        CompoundTag old = new CompoundTag();
        old.putInt("spring", 12000);
        back.load(old, "spring");
        assertEquals(12000, back.ticks());
    }

    @Test
    void ticksForTheWorkOfFe() {
        assertEquals(0, Winding.ticksFor(0, 8));
        assertEquals(40, Winding.ticksFor(320, 8), "a pump batch of 40 ticks");
        assertEquals(1, Winding.ticksFor(3, 8), "at least one tick");
        assertEquals(20, Winding.ticksFor(400, 20));
    }
}
