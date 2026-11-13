package org.overengineer.bettacraft.client;

import net.neoforged.neoforge.common.NeoForge;

/**
 * Client side of the development tools (dev source set, not in the released jar): started by {@code BettaCraftClient}
 * through {@code DevHooks} in development runs. The system properties are set by the Gradle flags of build.gradle.
 */
public final class DevClientHooks {
    private DevClientHooks() {}

    public static void init() {
        if (Boolean.getBoolean("bettacraft.smoketest.world")) {
            DevClientSmokeTest.register();
        } else if (Boolean.getBoolean("bettacraft.showcase.tour")) {
            DevClientShowcaseTour.register(); // M21: gradlew runClient -Pshowcasetour: screenshots of the showcase world
        } else if (Boolean.getBoolean("bettacraft.smoketest.mp")) {
            DevClientSmokeMP.register(); // gradlew runMultiplayerTest: joins the dedicated test server as Dev1 / Dev2
        } else if (Boolean.getBoolean("bettacraft.smoketest")) {
            // quit as soon as the title screen opens (models/textures baked without crash)
            NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ScreenEvent.Opening e) -> {
                if (e.getNewScreen() instanceof net.minecraft.client.gui.screens.TitleScreen) {
                    org.slf4j.LoggerFactory.getLogger("bettacraft/smoketest").info("[smoketest] client reached title screen");
                    net.minecraft.client.Minecraft.getInstance().stop();
                }
            });
        }
        if (!Boolean.getBoolean("bettacraft.smoketest.world")) {
            SmokeFreeze.registerRelease(); // copied smoke worlds: frozen screenshot animals swim again
        }
    }
}
