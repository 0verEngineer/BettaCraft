package org.overengineer.bettacraft.mixin.client;

import com.mojang.blaze3d.platform.Window;
import java.nio.IntBuffer;
import org.lwjgl.sdl.SDLHints;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Development only ({@code -Dbettacraft.background=true}, set for the client smoke tests and {@code -Pbackground}):
 * the game window opens without taking the focus (so it neither steals the keyboard nor captures the mouse of whatever
 * the developer is doing) and on a secondary monitor if there is one. Needs the FML early window off (the build sets
 * {@code earlyWindowControl = false} for these runs), otherwise the early loading window is created before this.
 *
 * <p>M52e: 26.3 creates the window with SDL instead of GLFW ({@code Window#createWindow} hands the flags to the
 * backend's {@code SDL_CreateWindow}). SDL is told not to activate the window when it is shown or raised, and the window
 * is created {@code SDL_WINDOW_NOT_FOCUSABLE}: it never takes the keyboard focus, not even by a click.
 */
@Mixin(Window.class)
public abstract class BackgroundWindowMixin {
    private static final boolean BACKGROUND = Boolean.getBoolean("bettacraft.background");
    private static boolean HINTS_APPLIED;

    @Shadow private int x;
    @Shadow private int y;
    @Shadow private int windowedX;
    @Shadow private int windowedY;
    @Shadow public abstract long handle();

    @ModifyArg(method = "createWindow", require = 1, index = 3, at = @At(value = "INVOKE",
            target = "Lcom/mojang/renderpearl/api/device/GpuBackend;createWindow(Ljava/lang/String;IIJ)J"))
    private long bettacraft$unfocusedWindow(long flags) {
        if (!BACKGROUND) return flags;
        SDLHints.SDL_SetHint(SDLHints.SDL_HINT_WINDOW_ACTIVATE_WHEN_SHOWN, "0");
        SDLHints.SDL_SetHint(SDLHints.SDL_HINT_WINDOW_ACTIVATE_WHEN_RAISED, "0");
        HINTS_APPLIED = true;
        return flags | SDLVideo.SDL_WINDOW_NOT_FOCUSABLE;
    }

    /** Verifiable: whether the background window really came up without the focus (logged once, after it is shown). */
    @Inject(method = "<init>(Lcom/mojang/blaze3d/platform/WindowEventHandler;Lcom/mojang/blaze3d/platform/DisplayData;Ljava/lang/String;ZLjava/lang/String;Lcom/mojang/blaze3d/platform/MonitorManager;Lcom/mojang/renderpearl/api/device/GpuBackend;I)V",
            require = 1, at = @At("TAIL"))
    private void bettacraft$logFocus(CallbackInfo ci) {
        if (!BACKGROUND) return;
        long flags = SDLVideo.SDL_GetWindowFlags(handle());
        boolean focused = (flags & SDLVideo.SDL_WINDOW_INPUT_FOCUS) != 0 || SDLKeyboard.SDL_GetKeyboardFocus() == handle();
        org.slf4j.LoggerFactory.getLogger("bettacraft/background").info("[background] window created: hints applied {}, not focusable {}, focused {}",
                HINTS_APPLIED, (flags & SDLVideo.SDL_WINDOW_NOT_FOCUSABLE) != 0, focused);
    }

    @Inject(method = "<init>(Lcom/mojang/blaze3d/platform/WindowEventHandler;Lcom/mojang/blaze3d/platform/DisplayData;Ljava/lang/String;ZLjava/lang/String;Lcom/mojang/blaze3d/platform/MonitorManager;Lcom/mojang/renderpearl/api/device/GpuBackend;I)V",
            require = 0, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setMode()V"))
    private void bettacraft$secondMonitor(CallbackInfo ci) {
        if (!BACKGROUND) return;
        IntBuffer displays = SDLVideo.SDL_GetDisplays();
        if (displays == null) return;
        int primary = SDLVideo.SDL_GetPrimaryDisplay();
        try {
            for (int i = 0; i < displays.limit(); i++) {
                int d = displays.get(i);
                if (d == 0 || d == primary) continue;
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    SDL_Rect r = SDL_Rect.malloc(stack);
                    if (!SDLVideo.SDL_GetDisplayUsableBounds(d, r)) continue;
                    this.windowedX = this.x = r.x() + 40;
                    this.windowedY = this.y = r.y() + 40;
                }
                return;
            }
        } finally {
            org.lwjgl.sdl.SDLStdinc.SDL_free(displays);
        }
    }
}
