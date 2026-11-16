package org.overengineer.bettacraft.gametest.integration;

import java.util.List;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.overengineer.bettacraft.BettaCraft;

/**
 * M42: the optional integration suite. Its test classes carry no {@code @GameTestHolder} (that would put them into every
 * GameTest run); they are registered here only when the system property {@code bettacraft.integration} is set, which
 * {@code gradlew integrationTests} (or {@code runGameTestServer -Pintegration}) does together with every dev group of the
 * mods we integrate. Unlike the tests of the normal suite they do not pass silently without their mod: a missing mod of the
 * group fails ({@link #require}), so a run with an incomplete group cannot look green.
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
            IntegrationStorageTests.class, IntegrationTunnelsTests.class);

    private IntegrationSuite() {}

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        if (!ENABLED) return;
        for (Class<?> c : CLASSES) event.register(c);
        LOGGER.info("[integration] registered {} integration test classes", CLASSES.size());
    }

    /** Fails the test when one of the mods is not loaded (the group is incomplete). */
    public static void require(String... mods) {
        for (String m : mods) {
            if (!ModList.get().isLoaded(m)) throw new GameTestAssertException("the integration group lacks the mod " + m + " (see run/*-mods)");
        }
    }
}
