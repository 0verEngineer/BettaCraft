package org.overengineer.bettacraft.fabric.dev.mixin;

import org.overengineer.bettacraft.fabric.dev.FabricGameTests;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52 (dev): our GameTest classes belong to bettacraft without being entry points. */
@Pseudo
@Mixin(targets = "net.fabricmc.fabric.impl.gametest.FabricGameTestModInitializer", remap = false)
public abstract class FabricGameTestIdsMixin {
    @Inject(method = "getModIdForTestClass", at = @At("HEAD"), cancellable = true, remap = false)
    private static void bettacraft$ours(Class<?> testClass, CallbackInfoReturnable<String> cir) {
        if (FabricGameTests.CLASSES.contains(testClass)) cir.setReturnValue("bettacraft");
    }
}
