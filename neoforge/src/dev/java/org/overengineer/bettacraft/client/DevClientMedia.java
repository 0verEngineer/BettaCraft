package org.overengineer.bettacraft.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M43: the release media ({@code gradlew runClient -Psmoketestm43}, background window): copies the showcase world
 * "Bettacraft Schaukasten" (made by {@code gradlew buildShowcase}) to a fresh "Bettacraft Medien" (the showcase itself stays
 * untouched), opens it and runs {@link DevClientSmokeReadme} in its media mode: the README room built beside the showcase,
 * the extra tanks and the fixed camera scenes of the showcase rows, at a fixed time of day and clear weather. With
 * {@code -Pshaders} the stills are taken with the shader pack (suffix {@code _shaders}). Then the client quits.
 */
final class DevClientMedia {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");
    /** M58: {@code -PmediaSource} / {@code -PmediaWorld} pick other worlds (a showcase built under another name, a copy that is not the user's). */
    static final String SOURCE = System.getProperty("bettacraft.media.source", "Bettacraft Schaukasten"),
            COPY = System.getProperty("bettacraft.media.world", "Bettacraft Medien");
    private static boolean opened;
    private static int ticks;
    private static DevClientSmokeReadme scene;
    private static boolean done;

    private DevClientMedia() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Opening e) -> {
            if (opened || !(e.getNewScreen() instanceof TitleScreen)) return;
            opened = true;
            Minecraft mc = Minecraft.getInstance();
            Path saves = mc.gameDirectory.toPath().resolve("saves");
            if (!Files.isDirectory(saves.resolve(SOURCE))) {
                LOGGER.error("[smoketest] media: showcase world missing: run gradlew buildShowcase first");
                mc.stop();
                return;
            }
            try {
                delete(saves.resolve(COPY));
                copy(saves.resolve(SOURCE), saves.resolve(COPY));
            } catch (IOException ex) {
                LOGGER.error("[smoketest] media: copying the showcase world failed", ex);
                mc.stop();
                return;
            }
            LOGGER.info("[smoketest] media: opening {}", COPY);
            mc.createWorldOpenFlows().openWorld(COPY, () -> mc.setScreen(new TitleScreen()));
        });
        // a world made by a dedicated server: the client offers a backup before opening it; just load it
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post e) -> {
            if (!opened || scene != null || !(e.getScreen() instanceof net.minecraft.client.gui.screens.BackupConfirmScreen
                    || e.getScreen() instanceof net.minecraft.client.gui.screens.ConfirmScreen)) return;
            String skip = net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinSkipButton").getString();
            net.minecraft.client.gui.components.Button pick = null;
            for (var w : e.getListenersList()) {
                if (!(w instanceof net.minecraft.client.gui.components.Button b) || !b.isActive()) continue;
                if (pick == null || b.getMessage().getString().equals(skip)) pick = b;
            }
            if (pick != null) {
                var b = pick;
                Minecraft.getInstance().execute(b::onPress);
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (done || mc.player == null || mc.getSingleplayerServer() == null) return;
            mc.options.pauseOnLostFocus = false;
            if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
            ticks++;
            if (ticks > 20) mc.player.getAbilities().flying = true;
            try {
                if (ticks == 100) scene = new DevClientSmokeReadme(true);
                if (scene != null && scene.tick()) {
                    done = true;
                    LOGGER.info("[smoketest] media finished");
                    mc.stop();
                }
            } catch (RuntimeException ex) {
                LOGGER.error("[smoketest] media FAILED", ex);
                done = true;
                mc.stop();
            }
        });
    }

    private static void copy(Path from, Path to) throws IOException {
        try (Stream<Path> files = Files.walk(from)) {
            for (Path f : (Iterable<Path>) files::iterator) {
                if (f.getFileName().toString().equals("session.lock")) continue;
                Path t = to.resolve(from.relativize(f).toString());
                if (Files.isDirectory(f)) Files.createDirectories(t);
                else Files.copy(f, t);
            }
        }
    }

    private static void delete(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path f : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(f);
        }
    }
}
