package org.overengineer.bettacraft.mixin.client;

import net.minecraft.client.Options;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * M54, development only ({@code -Dbettacraft.background=true}: every client smoke run, {@code -Pbackground}, the multiplayer
 * test clients; {@code -Dbettacraft.silent=true}: every dev client of the build unless {@code -Psound}): the game is silent (every sound category at 0) without changing the saved options, so a client in the
 * background never plays music or sounds while the developer works. A normal game (no such property) is untouched.
 */
@Mixin(Options.class)
public abstract class BackgroundSoundMixin {
    private static boolean logged;
    private static final boolean SILENT = Boolean.getBoolean("bettacraft.background") || Boolean.getBoolean("bettacraft.silent");

    @Inject(method = "getSoundSourceVolume", at = @At("HEAD"), cancellable = true, require = 0)
    private void bettacraft$silent(SoundSource source, CallbackInfoReturnable<Float> cir) {
        if (!SILENT) return;
        if (!logged) { // the smoke runs check this line
            logged = true;
            org.slf4j.LoggerFactory.getLogger("bettacraft/background").info("[background] sound muted (dev client)");
        }
        cir.setReturnValue(0F);
    }
}
