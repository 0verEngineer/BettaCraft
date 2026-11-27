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
import org.overengineer.bettacraft.registry.ModBlocks;

/**
 * M52: which baked models of our blocks are replaced by our dynamic models after baking (NeoForge: ModifyBakingResult;
 * Fabric: the model loading plugin's after-bake modifier), the same on both loaders: cabinets (planks or stone of the
 * block entity), filter pipes and bundles (covers, arms), the interior floor (its substrate) and the meshed plants.
 */
public final class ModelSwaps {
    private static volatile Map<ModelResourceLocation, Function<BakedModel, DynamicModel>> swaps;

    private ModelSwaps() {}

    /** The extra (standalone) models to load (M68: none; the bundle's arms are made in code, {@link PipeQuads}). */
    public static List<ResourceLocation> extraModels() {
        return new ArrayList<>();
    }

    /** Before a new bake: forget sprites and the list (the blocks exist by then). */
    public static void beforeBake() {
        CabinetBakedModel.clearSprites();
        PipeQuads.clearCache(); // M68: the pipe quads hold the sprites of the old atlas
        org.overengineer.bettacraft.client.tank.TankFlanges.clearCache(); // M65: the bulkhead quads hold the sprite of the old atlas
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
        for (Block b : List.of(ModBlocks.GLASS_PIPE.get(), ModBlocks.STAINLESS_STEEL_PIPE.get())) { // M68: covers and wrench caps on every pipe
            for (BlockState s : b.getStateDefinition().getPossibleStates()) m.put(BlockModelShaper.stateToModelLocation(s), PipeModel::new);
        }
        for (BlockState s : ModBlocks.FILTER_PIPE_BUNDLE.get().getStateDefinition().getPossibleStates()) {
            m.put(BlockModelShaper.stateToModelLocation(s), PipeBundleModel::new);
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
