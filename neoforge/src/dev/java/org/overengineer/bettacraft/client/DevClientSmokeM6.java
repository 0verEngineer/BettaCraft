package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.client.screen.TankScreen;
import org.overengineer.bettacraft.progression.GuideBook;
import org.overengineer.bettacraft.tank.TankBlockEntity;
import org.overengineer.bettacraft.tank.TankMultiblock;
import org.overengineer.bettacraft.tank.menu.TankMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M6 part of the dev client smoke test: clean world shots and tank menu tabs for the handbook images (en and de),
 * handbook pages (en and de) and the advancement tab. Runs after {@link DevClientSmokeTest} built its scene.
 */
final class DevClientSmokeM6 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    private static final String[] GUIDE_PAGES = {"index", "getting_started", "bettas", "bettas/types", "aquarium", "aquarium/menu",
            "equipment", "equipment/heaters", "food", "food/fry_food", "breeding", "interior/decor", "settings"};

    private record Step(int delay, BooleanSupplier ready, Runnable action) {}

    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private CompletableFuture<Void> reload;

    /** Camera spots (feet position, yaw, pitch) prepared by the scene. */
    record Spot(String name, double x, double y, double z, float yaw, float pitch) {}

    private final Minecraft mc = Minecraft.getInstance();

    DevClientSmokeM6(java.util.List<Spot> worldShots, BlockPos overviewTank, BlockPos typesTank, BlockPos breedTank) {
        // English world shots for the handbook, without HUD
        then(1, () -> mc.options.hideGui = true);
        for (Spot s : worldShots) {
            then(1, () -> server(p -> p.teleportTo(p.serverLevel(), s.x(), s.y(), s.z(), s.yaw(), s.pitch())));
            then(45, () -> shot("guide_" + s.name()));
        }
        menus("", overviewTank, typesTank, breedTank);
        guide("en");
        advancements("en");
        // German
        then(5, () -> {
            mc.setScreen(null);
            mc.options.languageCode = "de_de";
            mc.getLanguageManager().setSelected("de_de");
            reload = mc.reloadResourcePacks();
        });
        until(() -> reload != null && reload.isDone());
        then(60, () -> LOGGER.info("[smoketest] language now {}", mc.getLanguageManager().getSelected()));
        menus("_de", overviewTank, typesTank, breedTank);
        guide("de");
        advancements("de");
        then(5, () -> {
            mc.setScreen(null);
            mc.options.hideGui = false;
            // leave the dev options in English (only the file, no second reload)
            mc.options.languageCode = "en_us";
            mc.getLanguageManager().setSelected("en_us");
            mc.options.save();
        });
    }

    private void menus(String suffix, BlockPos overviewTank, BlockPos typesTank, BlockPos breedTank) {
        menu(overviewTank, 0, "guide_menu_overview" + suffix);
        menu(typesTank, 1, "guide_menu_inhabitants" + suffix);
        menu(breedTank, 2, "guide_menu_breeding" + suffix);
        then(5, () -> server(ServerPlayer::closeContainer));
    }

    private void menu(BlockPos tank, int tab, String name) {
        then(5, () -> server(p -> {
            p.closeContainer();
            // close enough for the menu (stillValid), the tank behind the screen
            p.teleportTo(p.serverLevel(), tank.getX() + 1.0, tank.getY() + 1.0, tank.getZ() - 2.5, 0F, 20F);
            TankBlockEntity c = TankMultiblock.getController(p.serverLevel(), tank);
            if (c == null) return;
            p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new TankMenu(id, inv, c), Component.translatable("gui.bettacraft.tank.title")),
                    buf -> buf.writeBlockPos(c.getBlockPos()));
        }));
        then(30, () -> {
            if (mc.screen instanceof TankScreen ts) ts.selectTabForTest(tab);
        });
        then(25, this::mouseToTopRight);
        then(5, () -> shot(name));
    }

    /** Handbook and advancement shots at 1280x720 (automatic GUI scale), the rest at 1280x960 (GUI scale 4). */
    private void guiScale(int scale) {
        if (scale == 3) mc.getWindow().setWindowed(1280, 720);
        else mc.getWindow().setWindowed(1280, 960);
    }

    private void guide(String lang) {
        then(1, () -> guiScale(3));
        guidePages(lang);
        if (org.overengineer.bettacraft.platform.Platform.get().isModLoaded("jei")) {
            // JEI information page (the "without GuideME" help) of the tank and of the mosquito larvae
            for (var item : java.util.List.of(org.overengineer.bettacraft.registry.ModItems.TANK, org.overengineer.bettacraft.registry.ModItems.MOSQUITO_LARVAE)) {
                then(5, () -> {
                    mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
                });
                then(10, () -> LOGGER.info("[smoketest] JEI info for {}: {}", item.getId(),
                        org.overengineer.bettacraft.compat.jei.BettaCraftJeiPlugin.showInfo(new net.minecraft.world.item.ItemStack(item.get()))));
                then(25, this::mouseToTopRight);
                then(5, () -> shot("m6_jei_" + lang + "_" + item.getId().getPath()));
            }
            then(5, () -> mc.setScreen(null));
        }
        then(1, () -> guiScale(0));
    }

    private void guidePages(String lang) {
        if (!GuideBook.available()) {
            then(1, () -> LOGGER.info("[smoketest] GuideME not installed: no handbook pages"));
            return;
        }
        then(5, () -> server(ServerPlayer::closeContainer));
        for (String page : GUIDE_PAGES) {
            then(10, () -> org.overengineer.bettacraft.compat.guideme.GuideMECompat.open(mc.player, GuideBook.GUIDE_ID, BettaCraft.id(page + ".md")));
            then(45, this::mouseToCorner);
            then(5, () -> shot("m6_guide_" + lang + "_" + page.replace('/', '_')));
        }
        then(5, () -> mc.setScreen(null));
    }

    private void advancements(String lang) {
        then(1, () -> guiScale(3));
        then(5, () -> {
            mc.setScreen(null);
            mc.getToasts().clear();
            var adv = mc.player.connection.getAdvancements();
            mc.setScreen(new net.minecraft.client.gui.screens.advancements.AdvancementsScreen(adv));
        });
        then(10, () -> {
            var adv = mc.player.connection.getAdvancements();
            var root = adv.get(BettaCraft.id("root"));
            if (root != null) adv.setSelectedTab(root, true);
        });
        then(25, this::mouseToCorner);
        then(5, () -> shot("m6_advancements_" + lang));
        then(5, () -> mc.setScreen(null));
        then(1, () -> guiScale(0));
    }

    /** Server side: every Bettacraft advancement for the player (to show the whole tree). */
    static void grantAll(ServerPlayer p) {
        for (var holder : p.server.getAdvancements().getAllAdvancements()) {
            if (!holder.id().getNamespace().equals(BettaCraft.MOD_ID) || holder.id().getPath().startsWith("recipes/")) continue;
            var progress = p.getAdvancements().getOrStartProgress(holder);
            for (String c : progress.getRemainingCriteria()) p.getAdvancements().award(holder, c);
        }
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, () -> true, action));
    }

    private void until(BooleanSupplier ready) {
        steps.add(new Step(0, ready, () -> {}));
    }

    /** Returns true when all steps are done. */
    boolean tick() {
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (steps.isEmpty()) return true;
        Step s = steps.peek();
        if (wait < s.delay()) {
            wait++;
            return false;
        }
        if (!s.ready().getAsBoolean()) return false;
        steps.poll();
        wait = 0;
        s.action().run();
        return steps.isEmpty();
    }

    /** Mouse to the bottom right corner (no hover tooltips, navigation bar closed). */
    private void mouseToCorner() {
        mouseTo(mc.getWindow().getWidth() - 2, mc.getWindow().getHeight() - 2);
    }

    /** Container screens: top right (JEI has its config button bottom right). */
    private void mouseToTopRight() {
        mouseTo(mc.getWindow().getWidth() - 2, 2);
    }

    private void mouseTo(double x, double y) {
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), x, y);
        try {
            for (String f : new String[]{"xpos", "ypos"}) {
                java.lang.reflect.Field fld = net.minecraft.client.MouseHandler.class.getDeclaredField(f);
                fld.setAccessible(true);
                fld.setDouble(mc.mouseHandler, f.equals("xpos") ? x : y);
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("[smoketest] cannot move the mouse: {}", e.toString());
        }
    }

    private void server(java.util.function.Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
