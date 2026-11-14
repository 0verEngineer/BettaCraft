package org.overengineer.bettacraft.client.interior;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.overengineer.bettacraft.plants.AquaticPlantBlock;
import org.overengineer.bettacraft.plants.PlantPart;
import org.overengineer.bettacraft.registry.ModBlocks;

/**
 * M21 plant prototype: the blocks of Rotala, Vallisneria and Anubias stay as they are (collision, the outline for the
 * shears, growth, the sync to the miniature), but in the tank interior they draw nothing while
 * {@code features.plantMeshes} is on: {@link PlantMeshRenderer} draws the plant with real leaves from its root instead.
 * The miniature outside the tank still gets the block model (its builder passes a {@link RegionBlockGetter}): since
 * M21.1 it keeps those voxels apart and shows them only beyond the LOD distance; closer, the tank draws the meshes
 * ({@link MiniaturePlants}). While the chunk renderer asks for the model data, the roots are noted for
 * {@link PlantMeshRenderer}. The planted cutting is not wrapped.
 */
public final class PlantMeshModel extends BakedModelWrapper<BakedModel> {
    public PlantMeshModel(BakedModel original) {
        super(original);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
        ModelData d = super.getModelData(level, pos, state, data);
        if (level instanceof RegionBlockGetter) return d.derive().with(PlantMeshRenderer.MINIATURE, Boolean.TRUE).build();
        if (state.getBlock() instanceof AquaticPlantBlock) {
            if (state.getValue(AquaticPlantBlock.PART) == PlantPart.ROOT) PlantMeshRenderer.noteRoot(pos);
            PlantMeshRenderer.noteCompiled(pos); // the section was compiled again (e.g. its light changed): relight its plants
        }
        return d;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        if (Boolean.TRUE.equals(data.get(PlantMeshRenderer.MINIATURE)) || !PlantMeshRenderer.active()) {
            return super.getQuads(state, side, rand, data, renderType);
        }
        return List.of();
    }

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (var e : ModBlocks.AQUATIC_PLANTS.entrySet()) {
            if (!e.getKey().meshed()) continue;
            for (BlockState s : e.getValue().get().getStateDefinition().getPossibleStates()) {
                if (s.getValue(AquaticPlantBlock.PART) == PlantPart.CUTTING) continue;
                ModelResourceLocation loc = BlockModelShaper.stateToModelLocation(s);
                BakedModel m = event.getModels().get(loc);
                if (m != null && !(m instanceof PlantMeshModel)) event.getModels().put(loc, new PlantMeshModel(m));
            }
        }
    }
}
