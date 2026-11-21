package org.overengineer.bettacraft.platform.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ServiceLoader;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.platform.model.ModelKey;

/**
 * M52: what the client code needs from the loader's rendering (NeoForge: model data and render types per model; Fabric:
 * its renderer API). One implementation per loader ({@code META-INF/services/...ClientPlatform}); only used on the client.
 */
public interface ClientPlatform {
    static ClientPlatform get() {
        return Instance.PLATFORM;
    }

    /** The data a model draws a block with (an opaque object for {@link #renderTypes} and {@link #renderBatched}). */
    Object modelData(BakedModel model, BlockAndTintGetter level, BlockPos pos, BlockState state);

    /** The render layers a model draws a block in. */
    Iterable<RenderType> renderTypes(BakedModel model, BlockState state, RandomSource rand, Object modelData);

    /** Tesselates a block into a buffer (one layer, or all with null). */
    void renderBatched(BlockRenderDispatcher brd, BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack ps, VertexConsumer vc,
            boolean checkSides, RandomSource rand, Object modelData, @Nullable RenderType layer);

    /** Draws a block like an item in the world (no model data), in one layer. */
    void renderSingleBlock(BlockRenderDispatcher brd, BlockState state, PoseStack ps, MultiBufferSource buffers, int light, int overlay,
            @Nullable RenderType layer);

    /** A value of the render data a block entity at {@code pos} gave its model, or null. */
    <T> @Nullable T modelValue(BlockAndTintGetter level, BlockPos pos, ModelKey<T> key);

    /** M52: the loader's adapter that draws one of our dynamic models (NeoForge model data, Fabric's renderer API). */
    BakedModel wrap(DynamicModel model);

    /** The quads of a (foreign) model without render data, of one render layer (null: all). */
    java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> quads(BakedModel model, @Nullable BlockState state,
            @Nullable net.minecraft.core.Direction side, RandomSource rand, @Nullable RenderType layer);

    /** The chunk render layers of a (foreign) model without render data. */
    java.util.Collection<RenderType> layers(BakedModel model, BlockState state, RandomSource rand);

    /** A model registered as an extra (standalone) model ({@code client.model.ModelSwaps#EXTRA}). */
    BakedModel standaloneModel(net.minecraft.resources.ResourceLocation id);

    /** A baked quad (NeoForge's carries its own ambient occlusion flag). */
    net.minecraft.client.renderer.block.model.BakedQuad quad(int[] vertices, int tint, net.minecraft.core.Direction direction,
            net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, boolean shade, boolean ambientOcclusion);

    /** The ambient occlusion flag of a quad (NeoForge), true elsewhere. */
    boolean quadAo(net.minecraft.client.renderer.block.model.BakedQuad quad);

    /** The pixels of a sprite as loaded (frame 0 on top). */
    com.mojang.blaze3d.platform.NativeImage spriteImage(net.minecraft.client.renderer.texture.SpriteContents contents);

    /**
     * M53: the screen of Bettacraft's client config (NeoForge's config screen, on Fabric the same one of Forge Config API
     * Port), returning to {@code parent}; null when there is none.
     */
    @Nullable
    net.minecraft.client.gui.screens.Screen clientConfigScreen(net.minecraft.client.gui.screens.Screen parent);

    final class Instance {
        private static final ClientPlatform PLATFORM = ServiceLoader.load(ClientPlatform.class, ClientPlatform.class.getClassLoader()).findFirst()
                .orElseThrow(() -> new IllegalStateException("Bettacraft: no client platform implementation"));

        private Instance() {}
    }
}
