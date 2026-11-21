package org.overengineer.bettacraft.mixin.interior;

import net.minecraft.server.MinecraftServer;
import org.overengineer.bettacraft.tank.interior.InteriorCleanup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * M53c: after a complete save of the world (autosave, {@code /save-all}, the stop) the interior regions removed before are
 * dead: nothing on the disk points to them any more, so the clean-up may delete their chunks ({@link InteriorCleanup}).
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerSaveMixin {
    @Inject(method = "saveAllChunks", at = @At("RETURN"))
    private void bettacraft$worldSaved(boolean suppressLog, boolean flush, boolean forced, CallbackInfoReturnable<Boolean> cir) {
        InteriorCleanup.worldSaved((MinecraftServer) (Object) this, forced);
    }
}
