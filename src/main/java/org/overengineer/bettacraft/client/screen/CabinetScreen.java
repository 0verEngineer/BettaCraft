package org.overengineer.bettacraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.CabinetMenu;
import org.overengineer.bettacraft.equipment.OutletSide;

/**
 * Cabinet: filter slot, the two heater slots of that filter (only with a filter), 3x3 storage; M22.4: the button
 * "Outlet: back / left / right / front" (which tank wall the filter hose goes up; hidden while the server has
 * {@code features.cabinetOutletSide} off).
 */
public class CabinetScreen extends AbstractContainerScreen<CabinetMenu> {
    private static final ResourceLocation TEXTURE = BettaCraft.id("textures/gui/cabinet.png");
    /** M22.4: below the heater slots, left of the inventory (the inventory label moves under the storage). */
    public static final int OUTLET_X = 7, OUTLET_Y = 62, OUTLET_W = 58, OUTLET_H = 12;
    private TankScreen.FitButton outletButton;

    public CabinetScreen(CabinetMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        outletButton = new TankScreen.FitButton(leftPos + OUTLET_X, topPos + OUTLET_Y, OUTLET_W, OUTLET_H, Component.empty(), b -> {
            OutletSide s = menu.outletSide();
            if (s == null || minecraft == null || minecraft.gameMode == null) return;
            OutletSide next = hasShiftDown() ? OutletSide.byIndex((s.ordinal() + OutletSide.values().length - 1) % OutletSide.values().length) : s.next();
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CabinetMenu.BUTTON_OUTLET0 + next.ordinal());
        });
        outletButton.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.cabinet.outlet.tooltip")));
        addRenderableWidget(outletButton);
        powerBtn = addRenderableWidget(powerButton(leftPos + OUTLET_X + OUTLET_W + 2, topPos + OUTLET_Y, 84 - OUTLET_W, OUTLET_H, menu::power, menu.containerId));
        updateOutlet();
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
        b.visible = org.overengineer.bettacraft.config.CommonConfig.powerRegulator();
        return b;
    }


    /** Label and visibility of the outlet button from the menu's synced value. */
    private void updateOutlet() {
        if (outletButton == null) return;
        OutletSide s = menu.outletSide();
        outletButton.visible = s != null;
        if (s != null) outletButton.setMessage(Component.translatable("gui.bettacraft.cabinet.outlet", Component.translatable(s.translationKey())));
        // the inventory label leaves room for the button
        inventoryLabelX = s != null ? 98 : 8;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateOutlet();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (!menu.getSlot(CabinetMenu.FILTER).hasItem()) {
            g.blit(TEXTURE, leftPos + CabinetMenu.FILTER_X, topPos + CabinetMenu.FILTER_Y, 176, 34, 16, 16);
        }
        if (!menu.getSlot(CabinetMenu.AIR_PUMP).hasItem()) { // M29.1
            g.blit(TEXTURE, leftPos + CabinetMenu.AIR_PUMP_X, topPos + CabinetMenu.AIR_PUMP_Y, 176, 50, 16, 16);
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
        // M29.1: the heaters' label gave way to the air pump slot; empty slots explain themselves in a tooltip
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        super.renderTooltip(g, mouseX, mouseY);
        if (hoveredSlot == null || hoveredSlot.hasItem() || !menu.getCarried().isEmpty()) return;
        int i = hoveredSlot.index;
        String key = i == CabinetMenu.AIR_PUMP ? "gui.bettacraft.cabinet.air_pump.tooltip"
                : i == CabinetMenu.FILTER ? "gui.bettacraft.cabinet.filter"
                : i >= CabinetMenu.HEATER0 && i < CabinetMenu.HEATER0 + 2 && menu.hasFilter() ? "gui.bettacraft.cabinet.heaters" : null;
        if (key != null) g.renderTooltip(font, font.split(Component.translatable(key), 180), mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        updateOutlet();
        if (powerBtn != null) {
            powerBtn.visible = org.overengineer.bettacraft.config.CommonConfig.powerRegulator() && menu.hasFilter();
            powerBtn.setMessage(Component.literal(menu.power() * 10 + "%"));
        }
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
