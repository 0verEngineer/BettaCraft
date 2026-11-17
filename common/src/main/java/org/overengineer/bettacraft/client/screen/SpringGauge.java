package org.overengineer.bettacraft.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * M37: the spring of a device as a vertical gauge in its window (brass on dark, filled from below) with a tooltip:
 * "Spring: 40 % (wind it with the crank)". Hidden while the device may not be wound ({@code pct < 0}).
 */
final class SpringGauge {
    static final int W = 6;

    private SpringGauge() {}

    static void draw(GuiGraphics g, int x, int y, int h, int pct) {
        if (pct < 0) return;
        g.fill(x - 1, y - 1, x + W + 1, y + h + 1, 0xFF555555);
        g.fill(x, y, x + W, y + h, 0xFF2A2A2A);
        int f = (int) Math.round(h * Math.min(100, pct) / 100.0);
        if (f > 0) g.fill(x, y + h - f, x + W, y + h, 0xFFC8A040);
        // the coil: a few darker rings
        for (int i = y + 3; i < y + h; i += 6) g.fill(x, i, x + W, i + 1, 0x40000000);
    }

    static boolean over(int mouseX, int mouseY, int x, int y, int h) {
        return mouseX >= x - 1 && mouseX < x + W + 1 && mouseY >= y - 1 && mouseY < y + h + 1;
    }

    static void tooltip(GuiGraphics g, Font font, int mouseX, int mouseY, int pct) {
        if (pct < 0) return;
        g.renderTooltip(font, font.split(Component.translatable("gui.bettacraft.spring.tooltip", pct), 200), mouseX, mouseY);
    }
}
