package org.overengineer.bettacraft.client.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.interior.PlantMeshModel;
import org.overengineer.bettacraft.client.interior.SubstrateFloorModel;
import org.overengineer.bettacraft.plants.AquaticPlantBlock;
import org.overengineer.bettacraft.plants.PlantPart;
import org.overengineer.bettacraft.platform.client.ClientPlatform;
import org.overengineer.bettacraft.platform.client.DynamicModel;
import org.overengineer.bettacraft.plumbing.PipeLine;
import org.overengineer.bettacraft.registry.ModBlocks;

/**
 * M52: which baked models of our blocks are replaced by our dynamic models after baking (NeoForge: ModifyBakingResult;
 * Fabric: the model loading plugin's after-bake modifier), the same on both loaders: cabinets (planks or stone of the
 * block entity), filter pipes and bundles (covers, arms), the interior floor (its substrate) and the meshed plants.
 */
public final class ModelSwaps {
    private static volatile Map<ModelResourceLocation, Function<BakedModel, DynamicModel>> swaps;

    private ModelSwaps() {}

    /** The extra (standalone) models to load: the arms of the pipe bundle. */
    public static List<ResourceLocation> extraModels() {
        List<ResourceLocation> out = new ArrayList<>();
        for (PipeLine l : PipeLine.values()) for (Direction d : Direction.values()) for (boolean tee : new boolean[]{false, true}) {
            out.add(FilterBundleModel.armModel(l, d, tee));
            out.add(FilterBundleModel.armModel(l, d, tee, true)); // M65: the arm in the middle of the face (towards a single pipe)
        }
        return out;
    }

    /** Before a new bake: forget sprites and the list (the blocks exist by then). */
    public static void beforeBake() {
        CabinetBakedModel.clearSprites();
    }

    /** The model that replaces the baked one at {@code loc}, or null to keep it. */
    public static @Nullable BakedModel swap(ModelResourceLocation loc, BakedModel baked) {
        if (baked instanceof DynamicModel) return null;
        Function<BakedModel, DynamicModel> f = targets().get(loc);
        if (f == null) return null;
        return ClientPlatform.get().wrap(f.apply(baked));
    }

    private static Map<ModelResourceLocation, Function<BakedModel, DynamicModel>> targets() {
        Map<ModelResourceLocation, Function<BakedModel, DynamicModel>> m = swaps;
        if (m != null) return m;
        m = new HashMap<>();
        for (BlockState s : ModBlocks.CABINET.get().getStateDefinition().getPossibleStates()) {
            m.put(BlockModelShaper.stateToModelLocation(s), b -> new CabinetBakedModel(b, null, null));
        }
        m.put(ModelResourceLocation.inventory(BettaCraft.id("cabinet")), b -> new CabinetBakedModel(b, null, null));
        for (Block b : List.of(ModBlocks.FILTER_INLET_PIPE.get(), ModBlocks.FILTER_OUTLET_PIPE.get(), ModBlocks.FILTER_FRESH_PIPE.get(),
                ModBlocks.FILTER_WASTE_PIPE.get())) {
            for (BlockState s : b.getStateDefinition().getPossibleStates()) m.put(BlockModelShaper.stateToModelLocation(s), FilterPipeModel::new);
        }
        for (BlockState s : ModBlocks.FILTER_PIPE_BUNDLE.get().getStateDefinition().getPossibleStates()) {
            m.put(BlockModelShaper.stateToModelLocation(s), FilterBundleModel::new);
        }
        for (BlockState s : ModBlocks.INTERIOR_FLOOR.get().getStateDefinition().getPossibleStates()) {
            m.put(BlockModelShaper.stateToModelLocation(s), SubstrateFloorModel::new);
        }
        for (var e : ModBlocks.AQUATIC_PLANTS.entrySet()) {
            if (!e.getKey().meshed()) continue;
            for (BlockState s : e.getValue().get().getStateDefinition().getPossibleStates()) {
                if (s.getValue(AquaticPlantBlock.PART) == PlantPart.CUTTING) continue;
                m.put(BlockModelShaper.stateToModelLocation(s), PlantMeshModel::new);
            }
        }
        swaps = m;
        return m;
    }
}
