package org.overengineer.bettacraft.compat;

import java.lang.reflect.Method;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * M51: FTB Teams through reflection (no compile dependency): whether two players are in the same team (a party; every
 * player alone is in his own player team, which does not count). Without the mod, before its manager is loaded or on any
 * error the answer is "no".
 */
public final class FtbTeamsCompat {
    public static final String MOD_ID = "ftbteams";
    private static Boolean loaded;
    @Nullable
    private static Method api, managerLoaded, manager, sameTeam;
    private static boolean failed;

    /** GameTests: overrides the answer (null = ask FTB Teams). */
    @Nullable
    public static java.util.function.BiPredicate<UUID, UUID> testOverride;

    private FtbTeamsCompat() {}

    public static boolean loaded() {
        if (loaded == null) loaded = org.overengineer.bettacraft.platform.Platform.get().isModLoaded(MOD_ID);
        return loaded;
    }

    /** Whether {@code a} and {@code b} are different players of one party team. */
    public static boolean sameTeam(@Nullable UUID a, @Nullable UUID b) {
        if (a == null || b == null || a.equals(b)) return false;
        if (testOverride != null) return testOverride.test(a, b);
        if (!loaded() || failed) return false;
        try {
            if (api == null) {
                Class<?> c = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
                api = c.getMethod("api");
                Class<?> apiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API");
                managerLoaded = apiClass.getMethod("isManagerLoaded");
                manager = apiClass.getMethod("getManager");
                sameTeam = Class.forName("dev.ftb.mods.ftbteams.api.TeamManager").getMethod("arePlayersInSameTeam", UUID.class, UUID.class);
            }
            Object o = api.invoke(null);
            if (o == null || !(Boolean) managerLoaded.invoke(o)) return false;
            Object m = manager.invoke(o);
            return m != null && (Boolean) sameTeam.invoke(m, a, b);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            failed = true;
            org.slf4j.LoggerFactory.getLogger("bettacraft/ftbteams").warn("FTB Teams could not be asked about teams: {}", e.toString());
            return false;
        }
    }
}
