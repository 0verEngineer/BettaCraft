package org.overengineer.bettacraft.entity.betta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.config.ServerConfig;

/** M33 part d: death of old age (health.oldAge): the lifespan, the age counted with the care, older animals, the care modes. */
class OldAgeTest {
    @Test
    void lifespanIsYearsOfDays() {
        assertEquals(192, OldAge.lifespanDays(2, 96), 1e-9, "the default: 2 years of 96 days");
        assertEquals(36.5, OldAge.lifespanDays(0.1, 365), 1e-9);
        assertEquals(0, OldAge.lifespanDays(-1, 96), 1e-9);
    }

    @Test
    void ageIsCountedWithTheCareAndSaved() {
        BettaHealth h = new BettaHealth();
        assertFalse(h.ageKnown(), "a new health has no age until it is started");
        h.step(1, BettaHealth.Conditions.GOOD, BettaHealth.Params.DEFAULT);
        assertEquals(-1, h.ageDays(), 1e-9, "not counted before it is started");
        h.setAgeDays(10);
        h.step(1.5, BettaHealth.Conditions.GOOD, BettaHealth.Params.DEFAULT);
        assertEquals(11.5, h.ageDays(), 1e-6);
        BettaHealth back = BettaHealth.load(h.save());
        assertEquals(11.5, back.ageDays(), 1e-6, "saved and loaded");
        CompoundTag old = h.save();
        old.remove("age_days");
        assertFalse(BettaHealth.load(old).ageKnown(), "data of 1.25 has no age");
        old.putDouble("age_days", Double.NaN);
        assertEquals(0, BettaHealth.load(old).ageDays(), 1e-9, "corrupt age: 0");
    }

    @Test
    void olderAnimalsStartAtTheAgeOfTheirStage() {
        // egg 0.5, larva 0.5, fry 2, juvenile 2.5, subadult 3.5 (the defaults)
        java.util.function.ToDoubleFunction<LifeStage> d = s -> switch (s) {
            case EGG, LARVA -> 0.5;
            case FRY -> 2;
            case JUVENILE -> 2.5;
            case SUBADULT -> 3.5;
            case ADULT -> Double.POSITIVE_INFINITY;
        };
        assertEquals(1.0 + 0.7, OldAge.ageFromStage(LifeStage.FRY, 0.7, d), 1e-9);
        assertEquals(9.0, OldAge.ageFromStage(LifeStage.ADULT, 0, d), 1e-9, "a grown animal: the age at which it grew up");
        BettaHealth h = new BettaHealth();
        OldAge.start(h, LifeStage.JUVENILE, 1, d);
        assertEquals(4.0, h.ageDays(), 1e-9);
        OldAge.start(h, LifeStage.ADULT, 0, d);
        assertEquals(4.0, h.ageDays(), 1e-9, "started once");
    }

    @Test
    void diesOnlyWhenOnAndInNormalCare() {
        assertFalse(OldAge.due(false, ServerConfig.CareMode.NORMAL, 500, 192), "off by default");
        assertTrue(OldAge.due(true, ServerConfig.CareMode.NORMAL, 192, 192));
        assertFalse(OldAge.due(true, ServerConfig.CareMode.NORMAL, 191.9, 192));
        assertFalse(OldAge.due(true, ServerConfig.CareMode.NO_DEATH, 500, 192), "no death in NO_DEATH");
        assertFalse(OldAge.due(true, ServerConfig.CareMode.DECOR, 500, 192), "no death in DECOR");
        assertFalse(OldAge.due(true, ServerConfig.CareMode.NORMAL, -1, 192), "no age yet");
    }
}
