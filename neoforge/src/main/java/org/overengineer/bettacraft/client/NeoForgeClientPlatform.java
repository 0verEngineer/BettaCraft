package org.overengineer.bettacraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.neoforge.NeoModelData;
import org.overengineer.bettacraft.platform.client.ClientPlatform;
import org.overengineer.bettacraft.platform.client.RenderLevelStageEvent;
import org.overengineer.bettacraft.platform.model.ModelKey;

/** M52: the client platform layer on NeoForge: model data and render types per model, the render stage event. */
public final class NeoForgeClientPlatform implements ClientPlatform {
    @Override
    public Object modelData(BakedModel model, BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return model.getModelData(level, pos, state, ModelData.EMPTY);
    }

    @Override
    public Iterable<RenderType> renderTypes(BakedModel model, BlockState state, RandomSource rand, Object modelData) {
        return model.getRenderTypes(state, rand, (ModelData) modelData);
    }

    @Override
    public void renderBatched(BlockRenderDispatcher brd, BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack ps, VertexConsumer vc,
            boolean checkSides, RandomSource rand, Object modelData, @Nullable RenderType layer) {
        brd.renderBatched(state, pos, level, ps, vc, checkSides, rand, (ModelData) modelData, layer);
    }

    @Override
    public void renderSingleBlock(BlockRenderDispatcher brd, BlockState state, PoseStack ps, MultiBufferSource buffers, int light, int overlay,
            @Nullable RenderType layer) {
        brd.renderSingleBlock(state, ps, buffers, light, overlay, ModelData.EMPTY, layer);
    }

    @Override
    public <T> @Nullable T modelValue(BlockAndTintGetter level, BlockPos pos, ModelKey<T> key) {
        return level.getModelData(pos).get(NeoModelData.prop(key));
    }

    @Override
    public BakedModel wrap(org.overengineer.bettacraft.platform.client.DynamicModel model) {
        return new NeoDynamicModel(model);
    }

    @Override
    public java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> quads(BakedModel model, @Nullable BlockState state,
            @Nullable net.minecraft.core.Direction side, RandomSource rand, @Nullable RenderType layer) {
        return model.getQuads(state, side, rand, ModelData.EMPTY, layer);
    }

    @Override
    public java.util.Collection<RenderType> layers(BakedModel model, BlockState state, RandomSource rand) {
        java.util.List<RenderType> out = new java.util.ArrayList<>(2);
        for (RenderType rt : model.getRenderTypes(state, rand, ModelData.EMPTY)) out.add(rt);
        return out;
    }

    @Override
    public BakedModel standaloneModel(net.minecraft.resources.ResourceLocation id) {
        return net.minecraft.client.Minecraft.getInstance().getModelManager()
                .getModel(net.minecraft.client.resources.model.ModelResourceLocation.standalone(id));
    }

    @Override
    public net.minecraft.client.renderer.block.model.BakedQuad quad(int[] vertices, int tint, net.minecraft.core.Direction direction,
            net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, boolean shade, boolean ambientOcclusion) {
        return new net.minecraft.client.renderer.block.model.BakedQuad(vertices, tint, direction, sprite, shade, ambientOcclusion);
    }

    @Override
    public boolean quadAo(net.minecraft.client.renderer.block.model.BakedQuad quad) {
        return quad.hasAmbientOcclusion();
    }

    @Override
    public com.mojang.blaze3d.platform.NativeImage spriteImage(net.minecraft.client.renderer.texture.SpriteContents contents) {
        return contents.getOriginalImage();
    }

    /** NeoForge's render stage event as the common one. */
    public static RenderLevelStageEvent wrap(net.neoforged.neoforge.client.event.RenderLevelStageEvent e) {
        return new RenderLevelStageEvent(stage(e.getStage()), e.getLevelRenderer(), e.getPoseStack(), e.getModelViewMatrix(), e.getProjectionMatrix(),
                e.getRenderTick(), e.getPartialTick(), e.getCamera(), e.getFrustum());
    }

    private static RenderLevelStageEvent.Stage stage(net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage s) {
        for (RenderLevelStageEvent.Stage ours : RenderLevelStageEvent.Stage.values()) {
            if (STAGES[ours.ordinal()] == s) return ours;
        }
        return RenderLevelStageEvent.Stage.AFTER_LEVEL; // a stage we never draw at
    }

    private static final net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage[] STAGES;

    static {
        var S = RenderLevelStageEvent.Stage.values();
        STAGES = new net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage[S.length];
        for (RenderLevelStageEvent.Stage ours : S) {
            STAGES[ours.ordinal()] = switch (ours) {
                case AFTER_SKY -> net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_SKY;
                case AFTER_SOLID_BLOCKS -> net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS;
                case AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS -> net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS;
                case AFTER_CUTOUT_BLOCKS -> net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS;
                case AFTER_ENTITIES -> net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_ENTITIES;
                case AFTER_BLOCK_ENTITIES -> net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES;
                case AFTER_TRANSLUCENT_BLOCKS -> net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS;
                case AFTER_TRIPWIRE_BLOCKS -> net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS;
                case AFTER_PARTICLES -> net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES;
                case AFTER_WEATHER -> net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_WEATHER;
                case AFTER_LEVEL -> net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_LEVEL;
            };
        }
    }
}
