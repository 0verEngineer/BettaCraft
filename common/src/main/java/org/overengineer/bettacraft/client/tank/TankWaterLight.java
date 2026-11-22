package org.overengineer.bettacraft.client.tank;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;

/**
 * M63: smooth light for the tank water. The water used to get the light of its own block (flat per block); in a room lit
 * unevenly every block of the water had another brightness and the surface showed block-sized rectangles, plainly with
 * shader packs (which draw the water by the light value), faintly without. Now every corner of the block has a light: the
 * brightest block and sky light of the eight blocks around that corner, and every vertex of a water face takes the light
 * of its corner. Neighbouring blocks share their corners, so the light runs on without a step from block to block.
 * <p>
 * The maximum (not the mean) because chunk renderers merge the light of a model quad with the light of its block by taking
 * the brighter one (NeoForge, Sodium, Fabric's renderers): a corner light at least as bright as the own block is never
 * overruled, so the water drawn by the model (shader packs) gets exactly these values. The eight corner lights fit one
 * long: corner {@code x | y << 1 | z << 2}, one byte each (block light in the high, sky light in the low four bits).
 */
public final class TankWaterLight {
    private TankWaterLight() {}

    /** The eight corner lights of the block at {@code pos}. */
    public static long corners(BlockAndTintGetter level, BlockPos pos) {
        // the packed lights (LightTexture.pack layout) of the 27 blocks around pos, index (dx+1) + 3 (dy+1) + 9 (dz+1)
        int[] around = new int[27];
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dz = -1; dz <= 1; dz++) for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
            m.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
            around[(dx + 1) + 3 * (dy + 1) + 9 * (dz + 1)] = LevelRenderer.getLightCoords(level, m);
        }
        return corners(around);
    }

    /** The eight corner lights from the packed lights of the 27 blocks around a block, index {@code (dx+1) + 3 (dy+1) + 9 (dz+1)}. */
    public static long corners(int[] around) {
        long out = 0;
        for (int c = 0; c < 8; c++) {
            int cx = c & 1, cy = (c >> 1) & 1, cz = (c >> 2) & 1;
            int block = 0, sky = 0;
            // the blocks touching corner (cx, cy, cz): offsets cx-1 .. cx on every axis
            for (int dz = cz - 1; dz <= cz; dz++) for (int dy = cy - 1; dy <= cy; dy++) for (int dx = cx - 1; dx <= cx; dx++) {
                int l = around[(dx + 1) + 3 * (dy + 1) + 9 * (dz + 1)];
                block = Math.max(block, (l >> 4) & 15);
                sky = Math.max(sky, (l >> 20) & 15);
            }
            out |= (long) (block << 4 | sky) << (c * 8);
        }
        return out;
    }

    /**
     * The packed light (LightTexture.pack layout) of the corner of a water vertex (block coordinates 0..1): the nearer corner
     * sideways; the upper corners for everything above the bottom of the block (the surface of a part-filled block too, so it
     * takes the light from above, a lamp on the lid), the lower ones at the bottom (0 or the inset of the bottom face).
     */
    public static int at(long corners, float x, float y, float z) {
        int c = (x >= 0.5F ? 1 : 0) | (y >= 0.05F ? 2 : 0) | (z >= 0.5F ? 4 : 0);
        int b = (int) (corners >>> (c * 8)) & 0xFF;
        return ((b >> 4) & 15) << 4 | (b & 15) << 20;
    }

    /** The same packed light at every corner (tests, a block without neighbours). */
    public static long uniform(int packed) {
        int b = ((packed >> 4) & 15) << 4 | ((packed >> 20) & 15);
        long out = 0;
        for (int c = 0; c < 8; c++) out |= (long) b << (c * 8);
        return out;
    }
}
