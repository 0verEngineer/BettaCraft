package org.overengineer.bettacraft.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.equipment.FeederMenu;

/**
 * Feeder screen: the food slots (3x3 on a tank, two for the clip-on feeder, M66) and a "n x per day" button; a small hint says
 * that the feeder also takes food from a cabinet under the tank.
 */
public class FeederScreen extends AbstractContainerScreen<FeederMenu> {
    private static final Identifier TEXTURE = BettaCraft.id("textures/gui/feeder.png");
    private Button cycle;

    public FeederScreen(FeederMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = FeederMenu.WIDTH;
        imageHeight = FeederMenu.HEIGHT;
        inventoryLabelX = FeederMenu.INV_X;
        inventoryLabelY = FeederMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
        int bx = menu.slotCount() == 2 ? 112 : 124;
        cycle = addRenderableWidget(Button.builder(Component.empty(), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, FeederMenu.BUTTON_CYCLE);
        }).bounds(leftPos + bx, topPos + 34, 50, 16).build());
        cycle.setTooltip(Tooltip.create(Component.translatable("gui.bettacraft.feeder.tooltip")));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        cycle.setMessage(Component.translatable("gui.bettacraft.feeder.per_day", menu.feedingsPerDay()));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        for (int i = 0; i < menu.slotCount(); i++) {
            int[] at = FeederMenu.slotAt(menu.slotCount(), i);
            g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + at[0] - 1, topPos + at[1] - 1, 180, 0, 18, 18, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        // small and grey under the slots: it also takes food from a cabinet under the tank
        var lines = font.split(Component.translatable("gui.bettacraft.feeder.cabinet_hint"), (int) ((imageWidth - 16) / 0.8F));
        g.pose().pushMatrix();
        g.pose().translate((float) (8), (float) (76));
        g.pose().scale(0.8F, 0.8F);
        int y = 0;
        for (var l : lines) {
            if (y > 11) break;
            g.text(font, l, 0, y, org.overengineer.bettacraft.client.ClientCompat.textColor(0x606060), false);
            y += 10;
        }
        g.pose().popMatrix();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }
}
