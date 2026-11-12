package org.overengineer.bettacraft.client.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/**
 * M11 regression test for the water seam: the water of a tank is drawn block by block, and across the blocks of one tank
 * (cuboid and free shapes, full and partial fill) it must be one seamless body - no faces between blocks, no overlapping
 * boxes (two translucent layers = darker), no gaps (lighter), one surface height per layer.
 */
class TankWaterGeometryTest {
    private static final float E = 1e-6F;

    /** Builds the bodies of every block of a shape with a fill per layer (index = y), like the block states do. */
    private static Map<BlockPos, TankWaterGeometry.Body> bodies(Set<BlockPos> shape, int[] fillPerLayer) {
        Map<BlockPos, TankWaterGeometry.Body> out = new HashMap<>();
        for (BlockPos p : shape) {
            int mask = 0;
            for (Direction d : Direction.values()) if (shape.contains(p.relative(d))) mask |= 1 << d.ordinal();
            int fill = fillPerLayer[p.getY()];
            BlockPos up = p.above();
            boolean waterAbove = shape.contains(up) && up.getY() < fillPerLayer.length && fillPerLayer[up.getY()] > 0;
            TankWaterGeometry.Body b = TankWaterGeometry.body(mask, fill, waterAbove);
            if (b != null) out.put(p, b);
        }
        return out;
    }

