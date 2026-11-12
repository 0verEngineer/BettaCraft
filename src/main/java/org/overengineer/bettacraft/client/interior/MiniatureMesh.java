package org.overengineer.bettacraft.client.interior;

import com.mojang.blaze3d.vertex.VertexBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;

/** GPU buffers of one miniature, one per chunk render layer. */
final class MiniatureMesh implements AutoCloseable {
    final Map<RenderType, VertexBuffer> buffers = new LinkedHashMap<>();
    long vertexBytes;
    int quads;

    @Override
    public void close() {
        for (VertexBuffer vb : buffers.values()) vb.close();
        buffers.clear();
    }
}
