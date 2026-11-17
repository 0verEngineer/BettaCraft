package org.overengineer.bettacraft.client;

/**
 * Client side of the development tools (dev source set, not in the released jar): started by {@code BettaCraftClient}
 * through {@code DevHooks} in development runs. The system properties are set by the Gradle flags of build.gradle.
 */
public final class DevClientHooks {
    private DevClientHooks() {}

    public static void init() {
        if (Boolean.getBoolean("bettacraft.smoketest.world")) {
            DevClientSmokeTest.register();
        } else if (Boolean.getBoolean("bettacraft.media")) {
            DevClientMedia.register(); // M43: gradlew runClient -Psmoketestm43: README and mod page media in a copy of the showcase world
        } else if (Boolean.getBoolean("bettacraft.showcase.tour")) {
            DevClientShowcaseTour.register(); // M21: gradlew runClient -Pshowcasetour: screenshots of the showcase world
        } else if (Boolean.getBoolean("bettacraft.smoketest.mp")) {
            DevClientSmokeMP.register(); // gradlew runMultiplayerTest: joins the dedicated test server as Dev1 / Dev2
        } else if (Boolean.getBoolean("bettacraft.smoketest")) {
            // quit as soon as the title screen opens (models/textures baked without crash)
            org.overengineer.bettacraft.platform.client.ClientEvents.SCREEN_OPENING.add(screen -> {
                if (screen instanceof net.minecraft.client.gui.screens.TitleScreen) {
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
