package org.overengineer.bettacraft.client.screen;

import org.overengineer.bettacraft.config.Features;
import org.overengineer.bettacraft.config.Feature;
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
 * {@code features.equipment.cabinetOutletSide} off).
 */
public class CabinetScreen extends AbstractContainerScreen<CabinetMenu> {
    private static final ResourceLocation TEXTURE = BettaCraft.id("textures/gui/cabinet.png");
    /** M22.4: below the heater slots, left of the inventory (the inventory label moves under the storage). */
    public static final int OUTLET_X = 7, OUTLET_Y = 62, OUTLET_W = 66, OUTLET_H = 12;
    /** M66: the stored energy under the heater row. */
    private static final int ENERGY_X = 8, ENERGY_Y = 80, ENERGY_W = 200;
    private TankScreen.FitButton outletButton;

    public CabinetScreen(CabinetMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = CabinetMenu.WIDTH; // M66: bigger: the front button's text fits, the energy shows
        imageHeight = CabinetMenu.HEIGHT;
        inventoryLabelX = CabinetMenu.INV_X;
        inventoryLabelY = CabinetMenu.INV_Y - 11;
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
        powerBtn = addRenderableWidget(powerButton(leftPos + OUTLET_X + OUTLET_W + 2, topPos + OUTLET_Y, 42, OUTLET_H, menu::power, menu.containerId));
        driveButton = addRenderableWidget(new DriveButton(leftPos + DRIVE_X, topPos + DRIVE_Y)); // M41
        // M44: the front of this cabinet (click: next, sneak + click: previous) and the joints
        frontButton = addRenderableWidget(new TankScreen.FitButton(leftPos + FRONT_X, topPos + FRONT_Y, FRONT_W, FRONT_H, Component.empty(), b -> {
            org.overengineer.bettacraft.equipment.CabinetFrontSetting f = menu.frontSetting();
            if (f == null || minecraft == null || minecraft.gameMode == null) return;
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CabinetMenu.BUTTON_FRONT0 + (hasShiftDown() ? f.previous() : f.next()).ordinal());
        }));
        jointButton = addRenderableWidget(new JointButton(leftPos + JOINT_X, topPos + FRONT_Y));
        // M49: opens the chambers of the sump (above its slot)
        sumpButton = addRenderableWidget(new TankScreen.FitButton(leftPos + CabinetMenu.SUMP_X - 1, topPos + CabinetMenu.SUMP_Y - 14, 18, 12,
                Component.translatable("gui.bettacraft.cabinet.sump_open"), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CabinetMenu.BUTTON_SUMP);
        }));
        sumpButton.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.cabinet.sump_open.tooltip")));
        updateOutlet();
    }

    // ------------------------------------------------------------------ M44 front

    /** M44: right of the title, above the storage: the front button and the joint switch. */
    public static final int FRONT_X = 100, FRONT_Y = 3, FRONT_W = 96, FRONT_H = 12, JOINT_X = FRONT_X + FRONT_W + 1, JOINT_W = 12;
    private TankScreen.FitButton frontButton;
    private JointButton jointButton;
    private org.overengineer.bettacraft.equipment.CabinetFrontSetting shownFront;

    private void updateFront() {
        if (frontButton == null) return;
        org.overengineer.bettacraft.equipment.CabinetFrontSetting f = menu.frontSetting();
        frontButton.visible = f != null;
        jointButton.visible = f != null;
        if (f == null || f == shownFront) return;
        shownFront = f;
        frontButton.setMessage(Component.translatable("gui.bettacraft.cabinet.front", Component.translatable(f.translationKey() + ".short")));
        frontButton.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.cabinet.front", Component.translatable(f.translationKey()))
                .append(NL).append(Component.translatable("gui.bettacraft.cabinet.front.tooltip").withStyle(net.minecraft.ChatFormatting.GRAY))));
    }

    /** M44: a small switch with a picture of two doors: a dark joint between them while the joints are on. */
    private final class JointButton extends net.minecraft.client.gui.components.Button {
        private Boolean shown;

        JointButton(int x, int y) {
            super(x, y, JOINT_W, FRONT_H, Component.translatable("gui.bettacraft.cabinet.joints"), b -> {
                if (minecraft == null || minecraft.gameMode == null) return;
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CabinetMenu.BUTTON_JOINTS);
            }, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean on = menu.joints();
            if (shown == null || shown != on) {
                shown = on;
                setTooltip(Tooltip.create(Component.translatable(on ? "gui.bettacraft.cabinet.joints.on" : "gui.bettacraft.cabinet.joints.off")
                        .append(NL).append(Component.translatable("gui.bettacraft.cabinet.joints.tooltip").withStyle(net.minecraft.ChatFormatting.GRAY))));
            }
            super.renderWidget(g, mouseX, mouseY, partialTick);
            int x = getX() + 3, y = getY() + 3, w = getWidth() - 6, h = getHeight() - 6;
            g.fill(x, y, x + w, y + h, 0xFFB08A58); // two doors in planks colour
            if (on) g.fill(x + w / 2, y, x + w / 2 + 1, y + h, 0xFF2A2320); // the joint
            else g.fill(x + w / 2, y + h / 2 - 1, x + w / 2 + 1, y + h / 2 + 1, 0xFF6A5236); // only the handles
        }

        @Override
        public void renderString(GuiGraphics g, net.minecraft.client.gui.Font font, int color) {
            // the picture instead of the text
        }
    }

    // ------------------------------------------------------------------ M41 Create shaft

    /** M41: left of the filter slot: the side of the Create shaft (Create's shaft icon; click: next side, sneak: back). */
    public static final int DRIVE_X = 6, DRIVE_Y = 17, DRIVE_S = 18;
    private DriveButton driveButton;
    private static final String NL = "\n";

    private final class DriveButton extends net.minecraft.client.gui.components.Button {
        private org.overengineer.bettacraft.equipment.DriveSide shown;
        private boolean allowed = true;

        DriveButton(int x, int y) {
            super(x, y, DRIVE_S, DRIVE_S, Component.empty(), b -> {
                if (minecraft == null || minecraft.gameMode == null) return;
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, hasShiftDown() ? CabinetMenu.BUTTON_DRIVE_PREVIOUS : CabinetMenu.BUTTON_DRIVE_NEXT);
            }, DEFAULT_NARRATION);
        }

        void update() {
            org.overengineer.bettacraft.equipment.DriveSide s = menu.driveSide();
            visible = s != null;
            boolean ok = menu.driveAllowed();
            if (s == null || (s == shown && ok == allowed)) return;
            shown = s;
            allowed = ok;
            Component side = Component.translatable(s.translationKey());
            net.minecraft.network.chat.MutableComponent tip = Component.translatable("gui.bettacraft.cabinet.drive", side);
            tip.append(NL).append(Component.translatable("gui.bettacraft.cabinet.drive.tooltip").withStyle(net.minecraft.ChatFormatting.GRAY));
            if (!ok) tip.append(NL).append(Component.translatable("gui.bettacraft.cabinet.drive.not_allowed").withStyle(net.minecraft.ChatFormatting.RED));
            setTooltip(Tooltip.create(tip));
            setMessage(Component.translatable("gui.bettacraft.cabinet.drive", side));
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            if (shown == null) return;
            net.minecraft.world.item.Item shaft = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath("create", "shaft"));
            g.renderItem(new net.minecraft.world.item.ItemStack(shaft), getX() + 1, getY() + 1);
            g.pose().pushPose();
            g.pose().translate(0, 0, 250); // above the item
            if (shown == org.overengineer.bettacraft.equipment.DriveSide.NONE) {
                g.fill(getX() + 1, getY() + 1, getX() + DRIVE_S - 1, getY() + DRIVE_S - 1, 0x99404040); // greyed: no shaft
            } else {
                String letter = Component.translatable(shown.translationKey() + ".short").getString();
                g.drawString(font, letter, getX() + DRIVE_S - 1 - font.width(letter), getY() + DRIVE_S - 9, allowed ? 0xFFFFFF : 0xFF5555, true);
            }
            g.pose().popPose();
        }

        @Override
        public void renderString(GuiGraphics g, net.minecraft.client.gui.Font font, int color) {
            // the icon and the side's letter instead of the text
        }
    }

    private TankScreen.FitButton powerBtn;
    /** M49: the button that opens the sump chambers. */
    private TankScreen.FitButton sumpButton;

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


    /** Label and visibility of the outlet button from the menu's synced value. */
    private void updateOutlet() {
        if (outletButton == null) return;
        OutletSide s = menu.outletSide();
        outletButton.visible = s != null;
        if (s != null) outletButton.setMessage(Component.translatable("gui.bettacraft.cabinet.outlet", Component.translatable(s.translationKey())));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateOutlet();
        updateFront();
        if (driveButton != null) driveButton.update();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (!menu.getSlot(CabinetMenu.FILTER).hasItem()) {
            g.blit(TEXTURE, leftPos + CabinetMenu.FILTER_X, topPos + CabinetMenu.FILTER_Y, 220, 34, 16, 16);
        }
        if (!menu.getSlot(CabinetMenu.AIR_PUMP).hasItem()) { // M29.1
            g.blit(TEXTURE, leftPos + CabinetMenu.AIR_PUMP_X, topPos + CabinetMenu.AIR_PUMP_Y, 220, 50, 16, 16);
        }
        if (org.overengineer.bettacraft.equipment.Sump.enabled() || menu.hasSump()) { // M49: the sump slot
            g.blit(TEXTURE, leftPos + CabinetMenu.SUMP_X - 1, topPos + CabinetMenu.SUMP_Y - 1, 220, 0, 18, 18);
            if (!menu.hasSump()) g.fill(leftPos + CabinetMenu.SUMP_X + 2, topPos + CabinetMenu.SUMP_Y + 5, leftPos + CabinetMenu.SUMP_X + 14,
                    topPos + CabinetMenu.SUMP_Y + 13, 0x557AA8C0); // a hint of water
        }
        if (menu.hasFilter()) {
            for (int i = 0; i < 2; i++) {
                int x = leftPos + CabinetMenu.HEATER_X + i * 18, y = topPos + CabinetMenu.HEATER_Y;
                g.blit(TEXTURE, x - 1, y - 1, 220, 0, 18, 18);
                if (!menu.getSlot(CabinetMenu.HEATER0 + i).hasItem()) g.blit(TEXTURE, x, y, 220, 18, 16, 16);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // M44: the title leaves room for the front button (long wood names are cut)
        boolean room = frontButton != null && frontButton.visible;
        String t = room ? font.plainSubstrByWidth(title.getString(), FRONT_X - titleLabelX - 2) : title.getString();
        g.drawString(font, t, titleLabelX, titleLabelY, 0x404040, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        EnergyRow.draw(g, font, ENERGY_X, ENERGY_Y, ENERGY_W, menu.energy()); // M66
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
                : i == CabinetMenu.SUMP ? "gui.bettacraft.cabinet.sump.tooltip"
                : i == CabinetMenu.FILTER ? "gui.bettacraft.cabinet.filter"
                : i >= CabinetMenu.HEATER0 && i < CabinetMenu.HEATER0 + 2 && menu.hasFilter() ? "gui.bettacraft.cabinet.heaters" : null;
        if (key != null) g.renderTooltip(font, font.split(Component.translatable(key), 180), mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        updateOutlet();
        updateFront();
        if (driveButton != null) driveButton.update();
        if (sumpButton != null) sumpButton.visible = menu.hasSump() && org.overengineer.bettacraft.equipment.Sump.enabled();
        if (powerBtn != null) {
            powerBtn.visible = Features.enabled(Feature.POWER_REGULATOR) && menu.hasFilter();
            powerBtn.setMessage(Component.literal(menu.power() * 10 + "%"));
        }
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (energyTipForTest || EnergyRow.over(mouseX - leftPos, mouseY - topPos, ENERGY_X, ENERGY_Y, ENERGY_W)) EnergyRow.tooltip(g, font, energyTipForTest ? leftPos + 10 : mouseX, energyTipForTest ? topPos + 60 : mouseY, menu.energy(), "gui.bettacraft.filter.energy_tooltip.cabinet");
    }

    private boolean energyTipForTest;

    /** Dev smoke test only: shows the tooltip of the energy row. */
    public void showEnergyTipForTest() {
        energyTipForTest = true;
    }
}
