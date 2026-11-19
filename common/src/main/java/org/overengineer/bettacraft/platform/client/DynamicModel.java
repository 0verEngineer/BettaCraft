package org.overengineer.bettacraft.platform.client;

import java.util.Collection;
import java.util.List;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.platform.model.ModelValues;

/**
 * M52: a block model of ours whose quads depend on the render data of its block entity ({@link ModelValues}) and on the
 * neighbours. The loaders draw it through their own adapter ({@link ClientPlatform#wrap}): NeoForge's model data and
 * render types, Fabric's renderer API (one material per render layer).
 */
public interface DynamicModel {
    /** The render data while the block is meshed: the block entity's values plus what the model reads from the level. */
    default ModelValues modelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelValues data) {
        return data;
    }

    /** The quads of one side (null: the unculled ones) in one render layer (null: all layers). */
    List<BakedQuad> quads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelValues data, @Nullable ChunkSectionLayer layer);

    /** The chunk render layers the block is drawn in. */
    Collection<ChunkSectionLayer> layers(@org.jetbrains.annotations.Nullable BlockState state, RandomSource rand, ModelValues data);

    /** The particle sprite (breaking particles). */
    TextureAtlasSprite particle(ModelValues data);

    /** Whether the parts use ambient occlusion (the loader's adapter asks). */
    default boolean ambientOcclusion() {
        return true;
    }

    /** The model this one wraps, or null. */
    default @Nullable BlockStateModel original() {
        return null;
    }
}
