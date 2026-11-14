package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.neoforge.client.gui.ModListScreen;
import net.neoforged.neoforge.client.gui.widget.ModListWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M26.1 client smoke ({@code -Psmoketestm26_1}): the NeoForge mod list with Bettacraft selected, showing the wide logo
 * ({@code logoFile}) above the mod information. Screenshot {@code bettacraft_m26_1_modlist}. Checks (errors in the log):
 * the mod list has a Bettacraft entry.
 */
final class DevClientSmokeM261 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;

    DevClientSmokeM261() {
        then(1, () -> mc.setScreen(new ModListScreen(null)));
        then(10, this::selectBettacraft);
        then(30, () -> shot("modlist"));
        then(1, () -> mc.setScreen(null));
    }

    private void selectBettacraft() {
        if (!(mc.screen instanceof ModListScreen screen)) {
            LOGGER.error("[smoketest] M26.1 the mod list did not open");
            return;
        }
        for (var child : screen.children()) {
            if (child instanceof ModListWidget list) {
                for (ModListWidget.ModEntry entry : list.children()) {
                    if ("bettacraft".equals(entry.getInfo().getModId())) {
                        screen.setSelected(entry);
                        list.setSelected(entry);
                        return;
                    }
                }
            }
        }
        LOGGER.error("[smoketest] M26.1 no Bettacraft entry in the mod list");
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    boolean tick() {
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

    private void shot(String name) {
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m26_1_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
