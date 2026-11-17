package org.overengineer.bettacraft.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.overengineer.bettacraft.platform.network.PacketDistributor;
import org.overengineer.bettacraft.filter.FilterMedium;
import org.overengineer.bettacraft.filter.FilterPumpHeadBlockEntity;
import org.overengineer.bettacraft.filter.MultiblockFilter;
import org.overengineer.bettacraft.network.RegulatorPayload;

/**
 * M24: the multiblock external filter (opened on its pump head or a part): size, problem, the tank it cleans, flow,
 * cleaning, every chamber's medium with its dirt, and the power regulator.
 */
public class MultiblockFilterScreen extends Screen {
    private static final int W = 280, H = 214;
    private final BlockPos pos;
    private int left, top;

    public MultiblockFilterScreen(BlockPos pos) {
        super(Component.translatable("block.bettacraft.filter_pump_head"));
        this.pos = pos;
    }

    private FilterPumpHeadBlockEntity be() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getBlockEntity(pos) instanceof FilterPumpHeadBlockEntity h ? h : null;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> PacketDistributor.sendToServer(new RegulatorPayload(pos, -1, true)))
                .bounds(left + W - 70, top + 6, 20, 14).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> PacketDistributor.sendToServer(new RegulatorPayload(pos, 1, true)))
                .bounds(left + W - 26, top + 6, 20, 14).build());
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (be() == null || mc.player == null || mc.player.distanceToSqr(pos.getCenter()) > 100) onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        FilterPumpHeadBlockEntity h = be();
        if (h == null) return;
        int x = left + 10, y = top + 8;
        g.drawString(font, title, x, y, org.overengineer.bettacraft.client.ClientCompat.textColor(0xFFFFFF));
        g.drawCenteredString(font, Component.literal(h.power() * 10 + " %"), left + W - 38, top + 9, 0xFFE0E0E0);
        y += 16;
        MultiblockFilter.Problem p = h.problem();
        if (p != MultiblockFilter.Problem.NONE) {
            g.drawString(font, Component.translatable("gui.bettacraft.mb_filter.problem." + p.name().toLowerCase(java.util.Locale.ROOT),
                    MultiblockFilter.maxChambers()).withStyle(ChatFormatting.GOLD), x, y, org.overengineer.bettacraft.client.ClientCompat.textColor(0xFFFFFF));
            y += 12;
        }
        g.drawString(font, Component.translatable("gui.bettacraft.mb_filter.size", h.chambers(), h.inlets(), h.outlets()), x, y, 0xFFE0E0E0);
        y += 12;
        Component tank = h.servedTank() == null ? Component.translatable("gui.bettacraft.mb_filter.no_tank").withStyle(ChatFormatting.GOLD)
                : Component.translatable("gui.bettacraft.mb_filter.tank", h.servedTank().getX(), h.servedTank().getY(), h.servedTank().getZ()).withStyle(ChatFormatting.GREEN);
        g.drawString(font, tank, x, y, org.overengineer.bettacraft.client.ClientCompat.textColor(0xFFFFFF));
        y += 12;
        g.drawString(font, Component.translatable("gui.bettacraft.mb_filter.flow", h.flow(), Math.round(h.cleanBlocks())).withStyle(ChatFormatting.AQUA), x, y, org.overengineer.bettacraft.client.ClientCompat.textColor(0xFFFFFF));
        y += 12;
        if (h.freshPorts() > 0 || h.wastePorts() > 0) { // M40 part b: the water ports
            Component w = h.wastePorts() > 0 ? Component.translatable("gui.bettacraft.mb_filter.water", h.freshMb(), h.wasteMb())
                    : Component.translatable("gui.bettacraft.mb_filter.no_waste", h.freshMb());
            g.drawString(font, w.copy().withStyle(ChatFormatting.DARK_AQUA), x, y, org.overengineer.bettacraft.client.ClientCompat.textColor(0xFFFFFF));
            y += 12;
        }
        // M40: every tank of the filter (A1: its share by water volume), or why the filter does not work for it
        var tanks = h.tanks();
        if (h.retrofit() && tanks.stream().anyMatch(t -> t.status().warning())) {
            g.drawString(font, Component.translatable("gui.bettacraft.mb_filter.retrofit").withStyle(ChatFormatting.RED), x, y, org.overengineer.bettacraft.client.ClientCompat.textColor(0xFFFFFF));
            y += 11;
        }
        for (int i = 0; i < tanks.size() && i < 4; i++) {
            var t = tanks.get(i);
            String at = t.tank().getX() + " " + t.tank().getY() + " " + t.tank().getZ();
            Component line = t.status() == MultiblockFilter.LinkStatus.OK
                    ? Component.translatable("gui.bettacraft.mb_filter.link", at, t.percent(), t.flow(), Math.round(t.clean())).withStyle(ChatFormatting.GREEN)
                    : Component.translatable("gui.bettacraft.mb_filter.link_off", at, Component.translatable("gui.bettacraft.mb_filter.link." + t.status().key()))
                    .withStyle(t.status().warning() ? ChatFormatting.RED : ChatFormatting.GOLD);
            g.drawString(font, line, x, y, org.overengineer.bettacraft.client.ClientCompat.textColor(0xFFFFFF));
            y += 10;
        }
        if (tanks.size() > 4) {
            g.drawString(font, Component.translatable("gui.bettacraft.mb_filter.more_tanks", tanks.size() - 4).withStyle(ChatFormatting.GRAY), x, y, org.overengineer.bettacraft.client.ClientCompat.textColor(0xFFFFFF));
            y += 10;
        }
        y += 6;
        int[] m = h.media();
        int col = 0;
        for (int i = 0; i < m.length && i < 16; i++) {
            FilterMedium md = FilterMedium.values()[Math.max(0, Math.min(FilterMedium.values().length - 1, m[i] / 1000))];
            int dirt = m[i] % 1000;
            int cx = x + col * 132, cy = y + (i / 2) * 11;
            Component name = Component.translatable("gui.bettacraft.mb_filter.medium." + md.getSerializedName());
            Component line = md == FilterMedium.NONE ? Component.translatable("gui.bettacraft.mb_filter.chamber_empty", i + 1)
                    : Component.translatable(md.cleanable() ? "gui.bettacraft.mb_filter.chamber_dirt" : "gui.bettacraft.mb_filter.chamber_used", i + 1, name, dirt);
            ChatFormatting c = md == FilterMedium.NONE ? ChatFormatting.DARK_GRAY : dirt >= 80 ? ChatFormatting.RED : dirt >= 40 ? ChatFormatting.GOLD : ChatFormatting.GRAY;
            g.drawString(font, line.copy().withStyle(c), cx, cy, org.overengineer.bettacraft.client.ClientCompat.textColor(0xFFFFFF));
            col = 1 - col;
        }
        g.drawCenteredString(font, Component.translatable("gui.bettacraft.mb_filter.hint").withStyle(ChatFormatting.GRAY), left + W / 2, top + H + 4, org.overengineer.bettacraft.client.ClientCompat.textColor(0xA0A0A0));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + W, top + H, 0xE0101418);
        g.renderOutline(left, top, W, H, 0xFF6080A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
