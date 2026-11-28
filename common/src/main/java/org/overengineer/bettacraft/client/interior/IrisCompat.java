package org.overengineer.bettacraft.client.interior;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

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
        if ((org.overengineer.bettacraft.platform.Platform.get().isModLoaded("iris") || org.overengineer.bettacraft.platform.Platform.get().isModLoaded("oculus"))) {
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

    /** M67.5: Iris's per-thread switch that keeps a block-format buffer builder in the plain block format, or null. */
    @SuppressWarnings("unchecked")
    private static final ThreadLocal<Boolean> SKIP_EXTENSION = skipExtension();

    @SuppressWarnings("unchecked")
    private static ThreadLocal<Boolean> skipExtension() {
        if (API == null) return null;
        try {
            java.lang.reflect.Field f = Class.forName("net.irisshaders.iris.vertices.ImmediateState").getField("skipExtension");
            return (ThreadLocal<Boolean>) f.get(null);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * M67.5: makes {@code make} with Iris's vertex format extension switched off (or on) on this thread. Iris turns a buffer
     * builder of the block format into its extended terrain format while a pack is in use and the level renders.
     */
    public static <T> T withExtension(boolean extend, java.util.function.Supplier<T> make) {
        if (SKIP_EXTENSION == null) return make.get();
        Boolean before = SKIP_EXTENSION.get();
        SKIP_EXTENSION.set(!extend);
        // Iris extends only while it thinks the level is being drawn; our cells are scheduled outside that window on 26.3
        boolean rendering = renderingLevel();
        if (extend) setRenderingLevel(true);
        try {
            return make.get();
        } finally {
            SKIP_EXTENSION.set(before);
            if (extend) setRenderingLevel(rendering);
        }
    }

    private static final java.lang.reflect.Field RENDERING_LEVEL = renderingLevelField();

    private static java.lang.reflect.Field renderingLevelField() {
        if (API == null) return null;
        try {
            return Class.forName("net.irisshaders.iris.vertices.ImmediateState").getField("isRenderingLevel");
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean renderingLevel() {
        try {
            return RENDERING_LEVEL != null && RENDERING_LEVEL.getBoolean(null);
        } catch (Throwable t) {
            return false;
        }
    }

    private static void setRenderingLevel(boolean on) {
        try {
            if (RENDERING_LEVEL != null) RENDERING_LEVEL.setBoolean(null, on);
        } catch (Throwable ignored) {
        }
    }
}
