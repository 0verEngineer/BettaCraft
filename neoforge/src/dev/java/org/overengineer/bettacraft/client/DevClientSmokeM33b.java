package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.config.Feature;
import org.overengineer.bettacraft.config.FeatureGate;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.progression.GuideBook;
import org.overengineer.bettacraft.registry.ModCreativeTabs;
import org.overengineer.bettacraft.registry.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M33 part b client smoke ({@code -Psmoketestm33b}): the tankmates and salt water switched off: the creative tab without
 * them (screenshot {@code bettacraft_m33b_creative}, log lines PASS / FAIL for the tab contents), the handbook without their
 * chapters in the navigation ({@code bettacraft_m33b_guide}) and a page of a feature that is off with its notice
 * ({@code bettacraft_m33b_notice}); then everything on again and the tab is complete.
 */
final class DevClientSmokeM33b {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;
    private int allOn = -1;

    DevClientSmokeM33b() {
        then(1, () -> allOn = tabCount("all on"));
        then(1, () -> set(false));
        then(5, this::openCreativeTab);
        then(20, this::checkTab);
        then(10, () -> shot("creative"));
        then(1, () -> mc.setScreen(null));
        then(5, () -> org.overengineer.bettacraft.compat.guideme.GuideMECompat.open(mc.player, GuideBook.GUIDE_ID, BettaCraft.id("index.md")));
        then(30, this::checkGuide);
        then(10, () -> shot("guide"));
        then(5, () -> org.overengineer.bettacraft.compat.guideme.GuideMECompat.open(mc.player, GuideBook.GUIDE_ID, BettaCraft.id("tankmates/cichlids.md")));
        then(30, () -> shot("notice"));
        then(1, () -> mc.setScreen(null));
        then(1, () -> set(true));
        then(5, () -> {
            int n = tabCount("on again");
            LOGGER.info("[smoketest] M33b tab complete again {}: {} of {}", n == allOn ? "PASS" : "FAIL", n, allOn);
        });
    }

    private void set(boolean on) {
        // single player: both sides read the same common config
        Features.set(Feature.TANKMATES, on);
        Features.set(Feature.SALTWATER, on);
        ClientFeatures.refresh();
    }

    private int tabCount(String what) {
        ClientFeatures.invalidateCreativeTabs();
        CreativeModeTab tab = ModCreativeTabs.MAIN.get();
        net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(), true, mc.level.registryAccess());
        int n = tab.getDisplayItems().size();
        LOGGER.info("[smoketest] M33b creative tab ({}): {} entries", what, n);
        return n;
    }

    private void openCreativeTab() {
        var scr = new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true);
        mc.setScreen(scr);
        try {
            var m = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
            m.setAccessible(true);
            m.invoke(scr, ModCreativeTabs.MAIN.get());
        } catch (ReflectiveOperationException ex) {
            LOGGER.warn("[smoketest] M33b cannot select the creative tab: {}", ex.toString());
        }
    }

    private void checkTab() {
        var items = ModCreativeTabs.MAIN.get().getDisplayItems();
        long hidden = items.stream().filter(s -> !FeatureGate.stack(s)).count();
        boolean noNeons = items.stream().noneMatch(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().equals("neon_tetra_bucket"));
        boolean noSalt = items.stream().noneMatch(s -> s.is(ModItems.SEA_SALT.get()));
        boolean bettas = items.stream().anyMatch(s -> s.is(ModItems.BETTA_BUCKET.get()));
        boolean ok = hidden == 0 && noNeons && noSalt && bettas && items.size() < allOn;
        LOGGER.info("[smoketest] M33b creative tab without tankmates and salt water {}: {} entries (all on {}), off entries {}, neons {}, sea salt {}, bettas {}",
                ok ? "PASS" : "FAIL", items.size(), allOn, hidden, !noNeons, !noSalt, bettas);
    }

    private void checkGuide() {
        var guide = guideme.Guides.getById(GuideBook.GUIDE_ID);
        if (guide == null) {
            LOGGER.error("[smoketest] M33b FAIL: no handbook (GuideME missing?)");
            return;
        }
        var hidden = org.overengineer.bettacraft.compat.guideme.GuidePageFeatures.hidden(guide);
        boolean ok = hidden.contains(BettaCraft.id("tankmates.md")) && hidden.contains(BettaCraft.id("tankmates/neon_tetra.md"))
                && hidden.contains(BettaCraft.id("saltwater.md")) && !hidden.contains(BettaCraft.id("bettas.md"));
        LOGGER.info("[smoketest] M33b handbook hides the chapters {}: {} pages hidden", ok ? "PASS" : "FAIL", hidden.size());
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if (steps.isEmpty()) return true;
        Step st = steps.peek();
        if (wait < st.delay()) {
            wait++;
            return false;
        }
        steps.poll();
        wait = 0;
        st.action().run();
        return steps.isEmpty();
    }

    private void shot(String name) {
        mc.getToasts().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m33b_" + name + ".png", mc.getMainRenderTarget(), c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
