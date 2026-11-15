package org.overengineer.bettacraft.client.screen;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
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
        powerBtn = addRenderableWidget(powerButton(leftPos + 112, topPos + 35, 56, 16, menu::power, menu.containerId));
    }

    private TankScreen.FitButton powerBtn;

    /** M23: the power regulator button: click +10 %, sneak + click -10 % (tooltip explains). */
    private TankScreen.FitButton powerButton(int x, int y, int w, int h, java.util.function.IntSupplier value, int containerId) {
        TankScreen.FitButton b = new TankScreen.FitButton(x, y, w, h, Component.empty(), btn -> {
            if (minecraft == null || minecraft.gameMode == null) return;
            minecraft.gameMode.handleInventoryButtonClick(containerId, hasShiftDown()
                    ? org.overengineer.bettacraft.equipment.ExternalFilterMenu.BUTTON_POWER_DOWN : org.overengineer.bettacraft.equipment.ExternalFilterMenu.BUTTON_POWER_UP);
        });
        b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.bettacraft.regulator.button_tooltip")));
        b.visible = Features.enabled(Feature.POWER_REGULATOR);
        return b;
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
        if (powerBtn != null) powerBtn.setMessage(Component.translatable("gui.bettacraft.regulator.button", menu.power() * 10));
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
