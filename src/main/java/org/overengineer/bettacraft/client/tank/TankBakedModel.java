package org.overengineer.bettacraft.client.tank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.overengineer.bettacraft.config.ClientConfig;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankBlockItem;
import org.overengineer.bettacraft.tank.TankStyle;

/**
 * Tank block model. On every <b>outer</b> face (not connected to the same tank):
 * <ul>
 *     <li>{@link TankStyle#TEXTURED}: the glass block's own face quads with the glass' render type. M11: connected
 *     textures - where the face continues into the neighbouring block of the same tank, the UVs skip the frame of the
 *     glass texture ({@link ConnectedGlass}), so the frame only shows along the outer edges of the whole tank. Works for
 *     the glass of any mod whose model has ordinary face quads (the frame width is read from the sprite).</li>
 *     <li>{@link TankStyle#CLEAN}: no glass quads at all (perfectly clear).</li>
 * </ul>
 * Both get thin dark-grey edge lines ({@code render.tankEdgeWidth} pixels, default 0.25 = 1/64 block) along the outer edges
 * of the whole tank, the concave edges of free shapes (M10) and, new in M11, a small square in the in-plane inner
 * corners of a flat wall (e.g. the front of a T), where the lines of two neighbouring blocks meet.
 * The concave and corner masks need the neighbours' block states, which {@link #getModelData} reads while the chunk is
 * meshed; Sodium 0.8 and Embeddium call that method as well (checked in their jars).
 */
public class TankBakedModel implements IDynamicBakedModel {
    private static final float EPS = 0.001F;

    /** Per side (6 bits each, side.ordinal() * 6): edge directions with a concave crease. */
    public static final ModelProperty<Long> CONCAVE = new ModelProperty<>();
    /** Per side (4 bits each, side.ordinal() * 4): in-plane inner corners ({@link TankBlock#innerCornerMask}). */
    public static final ModelProperty<Integer> CORNERS = new ModelProperty<>();
    /** M18: RGB of the tank's coloured lights for the water drawn by the model (shader packs); absent = no tint. */
    public static final ModelProperty<Integer> LIGHT = new ModelProperty<>();

    private final TextureAtlasSprite edge;
    private final TextureAtlasSprite fallbackParticle;
    private final ItemTransforms transforms;
    @Nullable
    private final BlockState fixedGlass;
    private final TankStyle fixedStyle;
    /** Edge quads by (side, connection mask, concave bits, corner bits); rebuilt when the edge width changes. */
    private final Map<Long, List<BakedQuad>> edgeCache = new ConcurrentHashMap<>();
    private volatile int edgeCacheWidth = -1;
    /** Bit of {@link #CORNERS}: the block above belongs to the tank and holds water (no water surface in this block). */
    private static final int WATER_ABOVE_BIT = 1 << 30;
    /** Bits 24..27 of {@link #CORNERS}: {@link TankWaterRenderer#gapMask}. */
    private static final int GAP_SHIFT = 24;
    /** Water quads drawn by the model while a shader pack is in use, by (fill, connection mask, water above, gaps). */
    private final Map<Long, List<BakedQuad>> waterCache = new ConcurrentHashMap<>();
    /** Connected / cut glass quads: original quad -> (hidden slots, water behind it) -> the pieces. */
    private final Map<BakedQuad, Map<Integer, BakedQuad[]>> glassCache = new ConcurrentHashMap<>();
    /** Frame width (fraction of the face) per glass sprite. */
    private final Map<ResourceLocation, Float> frameCache = new ConcurrentHashMap<>();
    private final Map<String, TankBakedModel> itemVariants;
    private final ItemOverrides overrides;

