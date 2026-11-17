package org.overengineer.bettacraft.client;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.progression.GuideBook;
import org.overengineer.bettacraft.registry.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * M42 client smoke ({@code -Pintegration -Psmoketestm42}): the client side of the integrations with every integrated mod
 * loaded: JEI's recipe screens of the food maker (pellets) and the crucible (stainless steel) and the information page of a
 * filter medium, the GuideME handbook on the filter media page (its new automation paragraph). Screenshots
 * {@code bettacraft_m42_jei_pellets}, {@code _jei_steel}, {@code _jei_media}, {@code _guide_media}. The overlays (M8 scene)
 * and the JourneyMap waypoint (M32 part d scene) run with {@code -Pintegration} as their own scenes.
 */
final class DevClientSmokeM42 {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettacraft/smoketest");

    private record Step(int delay, Runnable action) {}

    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Step> steps = new ArrayDeque<>();
    private int wait;

    DevClientSmokeM42() {
        LOGGER.info("[smoketest] M42 mods: jei {}, guideme {}, create {}, ae2 {}", org.overengineer.bettacraft.platform.Platform.get().isModLoaded("jei"), org.overengineer.bettacraft.platform.Platform.get().isModLoaded("guideme"),
                org.overengineer.bettacraft.platform.Platform.get().isModLoaded("create"), org.overengineer.bettacraft.platform.Platform.get().isModLoaded("ae2"));
        then(1, () -> mc.options.hideGui = false);
        then(40, () -> jei(new ItemStack(ModItems.BETTA_PELLETS.get()), true));
        then(40, () -> shot("jei_pellets"));
        then(1, () -> mc.setScreen(null));
        then(10, () -> jei(new ItemStack(ModItems.STAINLESS_STEEL_INGOT.get()), true));
        then(40, () -> shot("jei_steel"));
        then(1, () -> mc.setScreen(null));
        then(10, () -> jei(new ItemStack(ModItems.ACTIVATED_CARBON.get()), true));
        then(40, () -> shot("jei_media"));
        then(1, () -> mc.setScreen(null));
        then(10, () -> {
            if (!org.overengineer.bettacraft.platform.Platform.get().isModLoaded("guideme")) {
                LOGGER.error("[smoketest] M42 GuideME missing");
                return;
            }
            org.overengineer.bettacraft.compat.guideme.GuideMECompat.open(mc.player, GuideBook.GUIDE_ID, BettaCraft.id("equipment/filter_media.md"));
        });
        then(60, () -> {
            LOGGER.info("[smoketest] M42 handbook screen: {}", mc.screen == null ? "NONE (FAIL)" : mc.screen.getClass().getSimpleName());
            shot("guide_media");
        });
        then(1, () -> mc.setScreen(null));
    }

    /** JEI's recipe screen of the stack (the recipes that make it, or with {@code output} false the ones that use it). */
    private void jei(ItemStack stack, boolean output) {
        try {
            var f = org.overengineer.bettacraft.compat.jei.BettaCraftJeiPlugin.class.getDeclaredField("runtime");
            f.setAccessible(true);
            mezz.jei.api.runtime.IJeiRuntime rt = (mezz.jei.api.runtime.IJeiRuntime) f.get(null);
            if (rt == null) {
                LOGGER.error("[smoketest] M42 JEI has no runtime (FAIL)");
                return;
            }
            var focus = rt.getJeiHelpers().getFocusFactory().createFocus(output ? mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT
                    : mezz.jei.api.recipe.RecipeIngredientRole.INPUT, mezz.jei.api.constants.VanillaTypes.ITEM_STACK, stack);
            rt.getRecipesGui().show(focus);
            LOGGER.info("[smoketest] M42 JEI recipes of {}: screen {}", stack, mc.screen == null ? "NONE" : mc.screen.getClass().getSimpleName());
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.error("[smoketest] M42 JEI (FAIL)", e);
        }
    }

    private void then(int delay, Runnable action) {
        steps.add(new Step(delay, action));
    }

    boolean tick() {
        if (mc.player != null && !steps.isEmpty()) {
            mc.player.getAbilities().flying = true;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
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

    @SuppressWarnings("unused")
    private void server(Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server.getPlayerList().getPlayers().getFirst()));
    }

    private void shot(String name) {
        mc.getToastManager().clear();
        Screenshot.grab(mc.gameDirectory, "bettacraft_m42_" + name + ".png", mc.getMainRenderTarget(), 1, c -> LOGGER.info("[smoketest] {}", c.getString()));
    }
}
