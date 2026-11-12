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
 * Both get thin dark-grey edge lines (M11: {@code render.tankEdgeWidth} / 64 block, default 1/32) along the outer edges
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
    /** Connected / split glass quads: original quad -> [hidden slots 0..15 x split fill 0..16] -> 1 or 2 quads. */
    private final Map<BakedQuad, BakedQuad[][]> glassCache = new ConcurrentHashMap<>();
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
        if (side == null) return List.of();
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
                // translucent glass of a side face is split at the water height (water seam fix, see TankWaterGeometry)
                int split = 0;
                if (state != null && side.getAxis().isHorizontal() && !TankWaterRenderer.m10Look
                        && (renderType == null || renderType == RenderType.translucent())) {
                    Integer cd = data.get(CORNERS);
                    boolean waterAbove = cd != null && (cd & WATER_ABOVE_BIT) != 0;
                    int fill = state.getValue(TankBlock.FILL);
                    if (TankWaterGeometry.glassSplit(fill, waterAbove) > 0) split = fill;
                }
                if (hidden == 0 && split == 0) {
                    out.addAll(quads);
                } else {
                    for (BakedQuad q : quads) java.util.Collections.addAll(out, connected(q, side, hidden, split));
                }
            }
        }
        if (renderType == null || renderType == RenderType.cutout()) {
            int width = ClientConfig.TANK_EDGE_WIDTH.get();
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
        if (styleOf(data) == TankStyle.CLEAN) return ChunkRenderTypeSet.of(RenderType.cutout());
        BlockState glass = glassOf(data);
        return ChunkRenderTypeSet.union(glassModel(glass).getRenderTypes(glass, rand, ModelData.EMPTY), ChunkRenderTypeSet.of(RenderType.cutout()));
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
        if (TankBlock.isConnected(state, Direction.UP) && state.getValue(TankBlock.FILL) > 0) {
            BlockState above = level.getBlockState(pos.above());
            if (above.getBlock() instanceof TankBlock && above.getValue(TankBlock.FILL) > 0) corners |= WATER_ABOVE_BIT;
        }
        if (concave == 0 && corners == 0) return modelData;
        return modelData.derive().with(CONCAVE, concave).with(CORNERS, corners).build();
    }

    // ------------------------------------------------------------------ connected glass

    /**
     * The glass quad with UVs that skip the frame on the hidden edge slots, split at the water height of fill
     * {@code split} (0 = no split) into the piece in front of the water and the piece above it (cached per quad).
     */
    private BakedQuad[] connected(BakedQuad q, Direction side, int hidden, int split) {
        BakedQuad[][] variants = glassCache.computeIfAbsent(q, k -> new BakedQuad[16 * 17][]);
        int key = hidden * 17 + split;
        BakedQuad[] r = variants[key];
        if (r == null) {
            float frame = hidden == 0 ? 0 : frameOf(q.getSprite());
            if (split == 0) {
                r = new BakedQuad[]{rebuild(q, side, hidden, frame, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)};
            } else {
                // side faces: t = 1 - y; lower piece y in [0, h], upper piece y in [h, 1]
                float t = 1 - TankWaterGeometry.glassSplit(split, false);
                r = new BakedQuad[]{rebuild(q, side, hidden, frame, t, Float.POSITIVE_INFINITY),
                        rebuild(q, side, hidden, frame, Float.NEGATIVE_INFINITY, t)};
            }
            variants[key] = r;
        }
        return r;
    }

    /** One piece (face-local t in [tMin, tMax]) of a glass quad with remapped UVs; the quad itself if nothing changes. */
    private static BakedQuad rebuild(BakedQuad q, Direction side, int hidden, float frame, float tMin, float tMax) {
        boolean clip = tMin > Float.NEGATIVE_INFINITY || tMax < Float.POSITIVE_INFINITY;
        if (frame <= 0 && !clip) return q;
        int[] data = q.getVertices();
        int stride = data.length / 4;
        if (stride < 6) return q;
        float[] s = new float[4], t = new float[4], u = new float[4], v = new float[4];
        for (int i = 0; i < 4; i++) {
            int o = i * stride;
            float x = Float.intBitsToFloat(data[o]), y = Float.intBitsToFloat(data[o + 1]), z = Float.intBitsToFloat(data[o + 2]);
            s[i] = BoxFaces.u(side, x, y, z);
            t[i] = BoxFaces.v(side, x, y, z);
            u[i] = Float.intBitsToFloat(data[o + 4]);
            v[i] = Float.intBitsToFloat(data[o + 5]);
        }
        if (!ConnectedGlass.clipAndRemapUv(s, t, u, v, hidden, frame, tMin, tMax)) return q;
        int[] copy = Arrays.copyOf(data, data.length);
        for (int i = 0; i < 4; i++) {
            if (clip && side.getAxis().isHorizontal()) copy[i * stride + 1] = Float.floatToRawIntBits(1 - t[i]);
            copy[i * stride + 4] = Float.floatToRawIntBits(u[i]);
            copy[i * stride + 5] = Float.floatToRawIntBits(v[i]);
        }
        return new BakedQuad(copy, q.getTintIndex(), q.getDirection(), q.getSprite(), q.isShade(), q.hasAmbientOcclusion());
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
    private List<BakedQuad> edgeQuads(Direction side, int mask, int concave, int corners, int width) {
        if (edgeCacheWidth != width) {
            edgeCache.clear();
            edgeCacheWidth = width;
        }
        long key = (((long) side.ordinal() * 64 + mask) * 64 + concave) * 16 + corners;
        List<BakedQuad> cached = edgeCache.get(key);
        if (cached != null) return cached;
        float line = width / 64F;
        List<BakedQuad> list = new ArrayList<>();
        float[] c = new float[12];
        for (Direction e : Direction.values()) {
            if (e.getAxis() == side.getAxis()) continue;
            if ((mask & (1 << e.ordinal())) != 0 && (concave & (1 << e.ordinal())) == 0) continue;
            float[] b = {0, 0, 0, 1, 1, 1};
            towards(b, e, line);
            outOfPlane(b, side);
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
