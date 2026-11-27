package org.overengineer.bettacraft.client.screen;

import net.minecraft.client.Minecraft;
import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.ExternalFilterMenu;
import org.overengineer.bettacraft.equipment.FilterHeaters;

/**
 * External filter: two heater slots (any size), a hint and the power regulator. M66: a bigger menu (216 x 184): the hint
 * fits in two lines, and the energy stored in the tank's store shows at the bottom.
 */
public class ExternalFilterScreen extends AbstractContainerScreen<ExternalFilterMenu> {
    static final Identifier TEXTURE = BettaCraft.id("textures/gui/external_filter.png");
    /** The energy row (menu coordinates). */
    private static final int ENERGY_X = 8, ENERGY_Y = 78, ENERGY_W = 200;

    public ExternalFilterScreen(ExternalFilterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, ExternalFilterMenu.WIDTH, ExternalFilterMenu.HEIGHT);
        inventoryLabelX = ExternalFilterMenu.INV_X;
        inventoryLabelY = ExternalFilterMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
        powerBtn = addRenderableWidget(powerButton(leftPos + 134, topPos + 35, 66, 16, menu::power, menu.containerId));
    }

    private TankScreen.FitButton powerBtn;

    /** M23: the power regulator button: click +10 %, sneak + click -10 % (tooltip explains). */
    private TankScreen.FitButton powerButton(int x, int y, int w, int h, java.util.function.IntSupplier value, int containerId) {
        TankScreen.FitButton b = new TankScreen.FitButton(x, y, w, h, Component.empty(), btn -> {
            if (minecraft == null || minecraft.gameMode == null) return;
            minecraft.gameMode.handleInventoryButtonClick(containerId, Minecraft.getInstance().hasShiftDown()
                    ? org.overengineer.bettacraft.equipment.ExternalFilterMenu.BUTTON_POWER_DOWN : org.overengineer.bettacraft.equipment.ExternalFilterMenu.BUTTON_POWER_UP);
        });
        b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.bettacraft.regulator.button_tooltip")));
        b.visible = Features.enabled(Feature.POWER_REGULATOR);
        return b;
    }


    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        SpringGauge.draw(g, leftPos + SPRING_X, topPos + SPRING_Y, SPRING_H, menu.springPercent()); // M37
        for (int i = 0; i < FilterHeaters.SLOTS; i++) {
            if (!menu.getSlot(i).hasItem()) {
                g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + ExternalFilterMenu.SLOT_X + i * 18, topPos + ExternalFilterMenu.SLOT_Y, 220, 18, 16, 16, 256, 256);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        // the hint under the slots, wrapped (a longer translation takes a third line, drawn smaller)
        Component hint = Component.translatable("gui.bettacraft.filter.heaters_hint");
        var lines = font.split(hint, imageWidth - 48); // right of the spring gauge
        int y = 56;
        for (var l : lines) {
            if (y > 66) break;
            g.text(font, l, (imageWidth - font.width(l)) / 2, y, org.overengineer.bettacraft.client.ClientCompat.textColor(0x606060), false);
            y += 9;
        }
        EnergyRow.draw(g, font, ENERGY_X, ENERGY_Y, ENERGY_W, menu.energy());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (powerBtn != null) powerBtn.setMessage(Component.translatable("gui.bettacraft.regulator.button", menu.power() * 10));
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (SpringGauge.over(mouseX, mouseY, leftPos + SPRING_X, topPos + SPRING_Y, SPRING_H)) SpringGauge.tooltip(g, font, mouseX, mouseY, menu.springPercent());
        else if (energyTipForTest || EnergyRow.over(mouseX - leftPos, mouseY - topPos, ENERGY_X, ENERGY_Y, ENERGY_W)) EnergyRow.tooltip(g, font, energyTipForTest ? leftPos + 10 : mouseX, energyTipForTest ? topPos + 60 : mouseY, menu.energy(), "gui.bettacraft.filter.energy_tooltip");
    }

    private boolean energyTipForTest;

    /** Dev smoke test only: shows the tooltip of the energy row. */
    public void showEnergyTipForTest() {
        energyTipForTest = true;
    }

    /** M37: the spring gauge on the left. */
    static final int SPRING_X = 12, SPRING_Y = 18, SPRING_H = 52;
}
