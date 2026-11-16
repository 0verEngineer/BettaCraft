package org.overengineer.bettacraft.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.overengineer.bettacraft.network.WaterChangerPayload;
import org.overengineer.bettacraft.plumbing.WaterChangerBlockEntity;

/**
 * M24: the water change controller: interval (days, sneak: 5), share (%, sneak: 1), on/off, change now, and what it
 * did last. Values come live from the synced block entity; changes go to the server.
 */
public class WaterChangerScreen extends Screen {
    private static final int W = 270, H = 154; // M40 part b: two lines for the filter
    private final BlockPos pos;
    private int left, top;
    private Button toggle;

    public WaterChangerScreen(BlockPos pos) {
        super(Component.translatable("block.bettacraft.water_change_controller"));
        this.pos = pos;
    }

    private WaterChangerBlockEntity be() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getBlockEntity(pos) instanceof WaterChangerBlockEntity w ? w : null;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> send(hasShiftDown() ? -5 : -1, 0, null, false)).bounds(left + 150, top + 30, 20, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> send(hasShiftDown() ? 5 : 1, 0, null, false)).bounds(left + W - 30, top + 30, 20, 16).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> send(0, hasShiftDown() ? -1 : -5, null, false)).bounds(left + 150, top + 50, 20, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> send(0, hasShiftDown() ? 1 : 5, null, false)).bounds(left + W - 30, top + 50, 20, 16).build());
        toggle = addRenderableWidget(Button.builder(Component.empty(), b -> {
            WaterChangerBlockEntity w = be();
            if (w != null) send(0, 0, !w.enabled(), false);
        }).bounds(left + 10, top + 72, 120, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.bettacraft.water_changer.now"), b -> send(0, 0, null, true))
                .bounds(left + 140, top + 72, 120, 18).build());
    }

    private void send(int dInterval, int dShare, Boolean enabled, boolean now) {
        WaterChangerBlockEntity w = be();
        if (w == null) return;
        PacketDistributor.sendToServer(new WaterChangerPayload(pos, w.intervalDays() + dInterval, w.share() + dShare,
                enabled != null ? enabled : w.enabled(), now));
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (be() == null || mc.player == null || mc.player.distanceToSqr(pos.getCenter()) > 64) onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        WaterChangerBlockEntity w = be();
        if (w != null && toggle != null) toggle.setMessage(Component.translatable(w.enabled() ? "gui.bettacraft.water_changer.on" : "gui.bettacraft.water_changer.off"));
        super.render(g, mouseX, mouseY, partialTick);
        if (w == null) return;
        g.drawCenteredString(font, title, left + W / 2, top + 8, 0xFFFFFF);
        g.drawString(font, Component.translatable("gui.bettacraft.water_changer.interval", w.intervalDays()), left + 10, top + 34, 0xFFE0E0E0);
        g.drawString(font, Component.translatable("gui.bettacraft.water_changer.share", w.share()), left + 10, top + 54, 0xFFE0E0E0);
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        Component status = Component.translatable("gui.bettacraft.water_changer.status." + w.status().name().toLowerCase(java.util.Locale.ROOT), w.lastMb());
        g.drawString(font, status.copy().withStyle(w.status() == WaterChangerBlockEntity.Status.CHANGED || w.status() == WaterChangerBlockEntity.Status.WAITING
                ? ChatFormatting.AQUA : ChatFormatting.GOLD), left + 10, top + 98, 0xFFFFFF);
        if (w.filterTanks() > 0) { // M40 part b: on a multiblock filter
            g.drawString(font, Component.translatable("gui.bettacraft.water_changer.filter", w.filterTanks()).withStyle(ChatFormatting.DARK_AQUA), left + 10, top + 126, 0xFFFFFF);
            if (w.lastWasteMb() > 0) g.drawString(font, Component.translatable("gui.bettacraft.water_changer.filter_waste", w.lastWasteMb())
                    .withStyle(ChatFormatting.GRAY), left + 10, top + 138, 0xFFFFFF);
        }
        if (w.last() >= 0 && w.enabled()) {
            double days = Math.max(0, (w.last() + w.intervalDays() * 24000L - now) / 24000.0);
            g.drawString(font, Component.translatable("gui.bettacraft.water_changer.next", String.format(java.util.Locale.ROOT, "%.1f", days))
                    .withStyle(ChatFormatting.GRAY), left + 10, top + 112, 0xFFFFFF);
        }
        g.drawCenteredString(font, Component.translatable("gui.bettacraft.water_changer.hint").withStyle(ChatFormatting.GRAY), left + W / 2, top + H + 4, 0xA0A0A0);
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
