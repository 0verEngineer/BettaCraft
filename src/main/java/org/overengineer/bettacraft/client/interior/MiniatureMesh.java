package org.overengineer.bettacraft.client.interior;

import com.mojang.blaze3d.vertex.VertexBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;

/** GPU buffers of one miniature cell (2x2x2 sections), one per chunk render layer. */
final class MiniatureMesh implements AutoCloseable {
    final Map<RenderType, VertexBuffer> buffers = new LinkedHashMap<>();
    /** M21.1: the blocks of the mesh plants (Rotala, Vallisneria, Anubias), left out while the tank shows their meshes. */
    final Map<RenderType, VertexBuffer> plantBuffers = new LinkedHashMap<>();
    long vertexBytes;
    int quads;
    /** Interior-local bounds of the cell (for frustum culling): min x/y/z, size in blocks. */
    int minX, minY, minZ, extent;

    @Override
    public void close() {
        for (VertexBuffer vb : buffers.values()) vb.close();
        buffers.clear();
        for (VertexBuffer vb : plantBuffers.values()) vb.close();
        plantBuffers.clear();
    }
}
