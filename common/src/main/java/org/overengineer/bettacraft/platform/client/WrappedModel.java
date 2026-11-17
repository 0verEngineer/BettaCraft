package org.overengineer.bettacraft.platform.client;

import java.util.Collection;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.platform.model.ModelValues;

/** M52: a {@link DynamicModel} around a baked model (by default it draws the wrapped model's quads and layers). */
public abstract class WrappedModel implements BakedModel, DynamicModel {
    protected final BakedModel originalModel;

    protected WrappedModel(BakedModel original) {
        this.originalModel = original;
    }

    @Override
    public @Nullable BakedModel original() {
        return originalModel;
    }

    @Override
    public List<BakedQuad> quads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelValues data, @Nullable RenderType layer) {
        return ClientPlatform.get().quads(originalModel, state, side, rand, layer);
    }

    @Override
    public Collection<RenderType> layers(BlockState state, RandomSource rand, ModelValues data) {
        return ClientPlatform.get().layers(originalModel, state, rand);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return quads(state, side, rand, ModelValues.EMPTY, null);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return originalModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return originalModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return originalModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return originalModel.isCustomRenderer();
    }

    @Override
    @SuppressWarnings("deprecation")
    public TextureAtlasSprite getParticleIcon() {
        return originalModel.getParticleIcon();
    }

    @Override
    @SuppressWarnings("deprecation")
    public ItemTransforms getTransforms() {
        return originalModel.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return originalModel.getOverrides();
    }
}
