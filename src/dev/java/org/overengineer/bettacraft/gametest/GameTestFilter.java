package org.overengineer.bettacraft.gametest;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M42: {@code runGameTestServer -Pgametests=<prefix>[,<prefix>...]} runs only the GameTests whose name (the lower-case
 * method name; our classes do not prefix it) or batch starts with one of the prefixes, e.g.
 * {@code -Pgametests=bettacraft_integration} for the integration-only tests or {@code -Pgametests=bettacraft_storage} (for
 * quick rounds while writing tests; the full runs never set it). The GameTest server copies the registered tests before it starts; the copy is filtered here, before the
 * batches are made.
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class GameTestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/gametest");
    private static final String ONLY = System.getProperty("bettacraft.gametest.only", "");

    private GameTestFilter() {}

    @SubscribeEvent
    public static void onAboutToStart(ServerAboutToStartEvent event) {
        if (ONLY.isBlank() || !(event.getServer() instanceof GameTestServer server)) return;
        List<String> prefixes = Arrays.stream(ONLY.toLowerCase(Locale.ROOT).split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        try {
            Field f = GameTestServer.class.getDeclaredField("testFunctions");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Collection<TestFunction> tests = (Collection<TestFunction>) f.get(server);
            int before = tests.size();
            tests.removeIf(t -> prefixes.stream().noneMatch(p -> t.testName().toLowerCase(Locale.ROOT).startsWith(p)
                    || t.batchName().toLowerCase(Locale.ROOT).startsWith(p)));
            LOGGER.info("[gametest] -Pgametests={}: {} of {} tests", ONLY, tests.size(), before);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot filter the GameTests", e);
        }
    }
}
