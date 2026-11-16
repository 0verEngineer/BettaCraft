package org.overengineer.bettacraft.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** M44: the automatic front of a cabinet row ({@link CabinetFront#layout}) for every length 1..32. */
class CabinetFrontTest {
    private static String row(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) {
            b.append(switch (CabinetFront.layout(n, i)) {
                case TWO -> 'T';
                case PAIR_L -> 'L';
                case PAIR_R -> 'R';
                case SINGLE -> 'S';
                case DRAWERS -> 'D';
            });
        }
        return b.toString();
    }

    @Test
    void shortRows() {
        assertEquals("T", row(1));
        assertEquals("LR", row(2));
        assertEquals("DSD", row(3));
        assertEquals("DLRD", row(4));
        assertEquals("DDSDD", row(5));
        assertEquals("DLRLRD", row(6));
        assertEquals("DLRSLRD", row(7));
        assertEquals("DLRLRLRD", row(8));
        assertEquals("DLRDSDLRD", row(9));
        assertEquals("DLRLRLRLRLRD", row(12));
    }

    @Test
    void everyRowUpTo32() {
        for (int n = 1; n <= CabinetFront.MAX_ROW; n++) {
            String r = row(n);
            assertEquals(n, r.length());
            // symmetric: block i mirrors block n-1-i (a left leaf mirrors a right leaf)
            for (int i = 0; i < n; i++) {
                assertEquals(CabinetFront.layout(n, i).mirrored(), CabinetFront.layout(n, n - 1 - i), n + ": " + r + " at " + i);
            }
            // no half double doors: every left leaf has its right leaf next to it and the other way round
            for (int i = 0; i < n; i++) {
                if (r.charAt(i) == 'L') assertTrue(i + 1 < n && r.charAt(i + 1) == 'R', n + ": " + r);
                if (r.charAt(i) == 'R') assertTrue(i > 0 && r.charAt(i - 1) == 'L', n + ": " + r);
            }
            // a single door only in odd rows, then exactly one in the middle
            long singles = r.chars().filter(c -> c == 'S').count();
            if (n % 2 == 0 || n == 1) assertEquals(0, singles, n + ": " + r);
            else {
                assertEquals(1, singles, n + ": " + r);
                assertEquals('S', r.charAt(n / 2), n + ": " + r);
            }
            // drawers at both ends from 3 blocks on, the old front only alone
            if (n >= 3) assertTrue(r.charAt(0) == 'D' && r.charAt(n - 1) == 'D', n + ": " + r);
            assertEquals(n == 1, r.contains("T"), n + ": " + r);
            // never more than three drawer blocks side by side (the ends of short odd rows)
            assertTrue(!r.contains("DDD") || n <= 5, n + ": " + r);
        }
    }

    @Test
    void longRowsPairByPosition() {
        for (int a = -6; a < 6; a++) {
            CabinetFront f = CabinetFront.longRow(a), g = CabinetFront.longRow(a + 1);
            assertEquals(f.mirrored(), g);
            assertTrue(f == CabinetFront.PAIR_L || f == CabinetFront.PAIR_R);
        }
        assertEquals(CabinetFront.PAIR_L, CabinetFront.longRow(0));
    }

    @Test
    void settings() {
        assertEquals(CabinetFrontSetting.AUTO, CabinetFrontSetting.byId(null));
        assertEquals(CabinetFrontSetting.AUTO, CabinetFrontSetting.byId("nonsense"));
        for (CabinetFrontSetting s : CabinetFrontSetting.values()) {
            assertEquals(s, CabinetFrontSetting.byId(s.id()));
            assertEquals(s, s.next().previous());
            assertEquals(s == CabinetFrontSetting.AUTO, s.fixed() == null);
        }
    }
}
