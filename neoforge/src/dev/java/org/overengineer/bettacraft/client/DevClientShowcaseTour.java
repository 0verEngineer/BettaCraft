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
 * <p>
 * M21.1, {@code -Pshowcasebench}: instead of the pictures a frame-time bench of the plant meshes outside: at a few spots
 * (in front of the plant row, the whole showcase from above) the frames of 10 s are measured with
 * {@code render.miniaturePlantMeshDistance} 0 (voxels, as before), 24 (default) and 256 (every tank with meshes);
 * logged as {@code [smoketest] bench ...} lines, plus a picture per spot and setting.
 */
final class DevClientShowcaseTour {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    /** Must match DevShowcase: origin = spawn + (3, 0, 6), rows 9 blocks apart, the tanks 6 blocks apart. */
    private static final int ROW = 9;
    /** M21.5: the tanks stand on cabinets, one block higher: the camera too. */
    private static final double EYE = 2.9;
    /** M21.5: the plant row (Rotala, Vallisneria) after the two new colour rows. */
    private static final int PLANT_ROW = 5;

    private record Step(int delay, Runnable action) {}

    private static final boolean BENCH = Boolean.getBoolean("bettacraft.showcase.bench");
    private static boolean opened;
    /** Bench: frames and render-thread time of the miniatures / the plant meshes outside, summed while measuring. */
    private static boolean measuring;
    private static long frames, miniNanos, plantNanos, tanksWithMeshes, start;
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
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RenderFrameEvent.Post e) -> {
            if (!measuring) return;
            frames++;
            miniNanos += org.overengineer.bettacraft.client.interior.MiniatureRenderer.lastFrameNanos;
            plantNanos += org.overengineer.bettacraft.client.interior.MiniaturePlants.lastNanos;
            tanksWithMeshes += org.overengineer.bettacraft.client.interior.MiniaturePlants.lastTanks;
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
        if (BENCH) {
            bench();
            return;
        }
        // M21.5: the rows and their lengths are read from the world (the tanks of a row, scanned on the server)
        then(1, () -> {
            int[] lengths = rowLengths();
            LOGGER.info("[smoketest] showcase tour: {} rows, lengths {}", lengths.length, java.util.Arrays.toString(lengths));
            for (int row = 0; row < lengths.length; row++) {
                int rr = row;
                // close enough to see the animals and read the name plates: the named dry tank at the start of the row,
                // then one tank in the middle of each picture (2 wide, 6 apart), from the path in front of the row
                for (int n = 0, x = -4; x < lengths[row] + 4; n++, x = n == 1 ? 1 : x + 6) {
                    int nn = n, xx = x;
                    then(1, () -> look(xx, rr * ROW - 3.4, EYE, 0F, 16F));
                    then(n == 0 ? 100 : 45, () -> shot("showcase_" + (rr < 10 ? "0" : "") + rr + "_" + (nn < 10 ? "0" : "") + nn));
                }
            }
            // the whole showcase from above
            then(1, () -> look(70, -20, 44, 0F, 50F));
            then(160, () -> shot("showcase_overview"));
        });
    }

    /** M21.5: per row (9 blocks apart from the origin) the x of its last tank block (scanned on the server). */
    private int[] rowLengths() {
        var server = mc.getSingleplayerServer();
        return server.submit(() -> {
            var level = server.overworld();
            var spawn = level.getRespawnData().pos();
            int ground = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, spawn.getX() - 40, spawn.getZ() - 40);
            java.util.List<Integer> out = new java.util.ArrayList<>();
            for (int row = 0; row < 30; row++) {
                int last = -1;
                for (int x = 0; x < 200; x++)
                    for (int y = 0; y < 4 && last < x; y++)
                        for (int z = 0; z < 3; z++) {
                            var p = new net.minecraft.core.BlockPos(spawn.getX() + 3 + x, ground + y, spawn.getZ() + 6 + row * ROW + z);
                            if (level.getBlockState(p).is(org.overengineer.bettacraft.registry.ModBlocks.TANK.get())) {
                                last = x;
                                break;
                            }
                        }
                if (last < 0) break;
                out.add(last);
            }
            return out.stream().mapToInt(Integer::intValue).toArray();
        }).join();
    }

    private void bench() {
        then(1, () -> {
            mc.options.framerateLimit().set(260);
            mc.options.enableVsync().set(false);
            mc.getWindow().updateVsync(false);
        });
        // the plant row: Rotala (12th tank, x ~ 67) close up, Rotala and Vallisneria (15th, x ~ 85) from 15 blocks, and the
        // whole showcase from above (the plant row further away than 24 blocks)
        String[] names = {"rotala_near", "plants_mid", "overview"};
        double[][] spots = {{68, PLANT_ROW * ROW - 3.4, EYE, 16}, {76, PLANT_ROW * ROW - 15, 5, 12}, {70, -20, 44, 50}};
        int saved = org.overengineer.bettacraft.config.ClientConfig.MINIATURE_PLANT_MESH_DISTANCE.get();
        for (int i = 0; i < spots.length; i++) {
            double[] s = spots[i];
            String name = names[i];
            then(1, () -> look(s[0], s[1], s[2], 0F, (float) s[3]));
            for (int lod : new int[] {0, 24, 256}) {
                then(1, () -> org.overengineer.bettacraft.config.ClientConfig.MINIATURE_PLANT_MESH_DISTANCE.set(lod));
                then(lod == 0 ? 200 : 100, () -> {
                    frames = miniNanos = plantNanos = tanksWithMeshes = 0;
                    start = System.nanoTime();
                    measuring = true;
                });
                then(200, () -> {
                    measuring = false;
                    long n = Math.max(1, frames);
                    LOGGER.info(String.format(java.util.Locale.ROOT,
                            "[smoketest] bench %s lod %d: %d frames, %.2f ms per frame, miniatures %.3f ms, plant meshes outside %.3f ms, %.1f tanks with meshes",
                            name, lod, frames, (System.nanoTime() - start) / 1e6 / n, miniNanos / 1e6 / n, plantNanos / 1e6 / n, tanksWithMeshes / (double) n));
                    shot("bench_" + name + "_lod" + lod);
                });
            }
        }
        then(1, () -> org.overengineer.bettacraft.config.ClientConfig.MINIATURE_PLANT_MESH_DISTANCE.set(saved));
    }

    /** Camera relative to the showcase origin (spawn + (3, 0, 6)), eye height above the ground. */
    private void look(double x, double z, double eyeUp, float yaw, float pitch) {
        server(p -> {
            var spawn = p.level().getRespawnData().pos();
            int ground = p.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, spawn.getX() - 40, spawn.getZ() - 40);
            p.teleportTo(p.level(), spawn.getX() + 3 + x, ground + eyeUp - p.getEyeHeight(), spawn.getZ() + 6 + z, java.util.Set.of(), yaw, pitch, false);
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
