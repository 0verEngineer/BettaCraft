package org.overengineer.bettacraft.client;

import java.lang.reflect.Field;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** M52e: NeoForge 26.3 dropped {@code getGuiLeft} / {@code getGuiTop}; the smoke scenes read the screen position here. */
public final class DevGui {
    private static final Field LEFT = field("leftPos"), TOP = field("topPos");

    private DevGui() {}

    private static Field field(String name) {
        try {
            Field f = AbstractContainerScreen.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static int left(AbstractContainerScreen<?> screen) {
        try {
            return LEFT.getInt(screen);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public static int top(AbstractContainerScreen<?> screen) {
        try {
            return TOP.getInt(screen);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
