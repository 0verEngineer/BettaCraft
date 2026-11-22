package org.overengineer.bettacraft.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.client.screen.TankScreen;

/**
 * M62 part 4: the tank menu grows with the screen. The smallest menu (320 x 240 GUI pixels, GUI scale 4 at 1280 x 960) keeps
 * the old 234 x 123 content area; a larger screen gives more room up to a cap, and leaves room beside it for a recipe viewer;
 * the tab widths always fill the row exactly.
 */
class TankMenuLayoutTest {
    @Test
    void theSmallestScreenKeepsTheOldMenu() {
        assertEquals(234, TankScreen.contentWidth(320));
        assertEquals(123, TankScreen.contentHeight(240));
        assertEquals(234, TankScreen.contentWidth(200), "never smaller than the old menu");
        assertEquals(123, TankScreen.contentHeight(100), "never lower than the old menu");
    }

    @Test
    void aLargerScreenGivesARoomierMenuUpToTheCap() {
        // 1920 x 1080 at GUI scale 4: 480 x 270
        int w = TankScreen.contentWidth(480), h = TankScreen.contentHeight(270);
        assertTrue(w > 234 && w + 14 <= 480 * 0.7, "wider, a recipe viewer still has room beside it: " + w);
        assertTrue(h > 123 && h + 107 <= 270 - 16, "taller, with a margin above and below: " + h);
        // GUI scale 1 or 2: the cap
        assertEquals(TankScreen.MAX_CONTENT_W, TankScreen.contentWidth(1920));
        assertEquals(TankScreen.MAX_CONTENT_H, TankScreen.contentHeight(1080));
        for (int sw = 320; sw < 2000; sw += 7) assertTrue(TankScreen.contentWidth(sw) >= TankScreen.contentWidth(sw - 7), "grows steadily");
    }

    @Test
    void tabsFillTheRowAndLabelsShrinkOnlyWhenNeeded() {
        int[] labels = {48, 42, 48, 30, 42, 30, 36, 30, 18};
        int sum = 0;
        for (int l : labels) sum += l;
        for (int total : new int[]{236, 300, 382}) {
            float s = TankScreen.tabScale(sum, labels.length, total);
            assertTrue(s >= 0.55F && s <= 1F, "scale " + s);
            int[] w = TankScreen.tabWidths(labels, s, total);
            int got = 0;
            for (int x : w) got += x;
            assertEquals(total, got, "the tabs fill the row");
        }
        assertEquals(1F, TankScreen.tabScale(sum, labels.length, 382), "full-size labels in a wide menu");
        assertTrue(TankScreen.tabScale(sum, labels.length, 236) < 1F, "smaller labels in the smallest menu");
    }
}
