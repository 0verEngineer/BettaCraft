package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.neoforge.client.gui.modlist.ModListScreen;
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
        then(1, () -> mc.gui.setScreen(ModListScreen.create(null)));
        then(10, this::selectBettacraft);
        then(30, () -> shot("modlist"));
        then(1, () -> mc.gui.setScreen(null));
    }

    private void selectBettacraft() {
        if (!(mc.gui.screen() instanceof ModListScreen screen)) {
            LOGGER.error("[smoketest] M26.1 the mod list did not open");
            return;
        }
        // M52d: NeoForge 26.2 rebuilt the mod list (private list classes): filter it by the search box, select the one entry
        for (var child : screen.children()) {
            if (child instanceof net.minecraft.client.gui.components.EditBox box) box.setValue("bettacraft");
        }
        for (var child : screen.children()) {
            if (child instanceof net.minecraft.client.gui.components.AbstractSelectionList<?> list && !list.children().isEmpty()) {
                selectFirst(list);
                return;
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
        mc.gui.toastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m26_1_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }

    private static void selectFirst(net.minecraft.client.gui.components.AbstractSelectionList<?> list) {
        Object entry = list.children().get(0);
        for (java.lang.reflect.Method m : list.getClass().getMethods()) {
            if (m.getName().equals("setSelected") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isInstance(entry)) {
                try {
                    m.invoke(list, entry);
                    return;
                } catch (ReflectiveOperationException e) {
                    LOGGER.error("[smoketest] M26.1 could not select the entry", e);
                }
            }
        }
    }
}