    public TankBakedModel(TextureAtlasSprite edge, TextureAtlasSprite fallbackParticle, ItemTransforms transforms, @Nullable BlockState fixedGlass,
                          TankStyle fixedStyle, @Nullable Map<String, TankBakedModel> sharedVariants) {
        this.edge = edge;
        this.fallbackParticle = fallbackParticle;
        this.transforms = transforms;
        this.fixedGlass = fixedGlass;
        this.fixedStyle = fixedStyle;
        this.itemVariants = sharedVariants != null ? sharedVariants : new ConcurrentHashMap<>();
        this.overrides = new ItemOverrides() {
            @Override
            public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
                TankStyle style = TankBlockItem.getStyle(stack);
                String key = TankBlockItem.getGlass(stack) + "|" + style.getSerializedName();
                return itemVariants.computeIfAbsent(key, k -> new TankBakedModel(edge, fallbackParticle, transforms,
                        TankBlockItem.glassBlock(stack).defaultBlockState(), style, itemVariants));
            }
        };
    }

    private static BakedModel glassModel(BlockState glass) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(glass);
    }

    private BlockState glassOf(ModelData data) {
        if (fixedGlass != null) return fixedGlass;
        BlockState g = data.get(TankBlockEntity.GLASS_STATE);
        return g != null ? g : Blocks.GLASS.defaultBlockState();
    }

    private TankStyle styleOf(ModelData data) {
        if (fixedGlass != null) return fixedStyle;
        TankStyle s = data.get(TankBlockEntity.STYLE);
        return s != null ? s : TankStyle.TEXTURED;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        if (side == null) {
            // with a shader pack the water is part of the block model (shaders then treat it like tinted glass)
            if (state != null && state.getBlock() instanceof TankBlock && (renderType == null || renderType == RenderType.translucent())
                    && org.overengineer.bettacraft.client.interior.IrisCompat.shaderPackInUse()) {
                return waterQuads(state, data);
            }
            return List.of();
        }
        int mask = state != null && state.getBlock() instanceof TankBlock ? TankBlock.connectionMask(state) : 0;
        if ((mask & (1 << side.ordinal())) != 0) return List.of();
        Long concaveData = data.get(CONCAVE);
        long concave = concaveData == null ? 0 : concaveData;
        List<BakedQuad> out = new ArrayList<>();
        if (styleOf(data) == TankStyle.TEXTURED) {
            BlockState glass = glassOf(data);
            BakedModel gm = glassModel(glass);
            if (renderType == null || gm.getRenderTypes(glass, rand, ModelData.EMPTY).contains(renderType)) {
                List<BakedQuad> quads = gm.getQuads(glass, side, rand, ModelData.EMPTY, renderType);
                int hidden = ClientConfig.CONNECTED_GLASS.get() ? ConnectedGlass.hiddenSlots(mask, concave, side) : 0;
                // translucent glass in front of water is cut where the water face behind it ends (the water height and
                // INSET from the outer edges), so every water face sorts behind its glass piece (see TankWaterGeometry)
                int water = 0; // 0 = no cut, else fill | mask << 5 | above << 11
                if (state != null && !TankWaterRenderer.m10Look && (renderType == null || renderType == RenderType.translucent())) {
                    int fill = state.getValue(TankBlock.FILL);
                    if (fill > 0) {
                        Integer cd = data.get(CORNERS);
                        boolean waterAbove = cd != null && (cd & WATER_ABOVE_BIT) != 0;
                        water = fill | mask << 5 | (waterAbove ? 1 << 11 : 0);
                    }
                }
                if (hidden == 0 && water == 0) {
                    out.addAll(quads);
                } else {
                    for (BakedQuad q : quads) java.util.Collections.addAll(out, connected(q, side, hidden, water));
                }
            }
        }
        if (renderType == null || renderType == RenderType.cutout()) {
            int width = edgeWidth();
            if (width > 0) {
                Integer cornerData = data.get(CORNERS);
                int corners = cornerData == null ? 0 : (cornerData >>> (side.ordinal() * 4)) & 15;
                out.addAll(edgeQuads(side, mask, (int) ((concave >>> (side.ordinal() * 6)) & 63), corners, width));
            }
        }
        return out;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        ChunkRenderTypeSet own = state.getBlock() instanceof TankBlock && state.getValue(TankBlock.FILL) > 0
                && org.overengineer.bettacraft.client.interior.IrisCompat.shaderPackInUse()
                ? ChunkRenderTypeSet.of(RenderType.cutout(), RenderType.translucent()) : ChunkRenderTypeSet.of(RenderType.cutout());
        if (styleOf(data) == TankStyle.CLEAN) return own;
        BlockState glass = glassOf(data);
        return ChunkRenderTypeSet.union(glassModel(glass).getRenderTypes(glass, rand, ModelData.EMPTY), own);
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
        return List.of(fabulous ? Sheets.translucentCullBlockSheet() : Sheets.translucentItemSheet());
    }

    @Override
    public ModelData getModelData(net.minecraft.world.level.BlockAndTintGetter level, net.minecraft.core.BlockPos pos, BlockState state, ModelData modelData) {
        if (!(state.getBlock() instanceof TankBlock)) return modelData;
        long concave = TankBlock.concaveMask(level, pos, state);
        int corners = TankBlock.innerCornerMask(level, pos, state, concave);
        if (state.getValue(TankBlock.FILL) > 0 && TankWaterRenderer.waterAbove(level, pos, state)) corners |= WATER_ABOVE_BIT;
        corners |= TankWaterRenderer.gapMask(level, pos, state) << GAP_SHIFT;
        int light = state.getValue(TankBlock.FILL) > 0 ? TankLightTint.colorAt(level, pos) : 0;
        if (concave == 0 && corners == 0 && light == 0) return modelData;
        var b = modelData.derive().with(CONCAVE, concave).with(CORNERS, corners);
        if (light != 0) b.with(LIGHT, light);
        return b.build();
    }

    // ------------------------------------------------------------------ water (shader packs)

    /**
     * Shader packs (Iris) give the fluid path the material of vanilla water: packs like Complementary then draw the
     * side faces of tank water as deep, reflecting water and the fish behind them disappear. While a pack is in use the
     * same faces ({@link TankWaterRenderer#faces}) are part of this model, so the pack sees the tank block (a generic
     * translucent block, like tinted glass). Iris rebuilds all chunks when a pack is switched.
     */
    private List<BakedQuad> waterQuads(BlockState state, ModelData data) {
        Integer cd = data.get(CORNERS);
        int bits = cd == null ? 0 : cd;
        boolean above = (bits & WATER_ABOVE_BIT) != 0;
        int gaps = (bits >>> GAP_SHIFT) & 15;
        Integer ld = data.get(LIGHT);
        int light = ld == null ? 0 : ld;
        // M18: the light colour (and the tint strength it was mixed with) is part of the key
        long key = (state.getValue(TankBlock.FILL) | TankBlock.connectionMask(state) << 5 | (above ? 1 << 11 : 0) | gaps << 12)
                | (long) light << 16 | (light == 0 ? 0L : (long) Math.round(TankLightTint.strength() * 100) << 40);
        return waterCache.computeIfAbsent(key, k -> {
            var atlas = Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS);
            TextureAtlasSprite still = atlas.apply(TankWaterRenderer.STILL), sideSprite = atlas.apply(TankWaterRenderer.SIDE);
            List<BakedQuad> list = new ArrayList<>();
            TankWaterRenderer.faces(state, above, gaps, light, (d, c, col, rev) ->
                    list.add(bakeWater(d, c, col, rev, d.getAxis() == Direction.Axis.Y ? still : sideSprite)));
            return List.copyOf(list);
        });
    }

    private static BakedQuad bakeWater(Direction d, float[] c, int argb, boolean reversed, TextureAtlasSprite sprite) {
        QuadBakingVertexConsumer q = new QuadBakingVertexConsumer();
        q.setSprite(sprite);
        q.setDirection(reversed ? d.getOpposite() : d);
        q.setShade(false);
        q.setTintIndex(-1);
        float nx = d.getStepX(), ny = d.getStepY(), nz = d.getStepZ();
        if (reversed) {
            nx = -nx; ny = -ny; nz = -nz;
        }
        for (int k = 0; k < 4; k++) {
            int i = reversed ? 3 - k : k;
            float x = c[i * 3], y = c[i * 3 + 1], z = c[i * 3 + 2];
            q.addVertex(x, y, z)
                    .setColor(argb)
                    .setUv(sprite.getU(BoxFaces.u(d, x, y, z)), sprite.getV(BoxFaces.v(d, x, y, z)))
                    .setNormal(nx, ny, nz);
        }
        return q.bakeQuad();
    }

    // ------------------------------------------------------------------ connected glass

    /**
     * The glass quad with UVs that skip the frame on the hidden edge slots, split at the water height of fill
     * {@code split} (0 = no split) into the piece in front of the water and the piece above it (cached per quad).
     */
    private BakedQuad[] connected(BakedQuad q, Direction side, int hidden, int water) {
        Map<Integer, BakedQuad[]> variants = glassCache.computeIfAbsent(q, k -> new ConcurrentHashMap<>());
        int key = hidden | water << 4;
        BakedQuad[] r = variants.get(key);
        if (r == null) {
            float frame = hidden == 0 ? 0 : frameOf(q.getSprite());
            float[] cuts = water == 0 ? new float[]{Float.NaN}
                    : TankWaterGeometry.glassCuts((water >> 5) & 63, water & 31, (water & (1 << 11)) != 0, side);
            List<Float> sc = new ArrayList<>(), tc = new ArrayList<>();
            boolean inT = false;
            for (float c : cuts) {
                if (Float.isNaN(c)) inT = true;
                else (inT ? tc : sc).add(c);
            }
            List<BakedQuad> pieces = new ArrayList<>();
            for (int i = 0; i <= sc.size(); i++) {
                float s0 = i == 0 ? Float.NEGATIVE_INFINITY : sc.get(i - 1), s1 = i == sc.size() ? Float.POSITIVE_INFINITY : sc.get(i);
                for (int j = 0; j <= tc.size(); j++) {
                    float t0 = j == 0 ? Float.NEGATIVE_INFINITY : tc.get(j - 1), t1 = j == tc.size() ? Float.POSITIVE_INFINITY : tc.get(j);
                    BakedQuad piece = rebuild(q, side, hidden, frame, s0, s1, t0, t1);
                    if (piece != null) pieces.add(piece);
                }
            }
            r = pieces.toArray(new BakedQuad[0]);
            variants.put(key, r);
        }
        return r;
    }

    /**
     * One piece (face-local s in [sMin, sMax], t in [tMin, tMax]) of a glass quad with remapped UVs; the quad itself if
     * nothing changes, null if the piece is empty.
     */
    @Nullable
    private static BakedQuad rebuild(BakedQuad q, Direction side, int hidden, float frame, float sMin, float sMax, float tMin, float tMax) {
        boolean clip = sMin > Float.NEGATIVE_INFINITY || sMax < Float.POSITIVE_INFINITY || tMin > Float.NEGATIVE_INFINITY
                || tMax < Float.POSITIVE_INFINITY;
        if (frame <= 0 && !clip) return q;
        int[] data = q.getVertices();
        int stride = data.length / 4;
        if (stride < 6) return q;
        float[] s = new float[4], t = new float[4], u = new float[4], v = new float[4];
        float[][] pos = new float[4][];
        for (int i = 0; i < 4; i++) {
            int o = i * stride;
            float x = Float.intBitsToFloat(data[o]), y = Float.intBitsToFloat(data[o + 1]), z = Float.intBitsToFloat(data[o + 2]);
            pos[i] = new float[]{x, y, z};
            s[i] = BoxFaces.u(side, x, y, z);
            t[i] = BoxFaces.v(side, x, y, z);
            u[i] = Float.intBitsToFloat(data[o + 4]);
            v[i] = Float.intBitsToFloat(data[o + 5]);
        }
        if (!ConnectedGlass.clipAndRemapUv(s, t, u, v, hidden, frame, sMin, sMax, tMin, tMax)) return q;
        if (clip) {
            float smin = Math.min(Math.min(s[0], s[1]), Math.min(s[2], s[3])), smax = Math.max(Math.max(s[0], s[1]), Math.max(s[2], s[3]));
            float tmin = Math.min(Math.min(t[0], t[1]), Math.min(t[2], t[3])), tmax = Math.max(Math.max(t[0], t[1]), Math.max(t[2], t[3]));
            if (smax - smin < 1e-5F || tmax - tmin < 1e-5F) return null;
        }
        int[] copy = Arrays.copyOf(data, data.length);
        for (int i = 0; i < 4; i++) {
            if (clip) {
                setFacePos(side, pos[i], s[i], t[i]);
                copy[i * stride] = Float.floatToRawIntBits(pos[i][0]);
                copy[i * stride + 1] = Float.floatToRawIntBits(pos[i][1]);
                copy[i * stride + 2] = Float.floatToRawIntBits(pos[i][2]);
            }
            copy[i * stride + 4] = Float.floatToRawIntBits(u[i]);
            copy[i * stride + 5] = Float.floatToRawIntBits(v[i]);
        }
        return new BakedQuad(copy, q.getTintIndex(), q.getDirection(), q.getSprite(), q.isShade(), q.hasAmbientOcclusion());
    }

    /** Inverse of {@link BoxFaces#u} / {@link BoxFaces#v}: sets the in-plane coordinates of {@code p} on face {@code side}. */
    private static void setFacePos(Direction side, float[] p, float s, float t) {
        switch (side) {
            case DOWN -> { p[0] = s; p[2] = 1 - t; }
            case UP -> { p[0] = s; p[2] = t; }
            case NORTH -> { p[0] = 1 - s; p[1] = 1 - t; }
            case SOUTH -> { p[0] = s; p[1] = 1 - t; }
            case WEST -> { p[2] = s; p[1] = 1 - t; }
            case EAST -> { p[2] = 1 - s; p[1] = 1 - t; }
        }
    }

    /** Frame width of a glass sprite as a fraction of the face (read once from its pixels; vanilla glass: 1/16). */
    private float frameOf(TextureAtlasSprite sprite) {
        return frameCache.computeIfAbsent(sprite.contents().name(), k -> {
            try {
                var c = sprite.contents();
                var img = c.getOriginalImage();
                int w = c.width(), h = c.height();
                int[] argb = new int[w * h];
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int abgr = img.getPixelRGBA(x, y);
                        argb[y * w + x] = (abgr & 0xFF00FF00) | ((abgr & 0xFF) << 16) | ((abgr >> 16) & 0xFF);
                    }
                }
                return ConnectedGlass.frameTexels(argb, w, h) / (float) w;
            } catch (RuntimeException e) {
                return 1F / 16F;
            }
        });
    }

    // ------------------------------------------------------------------ edge lines

    /**
     * Edge lines on face {@code side} towards every perpendicular direction that is not connected, towards the
     * connected ones with a concave crease ({@code concave}, bit = direction ordinal), and corner squares for the
     * in-plane inner corners ({@code corners}, see {@link TankBlock#cornerIndex}).
     */
    /**
     * The configured edge width in 1/1000 pixel: 0 = off, otherwise at least 0.1 px (thinner lines are far below a screen
     * pixel even up close and only flicker).
     */
    static int edgeWidth() {
        double px = ClientConfig.TANK_EDGE_WIDTH.get();
        if (px <= 0) return 0;
        return (int) Math.round(Math.min(1.0, Math.max(0.1, px)) * 1000);
    }

    private List<BakedQuad> edgeQuads(Direction side, int mask, int concave, int corners, int width) {
        if (edgeCacheWidth != width) {
            edgeCache.clear();
            edgeCacheWidth = width;
        }
        // M17: the width is part of the key: a meshing thread that still used the old width cannot leave stale quads
        long key = ((((long) side.ordinal() * 64 + mask) * 64 + concave) * 16 + corners) | ((long) width << 32);
        List<BakedQuad> cached = edgeCache.get(key);
        if (cached != null) return cached;
        float line = width / 1000F / 16F;
        List<BakedQuad> list = new ArrayList<>();
        float[] c = new float[12];
        for (Direction e : Direction.values()) {
            if (e.getAxis() == side.getAxis()) continue;
            if ((mask & (1 << e.ordinal())) != 0 && (concave & (1 << e.ordinal())) == 0) continue;
            float[] b = {0, 0, 0, 1, 1, 1};
            towards(b, e, line);
            outOfPlane(b, side);
            // at an outer edge the line of the neighbouring face sits EPS out of its own plane: reach out to it, or a
            // hairline gap opens between the two lines at the corner
            if ((mask & (1 << e.ordinal())) == 0) beyond(b, e);
            BoxFaces.corners(side, b[0], b[1], b[2], b[3], b[4], b[5], c);
            list.add(bake(side, c));
        }
        for (Direction a : Direction.values()) {
            for (Direction bdir : Direction.values()) {
                if (a.getAxis() == side.getAxis() || bdir.getAxis() == side.getAxis() || a.getAxis() == bdir.getAxis() || a.ordinal() > bdir.ordinal()) continue;
                if ((corners & (1 << TankBlock.cornerIndex(side, a, bdir))) == 0) continue;
                float[] b = {0, 0, 0, 1, 1, 1};
                towards(b, a, line);
                towards(b, bdir, line);
                outOfPlane(b, side);
                BoxFaces.corners(side, b[0], b[1], b[2], b[3], b[4], b[5], c);
                list.add(bake(side, c));
            }
        }
        List<BakedQuad> result = List.copyOf(list);
        edgeCache.put(key, result);
        return result;
    }

    /** Shrinks the box {@code b} (x0,y0,z0,x1,y1,z1) to the strip of width {@code w} along its side towards {@code e}. */
    private static void towards(float[] b, Direction e, float w) {
        switch (e) {
            case DOWN -> b[4] = w;
            case UP -> b[1] = 1 - w;
            case NORTH -> b[5] = w;
            case SOUTH -> b[2] = 1 - w;
            case WEST -> b[3] = w;
            case EAST -> b[0] = 1 - w;
        }
    }

    /** Extends the box {@code b} by EPS past the block border towards {@code e}. */
    private static void beyond(float[] b, Direction e) {
        switch (e) {
            case DOWN -> b[1] = -EPS;
            case UP -> b[4] = 1 + EPS;
            case NORTH -> b[2] = -EPS;
            case SOUTH -> b[5] = 1 + EPS;
            case WEST -> b[0] = -EPS;
            case EAST -> b[3] = 1 + EPS;
        }
    }

    /** Pushes the line slightly out of the glass plane to avoid z-fighting. */
    private static void outOfPlane(float[] b, Direction side) {
        switch (side) {
            case DOWN -> b[1] = -EPS;
            case UP -> b[4] = 1 + EPS;
            case NORTH -> b[2] = -EPS;
            case SOUTH -> b[5] = 1 + EPS;
            case WEST -> b[0] = -EPS;
            case EAST -> b[3] = 1 + EPS;
        }
    }

    private BakedQuad bake(Direction side, float[] c) {
        QuadBakingVertexConsumer q = new QuadBakingVertexConsumer();
        q.setSprite(edge);
        q.setDirection(side);
        q.setShade(true);
        q.setTintIndex(-1);
        for (int k = 0; k < 4; k++) {
            float x = c[k * 3], y = c[k * 3 + 1], z = c[k * 3 + 2];
            q.addVertex(x, y, z)
                    .setColor(0xFFFFFFFF)
                    .setUv(edge.getU(0.25F + 0.5F * BoxFaces.u(side, clamp(x), clamp(y), clamp(z))),
                            edge.getV(0.25F + 0.5F * BoxFaces.v(side, clamp(x), clamp(y), clamp(z))))
                    .setNormal(side.getStepX(), side.getStepY(), side.getStepZ());
        }
        return q.bakeQuad();
    }

    private static float clamp(float f) {
        return Math.max(0, Math.min(1, f));
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public TextureAtlasSprite getParticleIcon() {
        return fixedGlass != null ? glassModel(fixedGlass).getParticleIcon() : fallbackParticle;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return glassModel(glassOf(data)).getParticleIcon(ModelData.EMPTY);
    }

    @Override
    public ItemTransforms getTransforms() {
        return transforms;
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }
}
