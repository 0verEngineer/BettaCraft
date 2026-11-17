package org.overengineer.bettacraft.gametest.framework;

import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;

/**
 * M52b: one GameTest as the old (1.21.1) record had it: batch, name, structure ({@code namespace:path}), timeout, setup
 * ticks, required and the test itself. {@link BettaGameTests} registers it as a test function and a test instance.
 */
public record TestFunction(String batchName, String testName, String structureName, Rotation rotation, int maxTicks, long setupTicks,
        boolean required, boolean manualOnly, int maxAttempts, int requiredSuccesses, boolean skyAccess, Consumer<GameTestHelper> function) {
    public TestFunction(String batchName, String testName, String structureName, int maxTicks, long setupTicks, boolean required,
            Consumer<GameTestHelper> function) {
        this(batchName, testName, structureName, Rotation.NONE, maxTicks, setupTicks, required, false, 1, 1, false, function);
    }
}
