package org.overengineer.bettacraft.tank;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/** Shape algorithms of free tank shapes (M10) without Minecraft bootstrap. */
class TankShapeTest {
    private static LongSet set(int[]... ps) {
        LongSet s = new LongOpenHashSet();
        for (int[] p : ps) s.add(BlockPos.asLong(p[0], p[1], p[2]));
        return s;
    }

    private static LongSet box(int x, int y, int z, int sx, int sy, int sz) {
        LongSet s = new LongOpenHashSet();
        for (int a = 0; a < sx; a++) for (int b = 0; b < sy; b++) for (int c = 0; c < sz; c++) s.add(BlockPos.asLong(x + a, y + b, z + c));
        return s;
    }

    /** L in the x/z plane: (0,0) (1,0) (0,1); the notch is (1,1). */
    private static LongSet ell() {
        return set(new int[]{0, 0, 0}, new int[]{1, 0, 0}, new int[]{0, 0, 1});
    }

    @Test
    void boundsAndCuboid() {
        assertArrayEquals(new int[]{0, 0, 0, 1, 0, 1}, TankShape.bounds(ell()));
        assertFalse(TankShape.isCuboid(ell()));
        assertTrue(TankShape.isCuboid(box(3, 4, 5, 2, 3, 4)));
        assertTrue(TankShape.fits(TankShape.bounds(box(0, 0, 0, 16, 16, 16)), 16, 16, 16));
        assertFalse(TankShape.fits(TankShape.bounds(box(0, 0, 0, 17, 1, 1)), 16, 16, 16));
    }

    @Test
    void componentsSplitAtTheGap() {
        // a U: removing the bottom middle block leaves two arms
        LongSet u = box(0, 0, 0, 3, 1, 1);
        u.addAll(set(new int[]{0, 0, 1}, new int[]{2, 0, 1}));
        assertTrue(TankShape.isConnected(u));
        u.remove(BlockPos.asLong(1, 0, 0));
        List<LongSet> parts = TankShape.components(u);
        assertEquals(2, parts.size());
        assertTrue(parts.get(0).contains(BlockPos.asLong(0, 0, 0)), "deterministic order: lowest block first");
        assertEquals(2, parts.get(0).size());
        // diagonal contact does not connect
        assertEquals(2, TankShape.components(set(new int[]{0, 0, 0}, new int[]{1, 0, 1})).size());
    }

    @Test
    void stepFillsLayerByLayer() {
        // step: two blocks at y 0, one of them continues to y 1
        LongSet step = set(new int[]{0, 0, 0}, new int[]{1, 0, 0}, new int[]{1, 1, 0});
        int[] counts = TankShape.layerCounts(step, 0, 1);
        assertArrayEquals(new int[]{2, 1}, counts);
        assertArrayEquals(new long[]{2000, 500}, TankShape.layerWater(counts, 2500));
        assertEquals(1.5, TankShape.surfaceY(counts, 0, 2500), 1e-9);
        assertEquals(0.25, TankShape.surfaceY(counts, 0, 500), 1e-9);
        assertEquals(2.0, TankShape.surfaceY(counts, 0, 99999), 1e-9);
        // headroom 2: the top layer fills up to 14/16
        assertEquals(1.4375, TankShape.surfaceY(counts, 0, 2500, 2), 1e-9);
        assertEquals(0.25, TankShape.surfaceY(counts, 0, 500, 2), 1e-9, "lower layers unchanged");
        assertEquals(1.875, TankShape.surfaceY(counts, 0, 99999, 2), 1e-9);
        assertEquals(7, TankData.fillFor(2500, 2000, 1000, 14));
        assertEquals(14, TankData.fillFor(3000, 2000, 1000, 14));
        assertEquals(TankData.fillFor(2500, 2000, 1000), 8);
    }

    @Test
    void splitKeepsTheWaterOfEachPiece() {
        // 3 blocks in a row, water 1.5 layers deep? one layer only: 2400 mB in a 3-block layer -> 800 each
        LongSet row = box(0, 0, 0, 3, 1, 1);
        List<List<Long>> pieces = List.of(List.of(BlockPos.asLong(0, 0, 0)), List.of(BlockPos.asLong(2, 0, 0)));
        long[] shares = TankShape.splitWater(row, 2400, pieces);
        assertArrayEquals(new long[]{800, 800}, shares, "the middle block's water is gone");
        // L-shape with a raised arm: removing the low corner keeps the upper arm's water in the layer it was in
        LongSet tall = box(0, 0, 0, 2, 2, 1); // 2 wide, 2 high
        List<List<Long>> rest = new ArrayList<>();
        rest.add(List.of(BlockPos.asLong(1, 0, 0), BlockPos.asLong(1, 1, 0), BlockPos.asLong(0, 1, 0)));
        long[] s = TankShape.splitWater(tall, 3000, rest); // layer 0 full (2000), layer 1 half (1000)
        assertArrayEquals(new long[]{2000}, s, "1000 (layer 0 half) + 1000 (both layer-1 blocks)");
        // rounding never creates water
        long[] r = TankShape.splitWater(box(0, 0, 0, 3, 1, 1), 1000, List.of(List.of(BlockPos.asLong(0, 0, 0)), List.of(BlockPos.asLong(1, 0, 0))));
        assertTrue(r[0] + r[1] <= 667 && r[0] + r[1] >= 666, "333.3 + 333.3, got " + r[0] + "+" + r[1]);
    }

