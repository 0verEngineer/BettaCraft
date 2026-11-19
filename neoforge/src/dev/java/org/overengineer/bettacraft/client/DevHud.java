package org.overengineer.bettacraft.client;

import net.minecraft.client.Minecraft;

/** M52d: 26.2 moved the "hide GUI" flag (F1) into the HUD, which only toggles it. */
public final class DevHud {
    private DevHud() {
    }

    public static void setHidden(Minecraft mc, boolean hidden) {
        if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle();
    }
}
