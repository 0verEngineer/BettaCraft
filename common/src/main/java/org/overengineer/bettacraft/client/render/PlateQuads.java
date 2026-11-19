package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.overengineer.bettacraft.BettaCraft;

/**
 * M52e: the flat coloured plates of the monitor and the tank signs (backgrounds, grid and graph lines). Minecraft 26.3
 * removed the untextured {@code RenderTypes#textBackground} (text backgrounds are glyphs of the font now), so they are
 * drawn as text with a plain white texture, tinted by the vertex colour and lit by the lightmap like before. The
 * consumer adds the texture coordinate the text format needs to every vertex.
 */
public final class PlateQuads {
    public static final Identifier TEXTURE = BettaCraft.id("textures/misc/plate.png");

    private PlateQuads() {}

    public static RenderType type() {
        return RenderTypes.text(TEXTURE);
    }

    /** The plate consumer of a buffer source: write position, colour and light as for the old text background. */
    public static VertexConsumer buffer(MultiBufferSource buffers) {
        return new Plate(buffers.getBuffer(type()));
    }

    private record Plate(VertexConsumer d) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) { d.addVertex(x, y, z); d.setUv(0.5F, 0.5F); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { d.setColor(r, g, b, a); return this; }
        @Override public VertexConsumer setColor(int argb) { d.setColor(argb); return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { d.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { d.setUv2(u, v); return this; }
        @Override public VertexConsumer setUv3(float u, float v) { d.setUv3(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { d.setNormal(x, y, z); return this; }
        @Override public VertexConsumer setLineWidth(float width) { d.setLineWidth(width); return this; }
    }
}