    private static void assertSeamless(Set<BlockPos> shape, int[] fill) {
        Map<BlockPos, TankWaterGeometry.Body> bodies = bodies(shape, fill);
        for (var e : bodies.entrySet()) {
            BlockPos p = e.getKey();
            TankWaterGeometry.Body b = e.getValue();
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                boolean member = shape.contains(n);
                if (d == Direction.UP) {
                    // a top face only where no water goes on above
                    assertEquals(!(member && bodies.containsKey(n)), b.has(d), "top face of " + p);
                    continue;
                }
                assertEquals(!member, b.has(d), "face " + d + " of " + p + (member ? " towards a block of the same tank" : " outside"));
                if (member) {
                    // reaches the border towards the neighbour exactly
                    float border = switch (d) {
                        case WEST -> b.x0();
                        case EAST -> b.x1() - 1;
                        case NORTH -> b.z0();
                        case SOUTH -> b.z1() - 1;
                        case DOWN -> b.y0();
                        default -> 0;
                    };
                    assertEquals(0F, border, E, "water of " + p + " reaches the border towards " + d);
                }
            }
        }
        // no two bodies overlap (world space)
        BlockPos[] keys = bodies.keySet().toArray(new BlockPos[0]);
        for (int i = 0; i < keys.length; i++) {
            for (int j = i + 1; j < keys.length; j++) {
                assertTrue(overlap(keys[i], bodies.get(keys[i]), keys[j], bodies.get(keys[j])) <= E, "overlap " + keys[i] + " / " + keys[j]);
            }
        }
        // one surface height per layer (where the layer has its top face)
        Map<Integer, Float> surface = new HashMap<>();
        for (var e : bodies.entrySet()) {
            if (!e.getValue().has(Direction.UP)) continue;
            Float prev = surface.put(e.getKey().getY(), e.getValue().y1());
            if (prev != null) assertEquals(prev, e.getValue().y1(), E, "surface of layer " + e.getKey().getY());
        }
    }

    private static float overlap(BlockPos a, TankWaterGeometry.Body ba, BlockPos b, TankWaterGeometry.Body bb) {
        float x = Math.min(a.getX() + ba.x1(), b.getX() + bb.x1()) - Math.max(a.getX() + ba.x0(), b.getX() + bb.x0());
        float y = Math.min(a.getY() + ba.y1(), b.getY() + bb.y1()) - Math.max(a.getY() + ba.y0(), b.getY() + bb.y0());
        float z = Math.min(a.getZ() + ba.z1(), b.getZ() + bb.z1()) - Math.max(a.getZ() + ba.z0(), b.getZ() + bb.z0());
        return x <= 0 || y <= 0 || z <= 0 ? 0 : x * y * z;
    }

    private static Set<BlockPos> box(int sx, int sy, int sz) {
        Set<BlockPos> s = new HashSet<>();
        for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) s.add(new BlockPos(x, y, z));
        return s;
    }

    @Test
    void cuboidIsSeamlessFullAndPartial() {
        assertSeamless(box(3, 2, 3), new int[]{16, 16});
        assertSeamless(box(3, 2, 3), new int[]{16, 9});
        assertSeamless(box(3, 2, 3), new int[]{7, 0});
        assertSeamless(box(4, 1, 1), new int[]{16});
        assertSeamless(box(1, 3, 1), new int[]{16, 16, 4});
    }

    /** The T of the M10/M11 scenes: a bar of 5 with a stem of 2, both ends of the bar raised by one block (a step). */
    @Test
    void freeShapesAreSeamless() {
        Set<BlockPos> t = new HashSet<>();
        for (int x = 0; x < 5; x++) t.add(new BlockPos(x, 0, 2));
        t.add(new BlockPos(2, 0, 0));
        t.add(new BlockPos(2, 0, 1));
        t.add(new BlockPos(0, 1, 2));
        t.add(new BlockPos(4, 1, 2));
        assertSeamless(t, new int[]{16, 16});
        assertSeamless(t, new int[]{16, 8});
        assertSeamless(t, new int[]{12, 0});
        // an L of two layers
        Set<BlockPos> l = new HashSet<>();
        for (int y = 0; y < 2; y++) for (int z = 0; z < 4; z++) for (int x = 0; x < 4; x++) if (!(x >= 2 && z <= 1)) l.add(new BlockPos(x, y, z));
        assertSeamless(l, new int[]{16, 11});
    }

    /**
     * The root cause of the M10 seam behind stained glass: translucent quads are sorted by their centre and write depth,
     * so a water face must always sort behind the glass face (piece) in front of it - its centre must be exactly the
     * glass centre moved inwards along the normal. Checked for every face of every block, full and partial fill.
     */
    @Test
    void everyWaterFaceSitsRightBehindItsGlass() {
        float[] c = new float[12];
        for (int mask = 0; mask < 64; mask++) {
            for (int fill = 1; fill <= 16; fill++) {
                for (boolean above : new boolean[]{false, true}) {
                    if (above && (fill < 16 || (mask & (1 << Direction.UP.ordinal())) == 0)) continue;
                    TankWaterGeometry.Body b = TankWaterGeometry.body(mask, fill, above);
                    float split = TankWaterGeometry.glassSplit(fill, above);
                    for (Direction d : Direction.values()) {
                        if (!b.has(d)) continue;
                        TankWaterGeometry.face(b, d, c);
                        float wx = (c[0] + c[6]) / 2, wy = (c[1] + c[7]) / 2, wz = (c[2] + c[8]) / 2;
                        if (d == Direction.UP) {
                            assertEquals(0.5F, wx, E, "top centre x");
                            assertEquals(0.5F, wz, E, "top centre z");
                            assertTrue(wy < 1, "top below the glass");
                            continue;
                        }
                        // centre of the glass face piece in front of the water (side faces: below the split)
                        float gy = d.getAxis().isVertical() ? (d == Direction.DOWN ? 0 : 1) : (split > 0 ? split / 2 : 0.5F);
                        float gx = 0.5F + 0.5F * d.getStepX(), gz = 0.5F + 0.5F * d.getStepZ();
                        if (d.getAxis().isVertical()) {
                            gx = 0.5F;
                            gz = 0.5F;
                        }
                        String where = d + " mask " + mask + " fill " + fill + " above " + above;
                        assertEquals(gx - TankWaterGeometry.INSET * d.getStepX(), wx, E, "x of " + where);
                        assertEquals(gy - TankWaterGeometry.INSET * d.getStepY(), wy, E, "y of " + where);
                        assertEquals(gz - TankWaterGeometry.INSET * d.getStepZ(), wz, E, "z of " + where);
                    }
                }
            }
        }
    }

    @Test
    void innerBlockHasNoFacesAndFillsItsCell() {
        TankWaterGeometry.Body b = TankWaterGeometry.body(0b111111, 16, true);
        assertNotNull(b);
        assertEquals(0, b.faces(), "no face at all inside the water body");
        assertEquals(1F, (b.x1() - b.x0()) * (b.y1() - b.y0()) * (b.z1() - b.z0()), E);
        // a single block: all faces, 0.5 px inside the glass
        TankWaterGeometry.Body s = TankWaterGeometry.body(0, 16, false);
        assertEquals(63, s.faces());
        assertEquals(TankWaterGeometry.INSET, s.x0(), E);
        assertEquals(1 - TankWaterGeometry.INSET, s.y1(), E, "a full block keeps the surface just below the rim");
        assertNull(TankWaterGeometry.body(0, 0, false), "dry block");
    }
}
