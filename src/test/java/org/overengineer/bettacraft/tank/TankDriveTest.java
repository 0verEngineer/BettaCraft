package org.overengineer.bettacraft.tank;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.equipment.DriveSide;

/** M41: the planning of Create's drive for the tank buffer (pure parts) and the cabinet's drive sides. */
class TankDriveTest {
    @Test
    void speedToWork() {
        assertEquals(0, TankDrive.work(0F, 64F));
        assertEquals(0.5, TankDrive.work(32F, 64F), 1e-9);
        assertEquals(1, TankDrive.work(64F, 64F), 1e-9);
        assertEquals(1, TankDrive.work(256F, 64F), 1e-9, "faster gives no more");
        assertEquals(1, TankDrive.work(16F, 16F), 1e-9, "the full speed is configurable");
        assertEquals(0, TankDrive.work(Float.NaN, 64F));
    }

    @Test
    void sharesFollowWhatEachCanGive() {
        // the tank wants less than the inputs can give: shared by their power
        assertArrayEquals(new double[]{75, 25}, TankDrive.shares(new double[]{256, 256.0 / 3}, 100, 1024), 1e-9);
        // the tank wants more: every input gives all it can
        assertArrayEquals(new double[]{256, 128}, TankDrive.shares(new double[]{256, 128}, 5000, 1024), 1e-9);
        // several adapters hit the limit of the tank
        double[] five = TankDrive.shares(new double[]{256, 256, 256, 256, 256}, 1e6, 1024);
        double sum = 0;
        for (double s : five) sum += s;
        assertEquals(1024, sum, 1e-9);
        assertEquals(1024 / 5.0, five[0], 1e-9);
        // nothing wanted, nothing turning
        assertArrayEquals(new double[]{0, 0}, TankDrive.shares(new double[]{256, 256}, 0, 1024), 1e-9);
        assertArrayEquals(new double[]{0}, TankDrive.shares(new double[]{0}, 100, 1024), 1e-9);
    }

    @Test
    void stressScalesWithTheLoad() {
        // 2 SU per FE/t, at least 0.5 SU per RPM
        assertEquals(512, TankDrive.stress(256, 64F, 2.0, 0.5), 1e-9);
        assertEquals(64, TankDrive.stress(32, 64F, 2.0, 0.5), 1e-9);
        assertEquals(32, TankDrive.stress(0, 64F, 2.0, 0.5), 1e-9, "idle");
        assertEquals(0, TankDrive.stress(100, 0F, 2.0, 0.5), 1e-9, "standing still costs nothing");
        // Create multiplies the impact by the speed
        assertEquals(8F, TankDrive.impact(256, 64F, 2.0, 0.5), 1e-6);
        assertEquals(8F, TankDrive.impact(128, -32F, 2.0, 0.5), 1e-6, "either direction");
        assertEquals(0.5F, TankDrive.impact(0, 64F, 2.0, 0.5), 1e-6);
    }

    @Test
    void cabinetDriveSides() {
        assertNull(DriveSide.NONE.face(Direction.NORTH));
        assertEquals(Direction.SOUTH, DriveSide.BACK.face(Direction.NORTH));
        assertEquals(Direction.NORTH, DriveSide.FRONT.face(Direction.NORTH));
        assertEquals(Direction.EAST, DriveSide.LEFT.face(Direction.NORTH));
        assertEquals(Direction.WEST, DriveSide.RIGHT.face(Direction.NORTH));
        assertEquals(Direction.DOWN, DriveSide.BOTTOM.face(Direction.EAST));
        assertEquals(DriveSide.NONE, DriveSide.byId("sideways"));
        assertEquals(DriveSide.NONE, DriveSide.BOTTOM.next());
        assertEquals(DriveSide.BOTTOM, DriveSide.NONE.previous());
        assertEquals(DriveSide.LEFT, DriveSide.byIndex(2));
    }
}
