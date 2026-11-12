package org.overengineer.bettacraft.client.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/** M11 connected glass: edge slots, hidden frames, UV remapping and the frame width read from a texture. */
class ConnectedGlassTest {
    private static final float E = 1e-5F;

    @Test
    void everyFaceHasFourDistinctSlots() {
        for (Direction side : Direction.values()) {
            Set<Integer> slots = new HashSet<>();
            for (Direction e : Direction.values()) if (e.getAxis() != side.getAxis()) slots.add(ConnectedGlass.slot(side, e));
            assertEquals(Set.of(0, 1, 2, 3), slots, "slots of " + side);
        }
        // north face: u = 1 - x, so east is the low u end; v = 1 - y, so up is the low v end
        assertEquals(0, ConnectedGlass.slot(Direction.NORTH, Direction.EAST));
        assertEquals(2, ConnectedGlass.slot(Direction.NORTH, Direction.UP));
    }

    @Test
    void hiddenOnlyWhereTheFaceContinues() {
        int mask = (1 << Direction.EAST.ordinal()) | (1 << Direction.WEST.ordinal()) | (1 << Direction.NORTH.ordinal());
        int southFace = ConnectedGlass.hiddenSlots(mask, 0, Direction.SOUTH);
        assertEquals((1 << ConnectedGlass.slot(Direction.SOUTH, Direction.EAST)) | (1 << ConnectedGlass.slot(Direction.SOUTH, Direction.WEST)), southFace);
        // a concave crease towards east keeps the frame there
        long crease = 1L << (Direction.SOUTH.ordinal() * 6 + Direction.EAST.ordinal());
        assertEquals(1 << ConnectedGlass.slot(Direction.SOUTH, Direction.WEST), ConnectedGlass.hiddenSlots(mask, crease, Direction.SOUTH));
        assertEquals(0, ConnectedGlass.hiddenSlots(0, 0, Direction.UP), "single block: every frame shows");
    }

    @Test
    void remapSkipsTheFrameOnHiddenEdgesOnly() {
        float f = 1F / 16F;
        // face-local corners and an atlas sprite at u 0.5..0.6, v 0.2..0.3 (u along s, v along t)
        float[] s = {0, 0, 1, 1}, t = {0, 1, 1, 0};
        float[] u = {0.5F, 0.5F, 0.6F, 0.6F}, v = {0.2F, 0.3F, 0.3F, 0.2F};
        float[] u2 = u.clone(), v2 = v.clone();
        assertTrue(ConnectedGlass.remapUv(s, t, u2, v2, 0, f));
        for (int i = 0; i < 4; i++) {
            assertEquals(u[i], u2[i], E);
            assertEquals(v[i], v2[i], E);
        }
        u2 = u.clone();
        v2 = v.clone();
        assertTrue(ConnectedGlass.remapUv(s, t, u2, v2, 0b0010, f)); // high u end hidden
        assertEquals(0.5F, u2[0], E, "low end keeps its frame");
        assertEquals(0.6F - 0.1F * f, u2[2], E, "high end stops before the frame");
        assertEquals(0.2F, v2[0], E);
        // rotated mapping: u along t, v along s (a glass model with a rotated face)
        float[] ur = {0.5F, 0.6F, 0.6F, 0.5F}, vr = {0.2F, 0.2F, 0.3F, 0.3F};
        assertTrue(ConnectedGlass.remapUv(s, t, ur, vr, 0b1111, f));
        assertEquals(0.5F + 0.1F * f, ur[0], E);
        assertEquals(0.6F - 0.1F * f, ur[1], E);
        assertEquals(0.2F + 0.1F * f, vr[0], E);
        assertEquals(0.3F - 0.1F * f, vr[2], E);
        // two neighbours: both sample frame-free texels at the shared edge
        assertEquals(1 - f, ConnectedGlass.remap(1, false, true, f), E);
        assertEquals(f, ConnectedGlass.remap(0, true, false, f), E);
    }

    private static int[] texture(int w, int frame, int frameArgb, int insideArgb) {
        int[] px = new int[w * w];
        for (int y = 0; y < w; y++) {
            for (int x = 0; x < w; x++) {
                int ring = Math.min(Math.min(x, y), Math.min(w - 1 - x, w - 1 - y));
                px[y * w + x] = ring < frame ? frameArgb : insideArgb;
            }
        }
        return px;
    }

    @Test
    void frameWidthIsReadFromTheTexture() {
        // vanilla glass: opaque light ring, transparent inside with a few streaks
        int[] glass = texture(16, 1, 0xFFD0D0D0, 0x00808080);
        glass[2 * 16 + 4] = 0xFFD0D0D0;
        glass[3 * 16 + 3] = 0xFFD0D0D0;
        glass[4 * 16 + 2] = 0xFFD0D0D0;
        assertEquals(1, ConnectedGlass.frameTexels(glass, 16, 16));
        // stained glass: the frame is only more opaque
        assertEquals(1, ConnectedGlass.frameTexels(texture(16, 1, 0xA3666699, 0x66666699), 16, 16));
        // a 32x resource pack with a 2 texel frame
        assertEquals(2, ConnectedGlass.frameTexels(texture(32, 2, 0xFF303030, 0x40A0C0E0), 32, 32));
        // clear modded glass without a frame
        assertEquals(0, ConnectedGlass.frameTexels(texture(16, 0, 0, 0x30FFFFFF), 16, 16));
    }
}
