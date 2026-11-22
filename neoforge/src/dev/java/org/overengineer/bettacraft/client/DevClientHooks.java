package org.overengineer.bettacraft.client;

/**
 * Client side of the development tools (dev source set, not in the released jar): started by {@code BettaCraftClient}
 * through {@code DevHooks} in development runs. The system properties are set by the Gradle flags of build.gradle.
 */
public final class DevClientHooks {
    private DevClientHooks() {}

    public static void init() {
        // M56: a fresh run dir opens the accessibility onboarding (narrator) first and no hook below would ever see the title
        // screen: skip it like FabricClientSmoke does (the option is saved, the title screen opens and the hooks fire)
        org.overengineer.bettacraft.platform.client.ClientEvents.SCREEN_OPENING.add(screen -> {
            if (!(screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)) return;
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            mc.execute(() -> {
                mc.options.onboardAccessibility = false;
                mc.options.save();
                mc.gui.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
            });
        });
        if (Boolean.getBoolean("bettacraft.smoketest.worldtour")) {
            DevClientWorldTour.register(); // M63b: gradlew runClient -Psmoketestworldtour: every tank of a copy of the showcase
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ScreenEvent.Init.Post e) -> DevClientWorldTour.afterInit(e.getScreen()));
        } else if (Boolean.getBoolean("bettacraft.smoketest.world")) {
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
