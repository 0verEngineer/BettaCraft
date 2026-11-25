package org.overengineer.bettacraft.client.screen;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.overengineer.bettacraft.equipment.MenuEnergy;
import org.overengineer.bettacraft.tank.TankPower;

/** M66: the stored energy of a filter menu: "Energy: 12,000 / 100,000 FE" and a bar, with a tooltip that says whose store it is. */
final class EnergyRow {
    static final int BAR_W = 64;

    private EnergyRow() {}

    /** The row at {@code x, y} (menu coordinates) {@code w} wide. */
    static void draw(GuiGraphics g, Font font, int x, int y, int w, MenuEnergy e) {
        long cap = e.capacity();
        int room = w - BAR_W - 6;
        Component text;
        if (cap <= 0) text = Component.translatable("gui.bettacraft.filter.energy_none");
        else {
            text = Component.translatable("gui.bettacraft.filter.energy", String.format(java.util.Locale.ROOT, "%,d", e.stored()),
                    String.format(java.util.Locale.ROOT, "%,d", cap));
        }
        int tw = font.width(text);
        if (tw <= room) g.drawString(font, text, x, y, 0x404040, false);
        else {
            float s = Math.max(0.5F, room / (float) tw);
            g.pose().pushPose();
            g.pose().translate(x, y + (1 - s) * 4, 0);
            g.pose().scale(s, s, 1);
            g.drawString(font, text, 0, 0, 0x404040, false);
            g.pose().popPose();
        }
        int bx = x + w - BAR_W;
        g.fill(bx - 1, y, bx + BAR_W + 1, y + 8, 0xFF555555);
        g.fill(bx, y + 1, bx + BAR_W, y + 7, 0xFF202020);
        if (cap > 0) {
            int f = (int) Math.round(BAR_W * Math.min(1.0, e.stored() / (double) cap));
            if (f > 0) g.fill(bx, y + 1, bx + f, y + 7, 0xFFD04040);
        }
    }

    static boolean over(int mx, int my, int x, int y, int w) {
        return mx >= x && mx < x + w && my >= y - 1 && my < y + 9;
    }

    static void tooltip(GuiGraphics g, Font font, int mx, int my, MenuEnergy e) {
        List<FormattedCharSequence> lines = font.split(Component.translatable("gui.bettacraft.filter.energy_tooltip"), 200);
        if (!TankPower.anyRequired()) lines = new java.util.ArrayList<>(lines);
        if (!TankPower.anyRequired()) lines.addAll(font.split(Component.translatable("gui.bettacraft.filter.energy_off").withStyle(net.minecraft.ChatFormatting.GRAY), 200));
        g.renderTooltip(font, lines, mx, my);
    }
}
