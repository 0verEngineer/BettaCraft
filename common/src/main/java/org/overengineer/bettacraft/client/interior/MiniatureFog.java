package org.overengineer.bettacraft.client.interior;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector3d;
import org.joml.Vector4f;

/**
 * M67.5: the miniature's fog. Its meshes are in region units and the terrain shader takes the fog distance from
 * {@code Position + (ChunkPosition - CameraBlockPos) + CameraOffset} before the model-view matrix. Up to 1.56.0 that was the
 * position from the region's corner in region blocks (the translation and the scale were in the model-view matrix): the far
 * half of a big tank (192 region blocks at scale 8) lay in the sky fog (a white-blue haze over the miniature) and, under
 * water, in the water fog (all blue inside a big tank). Now the camera vector in region units goes into the chunk position
 * (whole blocks, {@link #chunk}) and the model-view matrix (the rest), and the fog distances of the frame are divided by the
 * scale in a Fog block of our own ({@link #scaled}: a ring buffer per scale, written once a frame).
 */
public final class MiniatureFog {
    private static final Vector4f COLOR = new Vector4f();
    private static final float[] DIST = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
    private static final Map<Integer, MappableRingBuffer> RINGS = new HashMap<>();
    private static final Map<Integer, Integer> WRITTEN = new HashMap<>();

    private MiniatureFog() {}

    /** The fog of the frame as the game writes it ({@code FogCaptureMixin}). */
    public static void capture(Vector4f color, float envStart, float envEnd, float renderStart, float renderEnd, float skyEnd, float cloudsEnd) {
        COLOR.set(color);
        DIST[0] = envStart;
        DIST[1] = envEnd;
        DIST[2] = renderStart;
        DIST[3] = renderEnd;
        DIST[4] = skyEnd;
        DIST[5] = cloudsEnd;
    }

    /** A Fog block with this frame's fog, its distances divided by {@code scale} (blocks per region block). Before a render pass. */
    public static GpuBufferSlice scaled(float scale) {
        int key = Float.floatToIntBits(scale);
        MappableRingBuffer ring = RINGS.computeIfAbsent(key, k -> new MappableRingBuffer(() -> "Bettacraft miniature fog", 130, FogRenderer.FOG_UBO_SIZE));
        int frame = org.overengineer.bettacraft.client.render.TravelWater.frame();
        Integer last = WRITTEN.get(key);
        if (last == null || last != frame) {
            WRITTEN.put(key, frame);
            ring.rotate();
            try (var view = RenderSystem.getDevice().createCommandEncoder().mapBuffer(ring.currentBuffer(), false, true)) {
                Std140Builder.intoBuffer(view.data()).putVec4(COLOR).putFloat(div(DIST[0], scale)).putFloat(div(DIST[1], scale))
                        .putFloat(div(DIST[2], scale)).putFloat(div(DIST[3], scale)).putFloat(div(DIST[4], scale)).putFloat(div(DIST[5], scale));
            }
        }
        return ring.currentBuffer().slice(0L, FogRenderer.FOG_UBO_SIZE);
    }

    private static float div(float d, float scale) {
        return Math.abs(d) >= 1e30F ? d : Math.max(-1e30F, Math.min(1e30F, d / scale));
    }

    /**
     * The whole-block chunk position for a draw whose camera vector in region units is {@code offset} (the shader then works
     * with {@code Position + chunk - camera}), and the rest it leaves, which the model-view matrix takes back out.
     */
    public static int[] chunk(net.minecraft.world.phys.Vec3 cam, Vector3d offset, Vector3d rest) {
        int x = (int) Math.floor(cam.x + offset.x + 0.5), y = (int) Math.floor(cam.y + offset.y + 0.5), z = (int) Math.floor(cam.z + offset.z + 0.5);
        rest.set(x - cam.x - offset.x, y - cam.y - offset.y, z - cam.z - offset.z);
        return new int[]{x, y, z};
    }
}
