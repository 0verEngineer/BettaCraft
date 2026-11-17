package org.overengineer.bettacraft.platform.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * M52: the client events the common client code listens to; each loader's client glue fires them from its own events, in
 * the order the listeners were added.
 */
public final class ClientEvents {
    private ClientEvents() {}

    /** The end of a client tick. */
    public static final List<Runnable> TICK_POST = new CopyOnWriteArrayList<>();

    /** A level render stage. */
    public static final List<Consumer<RenderLevelStageEvent>> RENDER_LEVEL = new CopyOnWriteArrayList<>();

    @FunctionalInterface
    public interface Scroll {
        /** The mouse wheel turned outside a screen; true takes the scroll (the hotbar does not move). */
        boolean onScroll(double deltaX, double deltaY);
    }

    public static final List<Scroll> MOUSE_SCROLL = new CopyOnWriteArrayList<>();

    @FunctionalInterface
    public interface Tooltip {
        void onTooltip(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag);
    }

    /** An item tooltip is built (lines may be added). */
    public static final List<Tooltip> ITEM_TOOLTIP = new CopyOnWriteArrayList<>();

    /** A screen is about to open (NeoForge: ScreenEvent.Opening; Fabric: before it is initialised). */
    public static final List<Consumer<net.minecraft.client.gui.screens.Screen>> SCREEN_OPENING = new CopyOnWriteArrayList<>();

    public static void fireScreenOpening(net.minecraft.client.gui.screens.Screen screen) {
        for (Consumer<net.minecraft.client.gui.screens.Screen> c : SCREEN_OPENING) c.accept(screen);
    }

    public static void fireTickPost() {
        for (Runnable r : TICK_POST) r.run();
    }

    public static void fireRenderLevel(RenderLevelStageEvent e) {
        for (Consumer<RenderLevelStageEvent> c : RENDER_LEVEL) c.accept(e);
    }

    public static boolean fireScroll(double dx, double dy) {
        boolean taken = false;
        for (Scroll s : MOUSE_SCROLL) taken |= s.onScroll(dx, dy);
        return taken;
    }

    public static void fireTooltip(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        for (Tooltip t : ITEM_TOOLTIP) t.onTooltip(stack, context, lines, flag);
    }
}
