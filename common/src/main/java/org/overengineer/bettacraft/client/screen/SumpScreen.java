package org.overengineer.bettacraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.overengineer.bettacraft.equipment.Sump;
import org.overengineer.bettacraft.equipment.SumpMenu;

/**
 * M49 part a: the sump window: the chambers in three groups (heaters, skimmer; filter media; the refugium with algae and light),
 * drawn like the dosing pump window (no texture). What the sump does is in the tab "Sump" of the tank menu.
 */
public class SumpScreen extends AbstractContainerScreen<SumpMenu> {
    private static final int BG = 0xFFC6C6C6, DARK = 0xFF555555, LIGHT = 0xFFFFFFFF, SLOT = 0xFF8B8B8B, TEXT = 0x404040, WATER = 0xFF9CC8DC;
    private static final String[] TIPS = {"heater", "heater", "skimmer", "media", "media", "algae", "light"};

    public SumpScreen(SumpMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 178;
        inventoryLabelY = SumpMenu.INV_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, BG);
        g.fill(x, y, x + imageWidth, y + 1, LIGHT);
        g.fill(x, y, x + 1, y + imageHeight, LIGHT);
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, DARK);
        g.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, DARK);
        // the glass box of the sump with its water and the baffles between the chambers
        g.fill(x + 8, y + 14, x + 168, y + 82, 0xFF7A8A90);
        g.fill(x + 9, y + 18, x + 167, y + 81, WATER);
        g.fill(x + 61, y + 15, x + 63, y + 81, 0xFF6A7A80);
        g.fill(x + 97, y + 15, x + 99, y + 81, 0xFF6A7A80);
        g.fill(x + 99, y + 18, x + 167, y + 81, 0xFFA6D4B4); // the refugium: greener water
        for (Slot s : menu.slots) {
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, SLOT);
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y, DARK);
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x, y + s.y + 17, DARK);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY - 1, org.overengineer.bettacraft.client.ClientCompat.textColor(TEXT), false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, org.overengineer.bettacraft.client.ClientCompat.textColor(TEXT), false);
        small(g, Component.translatable("gui.bettacraft.sump.heaters"), 34, 41, 50);
        small(g, Component.translatable("gui.bettacraft.sump.skimmer"), 80, 41, 34);
        small(g, Component.translatable("gui.bettacraft.sump.refugium"), 133, 22, 66);
        small(g, Component.translatable("gui.bettacraft.sump.refugium_slots"), 133, 32, 66);
        small(g, Component.translatable("gui.bettacraft.sump.media"), 34, 72, 50);
    }

    private void small(GuiGraphics g, Component c, int cx, int y, int max) {
        float s = 0.75F;
        int w = font.width(c);
        if (w * s > max) s = max / (float) w;
        g.pose().pushMatrix();
        g.pose().translate(cx, y);
        g.pose().scale(s, s);
        g.drawString(font, c, -w / 2, 0, org.overengineer.bettacraft.client.ClientCompat.textColor(0x30404A), false);
        g.pose().popMatrix();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < Sump.CHAMBERS && menu.getCarried().isEmpty()) {
            g.setTooltipForNextFrame(font, font.split(Component.translatable("gui.bettacraft.sump.slot." + TIPS[hoveredSlot.index]), 180), mouseX, mouseY);
        }
    }
}
