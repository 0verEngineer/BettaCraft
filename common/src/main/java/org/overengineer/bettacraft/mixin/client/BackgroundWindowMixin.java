package org.overengineer.bettacraft.mixin.client;

import com.mojang.blaze3d.platform.Window;
import java.nio.IntBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Development only ({@code -Dbettacraft.background=true}, set for the client smoke tests and {@code -Pbackground}):
 * the game window opens without taking the focus (so it neither steals the keyboard nor captures the mouse of whatever
 * the developer is doing) and on a secondary monitor if there is one. Needs the FML early window off (the build sets
 * {@code earlyWindowControl = false} for these runs), otherwise the early loading window is created before this.
 */
@Mixin(Window.class)
public abstract class BackgroundWindowMixin {
    private static final boolean BACKGROUND = Boolean.getBoolean("bettacraft.background");

    @Shadow private int x;
    @Shadow private int y;
    @Shadow private int windowedX;
    @Shadow private int windowedY;

    @Inject(method = "<init>", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/neoforged/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J"))
    private void bettacraft$unfocusedHints(CallbackInfo ci) {
        if (!BACKGROUND) return;
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
        HINTS_APPLIED = true;
    }

    /** M52: Fabric (no early loading window): the window hints before vanilla creates the window. */
    @Inject(method = "<init>", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J", remap = false))
    private void bettacraft$unfocusedHintsVanilla(CallbackInfo ci) {
        if (!BACKGROUND) return;
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
        HINTS_APPLIED = true;
    }

    private static boolean HINTS_APPLIED;

    /**
     * M52 (backport of the 26.2 fix): verifiable, whether the background window really came up without the focus (logged
     * once, after it is shown; the client smoke runs look for "focused false").
     */
    @Inject(method = "<init>", require = 1, at = @At("TAIL"))
    private void bettacraft$logFocus(CallbackInfo ci) {
        if (!BACKGROUND) return;
        boolean focused = GLFW.glfwGetWindowAttrib(((Window) (Object) this).handle(), GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
        org.slf4j.LoggerFactory.getLogger("bettacraft/background").info("[background] window created: hints applied {}, focused {}",
                HINTS_APPLIED, focused);
    }

    @Inject(method = "<init>", require = 0, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setMode()V"))
    private void bettacraft$secondMonitor(CallbackInfo ci) {
        if (!BACKGROUND) return;
        PointerBuffer monitors = GLFW.glfwGetMonitors();
        long primary = GLFW.glfwGetPrimaryMonitor();
        if (monitors == null) return;
        for (int i = 0; i < monitors.limit(); i++) {
            long m = monitors.get(i);
            if (m == primary) continue;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer mx = stack.mallocInt(1), my = stack.mallocInt(1), mw = stack.mallocInt(1), mh = stack.mallocInt(1);
                GLFW.glfwGetMonitorWorkarea(m, mx, my, mw, mh);
                this.windowedX = this.x = mx.get(0) + 40;
                this.windowedY = this.y = my.get(0) + 40;
            }
            return;
        }
    }
}
