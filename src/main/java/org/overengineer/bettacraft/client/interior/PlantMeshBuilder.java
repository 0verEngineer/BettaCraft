package org.overengineer.bettacraft.client.interior;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import org.joml.Vector3f;
import org.overengineer.bettacraft.plants.PlantColors;
import org.overengineer.bettacraft.plants.PlantPart;
import org.overengineer.bettacraft.plants.PlantSpecies;

/**
 * Builds the quads of one plant ({@link PlantMeshRenderer.Mesh}); positions relative to the corner of the root block,
 * in interior blocks. The structure comes from the blocks of the plant alone ({@link PlantSkeleton}), so plants of
 * existing worlds, trimmed plants and half grown ones all get their mesh.
 * <p>
 * M21/M21.1: Rotala (stems with pairs of round leaves), Vallisneria (continuous ribbons), Anubias (rhizome, petioles,
 * broad blades). M22.1: every other species with a shape of its own:
 * <ul>
 *     <li>Java fern and the Bucephalandras: a creeping rhizome, every leaf a curved blade from the rhizome to its tip
 *     (long lanceolate fronds, small wavy leaves, needles).</li>
 *     <li>Cryptocorynes and the Amazon sword: rosettes, every leaf a petiole and a curved blade from the crown.</li>
 *     <li>Ludwigia and Alternanthera: stems like the Rotala with their own, larger leaves.</li>
 *     <li>Java moss: fine sprigs on every block; Monte Carlo: tiny round leaves on runners.</li>
 *     <li>Tiger lotus: the bulb, mottled leaves under water and floating leaves on long petioles.</li>
 *     <li>Corals: a tissue mat with polyps that close when something swims by (zoanthids, star polyps), mushroom discs
 *     that rise and sink, the leather coral's trunk and cap with waving polyps, Acropora branches and the Lobophyllia
 *     dome (rigid, small waving polyps), the hammer coral's skeleton with pulsing tentacle heads, the bubble tip
 *     anemone's foot, oral disc and tentacles with bubble tips.</li>
 * </ul>
 */
final class PlantMeshBuilder {
    final PlantMeshRenderer.Mesh m = new PlantMeshRenderer.Mesh();
    final BlockPos root;
    final PlantSpecies sp;
    /** The parts of the plant's blocks (keys {@link BlockPos#asLong} relative to the root) and their redness. */
    final Long2ObjectOpenHashMap<PlantPart> blocks;
    final Long2IntOpenHashMap tints;
    final PlantMeshRenderer.Style style;
    final TextureAtlasSprite[] sprites;
    int curLight;
    int curColor;
    int curMode = PlantMeshRenderer.ANIM_NONE;
    final Vector3f anchor = new Vector3f();
    float reach;

    PlantMeshBuilder(BlockPos root, PlantSpecies sp, Long2ObjectOpenHashMap<PlantPart> blocks, Long2IntOpenHashMap tints,
                     PlantMeshRenderer.Style style) {
        this.root = root;
        this.sp = sp;
        this.blocks = blocks;
        this.tints = tints;
        this.style = style;
        this.sprites = style.atlas() ? PlantMeshRenderer.miniSprites() : null;
    }

    void build() {
        switch (sp) {
            case ROTALA -> stems(PlantTex.ROUND, 1.0F, 0.68F, 0.5F);
            case LUDWIGIA -> stems(PlantTex.ELLIPTIC, 1.55F, 0.95F, 0.55F);
            case ALTERNANTHERA -> stems(PlantTex.NARROW, 1.75F, 0.62F, 0.45F);
            case VALLISNERIA -> ribbons();
            case ANUBIAS_NANA -> anubias();
            case JAVA_FERN -> rhizomeLeaves(PlantTex.LANCE, 0.10F, 1.8F, 3.0F, 0.3F, 0.02, 0.6);
            case BUCE_GREEN_WAVY, BUCE_BROWNIE_RED, BUCE_BLUE -> rhizomeLeaves(PlantTex.WAVY, 0.22F, 1.5F, 2.8F, 0.22F, 0.01, 0.15);
            case BUCE_MINI_NEEDLE -> rhizomeLeaves(PlantTex.NEEDLE, 0.15F, 0.9F, 1.3F, 0.2F, 0.01, 0.15);
            case CRYPT_GREEN, CRYPT_BROWN, CRYPT_FLAMINGO -> rosette(PlantTex.CRYPT, 0.32F, 1.7F, 3.0F, 0.025, 0.4);
            case AMAZON_SWORD -> rosette(PlantTex.SWORD, 0.38F, 2.3F, 4.6F, 0.03, 0.7);
            case JAVA_MOSS -> moss();
            case MONTE_CARLO -> carpet();
            case TIGER_LOTUS -> lotus();
            case ZOANTHUS, GREEN_STAR_POLYPS -> polyps();
            case MUSHROOM_CORAL -> mushrooms();
            case LEATHER_CORAL -> toadstool();
            case ACROPORA -> acropora();
            case HAMMER_CORAL -> hammer();
            case LOBOPHYLLIA -> dome();
            case BUBBLE_TIP_ANEMONE -> anemone();
            // M28 part a
            case ANUBIAS_PETITE -> anubias(PlantTex.OVATE, 0.8F);
            case ANUBIAS_PINTO -> anubias(PlantTex.PINTO, 1F);
            case ANUBIAS_WHITE -> anubias(PlantTex.OVATE, 1F);
            case ANUBIAS_COFFEEFOLIA -> anubias(PlantTex.COFFEE, 1.05F);
            case ANUBIAS_ROUND_LEAF -> anubias(PlantTex.ROUND, 1.2F);
            case BUCE_KEDAGANG -> rhizomeLeaves(PlantTex.NARROW, 0.2F, 1.1F, 1.9F, 0.22F, 0.01, 0.15);
            case BUCE_THEIA, BUCE_PINK_LADY, BUCE_SNOW_WHITE -> rhizomeLeaves(PlantTex.WAVY, 0.22F, 1.5F, 2.8F, 0.22F, 0.01, 0.15);
            case BUCE_GODZILLA -> rhizomeLeaves(PlantTex.WAVY, 0.2F, 1.7F, 3.1F, 0.25F, 0.01, 0.15);
            case BUCE_MINI_PINK -> rhizomeLeaves(PlantTex.WAVY, 0.22F, 1.1F, 1.9F, 0.18F, 0.01, 0.15);
            case LUDWIGIA_SUPER_RED -> stems(PlantTex.ELLIPTIC, 1.45F, 0.85F, 0.55F);
            case ROTALA_HRA -> stems(PlantTex.NARROW, 1.15F, 0.5F, 0.5F);
            case ROTALA_WALLICHII -> stems(PlantTex.NEEDLE, 1.0F, 0.4F, 0.45F, 3);
            case HYGROPHILA_PINNATIFIDA -> stems(PlantTex.LOBED, 2.0F, 1.0F, 0.6F);
            case POGOSTEMON_DOWNOI -> stems(PlantTex.CRISP, 1.1F, 0.55F, 0.7F, 2);
            case LIMNOPHILA_SESSILIFLORA -> stems(PlantTex.FEATHER, 1.2F, 0.8F, 0.5F, 3);
            case MYRIOPHYLLUM_RED -> stems(PlantTex.FEATHER, 1.1F, 0.75F, 0.5F, 3);
            case BACOPA_CAROLINIANA -> stems(PlantTex.ROUND, 1.25F, 0.95F, 0.5F);
            // M28 part b
            case HC_CUBA -> tinyCarpet();
            case ELEOCHARIS -> hairgrass();
            case STAUROGYNE -> staurogyne();
            case MARSILEA -> clover();
            case CHRISTMAS_MOSS -> christmasMoss();
            case FLAME_MOSS -> flameMoss();
            case RICCIA -> riccia();
            case MARIMO -> marimo();
            case ECHINODORUS_RED_DIAMOND -> rosette(PlantTex.SWORD, 0.32F, 1.5F, 2.8F, 0.025, 0.5);
            case CRYPT_PARVA -> rosette(PlantTex.NARROW, 0.15F, 0.55F, 0.9F, 0.02, 0.3);
            case SAGITTARIA -> rosette(PlantTex.NARROW, 0.06F, 0.5F, 0.8F, 0.03, 0.45);
            case APONOGETON -> aponogeton();
            case NYMPHAEA_GREEN -> lotus();
        }
    }

    // ------------------------------------------------------------------ blocks

    PlantPart part(int x, int y, int z) {
        return blocks.get(BlockPos.asLong(x, y, z));
    }

    PlantPart part(long skeletonKey) {
        return part(PlantSkeleton.x(skeletonKey), PlantSkeleton.y(skeletonKey), PlantSkeleton.z(skeletonKey));
    }

    boolean has(int x, int y, int z) {
        return blocks.containsKey(BlockPos.asLong(x, y, z));
    }

    /** The blocks of the plant as parts, keyed for {@link PlantSkeleton}. */
    Long2ObjectOpenHashMap<PlantPart> parts() {
        Long2ObjectOpenHashMap<PlantPart> out = new Long2ObjectOpenHashMap<>();
        for (var e : blocks.long2ObjectEntrySet()) {
            long k = e.getLongKey();
            out.put(PlantSkeleton.key(BlockPos.getX(k), BlockPos.getY(k), BlockPos.getZ(k)), e.getValue());
        }
        return out;
    }

    /** Colour of a part of this plant at the block (its redness) and the light of the block, for the next quads. */
    void use(int x, int y, int z, PlantPart part) {
        curColor = colorOf(x, y, z, part);
        long key = root.offset(x, y, z).asLong();
        int i = m.lightIndex.getOrDefault(key, -1);
        if (i < 0) {
            i = m.lightPos.size();
            m.lightPos.add(key);
            m.lightIndex.put(key, i);
        }
        curLight = i;
    }

    void use(long skeletonKey, PlantPart part) {
        use(PlantSkeleton.x(skeletonKey), PlantSkeleton.y(skeletonKey), PlantSkeleton.z(skeletonKey), part);
    }

    int colorOf(int x, int y, int z, PlantPart part) {
        int tint = tints.getOrDefault(BlockPos.asLong(x, y, z), 0);
        return 0xFF000000 | saturate(PlantColors.color(sp, part, tint), style.saturate());
    }

    /** RGB with the saturation scaled by {@code f} (1 = unchanged). */
    static int saturate(int rgb, float f) {
        if (f == 1F) return rgb;
        float r = (rgb >> 16 & 255), g = (rgb >> 8 & 255), b = (rgb & 255);
        float grey = r * 0.299F + g * 0.587F + b * 0.114F;
        int nr = Math.clamp(Math.round(grey + (r - grey) * f), 0, 255);
        int ng = Math.clamp(Math.round(grey + (g - grey) * f), 0, 255);
        int nb = Math.clamp(Math.round(grey + (b - grey) * f), 0, 255);
        return nr << 16 | ng << 8 | nb;
    }

    /** The next quads move like this (corals): anchored at {@code at}, the weight grows to 1 at {@code reach} from it. */
    void anim(int mode, Vector3f at, float reach) {
        curMode = mode;
        anchor.set(at);
        this.reach = reach;
    }

    void still() {
        curMode = PlantMeshRenderer.ANIM_NONE;
        reach = 0;
    }

    static Vector3f c(int x, int y, int z) {
        return new Vector3f(x + 0.5F, y + 0.5F, z + 0.5F);
    }

    static Vector3f c(long skeletonKey) {
        return c(PlantSkeleton.x(skeletonKey), PlantSkeleton.y(skeletonKey), PlantSkeleton.z(skeletonKey));
    }

    /** A small stable number per position (0..1) for the little variations of a plant. */
    static float hash(int x, int y, int z, int salt) {
        int h = x * 73856093 ^ y * 19349663 ^ z * 83492791 ^ salt * 0x9E3779B9;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return (h & 0xFFFF) / 65535F;
    }

    static float hash(long k, int salt) {
        return hash(PlantSkeleton.x(k), PlantSkeleton.y(k), PlantSkeleton.z(k), salt);
    }

    // ------------------------------------------------------------------ primitives

    void quad(PlantTex tex, Vector3f a, Vector3f b, Vector3f c, Vector3f d, float u0, float v0, float u1, float v1, Vector3f n) {
        if (sprites != null) {
            // the atlas does not repeat: tiled stems and ribbons are folded back into one tile per quad
            float shift = (float) Math.floor(Math.min(v0, v1));
            v0 = Math.clamp(v0 - shift, 0F, 1F);
            v1 = Math.clamp(v1 - shift, 0F, 1F);
        }
        quad4(tex, a, b, c, d, u0, v1, u1, v1, u1, v0, u0, v0, n, n, n, n, curColor, curColor, curColor, curColor);
    }

    /** One quad with its own uv, normal and colour per corner (uv within 0..1). */
    void quad4(PlantTex tex, Vector3f a, Vector3f b, Vector3f c, Vector3f d, float ua, float va, float ub, float vb, float uc, float vc,
               float ud, float vd, Vector3f na, Vector3f nb, Vector3f nc, Vector3f nd, int ca, int cb, int cc, int cd) {
        PlantMeshRenderer.Layer l = m.layers[tex.ordinal()];
        TextureAtlasSprite s = sprites == null ? null : sprites[tex.ordinal()];
        vert(l, s, a, ua, va, na, ca);
        vert(l, s, b, ub, vb, nb, cb);
        vert(l, s, c, uc, vc, nc, cc);
        vert(l, s, d, ud, vd, nd, cd);
        l.light.add(curLight);
        l.mode.add(curMode);
        m.quads++;
    }

