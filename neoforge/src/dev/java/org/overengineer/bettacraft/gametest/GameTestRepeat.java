package org.overengineer.bettacraft.gametest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.gametest.framework.TestFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M63: {@code -Pgametestrepeat=<n>} (both loaders): every test that runs (after {@code -Pgametests}) n times side by side in
 * its batch, the copies named {@code <test>_r<i>}. For hunting flaky tests: one run gives n tries. Copies of a test that
 * changes the config share that config (their batch then has n tests changing it at once).
 */
public final class GameTestRepeat {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/gametest");

    private GameTestRepeat() {}

    public static void repeat(Collection<TestFunction> tests, int n) {
        if (n <= 1) return;
        List<TestFunction> copies = new ArrayList<>();
        for (TestFunction t : tests) {
            for (int i = 2; i <= n; i++) {
                copies.add(new TestFunction(t.batchName(), t.testName() + "_r" + i, t.structureName(), t.rotation(), t.maxTicks(), t.setupTicks(),
                        t.required(), t.manualOnly(), t.maxAttempts(), t.requiredSuccesses(), t.skyAccess(), t.function()));
            }
        }
        tests.addAll(copies);
        LOGGER.info("[gametest] -Pgametestrepeat={}: {} tests", n, tests.size());
    }
}
