package org.overengineer.bettacraft.mixin.client;

import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import org.overengineer.bettacraft.client.interior.PlantMeshRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * M21 follow-up: the client's own light engine changed the light of a section (a block placed or removed next to a
 * plant, a lamp switched): the plant meshes there read their light values again ({@link PlantMeshRenderer}). Optional
 * ({@code require = 0}): a mod replacing the light engine only costs the event, recompiled sections still relight.
 */
@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheMixin {
    @Inject(method = "onLightUpdate", at = @At("HEAD"), require = 0)
    private void bettacraft$plantMeshLight(LightLayer layer, SectionPos pos, CallbackInfo ci) {
        PlantMeshRenderer.onSectionLight(pos.x(), pos.y(), pos.z());
    }
}
