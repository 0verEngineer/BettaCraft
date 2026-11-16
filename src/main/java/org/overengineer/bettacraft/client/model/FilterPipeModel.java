package org.overengineer.bettacraft.client.model;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.overengineer.bettacraft.plumbing.FilterPipeBlockEntity;
import org.overengineer.bettacraft.registry.ModBlocks;

/**
 * M40: the model of the filter pipes. Without a cover it is the pipe's own multipart model; with a cover (a facade, set by
 * part c through {@link FilterPipeBlockEntity#COVER}) the pipe is drawn as that block, like the substrate of the interior
 * floor.
 */
public final class FilterPipeModel extends BakedModelWrapper<BakedModel> {
    public FilterPipeModel(BakedModel original) {
        super(original);
    }

    @Nullable
    private static BlockState cover(ModelData data) {
        return data.get(FilterPipeBlockEntity.COVER);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        BlockState c = cover(data);
        if (c == null) return super.getQuads(state, side, rand, data, renderType);
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(c).getQuads(c, side, rand, ModelData.EMPTY, renderType);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        BlockState c = cover(data);
        if (c == null) return super.getRenderTypes(state, rand, data);
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(c).getRenderTypes(c, rand, ModelData.EMPTY);
    }

    @Override
    public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon(ModelData data) {
        BlockState c = cover(data);
        return c == null ? super.getParticleIcon(data) : Minecraft.getInstance().getBlockRenderer().getBlockModel(c).getParticleIcon(ModelData.EMPTY);
    }

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (Block b : List.of(ModBlocks.FILTER_INLET_PIPE.get(), ModBlocks.FILTER_OUTLET_PIPE.get(), ModBlocks.FILTER_FRESH_PIPE.get(),
                ModBlocks.FILTER_WASTE_PIPE.get())) {
            for (BlockState s : b.getStateDefinition().getPossibleStates()) {
                ModelResourceLocation loc = BlockModelShaper.stateToModelLocation(s);
                BakedModel m = event.getModels().get(loc);
                if (m != null && !(m instanceof FilterPipeModel)) event.getModels().put(loc, new FilterPipeModel(m));
            }
        }
    }
}
