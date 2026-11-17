package org.overengineer.bettacraft.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.overengineer.bettacraft.BettaCraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One composited texture per look of a betta. The greyscale masks (body, fins, pattern, bars, sheen, eyes) used to be
 * drawn as separate passes on the same model: shader packs without their own translucent entity program (MakeUp,
 * superDuperVanilla) let those coplanar passes z-fight (flickering fish) and Shrimple dropped the translucent passes
 * (no fins). Composited on the CPU, the fish is drawn in a single pass like any other mob.
 * <p>
 * Body and fins are blended "over" each other (rgb = mask.rgb * tint.rgb, alpha = mask.a * tint.a); the overlays are
 * blended "atop", they colour the fish but add no coverage. The textures
 * live in an LRU cache (render thread only) and are released when evicted or when resources reload.
 */
public final class BettaSkins {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/skins");
    private static final int MAX = 1024; // 64x64 RGBA = 16 KiB each
    private static final Map<Identifier, NativeImage> MASKS = new HashMap<>();
    private static final LinkedHashMap<Key, Identifier> SKINS = new LinkedHashMap<>(64, 0.75F, true);
    private static int counter;

    /** A mask texture and its ARGB tint. */
    public record Layer(Identifier mask, int argb) {}

    private record Key(Layer[] layers) {
        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && java.util.Arrays.equals(layers, k.layers);
        }

        @Override
        public int hashCode() {
            return java.util.Arrays.hashCode(layers);
        }
    }

    private BettaSkins() {}

    /** The composited texture of these layers (bottom first); {@code null} if a mask could not be loaded. */
    @Nullable
    public static Identifier get(Layer... layers) {
        Key key = new Key(layers);
        Identifier loc = SKINS.get(key);
        if (loc != null) return loc;
        NativeImage img = composite(layers);
        if (img == null) return null;
        loc = BettaCraft.id("dynamic/betta_skin_" + (counter++));
        Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(loc::toString, img));
        SKINS.put(key, loc);
        if (SKINS.size() > MAX) {
            var it = SKINS.entrySet().iterator();
            Identifier old = it.next().getValue();
            it.remove();
            Minecraft.getInstance().getTextureManager().release(old);
        }
        return loc;
    }

    /** Drops every composite and mask (resource reload: the masks may have changed). */
    public static void clear() {
        var tm = Minecraft.getInstance().getTextureManager();
        SKINS.values().forEach(tm::release);
        SKINS.clear();
        MASKS.values().forEach(NativeImage::close);
        MASKS.clear();
    }

    @Nullable
    private static NativeImage composite(Layer[] layers) {
        NativeImage base = mask(layers[0].mask());
        if (base == null) return null;
        int w = base.getWidth(), h = base.getHeight();
        float[] r = new float[w * h], g = new float[w * h], b = new float[w * h], a = new float[w * h];
        for (int li = 0; li < layers.length; li++) {
            Layer layer = layers[li];
            // the first two layers (body, fins) give the coverage; the overlays are blended "atop" (keep the alpha)
            boolean atop = li >= 2;
            NativeImage m = mask(layer.mask());
            if (m == null || m.getWidth() != w || m.getHeight() != h) continue;
            float tr = ((layer.argb() >> 16) & 0xFF) / 255F, tg = ((layer.argb() >> 8) & 0xFF) / 255F;
            float tb = (layer.argb() & 0xFF) / 255F, ta = ((layer.argb() >>> 24) & 0xFF) / 255F;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int p = net.minecraft.util.ARGB.toABGR(m.getPixel(x, y)); // ABGR
                    float sa = ((p >>> 24) & 0xFF) / 255F * ta;
                    if (sa <= 0) continue;
                    float sr = (p & 0xFF) / 255F * tr, sg = ((p >> 8) & 0xFF) / 255F * tg, sb = ((p >> 16) & 0xFF) / 255F * tb;
                    int i = y * w + x;
                    float da = a[i];
                    if (atop) {
                        if (da <= 0) continue;
                        r[i] += (sr - r[i]) * sa;
                        g[i] += (sg - g[i]) * sa;
                        b[i] += (sb - b[i]) * sa;
                        continue;
                    }
                    float oa = sa + da * (1 - sa);
                    // straight (non-premultiplied) "over"
                    r[i] = (sr * sa + r[i] * da * (1 - sa)) / oa;
                    g[i] = (sg * sa + g[i] * da * (1 - sa)) / oa;
                    b[i] = (sb * sa + b[i] * da * (1 - sa)) / oa;
                    a[i] = oa;
                }
            }
        }
        NativeImage out = new NativeImage(w, h, true);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                int ca = Math.round(a[i] * 255), cr = Math.round(r[i] * 255), cg = Math.round(g[i] * 255), cb = Math.round(b[i] * 255);
                out.setPixelABGR(x, y, ca << 24 | cb << 16 | cg << 8 | cr);
            }
        }
        return out;
    }

    @Nullable
    private static NativeImage mask(Identifier loc) {
        NativeImage img = MASKS.get(loc);
        if (img != null || MASKS.containsKey(loc)) return img;
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(loc)) {
            img = NativeImage.read(in);
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Could not load betta mask {}", loc, e);
            img = null;
        }
        MASKS.put(loc, img);
        return img;
    }
}
