package org.overengineer.bettacraft.platform.client;

import java.util.Collection;
import java.util.List;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.platform.model.ModelValues;

/** M52: a {@link DynamicModel} around a block model (by default it draws the wrapped model's quads and layers). */
public abstract class WrappedModel implements DynamicModel {
    protected final BlockStateModel originalModel;

    protected WrappedModel(BlockStateModel original) {
        this.originalModel = original;
    }

    @Override
    public @Nullable BlockStateModel original() {
        return originalModel;
    }

    @Override
    public List<BakedQuad> quads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelValues data, @Nullable ChunkSectionLayer layer) {
        return ClientPlatform.get().quads(originalModel, state, side, rand, layer);
    }

    @Override
    public Collection<ChunkSectionLayer> layers(BlockState state, RandomSource rand, ModelValues data) {
        return ClientPlatform.get().layers(originalModel, state, rand);
    }

    @Override
    @SuppressWarnings("deprecation")
    public TextureAtlasSprite particle(ModelValues data) {
        return originalModel.particleMaterial().sprite();
    }
}
