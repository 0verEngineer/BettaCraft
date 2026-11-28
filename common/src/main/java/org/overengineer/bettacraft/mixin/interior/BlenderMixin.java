package org.overengineer.bettacraft.mixin.interior;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.overengineer.bettacraft.registry.ModDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The void interior dimension has no terrain to blend. Since 1.21.6 every chunk saved by an older version counts as old
 * (DataVersion below 4295), so a new interior chunk next to the interior chunks of a 1.21.1 world started the blending,
 * which reads chunks around it outside the small pyramid of {@link ChunkGenerationTaskMixin} ("Requested chunk
 * unavailable during world generation", the server hung). Interior chunks are generated without blending.
 */
@Mixin(Blender.class)
public abstract class BlenderMixin {
    @Inject(method = "of", at = @At("HEAD"), cancellable = true)
    private static void bettacraft$noInteriorBlending(WorldGenRegion region, CallbackInfoReturnable<Blender> cir) {
        if (region != null && region.getLevel().dimension() == ModDimensions.TANK_INTERIOR) cir.setReturnValue(Blender.empty());
    }
}
