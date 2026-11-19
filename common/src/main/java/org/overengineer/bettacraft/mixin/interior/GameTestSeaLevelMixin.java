package org.overengineer.bettacraft.mixin.interior;

import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GameTest server only (M52b): up to 1.21.1 every level answered sea level 63, since 1.21.2 a level answers its generator's
 * (the flat test world: -63, below the ground). The GameTests build their open water at the sea level, above the test
 * structures; the test world keeps the old answer so they find the same conditions. Never active outside a GameTest
 * server.
 * <p>It ships in the jars like {@link GameTestServerMixin}: NeoForge reads mixin configs only from the mod's own
 * {@code neoforge.mods.toml} (the dev source set is no mod of its own), so a dev-only config is not possible there; the
 * check is one {@code instanceof} on a call that is not hot.
 */
@Mixin(ServerLevel.class)
public abstract class GameTestSeaLevelMixin {
    @Inject(method = "getSeaLevel", at = @At("HEAD"), cancellable = true)
    private void bettacraft$oldTestSeaLevel(CallbackInfoReturnable<Integer> cir) {
        if (((ServerLevel) (Object) this).getServer() instanceof GameTestServer) cir.setReturnValue(63);
    }
}
