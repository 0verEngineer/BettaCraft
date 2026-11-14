package org.overengineer.bettacraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.overengineer.bettacraft.reef.DosingCanisterItem;
import org.overengineer.bettacraft.reef.DosingPumpBlockEntity;
import org.overengineer.bettacraft.reef.DosingPumpMenu;

/**
 * M25 part c: the dosing pump window: four columns (canister, solution, mL per day with - and +; sneaking: by 10), what
 * the pump does and the alkalinity, calcium, magnesium and trace elements of the tank it doses into.
 */
public class DosingPumpScreen extends AbstractContainerScreen<DosingPumpMenu> {
    private static final int BG = 0xFFC6C6C6, DARK = 0xFF555555, LIGHT = 0xFFFFFFFF, SLOT = 0xFF8B8B8B, TEXT = 0x404040;

    public DosingPumpScreen(DosingPumpMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 186;
        inventoryLabelY = DosingPumpMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        for (int ch = 0; ch < DosingPumpBlockEntity.CHANNELS; ch++) {
            int x0 = leftPos + 3 + ch * 43, c = ch;
            Button minus = addRenderableWidget(Button.builder(Component.literal("-"), b -> click(c, false)).bounds(x0 + 3, topPos + 59, 17, 12).build());
            Button plus = addRenderableWidget(Button.builder(Component.literal("+"), b -> click(c, true)).bounds(x0 + 22, topPos + 59, 17, 12).build());
            minus.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.dosing.button")));
            plus.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.dosing.button")));
        }
    }

    private void click(int ch, boolean up) {
        int k = (up ? 1 : 0) + (Screen.hasShiftDown() ? 2 : 0);
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ch * 4 + k);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, BG);
        g.fill(x, y, x + imageWidth, y + 1, LIGHT);
        g.fill(x, y, x + 1, y + imageHeight, LIGHT);
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, DARK);
        g.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, DARK);
        // the channel columns
        for (int ch = 1; ch < DosingPumpBlockEntity.CHANNELS; ch++) g.fill(x + 3 + ch * 43 - 1, y + 17, x + 3 + ch * 43, y + 72, 0xFFA8A8A8);
        for (Slot s : menu.slots) {
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, SLOT);
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y, DARK);
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x, y + s.y + 17, DARK);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        for (int ch = 0; ch < DosingPumpBlockEntity.CHANNELS; ch++) {
            int cx = 3 + ch * 43 + 21;
            ItemStack s = menu.slots.get(ch).getItem();
            Component what = s.getItem() instanceof DosingCanisterItem c
                    ? Component.translatable(c.additive() == null ? "gui.bettacraft.dosing.dry" : "gui.bettacraft.dosing.additive." + c.additive().id())
                    : Component.translatable("gui.bettacraft.dosing.no_canister");
            small(g, what, cx, 41, s.isEmpty() || (s.getItem() instanceof DosingCanisterItem c && c.additive() == null) ? 0x9A2020 : TEXT, 41);
            int ml = menu.perDay(ch);
            small(g, ml <= 0 ? Component.translatable("gui.bettacraft.dosing.channel_off") : Component.translatable("gui.bettacraft.dosing.per_day", ml), cx, 50,
                    ml <= 0 ? 0x707070 : 0x1E4A80, 41);
        }
        Component st = DosingPumpBlockEntity.statusText(menu.status(), menu.tanks());
        int col = menu.status() == DosingPumpBlockEntity.RUNNING ? 0x206020 : 0x9A5A10;
        small(g, st, imageWidth / 2, 76, col, imageWidth - 8);
        if (menu.status() != DosingPumpBlockEntity.NO_TANK) {
            Component v = Component.translatable("gui.bettacraft.dosing.values", menu.calcium(), String.format(java.util.Locale.ROOT, "%.1f", menu.kh()),
                    menu.magnesium(), menu.trace());
            small(g, v, imageWidth / 2, 85, TEXT, imageWidth - 8);
        }
    }

    /** Centred text at 0.75 scale (shrunk further to fit the width). */
    private void small(GuiGraphics g, Component c, int cx, int y, int color, int max) {
        float s = 0.75F;
        int w = font.width(c);
        if (w * s > max) s = max / (float) w;
        g.pose().pushPose();
        g.pose().translate(cx, y, 0);
        g.pose().scale(s, s, 1);
        g.drawString(font, c, -w / 2, 0, color, false);
        g.pose().popPose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
