package org.overengineer.bettacraft.fabric.client;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.model.ModelSwaps;
import org.overengineer.bettacraft.client.tank.TankBakedModel;
import org.overengineer.bettacraft.registry.ModBlocks;
import org.overengineer.bettacraft.tank.TankStyle;

/**
 * M52: our dynamic models on Fabric: the extra models and the swaps after baking ({@link ModelSwaps}, as on NeoForge),
 * and the tank, whose model NeoForge builds with its geometry loader ({@code "loader": "bettacraft:tank"}, ignored by
 * vanilla's model reader: the JSON stays a plain parent model here and is replaced after baking).
 */
final class FabricModels {
    private FabricModels() {}

    static void register() {
        ModelLoadingPlugin.register(ctx -> {
            ModelSwaps.beforeBake();
            ctx.addModels(ModelSwaps.extraModels());
            ctx.modifyModelAfterBake().register((model, c) -> {
                ModelResourceLocation loc = c.topLevelId();
                if (model == null || loc == null) return model;
                if (isTank(loc)) return tank(model, c.textureGetter());
                BakedModel swapped = ModelSwaps.swap(loc, model);
                return swapped != null ? swapped : model;
            });
        });
    }

    private static volatile java.util.Set<ModelResourceLocation> tankModels;

    private static boolean isTank(ModelResourceLocation loc) {
        java.util.Set<ModelResourceLocation> set = tankModels;
        if (set == null) {
            set = new java.util.HashSet<>();
            for (BlockState s : ModBlocks.TANK.get().getStateDefinition().getPossibleStates()) set.add(BlockModelShaper.stateToModelLocation(s));
            set.add(ModelResourceLocation.inventory(BettaCraft.id("tank")));
            tankModels = set;
        }
        return set.contains(loc);
    }

    private static BakedModel tank(BakedModel parsed, java.util.function.Function<Material, net.minecraft.client.renderer.texture.TextureAtlasSprite> sprites) {
        var edge = sprites.apply(new Material(InventoryMenu.BLOCK_ATLAS, BettaCraft.id("block/tank_edge")));
        var clear = sprites.apply(new Material(InventoryMenu.BLOCK_ATLAS, BettaCraft.id("block/tank_clear_icon")));
        var particle = sprites.apply(new Material(InventoryMenu.BLOCK_ATLAS, Identifier.withDefaultNamespace("block/glass")));
        @SuppressWarnings("deprecation")
        var transforms = parsed.getTransforms();
        return new FabricDynamicModel(new TankBakedModel(edge, clear, particle, transforms, null, TankStyle.TEXTURED, null));
    }
}
