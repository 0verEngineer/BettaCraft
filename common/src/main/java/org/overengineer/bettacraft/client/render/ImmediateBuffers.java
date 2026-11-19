package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

/**
 * M52d: Minecraft 26.2 removed the immediate buffer source ({@code renderBuffers().bufferSource()}). The parts of the mod
 * that draw in a level render stage (miniature plant meshes, sway, jets, the decor preview, the plant meshes outside the
 * tanks) draw into this one: a vertex builder per render type; at {@link #endBatch} the geometry is drawn with the render
 * type's prepared state (pipeline, textures, current model-view). Translucent render types sort their quads.
 *
 * <p>M52e (26.3): the level stages of the main pass run ahead of it ({@link LevelDraws}): a batch ended then is kept, all
 * batches of the frame go into one vertex (and index) buffer before the pass is opened, and the draws are ops of their
 * stage that are drawn into the pass. Outside that (a stage of its own, the Iris shadow pass) a batch is drawn at once.
 * Render thread only.
 */
public final class ImmediateBuffers implements MultiBufferSource {
    private static final Set<ImmediateBuffers> FRAME = new LinkedHashSet<>();

    private final String label;
    private final int initialBytes;
    private final Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();
    private final Map<RenderType, ByteBufferBuilder> memory = new HashMap<>();
    private final ByteBufferBuilder sortMemory = new ByteBufferBuilder(1 << 12);
    private final List<Batch> batches = new ArrayList<>();
    private GpuBuffer vertexBuffer;
    private GpuBuffer indexBuffer;

    /** A batch kept for the upload of the frame: its bytes, then where they went. */
    private static final class Batch {
        final PreparedRenderType prepared;
        final ByteBuffer vertices;
        final int vertexSize;
        final @Nullable ByteBuffer indices;
        final IndexType indexType;
        final int indexCount;
        final PrimitiveTopology topology;
        int baseVertex, firstIndex;
        GpuBuffer vb;
        @Nullable GpuBuffer ib;

        Batch(PreparedRenderType prepared, ByteBuffer vertices, int vertexSize, @Nullable ByteBuffer indices, IndexType indexType,
              int indexCount, PrimitiveTopology topology) {
            this.prepared = prepared;
            this.vertices = vertices;
            this.vertexSize = vertexSize;
            this.indices = indices;
            this.indexType = indexType;
            this.indexCount = indexCount;
            this.topology = topology;
        }
    }

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
        if (LevelDraws.recording()) {
            ByteBuffer indices = mesh.indexBuffer();
            Batch b = new Batch(type.prepare(), copy(mesh.vertexBuffer()), state.format().getVertexSize(), indices == null ? null : copy(indices),
                    state.indexType(), state.indexCount(), state.primitiveTopology());
            batches.add(b);
            FRAME.add(this);
            LevelDraws.add(pass -> b.prepared.drawFromBuffer(new StagedVertexBuffer.ExecuteInfo(b.vb, b.ib, b.ib != null ? b.indexType
                    : RenderSystem.getSequentialBuffer(b.topology).type(), b.baseVertex, b.firstIndex, b.indexCount, b.topology), pass));
            return;
        }
        if (LevelDraws.inPass()) return; // never upload into an open pass (LevelDraws logs it)
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
        var info = new StagedVertexBuffer.ExecuteInfo(vb, ib, indexType, 0, 0, state.indexCount(), state.primitiveTopology());
        PreparedRenderType prepared = type.prepare();
        LevelDraws.add(pass -> prepared.drawFromBuffer(info, pass));
    }

    private static ByteBuffer copy(ByteBuffer src) {
        ByteBuffer out = ByteBuffer.allocateDirect(src.remaining()).order(src.order());
        out.put(src.duplicate());
        return out.flip();
    }

    /** A new frame: nothing kept from the last one. */
    static void beginFrame() {
        for (ImmediateBuffers b : FRAME) b.batches.clear();
        FRAME.clear();
    }

    /** All batches of the frame into the GPU buffers, before the main pass is opened. */
    static void uploadFrame() {
        for (ImmediateBuffers b : FRAME) b.uploadBatches();
    }

    private void uploadBatches() {
        if (batches.isEmpty()) return;
        int vertexBytes = 0, indexBytes = 0;
        for (Batch b : batches) {
            vertexBytes = Mth.roundToward(vertexBytes, b.vertexSize);
            b.baseVertex = vertexBytes / b.vertexSize;
            vertexBytes += b.vertices.remaining();
            if (b.indices != null) {
                indexBytes = Mth.roundToward(indexBytes, b.indexType.bytes);
                b.firstIndex = indexBytes / b.indexType.bytes;
                indexBytes += b.indices.remaining();
            } else {
                RenderSystem.getSequentialBuffer(b.topology).getBuffer(b.indexCount); // sized now, outside the pass
            }
        }
        ByteBuffer all = MemoryUtil.memAlloc(vertexBytes);
        try {
            for (Batch b : batches) {
                all.position(b.baseVertex * b.vertexSize);
                all.put(b.vertices.duplicate());
            }
            all.position(0).limit(vertexBytes);
            vertexBuffer = upload(vertexBuffer, all, GpuBuffer.USAGE_VERTEX, " vertices");
        } finally {
            MemoryUtil.memFree(all);
        }
        if (indexBytes > 0) {
            ByteBuffer idx = MemoryUtil.memAlloc(indexBytes);
            try {
                for (Batch b : batches) {
                    if (b.indices == null) continue;
                    idx.position(b.firstIndex * b.indexType.bytes);
                    idx.put(b.indices.duplicate());
                }
                idx.position(0).limit(indexBytes);
                indexBuffer = upload(indexBuffer, idx, GpuBuffer.USAGE_INDEX, " indices");
            } finally {
                MemoryUtil.memFree(idx);
            }
        }
        for (Batch b : batches) {
            b.vb = vertexBuffer;
            b.ib = b.indices != null ? indexBuffer : null;
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
        batches.clear();
        FRAME.remove(this);
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
