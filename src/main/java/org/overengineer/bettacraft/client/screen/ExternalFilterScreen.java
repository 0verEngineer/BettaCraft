package org.overengineer.bettacraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.ExternalFilterMenu;
import org.overengineer.bettacraft.equipment.FilterHeaters;

/** External filter: two heater slots (any size) and a hint. */
public class ExternalFilterScreen extends AbstractContainerScreen<ExternalFilterMenu> {
    static final ResourceLocation TEXTURE = BettaCraft.id("textures/gui/external_filter.png");

    public ExternalFilterScreen(ExternalFilterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for (int i = 0; i < FilterHeaters.SLOTS; i++) {
            if (!menu.getSlot(i).hasItem()) {
                g.blit(TEXTURE, leftPos + ExternalFilterMenu.SLOT_X + i * 18, topPos + ExternalFilterMenu.SLOT_Y, 176, 18, 16, 16);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        Component hint = Component.translatable("gui.bettacraft.filter.heaters_hint");
        g.drawString(font, hint, (imageWidth - font.width(hint)) / 2, 58, 0x606060, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
