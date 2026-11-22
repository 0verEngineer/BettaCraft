package org.overengineer.bettacraft.gametest.integration;

import org.overengineer.bettacraft.gametest.framework.GameTestFailure;
import java.util.List;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * M42: the optional integration suite. Its test classes carry no {@code @GameTestHolder} (that would put them into every
 * GameTest run); they are registered here only when the system property {@code bettacraft.integration} is set, which
 * {@code gradlew integrationTests} (or {@code runGameTestServer -Pintegration}) does together with every dev group of the
 * mods we integrate. Unlike the tests of the normal suite they do not pass silently without their mod: a missing mod of the
 * group fails ({@link #lacks}), so a run with an incomplete group cannot look green.
 */
public final class IntegrationSuite {
    static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("bettacraft/integration");
    public static final boolean ENABLED = Boolean.getBoolean("bettacraft.integration");
    /** The template of every integration test (in data/bettacraft/structure of src/dev). */
    public static final String EMPTY = "empty_32x10x32";

    /** Every test class of the suite, read by {@code BettaGameTests} (only when {@link #ENABLED}). */
    static final List<Class<?>> CLASSES = List.of(
            IntegrationGroupTests.class, IntegrationTransportTests.class, IntegrationModTests.class, IntegrationPowerTests.class,
            IntegrationStorageTests.class, IntegrationTunnelsTests.class, IntegrationBreedingTests.class);

    private IntegrationSuite() {}

    /**
     * Whether the test lacks one of its mods: then it fails (the group is incomplete). M59: every run has the whole group (the
     * manifest dev-mods.json downloads it, locally and in CI), so there is no partial run any more (a mod without a build for
     * this Minecraft version is a documented gap, see VersionGaps).
     */
    public static boolean lacks(GameTestHelper h, String... mods) {
        List<String> missing = java.util.Arrays.stream(mods).filter(m -> !org.overengineer.bettacraft.platform.Platform.get().isModLoaded(m)).toList();
        if (missing.isEmpty()) return false;
        if (org.overengineer.bettacraft.gametest.framework.VersionGaps.allWithoutBuild(missing.toArray(String[]::new))) {
            // M52: no build of these mods for this Minecraft version (a documented gap)
            LOGGER.warn("[integration] skipped: {} have no build for Minecraft {}", missing, org.overengineer.bettacraft.gametest.framework.VersionGaps.MINECRAFT);
            h.succeed();
            return true;
        }
        throw new GameTestFailure("the integration group lacks " + missing + " (see dev-mods.json and run/*-mods)");
    }
}
