package org.overengineer.bettacraft.client.tank;

import java.util.ArrayList;
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
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.overengineer.bettacraft.tank.TankBlock;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankBlockItem;

/**
 * Tank block model: on every <b>outer</b> face (not connected to the same tank) the glass block's own face quads are
 * emitted with the glass' render type, plus thin dark-grey edge lines only along the outer edges of the whole tank.
 * Glass from other mods works as long as its model has normal per-face quads (connected textures are not emulated).
 */
public class TankBakedModel implements IDynamicBakedModel {
    private static final float LINE = 1F / 16F;
    private static final float EPS = 0.001F;

    private final TextureAtlasSprite edge;
    private final TextureAtlasSprite fallbackParticle;
    private final ItemTransforms transforms;
    @Nullable
    private final BlockState fixedGlass;
    private final List<BakedQuad>[] edgeCache;
    private final Map<ResourceLocation, TankBakedModel> itemVariants;
    private final ItemOverrides overrides;

    @SuppressWarnings("unchecked")
    public TankBakedModel(TextureAtlasSprite edge, TextureAtlasSprite fallbackParticle, ItemTransforms transforms, @Nullable BlockState fixedGlass,
                          @Nullable Map<ResourceLocation, TankBakedModel> sharedVariants) {
        this.edge = edge;
        this.fallbackParticle = fallbackParticle;
        this.transforms = transforms;
        this.fixedGlass = fixedGlass;
        this.edgeCache = new List[6 * 64];
        this.itemVariants = sharedVariants != null ? sharedVariants : new ConcurrentHashMap<>();
        this.overrides = new ItemOverrides() {
            @Override
            public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
                ResourceLocation id = TankBlockItem.getGlass(stack);
                return itemVariants.computeIfAbsent(id, k -> new TankBakedModel(edge, fallbackParticle, transforms,
                        TankBlockItem.glassBlock(stack).defaultBlockState(), itemVariants));
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

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        if (side == null) return List.of();
        int mask = state != null && state.getBlock() instanceof TankBlock ? TankBlock.connectionMask(state) : 0;
        if ((mask & (1 << side.ordinal())) != 0) return List.of();
        BlockState glass = glassOf(data);
        BakedModel gm = glassModel(glass);
        List<BakedQuad> out = new ArrayList<>();
        if (renderType == null || gm.getRenderTypes(glass, rand, ModelData.EMPTY).contains(renderType)) {
            out.addAll(gm.getQuads(glass, side, rand, ModelData.EMPTY, renderType));
        }
        if (renderType == null || renderType == RenderType.cutout()) {
            out.addAll(edgeQuads(side, mask));
        }
        return out;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        BlockState glass = glassOf(data);
        return ChunkRenderTypeSet.union(glassModel(glass).getRenderTypes(glass, rand, ModelData.EMPTY), ChunkRenderTypeSet.of(RenderType.cutout()));
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
        return List.of(fabulous ? Sheets.translucentCullBlockSheet() : Sheets.translucentItemSheet());
    }

    /** Edge lines on face {@code side} towards every perpendicular direction that is not connected. */
    private List<BakedQuad> edgeQuads(Direction side, int mask) {
        int key = side.ordinal() * 64 + mask;
        List<BakedQuad> cached = edgeCache[key];
        if (cached != null) return cached;
        List<BakedQuad> list = new ArrayList<>();
        float[] c = new float[12];
        for (Direction e : Direction.values()) {
            if (e.getAxis() == side.getAxis() || (mask & (1 << e.ordinal())) != 0) continue;
            float x0 = 0, y0 = 0, z0 = 0, x1 = 1, y1 = 1, z1 = 1;
            switch (e) {
                case DOWN -> y1 = LINE;
                case UP -> y0 = 1 - LINE;
                case NORTH -> z1 = LINE;
                case SOUTH -> z0 = 1 - LINE;
                case WEST -> x1 = LINE;
                case EAST -> x0 = 1 - LINE;
            }
            // push the line slightly out of the glass plane to avoid z-fighting
            switch (side) {
                case DOWN -> y0 = -EPS;
                case UP -> y1 = 1 + EPS;
                case NORTH -> z0 = -EPS;
                case SOUTH -> z1 = 1 + EPS;
                case WEST -> x0 = -EPS;
                case EAST -> x1 = 1 + EPS;
            }
            BoxFaces.corners(side, x0, y0, z0, x1, y1, z1, c);
            list.add(bake(side, c));
        }
        List<BakedQuad> result = List.copyOf(list);
        edgeCache[key] = result;
        return result;
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
