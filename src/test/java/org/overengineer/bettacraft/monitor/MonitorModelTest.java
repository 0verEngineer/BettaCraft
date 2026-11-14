package org.overengineer.bettacraft.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

/** M27: cutting monitor walls into screens, the dashboard layout and the tank history (no game needed). */
class MonitorModelTest {
    /** A wall of {@code w} x {@code h} monitors facing north, top left at (0, h - 1, 0); north-facing: right = west (-x). */
    private static Set<BlockPos> wall(int w, int h) {
        Set<BlockPos> s = new HashSet<>();
        for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) s.add(new BlockPos(-x, y, 0));
        return s;
    }

    @Test
    void wallsUpTo8x5AreOneScreen() {
        for (int[] size : new int[][]{{1, 1}, {3, 2}, {8, 5}, {1, 5}, {8, 1}}) {
            List<MonitorScreens.Rect> r = MonitorScreens.cut(wall(size[0], size[1]), Direction.NORTH);
            assertEquals(1, r.size(), size[0] + "x" + size[1]);
            assertEquals(size[0], r.get(0).w());
            assertEquals(size[1], r.get(0).h());
            assertEquals(new BlockPos(0, size[1] - 1, 0), r.get(0).origin(), "top left as the viewer sees it");
        }
    }

    @Test
    void biggerWallsAreCut() {
        List<MonitorScreens.Rect> r = MonitorScreens.cut(wall(9, 6), Direction.NORTH);
        int blocks = 0;
        for (MonitorScreens.Rect x : r) {
            assertTrue(x.w() <= MonitorScreens.MAX_W && x.h() <= MonitorScreens.MAX_H);
            blocks += x.w() * x.h();
        }
        assertEquals(54, blocks);
        assertEquals(8, r.get(0).w());
        assertEquals(5, r.get(0).h());
    }

    @Test
    void anExtraBlockKeepsTheBigScreen() {
        // a 3x2 screen with one block more to the right of the top row: the 3x2 stays, the extra block is its own screen
        Set<BlockPos> s = wall(3, 2);
        s.add(new BlockPos(-3, 1, 0));
        List<MonitorScreens.Rect> r = MonitorScreens.cut(s, Direction.NORTH);
        assertEquals(2, r.size());
        assertEquals(3, r.get(0).w());
        assertEquals(2, r.get(0).h());
    }

    @Test
    void cuttingIsTheSameForEveryFacing() {
        for (Direction f : Direction.Plane.HORIZONTAL) {
            Direction right = MonitorScreens.right(f);
            Set<BlockPos> s = new HashSet<>();
            for (int c = 0; c < 4; c++) for (int y = 0; y < 3; y++) s.add(BlockPos.ZERO.relative(right, c).above(y));
            List<MonitorScreens.Rect> r = MonitorScreens.cut(s, f);
            assertEquals(1, r.size(), f.toString());
            assertEquals(BlockPos.ZERO.above(2), r.get(0).origin(), f.toString());
        }
    }

    @Test
    void layoutRoundTripAndClip() {
        UUID a = new UUID(1, 2), b = new UUID(3, 4);
        List<MonitorTank> tanks = List.of(new MonitorTank(b, "Zebra", BlockPos.ZERO, true, new float[0], 0),
                new MonitorTank(a, "Alpha", BlockPos.ZERO, true, new float[0], 0));
        DashboardLayout l = DashboardLayout.autoLayout(tanks, 2, 1);
        assertEquals(4, l.tiles.size(), "two tiles per tank on 2x1");
        assertEquals(a, l.tiles.get(0).tank, "sorted by name");
        assertEquals(MonitorValue.TEMPERATURE, l.tiles.get(0).value());
        assertTrue(l.tiles.get(0).spark());
        DashboardLayout back = DashboardLayout.load(l.save());
        assertTrue(back.sameAs(l));
        assertEquals(1, back.historyNeeds().get(a).size());
        back.clip(4, 4);
        assertEquals(2, back.tiles.size(), "only the tiles of the left block stay");
        back.tiles.get(1).id = back.tiles.get(0).id;
        back.sanitize(4, 4);
        assertTrue(back.tiles.get(0).id != back.tiles.get(1).id, "unique ids");
        assertTrue(DashboardLayout.load(new CompoundTag()).auto);
    }

    @Test
    void historyKeepsSevenDaysWithGaps() {
        TankHistory h = new TankHistory();
        UUID id = new UUID(5, 6);
        float[] v = new float[MonitorValue.ALL.length];
        for (int i = 0; i < 200; i++) {
            v[MonitorValue.TEMPERATURE.ordinal()] = i;
            h.record(id, i, v);
        }
        TankHistory.Series s = h.series(id);
        assertEquals(TankHistory.HOURS, s.size());
        float[] t = s.values(MonitorValue.TEMPERATURE, 24);
        assertEquals(199F, t[23]);
        assertEquals(176F, t[0]);
        h.record(id, 203, v); // three hours unloaded
        float[] g = s.values(MonitorValue.TEMPERATURE, 4);
        assertTrue(Float.isNaN(g[0]) && Float.isNaN(g[2]));
        assertFalse(Float.isNaN(g[3]));
        h.record(id, 100, v); // older: ignored
        assertEquals(203, s.lastHour());
        TankHistory back = TankHistory.load(h.save(new CompoundTag(), null), null);
        assertEquals(TankHistory.HOURS, back.series(id).size());
        assertEquals(203, back.series(id).lastHour());
        assertEquals(199F, back.series(id).values(MonitorValue.TEMPERATURE, 4)[3]);
    }
}