    private void vert(PlantMeshRenderer.Layer l, TextureAtlasSprite s, Vector3f p, float u, float v, Vector3f n, int color) {
        if (s != null) {
            u = s.getU(Math.clamp(u, 0F, 1F));
            v = s.getV(Math.clamp(v, 0F, 1F));
        }
        l.v.add(p.x);
        l.v.add(p.y);
        l.v.add(p.z);
        l.v.add(u);
        l.v.add(v);
        l.v.add(n.x);
        l.v.add(n.y);
        l.v.add(n.z);
        l.v.add(Math.max(0, p.y));
        l.v.add(anchor.x);
        l.v.add(anchor.y);
        l.v.add(anchor.z);
        l.v.add(curMode == PlantMeshRenderer.ANIM_NONE || reach <= 0 ? 0F : Math.min(1F, p.distance(anchor) / reach));
        // a phase per anchor: neighbouring polyps and tentacles move out of step
        l.v.add((float) ((anchor.x * 12.9898 + anchor.y * 37.719 + anchor.z * 78.233) % (Math.PI * 2)));
        l.color.add(color);
    }

    /** A thin square tube from a to b (stem texture). */
    void tube(Vector3f a, Vector3f b, float r) {
        tube(PlantTex.STEM, a, b, r, r, 4);
    }

