package org.overengineer.bettacraft.client.render;

import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * M52d: Minecraft 26.2 removed the immediate buffer source ({@code renderBuffers().bufferSource()}). The parts of the mod
 * that draw right away in a level render stage (miniature plant meshes, sway, jets, the decor preview, the plant meshes
 * outside the tanks) draw into this one: a vertex builder per render type, and at {@link #endBatch} the geometry goes into a
 * GPU buffer of the mod and is drawn with the render type's prepared state (pipeline, textures, current model-view), like
 * the 26.1 {@code BufferSource#endBatch} did. Translucent render types sort their quads (camera-relative vertices).
 * Render thread only.
 */
public final class ImmediateBuffers implements MultiBufferSource {
    private final String label;
    private final int initialBytes;
    private final Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();
    private final Map<RenderType, ByteBufferBuilder> memory = new HashMap<>();
    private final ByteBufferBuilder sortMemory = new ByteBufferBuilder(1 << 12);
    private GpuBuffer vertexBuffer;
    private GpuBuffer indexBuffer;

    public ImmediateBuffers(String label, int initialBytes) {
        this.label = label;
        this.initialBytes = initialBytes;
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        BufferBuilder bb = builders.get(type);
        if (bb == null) {
            ByteBufferBuilder mem = memory.computeIfAbsent(type, t -> new ByteBufferBuilder(initialBytes));
            bb = new BufferBuilder(mem, type.primitiveTopology(), type.format());
            builders.put(type, bb);
        }
        return bb;
    }

    /** Draws everything recorded so far, render type by render type in the order they were first used. */
    public void endBatch() {
        for (RenderType type : new ArrayList<>(builders.keySet())) endBatch(type);
    }

    /** Draws what was recorded for one render type. */
    public void endBatch(RenderType type) {
        BufferBuilder bb = builders.remove(type);
        if (bb == null) return;
        try (MeshData mesh = bb.build()) {
            if (mesh != null) draw(type, mesh);
        }
    }

    private void draw(RenderType type, MeshData mesh) {
        if (type.sortOnUpload()) mesh.sortQuads(sortMemory, RenderSystem.getProjectionType().vertexSorting());
        MeshData.DrawState state = mesh.drawState();
        GpuBuffer vb = vertexBuffer = upload(vertexBuffer, mesh.vertexBuffer(), GpuBuffer.USAGE_VERTEX, " vertices");
        GpuBuffer ib;
        IndexType indexType;
        ByteBuffer indices = mesh.indexBuffer();
        if (indices != null) {
            ib = indexBuffer = upload(indexBuffer, indices, GpuBuffer.USAGE_INDEX, " indices");
            indexType = state.indexType();
        } else {
            RenderSystem.AutoStorageIndexBuffer seq = RenderSystem.getSequentialBuffer(state.primitiveTopology());
            ib = seq.getBuffer(state.indexCount());
            indexType = seq.type();
        }
        // M52e (26.3): render types no longer open their own pass (no output targets): one pass on the main target
        var info = new net.minecraft.client.renderer.StagedVertexBuffer.ExecuteInfo(vb, ib, indexType, 0, 0, state.indexCount(),
                state.primitiveTopology());
        var target = net.minecraft.client.Minecraft.getInstance().gameRenderer.mainRenderTarget();
        try (com.mojang.renderpearl.api.commands.RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> label, target.getColorTextureView(), java.util.Optional.empty(), target.getDepthTextureView(),
                java.util.OptionalDouble.empty())) {
            type.prepare().drawFromBuffer(info, pass);
        }
    }

    private GpuBuffer upload(GpuBuffer buffer, ByteBuffer data, int usage, String suffix) {
        int size = data.remaining();
        if (buffer == null || buffer.size() < size) {
            long capacity = Math.max(size, buffer == null ? size : buffer.size() * 2);
            if (buffer != null) buffer.close();
            buffer = RenderSystem.getDevice().createBuffer(() -> label + suffix, usage | GpuBuffer.USAGE_COPY_DST, capacity);
        }
        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(0, size), data);
        return buffer;
    }

    /** Frees the native memory and the GPU buffers. */
    public void close() {
        builders.clear();
        for (ByteBufferBuilder m : memory.values()) m.close();
        memory.clear();
        sortMemory.close();
        if (vertexBuffer != null) vertexBuffer.close();
        if (indexBuffer != null) indexBuffer.close();
        vertexBuffer = indexBuffer = null;
    }

    /** A consumer that drops everything (26.1: {@code VertexMultiConsumer.create()} with no consumers). */
    public static final VertexConsumer NULL = new VertexConsumer() {
        @Override public VertexConsumer addVertex(float x, float y, float z) { return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setColor(int color) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setUv3(float u, float v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
        @Override public VertexConsumer setLineWidth(float width) { return this; }
    };

    /** Shared instance for the level-stage renderers of the mod. */
    private static ImmediateBuffers shared;

    public static ImmediateBuffers shared() {
        if (shared == null) shared = new ImmediateBuffers("Bettacraft immediate", 1 << 16);
        return shared;
    }
}
