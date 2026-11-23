package org.overengineer.bettacraft.client.tank;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.debug.LogArea;

/**
 * M64: frameless copies of the glass textures for the connected glass of textured tanks.
 * <p>
 * Until M64 the frame of a glass texture was hidden only by stretching the UVs over it ({@link ConnectedGlass#remap}). That
 * works for the full-size texture, but not for its mipmaps: a smaller mip level averages the frame into the texels next to
 * it, so at a distance (or at a flat angle, where the GPU picks a small mip level sooner) every block of a tank showed a
 * frame of its own again - a grid over the whole tank. Now the block atlas holds, for every glass texture, 15 copies in
 * which the frame is replaced by the inside of the glass on 1 to 4 of its edges ({@link #variantId}); their mipmaps are made
 * from the frameless pixels, so the frame stays away at every distance. The textured tank picks the copy whose missing
 * edges are the edges where the face goes on in the neighbouring block and keeps the UVs as they are.
 * <p>
 * The copies are made for the block textures whose name contains "glass" (any mod), square, 16 to 128 pixels, not animated,
 * with a frame ({@link ConnectedGlass#frameTexels}). A tank whose glass has no copies (a texture named otherwise) keeps the
 * stretched UVs. The source goes into the block atlas through {@code SpriteSourceListMixin}; it is not a registered sprite
 * source type (no atlas file names it).
 */
public final class GlassVariants implements SpriteSource {
    public static final GlassVariants INSTANCE = new GlassVariants();
    /** The atlas these copies go into. */
    public static final Identifier BLOCK_ATLAS = net.minecraft.data.AtlasIds.BLOCKS;
    private static final String PREFIX = "tank_glass/";
    private static final MapCodec<GlassVariants> CODEC = MapCodec.unit(INSTANCE);
    /** Texture edges: bit 0 = left (u = 0), 1 = right (u = 1), 2 = top (v = 0), 3 = bottom (v = 1). */
    public static final int LEFT = 1, RIGHT = 2, TOP = 4, BOTTOM = 8;

    /** Smoke tests: the old way (UVs stretched over the frame) for the before picture of the glass guard. */
    public static volatile boolean offForTest;
    /** Smoke tests: every face without any frame (also on the tank's outer edges): the reference picture of the glass guard. */
    public static volatile boolean allFramelessForTest;

    private GlassVariants() {}

    /** The sprite of {@code glass} (a block atlas sprite) without its frame on the texture edges {@code edges} (1..15). */
    public static Identifier variantId(Identifier glass, int edges) {
        return BettaCraft.id(PREFIX + glass.getNamespace() + "/" + glass.getPath() + "/" + edges);
    }

    /** Whether a texture (path below textures/, without .png) gets frameless copies: a block texture named glass, not a pane. */
    static boolean candidate(Identifier texture) {
        String p = texture.getPath();
        if (!p.startsWith("block")) return false;
        String name = p.substring(p.lastIndexOf('/') + 1);
        return name.contains("glass") && !name.contains("pane") && !name.endsWith("_n") && !name.endsWith("_s") && !name.endsWith("_e");
    }

    @Override
    public void run(ResourceManager rm, SpriteSource.Output out) {
        int made = 0;
        Map<Identifier, Resource> found = new java.util.HashMap<>(rm.listResources("textures/block", loc -> loc.getPath().endsWith(".png")));
        found.putAll(rm.listResources("textures/blocks", loc -> loc.getPath().endsWith(".png")));
        for (var e : found.entrySet()) {
            Identifier tex = TEXTURE_ID_CONVERTER.fileToId(e.getKey());
            if (!candidate(tex)) continue;
            int[] argb;
            int w, h;
            Optional<TextureMetadataSection> texMeta;
            try {
                var meta = e.getValue().metadata();
                if (meta.getSection(net.minecraft.client.resources.metadata.animation.AnimationMetadataSection.TYPE).isPresent()) continue;
                // 1.21.11: the copy keeps the texture section of the original (vanilla glass: mipmap strategy "mean")
                texMeta = meta.getSection(TextureMetadataSection.TYPE);
                try (InputStream in = e.getValue().open(); NativeImage img = NativeImage.read(in)) {
                    w = img.getWidth();
                    h = img.getHeight();
                    if (w != h || w < 16 || w > 128 || Integer.bitCount(w) != 1) continue;
                    argb = new int[w * h];
                    for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) argb[y * w + x] = img.getPixel(x, y); // 1.21.11: getPixel is ARGB
                }
            } catch (Exception ex) {
                if (LogArea.RENDER.debug) LogArea.RENDER.debug("glass variants: {} not read ({})", tex, ex.toString());
                continue;
            }
            int f = ConnectedGlass.frameTexels(argb, w, h);
            if (f <= 0) continue;
            for (int edges = 1; edges < 16; edges++) {
                int[] px = ConnectedGlass.withoutFrame(argb, w, h, f, edges);
                Identifier id = variantId(tex, edges);
                final int width = w, height = h;
                final Optional<TextureMetadataSection> section = texMeta;
                out.add(id, loader -> {
                    NativeImage img = new NativeImage(width, height, false);
                    for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) img.setPixel(x, y, px[y * width + x]);
                    return new SpriteContents(id, new FrameSize(width, height), img, Optional.empty(), List.of(), section);
                });
            }
            made++;
        }
        LogArea.RENDER.info("Connected tank glass: frameless copies of {} glass textures", made);
    }

    @Override
    public MapCodec<? extends SpriteSource> codec() {
        return CODEC;
    }

    /** The frameless copy of {@code glass} for these texture edges, or null when the atlas has none. */
    @Nullable
    public static TextureAtlasSprite variant(TextureAtlasSprite glass, int edges) {
        if (edges <= 0 || edges > 15 || offForTest) return null;
        if (!net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS.equals(glass.atlasLocation())) return null;
        TextureAtlasSprite s = org.overengineer.bettacraft.client.ClientCompat.blockAtlas().getSprite(variantId(glass.contents().name(), edges));
        return s == null || s.contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation()) ? null : s;
    }
}
