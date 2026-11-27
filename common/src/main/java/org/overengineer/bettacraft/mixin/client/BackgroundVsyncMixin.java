package org.overengineer.bettacraft.mixin.client;

import com.mojang.blaze3d.systems.GpuSurface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * M67.5, development only ({@code -Dbettacraft.novsync=true}: every dev client of the build unless {@code -Pvsync};
 * {@code -Dbettacraft.background=true}: every smoke, tour and multiplayer test client): the window never waits for
 * VSync, which flickered now and then and disturbed the test screenshots. Only the value the surface's present mode is
 * chosen with is changed (since 26.2 the game picks it when it configures the surface), the saved option stays as the user
 * set it. A normal game (no such property) is untouched.
 */
@Mixin(GpuSurface.PresentMode.class)
public abstract class BackgroundVsyncMixin {
    private static boolean logged;
    private static final boolean NO_VSYNC = Boolean.getBoolean("bettacraft.background") || Boolean.getBoolean("bettacraft.novsync");

    @ModifyVariable(method = "getSupportedVsyncMode", at = @At("HEAD"), argsOnly = true, require = 0)
    private static boolean bettacraft$noVsync(boolean vsync) {
        if (!NO_VSYNC) return vsync;
        if (!logged) { // the smoke runs check this line
            logged = true;
            org.slf4j.LoggerFactory.getLogger("bettacraft/background").info("[background] vsync off (dev client)");
        }
        return false;
    }
}
