package org.overengineer.bettacraft.platform.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.platform.model.ModelKey;

/**
 * M52: what the client code needs from the loader's rendering (NeoForge: model data and render types per model part;
 * Fabric: its renderer API). One implementation per loader ({@code META-INF/services/...ClientPlatform}); only used on the
 * client. M52b (1.21.11): block models are {@link BlockStateModel}s made of parts, the chunk layers are
 * {@link ChunkSectionLayer}s; quads keep the 1.21.1 vertex layout ({@link QuadBaker#STRIDE} ints per vertex: x, y, z,
 * ABGR colour, u, v, light, normal) so the model code of the mod is unchanged.
 */
public interface ClientPlatform {
    static ClientPlatform get() {
        return Instance.PLATFORM;
    }

    /** The data a model draws a block with (an opaque object for {@link #tesselate}). */
    Object modelData(BlockStateModel model, BlockAndTintGetter level, BlockPos pos, BlockState state);

    /**
     * Tesselates a block into the buffer of each chunk layer it draws in ({@code buffers}; the miniature's mesher), with
     * face culling against {@code level} when {@code checkSides}. Only the layer {@code only} when not null.
     */
    void tesselate(BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack ps, Function<ChunkSectionLayer, VertexConsumer> buffers,
            boolean checkSides, RandomSource rand, Object modelData, @Nullable ChunkSectionLayer only);

    /** A value of the render data a block entity at {@code pos} gave its model, or null. */
    <T> @Nullable T modelValue(BlockAndTintGetter level, BlockPos pos, ModelKey<T> key);

    /** M52: the loader's adapter that draws one of our dynamic models (NeoForge model data, Fabric's renderer API). */
    BlockStateModel wrap(DynamicModel model);

    /** M52b: our dynamic model inside a model made by {@link #wrap}, or null for any other model. */
    @Nullable DynamicModel dynamic(BlockStateModel model);

    /** The quads of a (foreign) model without render data, of one side (null: unculled) and chunk layer (null: all). */
    List<BakedQuad> quads(BlockStateModel model, @Nullable BlockState state, @Nullable Direction side, RandomSource rand,
            @Nullable ChunkSectionLayer layer);

    /** The chunk layers of a (foreign) model without render data. */
    Collection<ChunkSectionLayer> layers(BlockStateModel model, BlockState state, RandomSource rand);

    /** A model registered as an extra (standalone) model ({@code client.model.ModelSwaps#EXTRA}). */
    BlockStateModel standaloneModel(net.minecraft.resources.Identifier id);

    /** A baked quad from the 1.21.1 vertex layout (NeoForge's carries its colours, normals and ambient occlusion flag). */
    BakedQuad quad(int[] vertices, int tint, Direction direction, TextureAtlasSprite sprite, boolean shade, boolean ambientOcclusion);

    /** The 1.21.1 vertex layout of a quad (positions, colours, UVs and normals as far as the loader keeps them). */
    int[] vertices(BakedQuad quad);

    /** The ambient occlusion flag of a quad (NeoForge), true elsewhere. */
    boolean quadAo(BakedQuad quad);

    /** The pixels of a sprite as loaded (frame 0 on top). */
    com.mojang.blaze3d.platform.NativeImage spriteImage(net.minecraft.client.renderer.texture.SpriteContents contents);

    final class Instance {
        private static final ClientPlatform PLATFORM = ServiceLoader.load(ClientPlatform.class, ClientPlatform.class.getClassLoader()).findFirst()
                .orElseThrow(() -> new IllegalStateException("Bettacraft: no client platform implementation"));

        private Instance() {}
    }
}
