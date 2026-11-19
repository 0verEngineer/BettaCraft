package org.overengineer.bettacraft.client.screen;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.overengineer.bettacraft.food.FoodMakerBlockEntity;
import org.overengineer.bettacraft.food.FoodMakerMenu;
import org.overengineer.bettacraft.food.FoodMakerRecipe;

/**
 * M26 part b: the food maker window: the product (&lt; automatic / a recipe &gt;), the four inputs, the progress arrow,
 * the output, the spring (or the FE) as a bar on the left and the status.
 */
public class FoodMakerScreen extends AbstractContainerScreen<FoodMakerMenu> {
    private static final Identifier BURN = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final int BG = 0xFFC6C6C6, DARK = 0xFF555555, LIGHT = 0xFFFFFFFF, SLOT = 0xFF8B8B8B, TEXT = 0x404040;

    public FoodMakerScreen(FoodMakerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 176, 166);
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> click(FoodMakerMenu.PREV)).bounds(leftPos + 22, topPos + 16, 12, 12).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> click(FoodMakerMenu.NEXT)).bounds(leftPos + 142, topPos + 16, 12, 12).build());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private List<RecipeHolder<FoodMakerRecipe>> recipes() {
        return minecraft == null || minecraft.level == null ? List.of() : FoodMakerBlockEntity.recipes(null);
    }

    /** The chosen product: EMPTY for automatic. */
    private ItemStack product() {
        int i = menu.selected();
        List<RecipeHolder<FoodMakerRecipe>> l = recipes();
        return i <= 0 || i > l.size() ? ItemStack.EMPTY : l.get(i - 1).value().result();
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
        for (var s : menu.slots) {
            if (s.index == FoodMakerBlockEntity.OUTPUT) continue;
            slot(g, x + s.x - 1, y + s.y - 1, 18);
        }
        slot(g, x + FoodMakerMenu.OUT_X, y + FoodMakerMenu.OUT_Y, 26);
        // the spring (or the FE) on the left
        int bx = x + 9, by = y + 32, bh = 36;
        g.fill(bx - 1, by - 1, bx + 7, by + bh + 1, DARK);
        g.fill(bx, by, bx + 6, by + bh, 0xFF2A2A2A);
        int h = (int) Math.round((double) bh * menu.drive() / menu.driveMax());
        if (h > 0) g.fill(bx, by + bh - h, bx + 6, by + bh, menu.powered() ? 0xFFD03030 : 0xFFC8A040);
        if (menu.powered()) SpringGauge.draw(g, bx + 9, by, bh, menu.springPercent()); // M37: FE first, then the spring
        // progress
        int w = (int) Math.ceil(24.0 * menu.progress() / menu.batchTicks());
        // the empty arrow (shaft and head), then the progress over it
        int ax = x + 76, ay = y + 46;
        g.fill(ax + 1, ay + 6, ax + 16, ay + 10, SLOT);
        for (int i = 0; i < 8; i++) g.fill(ax + 15 + i, ay + 1 + i, ax + 16 + i, ay + 15 - i, SLOT);
        if (w > 0) g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, BURN, 24, 16, 0, 0, ax, ay, Math.min(24, w), 16);
    }

    private static void slot(GuiGraphicsExtractor g, int x, int y, int size) {
        g.fill(x, y, x + size, y + size, SLOT);
        g.fill(x, y, x + size - 1, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + size - 1, 0xFF373737);
        g.fill(x + 1, y + size - 1, x + size, y + size, LIGHT);
        g.fill(x + size - 1, y + 1, x + size, y + size, LIGHT);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, 5, org.overengineer.bettacraft.client.ClientCompat.textColor(TEXT), false);

        ItemStack p = product();
        Component name = p.isEmpty() ? Component.translatable("gui.bettacraft.food_maker.auto") : p.getHoverName();
        int nw = Math.min(font.width(name), 96);
        int cx = 88 - (nw + (p.isEmpty() ? 0 : 10)) / 2;
        if (!p.isEmpty()) {
            g.pose().pushMatrix();
            g.pose().translate(cx, 16);
            g.pose().scale(0.75F, 0.75F);
            g.item(p, 0, 0);
            g.pose().popMatrix();
            cx += 14;
        }
        g.text(font, font.plainSubstrByWidth(name.getString(), 96), cx, 18, org.overengineer.bettacraft.client.ClientCompat.textColor(0x1E4A80), false);
        Component st = FoodMakerBlockEntity.statusText(menu.status());
        float s = Math.min(0.75F, (imageWidth - 16) / (float) Math.max(1, font.width(st)));
        g.pose().pushMatrix();
        g.pose().translate(8, 72);
        g.pose().scale(s, s);
        int col = menu.status() == FoodMakerBlockEntity.WORKING ? 0x206020 : menu.status() == FoodMakerBlockEntity.IDLE ? TEXT : 0x9A5A10;
        g.text(font, st, 0, 0, org.overengineer.bettacraft.client.ClientCompat.textColor(col), false);
        g.pose().popMatrix();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int bx = leftPos + 8, by = topPos + 31;
        if (mouseX >= bx && mouseX < bx + 8 && mouseY >= by && mouseY < by + 38) {
            int pct = (int) Math.round(100.0 * menu.drive() / menu.driveMax());
            g.setTooltipForNextFrame(font, menu.powered()
                    ? Component.translatable("gui.bettacraft.food_maker.energy", menu.drive(), menu.driveMax())
                    : Component.translatable("gui.bettacraft.spring.tooltip", pct), mouseX, mouseY);
        } else if (menu.powered() && SpringGauge.over(mouseX, mouseY, leftPos + 18, topPos + 32, 36)) {
            SpringGauge.tooltip(g, font, mouseX, mouseY, menu.springPercent());
        }
    }
}
