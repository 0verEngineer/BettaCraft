package org.overengineer.bettacraft.client.model;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.client.ClientPlatform;
import org.overengineer.bettacraft.platform.client.WrappedModel;
import org.overengineer.bettacraft.platform.model.ModelValues;
import org.overengineer.bettacraft.plumbing.FilterPipeBlockEntity;
import org.overengineer.bettacraft.registry.ModBlocks;

/**
 * M40: the model of the filter pipes. Without a cover it is the pipe's own multipart model; with a cover (a facade, set by
 * part c through {@link FilterPipeBlockEntity#COVER}) the pipe is drawn as that block, like the substrate of the interior
 * floor.
 */
public final class FilterPipeModel extends WrappedModel {
    public FilterPipeModel(BlockStateModel original) {
        super(original);
    }

    @Nullable
    private static BlockState cover(ModelValues data) {
        return data.get(FilterPipeBlockEntity.COVER);
    }

    @Override
    public List<BakedQuad> quads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelValues data, @Nullable ChunkSectionLayer renderType) {
        BlockState c = cover(data);
        if (c == null) return super.quads(state, side, rand, data, renderType);
        return ClientPlatform.get().quads(Minecraft.getInstance().getBlockRenderer().getBlockModel(c), c, side, rand, renderType);
    }

    @Override
    public java.util.Collection<ChunkSectionLayer> layers(BlockState state, RandomSource rand, ModelValues data) {
        BlockState c = cover(data);
        if (c == null) return super.layers(state, rand, data);
        return ClientPlatform.get().layers(Minecraft.getInstance().getBlockRenderer().getBlockModel(c), c, rand);
    }

    @Override
    public net.minecraft.client.renderer.texture.TextureAtlasSprite particle(ModelValues data) {
        BlockState c = cover(data);
        return c == null ? super.particle(data) : Minecraft.getInstance().getBlockRenderer().getBlockModel(c).particleIcon();
    }
}