    @Test
    void openAndExposedTops() {
        LongSet step = set(new int[]{0, 0, 0}, new int[]{1, 0, 0}, new int[]{1, 1, 0});
        List<BlockPos> tops = TankShape.openTops(step);
        assertEquals(List.of(new BlockPos(0, 1, 0), new BlockPos(1, 2, 0)), tops);
        // surface in layer 1: both tops reach the water (the low one is full)
        assertEquals(2, TankShape.exposedTops(step, 1).size());
        // surface in layer 0: the tall column reaches it through its own blocks, the low block holds water itself
        assertEquals(2, TankShape.exposedTops(step, 0).size());
        // a bridge: a block at y 1 above air is never wet while the water stays in layer 0
        LongSet bridge = set(new int[]{0, 0, 0}, new int[]{0, 1, 0}, new int[]{1, 1, 0});
        assertEquals(1, TankShape.exposedTops(bridge, 0).size());
        assertEquals(TankShape.openTops(box(0, 0, 0, 3, 2, 3)).size(), TankShape.exposedTops(box(0, 0, 0, 3, 2, 3), 0).size(), "cuboid: whole top");
    }

    @Test
    void signFaceOnIrregularFront() {
        // cuboid: the whole front
        TankShape.SignFace f = TankShape.signFace(box(0, 0, 0, 4, 2, 3), Direction.SOUTH, true);
        assertNotNull(f);
        assertEquals(1, f.y());
        assertEquals(3.0, f.plane(), 1e-9);
        assertEquals(0, f.latMin());
        assertEquals(3, f.latMax());
        // L seen from the south: x 0 reaches z 1, x 1 only z 0 -> two runs of width 1, the front one wins
        TankShape.SignFace l = TankShape.signFace(ell(), Direction.SOUTH, true);
        assertEquals(2.0, l.plane(), 1e-9);
        assertEquals(0, l.latMin());
        assertEquals(0, l.latMax());
        // wide L: (0..3, z 0) plus (0, z 1): the 3 blocks of the back run are the widest flat part
        LongSet wide = box(0, 0, 0, 4, 1, 1);
        wide.add(BlockPos.asLong(0, 0, 1));
        TankShape.SignFace w = TankShape.signFace(wide, Direction.SOUTH, false);
        assertEquals(1.0, w.plane(), 1e-9);
        assertEquals(1, w.latMin());
        assertEquals(3, w.latMax());
        // north front uses the minimum plane
        TankShape.SignFace n = TankShape.signFace(wide, Direction.NORTH, true);
        assertEquals(0.0, n.plane(), 1e-9);
        assertEquals(4, n.width());
        // a T standing upright: the top row is the 1-wide stem
        LongSet t = box(0, 0, 0, 3, 1, 1);
        t.add(BlockPos.asLong(1, 1, 0));
        TankShape.SignFace top = TankShape.signFace(t, Direction.SOUTH, true);
        assertEquals(1, top.y());
        assertEquals(1, top.latMin());
        assertEquals(1.5, top.centre(), 1e-9);
    }

    @Test
    void bitSetRoundTrip() {
        LongSet s = ell();
        s.add(BlockPos.asLong(0, 1, 0));
        byte[] bits = TankShape.encode(s, 0, 0, 0, 2, 2, 2);
        assertEquals(s, TankShape.decode(bits, 0, 0, 0, 2, 2, 2));
        assertTrue(TankShape.bit(bits, 2, 2, 0, 1, 0));
        assertFalse(TankShape.bit(bits, 2, 2, 1, 0, 1));
        assertEquals(box(5, 6, 7, 3, 2, 2), TankShape.decode(TankShape.fullBox(3, 2, 2), 5, 6, 7, 3, 2, 2));
    }

    @Test
    void waterVolumeKeepsOutOfTheNotch() {
        LongSet l = ell();
        TankWaterVolume w = new TankWaterVolume(l, 0.8, new AABB(0, 0, 0, 2, 0.8, 2));
        // a point in the notch goes to the nearest wet block, off the notch walls
        Vec3 c = w.clamp(new Vec3(1.9, 0.5, 1.9), 0.25, 0.1, 0.3);
        assertTrue(l.contains(BlockPos.asLong((int) Math.floor(c.x), 0, (int) Math.floor(c.z))), "inside the shape: " + c);
        assertTrue(c.x <= 1.75 || c.z <= 0.75, "off the glass of the notch: " + c);
        assertTrue(w.contains(c));
        // inside the L nothing moves, except off the outer glass and below the surface
        Vec3 in = new Vec3(0.9, 0.3, 0.5);
        assertEquals(in, w.clamp(in, 0.25, 0.1, 0.3));
        assertEquals(0.5, w.clamp(new Vec3(0.5, 0.79, 0.5), 0.25, 0.1, 0.3).y, 1e-9);
        // the inner corner: no wall between (0,0) and its neighbours, so x may reach the middle line
        assertEquals(1.2, w.clamp(new Vec3(1.2, 0.3, 0.5), 0.25, 0.1, 0.3).x, 1e-9);
        // nest: in a block that holds the surface, below it
        Vec3 nest = w.surfacePoint(new Vec3(1.8, 0, 1.8), 0.3, 0.06);
        assertTrue(l.contains(BlockPos.asLong((int) Math.floor(nest.x), 0, (int) Math.floor(nest.z))), "nest in the shape: " + nest);
        assertEquals(0.74, nest.y, 1e-9);
    }
}
