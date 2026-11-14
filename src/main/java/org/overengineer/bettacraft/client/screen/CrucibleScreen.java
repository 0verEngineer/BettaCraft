package org.overengineer.bettacraft.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.crucible.CrucibleBlockEntity;
import org.overengineer.bettacraft.crucible.CrucibleMenu;

/** M24: the crucible: inputs, fuel with its flame, the melt progress and where the heat comes from. */
public class CrucibleScreen extends AbstractContainerScreen<CrucibleMenu> {
    static final ResourceLocation TEXTURE = BettaCraft.id("textures/gui/crucible.png");
    private static final ResourceLocation LIT = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");
    private static final ResourceLocation BURN = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");

    public CrucibleScreen(CrucibleMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        // flame between the inputs and the fuel (full while lava or FE heats it)
        CrucibleBlockEntity.Heat src = menu.source();
        int h = src == CrucibleBlockEntity.Heat.LAVA || src == CrucibleBlockEntity.Heat.FE ? 14
                : menu.heat() > 0 ? Math.max(1, (int) Math.ceil(13.0 * menu.heat() / menu.heatMax()) + 1) : 0;
        if (h > 0) g.blitSprite(LIT, 14, 14, 0, 14 - h, leftPos + 66, topPos + 54 + 14 - h, 14, h);
        int w = (int) Math.ceil(24.0 * menu.progress() / menu.meltTicks());
        if (w > 0) g.blitSprite(BURN, 24, 16, 0, 0, leftPos + 80, topPos + 30, w, 16);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        Component src = Component.translatable("gui.bettacraft.crucible.heat." + menu.source().name().toLowerCase(java.util.Locale.ROOT));
        g.drawString(font, src.copy().withStyle(ChatFormatting.DARK_GRAY), 86, 62, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
