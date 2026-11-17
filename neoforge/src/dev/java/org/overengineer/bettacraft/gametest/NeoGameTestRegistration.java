package org.overengineer.bettacraft.gametest;

import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.gametest.framework.BettaGameTests;

/**
 * M52b: NeoForge's half of {@link BettaGameTests}: the test functions go into the static registry in RegisterEvent, the
 * environments (one per batch) and test instances into the dynamic registries in RegisterGameTestsEvent. Both only where
 * NeoForge runs GameTests at all (dev GameTest server, or {@code neoforge.enableGameTest}).
 */
@EventBusSubscriber(modid = BettaCraft.MOD_ID)
public final class NeoGameTestRegistration {
    private NeoGameTestRegistration() {}

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.TEST_FUNCTION) || !GameTestHooks.isGametestEnabled()) return;
        BettaGameTests.registerFunctions((key, fn) -> event.register(Registries.TEST_FUNCTION, key.identifier(), () -> fn));
    }

    @SubscribeEvent
    public static void tests(RegisterGameTestsEvent event) {
        BettaGameTests.registerInstances(event::registerEnvironment, event::registerTest);
    }
}