    /** A tube with {@code sides} sides from a (radius r0) to b (radius r1), the texture repeating along it. */
    void tube(PlantTex tex, Vector3f a, Vector3f b, float r0, float r1, int sides) {
        Vector3f d = new Vector3f(b).sub(a);
        float len = d.length();
        if (len < 1e-4F) return;
        d.div(len);
        Vector3f s = Math.abs(d.y) > 0.9F ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0).cross(d).normalize();
        Vector3f t = new Vector3f(d).cross(s).normalize();
        Vector3f[] dir = new Vector3f[sides];
        for (int i = 0; i < sides; i++) {
            double ang = Math.PI * 2 * i / sides + (sides == 4 ? 0 : Math.PI / sides);
            dir[i] = new Vector3f(s).mul((float) Math.cos(ang)).add(new Vector3f(t).mul((float) Math.sin(ang)));
        }
        float vLen = tex == PlantTex.STEM ? len : len * 0.5F;
        for (int i = 0; i < sides; i++) {
            Vector3f o0 = dir[i], o1 = dir[(i + 1) % sides];
            Vector3f n = new Vector3f(o0).add(o1).normalize();
            float u0 = (float) i / sides, u1 = (float) (i + 1) / sides;
            quad(tex, new Vector3f(a).add(new Vector3f(o0).mul(r0)), new Vector3f(a).add(new Vector3f(o1).mul(r0)),
                    new Vector3f(b).add(new Vector3f(o1).mul(r1)), new Vector3f(b).add(new Vector3f(o0).mul(r1)), u0, 0, u1, vLen, n);
        }
    }

    /** Tubes along a polyline (smoothly thinner from r0 to r1). */
    void tubes(PlantTex tex, List<Vector3f> pts, float r0, float r1, int sides) {
        float total = 0;
        for (int i = 1; i < pts.size(); i++) total += pts.get(i).distance(pts.get(i - 1));
        float at = 0;
        for (int i = 1; i < pts.size(); i++) {
            float seg = pts.get(i).distance(pts.get(i - 1));
            float ra = r0 + (r1 - r0) * (total <= 0 ? 0 : at / total);
            float rb = r0 + (r1 - r0) * (total <= 0 ? 1 : (at + seg) / total);
            tube(tex, pts.get(i - 1), pts.get(i), ra, rb, sides);
            at += seg;
        }
    }

    /**
     * A leaf blade from {@code base} along {@code dir} ({@code len} long, {@code width} wide), the blade in the plane of
     * dir and {@code across}, arched a little towards its upper side.
     */
    void leaf(PlantTex tex, Vector3f base, Vector3f dir, Vector3f across, float len, float width, float arch) {
        Vector3f d = new Vector3f(dir).normalize();
        Vector3f s = new Vector3f(across).normalize();
        Vector3f n = new Vector3f(s).cross(d).normalize();
        if (n.y < 0) {
            n.negate();
            s.negate();
        }
        Vector3f half = new Vector3f(s).mul(width / 2);
        Vector3f mid = new Vector3f(base).add(new Vector3f(d).mul(len / 2)).add(new Vector3f(n).mul(arch * len));
        Vector3f tip = new Vector3f(base).add(new Vector3f(d).mul(len));
        // base half (v 1 .. 0.5), tip half (v 0.5 .. 0)
        quad(tex, new Vector3f(base).sub(half), new Vector3f(base).add(half), new Vector3f(mid).add(half), new Vector3f(mid).sub(half), 0, 0.5F, 1, 1, n);
        quad(tex, new Vector3f(mid).sub(half), new Vector3f(mid).add(half), new Vector3f(tip).add(half), new Vector3f(tip).sub(half), 0, 0, 1, 0.5F, n);
    }

    /**
     * A blade along a curve (base first, tip last): the leaf texture from v = 1 at the base to v = 0 at the tip, the
     * blade lying across the curve (horizontal across, turned by {@code twist} towards the tip).
     */
    void blade(PlantTex tex, List<Vector3f> pts, float width, float twist, float yawSeed) {
        int n = pts.size();
        if (n < 2) return;
        float total = 0;
        for (int i = 1; i < n; i++) total += pts.get(i).distance(pts.get(i - 1));
        if (total < 1e-3F) return;
        Vector3f[] side = new Vector3f[n];
        Vector3f[] nrm = new Vector3f[n];
        // one direction across for the whole blade (horizontal, across its way from base to tip), so it never flips
        Vector3f way = new Vector3f(pts.getLast()).sub(pts.getFirst());
        Vector3f across0 = new Vector3f(way.z, 0, -way.x);
        if (across0.lengthSquared() < 1e-4F) across0.set((float) Math.cos(yawSeed), 0, (float) Math.sin(yawSeed));
        across0.normalize();
        for (int i = 0; i < n; i++) {
            Vector3f t = new Vector3f(pts.get(Math.min(n - 1, i + 1))).sub(pts.get(Math.max(0, i - 1)));
            if (t.lengthSquared() < 1e-8F) t.set(0, 1, 0);
            t.normalize();
            Vector3f s = new Vector3f(across0).sub(new Vector3f(t).mul(across0.dot(t)));
            if (s.lengthSquared() < 1e-4F) s = i > 0 ? new Vector3f(side[i - 1]) : new Vector3f(across0);
            s.normalize();
            // a gentle twist towards the tip (around the blade's own line)
            float tw = twist * i / (n - 1);
            Vector3f ts = new Vector3f(t).cross(s);
            s = new Vector3f(s).mul((float) Math.cos(tw)).add(ts.mul((float) Math.sin(tw))).normalize();
            side[i] = s;
            Vector3f nv = new Vector3f(s).cross(t).normalize();
            if (i == 0 ? nv.y < 0 : nv.dot(nrm[i - 1]) < 0) nv.negate();
            nrm[i] = nv;
        }
        float at = 0;
        float half = width / 2;
        for (int i = 1; i < n; i++) {
            Vector3f a = pts.get(i - 1), b = pts.get(i);
            float seg = a.distance(b);
            float va = 1 - at / total, vb = 1 - (at + seg) / total;
            Vector3f sa = new Vector3f(side[i - 1]).mul(half), sb = new Vector3f(side[i]).mul(half);
            quad4(tex, new Vector3f(a).sub(sa), new Vector3f(a).add(sa), new Vector3f(b).add(sb), new Vector3f(b).sub(sb),
                    0, va, 1, va, 1, vb, 0, vb, nrm[i - 1], nrm[i - 1], nrm[i], nrm[i], curColor, curColor, curColor, curColor);
            at += seg;
        }
    }

    /**
     * A round disc around {@code center} facing {@code up} ({@code segments} slices, two rings), the texture mapped flat
     * (u along {@code ux}); the middle raised by {@code dome}, the rim waved by {@code wave}. Colours: {@code inner} in
     * the middle, {@code outer} at the rim.
     */
    void disc(PlantTex tex, Vector3f center, Vector3f up, Vector3f ux, float radius, float dome, float wave, int segments, int inner, int outer) {
        Vector3f nu = new Vector3f(up).normalize();
        Vector3f ax = new Vector3f(ux).sub(new Vector3f(nu).mul(ux.dot(nu)));
        if (ax.lengthSquared() < 1e-6F) ax = Math.abs(nu.y) < 0.9F ? new Vector3f(0, 1, 0).cross(nu) : new Vector3f(1, 0, 0);
        ax.normalize();
        Vector3f az = new Vector3f(nu).cross(ax).normalize();
        Vector3f mid = new Vector3f(center).add(new Vector3f(nu).mul(dome));
        int midColor = lerpColor(inner, outer, 0.5F);
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments, a1 = Math.PI * 2 * (i + 1) / segments;
            Vector3f[] ring = new Vector3f[4];
            float[][] uv = new float[4][];
            double[] angs = {a0, a1};
            int idx = 0;
            for (float rr : new float[] {0.5F, 1F}) {
                for (double a : angs) {
                    float w = rr == 1F ? (float) (wave * Math.sin(a * 5 + center.x)) : 0F;
                    float rad = radius * rr;
                    Vector3f p = new Vector3f(center).add(new Vector3f(ax).mul((float) Math.cos(a) * rad)).add(new Vector3f(az).mul((float) Math.sin(a) * rad))
                            .add(new Vector3f(nu).mul(dome * (1 - rr * rr) + w));
                    ring[idx] = p;
                    uv[idx] = new float[] {0.5F + 0.5F * rr * (float) Math.cos(a), 0.5F + 0.5F * rr * (float) Math.sin(a)};
                    idx++;
                }
            }
            // inner slice: a triangle (centre twice), outer ring: a quad
            quad4(tex, mid, ring[0], ring[1], mid, 0.5F, 0.5F, uv[0][0], uv[0][1], uv[1][0], uv[1][1], 0.5F, 0.5F, nu, nu, nu, nu,
                    inner, midColor, midColor, inner);
            quad4(tex, ring[0], ring[2], ring[3], ring[1], uv[0][0], uv[0][1], uv[2][0], uv[2][1], uv[3][0], uv[3][1], uv[1][0], uv[1][1],
                    nu, nu, nu, nu, midColor, outer, outer, midColor);
        }
    }

    /** A little star of crossed quads (polyps, bubble tips, moss): two upright planes and one lying flat. */
    void cross(PlantTex tex, Vector3f center, float size, float yaw, boolean flat) {
        float h = size / 2;
        for (int i = 0; i < 2; i++) {
            double a = yaw + i * Math.PI / 2;
            Vector3f s = new Vector3f((float) Math.cos(a) * h, 0, (float) Math.sin(a) * h);
            Vector3f n = new Vector3f((float) -Math.sin(a), 0, (float) Math.cos(a));
            quad(tex, new Vector3f(center).sub(s).sub(0, h, 0), new Vector3f(center).add(s).sub(0, h, 0), new Vector3f(center).add(s).add(0, h, 0),
                    new Vector3f(center).sub(s).add(0, h, 0), 0, 0, 1, 1, n);
        }
        if (flat) {
            Vector3f sx = new Vector3f((float) Math.cos(yaw) * h, 0, (float) Math.sin(yaw) * h);
            Vector3f sz = new Vector3f((float) -Math.sin(yaw) * h, 0, (float) Math.cos(yaw) * h);
            Vector3f up = new Vector3f(0, 1, 0);
            quad(tex, new Vector3f(center).sub(sx).sub(sz), new Vector3f(center).add(sx).sub(sz), new Vector3f(center).add(sx).add(sz),
                    new Vector3f(center).sub(sx).add(sz), 0, 0, 1, 1, up);
        }
    }

    /**
     * A skin over columns of the plant (dome, cap, tissue mat): {@code top} gives the height of the surface per column
     * ({@link #col}), {@code colour} its colour; corners shared by four columns lie on their mean, corners at the rim
     * {@code rimDrop} lower (the skin bends down to the rock). {@code down}: the underside (normals down).
     */
    Long2FloatOpenHashMap heightfield(PlantTex tex, Long2FloatOpenHashMap top, Long2IntOpenHashMap colour, Long2IntOpenHashMap lightY, float rimDrop,
                                      boolean down, int tile) {
        return heightfield(tex, top, colour, lightY, rimDrop, down, tile, null);
    }

    /** {@code rimTo}: per column the height the rim comes down to (null: just {@code rimDrop}). */
    Long2FloatOpenHashMap heightfield(PlantTex tex, Long2FloatOpenHashMap top, Long2IntOpenHashMap colour, Long2IntOpenHashMap lightY, float rimDrop,
                                      boolean down, int tile, Long2FloatOpenHashMap rimTo) {
        Long2FloatOpenHashMap h = new Long2FloatOpenHashMap();
        if (top.isEmpty()) return h;
        Long2IntOpenHashMap col = new Long2IntOpenHashMap();
        LongOpenHashSet corners = new LongOpenHashSet();
        for (long k : top.keySet()) {
            int x = colX(k), z = colZ(k);
            for (int dz = 0; dz <= 1; dz++) for (int dx = 0; dx <= 1; dx++) corners.add(col(x + dx, z + dz));
        }
        for (long k : corners) {
            int x = colX(k), z = colZ(k);
            float sum = 0;
            int n = 0;
            int r = 0, g = 0, b = 0;
            for (int dz = -1; dz <= 0; dz++)
                for (int dx = -1; dx <= 0; dx++) {
                    long ck = col(x + dx, z + dz);
                    if (!top.containsKey(ck)) continue;
                    sum += top.get(ck);
                    int cc = colour.get(ck);
                    r += cc >> 16 & 255;
                    g += cc >> 8 & 255;
                    b += cc & 255;
                    n++;
                }
            float hk = sum / n + (n < 4 ? (down ? rimDrop : -rimDrop) : 0);
            if (n < 4 && rimTo != null) {
                // the rim of a mat comes down to where its blocks begin (it covers them down to the rock)
                float low = Float.MAX_VALUE;
                for (int dz = -1; dz <= 0; dz++)
                    for (int dx = -1; dx <= 0; dx++) if (rimTo.containsKey(col(x + dx, z + dz))) low = Math.min(low, rimTo.get(col(x + dx, z + dz)));
                if (low != Float.MAX_VALUE) hk = Math.min(hk, low);
            }
            h.put(k, hk);
            col.put(k, 0xFF000000 | (r / n) << 16 | (g / n) << 8 | b / n);
        }
        // one smoothing pass over the corners inside the skin (the steps of the blocks become a gentle surface)
        Long2FloatOpenHashMap hs = new Long2FloatOpenHashMap(h);
        for (long k : corners) {
            int x = colX(k), z = colZ(k);
            long[] nb = {col(x - 1, z), col(x + 1, z), col(x, z - 1), col(x, z + 1)};
            float sum = 0;
            int n = 0;
            for (long o : nb) {
                if (!h.containsKey(o)) continue;
                sum += h.get(o);
                n++;
            }
            if (n == 4 && interior(top, x, z)) hs.put(k, h.get(k) * 0.5F + sum / n * 0.5F);
        }
        h = hs;
        Long2ObjectOpenHashMap<Vector3f> normal = new Long2ObjectOpenHashMap<>();
        for (long k : corners) {
            int x = colX(k), z = colZ(k);
            float hx0 = h.getOrDefault(col(x - 1, z), h.get(k)), hx1 = h.getOrDefault(col(x + 1, z), h.get(k));
            float hz0 = h.getOrDefault(col(x, z - 1), h.get(k)), hz1 = h.getOrDefault(col(x, z + 1), h.get(k));
            Vector3f nv = new Vector3f(-(hx1 - hx0) * 0.5F, 1, -(hz1 - hz0) * 0.5F).normalize();
            if (down) nv.negate();
            normal.put(k, nv);
        }
        for (long k : top.keySet()) {
            int x = colX(k), z = colZ(k);
            use(x, lightY.get(k), z, PlantPart.LEAF);
            long k00 = col(x, z), k10 = col(x + 1, z), k11 = col(x + 1, z + 1), k01 = col(x, z + 1);
            float u0 = Math.floorMod(x, tile) / (float) tile, v0 = Math.floorMod(z, tile) / (float) tile, du = 1F / tile;
            quad4(tex, new Vector3f(x, h.get(k00), z), new Vector3f(x + 1, h.get(k10), z), new Vector3f(x + 1, h.get(k11), z + 1),
                    new Vector3f(x, h.get(k01), z + 1), u0, v0, u0 + du, v0, u0 + du, v0 + du, u0, v0 + du,
                    normal.get(k00), normal.get(k10), normal.get(k11), normal.get(k01), col.get(k00), col.get(k10), col.get(k11), col.get(k01));
        }
        return h;
    }

    /** The height of a skin ({@link #heightfield}'s corners) at a point (bilinear between the corners of its column). */
    static float surface(Long2FloatOpenHashMap h, float x, float z, float fallback) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        float fx = x - x0, fz = z - z0;
        long a = col(x0, z0), b = col(x0 + 1, z0), c = col(x0 + 1, z0 + 1), d = col(x0, z0 + 1);
        if (!h.containsKey(a) || !h.containsKey(b) || !h.containsKey(c) || !h.containsKey(d)) return fallback;
        return (h.get(a) * (1 - fx) + h.get(b) * fx) * (1 - fz) + (h.get(d) * (1 - fx) + h.get(c) * fx) * fz;
    }

    /** All four columns around the corner (x, z) belong to the skin. */
    private static boolean interior(Long2FloatOpenHashMap top, int x, int z) {
        return top.containsKey(col(x - 1, z - 1)) && top.containsKey(col(x, z - 1)) && top.containsKey(col(x - 1, z)) && top.containsKey(col(x, z));
    }

    static long col(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    static int colX(long k) {
        return (int) (k >> 32);
    }

    static int colZ(long k) {
        return (int) k;
    }

    static int lerpColor(int a, int b, float t) {
        int r = Math.round((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t);
        int g = Math.round((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t);
        int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    /** Smooths the steps of the blocks into a gentle curve (the ends stay). */
    static List<Vector3f> smooth(List<Vector3f> pts, int iterations) {
        for (int it = 0; it < iterations; it++) {
            if (pts.size() < 3) return pts;
            List<Vector3f> sm = new ArrayList<>(pts.size());
            sm.add(pts.getFirst());
            for (int k = 1; k < pts.size() - 1; k++) {
                sm.add(new Vector3f(pts.get(k - 1)).add(pts.get(k)).add(pts.get(k)).add(pts.get(k + 1)).mul(0.25F));
            }
            sm.add(pts.getLast());
            pts = sm;
        }
        return pts;
    }

    // ------------------------------------------------------------------ stem plants (Rotala, Ludwigia, Alternanthera)

    /**
     * M21.1: the shoots as chains of stem nodes ({@link PlantSkeleton#stems}): a stem from every node to its parent (the
     * floor for the lowest ones), a bud of young leaves on every tip, and every leaf block a pair of leaves from the node
     * it hangs on, fanned out towards the block. M22.1: the leaf ({@code tex}, length, width, fan angle) per species.
     */
    void stems(PlantTex tex, float leafLen, float leafWidth, float fan) {
        stems(tex, leafLen, leafWidth, fan, 1);
    }

    /**
     * M28: {@code perSide} leaves from the node towards every leaf block (2 or 3: the whorls of the fine-leaved stem
     * plants, spread over twice the fan angle).
     */
    void stems(PlantTex tex, float leafLen, float leafWidth, float fan, int perSide) {
        PlantSkeleton.Stems sk = PlantSkeleton.stems(parts());
        LongOpenHashSet tips = sk.tips();
        float r = switch (sp) {
            case ROTALA, ROTALA_HRA, ROTALA_WALLICHII, LIMNOPHILA_SESSILIFLORA, MYRIOPHYLLUM_RED -> 0.075F;
            case BACOPA_CAROLINIANA -> 0.11F;
            default -> 0.09F;
        } * style.stem();
        for (long k : sk.order) {
            int x = PlantSkeleton.x(k), y = PlantSkeleton.y(k), z = PlantSkeleton.z(k);
            use(x, y, z, PlantPart.STEM);
            long p = sk.parent.get(k);
            Vector3f from = p == PlantSkeleton.FLOOR ? new Vector3f(x + 0.5F, Math.max(0, y), z + 0.5F) : c(p);
            tube(from, c(k), p == PlantSkeleton.FLOOR ? r * 1.07F : r);
            if (tips.contains(k)) {
                // the growing tip: a little bud of young leaves, on in the direction the shoot grows
                use(x, y, z, PlantPart.TOP);
                Vector3f dir = new Vector3f(c(k)).sub(from);
                if (dir.lengthSquared() < 1e-4F || p == PlantSkeleton.FLOOR) dir.set(0, 1, 0);
                dir.normalize();
                Vector3f tip = c(k).add(new Vector3f(dir).mul(0.3F));
                tube(c(k), tip, r * 0.8F);
                for (int i = 0; i < 3; i++) {
                    double a = i * Math.PI * 2 / 3 + (x * 7 + z * 13) * 0.3;
                    Vector3f d = new Vector3f((float) Math.cos(a), 0, (float) Math.sin(a)).add(new Vector3f(dir).mul(1.6F));
                    leaf(tex, tip, d, new Vector3f((float) -Math.sin(a), 0, (float) Math.cos(a)), 0.5F * leafLen * style.leafLong(),
                            0.36F * leafWidth / 0.68F * style.leafWide(), 0.08F);
                }
            }
        }
        for (var e : sk.leafAt.long2LongEntrySet()) {
            long k = e.getLongKey(), n = e.getLongValue();
            int x = PlantSkeleton.x(k), y = PlantSkeleton.y(k), z = PlantSkeleton.z(k);
            PlantPart p = part(x, y, z);
            if (p == null) continue;
            boolean top = p == PlantPart.TOP;
            use(x, y, z, top ? PlantPart.TOP : PlantPart.LEAF);
            int dx = x - PlantSkeleton.x(n), dy = y - PlantSkeleton.y(n), dz = z - PlantSkeleton.z(n);
            double a = dx == 0 && dz == 0 ? (x * 5 + z * 11 + y * 3) * 0.7 : Math.atan2(dz, dx);
            // a pair of leaves from the node of the stem, fanned out towards this block
            Vector3f node = c(n).add((float) Math.cos(a) * 0.06F, 0, (float) Math.sin(a) * 0.06F);
            for (int j = 0; j < 2 * perSide; j++) {
                // a pair (perSide 1: a - fan, a + fan), or a whorl from a - 2 fan to a + 2 fan
                double b = perSide == 1 ? a + (j == 0 ? -fan : fan) : a + fan * 2 * (j / (2.0 * perSide - 1) * 2 - 1);
                float rise = (top ? 0.8F : 0.38F) + dy * 0.7F;
                Vector3f d = new Vector3f((float) Math.cos(b), rise, (float) Math.sin(b));
                leaf(tex, node, d, new Vector3f((float) -Math.sin(b), 0, (float) Math.cos(b)), (top ? 0.78F : 1.0F) * leafLen * style.leafLong(),
                        (top ? 0.55F / 0.68F : 1F) * leafWidth * style.leafWide(), 0.1F);
            }
        }
        m.swayPerBlock = 0.03;
        m.swayMax = 0.9;
    }

    // ------------------------------------------------------------------ Vallisneria

    /**
     * M21.1: every ribbon ({@link PlantSkeleton#ribbons}) as one strip from its base on the floor (or the ribbon it
     * branches off) through its blocks up to the surface, round the bend and along the surface to a rounded tip; the
     * steps of the blocks smoothed into a curve, the strip turning from upright (slightly twisted) to flat on the water
     * without a gap.
     */
    void ribbons() {
        int lowest = Integer.MAX_VALUE;
        for (long k : blocks.keySet()) lowest = Math.min(lowest, BlockPos.getY(k));
        List<PlantSkeleton.Ribbon> ribbons = PlantSkeleton.ribbons(parts());
        for (PlantSkeleton.Ribbon rb : ribbons) {
            // a branch of one or two blocks lies against the ribbon it branches off (blocks of crossing ribbons the walk
            // could not tell apart): drawn, it would only be a little flap on that ribbon
            if (rb.from() != PlantSkeleton.FLOOR && rb.cells().size() <= 2 && !carries(rb, ribbons)) continue;
            List<Vector3f> pts = new ArrayList<>();
            List<int[]> cells = new ArrayList<>();
            long first = rb.cells().getLong(0);
            int fx = PlantSkeleton.x(first), fy = PlantSkeleton.y(first), fz = PlantSkeleton.z(first);
            Vector3f start;
            if (rb.from() != PlantSkeleton.FLOOR) {
                start = c(rb.from());
            } else if (fy > lowest) {
                // M22.1: the blocks below were taken (broken out): the ribbon grows from the plant block below it
                start = new Vector3f(fx + 0.5F, fy, fz + 0.5F);
                long below = belowOf(fx, fy, fz);
                if (below != Long.MIN_VALUE) start = c(BlockPos.getX(below), BlockPos.getY(below), BlockPos.getZ(below));
            } else {
                start = new Vector3f(fx + 0.5F, fy, fz + 0.5F);
            }
            pts.add(start);
            cells.add(new int[] {fx, fy, fz});
            for (long k : rb.cells()) {
                pts.add(c(k));
                cells.add(new int[] {PlantSkeleton.x(k), PlantSkeleton.y(k), PlantSkeleton.z(k)});
            }
            // a rounded tip half a block further
            Vector3f last = pts.getLast(), prev = pts.get(pts.size() - 2);
            Vector3f ext = new Vector3f(last).sub(prev);
            if (ext.lengthSquared() < 1e-6F) ext.set(0, 1, 0);
            pts.add(new Vector3f(last).add(ext.normalize().mul(0.45F)));
            cells.add(cells.getLast());
            pts = smooth(pts, 3);
            float yaw = (float) Math.floorMod(fx * 73 + fz * 151, 360) * (float) Math.PI / 180F;
            strip(pts, cells, yaw, 0.34F * style.ribbon());
        }
        m.swayPerBlock = 0.05;
        m.swayMax = 1.6;
    }

    /** Another ribbon branches off this one (then it is drawn however short it is, M22.1). */
    private static boolean carries(PlantSkeleton.Ribbon rb, List<PlantSkeleton.Ribbon> all) {
        for (PlantSkeleton.Ribbon o : all) if (o != rb && o.from() != PlantSkeleton.FLOOR && rb.cells().contains(o.from())) return true;
        return false;
    }

    /** A block of the plant below (x, y, z): straight below, else diagonally, else beside and lower; or Long.MIN_VALUE. */
    private long belowOf(int x, int y, int z) {
        for (int dy = 1; dy <= 2; dy++) {
            if (has(x, y - dy, z)) return BlockPos.asLong(x, y - dy, z);
            for (int dz = -dy; dz <= dy; dz++)
                for (int dx = -dy; dx <= dy; dx++) if (has(x + dx, y - dy, z + dz)) return BlockPos.asLong(x + dx, y - dy, z + dz);
        }
        for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) if ((dx != 0 || dz != 0) && has(x + dx, y, z + dz)) return BlockPos.asLong(x + dx, y, z + dz);
        return Long.MIN_VALUE;
    }

    /**
     * A ribbon strip along the points: the half width across is worked out per point (upright parts turn slowly with
     * {@code yaw}, parts along the surface lie flat), so neighbouring pieces share their edges.
     */
    void strip(List<Vector3f> pts, List<int[]> cells, float yaw, float w) {
        int n = pts.size();
        Vector3f[] side = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            Vector3f t = new Vector3f(pts.get(Math.min(n - 1, i + 1))).sub(pts.get(Math.max(0, i - 1)));
            if (t.lengthSquared() < 1e-8F) t.set(0, 1, 0);
            t.normalize();
            float ya = yaw + 0.08F * i;
            Vector3f sv = new Vector3f((float) Math.cos(ya), 0, (float) Math.sin(ya));
            Vector3f sh = new Vector3f(t).cross(0, 1, 0);
            Vector3f s;
            if (sh.lengthSquared() < 1e-4F) {
                s = new Vector3f(sv);
            } else {
                sh.normalize();
                if (sh.dot(sv) < 0) sh.negate();
                float up = Math.clamp((Math.abs(t.y) - 0.3F) / 0.5F, 0F, 1F);
                s = new Vector3f(sv).mul(up).add(new Vector3f(sh).mul(1 - up));
            }
            s.sub(new Vector3f(t).mul(s.dot(t)));
            if (s.lengthSquared() < 1e-8F) s = new Vector3f(sh.lengthSquared() > 1e-4F ? sh : sv);
            s.normalize();
            if (i > 0 && s.dot(side[i - 1]) < 0) s.negate();
            side[i] = s;
        }
        float v = 0;
        for (int i = 1; i < n; i++) {
            Vector3f a = pts.get(i - 1), b = pts.get(i);
            Vector3f seg = new Vector3f(b).sub(a);
            float len = seg.length();
            if (len < 1e-4F) continue;
            int[] cell = cells.get(i);
            PlantPart p = part(cell[0], cell[1], cell[2]);
            use(cell[0], cell[1], cell[2], p == PlantPart.TOP ? PlantPart.TOP : PlantPart.RIBBON);
            // narrower towards the rounded tip
            Vector3f s0 = new Vector3f(side[i - 1]).mul(w), s1 = new Vector3f(side[i]).mul(i == n - 1 ? w * 0.55F : w);
            Vector3f nrm = new Vector3f(s0).cross(seg).normalize();
            if (nrm.y < 0) nrm.negate();
            float v1 = v + len * 0.5F;
            quad(PlantTex.RIBBON, new Vector3f(a).sub(s0), new Vector3f(a).add(s0), new Vector3f(b).add(s1), new Vector3f(b).sub(s1), 0, v1, 1, v, nrm);
            v = v1;
        }
    }

    // ------------------------------------------------------------------ Anubias

    void anubias() {
        anubias(PlantTex.OVATE, 1F);
    }

    /** The leaf texture and width of an Anubias blade (M28: the other Anubias). */
    private PlantTex anubiasTex = PlantTex.OVATE;
    private float anubiasWide = 1F;

    void anubias(PlantTex tex, float wide) {
        anubiasTex = tex;
        anubiasWide = wide;
        List<Long> leafBlocks = new ArrayList<>();
        List<Long> stems = new ArrayList<>();
        rhizome(0.26F);
        for (var e : blocks.long2ObjectEntrySet()) {
            long k = e.getLongKey();
            PlantPart p = e.getValue();
            switch (p) {
                case STEM -> stems.add(k);
                case LEAF, TOP -> leafBlocks.add(k);
                default -> { }
            }
        }
        // leaf bases: stalk blocks next to a leaf block; every leaf block belongs to the nearest base
        List<Long> leafBases = new ArrayList<>();
        for (long k : stems) {
            int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
            boolean touches = false;
            for (int dy = -1; dy <= 1 && !touches; dy++)
                for (int dz = -1; dz <= 1 && !touches; dz++)
                    for (int dx = -1; dx <= 1 && !touches; dx++) {
                        PlantPart q = part(x + dx, y + dy, z + dz);
                        touches = q == PlantPart.LEAF || q == PlantPart.TOP;
                    }
            if (touches) leafBases.add(k);
        }
        // bases right next to each other are one leaf
        List<Long> merged = new ArrayList<>();
        for (long k : leafBases) {
            boolean near = false;
            for (long o : merged) near |= Math.abs(BlockPos.getX(o) - BlockPos.getX(k)) <= 1 && Math.abs(BlockPos.getY(o) - BlockPos.getY(k)) <= 1
                    && Math.abs(BlockPos.getZ(o) - BlockPos.getZ(k)) <= 1;
            if (!near) merged.add(k);
        }
        if (merged.isEmpty() && !leafBlocks.isEmpty()) merged.add(BlockPos.asLong(0, 0, 0));
        List<List<Long>> blades = new ArrayList<>();
        for (int n = 0; n < merged.size(); n++) blades.add(new ArrayList<>());
        for (long k : leafBlocks) {
            Vector3f p = c(BlockPos.getX(k), BlockPos.getY(k), BlockPos.getZ(k));
            int best = 0;
            double bd = Double.MAX_VALUE;
            for (int n = 0; n < merged.size(); n++) {
                long b = merged.get(n);
                double d = p.distanceSquared(c(BlockPos.getX(b), BlockPos.getY(b), BlockPos.getZ(b)));
                if (d < bd) {
                    bd = d;
                    best = n;
                }
            }
            blades.get(best).add(k);
        }
        for (int n = 0; n < merged.size(); n++) {
            if (blades.get(n).isEmpty()) continue;
            long b = merged.get(n);
            int bx = BlockPos.getX(b), by = BlockPos.getY(b), bz = BlockPos.getZ(b);
            Vector3f base = c(bx, by, bz);
            Vector3f from = nearestRhizome(base);
            use(bx, by, bz, PlantPart.STEM);
            Vector3f mid = new Vector3f(from).add(base).mul(0.5F).add(0, 0.25F, 0);
            tube(from, mid, 0.07F * style.stem());
            tube(mid, base, 0.065F * style.stem());
            anubiasBlade(blades.get(n), base);
        }
        m.swayPerBlock = 0.006;
        m.swayMax = 0.08;
    }

    /** The rhizome of an epiphyte: a thick tube through its ROOT and RHIZOME blocks. */
    void rhizome(float r) {
        for (var e : blocks.long2ObjectEntrySet()) {
            long k = e.getLongKey();
            int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
            PlantPart p = e.getValue();
            if (p != PlantPart.ROOT && p != PlantPart.RHIZOME) continue;
            use(x, y, z, PlantPart.RHIZOME);
            Vector3f a = new Vector3f(x + 0.5F, y + 0.32F, z + 0.5F);
            boolean linked = false;
            for (int[] d : new int[][] {{1, 0}, {0, 1}, {-1, 0}, {0, -1}}) {
                PlantPart q = part(x + d[0], y, z + d[1]);
                if (q == PlantPart.RHIZOME || q == PlantPart.ROOT) {
                    linked = true;
                    if (d[0] + d[1] > 0) tube(a, new Vector3f(x + d[0] + 0.5F, y + 0.32F, z + d[1] + 0.5F), r);
                }
            }
            if (!linked) tube(new Vector3f(a).sub(0.3F, 0, 0), new Vector3f(a).add(0.3F, 0, 0), r);
        }
    }

    private Vector3f nearestRhizome(Vector3f to) {
        Vector3f from = new Vector3f(0.5F, 0.4F, 0.5F);
        double fd = Double.MAX_VALUE;
        for (var e : blocks.long2ObjectEntrySet()) {
            PlantPart q = e.getValue();
            if (q != PlantPart.RHIZOME && q != PlantPart.ROOT) continue;
            long k = e.getLongKey();
            Vector3f r = new Vector3f(BlockPos.getX(k) + 0.5F, BlockPos.getY(k) + 0.4F, BlockPos.getZ(k) + 0.5F);
            if (r.distanceSquared(to) < fd) {
                fd = r.distanceSquared(to);
                from = r;
            }
        }
        return from;
    }

    private void anubiasBlade(List<Long> patch, Vector3f b) {
        Vector3f tip = new Vector3f(b);
        long tipKey = patch.getFirst();
        for (long k : patch) {
            Vector3f p = c(BlockPos.getX(k), BlockPos.getY(k), BlockPos.getZ(k));
            if (p.distanceSquared(b) > tip.distanceSquared(b)) {
                tip = p;
                tipKey = k;
            }
        }
        Vector3f dir = new Vector3f(tip).sub(b);
        if (dir.lengthSquared() < 0.01F) dir.set(0, 1, 0.3F);
        float len = dir.length() + 1.0F;
        dir.normalize();
        Vector3f across = Math.abs(dir.y) > 0.95F ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0).cross(dir).normalize();
        float spread = 0;
        for (long k : patch) {
            Vector3f p = c(BlockPos.getX(k), BlockPos.getY(k), BlockPos.getZ(k)).sub(b);
            spread = Math.max(spread, Math.abs(p.dot(across)));
        }
        float width = Math.max(1.2F, Math.min(3.4F, spread * 2 + 1.4F)) * anubiasWide;
        PlantPart p = part(BlockPos.getX(tipKey), BlockPos.getY(tipKey), BlockPos.getZ(tipKey));
        use(BlockPos.getX(tipKey), BlockPos.getY(tipKey), BlockPos.getZ(tipKey), p == null ? PlantPart.LEAF : p);
        // M28: the coloured "Pinto" texture carries its own green and white, the vertex colour only shades it
        if (anubiasTex == PlantTex.PINTO) curColor = 0xFF000000 | saturate(p == PlantPart.TOP ? 0xFFFFF8 : 0xF0F2EA, style.saturate());
        leaf(anubiasTex, b, dir, across, len * style.leafLong(), width * Math.min(1.15F, style.leafWide()), 0.1F);
    }

    // ------------------------------------------------------------------ M22.1 leaves from a crown or a rhizome

    /**
     * Leaves as curved blades: every leaf tip of the tree from {@code source} ({@link PlantSkeleton#leafTips}) is one
     * leaf along the shortest way from the source; the first {@code stalk} of it is the petiole, the rest the blade.
     * The width follows the leaf blocks next to its way (the widest spread), between {@code minW} and {@code maxW}.
     */
    void leaves(Long2ObjectOpenHashMap<PlantPart> parts, PlantSkeleton.Tree tree, LongOpenHashSet skip, PlantTex tex, float stalk,
                float minW, float maxW, float stalkR, Vector3f sourceAt) {
        LongArrayList tips = PlantSkeleton.leafTips(tree, 2, Math.max(1.6, maxW * 0.75));
        List<LongArrayList> paths = new ArrayList<>();
        for (long t : tips) {
            if (skip.contains(t)) continue;
            LongArrayList path = tree.path(t);
            if (path.size() >= 2) paths.add(path);
        }
        // the spread of the leaf blocks around each way: the widest is the blade's width
        float[] spread = new float[paths.size()];
        for (long k : parts.keySet()) {
            PlantPart p = parts.get(k);
            if (p != PlantPart.LEAF && p != PlantPart.TOP || skip.contains(k)) continue;
            Vector3f pc = c(k);
            int best = -1;
            float bd = Float.MAX_VALUE;
            for (int i = 0; i < paths.size(); i++) {
                for (long q : paths.get(i)) {
                    float d = pc.distanceSquared(c(q));
                    if (d < bd) {
                        bd = d;
                        best = i;
                    }
                }
            }
            if (best >= 0) spread[best] = Math.max(spread[best], (float) Math.sqrt(bd));
        }
        for (int i = 0; i < paths.size(); i++) {
            LongArrayList path = paths.get(i);
            List<Vector3f> pts = new ArrayList<>();
            long src = path.getLong(0);
            pts.add(sourceAt != null && PlantSkeleton.x(src) == 0 && PlantSkeleton.y(src) == 0 && PlantSkeleton.z(src) == 0
                    ? new Vector3f(sourceAt) : c(src).sub(0, 0.18F, 0));
            for (int j = 1; j < path.size(); j++) pts.add(c(path.getLong(j)));
            Vector3f last = pts.getLast(), prev = pts.get(pts.size() - 2);
            Vector3f ext = new Vector3f(last).sub(prev);
            if (ext.lengthSquared() > 1e-6F) pts.add(new Vector3f(last).add(ext.normalize().mul(0.5F)));
            pts = smooth(pts, 3);
            float total = 0;
            for (int j = 1; j < pts.size(); j++) total += pts.get(j).distance(pts.get(j - 1));
            // the petiole up to the blade
            float cut = total * stalk;
            List<Vector3f> stalkPts = new ArrayList<>(), bladePts = new ArrayList<>();
            float at = 0;
            stalkPts.add(pts.getFirst());
            for (int j = 1; j < pts.size(); j++) {
                Vector3f a = pts.get(j - 1), b = pts.get(j);
                float seg = a.distance(b);
                if (at + seg <= cut) {
                    stalkPts.add(b);
                } else if (at >= cut) {
                    bladePts.add(b);
                } else {
                    Vector3f mid = new Vector3f(a).lerp(b, (cut - at) / seg);
                    stalkPts.add(mid);
                    bladePts.add(new Vector3f(mid));
                    bladePts.add(b);
                }
                at += seg;
            }
            long tipKey = path.getLong(path.size() - 1);
            long baseKey = path.getLong(Math.min(path.size() - 1, Math.max(1, Math.round(path.size() * stalk))));
            if (stalkPts.size() >= 2 && stalk > 0) {
                use(baseKey, PlantPart.STEM);
                tubes(PlantTex.STEM, stalkPts, stalkR * style.stem(), stalkR * 0.8F * style.stem(), 4);
            }
            if (bladePts.size() < 2) bladePts.addFirst(new Vector3f(stalkPts.getLast()));
            PlantPart tp = part(tipKey);
            use(tipKey, tp == PlantPart.TOP ? PlantPart.TOP : PlantPart.LEAF);
            float w = Math.clamp(spread[i] * 2 + 1F, minW, maxW) * style.leafWide();
            float twist = (hash(tipKey, 7) - 0.5F) * 0.8F;
            // longer outside a little (the blade's tip extends along the curve)
            if (style.leafLong() != 1F && bladePts.size() >= 2) {
                Vector3f l2 = bladePts.getLast(), p2 = bladePts.get(bladePts.size() - 2);
                Vector3f e2 = new Vector3f(l2).sub(p2);
                if (e2.lengthSquared() > 1e-6F) bladePts.add(new Vector3f(l2).add(e2.normalize().mul(total * (1 - stalk) * (style.leafLong() - 1))));
            }
            blade(tex, bladePts, w, twist, hash(tipKey, 3) * 6.28F);
        }
    }

    /** Java fern and Bucephalandra: the rhizome, the leaves from it. */
    void rhizomeLeaves(PlantTex tex, float stalk, float minW, float maxW, float rhizomeR, double sway, double swayMax) {
        rhizome(rhizomeR);
        Long2ObjectOpenHashMap<PlantPart> parts = parts();
        PlantSkeleton.Tree tree = PlantSkeleton.tree(parts, k -> {
            PlantPart p = parts.get(k);
            return p == PlantPart.ROOT || p == PlantPart.RHIZOME;
        }, k -> true);
        leaves(parts, tree, new LongOpenHashSet(), tex, stalk, minW, maxW, 0.07F, null);
        m.swayPerBlock = sway;
        m.swayMax = swayMax;
    }

    /** Cryptocorynes and the Amazon sword: every leaf from the crown. */
    void rosette(PlantTex tex, float stalk, float minW, float maxW, double sway, double swayMax) {
        Long2ObjectOpenHashMap<PlantPart> parts = parts();
        long root = PlantSkeleton.key(0, 0, 0);
        PlantSkeleton.Tree tree = PlantSkeleton.tree(parts, k -> k == root, k -> true);
        // the crown: a short, thick base
        use(0, 0, 0, PlantPart.STEM);
        tube(PlantTex.STEM, new Vector3f(0.5F, 0F, 0.5F), new Vector3f(0.5F, 0.45F, 0.5F), 0.32F, 0.22F, 6);
        leaves(parts, tree, new LongOpenHashSet(), tex, stalk, minW, maxW, sp == PlantSpecies.AMAZON_SWORD ? 0.11F : 0.08F,
                new Vector3f(0.5F, 0.3F, 0.5F));
        m.swayPerBlock = sway;
        m.swayMax = swayMax;
    }

    // ------------------------------------------------------------------ moss, carpet

    /** Java moss: a few crossed sprigs on every block, turned and tilted a little each. */
    void moss() {
        for (var e : blocks.long2ObjectEntrySet()) {
            long k = e.getLongKey();
            int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
            use(x, y, z, PlantPart.MOSS);
            float yaw = hash(x, y, z, 1) * 6.28F;
            Vector3f at = new Vector3f(x + 0.3F + hash(x, y, z, 2) * 0.4F, y + 0.55F, z + 0.3F + hash(x, y, z, 3) * 0.4F);
            cross(PlantTex.MOSS, at, (1.5F + hash(x, y, z, 4) * 0.5F) * style.leafWide(), yaw, true);
        }
        m.swayPerBlock = 0.012;
        m.swayMax = 0.2;
    }

    /** Monte Carlo: tiny round leaves on short stalks over every block of the carpet, thin runners between them. */
    void carpet() {
        for (var e : blocks.long2ObjectEntrySet()) {
            long k = e.getLongKey();
            int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
            use(x, y, z, PlantPart.MAT);
            for (int[] d : new int[][] {{1, 0}, {0, 1}}) {
                if (has(x + d[0], y, z + d[1])) {
                    use(x, y, z, PlantPart.STEM);
                    tube(new Vector3f(x + 0.5F, y + 0.08F, z + 0.5F), new Vector3f(x + d[0] + 0.5F, y + 0.08F, z + d[1] + 0.5F), 0.04F * style.stem());
                }
            }
            use(x, y, z, PlantPart.MAT);
            int n = 3;
            for (int i = 0; i < n; i++) {
                float a = (hash(x, y, z, 10 + i) + i / (float) n) * 6.28F;
                Vector3f base = new Vector3f(x + 0.5F, y + 0.08F, z + 0.5F);
                Vector3f dir = new Vector3f((float) Math.cos(a), 0.35F + hash(x, y, z, 20 + i) * 0.3F, (float) Math.sin(a));
                Vector3f stalkTop = new Vector3f(base).add(new Vector3f(dir).normalize().mul(0.25F));
                tube(base, stalkTop, 0.03F * style.stem());
                leaf(PlantTex.ROUND, stalkTop, new Vector3f(dir.x, dir.y * 0.4F, dir.z), new Vector3f((float) -Math.sin(a), 0, (float) Math.cos(a)),
                        0.62F * style.leafLong(), 0.55F * style.leafWide(), 0.08F);
            }
        }
        m.swayPerBlock = 0.02;
        m.swayMax = 0.1;
    }

    // ------------------------------------------------------------------ M28 part b: carpets

    /** Thin runners from a block of a carpet to its neighbours (same layer) and down to the block below it. */
    private void runners(int x, int y, int z) {
        for (int[] d : new int[][] {{1, 0}, {0, 1}}) {
            if (has(x + d[0], y, z + d[1])) {
                use(x, y, z, PlantPart.STEM);
                tube(new Vector3f(x + 0.5F, y + 0.08F, z + 0.5F), new Vector3f(x + d[0] + 0.5F, y + 0.08F, z + d[1] + 0.5F), 0.04F * style.stem());
            }
        }
        if (y > 0) {
            // down to the block below (or to the floor, when it is gone)
            use(x, y, z, PlantPart.STEM);
            tube(new Vector3f(x + 0.5F, y - 0.92F, z + 0.5F), new Vector3f(x + 0.5F, y + 0.08F, z + 0.5F), 0.04F * style.stem());
        }
    }

    /** Hemianthus "Cuba": the tiniest round leaves, four to a block, on the carpet and its cushion layer. */
    void tinyCarpet() {
        for (var e : blocks.long2ObjectEntrySet()) {
            long k = e.getLongKey();
            int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
            runners(x, y, z);
            use(x, y, z, PlantPart.MAT);
            for (int i = 0; i < 4; i++) {
                float a = (hash(x, y, z, 10 + i) + i / 4F) * 6.28F;
                Vector3f base = new Vector3f(x + 0.5F, y + 0.08F, z + 0.5F);
                Vector3f dir = new Vector3f((float) Math.cos(a), 0.5F + hash(x, y, z, 20 + i) * 0.4F, (float) Math.sin(a));
                Vector3f top = new Vector3f(base).add(new Vector3f(dir).normalize().mul(0.18F));
                tube(base, top, 0.025F * style.stem());
                leaf(PlantTex.ROUND, top, new Vector3f(dir.x, dir.y * 0.5F, dir.z), new Vector3f((float) -Math.sin(a), 0, (float) Math.cos(a)),
                        0.4F * style.leafLong(), 0.34F * style.leafWide(), 0.06F);
            }
        }
        m.swayPerBlock = 0.02;
        m.swayMax = 0.08;
    }

    /** Eleocharis: runners over the floor, from them tufts of thin upright needles (a few short ones on every block). */
    void hairgrass() {
        Long2IntOpenHashMap topY = new Long2IntOpenHashMap();
        for (var e : blocks.long2ObjectEntrySet()) {
            long k = e.getLongKey();
            int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
            long ck = col(x, z);
            if (!topY.containsKey(ck) || y > topY.get(ck)) topY.put(ck, y);
            if (y == 0) runners(x, y, z);
        }
        for (long ck : topY.keySet()) {
            int x = colX(ck), z = colZ(ck), top = topY.get(ck);
            int n = top == 0 ? 2 : 5;
            for (int i = 0; i < n; i++) {
                float a = (hash(x, 0, z, 30 + i) + i / (float) n) * 6.28F;
                float lean = 0.08F + hash(x, 0, z, 40 + i) * 0.22F;
                float h = top == 0 ? 0.45F + hash(x, 0, z, 50 + i) * 0.4F : top + 0.5F + hash(x, 0, z, 50 + i) * 0.8F;
                Vector3f base = new Vector3f(x + 0.5F + (float) Math.cos(a) * 0.08F, 0.04F, z + 0.5F + (float) Math.sin(a) * 0.08F);
                List<Vector3f> pts = new ArrayList<>();
                for (int j = 0; j <= 4; j++) {
                    float t = j / 4F;
                    float out = lean * t * t * Math.max(1, h * 0.4F);
                    pts.add(new Vector3f(base.x + (float) Math.cos(a) * out, base.y + h * t, base.z + (float) Math.sin(a) * out));
                }
                use(x, Math.min(top, Math.round(h * 0.7F)), z, top == 0 ? PlantPart.MAT : PlantPart.LEAF);
                blade(PlantTex.NEEDLE, pts, 0.3F * style.leafWide(), (hash(x, 0, z, 60 + i) - 0.5F) * 0.6F, a);
            }
        }
        m.swayPerBlock = 0.03;
        m.swayMax = 0.25;
    }

    /** Staurogyne repens: short upright shoots with pairs of lanceolate leaves, low leaves over the rest of the carpet. */
    void staurogyne() {
        Long2IntOpenHashMap topY = new Long2IntOpenHashMap();
        for (var e : blocks.long2ObjectEntrySet()) {
            long k = e.getLongKey();
            int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
            long ck = col(x, z);
            if (!topY.containsKey(ck) || y > topY.get(ck)) topY.put(ck, y);
            if (y == 0) runners(x, y, z);
        }
        for (long ck : topY.keySet()) {
            int x = colX(ck), z = colZ(ck), top = topY.get(ck);
            float yaw0 = hash(x, 0, z, 70) * 6.28F;
            Vector3f foot = new Vector3f(x + 0.5F, 0.06F, z + 0.5F);
            Vector3f tip = new Vector3f(x + 0.5F, top + 0.55F, z + 0.5F);
            if (top > 0) {
                use(x, 0, z, PlantPart.STEM);
                tube(PlantTex.STEM, foot, tip, 0.07F * style.stem(), 0.05F * style.stem(), 4);
            }
            // a pair of leaves on every level, crossed with the one below; the top a little crown
            for (int y = 0; y <= top; y++) {
                boolean crown = y == top;
                use(x, y, z, crown && top > 0 ? PlantPart.TOP : PlantPart.LEAF);
                int pairs = crown ? 2 : 1;
                for (int j = 0; j < pairs * 2; j++) {
                    double b = yaw0 + y * Math.PI / 2 + j * Math.PI / pairs + (crown ? 0.3 : 0);
                    Vector3f at = new Vector3f(x + 0.5F, y == 0 ? 0.12F : y + 0.35F, z + 0.5F);
                    Vector3f d = new Vector3f((float) Math.cos(b), crown ? 0.75F : 0.35F, (float) Math.sin(b));
                    leaf(PlantTex.LANCE, at, d, new Vector3f((float) -Math.sin(b), 0, (float) Math.cos(b)), (crown ? 0.7F : 0.85F) * style.leafLong(),
                            0.36F * style.leafWide(), 0.1F);
                }
            }
        }
        m.swayPerBlock = 0.025;
        m.swayMax = 0.15;
    }

    /** Marsilea: runners, on every block a thin stalk with four leaflets like a clover (higher on the raised blocks). */
    void clover() {
        for (var e : blocks.long2ObjectEntrySet()) {
            long k = e.getLongKey();
            int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
            if (y == 0) runners(x, y, z);
            boolean raised = y > 0;
            Vector3f base = new Vector3f(x + 0.5F, raised ? y - 0.92F : 0.08F, z + 0.5F);
            Vector3f top = new Vector3f(x + 0.5F + (hash(x, y, z, 80) - 0.5F) * 0.2F, y + 0.25F + hash(x, y, z, 81) * 0.25F,
                    z + 0.5F + (hash(x, y, z, 82) - 0.5F) * 0.2F);
            use(x, y, z, PlantPart.STEM);
            tube(base, top, 0.03F * style.stem());
            use(x, y, z, raised ? PlantPart.LEAF : PlantPart.MAT);
            float yaw = hash(x, y, z, 83) * 6.28F;
            for (int i = 0; i < 4; i++) {
                double a = yaw + i * Math.PI / 2;
                Vector3f d = new Vector3f((float) Math.cos(a), 0.12F, (float) Math.sin(a));
                leaf(PlantTex.ROUND, top, d, new Vector3f((float) -Math.sin(a), 0, (float) Math.cos(a)), 0.4F * style.leafLong(),
                        0.42F * style.leafWide(), 0.04F);
            }
        }
        m.swayPerBlock = 0.02;
        m.swayMax = 0.1;
    }

    // ------------------------------------------------------------------ M28 part b: mosses

    /** Christmas moss: flat, triangular fronds in tiers (feathery leaves lying almost level) over a cushion of sprigs. */
    void christmasMoss() {
        for (var e : blocks.long2ObjectEntrySet()) {
            long k = e.getLongKey();
            int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
            use(x, y, z, PlantPart.MOSS);
            float yaw = hash(x, y, z, 1) * 6.28F;
            Vector3f at = new Vector3f(x + 0.5F, y + 0.45F, z + 0.5F);
            cross(PlantTex.MOSS, at, 1.2F * style.leafWide(), yaw, false);
            for (int i = 0; i < 2; i++) {
                double a = yaw + i * Math.PI + 0.4;
                Vector3f d = new Vector3f((float) Math.cos(a), -0.08F, (float) Math.sin(a));
                leaf(PlantTex.FEATHER, new Vector3f(at).sub(0, 0.15F, 0), d, new Vector3f((float) -Math.sin(a), 0, (float) Math.cos(a)),
                        1.4F * style.leafLong(), 1.1F * style.leafWide(), 0.05F);
            }
        }
        m.swayPerBlock = 0.012;
        m.swayMax = 0.2;
    }

    /** Flame moss: upright, twisted sprigs like little flames on the top of every column, sprigs below. */
    void flameMoss() {
        Long2IntOpenHashMap topY = new Long2IntOpenHashMap();
        for (var e : blocks.long2ObjectEntrySet()) {
            long k = e.getLongKey();
            int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
            long ck = col(x, z);
            if (!topY.containsKey(ck) || y > topY.get(ck)) topY.put(ck, y);
        }
        for (var e : blocks.long2ObjectEntrySet()) {
            long k = e.getLongKey();
            int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
            use(x, y, z, PlantPart.MOSS);
            float yaw = hash(x, y, z, 1) * 6.28F;
            if (y == topY.get(col(x, z))) {
                // the flame: a twisted blade standing up from the block
                List<Vector3f> pts = new ArrayList<>();
                float lean = (hash(x, y, z, 5) - 0.5F) * 0.5F;
                for (int j = 0; j <= 3; j++) {
                    float t = j / 3F;
                    pts.add(new Vector3f(x + 0.5F + (float) Math.cos(yaw) * lean * t * t, y + 0.1F + 1.5F * t, z + 0.5F + (float) Math.sin(yaw) * lean * t * t));
                }
                blade(PlantTex.MOSS, pts, 0.9F * style.leafWide(), 2.2F, yaw);
            } else {
                cross(PlantTex.MOSS, new Vector3f(x + 0.5F, y + 0.5F, z + 0.5F), 1.3F * style.leafWide(), yaw, false);
            }
        }
        m.swayPerBlock = 0.015;
        m.swayMax = 0.25;
    }

    /** Riccia: a dense cushion of small, forked thalli (flat stars of the Riccia texture). */
    void riccia() {
        for (var e : blocks.long2ObjectEntrySet()) {
            long k = e.getLongKey();
            int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
            use(x, y, z, PlantPart.MOSS);
            float yaw = hash(x, y, z, 1) * 6.28F;
            Vector3f at = new Vector3f(x + 0.3F + hash(x, y, z, 2) * 0.4F, y + 0.5F, z + 0.3F + hash(x, y, z, 3) * 0.4F);
            cross(PlantTex.RICCIA, at, (1.3F + hash(x, y, z, 4) * 0.4F) * style.leafWide(), yaw, true);
        }
        m.swayPerBlock = 0.01;
        m.swayMax = 0.12;
    }

    /** Marimo: a velvety ball (a sphere of the Marimo texture) with a few loose filaments standing off it. */
    void marimo() {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = 0, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (long k : blocks.keySet()) {
            minX = Math.min(minX, BlockPos.getX(k));
            maxX = Math.max(maxX, BlockPos.getX(k));
            maxY = Math.max(maxY, BlockPos.getY(k));
            minZ = Math.min(minZ, BlockPos.getZ(k));
            maxZ = Math.max(maxZ, BlockPos.getZ(k));
        }
        float rad = Math.max(0.45F, Math.max((maxY + 1) / 2F, Math.max(maxX - minX + 1, maxZ - minZ + 1) / 2F) * 0.95F);
        Vector3f c = new Vector3f((minX + maxX + 1) / 2F, rad + 0.02F, (minZ + maxZ + 1) / 2F);
        int lat = 7, lon = 12;
        int color = colorOf(0, 0, 0, PlantPart.MOSS), dark = lerpColor(color, 0xFF000000, 0.25F);
        for (int i = 0; i < lat; i++) {
            double t0 = Math.PI * i / lat, t1 = Math.PI * (i + 1) / lat;
            for (int j = 0; j < lon; j++) {
                double p0 = Math.PI * 2 * j / lon, p1 = Math.PI * 2 * (j + 1) / lon;
                Vector3f na = sphere(t0, p0), nb = sphere(t0, p1), nc = sphere(t1, p1), nd = sphere(t1, p0);
                Vector3f mid = new Vector3f(na).add(nc).normalize();
                Vector3f at = new Vector3f(mid).mul(rad).add(c);
                use(Math.round(at.x - 0.5F), Math.max(0, Math.min(maxY, Math.round(at.y - 0.5F))), Math.round(at.z - 0.5F), PlantPart.MOSS);
                // darker underneath (it lies in its own shadow)
                int ca = lerpColor(dark, color, (na.y + 1) / 2), cc = lerpColor(dark, color, (nc.y + 1) / 2);
                // a third of the (tileable) texture per quad: inside the tile, so the block atlas outside needs no repeat
                float u0 = (j % 3) / 3F, u1 = u0 + 1 / 3F, v0 = (i % 3) / 3F, v1 = v0 + 1 / 3F;
                quad4(PlantTex.MARIMO, new Vector3f(na).mul(rad).add(c), new Vector3f(nb).mul(rad).add(c), new Vector3f(nc).mul(rad).add(c),
                        new Vector3f(nd).mul(rad).add(c), u0, v0, u1, v0, u1, v1, u0, v1, na, nb, nc, nd, ca, ca, cc, cc);
            }
        }
        // loose filaments: small sprigs on the upper half
        for (int i = 0; i < 14; i++) {
            double t = Math.acos(1 - hash(i, 0, 0, 90) * 1.2), p = hash(i, 0, 0, 91) * Math.PI * 2;
            Vector3f n = sphere(t, p);
            Vector3f at = new Vector3f(n).mul(rad * 1.02F).add(c);
            use(Math.round(at.x - 0.5F), Math.max(0, Math.min(maxY, Math.round(at.y - 0.5F))), Math.round(at.z - 0.5F), PlantPart.MOSS);
            cross(PlantTex.MOSS, at, 0.5F * style.leafWide(), (float) p, false);
        }
        m.swayPerBlock = 0;
        m.swayMax = 0;
    }

    private static Vector3f sphere(double theta, double phi) {
        return new Vector3f((float) (Math.sin(theta) * Math.cos(phi)), (float) Math.cos(theta), (float) (Math.sin(theta) * Math.sin(phi)));
    }

    // ------------------------------------------------------------------ M28 part b: Aponogeton

    /**
     * Aponogeton ulvaceus: the tuber, long undulate leaves from it and, when grown, the flower spike on its long stalk:
     * white flowers on a short spike lying at the surface.
     */
    void aponogeton() {
        Long2ObjectOpenHashMap<PlantPart> parts = parts();
        long root = PlantSkeleton.key(0, 0, 0);
        use(0, 0, 0, PlantPart.RHIZOME);
        tube(PlantTex.STEM, new Vector3f(0.5F, 0F, 0.5F), new Vector3f(0.5F, 0.4F, 0.5F), 0.42F, 0.34F, 8);
        tube(PlantTex.STEM, new Vector3f(0.5F, 0.4F, 0.5F), new Vector3f(0.5F, 0.7F, 0.5F), 0.34F, 0.12F, 8);
        PlantSkeleton.Tree toSpike = PlantSkeleton.tree(parts, k -> k == root, k -> {
            PlantPart p = parts.get(k);
            return p == PlantPart.STEM || p == PlantPart.TOP || p == PlantPart.ROOT;
        });
        LongOpenHashSet stalk = new LongOpenHashSet(), spikes = new LongOpenHashSet();
        for (LongArrayList spike : PlantSkeleton.clusters(parts, k -> parts.get(k) == PlantPart.TOP)) {
            if (spike.size() < 2) continue;
            long first = 0;
            int best = Integer.MAX_VALUE;
            for (long k : spike) {
                if (!toSpike.contains(k)) continue;
                int n = toSpike.path(k).size();
                if (n < best) {
                    best = n;
                    first = k;
                }
            }
            // only the spike on its stalk (leaf tips are TOP blocks too, but no stalk leads to them)
            if (best == Integer.MAX_VALUE || best < 4) continue;
            LongArrayList path = toSpike.path(first);
            spikes.addAll(spike);
            List<Vector3f> pts = new ArrayList<>();
            pts.add(new Vector3f(0.5F, 0.6F, 0.5F));
            for (int j = 1; j < path.size(); j++) {
                long k = path.getLong(j);
                if (parts.get(k) != PlantPart.STEM) continue;
                pts.add(c(k));
                stalk.add(k);
            }
            pts.add(c(first));
            pts = smooth(pts, 2);
            use(path.getLong(Math.max(0, path.size() - 2)), PlantPart.STEM);
            tubes(PlantTex.STEM, pts, 0.08F * style.stem(), 0.06F * style.stem(), 4);
            // the spike: along its blocks, a little above the water, white flowers on it
            Vector3f a = c(first), b = a;
            for (long k : spike) if (c(k).distanceSquared(a) > b.distanceSquared(a)) b = c(k);
            a = new Vector3f(a).add(0, 0.45F, 0);
            b = new Vector3f(b).add(0, 0.6F, 0);
            use(first, PlantPart.TOP);
            tube(PlantTex.STEM, c(first), a, 0.06F * style.stem(), 0.06F * style.stem(), 4);
            tube(PlantTex.STEM, a, b, 0.07F * style.stem(), 0.05F * style.stem(), 4);
            curColor = 0xFF000000 | saturate(0xF4F0DC, style.saturate());
            int n = Math.max(3, Math.round(a.distance(b) * 2.5F));
            for (int i = 0; i <= n; i++) {
                Vector3f at = new Vector3f(a).lerp(b, i / (float) n);
                cross(PlantTex.TUFT, at, 0.42F * style.detail(), i * 1.3F, false);
            }
        }
        // the leaves from the tuber (the flower stalk is no leaf)
        LongOpenHashSet skip = new LongOpenHashSet(stalk);
        skip.addAll(spikes);
        PlantSkeleton.Tree tree = PlantSkeleton.tree(parts, k -> k == root, k -> !skip.contains(k));
        leaves(parts, tree, skip, PlantTex.WAVY, 0.12F, 0.9F, 1.7F, 0.05F, new Vector3f(0.5F, 0.55F, 0.5F));
        m.swayPerBlock = 0.02;
        m.swayMax = 0.4;
    }

    // ------------------------------------------------------------------ tiger lotus


    /** Tiger lotus: the bulb, mottled leaves under water, floating leaves on long petioles at the surface. */
    void lotus() {
        Long2ObjectOpenHashMap<PlantPart> parts = parts();
        long root = PlantSkeleton.key(0, 0, 0);
        use(0, 0, 0, PlantPart.RHIZOME);
        tube(PlantTex.STEM, new Vector3f(0.5F, 0F, 0.5F), new Vector3f(0.5F, 0.45F, 0.5F), 0.5F, 0.42F, 8);
        tube(PlantTex.STEM, new Vector3f(0.5F, 0.45F, 0.5F), new Vector3f(0.5F, 0.8F, 0.5F), 0.42F, 0.15F, 8);
        // floating leaves: each group of PAD blocks, its petiole the way from the bulb through the stalk blocks
        PlantSkeleton.Tree toPads = PlantSkeleton.tree(parts, k -> k == root, k -> {
            PlantPart p = parts.get(k);
            return p == PlantPart.STEM || p == PlantPart.PAD || p == PlantPart.ROOT;
        });
        LongOpenHashSet petioles = new LongOpenHashSet();
        for (LongArrayList pad : PlantSkeleton.clusters(parts, k -> parts.get(k) == PlantPart.PAD)) {
            Vector3f mid = new Vector3f();
            int top = Integer.MIN_VALUE;
            for (long k : pad) {
                mid.add(c(k));
                top = Math.max(top, PlantSkeleton.y(k));
            }
            mid.div(pad.size());
            long centre = pad.getLong(0);
            for (long k : pad) if (c(k).distanceSquared(mid) < c(centre).distanceSquared(mid)) centre = k;
            float radius = (float) Math.sqrt(pad.size() / Math.PI) + 0.45F;
            Vector3f at = new Vector3f(mid.x, top + 0.88F, mid.z);
            if (toPads.contains(centre)) {
                LongArrayList path = toPads.path(centre);
                List<Vector3f> pts = new ArrayList<>();
                pts.add(new Vector3f(0.5F, 0.6F, 0.5F));
                for (int j = 1; j < path.size() - 1; j++) {
                    if (parts.get(path.getLong(j)) == PlantPart.PAD) break;
                    pts.add(c(path.getLong(j)));
                    petioles.add(path.getLong(j));
                }
                pts.add(new Vector3f(at).sub(0, 0.05F, 0));
                pts = smooth(pts, 2);
                use(path.getLong(Math.max(0, path.size() - 2)), PlantPart.STEM);
                tubes(PlantTex.STEM, pts, 0.07F * style.stem(), 0.06F * style.stem(), 4);
            }
            use(centre, PlantPart.PAD);
            float yaw = hash(centre, 5) * 6.28F;
            disc(PlantTex.LOTUS_PAD, at, new Vector3f(0, 1, 0), new Vector3f((float) Math.cos(yaw), 0, (float) Math.sin(yaw)),
                    radius * style.leafWide() / 1.2F, 0.04F, 0.03F, 12, curColor, curColor);
        }
        // the leaves under water, from the bulb (the petioles of the floating leaves are no leaves)
        PlantSkeleton.Tree tree = PlantSkeleton.tree(parts, k -> k == root, k -> {
            PlantPart p = parts.get(k);
            return p != PlantPart.PAD && !petioles.contains(k);
        });
        leaves(parts, tree, petioles, PlantTex.LOTUS_LEAF, 0.42F, 2.4F, 4.4F, 0.07F, new Vector3f(0.5F, 0.6F, 0.5F));
        m.swayPerBlock = 0.015;
        m.swayMax = 0.3;
    }

    // ------------------------------------------------------------------ M22.1 corals

    /** Tops of the columns of the given blocks: the highest block per column and the part there. */
    private void columns(java.util.function.Predicate<PlantPart> in, Long2FloatOpenHashMap top, Long2IntOpenHashMap topY,
                         Long2ObjectOpenHashMap<PlantPart> topPart, Long2IntOpenHashMap bottomY) {
        for (var e : blocks.long2ObjectEntrySet()) {
            PlantPart p = e.getValue();
            if (!in.test(p)) continue;
            long k = e.getLongKey();
            int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
            long ck = col(x, z);
            if (!topY.containsKey(ck) || y > topY.get(ck)) {
                topY.put(ck, y);
                topPart.put(ck, p);
            }
            if (bottomY != null && (!bottomY.containsKey(ck) || y < bottomY.get(ck))) bottomY.put(ck, y);
        }
        for (long ck : topY.keySet()) top.put(ck, topY.get(ck));
    }

    /** Zoanthids and green star polyps: the tissue mat over the rock and a polyp on its oral discs, closing when disturbed. */
    void polyps() {
        boolean star = sp == PlantSpecies.GREEN_STAR_POLYPS;
        Long2FloatOpenHashMap top = new Long2FloatOpenHashMap();
        Long2IntOpenHashMap topY = new Long2IntOpenHashMap();
        Long2ObjectOpenHashMap<PlantPart> topPart = new Long2ObjectOpenHashMap<>();
        Long2IntOpenHashMap bottomY = new Long2IntOpenHashMap();
        columns(p -> true, top, topY, topPart, bottomY);
        Long2IntOpenHashMap colour = new Long2IntOpenHashMap();
        Long2FloatOpenHashMap rimTo = new Long2FloatOpenHashMap();
        for (long ck : topY.keySet()) {
            rimTo.put(ck, bottomY.get(ck) + 0.12F);
            // star polyps encrust the rock as a sheet over the whole colony; zoanthids sit on a thin mat on the rock and
            // stand up on columns of their own
            top.put(ck, (star ? topY.get(ck) : bottomY.get(ck)) + (star ? 0.62F : 0.4F));
            colour.put(ck, colorOf(colX(ck), topY.get(ck), colZ(ck), PlantPart.LEAF));
        }
        Long2FloatOpenHashMap skin = heightfield(PlantTex.CORAL_MAT, top, colour, topY, star ? 0.45F : 0.25F, false, 4, rimTo);
        for (long ck : topY.keySet()) {
            int x = colX(ck), y = topY.get(ck), z = colZ(ck);
            boolean disc = topPart.get(ck) == PlantPart.TOP;
            // star polyps: on every oral disc and more in the mat between; zoanthids: a polyp on every column
            if (star && !disc && hash(x, y, z, 31) > 0.55F) continue;
            float jx = (hash(x, y, z, 32) - 0.5F) * 0.5F, jz = (hash(x, y, z, 33) - 0.5F) * 0.5F;
            float bx = x + 0.5F + jx, bz = z + 0.5F + jz;
            Vector3f base = new Vector3f(bx, surface(skin, bx, bz, y + 0.62F) - 0.06F, bz);
            float stalk = star ? 0.32F * (0.8F + hash(x, y, z, 34) * 0.4F)
                    : Math.max(0.45F, y + 0.55F - base.y) * (0.9F + hash(x, y, z, 34) * 0.2F);
            float radius = (star ? 0.62F : 0.5F) * (0.85F + hash(x, y, z, 35) * 0.3F) * style.detail();
            Vector3f lean = new Vector3f((hash(x, y, z, 36) - 0.5F) * 0.5F, 1, (hash(x, y, z, 37) - 0.5F) * 0.5F).normalize();
            Vector3f head = new Vector3f(base).add(new Vector3f(lean).mul(stalk));
            anim(PlantMeshRenderer.ANIM_POLYP, base, stalk + radius);
            use(x, y, z, PlantPart.LEAF);
            tube(PlantTex.TENTACLE, base, head, (star ? 0.12F : 0.22F) * style.detail(), (star ? 0.1F : 0.24F) * style.detail(), star ? 4 : 5);
            use(x, y, z, PlantPart.TOP);
            int tipC = disc || star ? curColor : lerpColor(curColor, colorOf(x, y, z, PlantPart.LEAF), 0.4F);
            int inner = star ? tipC : lerpColor(tipC, colorOf(x, y, z, PlantPart.LEAF), 0.15F);
            disc(star ? PlantTex.STAR_POLYP : PlantTex.ZOA_POLYP, head, lean, new Vector3f(1, 0, 0), radius, 0.06F, 0.02F, 8, inner, tipC);
            still();
        }
        m.reactive = true;
        m.swayPerBlock = 0.01;
        m.swayMax = 0.12;
    }

    /** Mushroom corals: discs on short stalks that slowly rise and sink. */
    void mushrooms() {
        Long2ObjectOpenHashMap<PlantPart> parts = parts();
        List<LongArrayList> centres = new ArrayList<>();
        // the middle of a disc: a TOP block with TOP blocks on at least three sides (the generator marks a small cross);
        // groups of TOP blocks without such a middle (trimmed, overlapped) count as one disc each
        for (LongArrayList cl : PlantSkeleton.clusters(parts, k -> parts.get(k) == PlantPart.TOP)) {
            int found = 0;
            for (long k : cl) {
                int n = 0;
                for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    if (parts.get(PlantSkeleton.key(PlantSkeleton.x(k) + d[0], PlantSkeleton.y(k), PlantSkeleton.z(k) + d[1])) == PlantPart.TOP) n++;
                }
                if (n >= 3) {
                    LongArrayList one = new LongArrayList();
                    one.add(k);
                    centres.add(one);
                    found++;
                }
            }
            if (found == 0) centres.add(cl);
        }
        if (centres.isEmpty()) {
            // trimmed down to the stalks: one disc on the root
            LongArrayList one = new LongArrayList();
            one.add(PlantSkeleton.key(0, 0, 0));
            centres.add(one);
        }
        List<Vector3f> mids = new ArrayList<>();
        for (LongArrayList cl : centres) {
            Vector3f mid = new Vector3f();
            for (long k : cl) mid.add(c(k));
            mids.add(mid.div(cl.size()));
        }
        float[] radius = new float[mids.size()];
        for (long k : parts.keySet()) {
            PlantPart p = parts.get(k);
            if (p != PlantPart.LEAF && p != PlantPart.TOP) continue;
            Vector3f pc = c(k);
            int best = 0;
            for (int i = 1; i < mids.size(); i++) if (pc.distanceSquared(mids.get(i)) < pc.distanceSquared(mids.get(best))) best = i;
            float dx = pc.x - mids.get(best).x, dz = pc.z - mids.get(best).z;
            radius[best] = Math.max(radius[best], (float) Math.sqrt(dx * dx + dz * dz));
        }
        // a disc is at most as wide as the generator makes them (discs that grew into each other are not one disc)
        for (int i = 0; i < radius.length; i++) radius[i] = Math.min(radius[i], 4.2F);
        for (int i = 0; i < mids.size(); i++) {
            Vector3f mid = mids.get(i);
            long ck = centres.get(i).getLong(0);
            Vector3f foot = new Vector3f(0.5F + (mid.x - 0.5F) * 0.3F, 0.1F, 0.5F + (mid.z - 0.5F) * 0.3F);
            Vector3f at = new Vector3f(mid.x, mid.y + 0.15F, mid.z);
            anim(PlantMeshRenderer.ANIM_RISE, foot, Math.max(0.5F, at.distance(foot) * 0.6F));
            use(ck, PlantPart.STEM);
            tube(PlantTex.FLESH, foot, new Vector3f(at).sub(0, 0.2F, 0), 0.32F, 0.26F, 6);
            int outer = colorOf(PlantSkeleton.x(ck), PlantSkeleton.y(ck), PlantSkeleton.z(ck), PlantPart.LEAF);
            int inner = colorOf(PlantSkeleton.x(ck), PlantSkeleton.y(ck), PlantSkeleton.z(ck), PlantPart.TOP);
            use(ck, PlantPart.LEAF);
            float yaw = hash(ck, 4) * 6.28F;
            Vector3f tilt = new Vector3f((hash(ck, 5) - 0.5F) * 0.35F, 1, (hash(ck, 6) - 0.5F) * 0.35F);
            disc(PlantTex.MUSHROOM, at, tilt, new Vector3f((float) Math.cos(yaw), 0, (float) Math.sin(yaw)), (radius[i] + 0.65F) * style.detail(),
                    0.22F, 0.08F, 12, inner, outer);
            still();
        }
        m.swayPerBlock = 0;
        m.swayMax = 0;
    }

    /** Leather coral: a thick trunk, a cap with drooping rim, a lawn of polyps on top that waves. */
    void toadstool() {
        Long2FloatOpenHashMap top = new Long2FloatOpenHashMap();
        Long2IntOpenHashMap topY = new Long2IntOpenHashMap();
        Long2ObjectOpenHashMap<PlantPart> topPart = new Long2ObjectOpenHashMap<>();
        Long2IntOpenHashMap bottomY = new Long2IntOpenHashMap();
        columns(p -> p == PlantPart.LEAF, top, topY, topPart, bottomY);
        // a polyp whose cap block below is gone still sits on the cap
        for (var e : blocks.long2ObjectEntrySet()) {
            if (e.getValue() != PlantPart.TOP) continue;
            long k = e.getLongKey();
            long ck = col(BlockPos.getX(k), BlockPos.getZ(k));
            if (topY.containsKey(ck)) continue;
            topY.put(ck, BlockPos.getY(k) - 1);
            bottomY.put(ck, BlockPos.getY(k) - 1);
            top.put(ck, BlockPos.getY(k) - 1);
        }
        int trunkTop = 0;
        boolean thick = false;
        for (var e : blocks.long2ObjectEntrySet()) {
            PlantPart p = e.getValue();
            long k = e.getLongKey();
            if (p == PlantPart.STEM || p == PlantPart.ROOT) trunkTop = Math.max(trunkTop, BlockPos.getY(k));
            if (p == PlantPart.STEM && (BlockPos.getX(k) != 0 || BlockPos.getZ(k) != 0)) thick = true;
        }
        float r = thick ? 1.45F : 0.55F;
        use(0, 0, 0, PlantPart.STEM);
        // the trunk reaches the underside of the cap in its middle
        float capLow = Float.MAX_VALUE, sum = 0;
        int n = 0;
        for (int dz = -1; dz <= 1; dz++)
            for (int dx = -1; dx <= 1; dx++) {
                if (!bottomY.containsKey(col(dx, dz))) continue;
                sum += bottomY.get(col(dx, dz));
                n++;
            }
        if (n > 0) capLow = sum / n;
        if (capLow == Float.MAX_VALUE) for (long ck : bottomY.keySet()) capLow = Math.min(capLow, bottomY.get(ck));
        if (capLow == Float.MAX_VALUE) capLow = trunkTop + 1;
        tube(PlantTex.FLESH, new Vector3f(0.5F, 0F, 0.5F), new Vector3f(0.5F, 0.6F, 0.5F), r * 1.35F, r, 8);
        tube(PlantTex.FLESH, new Vector3f(0.5F, 0.6F, 0.5F), new Vector3f(0.5F, capLow + 0.3F, 0.5F), r, r * 0.9F, 8);
        if (!top.isEmpty()) {
            Long2IntOpenHashMap colour = new Long2IntOpenHashMap();
            Long2FloatOpenHashMap under = new Long2FloatOpenHashMap();
            for (long ck : topY.keySet()) {
                top.put(ck, topY.get(ck) + 0.85F);
                under.put(ck, bottomY.get(ck) + 0.25F);
                colour.put(ck, colorOf(colX(ck), topY.get(ck), colZ(ck), PlantPart.LEAF));
            }
            Long2FloatOpenHashMap skin = heightfield(PlantTex.FLESH, top, colour, topY, 0.35F, false, 2);
            heightfield(PlantTex.FLESH, under, colour, topY, 0.25F, true, 2);
            // the polyp lawn: a tuft on every polyp block and on half of the cap
            for (long ck : topY.keySet()) {
                int x = colX(ck), y = topY.get(ck), z = colZ(ck);
                boolean polyp = has(x, y + 1, z) && part(x, y + 1, z) == PlantPart.TOP;
                if (!polyp && hash(x, y, z, 41) > 0.5F) continue;
                float bx = x + 0.3F + hash(x, y, z, 42) * 0.4F, bz = z + 0.3F + hash(x, y, z, 43) * 0.4F;
                Vector3f base = new Vector3f(bx, surface(skin, bx, bz, y + 0.85F) - 0.05F, bz);
                float h = (polyp ? 0.55F : 0.35F) * style.detail();
                Vector3f head = new Vector3f(base).add(0, h, 0);
                anim(PlantMeshRenderer.ANIM_WIGGLE, base, h + 0.3F);
                use(x, polyp ? y + 1 : y, z, PlantPart.TOP);
                tube(PlantTex.TENTACLE, base, head, 0.07F * style.detail(), 0.06F * style.detail(), 4);
                cross(PlantTex.TUFT, head, 0.55F * style.detail(), hash(x, y, z, 44) * 3F, true);
                still();
            }
        }
        m.swayPerBlock = 0;
        m.swayMax = 0;
    }

    /** Acropora: branches of the skeleton (thicker towards the base, pale tips), small polyps waving on them. */
    void acropora() {
        Long2ObjectOpenHashMap<PlantPart> parts = parts();
        long root = PlantSkeleton.key(0, 0, 0);
        PlantSkeleton.Tree tree = PlantSkeleton.tree(parts, k -> k == root, k -> true);
        Long2IntOpenHashMap reachTo = tree.reach();
        LongOpenHashSet ends = tree.ends();
        float sc = style.detail() > 1 ? 1.15F : 1F;
        for (long k : tree.order) {
            long p = tree.parent.get(k);
            float r = Math.min(0.62F, 0.2F + 0.05F * reachTo.get(k)) * sc;
            PlantPart part = parts.get(k);
            use(k, part == PlantPart.TOP ? PlantPart.TOP : PlantPart.LEAF);
            if (p == PlantSkeleton.FLOOR) {
                tube(PlantTex.ACRO, new Vector3f(0.5F, 0F, 0.5F), c(k), r * 1.3F, r, 6);
            } else {
                float rp = Math.min(0.62F, 0.2F + 0.05F * reachTo.get(p)) * sc;
                tube(PlantTex.ACRO, c(p), c(k), rp, r, 6);
            }
            if (ends.contains(k)) {
                Vector3f d = p == PlantSkeleton.FLOOR ? new Vector3f(0, 1, 0) : c(k).sub(c(p));
                if (d.lengthSquared() < 1e-4F) d.set(0, 1, 0);
                use(k, PlantPart.TOP);
                tube(PlantTex.ACRO, c(k), c(k).add(d.normalize().mul(0.35F)), r, r * 0.45F, 6);
            }
            // a polyp now and then
            if (hash(k, 51) < 0.35F) {
                Vector3f out = new Vector3f(hash(k, 52) - 0.5F, hash(k, 53) * 0.5F, hash(k, 54) - 0.5F);
                if (out.lengthSquared() < 1e-3F) out.set(1, 0, 0);
                Vector3f at = c(k).add(out.normalize().mul(r + 0.05F));
                anim(PlantMeshRenderer.ANIM_TUFT, at, 0.25F);
                use(k, PlantPart.TOP);
                cross(PlantTex.TUFT, at, 0.4F * style.detail(), hash(k, 55) * 3F, false);
                still();
            }
        }
        m.swayPerBlock = 0;
        m.swayMax = 0;
    }

    /** Hammer coral: the stony skeleton branches, on their ends fleshy heads of tentacles with hammer-shaped tips. */
    void hammer() {
        Long2ObjectOpenHashMap<PlantPart> parts = parts();
        long root = PlantSkeleton.key(0, 0, 0);
        PlantSkeleton.Tree skel = PlantSkeleton.tree(parts, k -> k == root, k -> {
            PlantPart p = parts.get(k);
            return p == PlantPart.STEM || p == PlantPart.ROOT;
        });
        for (long k : skel.order) {
            long p = skel.parent.get(k);
            use(k, PlantPart.STEM);
            tube(PlantTex.ACRO, p == PlantSkeleton.FLOOR ? new Vector3f(0.5F, 0F, 0.5F) : c(p), c(k), 0.36F, 0.32F, 6);
        }
        for (LongArrayList head : PlantSkeleton.clusters(parts, k -> parts.get(k) == PlantPart.LEAF || parts.get(k) == PlantPart.TOP)) {
            for (long k : head) {
                boolean tip = parts.get(k) == PlantPart.TOP;
                if (!tip && hash(k, 61) > 0.45F) continue;
                Vector3f to = c(k);
                // every tentacle grows from the nearest end of the skeleton (heads of neighbouring branches may touch)
                Vector3f base = new Vector3f(0.5F, 0.3F, 0.5F);
                float bd = Float.MAX_VALUE;
                for (long sk : skel.order) {
                    float d = c(sk).distanceSquared(to);
                    if (d < bd) {
                        bd = d;
                        base = c(sk);
                    }
                }
                Vector3f dir = new Vector3f(to).sub(base);
                if (dir.lengthSquared() < 1e-3F) dir.set(0, 1, 0);
                dir.normalize();
                Vector3f end = new Vector3f(to).add(new Vector3f(dir).mul(0.45F));
                float len = end.distance(base);
                anim(PlantMeshRenderer.ANIM_PULSE, base, len);
                use(k, PlantPart.LEAF);
                tube(PlantTex.TENTACLE, base, end, 0.34F * style.detail(), 0.28F * style.detail(), 6);
                // the hammer: a short bar across the tip
                Vector3f across = new Vector3f(dir).cross(0, 1, 0);
                if (across.lengthSquared() < 1e-3F) across.set(1, 0, 0);
                across.normalize().mul(0.55F * style.detail());
                if (hash(k, 62) > 0.5F) across = new Vector3f(across).cross(dir).normalize().mul(0.55F * style.detail());
                use(k, PlantPart.TOP);
                tube(PlantTex.TENTACLE, new Vector3f(end).sub(across), new Vector3f(end).add(across), 0.3F * style.detail(), 0.3F * style.detail(), 6);
                still();
            }
        }
        m.swayPerBlock = 0;
        m.swayMax = 0;
    }

    /** Lobophyllia: a fleshy dome, its ridges in the polyp colour, small waving polyps on the ridges. */
    void dome() {
        Long2FloatOpenHashMap top = new Long2FloatOpenHashMap();
        Long2IntOpenHashMap topY = new Long2IntOpenHashMap();
        Long2ObjectOpenHashMap<PlantPart> topPart = new Long2ObjectOpenHashMap<>();
        columns(p -> true, top, topY, topPart, null);
        Long2IntOpenHashMap colour = new Long2IntOpenHashMap();
        for (long ck : topY.keySet()) {
            top.put(ck, topY.get(ck) + 0.9F);
            PlantPart p = topPart.get(ck);
            colour.put(ck, colorOf(colX(ck), topY.get(ck), colZ(ck), p == PlantPart.TOP ? PlantPart.TOP : PlantPart.LEAF));
        }
        Long2FloatOpenHashMap rimTo = new Long2FloatOpenHashMap();
        for (long ck : topY.keySet()) rimTo.put(ck, 0.1F);
        Long2FloatOpenHashMap skin = heightfield(PlantTex.FLESH, top, colour, topY, 0.8F, false, 2, rimTo);
        for (long ck : topY.keySet()) {
            int x = colX(ck), y = topY.get(ck), z = colZ(ck);
            if (topPart.get(ck) != PlantPart.TOP || hash(x, y, z, 71) > 0.3F) continue;
            Vector3f at = new Vector3f(x + 0.5F, surface(skin, x + 0.5F, z + 0.5F, y + 0.9F) + 0.12F, z + 0.5F);
            anim(PlantMeshRenderer.ANIM_TUFT, at, 0.25F);
            use(x, y, z, PlantPart.TOP);
            cross(PlantTex.TUFT, at, 0.45F * style.detail(), hash(x, y, z, 72) * 3F, true);
            still();
        }
        m.swayPerBlock = 0;
        m.swayMax = 0;
    }

    /** Bubble tip anemone: the foot, the oral disc, tentacles with bubble tips that sway and pulse. */
    void anemone() {
        // the oral disc is the widest layer above the foot (the tentacles stand on every other block of it)
        Long2IntOpenHashMap perLayer = new Long2IntOpenHashMap();
        for (long k : blocks.keySet()) if (BlockPos.getY(k) >= 1) perLayer.addTo(BlockPos.getY(k), 1);
        int footY = 1, most = -1;
        for (long y : perLayer.keySet().toLongArray()) {
            int cnt = perLayer.get(y);
            if (cnt > most || cnt == most && y < footY) {
                most = cnt;
                footY = (int) y;
            }
        }
        int disc = 0;
        for (var e : blocks.long2ObjectEntrySet()) {
            long k = e.getLongKey();
            if (BlockPos.getY(k) != footY) continue;
            disc = Math.max(disc, Math.max(Math.abs(BlockPos.getX(k)), Math.abs(BlockPos.getZ(k))));
        }
        use(0, 0, 0, PlantPart.LEAF);
        tube(PlantTex.FLESH, new Vector3f(0.5F, 0F, 0.5F), new Vector3f(0.5F, footY + 0.4F, 0.5F), 1.0F, 0.8F, 8);
        int footC = colorOf(0, footY, 0, PlantPart.LEAF);
        disc(PlantTex.MUSHROOM, new Vector3f(0.5F, footY + 0.55F, 0.5F), new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), disc + 0.7F, 0.25F, 0.15F, 14,
                lerpColor(footC, colorOf(0, footY, 0, PlantPart.TOP), 0.35F), footC);
        // tentacles: from every bubble tip (TOP) down its column to the disc
        for (var e : blocks.long2ObjectEntrySet()) {
            if (e.getValue() != PlantPart.TOP) continue;
            long k = e.getLongKey();
            int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
            if (y <= footY) continue;
            List<Vector3f> pts = new ArrayList<>();
            int cx = x, cz = z;
            pts.add(c(x, y, z));
            for (int yy = y - 1; yy > footY; yy--) {
                if (!has(cx, yy, cz)) {
                    // the tips of the outer tentacles lean outwards by one block
                    if (has(cx - Integer.signum(cx), yy, cz - Integer.signum(cz))) {
                        cx -= Integer.signum(cx);
                        cz -= Integer.signum(cz);
                    } else {
                        break;
                    }
                }
                pts.add(c(cx, yy, cz));
            }
            pts.add(new Vector3f(cx + 0.5F, footY + 0.55F, cz + 0.5F));
            java.util.Collections.reverse(pts);
            pts = smooth(pts, 2);
            Vector3f base = pts.getFirst();
            float len = 0;
            for (int j = 1; j < pts.size(); j++) len += pts.get(j).distance(pts.get(j - 1));
            anim(PlantMeshRenderer.ANIM_PULSE, base, Math.max(0.5F, len));
            use(cx, footY + 1, cz, PlantPart.LEAF);
            tubes(PlantTex.TENTACLE, pts, 0.3F * style.detail(), 0.24F * style.detail(), 5);
            use(x, y, z, PlantPart.TOP);
            cross(PlantTex.BUBBLE, new Vector3f(pts.getLast()).add(0, 0.15F, 0), 0.75F * style.detail(), hash(x, y, z, 81) * 3F, true);
            still();
        }
        m.swayPerBlock = 0.02;
        m.swayMax = 0.3;
    }
}
