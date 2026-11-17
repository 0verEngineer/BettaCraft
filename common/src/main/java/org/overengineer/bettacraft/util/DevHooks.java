package org.overengineer.bettacraft.util;

import org.slf4j.LoggerFactory;

/**
 * Entry point into the development-only code of the dev source set ({@code src/dev}: smoke tests, showcase, GameTests),
 * which is part of the mod in {@code gradlew runClient/runServer/runGameTestServer} but not of the released jar (M22).
 * The class is looked up by name, so the main code has no compile-time reference to it; without it nothing happens.
 */
public final class DevHooks {
    private DevHooks() {}

    /** Calls {@code public static void init()} of the given dev class if it is present. */
    public static void init(String className) {
        Class<?> type;
        try {
            type = Class.forName(className);
        } catch (ClassNotFoundException e) {
            return; // a jar without the dev source set
        }
        try {
            type.getMethod("init").invoke(null);
        } catch (ReflectiveOperationException e) {
            LoggerFactory.getLogger("bettacraft/dev").error("dev hook {} failed", className, e);
        }
    }
}
