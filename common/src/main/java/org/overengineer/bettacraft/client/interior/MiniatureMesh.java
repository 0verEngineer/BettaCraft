package org.overengineer.bettacraft.client.interior;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

/**
 * GPU buffers of one miniature cell (2x2x2 sections), one per chunk layer. M52b (1.21.11): GPU buffers of the blaze3d
 * device (the vertex buffers of 1.21.1 are gone), drawn with the terrain pipelines like vanilla's chunk sections.
 */
final class MiniatureMesh implements AutoCloseable {
    /**
     * A vertex buffer with its quad count (the indices come from the shared sequential quad index buffer) and its vertex format
     * (M53b: with an Iris shader pack the terrain pipelines take Iris's extended format; a buffer in another format is not drawn).
     */
    record Buffer(GpuBuffer vertices, int indexCount, com.mojang.blaze3d.vertex.VertexFormat format) {
        static Buffer upload(MeshData d, String label) {
            try (d) {
                GpuBuffer vb = RenderSystem.getDevice().createBuffer(() -> label, GpuBuffer.USAGE_VERTEX, d.vertexBuffer());
                return new Buffer(vb, d.drawState().indexCount(), d.drawState().format());
            }
        }

        void close() {
            vertices.close();
        }
    }

    final Map<ChunkSectionLayer, Buffer> buffers = new EnumMap<>(ChunkSectionLayer.class);
    /** M21.1: the blocks of the mesh plants (Rotala, Vallisneria, Anubias), left out while the tank shows their meshes. */
    final Map<ChunkSectionLayer, Buffer> plantBuffers = new EnumMap<>(ChunkSectionLayer.class);
    /** M22.1: the swaying water plants (kelp, seagrass, corals ...): static, drawn while they do not sway ({@link MiniatureSway}). */
    final Map<ChunkSectionLayer, Buffer> swayBuffers = new EnumMap<>(ChunkSectionLayer.class);
    /** M22.1: the same blocks for drawing with their sway, or null. */
    MiniatureSway.Data sway;
    long vertexBytes;
    int quads;
    /** Interior-local bounds of the cell (for frustum culling): min x/y/z, size in blocks. */
    int minX, minY, minZ, extent;

    /** M53b: a buffer is in another vertex format than its pipeline takes; the cell is rebuilt (asked once). */
    boolean formatStale;

    /** M53b: every buffer of this layer is in the vertex format of the layer's pipeline. */
    boolean formatMatches(ChunkSectionLayer layer, com.mojang.blaze3d.vertex.VertexFormat format) {
        for (Map<ChunkSectionLayer, Buffer> m : List.of(buffers, plantBuffers, swayBuffers)) {
            Buffer b = m.get(layer);
            if (b != null && !b.format().equals(format)) return false;
        }
        return true;
    }

    @Override
    public void close() {
        for (Buffer b : buffers.values()) b.close();
        buffers.clear();
        for (Buffer b : plantBuffers.values()) b.close();
        plantBuffers.clear();
        for (Buffer b : swayBuffers.values()) b.close();
        swayBuffers.clear();
        sway = null;
    }
}
