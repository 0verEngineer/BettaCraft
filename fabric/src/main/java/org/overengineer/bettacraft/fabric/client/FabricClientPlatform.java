package org.overengineer.bettacraft.fabric.client;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.platform.client.ClientPlatform;
import org.overengineer.bettacraft.platform.client.DynamicModel;
import org.overengineer.bettacraft.platform.model.ModelDataSource;
import org.overengineer.bettacraft.platform.model.ModelKey;
import org.overengineer.bettacraft.platform.model.ModelValues;

/**
 * M52: the client platform on Fabric. A block has one chunk layer here ({@link ItemBlockRenderTypes}); our dynamic
 * models ({@link FabricDynamicModel}) have several, the miniature tesselates them one layer at a time through a plain
 * model of that layer's quads (vanilla's block renderer: smooth light, culling).
 */
public final class FabricClientPlatform implements ClientPlatform {
    private static final Direction[] SIDES = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private static @Nullable DynamicModel dynamic(BakedModel model) {
        return model instanceof FabricDynamicModel f ? f.dynamic() : null;
    }

    private static ModelValues blockEntityValues(BlockAndTintGetter level, BlockPos pos) {
        Object rd = level.getBlockEntityRenderData(pos);
        if (rd instanceof ModelValues v) return v;
        return level.getBlockEntity(pos) instanceof ModelDataSource s ? s.modelValues() : ModelValues.EMPTY;
    }

    @Override
    public Object modelData(BakedModel model, BlockAndTintGetter level, BlockPos pos, BlockState state) {
        DynamicModel d = dynamic(model);
        return d == null ? ModelValues.EMPTY : d.modelData(level, pos, state, blockEntityValues(level, pos));
    }

    @Override
    public Iterable<RenderType> renderTypes(BakedModel model, BlockState state, RandomSource rand, Object modelData) {
        DynamicModel d = dynamic(model);
        return d != null ? d.layers(state, rand, (ModelValues) modelData) : List.of(ItemBlockRenderTypes.getChunkRenderType(state));
    }

    @Override
    public void renderBatched(BlockRenderDispatcher brd, BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack ps, VertexConsumer vc,
            boolean checkSides, RandomSource rand, Object modelData, @Nullable RenderType layer) {
        BakedModel model = brd.getBlockModel(state);
        DynamicModel d = dynamic(model);
        if (d == null) {
            if (layer != null && layer != ItemBlockRenderTypes.getChunkRenderType(state)) return;
            brd.renderBatched(state, pos, level, ps, vc, checkSides, rand);
            return;
        }
        BakedModel quads = new LayerModel((BakedModel) d, d, state, rand, (ModelValues) modelData, layer);
        brd.getModelRenderer().tesselateBlock(level, quads, state, pos, ps, vc, checkSides, rand, state.getSeed(pos), OverlayTexture.NO_OVERLAY);
    }

    @Override
    public void renderSingleBlock(BlockRenderDispatcher brd, BlockState state, PoseStack ps, MultiBufferSource buffers, int light, int overlay,
            @Nullable RenderType layer) {
        BakedModel model = brd.getBlockModel(state);
        DynamicModel d = dynamic(model);
        if (d == null) {
            if (layer != null && layer != ItemBlockRenderTypes.getChunkRenderType(state)) return;
            brd.renderSingleBlock(state, ps, buffers, light, overlay);
            return;
        }
        RandomSource rand = RandomSource.create(42);
        for (RenderType rt : d.layers(state, rand, ModelValues.EMPTY)) {
            if (layer != null && rt != layer) continue;
            BakedModel quads = new LayerModel((BakedModel) d, d, state, rand, ModelValues.EMPTY, rt);
            brd.getModelRenderer().renderModel(ps.last(), buffers.getBuffer(entitySheet(rt)), state, quads, 1F, 1F, 1F, light, overlay);
        }
    }

    private static RenderType entitySheet(RenderType chunkLayer) {
        if (chunkLayer == ChunkSectionLayer.TRANSLUCENT) return Sheets.translucentBlockItemSheet();
        if (chunkLayer == ChunkSectionLayer.SOLID) return Sheets.solidBlockSheet();
        return Sheets.cutoutBlockSheet();
    }

    @Override
    public <T> @Nullable T modelValue(BlockAndTintGetter level, BlockPos pos, ModelKey<T> key) {
        return blockEntityValues(level, pos).get(key);
    }

    @Override
    public BakedModel wrap(DynamicModel model) {
        return new FabricDynamicModel(model);
    }

    @Override
    public List<BakedQuad> quads(BakedModel model, @Nullable BlockState state, @Nullable Direction side, RandomSource rand, @Nullable RenderType layer) {
        DynamicModel d = dynamic(model);
        if (d != null) return d.quads(state, side, rand, ModelValues.EMPTY, layer);
        if (layer != null && state != null && layer != ItemBlockRenderTypes.getChunkRenderType(state)) return List.of();
        return model.getQuads(state, side, rand);
    }

    @Override
    public Collection<RenderType> layers(BakedModel model, BlockState state, RandomSource rand) {
        DynamicModel d = dynamic(model);
        return d != null ? d.layers(state, rand, ModelValues.EMPTY) : List.of(ItemBlockRenderTypes.getChunkRenderType(state));
    }

    @Override
    public BakedModel standaloneModel(Identifier id) {
        return Minecraft.getInstance().getModelManager().getModel(id);
    }

    @Override
    public BakedQuad quad(int[] vertices, int tint, Direction direction, TextureAtlasSprite sprite, boolean shade, boolean ambientOcclusion) {
        return new BakedQuad(vertices, tint, direction, sprite, shade);
    }

    @Override
    public boolean quadAo(BakedQuad quad) {
        return true;
    }

    @Override
    public NativeImage spriteImage(SpriteContents contents) {
        return contents.originalImage;
    }

    /** The quads of one layer of a dynamic model as a plain (vanilla) model. */
    private static final class LayerModel implements BakedModel {
        private final BakedModel base;
        private final List<BakedQuad>[] sides;

        @SuppressWarnings("unchecked")
        LayerModel(BakedModel base, DynamicModel d, BlockState state, RandomSource rand, ModelValues data, @Nullable RenderType layer) {
            this.base = base;
            this.sides = new List[7];
            for (int i = 0; i < 7; i++) {
                Direction side = i < 6 ? SIDES[i] : null;
                sides[i] = new ArrayList<>(d.quads(state, side, rand, data, layer));
            }
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            return sides[side == null ? 6 : side.ordinal()];
        }

        @Override
        public boolean useAmbientOcclusion() {
            return base.useAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return base.isGui3d();
        }

        @Override
        public boolean usesBlockLight() {
            return base.usesBlockLight();
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        @SuppressWarnings("deprecation")
        public TextureAtlasSprite getParticleIcon() {
            return base.getParticleIcon();
        }

        @Override
        @SuppressWarnings("deprecation")
        public ItemTransforms getTransforms() {
            return base.getTransforms();
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }
    }
}
