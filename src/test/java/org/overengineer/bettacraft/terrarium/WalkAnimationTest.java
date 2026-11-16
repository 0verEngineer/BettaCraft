package org.overengineer.bettacraft.terrarium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** M44: the walk animation of mobs in tanks (scale, factor, the limit), with the distances measured in the M44 analysis. */
class WalkAnimationTest {
    @Test
    void parse() {
        assertEquals(WalkAnimation.AUTO, WalkAnimation.parse("auto"));
        assertEquals(WalkAnimation.AUTO, WalkAnimation.parse(" AUTO "));
        assertEquals(2.5F, WalkAnimation.parse("2.5"));
        assertEquals(2.5F, WalkAnimation.parse("2,5"));
        assertEquals(0F, WalkAnimation.parse("0"));
        assertNull(WalkAnimation.parse("-1"));
        assertNull(WalkAnimation.parse("11"));
        assertNull(WalkAnimation.parse("fast"));
        assertNull(WalkAnimation.parse(null));
    }

    @Test
    void autoFactorBySize() {
        assertEquals(WalkAnimation.SHRUNK, WalkAnimation.factor(WalkAnimation.AUTO, 0.18F));
        assertEquals(WalkAnimation.FULL, WalkAnimation.factor(WalkAnimation.AUTO, 1F));
        assertEquals(2.5F, WalkAnimation.factor(2.5F, 0.18F));
    }

    private static float swing(double perTick, float scale, float setting) {
        return WalkAnimation.swing(WalkAnimation.distance(perTick, 0, false, scale, WalkAnimation.factor(setting, scale)));
    }

    @Test
    void measuredMobsSwingLikeOutside() {
        // vanilla: a zombie in glass (0.18, 0.011 blocks a tick) barely swings; the chicken in a terrarium neither
        assertTrue(WalkAnimation.swing(0.011F) < 0.05F);
        // auto: every measured mob between 0.2 and 0.7 (the mobs outside: 0.23-0.31 on average, 0.45-0.9 walking)
        double[][] mobs = {{0.011, 0.18}, {0.012, 0.18}, {0.019, 0.2}, {0.018, 1}, {0.023, 1}, {0.029, 1}, {0.020, 1}, {0.025, 1}};
        for (double[] m : mobs) {
            float s = swing(m[0], (float) m[1], WalkAnimation.AUTO);
            assertTrue(s >= 0.2F && s <= 0.7F, "auto " + m[0] + " at scale " + m[1] + ": " + s);
        }
        // a fixed 2.5 for all puts the spider at the limit (it looks like running)
        assertTrue(swing(0.019, 0.2F, 2.5F) > 0.9F);
        // the limit is 1
        assertEquals(1F, swing(0.5, 0.18F, WalkAnimation.AUTO));
        // 0 = vanilla: no movement counts (the mixin leaves vanilla alone then)
        assertEquals(0F, swing(0.02, 1F, 0F));
        // a tiny scale is clamped
        assertEquals(WalkAnimation.distance(0.01, 0, false, 0.05F, 1F), WalkAnimation.distance(0.01, 0, false, 0.001F, 1F));
    }

    @Test
    void climbersCountTheClimb() {
        assertTrue(WalkAnimation.mostlyVertical(0, 0.014));
        assertTrue(!WalkAnimation.mostlyVertical(0.02, 0.005));
        assertTrue(!WalkAnimation.mostlyVertical(0, 0));
        // up the glass: without the climb the legs stand still
        assertEquals(0F, WalkAnimation.distance(0, 0.014, false, 1F, 3F));
        assertTrue(WalkAnimation.swing(WalkAnimation.distance(0, 0.014, true, 0.2F, 1.5F)) > 0.2F);
    }
}
