package org.overengineer.bettacraft.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.overengineer.bettacraft.platform.client.ClientEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M52: the Fabric client smoke ({@code gradlew runFabricClient -Psmoketestfabric}, background window): a fresh flat
 * world, then the M11 scene (tank styles, connected glass, edges, water, menus) and the M13 scene (plants, the tank
 * interior, its miniature) of the NeoForge client smoke, screenshots in run-fabric/screenshots; quits at the end.
 * {@code -Psmoketest} alone quits at the title screen (models and textures baked).
 */
public final class FabricClientSmoke implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static boolean created;
    private static int ticks;
    private static DevClientSmokeM11 m11;
    private static DevClientSmokeM13 m13;
    private static int stage;

    @Override
    public void onInitializeClient() {
        if (Boolean.getBoolean("bettacraft.smoketest.fabric")) {
            ClientEvents.SCREEN_OPENING.add(FabricClientSmoke::onScreen);
            ClientEvents.TICK_POST.add(FabricClientSmoke::tick);
        } else if (Boolean.getBoolean("bettacraft.smoketest")) {
            ClientEvents.SCREEN_OPENING.add(screen -> {
                if (screen instanceof TitleScreen) {
                    LOGGER.info("[smoketest] client reached title screen");
                    Minecraft.getInstance().stop();
                }
            });
        }
    }

    private static void onScreen(net.minecraft.client.gui.screens.Screen screen) {
        // a fresh run directory opens the accessibility onboarding first: skip it to the title screen
        if (screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                mc.options.onboardAccessibility = false;
                mc.options.save();
                mc.setScreen(new TitleScreen());
            });
            return;
        }
        if (created || !(screen instanceof TitleScreen)) return;
        created = true;
        Minecraft mc = Minecraft.getInstance();
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        LevelSettings settings = new LevelSettings("bettacraft_fabric_smoketest_" + System.currentTimeMillis(), GameType.CREATIVE, false,
                Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
        mc.execute(() -> mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new WorldOptions(42L, false, false),
                ra -> ra.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                screen));
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        ticks++;
        mc.options.pauseOnLostFocus = false;
        if (ticks == 2) mc.getWindow().setWindowed(1280, 960);
        if (ticks > 60) mc.player.getAbilities().flying = true;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        try {
            if (stage == 0 && ticks == 60) {
                LOGGER.info("[smoketest] fabric client in world: M11 scene");
                m11 = new DevClientSmokeM11();
                stage = 1;
            }
            if (stage == 1 && m11.tick()) {
                LOGGER.info("[smoketest] fabric client: M13 scene");
                m13 = new DevClientSmokeM13();
                stage = 2;
            }
            if (stage == 2 && m13.tick()) {
                stage = 3;
                LOGGER.info("[smoketest] fabric client world test finished");
                LOGGER.info("[smoketest] PASSED");
                mc.stop();
            }
        } catch (RuntimeException e) {
            LOGGER.error("[smoketest] FAILED: fabric client scene", e);
            mc.stop();
        }
    }
}
