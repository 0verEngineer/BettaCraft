package org.overengineer.bettacraft.platform.client;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/**
 * M52: builds a {@link BakedQuad} vertex by vertex in Minecraft's block vertex format (8 ints per vertex: position,
 * colour ABGR, uv, light, normal), the quads of our dynamic models on both loaders. M52c (26.1): a quad carries its chunk
 * layer ({@link #layer}, default cutout).
 */
public final class QuadBaker {
    private static final int STRIDE = 8;
    private final int[] data = new int[STRIDE * 4];
    private int vertex;
    private TextureAtlasSprite sprite;
    private Direction direction = Direction.DOWN;
    private boolean shade = true;
    private int tint = -1;
    private ChunkSectionLayer layer = ChunkSectionLayer.CUTOUT;

    public QuadBaker sprite(TextureAtlasSprite sprite) {
        this.sprite = sprite;
        return this;
    }

    public QuadBaker direction(Direction direction) {
        this.direction = direction;
        return this;
    }

    public QuadBaker shade(boolean shade) {
        this.shade = shade;
        return this;
    }

    public QuadBaker tint(int tint) {
        this.tint = tint;
        return this;
    }

    /** M52c: the chunk layer the quad is drawn in (26.1 quads carry their layer). */
    public QuadBaker layer(ChunkSectionLayer layer) {
        this.layer = layer;
        return this;
    }

    /** One vertex: position, colour (ARGB), texture coordinates and normal. */
    public QuadBaker vertex(float x, float y, float z, int argb, float u, float v, float nx, float ny, float nz) {
        if (vertex >= 4) throw new IllegalStateException("a quad has four vertices");
        int o = vertex++ * STRIDE;
        data[o] = Float.floatToRawIntBits(x);
        data[o + 1] = Float.floatToRawIntBits(y);
        data[o + 2] = Float.floatToRawIntBits(z);
        int a = argb >>> 24, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        data[o + 3] = a << 24 | b << 16 | g << 8 | r;
        data[o + 4] = Float.floatToRawIntBits(u);
        data[o + 5] = Float.floatToRawIntBits(v);
        data[o + 6] = 0;
        data[o + 7] = ((int) (nx * 127F) & 0xFF) | ((int) (ny * 127F) & 0xFF) << 8 | ((int) (nz * 127F) & 0xFF) << 16;
        return this;
    }

    public BakedQuad bake() {
        if (vertex != 4) throw new IllegalStateException("a quad needs four vertices, has " + vertex);
        BakedQuad q = ClientPlatform.get().quad(data.clone(), tint, direction, sprite, shade ? null : Direction.UP, false, layer); // M52e: 26.3 shades by a direction (up = unshaded)
        vertex = 0;
        java.util.Arrays.fill(data, 0);
        return q;
    }
}
