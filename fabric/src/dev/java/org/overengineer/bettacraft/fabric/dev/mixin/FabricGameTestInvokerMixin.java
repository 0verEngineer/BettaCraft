package org.overengineer.bettacraft.fabric.dev.mixin;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import org.overengineer.bettacraft.fabric.dev.FabricGameTests;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** M52 (dev): our test methods run as they are (static, private constructors), see FabricGameTests#invoker. */
@Pseudo
@Mixin(targets = "net.fabricmc.fabric.impl.gametest.FabricGameTestHelper", remap = false)
public abstract class FabricGameTestInvokerMixin {
    @Inject(method = "getTestMethodInvoker", at = @At("HEAD"), cancellable = true, remap = false)
    private static void bettacraft$ours(Method method, CallbackInfoReturnable<Consumer<GameTestHelper>> cir) {
        if (FabricGameTests.CLASSES.contains(method.getDeclaringClass())) cir.setReturnValue(FabricGameTests.invoker(method));
    }
}
