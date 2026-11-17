package org.overengineer.bettacraft.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.tank.BigTankRules.Factors;
import org.overengineer.bettacraft.tank.BigTankRules.Need;
import org.overengineer.bettacraft.tank.BigTankRules.Verdict;
import org.overengineer.bettacraft.tank.TankInhabitants.Acceptance;

/** M30: the size rule of the big animals and the interior scale of big tanks. */
class BigTankRulesTest {
    private static final Factors F = Factors.DEFAULT;

    @Test
    void theTargetSizesOfTheMilestone() {
        assertEquals(new Need(12, 6, 3, 100), BigTankRules.need(1.0, F), "a 1 m bamboo shark");
        assertEquals(new Need(32, 16, 8, 1758), BigTankRules.need(2.6, F), "a 2.6 m dolphin");
    }

    @Test
    void sidesInAnyOrientationAndTheHeight() {
        assertEquals(Verdict.OK, BigTankRules.check(12, 3, 6, 216, 0, 1.0, F));
        assertEquals(Verdict.OK, BigTankRules.check(6, 3, 12, 216, 0, 1.0, F), "turned by 90 degrees");
        assertEquals(Verdict.TOO_SMALL, BigTankRules.check(11, 3, 6, 198, 0, 1.0, F), "one block too short");
        assertEquals(Verdict.TOO_SMALL, BigTankRules.check(12, 2, 6, 144, 0, 1.0, F), "too low");
        assertEquals(Verdict.TOO_SMALL, BigTankRules.check(12, 3, 5, 180, 0, 1.0, F), "too narrow");
    }

    @Test
    void waterForEveryBigAnimal() {
        // 32 x 16 x 8 = 4096 water blocks: two dolphins (1757.6 each), not three
        double one = BigTankRules.water(2.6, F);
        assertEquals(Verdict.OK, BigTankRules.check(32, 8, 16, 4096, one, 2.6, F));
        assertEquals(Verdict.TOO_LITTLE_WATER, BigTankRules.check(32, 8, 16, 4096, 2 * one, 2.6, F));
        // a free shape whose box is big enough but which holds too little water
        assertEquals(Verdict.TOO_LITTLE_WATER, BigTankRules.check(12, 3, 6, 90, 0, 1.0, F));
    }

    @Test
    void interiorScaleOfBigTanks() {
        assertEquals(16, BigTankRules.scaleFor(16, 16, 16, 16, 16, 32), "the old largest tank keeps scale 16");
        assertEquals(8, BigTankRules.scaleFor(17, 4, 6, 16, 16, 32));
        assertEquals(8, BigTankRules.scaleFor(32, 16, 8, 16, 16, 32), "the dolphin tank: 256 x 128 x 64 interior blocks");
        assertEquals(4, BigTankRules.scaleFor(33, 8, 8, 16, 16, 32));
        assertEquals(8, BigTankRules.scaleFor(4, 4, 4, 8, 16, 32), "never larger than before (config default scale 8)");
        assertEquals(4, BigTankRules.scaleFor(20, 4, 4, 4, 16, 32));
    }

    @Test
    void bigAnimalsDependOnTheTank() {
        var dolphin = new InhabitantRules.Facts("minecraft:dolphin", 0.9F, 0.6F, false, false, false, false, true, 2.6);
        assertEquals(Acceptance.BIG, InhabitantRules.decide(dolphin, InhabitantRules.Settings.DEFAULT));
        var off = new InhabitantRules.Settings(List.of(), List.of(), 0.8, true, false);
        assertEquals(Acceptance.TOO_BIG, InhabitantRules.decide(dolphin, off), "features.bigTanks.bigAnimals off: too big as before");
        var whale = new InhabitantRules.Facts("x:whale", 4F, 3F, false, false, false, false, true);
        assertEquals(Acceptance.TOO_BIG, InhabitantRules.decide(whale, InhabitantRules.Settings.DEFAULT), "unknown big animals stay too big");
        var boss = new InhabitantRules.Facts("x:leviathan", 4F, 3F, false, false, true, true, true, 20);
        assertEquals(Acceptance.BOSS, InhabitantRules.decide(boss, InhabitantRules.Settings.DEFAULT), "bosses never");
        var shark = new InhabitantRules.Facts("x:shark", 1.2F, 0.6F, false, false, false, true, true, 2);
        assertEquals(Acceptance.HOSTILE, InhabitantRules.decide(shark, InhabitantRules.Settings.DEFAULT), "hostile big animals never");
        var denied = new InhabitantRules.Facts("minecraft:dolphin", 0.9F, 0.6F, false, false, false, false, true, 2.6);
        assertEquals(Acceptance.BLACKLISTED, InhabitantRules.decide(denied,
                new InhabitantRules.Settings(List.of(), List.of("minecraft:dolphin"), 0.8, true, true)), "the deny list wins");
    }

    /** M35: the size rule of a terrarium measures the land, the water part or all of the tank; the rooms shared. */
    @Test
    void terrariumVolumes() {
        assertEquals(60, BigTankRules.blocks(BigTankRules.Volume.LAND, 200, 60), 1e-9);
        assertEquals(140, BigTankRules.blocks(BigTankRules.Volume.WATER, 200, 60), 1e-9);
        assertEquals(200, BigTankRules.blocks(BigTankRules.Volume.ALL, 200, 60), 1e-9);
        assertEquals(200, BigTankRules.blocks(BigTankRules.Volume.WATER, 200, 0), 1e-9, "an aquarium: all water");
        assertEquals(200, BigTankRules.blocks(BigTankRules.Volume.LAND, 200, 500), 1e-9, "never more than the tank");
        assertTrue(BigTankRules.Volume.ALL.shares(BigTankRules.Volume.LAND) && BigTankRules.Volume.WATER.shares(BigTankRules.Volume.ALL));
        assertFalse(BigTankRules.Volume.LAND.shares(BigTankRules.Volume.WATER));
        // a 1 m tortoise on 12 x 3 x 6 blocks of land (216): fits; a 2 m alligator needs 24 x 12 x 6 and 800 blocks
        BigTankRules.Factors f = BigTankRules.Factors.DEFAULT;
        assertEquals(BigTankRules.Verdict.OK, BigTankRules.check(12, 3, 6, BigTankRules.blocks(BigTankRules.Volume.LAND, 216, 216), 0, 1.0, f));
        assertEquals(BigTankRules.Verdict.TOO_LITTLE_WATER, BigTankRules.check(12, 3, 6, BigTankRules.blocks(BigTankRules.Volume.LAND, 216, 50), 0, 1.0, f));
        assertEquals(BigTankRules.Verdict.TOO_SMALL, BigTankRules.check(16, 6, 8, 768, 0, 2.0, f));
        assertEquals(BigTankRules.Verdict.OK, BigTankRules.check(24, 6, 12, BigTankRules.blocks(BigTankRules.Volume.ALL, 1728, 900), 0, 2.0, f));
    }
}
