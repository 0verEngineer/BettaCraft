package org.overengineer.bettacraft.client.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import org.overengineer.bettacraft.tank.TankData;

/** M66 v6: the packed film levels and the world-space film texture coordinates. */
class AlgaeFilmTest {
    @Test
    void levelsPerGlassFace() {
        // 3 wide (x), 1 high, 2 deep (z)
        TankData t = TankData.create(List.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0), new BlockPos(2, 0, 0),
                new BlockPos(0, 0, 1), new BlockPos(1, 0, 1), new BlockPos(2, 0, 1)), 16);
        int p = AlgaeFilm.pack(t, BlockPos.asLong(0, 0, 0), new float[]{1F, 1F, 1F, 0.5F});
        // south and east lead into the tank: no glass, no film
        assertEquals(0, AlgaeFilm.front(p, Direction.SOUTH));
        assertEquals(0, AlgaeFilm.front(p, Direction.EAST));
        // stage 4 = level 8 in front; the back film fades with the water in front of it (2 blocks north-south, 3 west-east)
        assertEquals(8, AlgaeFilm.front(p, Direction.NORTH));
        assertEquals(Math.round(8 * (float) Math.exp(-2 / 8.0)), AlgaeFilm.back(p, Direction.NORTH));
        assertEquals(8, AlgaeFilm.front(p, Direction.WEST));
        assertEquals(Math.round(8 * (float) Math.exp(-3 / 8.0)), AlgaeFilm.back(p, Direction.WEST));
        assertEquals(0, AlgaeFilm.front(p, Direction.UP));
        // stage 2 = level 4
        int q = AlgaeFilm.pack(t, BlockPos.asLong(0, 0, 0), new float[]{0F, 0.5F, 0F, 0F});
        assertEquals(4, AlgaeFilm.front(q, Direction.WEST));
    }

    @Test
    void textureRunsOnAcrossBlocks() {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            boolean alongX = d.getAxis() == Direction.Axis.Z;
            for (int b = -6; b < 6; b++) {
                BlockPos p0 = alongX ? new BlockPos(b, 0, 0) : new BlockPos(0, 0, b), p1 = alongX ? new BlockPos(b + 1, 0, 0) : new BlockPos(0, 0, b + 1);
                // the end of block b at its +x / +z border meets the start of block b + 1 (modulo the texture's wrap)
                float end = alongX ? AlgaeFilm.u(d, p0, 1, 0.5F, 0) : AlgaeFilm.u(d, p0, 0, 0.5F, 1);
                float start = alongX ? AlgaeFilm.u(d, p1, 0, 0.5F, 0) : AlgaeFilm.u(d, p1, 0, 0.5F, 0);
                assertEquals(0, Math.floorMod(Math.round((end - start) * 64), 64), d + " block " + b);
                assertEquals(1F / AlgaeFilm.TILE, Math.abs((alongX ? AlgaeFilm.u(d, p0, 1, 0, 0) - AlgaeFilm.u(d, p0, 0, 0, 0)
                        : AlgaeFilm.u(d, p0, 0, 0, 1) - AlgaeFilm.u(d, p0, 0, 0, 0))), 1e-6, "one block is a quarter of the texture");
            }
        }
        for (int y = -6; y < 6; y++) {
            float top = AlgaeFilm.v(new BlockPos(0, y, 0), 1), bottomAbove = AlgaeFilm.v(new BlockPos(0, y + 1, 0), 0);
            assertEquals(0, Math.floorMod(Math.round((top - bottomAbove) * 64), 64), "layer " + y);
        }
    }

    /** M67.5: every tank and wall places the texture differently, still without a seam between the blocks of a wall. */
    @Test
    void placementPerTankAndWall() {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int c = 0; c < 40; c++) {
            int seed = AlgaeFilm.seedOf(new BlockPos(c * 37, 64, -c * 11));
            org.junit.jupiter.api.Assertions.assertNotEquals(0, seed);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                boolean alongX = d.getAxis() == Direction.Axis.Z;
                for (int b = -3; b < 20; b++) {
                    BlockPos p0 = alongX ? new BlockPos(b, 0, 0) : new BlockPos(0, 0, b), p1 = alongX ? new BlockPos(b + 1, 0, 0) : new BlockPos(0, 0, b + 1);
                    float end = alongX ? AlgaeFilm.u(d, p0, seed, 1, 0.5F, 0) : AlgaeFilm.u(d, p0, seed, 0, 0.5F, 1);
                    float start = AlgaeFilm.u(d, p1, seed, 0, 0.5F, 0);
                    assertEquals(0, Math.floorMod(Math.round((end - start) * 256), 256), d + " block " + b);
                    float a0 = alongX ? AlgaeFilm.u(d, p0, seed, 0, 0, 0) : AlgaeFilm.u(d, p0, seed, 0, 0, 0);
                    float a1 = alongX ? AlgaeFilm.u(d, p0, seed, 1, 0, 0) : AlgaeFilm.u(d, p0, seed, 0, 0, 1);
                    assertEquals(1F / AlgaeFilm.TILE, Math.abs(a1 - a0), 1e-6, "one block, no wrap inside a face");
                }
                for (int y = -3; y < 20; y++) {
                    float top = AlgaeFilm.v(new BlockPos(0, y, 0), seed, d, 1), bottomAbove = AlgaeFilm.v(new BlockPos(0, y + 1, 0), seed, d, 0);
                    assertEquals(0, Math.floorMod(Math.round((top - bottomAbove) * 256), 256), "layer " + y);
                }
                seen.add(Math.round(AlgaeFilm.u(d, BlockPos.ZERO, seed, 0, 0, 0) * 256) + "/" + Math.round(AlgaeFilm.v(BlockPos.ZERO, seed, d, 0) * 256));
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(seen.size() > 100, "placements differ between tanks and walls: " + seen.size());
    }
}
