package org.overengineer.bettacraft.client.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.interior.PlantMeshModel;
import org.overengineer.bettacraft.client.interior.SubstrateFloorModel;
import org.overengineer.bettacraft.client.tank.TankBakedModel;
import org.overengineer.bettacraft.plants.AquaticPlantBlock;
import org.overengineer.bettacraft.plants.PlantPart;
import org.overengineer.bettacraft.platform.client.ClientPlatform;
import org.overengineer.bettacraft.platform.client.DynamicModel;
import org.overengineer.bettacraft.plumbing.PipeLine;
import org.overengineer.bettacraft.registry.ModBlocks;

/**
 * M52: which baked models of our blocks are replaced by our dynamic models after baking (NeoForge: ModifyBakingResult;
 * Fabric: the model loading plugin's after-bake modifier), the same on both loaders: cabinets (planks or stone of the
 * block entity; the item: {@code client.item.BlockModelItem}), filter pipes and bundles (covers, arms), the interior floor (its substrate) and the meshed plants.
 */
public final class ModelSwaps {
    private static volatile Map<BlockState, Function<BlockStateModel, DynamicModel>> swaps;

    private ModelSwaps() {}

    /** The extra (standalone) models to load: the arms of the pipe bundle. */
    public static List<Identifier> extraModels() {
        List<Identifier> out = new ArrayList<>();
        for (PipeLine l : PipeLine.values()) for (Direction d : Direction.values()) for (boolean tee : new boolean[]{false, true}) {
            out.add(FilterBundleModel.armModel(l, d, tee));
            out.add(FilterBundleModel.armModel(l, d, tee, true)); // M65: the arm in the middle of the face (towards a single pipe)
        }
        return out;
    }

    /** Before a new bake: forget sprites and the list (the blocks exist by then). */
    public static void beforeBake() {
        CabinetBakedModel.clearSprites();
        tank = null;
    }

    /**
     * The model that replaces the baked one of a block state (M52b: models are per state since 1.21.5), or null to keep it.
     * {@code sprites}: the block atlas after stitching (the tank's edge and icon textures).
     */
    public static @Nullable BlockStateModel swap(BlockState state, BlockStateModel baked,
            Function<Identifier, net.minecraft.client.renderer.texture.TextureAtlasSprite> sprites) {
        if (baked instanceof DynamicModel || ClientPlatform.get().dynamic(baked) != null) return null;
        if (state.is(ModBlocks.TANK.get())) {
            // M52b: the tank on both loaders (1.21.1 NeoForge: its geometry loader "bettacraft:tank"); one model for all states
            TankBakedModel t = tank;
            if (t == null) tank = t = new TankBakedModel(sprites.apply(BettaCraft.id("block/tank_edge")), sprites.apply(BettaCraft.id("block/tank_clear_icon")),
                    sprites.apply(Identifier.withDefaultNamespace("block/glass")), null, org.overengineer.bettacraft.tank.TankStyle.TEXTURED, null);
            return ClientPlatform.get().wrap(t);
        }
        Function<BlockStateModel, DynamicModel> f = targets().get(state);
        if (f == null) return null;
        return ClientPlatform.get().wrap(f.apply(baked));
    }

    private static volatile @Nullable TankBakedModel tank;

    private static Map<BlockState, Function<BlockStateModel, DynamicModel>> targets() {
        Map<BlockState, Function<BlockStateModel, DynamicModel>> m = swaps;
        if (m != null) return m;
        m = new HashMap<>();
        for (BlockState s : ModBlocks.CABINET.get().getStateDefinition().getPossibleStates()) {
            m.put(s, b -> new CabinetBakedModel(b, null));
        }
        for (Block b : List.of(ModBlocks.FILTER_INLET_PIPE.get(), ModBlocks.FILTER_OUTLET_PIPE.get(), ModBlocks.FILTER_FRESH_PIPE.get(),
                ModBlocks.FILTER_WASTE_PIPE.get())) {
            for (BlockState s : b.getStateDefinition().getPossibleStates()) m.put(s, FilterPipeModel::new);
        }
        for (BlockState s : ModBlocks.FILTER_PIPE_BUNDLE.get().getStateDefinition().getPossibleStates()) {
            m.put(s, FilterBundleModel::new);
        }
        for (BlockState s : ModBlocks.INTERIOR_FLOOR.get().getStateDefinition().getPossibleStates()) {
            m.put(s, SubstrateFloorModel::new);
        }
        for (var e : ModBlocks.AQUATIC_PLANTS.entrySet()) {
            if (!e.getKey().meshed()) continue;
            for (BlockState s : e.getValue().get().getStateDefinition().getPossibleStates()) {
                if (s.getValue(AquaticPlantBlock.PART) == PlantPart.CUTTING) continue;
                m.put(s, PlantMeshModel::new);
            }
        }
        swaps = m;
        return m;
    }
}
