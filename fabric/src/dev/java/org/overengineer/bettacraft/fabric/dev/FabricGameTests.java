package org.overengineer.bettacraft.fabric.dev;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import org.overengineer.bettacraft.gametest.framework.BettaGameTests;

/**
 * M52: the GameTests on Fabric. M52b (1.21.11): the shared registrar {@link BettaGameTests} (the same tests, names and
 * batches as on NeoForge): the test functions go into the static registry here, the environments (one per batch) and the
 * test instances into the dynamic registries when Fabric's GameTest module adds its own ({@code FabricGameTestIdsMixin}).
 * Only our tests run: the run sets Fabric's test selection to {@code bettacraft:*} (fabric/build.gradle).
 */
public final class FabricGameTests implements ModInitializer {
    /** Whether this run is Fabric's GameTest server. */
    public static boolean enabled() {
        return System.getProperty("fabric-api.gametest") != null;
    }

    @Override
    public void onInitialize() {
        if (!enabled()) return;
        BettaGameTests.registerFunctions((key, fn) -> Registry.register(BuiltInRegistries.TEST_FUNCTION, key, fn));
    }
}
