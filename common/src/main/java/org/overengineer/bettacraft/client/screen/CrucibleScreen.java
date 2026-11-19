package org.overengineer.bettacraft.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.overengineer.bettacraft.BettaCraft;
import org.overengineer.bettacraft.crucible.CrucibleBlockEntity;
import org.overengineer.bettacraft.crucible.CrucibleMenu;

/** M24: the crucible: inputs, fuel with its flame, the melt progress and where the heat comes from. */
public class CrucibleScreen extends AbstractContainerScreen<CrucibleMenu> {
    static final Identifier TEXTURE = BettaCraft.id("textures/gui/crucible.png");
    private static final Identifier LIT = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final Identifier BURN = Identifier.withDefaultNamespace("container/furnace/burn_progress");

    public CrucibleScreen(CrucibleMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        // flame between the inputs and the fuel (full while lava or FE heats it)
        CrucibleBlockEntity.Heat src = menu.source();
        int h = src == CrucibleBlockEntity.Heat.LAVA || src == CrucibleBlockEntity.Heat.FE ? 14
                : menu.heat() > 0 ? Math.max(1, (int) Math.ceil(13.0 * menu.heat() / menu.heatMax()) + 1) : 0;
        if (h > 0) g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, LIT, 14, 14, 0, 14 - h, leftPos + 66, topPos + 54 + 14 - h, 14, h);
        int w = (int) Math.ceil(24.0 * menu.progress() / menu.meltTicks());
        if (w > 0) g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, BURN, 24, 16, 0, 0, leftPos + 80, topPos + 30, w, 16);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        Component src = Component.translatable("gui.bettacraft.crucible.heat." + menu.source().name().toLowerCase(java.util.Locale.ROOT));
        g.text(font, src.copy().withStyle(ChatFormatting.DARK_GRAY), 86, 62, org.overengineer.bettacraft.client.ClientCompat.textColor(0x404040), false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }
}
