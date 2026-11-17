package org.overengineer.bettacraft.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/** M32 part b: the rigid moves of travelling tanks (Create contraptions, Sable sub-levels). */
class TankMoveMapTest {
    @Test
    void twentyFourProperRotationsIdentityFirst() {
        Set<String> seen = new HashSet<>();
        for (int[] r : TankMoveMap.rotations()) {
            assertEquals(1, TankMoveMap.det(r));
            assertTrue(TankMoveMap.valid(r));
            assertTrue(seen.add(java.util.Arrays.toString(r)));
        }
        assertEquals(24, seen.size());
        assertTrue(new TankMoveMap(BlockPos.ZERO, BlockPos.ZERO, TankMoveMap.rotations().getFirst()).identityRotation());
        for (int i = 1; i < 4; i++) assertTrue(new TankMoveMap(BlockPos.ZERO, BlockPos.ZERO, TankMoveMap.rotations().get(i)).upright());
    }

    @Test
    void quarterTurnMatchesMinecraftsClockwiseRotation() {
        BlockPos from = new BlockPos(10, 64, 10), to = new BlockPos(-3, 70, 5);
        TankMoveMap m = new TankMoveMap(from, to, TankMoveMap.rotations().get(1));
        assertEquals(to, m.block(from));
        // Rotation.CLOCKWISE_90 turns the offset (x, y, z) into (-z, y, x)
        assertEquals(to.offset(-3, 2, 1), m.block(from.offset(1, 2, 3)));
        assertEquals(Direction.WEST, m.direction(Direction.SOUTH));
        assertEquals(1, m.quarterTurns());
        assertEquals(90F, m.yaw(0F), 1e-3);
        assertTrue(m.upright());
        assertFalse(m.identityRotation());
    }

    @Test
    void pointsTurnWithTheirBlock() {
        BlockPos from = new BlockPos(0, 0, 0), to = new BlockPos(100, 5, 100);
        for (int[] r : TankMoveMap.rotations()) {
            TankMoveMap m = new TankMoveMap(from, to, r);
            for (BlockPos b : new BlockPos[]{BlockPos.ZERO, new BlockPos(2, 1, -3), new BlockPos(-1, 4, 7)}) {
                Vec3 inside = m.point(b.getX() + 0.2, b.getY() + 0.9, b.getZ() + 0.6);
                assertEquals(m.block(b), BlockPos.containing(inside), "a point stays in its block");
                Vec3 centre = m.point(b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5);
                assertEquals(Vec3.atCenterOf(m.block(b)), centre);
            }
        }
    }

    @Test
    void translationOnly() {
        TankMoveMap m = TankMoveMap.translation(new BlockPos(1, 2, 3), new BlockPos(4, 6, 8));
        assertEquals(new BlockPos(5, 7, 9), m.block(new BlockPos(2, 3, 4)));
        assertEquals(33F, m.yaw(33F), 1e-4);
        assertEquals(Direction.NORTH, m.direction(Direction.NORTH));
        assertEquals(0, m.quarterTurns());
    }

    @Test
    void lyingOnItsSideIsNotUpright() {
        // a quarter turn about the x axis (a bearing on the side of a wall)
        int[] tilt = {1, 0, 0, 0, 0, -1, 0, 1, 0};
        assertTrue(TankMoveMap.valid(tilt));
        TankMoveMap m = new TankMoveMap(BlockPos.ZERO, BlockPos.ZERO, tilt);
        assertFalse(m.upright());
        assertEquals(Direction.SOUTH, m.direction(Direction.UP));
        assertFalse(TankMoveMap.valid(new int[]{2, 0, 0, 0, 1, 0, 0, 0, 1}));
        assertFalse(TankMoveMap.valid(new int[]{1, 0, 0}));
    }
}
