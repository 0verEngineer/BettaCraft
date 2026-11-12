package org.overengineer.bettacraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.CabinetMenu;

/** Cabinet: filter slot, the two heater slots of that filter (only with a filter), 3x3 storage. */
public class CabinetScreen extends AbstractContainerScreen<CabinetMenu> {
    private static final ResourceLocation TEXTURE = BettaCraft.id("textures/gui/cabinet.png");

    public CabinetScreen(CabinetMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (!menu.getSlot(CabinetMenu.FILTER).hasItem()) {
            g.blit(TEXTURE, leftPos + CabinetMenu.FILTER_X, topPos + CabinetMenu.FILTER_Y, 176, 34, 16, 16);
        }
        if (menu.hasFilter()) {
            for (int i = 0; i < 2; i++) {
                int x = leftPos + CabinetMenu.HEATER_X + i * 18, y = topPos + CabinetMenu.HEATER_Y;
                g.blit(TEXTURE, x - 1, y - 1, 176, 0, 18, 18);
                if (!menu.getSlot(CabinetMenu.HEATER0 + i).hasItem()) g.blit(TEXTURE, x, y, 176, 18, 16, 16);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        Component f = Component.translatable("gui.bettacraft.cabinet.filter");
        g.drawString(font, font.plainSubstrByWidth(f.getString(), 50), CabinetMenu.FILTER_X + 20, CabinetMenu.FILTER_Y + 4, 0x606060, false);
        if (menu.hasFilter()) {
            Component h = Component.translatable("gui.bettacraft.cabinet.heaters");
            g.drawString(font, font.plainSubstrByWidth(h.getString(), 42), CabinetMenu.HEATER_X + 38, CabinetMenu.HEATER_Y + 4, 0x606060, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
