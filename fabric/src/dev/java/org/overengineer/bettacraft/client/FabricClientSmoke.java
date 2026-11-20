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
    private static DevClientSmokeM7 m7;
    private static int stage;
    private static BookTour book;

    /** The pages of the handbook (Patchouli) and the recipe viewer pages, screenshots in English and German. */
    private static final class BookTour {
        private static final String[][] PAGES = {
                {"landing", null, "0"}, {"species", "bettacraft:tankmates/neon_tetra", "0"}, {"device", "bettacraft:equipment/air_pump", "0"},
                {"device_recipe", "bettacraft:equipment/air_pump", "2"}, {"multiblock", "bettacraft:aquarium/shapes", "2"},
                {"entity", "bettacraft:bettas", "2"}, {"table", "bettacraft:aquarium/temperature", "2"}};
        private final java.util.Deque<Runnable> steps = new java.util.ArrayDeque<>();
        private int wait;

        BookTour() {
            for (String lang : new String[]{"en_us", "de_de"}) {
                steps.add(() -> language(lang));
                for (int i = 0; i < 30; i++) steps.add(() -> {});
                for (String[] page : PAGES) {
                    steps.add(() -> open(page[1], Integer.parseInt(page[2])));
                    for (int i = 0; i < 25; i++) steps.add(() -> {});
                    steps.add(() -> shot("book_" + page[0] + "_" + lang));
                    steps.add(() -> Minecraft.getInstance().setScreen(null));
                }
                // the recipe viewer (JEI / REI / EMI, whichever runs): the uses of the food maker
                steps.add(() -> RecipeViewerShots.show(new net.minecraft.world.item.ItemStack(org.overengineer.bettacraft.registry.ModItems.FOOD_MAKER.get())));
                for (int i = 0; i < 30; i++) steps.add(() -> {});
                steps.add(() -> shot("viewer_food_maker_" + lang));
                steps.add(() -> RecipeViewerShots.show(new net.minecraft.world.item.ItemStack(org.overengineer.bettacraft.registry.ModItems.TANK.get())));
                for (int i = 0; i < 30; i++) steps.add(() -> {});
                steps.add(() -> shot("viewer_tank_" + lang));
                steps.add(() -> Minecraft.getInstance().setScreen(null));
            }
            steps.add(() -> language("en_us"));
            for (int i = 0; i < 20; i++) steps.add(() -> {});
        }

        boolean tick(Minecraft mc) {
            Runnable r = steps.poll();
            if (r == null) return true;
            r.run();
            return false;
        }

        private static void language(String code) {
            Minecraft mc = Minecraft.getInstance();
            if (code.equals(mc.options.languageCode)) return;
            mc.options.languageCode = code;
            mc.getLanguageManager().setSelected(code);
            mc.reloadResourcePacks();
        }

        private static void open(String entry, int page) {
            net.minecraft.resources.ResourceLocation bookId = org.overengineer.bettacraft.BettaCraft.id("handbook");
            var api = vazkii.patchouli.api.PatchouliAPI.get();
            if (entry == null) api.openBookGUI(bookId);
            else api.openBookEntry(bookId, net.minecraft.resources.ResourceLocation.parse(entry), page);
        }

        private static void shot(String name) {
            Minecraft mc = Minecraft.getInstance();
            mc.getToasts().clear();
            net.minecraft.client.Screenshot.grab(mc.gameDirectory, "bettacraft_fabric_" + name + ".png", mc.getMainRenderTarget(),
                    c -> LOGGER.info("[smoketest] {}", c.getString()));
        }
    }

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
            // -Psmoketestbook: only the handbook (Patchouli) and the recipe viewer, English and German
            if (stage == 0 && ticks == 60 && Boolean.getBoolean("bettacraft.smoketest.book")) {
                stage = 10;
                book = new BookTour();
            }
            if (stage == 10 && book.tick(mc)) {
                LOGGER.info("[smoketest] fabric client book test finished");
                LOGGER.info("[smoketest] PASSED");
                mc.stop();
                return;
            }
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
                stage = 4;
                // M52-final: with Better Fish Tanks (-Pfabricintegration) also the M7 scene: the animals of other mods in our
                // tank, a betta in a Better Fish Tanks tank, the menu with its fish bag
                if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded("betterfishtanks")) {
                    LOGGER.info("[smoketest] fabric client: M7 scene (aquarium mods)");
                    m7 = new DevClientSmokeM7();
                    stage = 5;
                }
            }
            if (stage == 5 && m7.tick()) {
                mc.player.closeContainer();
                stage = 4;
            }
            if (stage == 4) {
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
