package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * M52b: a buffer source that records what our 1.21.1 renderers draw (already transformed by their pose stack) and hands
 * it to Minecraft's 1.21.9 submit queue ({@link SubmitNodeCollector#submitCustomGeometry}) afterwards, render type by render
 * type. The renderers of the mod keep their vertex code; Minecraft draws the geometry in its batched feature passes.
 */
public final class RecordingBuffers implements MultiBufferSource {
    private final Map<RenderType, Recorder> recorders = new LinkedHashMap<>();

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        return recorders.computeIfAbsent(type, t -> new Recorder());
    }

    public boolean isEmpty() {
        for (Recorder r : recorders.values()) if (r.vertices > 0) return false;
        return true;
    }

    /** Queues everything recorded (at the identity pose: the vertices are transformed already) and forgets it. */
    public void submitTo(SubmitNodeCollector collector) {
        PoseStack identity = new PoseStack();
        for (Map.Entry<RenderType, Recorder> e : recorders.entrySet()) {
            Recorder r = e.getValue();
            if (r.vertices == 0) continue;
            collector.submitCustomGeometry(identity, e.getKey(), (pose, vc) -> r.replay(vc));
        }
        recorders.clear();
    }

    /** One render type's vertices: position, colour, UVs, light and normal of each vertex (attributes it never set stay 0). */
    private static final class Recorder implements VertexConsumer {
        private final FloatArrayList floats = new FloatArrayList(); // x y z u v nx ny nz
        private final IntArrayList ints = new IntArrayList(); // colour, uv1 (overlay), uv2 (light)
        private final IntArrayList lineWidths = new IntArrayList();
        int vertices;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            vertices++;
            floats.add(x);
            floats.add(y);
            floats.add(z);
            for (int i = 0; i < 5; i++) floats.add(0F);
            ints.add(-1);
            ints.add(0);
            ints.add(0);
            lineWidths.add(Float.floatToRawIntBits(1F));
            return this;
        }

        private int f() {
            return (vertices - 1) * 8;
        }

        private int i() {
            return (vertices - 1) * 3;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            ints.set(i(), (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255));
            return this;
        }

        @Override
        public VertexConsumer setColor(int argb) {
            ints.set(i(), argb);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            floats.set(f() + 3, u);
            floats.set(f() + 4, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            ints.set(i() + 1, (v & 0xFFFF) << 16 | (u & 0xFFFF));
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            ints.set(i() + 2, (v & 0xFFFF) << 16 | (u & 0xFFFF));
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            floats.set(f() + 5, x);
            floats.set(f() + 6, y);
            floats.set(f() + 7, z);
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            lineWidths.set(vertices - 1, Float.floatToRawIntBits(width));
            return this;
        }

        void replay(VertexConsumer vc) {
            for (int k = 0; k < vertices; k++) {
                int f = k * 8, i = k * 3;
                vc.addVertex(floats.getFloat(f), floats.getFloat(f + 1), floats.getFloat(f + 2))
                        .setColor(ints.getInt(i))
                        .setUv(floats.getFloat(f + 3), floats.getFloat(f + 4))
                        .setOverlay(ints.getInt(i + 1))
                        .setLight(ints.getInt(i + 2))
                        .setNormal(floats.getFloat(f + 5), floats.getFloat(f + 6), floats.getFloat(f + 7))
                        .setLineWidth(Float.intBitsToFloat(lineWidths.getInt(k)));
            }
        }
    }
}
