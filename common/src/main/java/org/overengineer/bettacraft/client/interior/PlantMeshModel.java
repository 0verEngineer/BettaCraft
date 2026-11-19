package org.overengineer.bettacraft.client.interior;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.platform.client.WrappedModel;
import org.overengineer.bettacraft.platform.model.ModelValues;
import org.overengineer.bettacraft.plants.AquaticPlantBlock;
import org.overengineer.bettacraft.plants.PlantPart;
import org.overengineer.bettacraft.registry.ModBlocks;

/**
 * M21 plant prototype: the blocks of Rotala, Vallisneria and Anubias stay as they are (collision, the outline for the
 * shears, growth, the sync to the miniature), but in the tank interior they draw nothing while
 * {@code features.visuals.plantMeshes} is on: {@link PlantMeshRenderer} draws the plant with real leaves from its root instead.
 * The miniature outside the tank still gets the block model (its builder passes a {@link RegionBlockGetter}): since
 * M21.1 it keeps those voxels apart and shows them only beyond the LOD distance; closer, the tank draws the meshes
 * ({@link MiniaturePlants}). While the chunk renderer asks for the model data, the roots are noted for
 * {@link PlantMeshRenderer}. The planted cutting is not wrapped.
 */
public final class PlantMeshModel extends WrappedModel {
    public PlantMeshModel(BlockStateModel original) {
        super(original);
    }

    @Override
    public ModelValues modelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelValues data) {
        ModelValues d = data;
        if (level instanceof RegionBlockGetter) return d.derive().with(PlantMeshRenderer.MINIATURE, Boolean.TRUE).build();
        if (state.getBlock() instanceof AquaticPlantBlock) {
            if (state.getValue(AquaticPlantBlock.PART) == PlantPart.ROOT) PlantMeshRenderer.noteRoot(pos);
            PlantMeshRenderer.noteCompiled(pos); // the section was compiled again (e.g. its light changed): relight its plants
        }
        return d;
    }

    @Override
    public List<BakedQuad> quads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelValues data, @Nullable ChunkSectionLayer renderType) {
        if (Boolean.TRUE.equals(data.get(PlantMeshRenderer.MINIATURE)) || !PlantMeshRenderer.active()) {
            return super.quads(state, side, rand, data, renderType);
        }
        return List.of();
    }
}
