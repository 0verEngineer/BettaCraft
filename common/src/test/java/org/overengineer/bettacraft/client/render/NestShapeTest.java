package org.overengineer.bettacraft.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** M15: the domed bubble nest stays inside the tank, and the smaller name sign. */
class NestShapeTest {
    /** {@code TankBreeding.nestSpot}: the nest keeps this far from a side wall. */
    private static final double WALL_MARGIN = 0.3 + 1.0 / 16;

    @Test
    void domeFitsUnderTheTopAndBesideTheWalls() {
        float size = 0.7F;
        // above the surface: no higher than the standard air gap of 1/16 block below the tank top
        assertTrue(size * NestRenderer.UP <= 1.0 / 16, "dome top " + size * NestRenderer.UP);
        // below the surface: clearly thicker than the old flat disc (0.035), so it shows from the side
        assertTrue(size * NestRenderer.DOWN >= 0.1, "foam depth " + size * NestRenderer.DOWN);
        float reach = NestRenderer.reach(size);
        assertTrue(reach <= WALL_MARGIN, "reach " + reach + " beyond the wall margin " + WALL_MARGIN);
        assertTrue(reach >= 0.25, "the nest is still about as wide as before: " + reach);
    }

    @Test
    void raggedOutlineMatchesTheTexture() {
        for (int a = 0; a < 360; a++) {
            float r = NestRenderer.rag((float) Math.toRadians(a));
            assertTrue(r > 0.3F && r < 0.49F, "outline radius " + r + " at " + a);
        }
    }

    @Test
    void signIsSmallerByDefault() {
        // without a loaded client config the factor is 1
        assertEquals(0.12F, TankSignRenderer.plateHeight(), 1e-6);
    }
}
