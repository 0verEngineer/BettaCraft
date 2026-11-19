package org.overengineer.bettacraft.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.overengineer.bettacraft.food.LiveFoodPumpBlockEntity;
import org.overengineer.bettacraft.food.LiveFoodPumpMenu;

/**
 * M26: the live food pump window: doses per day and portions per dose with - and +, "Dose now", the dose times, what
 * the pump did last, the food next to it and the tanks it reaches.
 */
public class LiveFoodPumpScreen extends AbstractContainerScreen<LiveFoodPumpMenu> {
    private static final int BG = 0xFFC6C6C6, DARK = 0xFF555555, LIGHT = 0xFFFFFFFF, TEXT = 0x404040;

    public LiveFoodPumpScreen(LiveFoodPumpMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, 112);
        inventoryLabelY = 72; // M52c: the 26.1 constructor derives it from imageHeight; keep the old place
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos, y = topPos;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> click(LiveFoodPumpMenu.DOSES_DOWN)).bounds(x + 112, y + 18, 16, 14).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> click(LiveFoodPumpMenu.DOSES_UP)).bounds(x + 152, y + 18, 16, 14).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> click(LiveFoodPumpMenu.PORTIONS_DOWN)).bounds(x + 112, y + 36, 16, 14).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> click(LiveFoodPumpMenu.PORTIONS_UP)).bounds(x + 152, y + 36, 16, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.live_food_pump.dose_now"), b -> click(LiveFoodPumpMenu.DOSE_NOW))
                .bounds(x + 8, y + 56, 80, 16).build());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, BG);
        g.fill(x, y, x + imageWidth, y + 1, LIGHT);
        g.fill(x, y, x + 1, y + imageHeight, LIGHT);
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, DARK);
        g.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, DARK);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, org.overengineer.bettacraft.client.ClientCompat.textColor(TEXT), false);
        left(g, Component.translatable("gui.bettacraft.live_food_pump.doses"), 8, 22, TEXT, 100);
        left(g, Component.translatable("gui.bettacraft.live_food_pump.portions"), 8, 40, TEXT, 100);
        centre(g, menu.doses() <= 0 ? Component.translatable("gui.bettacraft.live_food_pump.doses_off") : Component.literal(String.valueOf(menu.doses())),
                140, 22, menu.doses() <= 0 ? 0x707070 : 0x1E4A80);
        centre(g, Component.literal(String.valueOf(menu.portions())), 140, 40, 0x1E4A80);
        left(g, times(menu.doses()), 94, 61, 0x505050, 76);
        int col = menu.status() == LiveFoodPumpBlockEntity.PUMPED ? 0x206020
                : menu.status() == LiveFoodPumpBlockEntity.IDLE ? TEXT : 0x9A5A10;
        left(g, LiveFoodPumpBlockEntity.statusText(menu.status(), menu.lastPumped()), 8, 80, col, imageWidth - 16);
        left(g, Component.translatable("gui.bettacraft.live_food_pump.reach", menu.food(), menu.tanks()), 8, 92, TEXT, imageWidth - 16);
    }

    /** The dose times of the day ("7:00, 19:00"). */
    private static Component times(int doses) {
        if (doses <= 0) return Component.translatable("gui.bettacraft.live_food_pump.manual");
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < doses; i++) {
            long t = (LiveFoodPumpBlockEntity.FIRST_DOSE + i * 24000L / doses) % 24000L;
            long minutes = ((t + 6000) % 24000) * 60 / 1000;
            if (!b.isEmpty()) b.append(", ");
            b.append(minutes / 60).append(':').append(String.format(java.util.Locale.ROOT, "%02d", minutes % 60));
        }
        return Component.literal(b.toString());
    }

    private void left(GuiGraphicsExtractor g, Component c, int x, int y, int color, int max) {
        float s = 0.75F;
        int w = font.width(c);
        if (w * s > max) s = max / (float) w;
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(s, s);
        g.text(font, c, 0, 0, org.overengineer.bettacraft.client.ClientCompat.textColor(color), false);
        g.pose().popMatrix();
    }

    private void centre(GuiGraphicsExtractor g, Component c, int cx, int y, int color) {
        g.text(font, c, cx - font.width(c) / 2, y, org.overengineer.bettacraft.client.ClientCompat.textColor(color), false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }
}
