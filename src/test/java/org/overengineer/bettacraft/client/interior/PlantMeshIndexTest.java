package org.overengineer.bettacraft.client.interior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

/** M21 follow-up: event-driven invalidation of the plant meshes (no periodic rebuild). */
class PlantMeshIndexTest {
    private static final long A = BlockPos.asLong(10, 64, 10), B = BlockPos.asLong(40, 64, 10);

    private static PlantMeshIndex twoPlants() {
        PlantMeshIndex i = new PlantMeshIndex();
        i.put(A, 9, 64, 9, 11, 70, 11);   // a stem plant, 3x7x3
        i.put(B, 40, 64, 10, 40, 75, 10); // a ribbon, one column
        return i;
    }

    @Test
    void blockChangeMarksOnlyThePlantItTouches() {
        PlantMeshIndex i = twoPlants();
        assertEquals(1, i.blockChanged(10, 68, 10, 5)); // a block of A cut off
        assertTrue(i.needsRebuild(A));
        assertFalse(i.needsRebuild(B));
        assertEquals(5, i.dirtySince(A));
        // a second change keeps the frame of the first one (the delay is measured from it)
        i.blockChanged(10, 69, 10, 7);
        assertEquals(5, i.dirtySince(A));
        // building the mesh again clears the flag
        i.put(A, 9, 64, 9, 11, 67, 11);
        assertFalse(i.needsRebuild(A));
        assertEquals(-1, i.dirtySince(A));
        // nowhere near a plant: nothing
        assertEquals(0, i.blockChanged(100, 64, 100, 9));
        assertEquals(0, i.dirtyCount());
    }

    @Test
    void growthNextToThePlantCounts() {
        PlantMeshIndex i = twoPlants();
        // one block above the top and one beside it (a new block of the plant grows there)
        assertEquals(1, i.blockChanged(40, 76, 10, 1));
        i.put(B, 40, 64, 10, 40, 76, 10);
        assertEquals(1, i.blockChanged(41, 70, 11, 2));
        // two blocks away: another plant's business
        i.put(B, 40, 64, 10, 40, 76, 10);
        assertEquals(0, i.blockChanged(42, 70, 10, 3));
    }

    @Test
    void lightOnlyRelightsAndChunkLoadsRebuild() {
        PlantMeshIndex i = twoPlants();
        // section (0, 4, 0) holds A (blocks 0..15 / 64..79): relight A only, no rebuild
        assertEquals(1, i.sectionChanged(0, 4, 0, false, 1));
        assertTrue(i.takeRelight(A));
        assertFalse(i.takeRelight(A), "taken once");
        assertFalse(i.takeRelight(B));
        assertEquals(0, i.dirtyCount());
        // two sections above A (96..111): far from both boxes
        assertEquals(0, i.sectionChanged(0, 6, 0, false, 1));
        // B sits at x = 40 in section x 2 (32..47); a reload of chunk (2, 0) rebuilds B
        assertEquals(1, i.chunkChanged(2, 0, true, 4));
        assertTrue(i.needsRebuild(B));
        assertFalse(i.needsRebuild(A));
    }

    @Test
    void removeAndChunkRoots() {
        PlantMeshIndex i = twoPlants();
        assertEquals(1, i.rootsInChunk(0, 0, BlockPos::getX, BlockPos::getZ).size());
        assertEquals(B, i.rootsInChunk(2, 0, BlockPos::getX, BlockPos::getZ).getLong(0));
        i.blockChanged(10, 64, 10, 1);
        i.remove(A);
        assertFalse(i.contains(A));
        assertFalse(i.needsRebuild(A));
        assertEquals(0, i.blockChanged(10, 64, 10, 2), "a removed plant is not marked again");
        assertEquals(1, i.size());
        i.clear();
        assertEquals(0, i.size());
    }

    @Test
    void sectionBorderReachesTheNeighbour() {
        PlantMeshIndex i = new PlantMeshIndex();
        // a plant ending at y 79 (top of section 4): a light change in section 5 (80..) still reaches it (the light of
        // the block above the top leaf)
        i.put(A, 10, 70, 10, 10, 79, 10);
        assertEquals(1, i.sectionChanged(0, 5, 0, false, 1));
    }
}
