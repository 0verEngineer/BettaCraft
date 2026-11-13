package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M21: screenshot tour of the showcase world ({@code gradlew runClient -Pshowcasetour}, background window): opens
 * "Bettacraft Schaukasten" (built by {@code gradlew buildShowcase}), flies along the rows of {@code DevShowcase} and takes
 * {@code run/screenshots/showcase_<row>_<n>.png}, then an overview from above, and quits. The animals are not frozen.
 */
final class DevClientShowcaseTour {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    /** Must match DevShowcase: origin = spawn + (3, 0, 6), rows 9 blocks apart, the tanks 6 blocks apart. */
    private static final int ROWS = 10, ROW = 9;
    private static final int[] ROW_LENGTH = {96, 120, 72, 102, 30, 72, 60, 96, 140, 150};

    private record Step(int delay, Runnable action) {}

    private static boolean opened;
    private static DevClientShowcaseTour tour;
    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;

    static void register() {
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Opening e) -> {
            if (tour == null) LOGGER.info("[smoketest] showcase tour: screen {}", e.getNewScreen() == null ? "none" : e.getNewScreen().getClass().getName());
            if (!opened && e.getNewScreen() instanceof TitleScreen) {
                opened = true;
                Minecraft mc = Minecraft.getInstance();
                if (!mc.getLevelSource().levelExists("Bettacraft Schaukasten")) {
                    LOGGER.error("[smoketest] showcase world missing: run gradlew buildShowcase first");
                    mc.stop();
                    return;
                }
                mc.createWorldOpenFlows().openWorld("Bettacraft Schaukasten", () -> mc.setScreen(new TitleScreen()));
            }
        });
        // a world made by a dedicated server: the client offers a backup before opening it; just load it
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post e) -> {
            if (!opened || tour != null || !(e.getScreen() instanceof net.minecraft.client.gui.screens.BackupConfirmScreen
                    || e.getScreen() instanceof net.minecraft.client.gui.screens.ConfirmScreen)) return;
            String skip = net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinSkipButton").getString();
            net.minecraft.client.gui.components.Button pick = null;
            for (var w : e.getListenersList()) {
                if (!(w instanceof net.minecraft.client.gui.components.Button b) || !b.isActive()) continue;
                LOGGER.info("[smoketest] showcase tour: button {}", b.getMessage().getString());
                if (pick == null || b.getMessage().getString().equals(skip)) pick = b;
            }
            if (pick != null) {
                var b = pick;
                LOGGER.info("[smoketest] showcase tour: pressing {}", b.getMessage().getString());
                Minecraft.getInstance().execute(b::onPress);
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.getSingleplayerServer() == null) return;
            mc.options.pauseOnLostFocus = false;
            if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
            if (tour == null) {
                mc.getWindow().setWindowed(1280, 960);
                tour = new DevClientShowcaseTour();
            }
            try {
                if (tour.tick()) {
                    LOGGER.info("[smoketest] showcase tour finished");
                    mc.stop();
                }
            } catch (RuntimeException ex) {
                LOGGER.error("[smoketest] showcase tour FAILED", ex);
                mc.stop();
            }
        });
    }

    private DevClientShowcaseTour() {
        then(100, () -> mc.options.hideGui = true);
        then(1, () -> server(p -> p.getAbilities().flying = true));
        for (int row = 0; row < ROWS; row++) {
            int rr = row;
            // close enough to see the animals and read the name plates: the named dry tank at the start of the row,
            // then one tank in the middle of each picture (2 wide, 6 apart), from the path in front of the row
            for (int n = 0, x = -4; x < ROW_LENGTH[row] + 4; n++, x = n == 1 ? 1 : x + 6) {
                int nn = n, xx = x;
                then(1, () -> look(xx, rr * ROW - 3.4, 1.9, 0F, 16F));
                then(n == 0 ? 100 : 45, () -> shot("showcase_" + rr + "_" + (nn < 10 ? "0" : "") + nn));
            }
        }
        // the whole showcase from above
        then(1, () -> look(70, -20, 40, 0F, 50F));
        then(160, () -> shot("showcase_overview"));
    }

    /** Camera relative to the showcase origin (spawn + (3, 0, 6)), eye height above the ground. */
    private void look(double x, double z, double eyeUp, float yaw, float pitch) {
        server(p -> {
            var spawn = p.serverLevel().getSharedSpawnPos();
            int ground = p.serverLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, spawn.getX() - 40, spawn.getZ() - 40);
            p.teleportTo(p.serverLevel(), spawn.getX() + 3 + x, ground + eyeUp - p.getEyeHeight(), spawn.getZ() + 6 + z, yaw, pitch);
        });
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    boolean tick() {
        if (mc.player != null) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if (steps.isEmpty()) return true;
        Step s = steps.peek();
        if (wait < s.delay()) {
            wait++;
            return false;
        }
        steps.poll();
        wait = 0;
        s.action().run();
        return steps.isEmpty();
    }

    private void server(java.util.function.Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
