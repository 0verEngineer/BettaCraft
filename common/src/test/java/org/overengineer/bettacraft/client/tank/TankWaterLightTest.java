package org.overengineer.bettacraft.client.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * M63: the smooth light of the tank water ({@link TankWaterLight}). Neighbouring blocks must give their shared corners the
 * same light (no step at the block border, the rectangles of the report), and no corner may be darker than the block itself
 * (the chunk renderers keep the brighter of a quad's light and the block's).
 */
class TankWaterLightTest {
    private static int pack(int block, int sky) {
        return block << 4 | sky << 20;
    }

    /** A light field of 6 x 3 x 3 blocks (x 0..5): block light falls off from a torch at x = 0, sky light 15 under open sky only at x >= 3. */
    private static int field(int x, int y, int z) {
        int block = Math.max(0, 14 - 3 * x - Math.abs(y - 1) - Math.abs(z - 1));
        int sky = x >= 3 ? 15 : 9;
        return pack(block, sky);
    }

    private static long cornersOf(int bx) {
        int[] around = new int[27];
        for (int dz = -1; dz <= 1; dz++) for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
            around[(dx + 1) + 3 * (dy + 1) + 9 * (dz + 1)] = field(bx + dx, 1 + dy, 1 + dz);
        }
        return TankWaterLight.corners(around);
    }

    @Test
    void neighboursShareTheLightOfTheirCommonCorners() {
        for (int bx = 1; bx <= 3; bx++) {
            long a = cornersOf(bx), b = cornersOf(bx + 1);
            for (float y : new float[]{0F, 0.6F, 1F}) {
                for (float z : new float[]{0.02F, 0.98F}) {
                    assertEquals(TankWaterLight.at(a, 0.99F, y, z), TankWaterLight.at(b, 0.01F, y, z),
                            "corner light at the border of blocks " + bx + " and " + (bx + 1) + " (y " + y + ", z " + z + ")");
                }
            }
        }
    }

    @Test
    void noCornerIsDarkerThanItsBlock() {
        for (int bx = 1; bx <= 4; bx++) {
            long c = cornersOf(bx);
            int own = field(bx, 1, 1);
            for (int i = 0; i < 8; i++) {
                int l = TankWaterLight.at(c, i & 1, (i >> 1) & 1, (i >> 2) & 1);
                assertTrue(((l >> 4) & 15) >= ((own >> 4) & 15) && ((l >> 20) & 15) >= ((own >> 20) & 15),
                        "corner " + i + " of block " + bx + " is not darker than the block");
            }
        }
    }

    @Test
    void theSurfaceOfAPartFilledBlockTakesTheUpperCorners() {
        int[] around = new int[27];
        // only the block above (dx 0, dy +1, dz 0) is lit: a lamp on the lid
        around[1 + 3 * 2 + 9] = pack(15, 0);
        long c = TankWaterLight.corners(around);
        assertEquals(15, (TankWaterLight.at(c, 0.5F, 0.4F, 0.5F) >> 4) & 15, "the surface at 0.4 gets the light from above");
        assertEquals(0, (TankWaterLight.at(c, 0.5F, 0.03F, 0.5F) >> 4) & 15, "the bottom does not");
        assertEquals(TankWaterLight.uniform(pack(7, 12)), TankWaterLight.corners(filled(pack(7, 12))), "an even field gives the same light everywhere");
    }

    private static int[] filled(int packed) {
        int[] a = new int[27];
        java.util.Arrays.fill(a, packed);
        return a;
    }
}
