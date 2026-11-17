package org.overengineer.bettacraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.overengineer.bettacraft.reef.CalciumReactorBlockEntity;
import org.overengineer.bettacraft.reef.CalciumReactorMenu;

/** M49 part a: the calcium reactor window: the column of coral rubble, the status and the calcium and alkalinity of its tank. */
public class CalciumReactorScreen extends AbstractContainerScreen<CalciumReactorMenu> {
    private static final int BG = 0xFFC6C6C6, DARK = 0xFF555555, LIGHT = 0xFFFFFFFF, SLOT = 0xFF8B8B8B, TEXT = 0x404040;

    public CalciumReactorScreen(CalciumReactorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = CalciumReactorMenu.INV_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, BG);
        g.fill(x, y, x + imageWidth, y + 1, LIGHT);
        g.fill(x, y, x + 1, y + imageHeight, LIGHT);
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, DARK);
        g.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, DARK);
        // the column with the rubble in it
        g.fill(x + 70, y + 14, x + 106, y + 46, 0xFF6A7A80);
        g.fill(x + 71, y + 15, x + 105, y + 45, menu.status() == CalciumReactorBlockEntity.RUNNING ? 0xFFB8D8E4 : 0xFFA8B8C0);
        for (Slot s : menu.slots) {
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, SLOT);
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y, DARK);
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x, y + s.y + 17, DARK);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, org.overengineer.bettacraft.client.ClientCompat.textColor(TEXT), false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, org.overengineer.bettacraft.client.ClientCompat.textColor(TEXT), false);
        Component st = CalciumReactorBlockEntity.statusText(menu.status(), menu.tanks());
        int col = menu.status() == CalciumReactorBlockEntity.RUNNING ? 0x206020 : 0x9A5A10;
        small(g, st, imageWidth / 2, 52, col, imageWidth - 8);
        if (menu.tanks() > 0) {
            small(g, Component.translatable("gui.bettacraft.calcium_reactor.values", menu.calcium(), String.format(java.util.Locale.ROOT, "%.1f", menu.kh())),
                    imageWidth / 2, 62, TEXT, imageWidth - 8);
        }
    }

    private void small(GuiGraphics g, Component c, int cx, int y, int color, int max) {
        float s = 0.75F;
        int w = font.width(c);
        if (w * s > max) s = max / (float) w;
        g.pose().pushMatrix();
        g.pose().translate(cx, y);
        g.pose().scale(s, s);
        g.drawString(font, c, -w / 2, 0, org.overengineer.bettacraft.client.ClientCompat.textColor(color), false);
        g.pose().popMatrix();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
