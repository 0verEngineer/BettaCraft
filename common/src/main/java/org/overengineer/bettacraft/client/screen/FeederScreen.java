package org.overengineer.bettacraft.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.overengineer.bettacraft.equipment.FeederMenu;

/** Feeder screen: vanilla dispenser layout plus a "n x per day" button. */
public class FeederScreen extends AbstractContainerScreen<FeederMenu> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/dispenser.png");
    private Button cycle;

    public FeederScreen(FeederMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
        cycle = addRenderableWidget(Button.builder(Component.empty(), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, FeederMenu.BUTTON_CYCLE);
        }).bounds(leftPos + 120, topPos + 34, 50, 16).build());
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
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }
}
