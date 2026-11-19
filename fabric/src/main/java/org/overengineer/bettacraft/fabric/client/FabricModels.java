package org.overengineer.bettacraft.fabric.client;

import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import org.overengineer.bettacraft.client.model.ModelSwaps;

/**
 * M52: our dynamic models on Fabric: the extra models and the swaps after baking ({@link ModelSwaps}, as on NeoForge).
 * M52b (1.21.11): block models are per state; the tank is one of the swaps (no geometry loader any more).
 */
final class FabricModels {
    private FabricModels() {}

    static void register() {
        ModelLoadingPlugin.register(ctx -> {
            ModelSwaps.beforeBake();
            for (var id : ModelSwaps.extraModels()) {
                ExtraModelKey<BlockStateModel> key = FabricClientPlatform.STANDALONE.computeIfAbsent(id, k -> ExtraModelKey.create(k::toString));
                ctx.addModel(key, SimpleUnbakedExtraModel.blockStateModel(id));
            }
            ctx.modifyBlockModelAfterBake().register((model, c) -> {
                if (model == null) return model;
                // M52c (26.1): the sprites through the baker's materials (block atlas)
                var materials = c.baker().materials();
                BlockStateModel swapped = ModelSwaps.swap(c.state(), model,
                        id -> materials.get(new net.minecraft.client.resources.model.sprite.Material(id), () -> "bettacraft:" + id).sprite());
                return swapped != null ? swapped : model;
            });
        });
    }
}
