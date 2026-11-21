package org.overengineer.bettacraft.critters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** M54: the small animals crawl head first, in the world as in the tank (the apple snails went backwards in the world). */
class CritterFacingTest {
    @Test
    void theHeadPointsWhereTheAnimalGoes() {
        for (int yaw = -180; yaw <= 180; yaw += 15) {
            double[] head = CritterFacing.head(CritterFacing.modelTurn(yaw));
            double[] go = CritterFacing.heading(yaw);
            assertEquals(go[0], head[0], 1e-9, "x at yaw " + yaw);
            assertEquals(go[1], head[1], 1e-9, "z at yaw " + yaw);
        }
    }

    @Test
    void theTankYawIsMinecraftsYaw() {
        // the tank's movement gives its yaw as atan2(-dx, dz) (CritterMovement#yawOf): the same convention as an entity's yaw
        double[][] dirs = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}, {0.6, 0.8}};
        for (double[] d : dirs) {
            float yaw = CritterMovement.yawOf(d[0], d[1]);
            double[] go = CritterFacing.heading(yaw);
            assertEquals(d[0], go[0], 1e-6);
            assertEquals(d[1], go[1], 1e-6);
        }
    }
}
