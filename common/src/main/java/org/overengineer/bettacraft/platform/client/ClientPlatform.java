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

    final class Instance {
        private static final ClientPlatform PLATFORM = ServiceLoader.load(ClientPlatform.class, ClientPlatform.class.getClassLoader()).findFirst()
                .orElseThrow(() -> new IllegalStateException("Bettacraft: no client platform implementation"));

        private Instance() {}
    }
}
