package org.overengineer.bettacraft.client.interior;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.neoforged.fml.ModList;

/**
 * Optional Iris integration without a compile dependency (PLAN 3.3 / M3 Iris analysis): reads
 * {@code net.irisshaders.iris.api.v0.IrisApi#isShaderPackInUse()} and {@code #isRenderingShadowPass()} via method
 * handles. Without Iris both are false.
 */
public final class IrisCompat {
    private static final Object API;
    private static final MethodHandle PACK_IN_USE, SHADOW_PASS;

    static {
        Object api = null;
        MethodHandle pack = null, shadow = null;
        if (ModList.get() != null && (ModList.get().isLoaded("iris") || ModList.get().isLoaded("oculus"))) {
            try {
                Class<?> c = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                MethodHandles.Lookup l = MethodHandles.publicLookup();
                api = l.findStatic(c, "getInstance", MethodType.methodType(c)).invoke();
                pack = l.findVirtual(c, "isShaderPackInUse", MethodType.methodType(boolean.class));
                shadow = l.findVirtual(c, "isRenderingShadowPass", MethodType.methodType(boolean.class));
            } catch (Throwable t) {
                MiniatureRenderer.LOGGER.warn("Iris is loaded but its API could not be bound; miniatures ignore shader state", t);
                api = null;
            }
        }
        API = api;
        PACK_IN_USE = pack;
        SHADOW_PASS = shadow;
    }

    private IrisCompat() {}

    public static boolean shaderPackInUse() {
        if (API == null) return false;
        try {
            return (boolean) PACK_IN_USE.invoke(API);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean renderingShadowPass() {
        if (API == null) return false;
        try {
            return (boolean) SHADOW_PASS.invoke(API);
        } catch (Throwable t) {
            return false;
        }
    }
}
