package org.overengineer.bettacraft.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.overengineer.bettacraft.fertilizer.FertilizerMakerBlockEntity;
import org.overengineer.bettacraft.fertilizer.FertilizerMakerMenu;

/**
 * M37: the fertilizer maker window: kelp meal and nutrient salt on the left, the progress arrow, the water tank (blue)
 * and the fertilizer tank (green), the bottle slot and the filled bottle on the right, the spring (or the FE) as a bar on
 * the far left (with power: the FE bar and the spring beside it) and the status.
 */
public class FertilizerMakerScreen extends AbstractContainerScreen<FertilizerMakerMenu> {
    private static final ResourceLocation BURN = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");
    private static final int BG = 0xFFC6C6C6, DARK = 0xFF555555, LIGHT = 0xFFFFFFFF, SLOT = 0xFF8B8B8B, TEXT = 0x404040;
    private static final int BAR_X = 9, BAR_Y = 24, BAR_H = 38, WATER_X = 88, FERT_X = 108, TANK_Y = 18, TANK_H = 50, TANK_W = 12;

    public FertilizerMakerScreen(FertilizerMakerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, BG);
        g.fill(x, y, x + imageWidth, y + 1, LIGHT);
        g.fill(x, y, x + 1, y + imageHeight, LIGHT);
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, DARK);
        g.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, DARK);
        for (var s : menu.slots) slot(g, x + s.x - 1, y + s.y - 1, 18);
        // the drive: FE (red) or the spring (brass); with FE the spring stands beside it while it may be wound
        int bx = x + BAR_X, by = y + BAR_Y;
        g.fill(bx - 1, by - 1, bx + 7, by + BAR_H + 1, DARK);
        g.fill(bx, by, bx + 6, by + BAR_H, 0xFF2A2A2A);
        int h = (int) Math.round((double) BAR_H * menu.drive() / menu.driveMax());
        if (h > 0) g.fill(bx, by + BAR_H - h, bx + 6, by + BAR_H, menu.powered() ? 0xFFD03030 : 0xFFC8A040);
        if (menu.powered()) SpringGauge.draw(g, bx + 9, by, BAR_H, menu.springPercent());
        // progress arrow
        int ax = x + 56, ay = y + 35;
        g.fill(ax + 1, ay + 6, ax + 16, ay + 10, SLOT);
        for (int i = 0; i < 8; i++) g.fill(ax + 15 + i, ay + 1 + i, ax + 16 + i, ay + 15 - i, SLOT);
        int w = (int) Math.ceil(24.0 * menu.progress() / menu.batchTicks());
        if (w > 0) g.blitSprite(BURN, 24, 16, 0, 0, ax, ay, Math.min(24, w), 16);
        tank(g, x + WATER_X, y + TANK_Y, menu.water(), 0xFF3A70C8);
        tank(g, x + FERT_X, y + TANK_Y, menu.fertilizer(), 0xFF7A9A2A);
    }

    private static void tank(GuiGraphics g, int x, int y, int mb, int colour) {
        g.fill(x - 1, y - 1, x + TANK_W + 1, y + TANK_H + 1, DARK);
        g.fill(x, y, x + TANK_W, y + TANK_H, 0xFF2A2A2A);
        int h = (int) Math.round((double) TANK_H * Math.min(mb, FertilizerMakerBlockEntity.TANK_MB) / FertilizerMakerBlockEntity.TANK_MB);
        if (h > 0) g.fill(x, y + TANK_H - h, x + TANK_W, y + TANK_H, colour);
        for (int i = 1; i < 4; i++) g.fill(x, y + i * TANK_H / 4, x + 3, y + i * TANK_H / 4 + 1, 0xFF8B8B8B);
    }

    private static void slot(GuiGraphics g, int x, int y, int size) {
        g.fill(x, y, x + size, y + size, SLOT);
        g.fill(x, y, x + size - 1, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + size - 1, 0xFF373737);
        g.fill(x + 1, y + size - 1, x + size, y + size, LIGHT);
        g.fill(x + size - 1, y + 1, x + size, y + size, LIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, 5, TEXT, false);
        Component st = FertilizerMakerBlockEntity.statusText(menu.status());
        float s = Math.min(0.75F, (imageWidth - 16) / (float) Math.max(1, font.width(st)));
        g.pose().pushPose();
        g.pose().translate(8, 72, 0);
        g.pose().scale(s, s, 1);
        int col = menu.status() == FertilizerMakerBlockEntity.WORKING ? 0x206020 : menu.status() == FertilizerMakerBlockEntity.IDLE ? TEXT : 0x9A5A10;
        g.drawString(font, st, 0, 0, col, false);
        g.pose().popPose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int bx = leftPos + BAR_X, by = topPos + BAR_Y;
        if (mouseX >= bx - 1 && mouseX < bx + 7 && mouseY >= by - 1 && mouseY < by + BAR_H + 1) {
            int pct = (int) Math.round(100.0 * menu.drive() / menu.driveMax());
            g.renderTooltip(font, menu.powered() ? Component.translatable("gui.bettacraft.food_maker.energy", menu.drive(), menu.driveMax())
                    : Component.translatable("gui.bettacraft.spring.tooltip", pct), mouseX, mouseY);
        } else if (menu.powered() && SpringGauge.over(mouseX, mouseY, bx + 9, by, BAR_H)) {
            SpringGauge.tooltip(g, font, mouseX, mouseY, menu.springPercent());
        } else if (over(mouseX, mouseY, leftPos + WATER_X, topPos + TANK_Y)) {
            g.renderTooltip(font, Component.translatable("gui.bettacraft.fertilizer_maker.water", menu.water(), FertilizerMakerBlockEntity.TANK_MB), mouseX, mouseY);
        } else if (over(mouseX, mouseY, leftPos + FERT_X, topPos + TANK_Y)) {
            g.renderTooltip(font, Component.translatable("gui.bettacraft.fertilizer_maker.fertilizer", menu.fertilizer(), FertilizerMakerBlockEntity.TANK_MB), mouseX, mouseY);
        }
    }

    private static boolean over(int mx, int my, int x, int y) {
        return mx >= x - 1 && mx < x + TANK_W + 1 && my >= y - 1 && my < y + TANK_H + 1;
    }
}
