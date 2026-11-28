package org.overengineer.bettacraft.gametest.integration;

import java.util.List;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.overengineer.bettacraft.BettaCraft;

/**
 * M42: the optional integration suite. Its test classes carry no {@code @GameTestHolder} (that would put them into every
 * GameTest run); they are registered here only when the system property {@code bettacraft.integration} is set, which
 * {@code gradlew integrationTests} (or {@code runGameTestServer -Pintegration}) does together with every dev group of the
 * mods we integrate. Unlike the tests of the normal suite they do not pass silently without their mod: a missing mod of the
 * group fails ({@link #lacks}), so a run with an incomplete group cannot look green.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class IntegrationSuite {
    static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("bettacraft/integration");
    public static final boolean ENABLED = Boolean.getBoolean("bettacraft.integration");
    /** The template of every integration test (in data/bettacraft/structure of src/dev). */
    public static final String EMPTY = "empty_32x10x32";

    /** Every test class of the suite. */
    static final List<Class<?>> CLASSES = List.of(
            IntegrationGroupTests.class, IntegrationTransportTests.class, IntegrationModTests.class, IntegrationPowerTests.class,
            IntegrationStorageTests.class, IntegrationTunnelsTests.class, IntegrationBreedingTests.class);

    private IntegrationSuite() {}

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        if (!ENABLED) return;
        for (Class<?> c : CLASSES) event.register(c);
        LOGGER.info("[integration] registered {} integration test classes", CLASSES.size());
    }

    /**
     * Whether the test lacks one of its mods: then it fails (the group is incomplete). M59: every run has the whole group (the
     * manifest gradle/dev-mods.json downloads it, locally and in CI), so there is no partial run any more; the result stays a boolean
     * for the callers ({@code if (lacks(h, ...)) return;}).
     */
    public static boolean lacks(GameTestHelper h, String... mods) {
        List<String> missing = java.util.Arrays.stream(mods).filter(m -> !org.overengineer.bettacraft.platform.Platform.get().isModLoaded(m)).toList();
        if (missing.isEmpty()) return false;
        throw new GameTestAssertException("the integration group lacks " + missing + " (see gradle/dev-mods.json and run/*-mods)");
    }
}
