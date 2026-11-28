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
    /** M53: -Psmoketestm53 (users tab, gear, map mods, interior brightness, every animal from six sides). */
    private static DevClientSmokeM53 m53;
    private static DevClientSmokeM54 m54;
    /** M62 parts 5 and 6: -Psmoketestm62d (the Tylomelania shell, every plant icon, the clear glass tank icon). */
    private static DevClientSmokeM62d m62d;
    /** M62 part 1: -Psmoketestm62a (schools and climbing animals in the world). */
    private static DevClientSmokeM62a m62a;
    /** M55: -Psmoketestm55 (wild carpets, wild plants, container icons). */
    private static DevClientSmokeM55 m55;
    private static DevClientSmokeM56 m56;
    /** M62 part 4: -Psmoketestm62c (every tab of the tank menu, en/de, three GUI scales). */
    private static DevClientSmokeM62c m62c;
    /** M61: -Psmoketestm61 (block changes without states, leaving and joining the world). */
    private static DevClientSmokeM61 m61;
    /** M63: -Psmoketestm63 (the tank water with every shader pack). */
    private static DevClientSmokeM63 m63;
    /** M66: -Psmoketestm66 (the menus of the UI overhaul). */
    private static DevClientSmokeM69 m69;
    private static DevClientSmokeM66 m66;
    /** M68: -Psmoketestm68 (the one pipe system). */
    private static DevClientSmokeM68 m68;
    /** M67: -Psmoketestm67 (one energy store per tank). */
    private static DevClientSmokeM67 m67;
    private static DevClientSmokeM675 m675;
    /** M65: -Psmoketestm65 (pipes, water at blocks against the glass). */
    private static DevClientSmokeM65 m65;
    /** M64: -Psmoketestm64 (the tank optics of M64). */
    private static DevClientSmokeM64 m64;
    private static DevClientSmokeM62b m62b;
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
        if (Boolean.getBoolean("bettacraft.smoketest.worldtour")) {
            // M63b: -Psmoketestworldtour: every tank of a copy of the showcase (DevClientWorldTour)
            ClientEvents.SCREEN_OPENING.add(screen -> {
                if (screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
                    Minecraft mc = Minecraft.getInstance();
                    mc.execute(() -> {
                        mc.options.onboardAccessibility = false;
                        mc.options.save();
                        mc.setScreen(new TitleScreen());
                    });
                }
            });
            DevClientWorldTour.register();
            net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> DevClientWorldTour.afterInit(screen));
        } else if (Boolean.getBoolean("bettacraft.smoketest.fabric")) {
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
            if (stage == 0 && ticks == 60 && Boolean.getBoolean("bettacraft.smoketest.m62bonly")) {
                LOGGER.info("[smoketest] fabric client in world: M62b scene");
                m62b = new DevClientSmokeM62b();
                stage = 25;
            }
            if (stage == 25) {
                if (m62b.tick()) {
                    stage = 3;
                    LOGGER.info("[smoketest] fabric client world test finished");
                    LOGGER.info(m62b.problems() == 0 ? "[smoketest] PASSED" : "[smoketest] FAILED: M62b");
                    mc.stop();
                }
                return;
            }
            if (stage == 0 && ticks == 60 && Boolean.getBoolean("bettacraft.smoketest.m62donly")) {
                LOGGER.info("[smoketest] fabric client in world: M62d scene");
                m62d = new DevClientSmokeM62d();
                stage = 624;
            }
            if (stage == 624) {
                if (m62d.tick()) stage = 4;
                return;
            }
            if (stage == 0 && ticks == 60 && Boolean.getBoolean("bettacraft.smoketest.m68only")) {
                LOGGER.info("[smoketest] fabric client in world: M68 scene");
                m68 = new DevClientSmokeM68();
                stage = 68;
            }
            if (stage == 68) {
                if (m68.tick()) {
                    stage = 3;
                    LOGGER.info("[smoketest] fabric client world test finished");
                    LOGGER.info(m68.problems() == 0 ? "[smoketest] PASSED" : "[smoketest] FAILED: M68");
                    mc.stop();
                }
                return;
            }
            if (stage == 0 && ticks == 60 && Boolean.getBoolean("bettacraft.smoketest.m675only")) {
                LOGGER.info("[smoketest] fabric client in world: M67.5 scene");
                m675 = new DevClientSmokeM675();
                stage = 675;
            }
            if (stage == 675) {
                if (m675.tick()) {
                    stage = 3;
                    LOGGER.info("[smoketest] fabric client world test finished");
                    LOGGER.info(m675.problems() == 0 ? "[smoketest] PASSED" : "[smoketest] FAILED: M67.5");
                    mc.stop();
                }
                return;
            }
            if (stage == 0 && ticks == 60 && Boolean.getBoolean("bettacraft.smoketest.m67only")) {
                LOGGER.info("[smoketest] fabric client in world: M67 scene");
                m67 = new DevClientSmokeM67();
                stage = 67;
            }
            if (stage == 67) {
                if (m67.tick()) {
                    stage = 3;
                    LOGGER.info("[smoketest] fabric client world test finished");
                    LOGGER.info(m67.problems() == 0 ? "[smoketest] PASSED" : "[smoketest] FAILED: M67");
                    mc.stop();
                }
                return;
            }
            if (stage == 0 && ticks == 60 && Boolean.getBoolean("bettacraft.smoketest.m69only")) {
                LOGGER.info("[smoketest] fabric client in world: M69 scene");
                m69 = new DevClientSmokeM69();
                stage = 69;
            }
            if (stage == 69) {
                if (m69.tick()) {
                    stage = 3;
                    LOGGER.info("[smoketest] fabric client world test finished");
                    LOGGER.info(m69.problems() == 0 ? "[smoketest] PASSED" : "[smoketest] FAILED: M69");
                    mc.stop();
                }
                return;
            }
            if (stage == 0 && ticks == 60 && Boolean.getBoolean("bettacraft.smoketest.m66only")) {
                LOGGER.info("[smoketest] fabric client in world: M66 scene");
                m66 = new DevClientSmokeM66();
                stage = 66;
            }
            if (stage == 66) {
                if (m66.tick()) {
                    stage = 3;
                    LOGGER.info("[smoketest] fabric client world test finished");
                    LOGGER.info(m66.problems() == 0 ? "[smoketest] PASSED" : "[smoketest] FAILED: M66");
                    mc.stop();
                }
                return;
            }
            if (stage == 0 && ticks == 60 && Boolean.getBoolean("bettacraft.smoketest.m65only")) {
                LOGGER.info("[smoketest] fabric client in world: M65 scene");
                m65 = new DevClientSmokeM65();
                stage = 65;
            }
            if (stage == 65) {
                if (m65.tick()) {
                    stage = 3;
                    LOGGER.info("[smoketest] fabric client world test finished");
                    LOGGER.info(m65.problems() == 0 ? "[smoketest] PASSED" : "[smoketest] FAILED: M65");
                    mc.stop();
                }
                return;
            }
            if (stage == 0 && ticks == 60 && Boolean.getBoolean("bettacraft.smoketest.m64only")) {
                LOGGER.info("[smoketest] fabric client in world: M64 scene");
                m64 = new DevClientSmokeM64();
                stage = 64;
            }
            if (stage == 64) {
                if (m64.tick()) {
                    stage = 3;
                    LOGGER.info("[smoketest] fabric client world test finished");
                    LOGGER.info(m64.problems() == 0 ? "[smoketest] PASSED" : "[smoketest] FAILED: M64");
                    mc.stop();
                }
                return;
            }
            if (stage == 0 && ticks == 60 && Boolean.getBoolean("bettacraft.smoketest.m63only")) {
                LOGGER.info("[smoketest] fabric client in world: M63 scene");
                m63 = new DevClientSmokeM63();
                stage = 63;
            }
            if (stage == 63) {
                if (m63.tick()) {
                    stage = 3;
                    LOGGER.info("[smoketest] fabric client world test finished");
                    LOGGER.info(m63.problems() == 0 ? "[smoketest] PASSED" : "[smoketest] FAILED: M63");
                    mc.stop();
                }
                return;
            }
            if (stage == 0 && ticks == 60 && Boolean.getBoolean("bettacraft.smoketest.m61only")) {
                LOGGER.info("[smoketest] fabric client in world: M61 scene");
                m61 = new DevClientSmokeM61();
                stage = 24;
            }
            if (stage == 24) {
                if (m61.tick()) {
                    // no tank in this scene: straight to the end (stage 4 checks the crack overlay at a tank of the M11 scene)
                    stage = 3;
                    LOGGER.info("[smoketest] fabric client world test finished");
                    LOGGER.info(m61.problems() == 0 ? "[smoketest] PASSED" : "[smoketest] FAILED: M61");
                    mc.stop();
                }
                return;
            }
            if (stage == 0 && ticks == 60 && Boolean.getBoolean("bettacraft.smoketest.m62conly")) {
                LOGGER.info("[smoketest] fabric client in world: M62c scene");
                m62c = new DevClientSmokeM62c();
                stage = 62;
            }
            if (stage == 62) {
                if (m62c.tick()) {
                    stage = 3;
                    LOGGER.info("[smoketest] fabric client world test finished");
                    mc.stop();
                }
                return;
            }
            if (stage == 0 && ticks == 60 && Boolean.getBoolean("bettacraft.smoketest.m56only")) {
                LOGGER.info("[smoketest] fabric client in world: M56 scene");
                m56 = new DevClientSmokeM56();
                stage = 23;
            }
            if (stage == 23) {
                if (m56.tick()) stage = 4;
                return;
            }
            if (stage == 0 && ticks == 60 && Boolean.getBoolean("bettacraft.smoketest.m55only")) {
                LOGGER.info("[smoketest] fabric client in world: M55 scene");
                m55 = new DevClientSmokeM55();
                stage = 22;
            }
            if (stage == 22) {
                if (m55.tick()) stage = 4;
                return;
            }
            if (stage == 0 && ticks == 60 && Boolean.getBoolean("bettacraft.smoketest.m62aonly")) {
                LOGGER.info("[smoketest] fabric client in world: M62a scene");
                m62a = new DevClientSmokeM62a();
                stage = 620;
            }
            if (stage == 620) {
                if (m62a.tick()) {
                    // no tank in this scene: straight to the end (stage 4 checks the crack overlay at a tank of the M11 scene)
                    stage = 3;
                    LOGGER.info("[smoketest] fabric client world test finished");
                    LOGGER.info("[smoketest] PASSED");
                    mc.stop();
                }
                return;
            }
            if (stage == 0 && ticks == 60 && Boolean.getBoolean("bettacraft.smoketest.m54only")) {
                LOGGER.info("[smoketest] fabric client in world: M54 scene");
                m54 = new DevClientSmokeM54();
                stage = 21;
            }
            if (stage == 21) {
                if (m54.tick()) stage = 4;
                return;
            }
            if (stage == 0 && ticks == 60 && Boolean.getBoolean("bettacraft.smoketest.m53only")) {
                LOGGER.info("[smoketest] fabric client in world: M53 scene");
                m53 = new DevClientSmokeM53();
                stage = 20;
            }
            if (stage == 20) {
                if (m53.tick()) stage = 4;
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
