package org.overengineer.bettacraft.terrarium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** M54: the habits of the amphibians and reptiles (hours, tempo, actions). */
class HabitsTest {
    @Test
    void everySpeciesHasHabits() {
        for (HerpSpecies s : HerpSpecies.ALL) {
            Habits h = Habits.of(s);
            assertNotNull(h, s.id());
            assertTrue(h.tempo() >= 1.0 && h.tempo() <= 2.0, s.id() + " tempo");
            assertTrue(h.dashChance() == 0 || h.dashFactor() > 1.5, s.id() + " a fast phase is fast");
            assertFalse(h.idle().isEmpty(), s.id() + " does something while it sits");
            assertFalse(h.idle().contains(Habits.Action.COIL), s.id() + " coiling is a rest, not an idle action");
        }
        assertTrue(Habits.SNAKE.coils() && Habits.SNAKE.idle().contains(Habits.Action.HEAD_UP) && Habits.SNAKE.idle().contains(Habits.Action.TONGUE));
    }

    @Test
    void hoursOfTheDay() {
        // 6000 = noon, 18000 = midnight, 12500 = dusk, 23500 = dawn
        assertTrue(Habits.DART_FROG.active(6000, true));
        assertFalse(Habits.DART_FROG.active(18000, false));
        assertFalse(Habits.LEOPARD_GECKO.active(6000, true));
        assertTrue(Habits.LEOPARD_GECKO.active(18000, false));
        assertTrue(Habits.LEOPARD_GECKO.active(12500, true), "a gecko starts at dusk");
        assertTrue(Habits.SNAKE.active(23500, true) && Habits.SNAKE.active(12000, true) && !Habits.SNAKE.active(6000, true));
        assertTrue(Habits.SNAKE.restFactor(12500, true) < Habits.SNAKE.restFactor(18000, false), "the snake's best hours are at dusk");
        assertTrue(Habits.LEOPARD_GECKO.restFactor(6000, true) > 1.5 && Habits.LEOPARD_GECKO.hideFactor(6000, true) > 1.5, "a gecko rests and hides by day");
    }

    @Test
    void idleActionsComeFromTheList() {
        Random r = new Random(54);
        Set<Habits.Action> seen = EnumSet.noneOf(Habits.Action.class);
        for (int i = 0; i < 400; i++) seen.add(Habits.SNAKE.randomIdle(r::nextLong));
        assertEquals(EnumSet.copyOf(Habits.SNAKE.idle()), seen);
        for (Habits.Action a : Habits.Action.values()) {
            if (a == Habits.Action.NONE) continue;
            int t = a.ticks(r::nextLong);
            assertTrue(t > 10, a + " lasts a while");
            assertEquals(a, Habits.Action.of(a.ordinal()));
        }
        assertEquals(Habits.Action.NONE, Habits.Action.of(99));
    }
}
