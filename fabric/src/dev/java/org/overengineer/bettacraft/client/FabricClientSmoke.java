package org.overengineer.bettacraft.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.gamerules.GameRules;
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
 * interior, its miniature) of the NeoForge client smoke, screenshots in run/1.21.11/fabric/screenshots; quits at the end.
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

    /** The pages of the handbook (Modonomicon since M52b) and the recipe viewer pages, screenshots in English and German. */
    private static final class BookTour {
        // M52b: Modonomicon entries (the GuideME paths) and page numbers of the spread: a species with its picture, a device
        // text and its recipe, a scene as a multiblock, an animal as an entity page, a former table
        private static final String[][] PAGES = {
                {"landing", null, "0"}, {"species", "tankmates/neon_tetra", "0"}, {"device", "equipment/air_pump", "2"},
                {"device_recipe", "equipment/air_pump", "8"}, {"multiblock", "aquarium/shapes", "2"},
                {"entity", "bettas/bettas", "0"}, {"table", "aquarium/temperature", "2"}};
        private final java.util.Deque<Runnable> steps = new java.util.ArrayDeque<>();
        private int wait;

        BookTour() {
            for (String lang : new String[]{"en_us", "de_de"}) {
                steps.add(() -> language(lang));
                for (int i = 0; i < 30; i++) steps.add(() -> {});
                // Modonomicon renders a book's Markdown once when the world is joined: again in the new language (a player
                // rejoins the world after changing the language)
                steps.add(() -> com.klikli_dev.modonomicon.data.BookDataManager.get().prerenderMarkdown(Minecraft.getInstance().level.registryAccess()));
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
            net.minecraft.resources.Identifier bookId = org.overengineer.bettacraft.BettaCraft.id("handbook");
            // M52b/M52c: the handbook is a Modonomicon book on Fabric (entries bettacraft:<category>/<rest of the page path, "." for "/">)
            var gui = com.klikli_dev.modonomicon.client.gui.BookGuiManager.get();
            if (entry == null) gui.openBook(com.klikli_dev.modonomicon.client.gui.book.BookAddress.defaultFor(bookId));
            else gui.openEntry(bookId, org.overengineer.bettacraft.BettaCraft.id(entry), page);
        }

        private static void shot(String name) {
            Minecraft mc = Minecraft.getInstance();
            mc.getToastManager().clear();
            net.minecraft.client.Screenshot.grab(mc.gameDirectory, "bettacraft_fabric_" + name + ".png", mc.getMainRenderTarget(), 1,
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

    private static int crackTicks;
    private static final java.util.List<net.minecraft.core.BlockPos> cracked = new java.util.ArrayList<>();

    /** Cracks on the tank blocks near the player for a second (rendered every frame), then a screenshot; true when done. */
    private static boolean crack(Minecraft mc) {
        if (crackTicks++ == 0) {
            net.minecraft.core.BlockPos at = mc.player.blockPosition();
            for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(at.offset(-12, -8, -12), at.offset(12, 8, 12))) {
                if (cracked.size() >= 12) break;
                if (mc.level.getBlockState(p).getBlock() instanceof org.overengineer.bettacraft.tank.TankBlock) cracked.add(p.immutable());
            }
            LOGGER.info("[smoketest] crack overlay on {} tank blocks", cracked.size());
            if (cracked.isEmpty()) throw new IllegalStateException("no tank block near the player for the crack overlay");
            for (int i = 0; i < cracked.size(); i++) mc.levelRenderer.destroyBlockProgress(-100 - i, cracked.get(i), 3 + i % 6);
        }
        if (crackTicks == 30) BookTour.shot("m52c_crack");
        if (crackTicks < 40) return false;
        for (int i = 0; i < cracked.size(); i++) mc.levelRenderer.destroyBlockProgress(-100 - i, cracked.get(i), -1);
        return true;
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
        // M52c (26.1): the game rules are saved data of the world; set in tick() once the server is up
        LevelSettings settings = new LevelSettings("bettacraft_fabric_smoketest_" + System.currentTimeMillis(), GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
        mc.execute(() -> mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new WorldOptions(42L, false, false),
                ra -> ra.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                screen));
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        ticks++;
        if (ticks == 1) {
            var server = mc.getSingleplayerServer();
            server.execute(() -> {
                server.getGameRules().set(GameRules.ADVANCE_TIME, false, server);
                server.getGameRules().set(GameRules.SPAWN_MOBS, false, server);
            });
        }
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
                crackTicks = 0;
                // M52-final: with Better Fish Tanks (-Pfabricintegration) also the M7 scene: the animals of other mods in our
                // tank, a betta in a Better Fish Tanks tank, the menu with its fish bag
                if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded("betterfishtanks")) {
                    LOGGER.info("[smoketest] fabric client: M7 scene (aquarium mods)");
                    m7 = new DevClientSmokeM7();
                    stage = 5;
                }
            }
            if (stage == 5 && m7.tick()) {
                // the scene ends in the tank menu: close it and give the world a second before the crack overlay
                mc.player.closeContainer();
                crackTicks = -20;
                stage = 4;
            }
            // M52c (26.1): the crack overlay of mined blocks asks the models with AIR and no level (it crashed the tank model)
            if (stage == 4 && crack(mc)) {
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
